package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.BasketService;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.SqlResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

public class DefaultBasketService  extends SqlCrudService implements BasketService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEquipmentService.class);

    public DefaultBasketService(String schema, String table) {
        super(schema, table);
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

}
