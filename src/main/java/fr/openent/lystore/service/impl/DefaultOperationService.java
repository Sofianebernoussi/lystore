package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.OperationService;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.sql.Sql;

import java.util.List;

public class DefaultOperationService extends SqlCrudService implements OperationService {

    public DefaultOperationService(String schema, String table) {
        super(schema, table);
    }

    public void getLabels (Handler<Either<String, JsonArray>> handler) {

        String query = "SELECT * FROM " + Lystore.lystoreSchema +".label_operation";
        sql.prepared(query, new JsonArray(), SqlResult.validResultHandler(handler) );
    }

    private String getTextFilter(List<String> filters) {
        String filter = "";
        if (filters.size() > 0) {
            filter = "WHERE ";
            for (int i = 0; i < filters.size(); i++) {
                if (i > 0) {
                    filter += "AND ";
                }

                filter += "(LOWER(label.label) ~ LOWER(?) OR LOWER(o.order_number) ~ LOWER(?)) ";
            }
        }

        return filter;
    }

    public void getOperations(List<String> filters, Handler<Either<String, JsonArray>> handler){
        JsonArray params = new JsonArray();
        if (!filters.isEmpty()) {
            for (String filter : filters) {
                params.add(filter).add(filter);
            }
        }
        String query =  "SELECT  operation.* , to_json(label.*) as label, count(oce.*) as nbr_sub, " +
                "array_to_json(array_agg(o.order_number)) as bc_number, " +
                "array_to_json(array_agg(o.label_program)) as programs," +
                "SUM(ROUND(oce.price + ((oce.price *  oce.tax_amount) /100), 2)) AS amount," +
                " array_to_json(array_agg(c.name)) as contracts " +
                "FROM  " + Lystore.lystoreSchema +".operation "+
                "Inner join " + Lystore.lystoreSchema +".label_operation label on label.id = operation.id_label "+
                "Left join " + Lystore.lystoreSchema +".order_client_equipment oce on oce.id_operation = operation.id "+
                "left join " + Lystore.lystoreSchema +".order o on o.id = oce.id_order "+
                "left join " + Lystore.lystoreSchema +".contract c on c.id = oce.id_contract "+
                getTextFilter(filters) +
                "group By (operation.id, label.*)";
        sql.prepared(query, params, SqlResult.validResultHandler(handler) );
    }

    public void create(JsonObject operation, Handler<Either<String, JsonObject>> handler){
        String query = "INSERT INTO " + Lystore.lystoreSchema + ".operation(id_label, status) " +
                "VALUES (?, ?) RETURNING id;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(operation.getInteger("id_label"))
                .add(operation.getBoolean("status"));

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }
    public  void updateOperation(Integer id, JsonObject operation, Handler<Either<String, JsonObject>> handler){
        String query = " UPDATE " + Lystore.lystoreSchema + ".operation " +
                "SET id_label = ?, " +
                "status = ? " +
                " WHERE id = ?;";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray()
                .add(operation.getInteger("id_label"))
                .add(operation.getBoolean("status"))
                .add(id);
        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }

    public  void deleteOperation(JsonArray operationIds, Handler<Either<String, JsonObject>> handler){
        JsonArray values = new JsonArray();
        for (int i = 0; i < operationIds.size(); i++) {
            values.add(operationIds.getValue(i));
        }
        String query = "DELETE FROM " + Lystore.lystoreSchema + ".operation" + " WHERE id IN " + Sql.listPrepared(operationIds.getList());
        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }
}
