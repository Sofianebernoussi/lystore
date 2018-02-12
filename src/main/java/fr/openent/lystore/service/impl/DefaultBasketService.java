package fr.openent.lystore.service.impl;


import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.BasketService;
import fr.openent.lystore.service.NotificationService;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.SqlResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static fr.wseduc.webutils.http.Renders.getHost;

public class DefaultBasketService  extends SqlCrudService implements BasketService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEquipmentService.class);

    private static NotificationService notificationService;

    public DefaultBasketService(String schema, String table, Vertx vertx, JsonObject slackConfiguration) {
        super(schema, table);
        notificationService = new SlackService(
                vertx,
                slackConfiguration.getString("api-uri"),
                slackConfiguration.getString("token"),
                slackConfiguration.getString("bot-username"),
                slackConfiguration.getString("channel")
        );
    }

    public void listBasket(Integer idCampaign, String idStructure, Handler<Either<String, JsonArray>> handler){
        JsonArray values = new JsonArray();
        String query = "SELECT basket.id, basket.amount, basket.processing_date," +
                "     basket.id_campaign, basket.id_structure " +
                "    , array_to_json(array_agg( e.* )) as equipment " +
                "    , array_to_json(array_agg(DISTINCT ep.*)) as options " +
                "    FROM " + Lystore.lystoreSchema + ".basket_equipment basket " +
                "    LEFT JOIN " + Lystore.lystoreSchema + ".basket_option " +
                "    ON basket_option.id_basket_equipment = basket.id " +
                "    LEFT JOIN " +
                "       (Select equipment_option.*, tax.value tax_amount " +
                "       FROM lystore.equipment_option " +
                "       INNER JOIN  lystore.tax on tax.id = equipment_option.id_tax ) ep "+
                "    ON basket_option.id_option = ep.id " +
                "    INNER JOIN " +
                "       (Select equipment.*, tax.value tax_amount " +
                "       FROM lystore.equipment INNER JOIN  lystore.tax " +
                "       ON tax.id = equipment.id_tax ) as e"+
                "    ON e.id = basket.id_equipment " +
                "    WHERE basket.id_campaign = ? " +
                "    AND basket.id_structure = ? " +
                "    GROUP BY " +
                "    (basket.id, basket.amount, basket.processing_date, basket.id_campaign, basket.id_structure ); ";
        values.addNumber(idCampaign).addString(idStructure);

        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }
    public void create(final JsonObject basket, final Handler<Either<String, JsonObject>> handler) {
        String getIdQuery = "SELECT nextval('" + Lystore.lystoreSchema + ".basket_equipment_id_seq') as id";
        sql.raw(getIdQuery, SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    try {
                        final Number id = event.right().getValue().getNumber("id");
                        JsonArray statements = new JsonArray()
                                .add(getBasketEquipmentCreationStatement(id, basket));

                        JsonArray options = basket.getArray("options");
                        int i = 0;
                        while (null != options && i < options.size() ) {
                            statements.add(getBasketEquipmentOptionCreationStatement(id, (Number) options.get(i)));
                            i++;
                        }

                        sql.transaction(statements, new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(Message<JsonObject> event) {
                                handler.handle(SqlQueryUtils.getTransactionHandler(event, id));
                            }
                        });
                    } catch (ClassCastException e) {
                        LOGGER.error("An error occurred when casting tags ids", e);
                        handler.handle(new Either.Left<String, JsonObject>(""));
                    }
                } else {
                    LOGGER.error("An error occurred when selecting next val");
                    handler.handle(new Either.Left<String, JsonObject>(""));
                }
            }
        }));
    }
    public void delete(final Integer idBasket,final Handler<Either<String, JsonObject>> handler){
        JsonArray statements = new JsonArray()
                .addObject(getOptionsBasketDeletion(idBasket))
                .addObject(getEquipmentBasketDeletion(idBasket));

        sql.transaction(statements, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(SqlQueryUtils.getTransactionHandler(event, idBasket));
            }
        });
    }
    public void updateAmount(Integer idBasket, Integer amount, Handler<Either<String, JsonObject>> handler ) {
        JsonArray values = new JsonArray();
        String query = " UPDATE " + Lystore.lystoreSchema + ".basket_equipment " +
                " SET  amount = ? " +
                " WHERE id = ?; ";
        values.addNumber(amount).addNumber(idBasket);

        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler) );
    }
    public void listebasketItemForOrder( Integer idCampaign, String idStructure,
                                         Handler<Either<String, JsonArray>> handler ){
        JsonArray values = new JsonArray();
        String query = "SELECT  basket.id id_basket, basket.amount, basket.processing_date,  " +
                "                     basket.id_campaign, basket.id_structure, " +
                "                     e.id id_equipment , e.price, e.tax_amount, " +
                "                    nextval('" + Lystore.lystoreSchema + ".order_client_equipment_id_seq' )" +
                "                    as id_order " +
                "                      , Case Count(ep) " +
                "                      when 0 then  ROUND(e.price + ((e.price *  e.tax_amount) /100) , 3 )  " +
                "                      else ROUND(e.price + ((e.price *  e.tax_amount) /100)  " +
                "                               + SUM(ep.total_option_price)  , 3 )" +
                "                       END as total_price" +
                "                    , array_to_json(array_agg(DISTINCT ep.*)) as options  " +
                "                    FROM  " + Lystore.lystoreSchema + ".basket_equipment basket  " +
                "                    LEFT JOIN  " + Lystore.lystoreSchema + ".basket_option  " +
                "                    ON basket_option.id_basket_equipment = basket.id  " +
                "                    LEFT JOIN  " +
                "                       (Select equipment_option.*, tax.value tax_amount," +
                "                        equipment_option.price + ((equipment_option.price * tax.value )/100)" +
                "                        as total_option_price  " +
                "                       FROM " + Lystore.lystoreSchema + ".equipment_option  " +
                "                       INNER JOIN  " + Lystore.lystoreSchema + ".tax " +
                "                       ON tax.id = equipment_option.id_tax ) ep  " +
                "                    ON basket_option.id_option = ep.id  " +
                "                    INNER JOIN  " +
                "                       (Select equipment.*,  tax.value tax_amount  " +
                "                       FROM " + Lystore.lystoreSchema + ".equipment " +
                "                       INNER JOIN  " + Lystore.lystoreSchema + ".tax  " +
                "                       ON tax.id = equipment.id_tax ) as e " +
                "                    ON e.id = basket.id_equipment  " +
                "                    WHERE basket.id_campaign = ? " +
                "                    AND basket.id_structure = ? " +
                "                    GROUP BY  " +
                "                    (basket.id, basket.amount, basket.processing_date," +
                "                     basket.id_campaign, basket.id_structure, e.id, e.price, e.tax_amount );  ";
        values.addNumber(idCampaign).addString(idStructure);

        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }

    public void takeOrder(final HttpServerRequest request, final JsonArray baskets, Integer idCampaign,
                          String idStructure, final String nameStructure ,
                          final Handler<Either<String, JsonObject>> handler ){
        try {
            JsonArray statements = new JsonArray();
            JsonObject basket;
            for (int i = 0; i < baskets.size(); i++) {
                basket = baskets.get(i);
                statements.add(getUpdatePurseStatment(Float.valueOf( basket.getString("total_price")),
                        idCampaign , idStructure ));
                statements.add(getInsertEquipmentOrderStatement( basket));
                if(! "[null]".equals( basket.getString("options"))) {
                    statements.add(getInsertEquipmentOptionsStatement(basket));
                    statements.add(getDeletionBasketsOptionsStatments((Number) basket.getInteger("id_basket")));
                }
            }
            statements.add(getDeletionBasketsEquipmentStatments(idCampaign, idStructure));

            sql.transaction(statements, new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> event) {
                    JsonObject results = event.body().getArray("results")
                            .get(event.body().getArray("results").size()-1);
                   JsonArray objectResult = results.getArray("results").get(0);
                    handler.handle(getTransactionHandler(request, nameStructure, getTotalPriceOfBasketList(baskets),
                            event, Float.valueOf((String) objectResult.get(0))));
                }
            });
        }catch (ClassCastException e) {
            LOGGER.error("An error occurred when casting baskets elements", e);
            handler.handle(new Either.Left<String, JsonObject>(""));
        }
    }
    private static JsonObject getUpdatePurseStatment(Float price, Integer idCampaign , String idStructure) {
        final double cons = 100.0;
        String updateQuery = "UPDATE lystore.purse " +
                        "SET amount = amount - ?  " +
                        "WHERE id_campaign = ? " +
                        "AND id_structure = ? ;";

        JsonArray params = new JsonArray()
                .addNumber(Math.round(price * cons)/cons)
                .addNumber(idCampaign)
                .addString(idStructure);

        return new JsonObject()
                .putString("statement", updateQuery)
                .putArray("values", params)
                .putString("action", "prepared");
    }
    private JsonObject getOptionsBasketDeletion(Integer idBasket) {
        String insertBasketEquipmentRelationshipQuery =
                "DELETE FROM " + Lystore.lystoreSchema + ".basket_option " +
                        " WHERE id_basket_equipment = ? ;";

        JsonArray params = new JsonArray()
                .addNumber(idBasket);

        return new JsonObject()
                .putString("statement", insertBasketEquipmentRelationshipQuery)
                .putArray("values", params)
                .putString("action", "prepared");
    }
    private JsonObject getEquipmentBasketDeletion(Integer idBasket) {
        String insertBasketEquipmentRelationshipQuery =
                "DELETE FROM " + Lystore.lystoreSchema + ".basket_equipment " +
                        "WHERE id= ? ;";

        JsonArray params = new JsonArray()
                .addNumber(idBasket);

        return new JsonObject()
                .putString("statement", insertBasketEquipmentRelationshipQuery)
                .putArray("values", params)
                .putString("action", "prepared");
    }

    /**
     * Returns a basket equipment insert statement
     *
     * @param id    basket Id
     * @param basket basket Object
     * @return basket equipment relationship transaction statement
     */
    private JsonObject getBasketEquipmentCreationStatement(Number id, JsonObject basket) {
        String insertBasketEquipmentRelationshipQuery =
                "INSERT INTO " + Lystore.lystoreSchema + ".basket_equipment(" +
                        "id, amount, processing_date, id_equipment, id_campaign, id_structure)" +
                        "VALUES (?, ?, ?, ?, ?, ?);";

        JsonArray params = new JsonArray()
                .addNumber(id)
                .addNumber(basket.getInteger("amount"))
                .addString(basket.getString("processing_date"))
                .addNumber(basket.getInteger("equipment"))
                .addNumber(basket.getInteger("id_campaign"))
                .addString(basket.getString("id_structure"));

        return new JsonObject()
                .putString("statement", insertBasketEquipmentRelationshipQuery)
                .putArray("values", params)
                .putString("action", "prepared");
    }

    /**
     * Returns a basket equipment-option insert statement
     * @param id id of equipment-basket
     * @param option id of options
     * @return basket equipment-option relationship transaction statement
     */
    private JsonObject getBasketEquipmentOptionCreationStatement(Number id, Number option) {
        String insertBasketEquipmentOptionRelationshipQuery =
                "INSERT INTO " + Lystore.lystoreSchema + ".basket_option " +
                        "(id_basket_equipment, id_option) " +
                        " VALUES (?, ?);";
        JsonArray params = new JsonArray()
                .addNumber(id)
                .addNumber(option);
        return new JsonObject()
                .putString("statement", insertBasketEquipmentOptionRelationshipQuery)
                .putArray("values", params)
                .putString("action", "prepared");
    }


    private static JsonObject getInsertEquipmentOrderStatement (JsonObject basket){

    StringBuilder queryEquipmentOrder = new StringBuilder()
            .append(" INSERT INTO lystore.order_client_equipment ")
            .append(" (id, price, tax_amount, amount, id_campaign, id_equipment, id_structure) VALUES ")
            .append(" (?, ?, ?, ?, ?, ?, ?); ");
    JsonArray params = new JsonArray();
    params.addNumber(basket.getNumber("id_order"))
            .addNumber(Float.valueOf(basket.getString("price")))
            .addNumber(Float.valueOf(basket.getString("tax_amount")))
            .addNumber(basket.getInteger("amount"))
            .addNumber(basket.getNumber("id_campaign"))
            .addNumber((Number) basket.getNumber("id_equipment"))
            .addString(basket.getString("id_structure"));

    return new JsonObject()
            .putString("statement", queryEquipmentOrder.toString())
            .putArray("values", params)
            .putString("action", "prepared");

    }

    private static JsonObject getInsertEquipmentOptionsStatement (JsonObject basket){
        JsonArray options = new JsonArray(basket.getString("options"))  ;
        StringBuilder queryEOptionEquipmentOrder = new StringBuilder()
                .append( " INSERT INTO lystore.order_client_options " )
                .append(" ( tax_amount, price, id_order_client_equipment, id_option) VALUES ") ;
        JsonArray params = new JsonArray();
        for (int i=0; i<options.size(); i++){
            queryEOptionEquipmentOrder.append("(?, ?, ?, ?)");
            queryEOptionEquipmentOrder.append( i == options.size()-1 ? "; " : ", ");
            JsonObject option = options.get(i);
            params.addNumber( option.getNumber("tax_amount"))
                    .addNumber(option.getNumber("price"))
                    .addNumber( basket.getNumber("id_order"))
                    .addNumber( option.getNumber("id"));
        }
        return new JsonObject()
                .putString("statement", queryEOptionEquipmentOrder.toString())
                .putArray("values", params)
                .putString("action", "prepared");
    }

    private static JsonObject getDeletionBasketsOptionsStatments (Number idBasketEquipment){
        StringBuilder queryEquipmentOrder = new StringBuilder()
                .append( " DELETE FROM lystore.basket_option " )
                .append( "WHERE id_basket_equipment = ? ;");

        return new JsonObject()
                .putString("statement", queryEquipmentOrder.toString())
                .putArray("values",  new JsonArray().addNumber(idBasketEquipment))
                .putString("action", "prepared");
    }
    private static JsonObject getDeletionBasketsEquipmentStatments(Integer idCampaign, String idStructure){
        StringBuilder queryEquipmentOrder = new StringBuilder()
                .append( " DELETE FROM lystore.basket_equipment " )
                .append( " WHERE id_campaign = ? AND id_structure = ? RETURNING (SELECT  amount ")
                .append(" FROM lystore.purse where id_campaign= ? and id_structure= ? );");
        JsonArray params = new JsonArray().addNumber(idCampaign).addString(idStructure)
                .addNumber(idCampaign).addString(idStructure);
        return new JsonObject()
                .putString("statement", queryEquipmentOrder.toString())
                .putArray("values", params )
                .putString("action", "prepared");
    }
    /**
     * Returns the amount of purse from an order transactions.
     *
     * @param event PostgreSQL event
     * @return Transaction handler
     */
    private static Either<String, JsonObject>
    getTransactionHandler(HttpServerRequest request, String nameStructure, Float totalPrice,
                          Message<JsonObject> event, Float amount) {
        Either<String, JsonObject> either;
        JsonObject result = event.body();
        if (result.containsField("status") && "ok".equals(result.getString("status"))) {
                JsonObject returns = new JsonObject()
                        .putNumber("amount", amount);
                DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                final double cons = 100.0;
                Number total =  Math.round(totalPrice * cons)/cons;
                either = new Either.Right<>(returns);
                notificationService.sendMessage(
                        I18n.getInstance().translate( "the.structure" ,
                                getHost(request) , I18n.acceptLanguage(request) )
                                +" "+ nameStructure +" "+
                                I18n.getInstance().translate( "lystore.slack.order.message1" ,
                                        getHost(request) , I18n.acceptLanguage(request) )
                                +" "+ total.toString()  +" "+
                                I18n.getInstance().translate( "money.symbol" ,
                                        getHost(request) , I18n.acceptLanguage(request) )
                                + " " +
                                I18n.getInstance().translate( "determiner.male" ,
                                        getHost(request) , I18n.acceptLanguage(request) )
                                +" "+ format.format(new Date())+" ");
        } else {
            LOGGER.error("An error occurred when launching 'order' transaction");
            either = new Either.Left<>("");
        }
        return either;
    }

    private static Float getTotalPriceOfBasketList(JsonArray baskets) {
        Float total = Float.valueOf(0);
            for(int i = 0; i < baskets.size(); i++) {
            total += Float.valueOf(( (JsonObject) baskets.get(i)).getString("total_price"));
        }
        return total;
    }
}
