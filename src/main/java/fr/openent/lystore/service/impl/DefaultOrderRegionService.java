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
        String checkQuery = "SELECT id AS id_order_region_equipment from " + Lystore.lystoreSchema + ".\"order-region-equipment\" where id_order_client_equipment = " + order.getInteger("id_order_client_equipment");
        sql.raw(checkQuery, SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                String query;
                if (!event.right().getValue().containsKey("id_order_region_equipment")) {
                    query = "INSERT INTO " + Lystore.lystoreSchema + ".\"order-region-equipment\" " +
                            "( price, amount, creation_date, owner_name, owner_id, name, " +
                            "  equipment_key, status,   comment,";
                    if (order.containsKey("rank")){
                        query += " rank,";
                    }
                    if (order.containsKey("id_operation")) {
                        query += " id_operation,";
                    }
                    query += "id_order_client_equipment) " +
                            "VALUES (";
                    if (order.containsKey("rank")) {
                        query += "?,";
                    }
                    if (order.containsKey("id_operation")) {
                        query += "?,";
                    }
                    query += " ?, ?, ?, ?, ?, ?, ?, ?,  ?, ?)";

                } else {
                    query = "UPDATE " + Lystore.lystoreSchema + ".\"order-region-equipment\" " +
                            " set price = ?, amount = ?, modification_date = ? , owner_name = ? , owner_id = ?, name = ?, " +
                            "  equipment_key = ?, cause_status = ? ,   comment = ?,";
                    if (order.containsKey("rank"))
                        query += " rank = ?, ";
                    if (order.containsKey("id_operation")) {
                        query += " id_operation = ?, ";
                    }
                    query += "id_order_client_equipment = ? " +
                            "WHERE id = " +
                            event.right().getValue().getInteger("id_order_region_equipment");
                }
                JsonArray params = new JsonArray()
                        .add(order.getInteger("price"))
                        .add(order.getInteger("amount"))
                        .add(order.getString("creation_date"))
                        .add(user.getUsername())
                        .add(user.getUserId())
                        .add(order.getString("name"))
                        .add(order.getInteger("equipment_key"))
                        .add("IN PROGRESS")
                        .add(order.getString("comment"));
                if (order.containsKey("rank")){
                    params.add(order.getInteger("rank"));
                }
                if (order.containsKey("id_operation")){
                    params.add(order.getInteger("id_operation"));
                }
                params.add(order.getInteger("id_order_client_equipment"));

                Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(eventNewOrderRegion -> {
                    if (eventNewOrderRegion.isRight()) {
                        String updateOrderRegion = "" +
                                "UPDATE  " + Lystore.lystoreSchema +".\"order-region-equipment\" AS ore " +
                                "SET id_campaign = oce.id_campaign, " +
                                "    id_structure = oce.id_structure, " +
                                "    name = oce.name, " +
                                "    summary = oce.summary, " +
                                "    description = oce.description, " +
                                "    image = oce.image, " +
                                "    technical_spec = oce.technical_spec, " +
                                "    status = oce.status, " +
                                "    id_contract = oce.id_contract, " +
                                "    cause_status = oce.cause_status, " +
                                "    number_validation = oce.number_validation, " +
                                "    id_order = oce.id_order, " +
                                "    id_project = oce.id_project " +
                                "FROM " +
                                "  (SELECT id, " +
                                "          id_campaign, " +
                                "          id_structure, " +
                                "          name, " +
                                "          summary, " +
                                "          description, " +
                                "          image, " +
                                "          technical_spec, " +
                                "          status, " +
                                "          id_contract, " +
                                "          cause_status, " +
                                "          number_validation, " +
                                "          id_order, " +
                                "          id_project " +
                                "   FROM  " + Lystore.lystoreSchema +".order_client_equipment " +
                                "   WHERE id = ? ) AS oce " +
                                "WHERE ore.id_order_client_equipment = oce.id";

                        JsonArray id_oce =  new JsonArray().add(order.getInteger("id_order_client_equipment"));
                        Sql.getInstance().prepared(updateOrderRegion, id_oce, SqlResult.validUniqueResultHandler(handler));
                    } else {
                        LOGGER.error( eventNewOrderRegion.left());
                        return;
                    }
                }));
            }
        }));


    }

    @Override
    public void linkOrderToOperation(Integer id_order_client_equipment, Integer id_operation, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new JsonArray();
        String query = " UPDATE " + Lystore.lystoreSchema + ".order_client_equipment " +
                "SET  " +
                "status = ? ,id_operation = ? " +
                "WHERE id = ? ";

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


}
