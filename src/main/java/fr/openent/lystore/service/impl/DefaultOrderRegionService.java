package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.OrderRegionService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

public class DefaultOrderRegionService extends SqlCrudService implements OrderRegionService {

    public DefaultOrderRegionService(String table) {
        super(table);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOrderRegionService.class);
    @Override
    public void setOrderRegion(JsonObject order, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        String query = "";
        JsonArray params = new JsonArray();
        query = "" +
                "INSERT INTO  " + Lystore.lystoreSchema + ".\"order-region-equipment\" AS ore " +
                "(price, " +
                "amount, " +
                "creation_date, " +
                "owner_name, " +
                "owner_id, " +
                "equipment_key, " +
                "name, " +
                "comment, " +
                "id_order_client_equipment, " +
                "rank, ";
        query += order.containsKey("id_operation") ? "id_operation, " : "";
        query += "status, " +
                "id_campaign, " +
                "id_structure, " +
                "summary, " +
                "description, " +
                "image, " +
                "technical_spec, " +
                "id_contract, " +
                "cause_status, " +
                "number_validation, " +
                "id_order, " +
                "id_project) ";

        query += "SELECT " +
                "? ," +
                "? ," +
                "? ," +
                "? ," +
                "? ," +
                "? ," +
                "? ," +
                "? ," +
                "? ,";
        query +=  order.getInteger("rank") != -1? "?, " : "NULL, ";
        query += order.containsKey("id_operation") ? "?, " : "";
        query += " 'IN PROGRESS', " +
                "       id_campaign, " +
                "       id_structure, " +
                "       summary, " +
                "       description, " +
                "       image, " +
                "       technical_spec, " +
                "       id_contract, " +
                "       cause_status, " +
                "       number_validation, " +
                "       id_order, " +
                "       id_project " +
                "FROM  " + Lystore.lystoreSchema + ".order_client_equipment " +
                "WHERE id = ? " +
                "RETURNING id;";

        params.add(order.getFloat("price"))
                .add(order.getInteger("amount"))
                .add(order.getString("creation_date"))
                .add(user.getUsername())
                .add(user.getUserId())
                .add(order.getInteger("equipment_key"))
                .add(order.getString("name"))
                .add(order.getString("comment"))
                .add(order.getInteger("id_order_client_equipment"));
        if (order.getInteger("rank") != -1) {
            params.add(order.getInteger("rank"));
        }
        if (order.containsKey("id_operation")) {
            params.add(order.getInteger("id_operation"));
        }
        params.add(order.getInteger("id_order_client_equipment"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    public void updateOrderRegion(JsonObject order, int idOrder, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        String query = "";
        JsonArray params = new JsonArray();
        query = "" +
                "UPDATE " + Lystore.lystoreSchema + ".\"order-region-equipment\" " +
                " SET " +
                "price = ?, " +
                "amount = ?, " +
                "modification_date = ? , " +
                "owner_name = ? , " +
                "owner_id = ?, " +
                "name = ?, " +
                "equipment_key = ?, " +
                "cause_status = 'IN PROGRESS', ";
        query +=  order.getInteger("rank") != -1? "rank = ?,": "rank = NULL, ";
        query +=  order.containsKey("id_operation")?"id_operation = ?, ": "";
        query +=  "comment = ? " +
                "WHERE id = ? " +
                "RETURNING id;";

        params.add(order.getFloat("price"))
                .add(order.getInteger("amount"))
                .add(order.getString("creation_date"))
                .add(user.getUsername())
                .add(user.getUserId())
                .add(order.getString("name"))
                .add(order.getInteger("equipment_key"));
        if (order.getInteger("rank") != -1){
            params.add(order.getInteger("rank"));
        }
        if (order.containsKey("id_operation")){
            params.add(order.getInteger("id_operation"));
        }
        params.add(order.getString("comment"))
                .add(idOrder);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void linkOrderToOperation(Integer id_order_client_equipment, Integer id_operation, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new JsonArray();
        String query = " UPDATE " + Lystore.lystoreSchema + ".order_client_equipment " +
                "SET  " +
                "status = ? ,id_operation = ? " +
                "WHERE id = ? " +
                "RETURNING id;";

        values.add("IN PROGRESS");
        values.add(id_operation);
        values.add(id_order_client_equipment);
        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));

    }

    @Override
    public void createOrdersRegion(JsonObject orders, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        JsonArray ordersArray = orders.getJsonArray("orders");

        Integer id_title = ordersArray.getJsonObject(0).getInteger("title_id");
        String idQuery = "SELECT nextval('" + Lystore.lystoreSchema + ".project_id_seq') as id";
        sql.raw(idQuery, SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {

            @Override
            public void handle(Either<String, JsonObject> event) {
                final Number id = event.right().getValue().getInteger("id");
                JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                        .add(getProjectCreationStatement(id, id_title));

                for (int i = 0; i < ordersArray.size(); i++) {
                    JsonObject order = ordersArray.getJsonObject(i);
                    statements.add(getOrderRegionCreationStatement(id, order, user));
                }
                sql.transaction(statements, new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> event) {
                        if (event.body().containsKey("status") && "ok".equals(event.body().getString("status"))) {
                            handler.handle(new Either.Right<>(new JsonObject().put("message", "Created")));
                        } else {
                            String message = "An error occurred when handling orders region transaction";
                            LOGGER.error(message);
                            handler.handle(new Either.Left<String, JsonObject>(message));
                        }
                    }
                });

            }
        }));
    }

    private JsonObject getOrderRegionCreationStatement(Number id_project, JsonObject order, UserInfos user) {
        StringBuilder queryOrderRegionEquipment;
        JsonArray params;
        queryOrderRegionEquipment = new StringBuilder()
                .append(" INSERT INTO lystore.\"order-region-equipment\" ");

        if (order.containsKey("rank")) {
            queryOrderRegionEquipment.append(" ( price, amount, creation_date,  owner_name, owner_id, name, summary, description, image," +
                    " technical_spec, status, id_contract, equipment_key, id_campaign, id_structure," +
                    " comment,  id_project,  id_operation, rank) ")
                    .append("  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) ; ");

        } else {
            queryOrderRegionEquipment.append(" ( price, amount, creation_date,  owner_name, owner_id, name, summary, description, image," +
                    " technical_spec, status, id_contract, equipment_key, id_campaign, id_structure," +
                    " comment,  id_project,  id_operation) ")
                    .append("  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) ; ");
        }


        params = new fr.wseduc.webutils.collections.JsonArray()
                .add(order.getFloat("price"))
                .add(order.getInteger("amount"))
                .add(order.getString("creation_date"))
                .add(user.getUsername())
                .add(user.getUserId())
                .add(order.getString("name"))
                .add(order.getString("summary"))
                .add(order.getString("description"))
                .add(order.getString("image"))
                .add(order.getJsonArray("technical_specs"))
                .add("IN PROGRESS")
                .add(order.getInteger("id_contract"))
                .add(order.getInteger("equipment_key"))
                .add(order.getInteger("id_campaign"))
                .add(order.getString("id_structure"))
                .add(order.getString("comment"))
                .add(id_project)
                .add(order.getInteger("id_operation"))
        ;
        if (order.containsKey("rank")) {
            params.add(order.getInteger("rank"));
        }

        return new JsonObject()
                .put("statement", queryOrderRegionEquipment.toString())
                .put("values", params)
                .put("action", "prepared");

    }

    private JsonObject getProjectCreationStatement(Number id, Integer id_title) {
        StringBuilder queryProjectEquipment;
        JsonArray params;

        queryProjectEquipment = new StringBuilder()
                .append(" INSERT INTO lystore.project ")
                .append(" ( id, id_title ) VALUES ")
                .append(" (?, ?); ");
        params = new fr.wseduc.webutils.collections.JsonArray();

        params.add(id).add(id_title);

        return new JsonObject()
                .put("statement", queryProjectEquipment.toString())
                .put("values", params)
                .put("action", "prepared");
    }
    @Override
    public void deleteOneOrderRegion(int idOrderRegion, Handler<Either<String, JsonObject>> handler){
        String query = "" +
                "DELETE FROM " +
                Lystore.lystoreSchema +".\"order-region-equipment\" " +
                "WHERE id = ? " +
                "RETURNING id";
        sql.prepared(query, new JsonArray().add(idOrderRegion), SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void getOneOrderRegion(int idOrder, Handler<Either<String, JsonObject>> handler){
        String query = "" +
                "SELECT ore.*, " +
                "     ROUND( ore.price, 2 ) AS price_single_ttc, " +
                "       prj.id AS id_project, " +
                "       prj.preference AS preference, " +
                "       to_json(contract.*) contract, " +
                "       to_json(ct.*) contract_type, " +
                "       to_json(supplier.*) supplier, " +
                "       to_json(campaign.*) campaign, " +
                "       array_to_json(array_agg(DISTINCT structure_group.name)) AS structure_groups, " +
                "       to_json(prj.*) AS project, " +
                "       to_json(tt.*) AS title, " +
                "       o.order_number " +
                "FROM  " + Lystore.lystoreSchema + ".\"order-region-equipment\" AS ore " +
                "LEFT JOIN  " + Lystore.lystoreSchema + ".contract ON ore.id_contract = contract.id " +
                "INNER JOIN  " + Lystore.lystoreSchema + ".contract_type ct ON ct.id = contract.id_contract_type " +
                "INNER JOIN  " + Lystore.lystoreSchema + ".supplier ON contract.id_supplier = supplier.id " +
                "INNER JOIN  " + Lystore.lystoreSchema + ".campaign ON ore.id_campaign = campaign.id " +
                "INNER JOIN  " + Lystore.lystoreSchema + ".project AS prj ON ore.id_project = prj.id " +
                "INNER JOIN  " + Lystore.lystoreSchema + ".title AS tt ON tt.id = prj.id_title " +
                "INNER JOIN  " + Lystore.lystoreSchema + ".rel_group_campaign ON (ore.id_campaign = rel_group_campaign.id_campaign) " +
                "INNER JOIN  " + Lystore.lystoreSchema + ".rel_group_structure ON (ore.id_structure = rel_group_structure.id_structure) " +
                "LEFT OUTER JOIN  " + Lystore.lystoreSchema + ".order AS o ON (ore.id_order = o.id) " +
                "INNER JOIN  " + Lystore.lystoreSchema + ".structure_group ON (rel_group_structure.id_structure_group = structure_group.id " +
                "                                       AND rel_group_campaign.id_structure_group = structure_group.id) " +
                "WHERE ore.status = 'IN PROGRESS' AND ore.id = ? " +
                "GROUP BY (prj.preference, " +
                "          prj.id, " +
                "          ore.id, " +
                "          contract.id, " +
                "          ct.id, " +
                "          supplier.id, " +
                "          campaign.id, " +
                "          tt.id, " +
                "          o.order_number)";

        Sql.getInstance().prepared(query, new JsonArray().add(idOrder), SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void updateOperation(Integer idOperation, JsonArray idsOrders, Handler<Either<String, JsonObject>> handler) {
        String query = " UPDATE " + Lystore.lystoreSchema + ".\"order-region-equipment\" " +
                " SET id_operation = " +
                idOperation +
                " WHERE id IN " +
                Sql.listPrepared(idsOrders.getList()) +
                " RETURNING id";
        JsonArray values = new JsonArray();
        for (int i = 0; i < idsOrders.size(); i++) {
            values.add(idsOrders.getValue(i));
        }
        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }
}
