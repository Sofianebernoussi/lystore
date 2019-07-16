package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.OrderRegionService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

public class DefaultOrderRegionService extends SqlCrudService implements OrderRegionService {

    public DefaultOrderRegionService(String table) {
        super(table);
    }

    @Override
    public void setOrderRegion(JsonObject order, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        String checkQuery = "SELECT count(*) as nb from " + Lystore.lystoreSchema + ".\"order-region-equipment\" where id_order_client_equipment = " + order.getInteger("id_order_client_equipment");
        sql.raw(checkQuery, SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                final int nb = event.right().getValue().getInteger("nb");
                String query;
                if (nb == 0) {
                    query = "INSERT INTO " + Lystore.lystoreSchema + ".\"order-region-equipment\" " +
                            "( price, amount, creation_date, owner_name, owner_id, name, " +
                            "  equipment_key, cause_status,   comment,";
                    if (order.containsKey("rank"))
                        query += " rank,";

                    query += "id_order_client_equipment) " +
                            "VALUES (";

                    if (order.containsKey("rank"))

                    {
                        query += "?,";
                    }
                    query += " ?, ?, ?, ?, ?, ?, ?, ?,  ?, ?)";

                } else {
                    query = "UPDATE " + Lystore.lystoreSchema + ".\"order-region-equipment\" " +
                            " set price = ?, amount = ?, creation_date = ? , owner_name = ? , owner_id = ?, name = ?, " +
                            "  equipment_key = ?, cause_status = ? ,   comment = ?,";
                    if (order.containsKey("rank"))
                        query += " rank = ?,";

                    query += "id_order_client_equipment = ? ";
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
                if (order.containsKey("rank"))
                    params.add(order.getInteger("rank"));

                params.add(order.getInteger("id_order_client_equipment"));

                Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
            }
        }));


    }

    @Override
    public void createOrderRegion(JsonObject order, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Lystore.lystoreSchema + ".\"order-region-equipment\" " +
                "( price, amount, creation_date, owner_name, owner_id, name, " +
                "  equipment_key, cause_status,   comment, id_campaign, id_structure, id_operation )" +
                " VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";

        JsonArray params = new JsonArray()
                .add(order.getInteger("price"))
                .add(order.getInteger("amount"))
                .add(order.getString("creation_date"))
                .add(user.getUsername())
                .add(user.getUserId())
                .add(order.getString("name"))
                .add(order.getInteger("equipment_key"))
                .add("IN PROGRESS")
                .add(order.getString("comment"))
                .add(order.getInteger("id_campaign"))
                .add(order.getString("id_structure"))
                .add(order.getInteger("id_operation"));

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
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
}
