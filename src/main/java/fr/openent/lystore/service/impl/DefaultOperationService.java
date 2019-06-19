package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.OperationService;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
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
                "SUM(ROUND(oce.price + ((oce.price *  oce.tax_amount) /100), 2) * oce.amount ) AS amount," +
                " array_to_json(array_agg(c.name)) as contracts " +
                "FROM  " + Lystore.lystoreSchema +".operation "+
                "Inner join " + Lystore.lystoreSchema +".label_operation label on label.id = operation.id_label "+
                "Left join " + Lystore.lystoreSchema +".order_client_equipment oce on oce.id_operation = operation.id "+
                "left join " + Lystore.lystoreSchema +".order o on o.id = oce.id_order "+
                "left join " + Lystore.lystoreSchema +".contract c on c.id = oce.id_contract "+
                getTextFilter(filters) +
                "GROUP BY (operation.id, label.*)";
        sql.prepared(query, params, SqlResult.validResultHandler(handler) );
    }

    public void create(JsonObject operation, Handler<Either<String, JsonObject>> handler){
        String query = "INSERT INTO " +
                Lystore.lystoreSchema + ".operation(id_label, status) " +
                "VALUES (?, ?) RETURNING id;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(operation.getInteger("id_label"))
                .add(operation.getBoolean("status"));

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    public  void updateOperation(Integer id, JsonObject operation, Handler<Either<String, JsonObject>> handler){
        String query = "UPDATE " + Lystore.lystoreSchema + ".operation " +
                "SET id_label = ?, " +
                "status = ?, " +
                "id_instruction = ? " +
                "WHERE id = ? " +
                "RETURNING id";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray()
                .add(operation.getInteger("id_label"))
                .add(operation.getBoolean("status"))
                .add(operation.getInteger("id_instruction"))
                .add(id);
        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }

    public  void addInstructionId(Integer instructionId, JsonArray operationIds, Handler<Either<String, JsonObject>> handler){
        String query = " UPDATE " + Lystore.lystoreSchema + ".operation " +
                "SET status = true, " +
                "id_instruction = " +
                instructionId +
                " WHERE id IN " +
                Sql.listPrepared(operationIds.getList()) +
                " RETURNING id";
        JsonArray values = new JsonArray();
        for (int i = 0; i < operationIds.size(); i++) {
            values.add(operationIds.getValue(i));
        }
        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }

    public  void removeInstructionId( JsonArray operationIds, Handler<Either<String, JsonObject>> handler){
        String query = " UPDATE " + Lystore.lystoreSchema + ".operation " +
                "SET id_instruction = null " +
                " WHERE id IN " +
                Sql.listPrepared(operationIds.getList()) +
                " RETURNING id";
        JsonArray values = new JsonArray();
        for (int i = 0; i < operationIds.size(); i++) {
            values.add(operationIds.getValue(i));
        }
        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }

    public  void deleteOperation(JsonArray operationIds, Handler<Either<String, JsonObject>> handler){
        String query = "DELETE FROM " +
                Lystore.lystoreSchema +
                ".operation" + " WHERE id IN " +
                Sql.listPrepared(operationIds.getList()) +
                " RETURNING id";
        JsonArray values = new JsonArray();
        for (int i = 0; i < operationIds.size(); i++) {
            values.add(operationIds.getValue(i));
        }
        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void getOperationOrders(Integer operationId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT oce.*, contract.name as contract_name " +
                "FROM  " + Lystore.lystoreSchema + ".order_client_equipment oce  " +
                "INNER JOIN  " + Lystore.lystoreSchema + ".contract ON oce.id_contract = contract.id  " +
                "INNER JOIN  " + Lystore.lystoreSchema + ".operation ON (oce.id_operation = operation.id) " +
                "WHERE operation.id = ?  ";

        JsonArray params = new JsonArray()
                .add(operationId);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(event -> {
            if (event.isLeft()) {
                handler.handle(event.left());
                return;
            }

            JsonArray orders = event.right().getValue();
            JsonArray structures = new JsonArray();
            for (int i = 0; i < orders.size(); i++) {
                structures.add(orders.getJsonObject(i).getString("id_structure"));
            }

            String nQuery = "MATCH (s:Structure) WHERE s.id IN {structures} RETURN s.id as id, s.name as name, s.uai as uai";
            JsonObject nParams = new JsonObject()
                    .put("structures", structures);

            Neo4j.getInstance().execute(nQuery, nParams, Neo4jResult.validResultHandler(nEvent -> {
                if (nEvent.isLeft()) {
                    handler.handle(nEvent.left());
                    return;
                }

                JsonArray structureList = nEvent.right().getValue();
                JsonObject map = new JsonObject();
                for (int i = 0; i < structureList.size(); i++) {
                    map.put(structureList.getJsonObject(i).getString("id"), structureList.getJsonObject(i));
                }

                JsonObject order, structure;
                for (int i = 0; i < orders.size(); i++) {
                    order = orders.getJsonObject(i);
                    structure = map.getJsonObject(order.getString("id_structure"));
                    order.put("structure_name", structure.getString("name"));
                    order.put("structure_uai", structure.getString("uai"));
                }

                handler.handle(new Either.Right<>(orders));
            }));
        }));
    }
}
