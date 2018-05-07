package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.OrderService;
import fr.openent.lystore.service.PurseService;
import fr.openent.lystore.service.StructureService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.email.EmailSender;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.eventbus.impl.JsonObjectMessage;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

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
    public  void listOrder(String status, Handler<Either<String, JsonArray>> handler){
        String query = "SELECT oce.* , to_json(contract.*) contract ,to_json(supplier.*) supplier, " +
                "to_json(campaign.* ) campaign,  array_to_json(array_agg( DISTINCT oco.*)) as options," +
                "array_to_json(array_agg(distinct structure_group.name)) as structure_groups " +
                "FROM " + Lystore.lystoreSchema + ".order_client_equipment oce " +
                "LEFT JOIN "+ Lystore.lystoreSchema + ".order_client_options oco " +
                "ON oco.id_order_client_equipment = oce.id " +
                "LEFT JOIN "+ Lystore.lystoreSchema + ".contract ON oce.id_contract = contract.id " +
                "INNER JOIN "+ Lystore.lystoreSchema + ".supplier ON contract.id_supplier = supplier.id  " +
                "INNER JOIN "+ Lystore.lystoreSchema + ".campaign ON oce.id_campaign = campaign.id " +
                "INNER JOIN "+ Lystore.lystoreSchema + ".rel_group_structure ON (oce.id_structure = rel_group_structure.id_structure) " +
                "INNER JOIN "+ Lystore.lystoreSchema + ".structure_group ON (rel_group_structure.id_structure_group = structure_group.id) " +
                "WHERE oce.status = ? " +
                "GROUP BY (oce.id, contract.id, supplier.id, campaign.id); ";
        sql.prepared(query, new JsonArray().addString(status), SqlResult.validResultHandler(handler));
    }

    @Override
    public void listOrders(List<Integer> ids, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT oce.* , oce.price * oce.amount as total_price , " +
                "to_json(contract.*) contract ,to_json(supplier.*) supplier, " +
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

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getStructuresId(JsonArray ids, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT id, id_structure " +
                "FROM " + Lystore.lystoreSchema + ".order_client_equipment " +
                "WHERE id IN " + Sql.listPrepared(ids.toArray()) + ";";
        JsonArray params = new JsonArray();

        for (int i = 0; i < ids.size(); i++) {
            params.addNumber((Integer) ids.get(i));
        }

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getOrders(JsonArray ids, String structureId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT price, tax_amount, name, id_contract, " +
                "SUM(amount) as amount ";
        if (structureId != null) {
            query += ", id_structure ";
        }
        query += "FROM " + Lystore.lystoreSchema + ".order_client_equipment " +
                "WHERE id IN " + Sql.listPrepared(ids.toArray());
        if (structureId != null) {
            query += "AND id_structure = ?";
        }
        query += " GROUP BY equipment_key, price, tax_amount, name, id_contract ";
        if (structureId != null) {
            query += ", id_structure ";
        }
        query += "UNION " +
                "SELECT options.price, options.tax_amount," +
                "options.name, equipment.id_contract," +
                "SUM(equipment.amount) as amount ";
        if (structureId != null) {
            query += ", equipment.id_structure ";
        }
        query += "FROM " + Lystore.lystoreSchema + ".order_client_options options " +
                "INNER JOIN " + Lystore.lystoreSchema + ".order_client_equipment equipment " +
                "ON (options.id_order_client_equipment = equipment.id) " +
                "WHERE id_order_client_equipment IN " + Sql.listPrepared(ids.toArray());
        if (structureId != null) {
            query += " AND equipment.id_structure = ?";
        }
        query += " GROUP BY options.name, equipment_key, options.price, options.tax_amount," +
                "equipment.id_contract";
        if (structureId != null) {
            query += ", equipment.id_structure";
        }

        JsonArray params = new JsonArray();

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < ids.size(); j++) {
                params.addNumber((Number) ids.get(j));
            }
            if (structureId != null) {
                params.addString(structureId);
            }
        }

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void addFileId(JsonArray ids, String fileId) {
        String query = "UPDATE " + Lystore.lystoreSchema + ".order_client_equipment " +
                "SET file = ? WHERE id IN " + Sql.listPrepared(ids.toArray());

        JsonArray params = new JsonArray()
                .addString(fileId);

        for (int i = 0; i < ids.size(); i++) {
            params.addNumber((Number) ids.get(i));
        }

        Sql.getInstance().prepared(query, params, null);
    }

    @Override
    public void getOrderByIds(JsonArray ids, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Lystore.lystoreSchema + ".order_client_equipment " +
                "WHERE id IN " + Sql.listPrepared(ids.toArray());

        JsonArray params = new JsonArray();

        for (int i = 0; i < ids.size(); i++) {
            params.addNumber((Number) ids.get(i));
        }

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getOrdersGroupByValidationNumber(String status, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT row.number_validation, contract.name as contract_name, supplier.name as supplier_name, " +
                "array_to_json(array_agg(structure_group.name)) as structure_groups, count(rel_group_structure) as structure_count " +
                "FROM " + Lystore.lystoreSchema + ".order_client_equipment row " +
                "INNER JOIN " + Lystore.lystoreSchema + ".equipment ON (row.equipment_key = equipment.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".contract ON (equipment.id_contract = contract.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".supplier ON (contract.id_supplier = supplier.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_group_structure ON (row.id_structure = rel_group_structure.id_structure) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".structure_group ON (rel_group_structure.id_structure_group = structure_group.id) " +
                "WHERE row.status = ? " +
                "GROUP BY row.number_validation, contract.name, supplier.name";

        this.sql.prepared(query, new JsonArray().addString(status), SqlResult.validResultHandler(handler));
    }

    @Override
    public void getOrdersDetailsIndexedByValidationNumber(String status, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT price, tax_amount, amount::text, number_validation " +
                "FROM " + Lystore.lystoreSchema + ".order_client_equipment " +
                "WHERE status = ? " +
                "UNION ALL " +
                "SELECT order_client_options.price, order_client_options.tax_amount, order_client_options.amount::text, order_client_equipment.number_validation " +
                "FROM " + Lystore.lystoreSchema + ".order_client_options " +
                "INNER JOIN " + Lystore.lystoreSchema + ".order_client_equipment ON (order_client_equipment.id = order_client_options.id_order_client_equipment) " +
                "WHERE order_client_equipment.status = ? ";

        this.sql.prepared(query, new JsonArray().addString(status).addString(status), SqlResult.validResultHandler(handler));
    }

    @Override
    public void listExport(Integer idCampaign, String idStructure, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new JsonArray();
        String query = "SELECT oe.name as equipment_name, oe.amount as equipment_quantity, " +
                "oe.creation_date as equipment_creation_date, oe.summary as equipment_summary, " +
                "oe.status as equipment_status,cause_status, price_all_options, CASE count(price_all_options) " +
                "WHEN 0 THEN ROUND ((oe.price+( oe.tax_amount*oe.price)/100)*oe.amount,2) "+
                "ELSE ROUND((price_all_options +( oe.price + ROUND((oe.tax_amount*oe.price)/100,2)))*oe.amount,2)" +
                " END as price_total_equipment "+
                "FROM "+ Lystore.lystoreSchema + ".order_client_equipment  oe " +
                "LEFT JOIN (SELECT ROUND (SUM(( price +( tax_amount*price)/100)*amount),2) as price_all_options," +
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
                "WHEN 0 THEN ROUND((oe.price + (oe.tax_amount * oe.price)/100), 2) * oe.amount "+
                "ELSE (price_all_options +(ROUND(oe.price + (oe.tax_amount * oe.price)/100, 2))) * oe.amount " +
                "END as price_total_equipment "+
                "FROM "+ Lystore.lystoreSchema + ".order_client_equipment  oe " +
                "LEFT JOIN (SELECT SUM((ROUND(price +(tax_amount * price)/100,2))) as price_all_options," +
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
            statements.addObject(getEquipmentOrderDeletion(idOrder));
            statements.addObject(getNewPurse(idCampaign,idStructure));
            statements.addObject(getNewNbOrder(idCampaign, idStructure));


            sql.transaction(statements, new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> event) {
                    JsonArray results = event.body().getArray("results");
                    JsonObject res = new JsonObject();
                    JsonObject newPurse = results.get(3);
                    JsonObject newOrderNumber = results.get(4);
                    JsonArray newPurseArray = newPurse.getArray("results").get(0);
                    JsonArray newOrderNumberArray = newOrderNumber.getArray("results").get(0);
                    res.putNumber("f1", newPurseArray.size() > 0
                        ? Float.parseFloat(newPurseArray.get(0).toString())
                        : 0);
                    res.putNumber("f2", newOrderNumberArray.size() > 0
                            ? Float.parseFloat(newOrderNumberArray.get(0).toString())
                            : 0);
                    getTransactionHandler(event, res, handler);

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

        this.listOrders(ids, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    JsonArray res = event.right().getValue();
                    final JsonObject ordersObject = formatSendOrdersResult(res);
                    structureService.getStructureById(ordersObject.getArray("id_structures"),
                        new Handler<Either<String, JsonArray>>() {
                            @Override
                            public void handle(Either<String, JsonArray> structureArray) {
                                if(structureArray.isRight()){
                                    Either<String, JsonObject> either;
                                    JsonObject returns = new JsonObject()
                                            .putArray("ordersCSF",
                                                    getOrdersFormatedCSF(ordersObject.getArray("order"),
                                                            (JsonArray) structureArray.right().getValue()))
                                            .putArray("ordersBC",
                                                    getOrdersFormatedBC(ordersObject.getArray("order"),
                                                            (JsonArray) structureArray.right().getValue()))
                                            .putObject("total",
                                                    getTotalsOrdersPrices(ordersObject.getArray("order")))
                                            ;
                                    either = new Either.Right<>(returns);
                                    handler.handle(either);
                                }
                            }
                        });
                } else {
                    handler.handle(new Either.Left<String, JsonObject>("An error occurred when collecting orders"));
                }
            }
        });
    }

    @Override
    public void updateStatus(List<Integer> ids, String status, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE lystore.order_client_equipment " +
                " SET  status = ? " +
                " WHERE id in "+ Sql.listPrepared(ids.toArray()) +";";
        JsonArray params = new JsonArray().addString(status);

        for (Integer id : ids) {
            params.addNumber( id);
        }

        Sql.getInstance().prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }

    private JsonArray getOrdersFormatedCSF (JsonArray ordersArray, JsonArray structures) {
        JsonArray orders = new JsonArray();
        JsonObject orderOld;
        for (int i = 0 ; i< ordersArray.size(); i++){
            orderOld = ordersArray.get(i);
            JsonObject order = orderOld
                        .putObject("structure",
                                (getStructureObject( structures, orderOld.getString("id_structure"))));
            if (orderOld.getArray("options").size() == 0) {
                order.putBoolean("hasOptions", false);
            } else {
                order.putBoolean("hasOptions", true);
            }
            orders.add(order);
            }

        return orders;
    }

    private JsonArray getOrdersFormatedBC (JsonArray ordersArray, JsonArray structures) {
        JsonArray orders = new JsonArray();
        boolean isIn;
        JsonObject orderOld;
        JsonObject orderNew;
        for (int i = 0 ; i< ordersArray.size(); i++){
             isIn = false;
             orderOld = ordersArray.get(i);
            for(int j = 0; j<orders.size(); j++){
                orderNew = orders.get(j);
                if(orderOld.getNumber("equipment_key").equals(orderNew.getNumber("equipment_key"))){
                    isIn = true;
                    JsonArray structure;
                    structure = orderNew.getArray("structures");
                    structure.add(getStructureObject( structures,
                                    orderOld.getString("id_structure"),
                                    orderOld.getNumber("amount").toString(),
                                    orderOld.getString("number_validation")));
                    orderNew.putArray("structures", structure);
                    Integer amount = (Integer.parseInt(orderOld.getNumber("amount").toString()) +
                            Integer.parseInt( orderNew.getNumber("amount").toString())) ;
                    orderNew.putString("amount",amount.toString());
                }
            }
            if(! isIn) {
                JsonObject order = new JsonObject()
                        .putString("price", orderOld.getString("price"))
                        .putString("tax_amount", orderOld.getString("tax_amount"))
                        .putNumber("amount", orderOld.getNumber("amount"))
                        .putString("id_campaign", orderOld.getNumber("id_campaign").toString())
                        .putString("name", orderOld.getString("name"))
                        .putString("summary", orderOld.getString("summary"))
                        .putString("description", orderOld.getString("description"))
                        .putString("image", orderOld.getString("image"))
                        .putString("technical_spec", orderOld.getString("technical_spec"))
                        .putString("id_contract", orderOld.getNumber("id_contract").toString())
                        .putNumber("equipment_key", orderOld.getNumber("equipment_key"))
                        .putObject("contract", new JsonObject( orderOld.getString("contract")))
                        .putObject("supplier",new JsonObject( orderOld.getString("supplier")) )
                        .putObject("campaign", new JsonObject( orderOld.getString("campaign")))
                        .putArray("options", orderOld.getArray("options"))
                        .putArray("structures", new JsonArray()
                                .addObject(getStructureObject( structures,
                                        orderOld.getString("id_structure"),
                                        orderOld.getNumber("amount").toString(),
                                        orderOld.getString("number_validation"))));
                orders.add(order);
            }
        }
        return orders;
    }
    private JsonObject getStructureObject(JsonArray structures, String structureId ){
        JsonObject structure = new JsonObject();
        for (int i = 0; i < structures.size() ; i++) {
            if(((JsonObject) structures.get(i)).getString("id").equals(structureId)){
                structure =  structures.get(i);
            }
        }
        return structure;
    }
    private JsonObject getStructureObject(JsonArray structures, String structureId,
                                          String amount, String numberValidation ){
        JsonObject structure = new JsonObject();
        for (int i = 0; i < structures.size() ; i++) {
            if(((JsonObject) structures.get(i)).getString("id").equals(structureId)){
                structure = ((JsonObject) structures.get(i)).copy();
                structure.putString("amount", amount)
                        .putString("number_validation", numberValidation);
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
                        Float.parseFloat( ((JsonObject) orders.get(0)).getNumber("amount").toString());
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

    private JsonObject formatSendOrdersResult(JsonArray orders){
        JsonObject orderObject = new JsonObject();
        JsonArray structures = new JsonArray();
        JsonArray ordersList = new JsonArray();
        JsonObject order;
        for (int i = 0; i < orders.size(); i++) {
            order = orders.get(i);
            structures.addString(order.getString("id_structure"));
            order.putArray("options",
                    !order.getString("options").contains("null")
                            ? new JsonArray(order.getString("options"))
                            : new JsonArray());
            ordersList.addObject(order);
        }
        orderObject.putArray("order", ordersList)
                .putArray ("id_structures", structures);
        return orderObject;
    }
    private JsonObject getEquipmentOrderDeletion (Integer idOrder){
        String queryDeleteEquipmentOrder = "DELETE FROM " + Lystore.lystoreSchema + ".order_client_equipment"
                + " WHERE id = ? ";

        JsonArray params = new JsonArray()
                .addNumber(idOrder);

        return new JsonObject()
                .putString("statement", queryDeleteEquipmentOrder)
                .putArray("values", params)
                .putString("action", "prepared");
    }
    private  JsonObject getNewPurse(Integer idCampaign, String idStructure){
        String query = "SELECT amount FROM " + Lystore.lystoreSchema + ".purse " +
                "WHERE id_campaign = ? " +
                "AND id_structure = ?;";

        JsonArray params = new JsonArray()
                .addNumber(idCampaign).addString(idStructure);

        return  new JsonObject()
                .putString("statement",query)
                .putArray("values",params)
                .putString("action", "prepared");
    }

    private JsonObject getNewNbOrder(Integer idCampaign, String idStructure) {
        String query = "SELECT count(id) FROM " + Lystore.lystoreSchema + ".order_client_equipment " +
                "WHERE id_campaign = ? " +
                "AND id_structure = ?;";

        JsonArray params = new JsonArray()
                .addNumber(idCampaign).addString(idStructure);

        return  new JsonObject()
                .putString("statement",query)
                .putArray("values",params)
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
                                                handler.handle(new Either.Right<String, JsonObject>(result));
                                                emailSender.sendMails( request, result,  rows,  user,  url,
                                                        (JsonArray) stringJsonArrayEither.right().getValue());
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
                " WHERE id in "+ Sql.listPrepared(ids.toArray()) +";";
        JsonArray params = new JsonArray().addString(status);

        for (Integer id : ids) {
            params.addNumber( id);
        }
        return new JsonObject()
                .putString("statement", query)
                .putArray("values", params)
                .putString("action", "prepared");
    }


    private static void getTransactionHandler( Message<JsonObject> event, JsonObject amountPurseNbOrder,
                                               Handler<Either<String, JsonObject>> handler){
        JsonObject result = event.body();
        if (result.containsField("status")&& "ok".equals(result.getString("status"))){
            JsonObject returns = new JsonObject();
            returns.putNumber("amount", amountPurseNbOrder.getNumber("f1"));
            returns.putNumber("nb_order",amountPurseNbOrder.getNumber("f2"));
            handler.handle(new Either.Right<String, JsonObject>(returns));
        }  else {
            LOGGER.error("An error occurred when launching 'order' transaction");
            handler.handle(new Either.Left<String, JsonObject>(""));
        }

    }
    @Override
    public void getExportCsvOrdersAdmin(List<Integer> idsOrders, Handler<Either<String, JsonArray>> handler) {

    String query = "SELECT oce.id, oce.id_structure as idStructure, contract.name as namecontract," +
            " supplier.name as namesupplier, campaign.name as namecampaign, oce.amount as qty," +
            " oce.creation_date as date, CASE count(priceOptions)"+
        "WHEN 0 THEN ROUND ((oce.price+( oce.tax_amount*oce.price)/100)*oce.amount,2)"+
        "ELSE ROUND((priceOptions +( oce.price + ROUND((oce.tax_amount*oce.price)/100,2)))*oce.amount,2) "+
        "END as priceTotal "+
        "FROM "+ Lystore.lystoreSchema +".order_client_equipment  oce "+
        "LEFT JOIN (SELECT ROUND (SUM(( price +( tax_amount*price)/100)*amount),2) as priceOptions, "+
        "id_order_client_equipment FROM "+ Lystore.lystoreSchema +".order_client_options  GROUP BY id_order_client_equipment) opts "+
        "ON oce.id = opts.id_order_client_equipment "+
        "LEFT JOIN "+ Lystore.lystoreSchema +".contract ON contract.id=oce.id_contract "+
        "INNER JOIN "+ Lystore.lystoreSchema +".campaign ON campaign.id = oce.id_campaign "+
        "INNER JOIN "+ Lystore.lystoreSchema +".supplier ON contract.id_supplier = supplier.id "+
        "WHERE oce.id in "+ Sql.listPrepared(idsOrders.toArray()) +
        " GROUP BY oce.id, idStructure, qty,date,oce.price, oce.tax_amount, oce.id_campaign, priceOptions," +
            " namecampaign, namecontract, namesupplier ;";

            JsonArray params = new JsonArray();

            for(Integer id : idsOrders){
                params.addNumber(id);
            }

           sql.prepared(query, params, SqlResult.validResultHandler(handler));

    }
}

