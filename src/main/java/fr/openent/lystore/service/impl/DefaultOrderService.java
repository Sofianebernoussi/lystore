package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.ExportPDFService;
import fr.openent.lystore.service.OrderService;
import fr.openent.lystore.service.PurseService;
import fr.openent.lystore.service.StructureService;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import fr.wseduc.webutils.email.EmailSender;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.eventbus.impl.JsonObjectMessage;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.json.impl.Json;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.vertx.java.platform.Container;

import java.util.ArrayList;
import java.util.List;

public class DefaultOrderService extends SqlCrudService implements OrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger (DefaultOrderService.class);
    private PurseService purseService ;
    private EmailSendService emailSender ;
    private StructureService structureService;
    // private ExportPDFService exportPDFService;

    public DefaultOrderService(
            String schema, String table, EmailSender emailSender){
        super(schema,table);
        this.purseService = new DefaultPurseService();
        this.emailSender = new EmailSendService(emailSender);
        this.structureService = new DefaultStructureService();
        // this.exportPDFService = new DefaultExportPDFService( eb,  vertx,  container );
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

    /**
     * Get information on orders
     * @param ids list of orders
     * @param handler
     */
    JsonObject listOrder(List<Integer> ids){
        String query = "SELECT oce.* , oce.price * oce.amount as total_price , to_json(contract.*) contract ,to_json(supplier.*) supplier, " +
                "to_json(campaign.* ) campaign,  array_to_json(array_agg( DISTINCT oco.*)) as options " +
                "FROM lystore.order_client_equipment oce " +
                "LEFT JOIN "+ Lystore.lystoreSchema + ".order_client_options oco " +
                "ON oco.id_order_client_equipment = oce.id " +
                "LEFT JOIN "+ Lystore.lystoreSchema + ".contract ON oce.id_contract = contract.id " +
                "INNER JOIN "+ Lystore.lystoreSchema + ".supplier ON contract.id_supplier = supplier.id  " +
                "INNER JOIN "+ Lystore.lystoreSchema + ".campaign ON oce.id_campaign = campaign.id " +
                "WHERE oce.id in "+ Sql.listPrepared(ids.toArray()) +
                " GROUP BY (oce.id, contract.id, supplier.id, campaign.id); ";
        JsonArray params = new JsonArray();

        for (Integer id : ids) {
            params.addNumber( id);
        }

        return new JsonObject()
                .putString("statement", query)
                .putArray("values", params)
                .putString("action", "prepared");
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
    @Override
    public  void windUpOrders(List<Integer> ids, Handler<Either<String, JsonObject>> handler){
        JsonObject statement = getUpdateStatusStatement(ids, "DONE");
        sql.prepared(statement.getString("statement"),
                statement.getArray("values"),
                SqlResult.validUniqueResultHandler(handler));
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
    @Override
    public void sendOrders(List<Integer> ids,final Handler<Either<String, JsonObject>> handler){
        JsonArray statements = new JsonArray();
        statements.add(getUpdateStatusStatement(ids, "SENT"))
                .add(listOrder(ids));
        sql.transaction(statements, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> jsonObjectMessage) {
                final JsonObject ordersObject = formatSendOrdersResult( (JsonObject)
                        ((JsonObjectMessage) jsonObjectMessage).body()
                                .getArray("results").get(1) );
                structureService.getStructureById(ordersObject.getArray("id_structures"),
                        new Handler<Either<String, JsonArray>>() {
                            @Override
                            public void handle(Either<String, JsonArray> structureArray) {
                                if(structureArray.isRight()){
                                    Either<String, JsonObject> either;
                                    JsonArray orders = getOrdersOrderedByEquipment(
                                            ordersObject.getArray("order"),
                                            (JsonArray) structureArray.right().getValue());
                                    JsonObject returns = new JsonObject()
                                            .putArray("ordersObject",ordersObject.getArray("order"))
                                            .putObject("total",getTotalsOrdersPrices(ordersObject.getArray("order")))
                                            .putArray("structureArray", (JsonArray) structureArray.right().getValue());
                                    either = new Either.Right<>(returns);
                                    handler.handle(either);
                                }
                            }
                        });
            }
        });
    }
    private JsonArray getOrdersOrderedByEquipment (JsonArray ordersArray, JsonArray structures) {
        JsonArray orders = new JsonArray();
        boolean isIn;
        JsonObject orderOld;
        JsonObject orderNew;
        for (int i = 0 ; i< ordersArray.size(); i++){
             isIn = false;
             orderOld = ordersArray.get(i);
            for(int j = 0; j<orders.size(); j++){
                orderNew = (JsonObject) orders.get(j);
                if(orderOld.getString("equipment_key")
                        .equals(orderNew.getString("equipment_key"))){
                    isIn = true;
                    //TODO add the structure to the structureList
                }
            }
            if(! isIn) {
                JsonObject order = new JsonObject()
                        .putString("price", orderOld.getString("price"))
                        .putString("tax_amount", orderOld.getString("tax_amount"))
                        .putString("amount", orderOld.getString("amount"))
                        .putString("id_campaign", orderOld.getString("id_campaign"))
                        .putString("name", orderOld.getString("name"))
                        .putString("summary", orderOld.getString("summary"))
                        .putString("description", orderOld.getString("description"))
                        .putString("image", orderOld.getString("image"))
                        .putString("technical_spec", orderOld.getString("technical_spec"))
                        .putString("id_contract", orderOld.getString("id_contract"))
                        .putString("equipment_key", orderOld.getString("equipment_key"))
                        .putObject("contract", orderOld.getObject("contract"))
                        .putObject("supplier", orderOld.getObject("supplier"))
                        .putObject("campaign", orderOld.getObject("campaign"))
                        .putArray("options",orderOld.getArray("options"))
                        .putArray("structures", new JsonArray()
                                .add(getStructureObject( structures, orderOld.getString("id_structure") )) )
                        ;
                orders.add(order);
            }
        }
        return orders;
    }
    private JsonObject getStructureObject(JsonArray structures, String structureId){
        JsonObject structure = new JsonObject();
        for (int i = 0; i < structures.size() ; i++) {
            if(((JsonObject) structures.get(i)).getString("id").equals(structureId)){
                structure =  structures.get(i);
            }
        }
        return structure;
    }
    private JsonObject getTotalsOrdersPrices(JsonArray orders){

        Float tva = new Float(0);
        Float total = new Float(0);
        final Integer Const = 100;
        Float totalTTC ;
        try {
            tva = Float.parseFloat( ((JsonObject) orders.get(0)).getString("tax_amount"));
        }catch (ClassCastException e) {
            LOGGER.error("An error occurred when casting tax amount", e);
        }
        for(int i = 0 ; i < orders.size(); i++) {
            try {
                total += Float.parseFloat( ((JsonObject) orders.get(0)).getString("price")) *
                        Float.parseFloat( ((JsonObject) orders.get(0)).getString("amount"));
            }catch (ClassCastException e) {
                LOGGER.error("An error occurred when casting order price", e);
            }
        }
        totalTTC = (total * tva)/Const + total;
        return new JsonObject()
                .putNumber("totalPrice", total)
                .putNumber("tva", tva)
                .putNumber("totalTTC", totalTTC)
                ;
    }
    private JsonObject formatSendOrdersResult(JsonObject object){
        JsonArray fields = object.getArray("fields");
        JsonArray results = object.getArray("results");
        JsonArray result = new JsonArray();
        JsonArray idStructures = new JsonArray();
        JsonArray row ;
        JsonObject newRow = new JsonObject();
        for(int j = 0; j< results.size(); j++){
            row = results.get(j);
            for(int i = 0 ; i < fields.size() ; i++){
                newRow.putString(fields.get(i).toString(),
                        null != row.get(i) ? row.get(i).toString(): "null");
                if("id_structure".equals(fields.get(i).toString())){
                    idStructures.add(row.get(i).toString());
                }
            }
            result.add( newRow);
        }
        return  new JsonObject().putArray("order",result).putArray ("id_structures",idStructures);
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
                                .add(getValidateStatusStatement(ids, numberOrder, "VALID"))
                                .add(getAgentInformation( ids));
                        sql.transaction(statements, new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(Message<JsonObject> jsonObjectMessage) {
                                final JsonArray rows = ((JsonObject) ((JsonObjectMessage) jsonObjectMessage).body()
                                        .getArray("results").get(1)).getArray("results");
                                JsonArray names = new JsonArray();
                                final int agentNameIndex = 2;
                                final int structureIdIndex = 4;
                                JsonArray structureIds = new JsonArray();
                                for (int j = 0; j < rows.size(); j++) {
                                    names.addString((String) ((JsonArray)
                                            rows.get(j)).get(agentNameIndex));
                                    structureIds.addString((String) ((JsonArray)
                                            rows.get(j)).get(structureIdIndex));
                                }
                                final JsonArray agentNames = names;
                                emailSender.getPersonnelMailStructure(structureIds,
                                        new Handler<Either<String, JsonArray>>() {
                                            @Override
                                            public void handle(Either<String, JsonArray> stringJsonArrayEither) {
                                                final JsonObject result = new JsonObject()
                                                        .putString("number_validation", numberOrder)
                                                        .putArray("agent", agentNames);
                                                emailSender.sendMails( request, result,  rows,  user,  url,
                                                        (JsonArray) stringJsonArrayEither.right().getValue(), handler);
                                            }
                                        });
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



    private static JsonObject getAgentInformation(List<Integer> ids){
        String query = "SELECT oce.id, contract.name, agent.name, agent.email, oce.id_structure " +
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
    private static JsonObject getValidateStatusStatement(List<Integer>  ids, String numberOrder, String status){

        String query = "UPDATE lystore.order_client_equipment " +
                " SET  status = ?, number_validation = ?  " +
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
    private static JsonObject getUpdateStatusStatement(List<Integer>  ids, String status){

        String query = "UPDATE lystore.order_client_equipment " +
                " SET  status = ? " +
                " WHERE id in "+ Sql.listPrepared(ids.toArray()) +" ;  ";
        JsonArray params = new JsonArray().addString(status);

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

