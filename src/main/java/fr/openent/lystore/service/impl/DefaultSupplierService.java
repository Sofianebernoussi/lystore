package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.SupplierService;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.SqlResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

public class DefaultSupplierService extends SqlCrudService implements SupplierService {

    public DefaultSupplierService(String schema, String table) {
        super(schema, table);
    }

    public void getSuppliers(Handler<Either<String, JsonArray>> handler) {
        super.list(handler);
    }

    public void createSupplier(JsonObject agent, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Lystore.LYSTORE_SCHEMA + ".supplier (email, address, name, phone) " +
                "VALUES (?, ?, ?, ?) RETURNING id;";

        JsonArray params = new JsonArray()
                .addString(agent.getString("email"))
                .addString(agent.getString("address"))
                .addString(agent.getString("name"))
                .addString(agent.getString("phone"));

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    public void updateSupplier(Integer id, JsonObject supplier, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Lystore.LYSTORE_SCHEMA + ".supplier " +
                "SET email = ?, address = ?, name = ?, phone = ? " +
                "WHERE id = ? RETURNING *;";

        JsonArray params = new JsonArray()
                .addString(supplier.getString("email"))
                .addString(supplier.getString("address"))
                .addString(supplier.getString("name"))
                .addString(supplier.getString("phone"))
                .addNumber(id);

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    public void deleteSupplier(List<Integer> ids, Handler<Either<String, JsonObject>> handler) {
        SqlUtils.deleteIds("supplier", ids, handler);
    }
}
