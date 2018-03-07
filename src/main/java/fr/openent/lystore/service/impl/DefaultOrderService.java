package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.OrderService;
import fr.openent.lystore.service.PurseService;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import fr.wseduc.webutils.email.EmailSender;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.eventbus.impl.JsonObjectMessage;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.util.List;

public class DefaultOrderService extends SqlCrudService implements OrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger (DefaultOrderService.class);
    private PurseService purseService ;
    private final EmailSender emailSender;

    public DefaultOrderService(String schema, String table, EmailSender emailSender){
        super(schema,table);
        this.purseService = new DefaultPurseService();
        this.emailSender = emailSender;
    }

    @Override
    public void listOrder(Integer idCampaign, String idStructure, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new JsonArray();
        String query = "SELECT oe.id, oe.price, oe.tax_amount, oe.amount,oe.creation_date, oe.id_campaign," +
                " oe.id_structure, oe.name, oe.summary, oe.image, oe.status, oe.id_contract," +
                " array_to_json(array_agg(order_opts)) as options, c.name as name_supplier  " +
                "FROM "+ Lystore.lystoreSchema + ".order_client_equipment  oe " +
                "LEFT JOIN "+ Lystore.lystoreSchema + ".order_client_options order_opts ON " +
                "oe.id = order_opts.id_order_client_equipment " +
                "INNER JOIN (SELECT supplier.name, contract.id FROM " + Lystore.lystoreSchema + ".supplier INNER JOIN "
                + Lystore.lystoreSchema + ".contract ON contract.id_supplier = supplier.id) c " +
                "ON oe.id_contract = c.id WHERE id_campaign = ? AND id_structure = ? " +
                "GROUP BY (oe.id, c.name) ORDER BY creation_date";

        values.addNumber(idCampaign).addString(idStructure);
        sql.prepared(query, values, SqlResult.validResultHandler(handler));

    }

    @Override
    public  void listOrder(Handler<Either<String, JsonArray>> handler){
        String query = "SELECT oce.* , to_json(contract.*) contract ,to_json(supplier.*) supplier, " +
                "to_json(campaign.* ) campaign,  array_to_json(array_agg( DISTINCT oco.*)) as options " +
                "FROM lystore.order_client_equipment oce " +
                "LEFT JOIN "+ Lystore.lystoreSchema + ".order_client_options oco " +
                "ON oco.id_order_client_equipment = oce.id " +
                "LEFT JOIN "+ Lystore.lystoreSchema + ".contract ON oce.id_contract = contract.id " +
                "INNER JOIN "+ Lystore.lystoreSchema + ".supplier ON contract.id_supplier = supplier.id  " +
                "INNER JOIN "+ Lystore.lystoreSchema + ".campaign ON oce.id_campaign = campaign.id " +
                "GROUP BY (oce.id, contract.id, supplier.id, campaign.id); ";
        sql.prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }

    @Override
    public void listExport(Integer idCampaign, String idStructure, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new JsonArray();
        String query = "SELECT oe.name as equipment_name, oe.amount as equipment_quantity, " +
                "oe.creation_date as equipment_creation_date, oe.summary as equipment_summary, " +
                "oe.status as equipment_status,cause_status, price_all_options, CASE count(price_all_options) " +
                "WHEN 0 THEN ROUND ((oe.price+( oe.tax_amount*oe.price)/100)*oe.amount,2) "+
                "ELSE ROUND(price_all_options +(oe.price+(oe.tax_amount*oe.price)/100)*oe.amount,2)" +
                " END as price_total_equipment "+
                "FROM "+ Lystore.lystoreSchema + ".order_client_equipment  oe " +
                "LEFT JOIN (SELECT SUM(( price +( tax_amount*price)/100)*amount) as price_all_options," +
                " id_order_client_equipment FROM "+ Lystore.lystoreSchema + ".order_client_options " +
                "GROUP BY id_order_client_equipment)" +
                " opts ON oe.id = opts.id_order_client_equipment WHERE id_campaign = ? AND id_structure = ?" +
                " GROUP BY oe.id, price_all_options ORDER BY creation_date";

        values.addNumber(idCampaign).addString(idStructure);
        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void orderForDelete(Integer idOrder, Handler<Either<String, JsonObject>> handler) {

        String query = "SELECT  oe.id, oe.name,date_trunc('day',oe.creation_date)as creation_date, " +
                " id_campaign, id_structure," +
                " CASE count(opts) " +
                "WHEN 0 THEN ROUND ((oe.price+( oe.tax_amount*oe.price)/100)*oe.amount,2) "+
                "ELSE ROUND(price_all_options +(oe.price+(oe.tax_amount*oe.price)/100)*oe.amount,2) " +
                "END as price_total_equipment "+
                "FROM "+ Lystore.lystoreSchema + ".order_client_equipment  oe " +
                "LEFT JOIN (SELECT SUM(( price +( tax_amount*price)/100)*amount) as price_all_options," +
                " id_order_client_equipment FROM "+ Lystore.lystoreSchema + ".order_client_options " +
                "GROUP BY id_order_client_equipment)" +
                " opts ON oe.id = opts.id_order_client_equipment WHERE id= ? " +
                " GROUP BY oe.id, price_all_options";

        sql.prepared(query,new JsonArray().addNumber(idOrder),SqlResult.validUniqueResultHandler(handler));
    }


    @Override
    public void deleteOrder(final Integer idOrder, JsonObject order,
                            final String idStructure, final Handler<Either<String, JsonObject>> handler) {
        Integer idCampaign = order.getInteger("id_campaign");
        Float price = Float.valueOf(order.getString("price_total_equipment"));
        try{
            JsonArray statements = new JsonArray();
            statements.add(purseService.updatePurseAmountStatement(price, idCampaign, idStructure,"+"));
            statements.addObject(getOptionsOrderDeletion(idOrder));

            statements.addObject(getEquipmentOrderDeletion(idOrder, idCampaign, idStructure));

            sql.transaction(statements, new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> event) {
                    JsonObject results = event.body().getArray("results")
                            .get(event.body().getArray("results").size()-1);
                    JsonArray objectResult = results.getArray("results").get(0);
                    getTransactionHandler(event, new JsonObject((String) objectResult.get(0) ),handler);

                }
            });
        }catch(ClassCastException e){
            LOGGER.error("An error occurred when casting order elements", e);
            handler.handle(new Either.Left<String, JsonObject>(""));
        }
    }

    private JsonObject getOptionsOrderDeletion (Integer idOrder){
        String queryDeleteOptionsOrder = "DELETE FROM " + Lystore.lystoreSchema + ".order_client_options"
                + " WHERE id_order_client_equipment = ? ;";
        JsonArray params = new JsonArray()
                .addNumber(idOrder);
        return new JsonObject()
                .putString("statement", queryDeleteOptionsOrder)
                .putArray("values", params)
                .putString("action", "prepared");
    }

    private JsonObject getEquipmentOrderDeletion (Integer idOrder,Integer idCampaign, String idStructure){
        String queryDeleteEquipmentOrder = "DELETE FROM " + Lystore.lystoreSchema + ".order_client_equipment"
                + " WHERE id = ?  RETURNING " + getReturningQueryOfDeleteOrder();

        JsonArray params = new JsonArray()
                .addNumber(idOrder)
                .addNumber(idCampaign).addString(idStructure)
                .addNumber(idCampaign).addString(idStructure);

        return new JsonObject()
                .putString("statement", queryDeleteEquipmentOrder)
                .putArray("values", params)
                .putString("action", "prepared");
    }
    @Override
    public void validateOrders(final HttpServerRequest request,final UserInfos user, final List<Integer> ids,
                               final String url, final Handler<Either<String, JsonObject>> handler){
        String getIdQuery = "Select "+ Lystore.lystoreSchema + ".get_validation_number() as numberOrder ";
        sql.raw(getIdQuery, SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    try {
                        final String numberOrder = event.right().getValue().getString("numberorder");
                        JsonArray statements = new JsonArray()
                                .add(getUpdateStatusStatement(ids, numberOrder, "VALID"))
                                .add(getAgentInformation( ids));
                        sql.transaction(statements, new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(Message<JsonObject> jsonObjectMessage) {
                                JsonArray rows = ((JsonObject) ((JsonObjectMessage) jsonObjectMessage).body()
                                        .getArray("results").get(1)).getArray("results");
                                JsonArray names = new JsonArray();

                                final int contractNameIndex = 1 ;
                                final int agentNameIndex = 2;
                                final int agentEmailIndex = 3 ;
                                for(int j=0 ; j < rows.size(); j++) {
                                    names.addString((String)((JsonArray)
                                            rows.get(j)).get(agentNameIndex));
                                }
                                final JsonArray agentNames = names;
                                for(int i=0 ; i < rows.size(); i++){
                                    JsonArray row = rows.get(i);
                                    emailSender.sendEmail(request,
                                            (String) row.get(agentEmailIndex),
                                            null,
                                            null,
                                            "[LyStore] Commandes " + row.get(contractNameIndex),
                                            getAgentBodyMail(row, user, numberOrder, url),
                                            null,
                                            true,
                                            new Handler<Message<JsonObject>>() {
                                                @Override
                                                public void handle(Message<JsonObject> jsonObjectMessage) {
                                                    handler.handle(new Either.Right<String, JsonObject>(
                                                            new JsonObject()
                                                                    .putString("number",numberOrder)
                                                                    .putArray("agent", agentNames)));
                                                }
                                            });
                                }
                            }
                        });
                    } catch (ClassCastException e) {
                        LOGGER.error("An error occurred when casting numberOrder", e);
                        handler.handle(new Either.Left<String, JsonObject>(""));
                    }
                } else {
                    LOGGER.error("An error occurred when selecting number of the order");
                    handler.handle(new Either.Left<String, JsonObject>(""));
                }
            }
        }));

    }
    private static String getAgentBodyMail(JsonArray row, UserInfos user, String numberOrder, String url){
        final int contractName = 2 ;
        String body = "Bonjour " + row.get(contractName) + ", <br/> <br/>"
                + user.getFirstName() + " " + user.getLastName() + " vient de valider une commande sous le numéro \""
                + numberOrder + "\"."
                + " Une partie de la commande concerne le marche " + row.get(1) + ". "
                + "<br /> Pour générer le bon de commande et les CSF associés, il suffit de se rendre ici : <br />"
                + "<br />" + url + "#/order/client/waiting <br />"
                + "<br /> Bien Cordialement, "
                + "<br /> L'équipe LyStore. ";

        return formatAccentedString(body);

    }
    private static String formatAccentedString (String body){
        return  body.replace("&","&amp;").replace("€","&euro;")
                .replace("à","&agrave;").replace("â","&acirc;")
                .replace("é","&eacute;").replace("è","&egrave;")
                .replace("ê","&ecirc;").replace("î","&icirc;")
                .replace("ï","&iuml;") .replace("œ","&oelig;")
                .replace("ù","&ugrave;").replace("û","&ucirc;")
                .replace("ç","&ccedil;").replace("À","&Agrave;")
                .replace("Â","&Acirc;").replace("É","&Eacute;")
                .replace("È","&Egrave;").replace("Ê","&Ecirc;")
                .replace("Î","&Icirc;").replace("Ï","&Iuml;")
                .replace("Œ","&OElig;").replace("Ù","&Ugrave;")
                .replace("Û","&Ucirc;").replace("Ç","&Ccedil;");

    }
    private static JsonObject getAgentInformation(List<Integer> ids){
        String query = "SELECT oce.id, contract.name,agent.name, agent.email " +
                " FROM lystore.order_client_equipment oce " +
                " INNER JOIN lystore.contract ON contract.id = oce.id_contract " +
                " INNER JOIN lystore.agent ON contract.id_agent= agent.id " +
                " WHERE oce.id in "+ Sql.listPrepared(ids.toArray()) +" ;  ";
        JsonArray params = new JsonArray();

        for (Integer id : ids) {
            params.addNumber( id);
        }
        return new JsonObject().putString("statement", query)
                .putArray("values", params)
                .putString("action", "prepared");
    }
    private static JsonObject getUpdateStatusStatement(List<Integer>  ids, String numberOrder, String status){

        String query = "UPDATE lystore.order_client_equipment " +
                " SET  status = ?, \"number\" = ?  " +
                " WHERE id in "+ Sql.listPrepared(ids.toArray()) +" ;  ";
        JsonArray params = new JsonArray().addString(status).addString(numberOrder);

        for (Integer id : ids) {
            params.addNumber( id);
        }
        return new JsonObject()
                .putString("statement", query)
                .putArray("values", params)
                .putString("action", "prepared");
    }


    private static String getReturningQueryOfDeleteOrder(){
        return  "( SELECT row_to_json(row(p.amount, count(o.id ) )) " +
                " FROM " + Lystore.lystoreSchema + ".purse p, " + Lystore.lystoreSchema + ".order_client_equipment o " +
                " where p.id_campaign = ? " +
                " AND p.id_structure = ? " +
                " AND  o.id_campaign = ? " +
                " AND o.id_structure = ? " +
                " GROUP BY(p.amount) )";
    }
    private static void getTransactionHandler( Message<JsonObject> event, JsonObject amountPurseNbOrder,
                                               Handler<Either<String, JsonObject>> handler){
        JsonObject result = event.body();
        if (result.containsField("status")&& "ok".equals(result.getString("status"))){
            JsonObject returns = new JsonObject();
            returns.putNumber("amount",amountPurseNbOrder.getNumber("f1"));
            returns.putNumber("nb_order",amountPurseNbOrder.getNumber("f2"));
            handler.handle(new Either.Right<String, JsonObject>(returns));
        }  else {
            LOGGER.error("An error occurred when launching 'order' transaction");
            handler.handle(new Either.Left<String, JsonObject>(""));
        }

    }
}

