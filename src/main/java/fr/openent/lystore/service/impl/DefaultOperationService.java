package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.helpers.FutureHelper;
import fr.openent.lystore.service.OperationService;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import static fr.openent.lystore.service.impl.SqlUtils.getAllPriceOperation;

public class DefaultOperationService extends SqlCrudService implements OperationService {

    public DefaultOperationService(String schema, String table) {
        super(schema, table);
    }
    private static final Logger LOGGER = LoggerFactory.getLogger (DefaultOrderService.class);
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

        String queryOperation = "SELECT " +
                "operation.* , " +
                "to_json(label.*) as label, " +
                "count(oce.*) as nbr_sub, " +
                "array_to_json(array_agg(o.order_number)) as bc_number, " +
                "array_to_json(array_agg(o.label_program)) as programs, " +
                "array_to_json(array_agg(c.name)) as contracts " +
                "FROM  " + Lystore.lystoreSchema +".operation "+
                "INNER JOIN " + Lystore.lystoreSchema +".label_operation label on label.id = operation.id_label "+
                "LEFT JOIN " + Lystore.lystoreSchema +".order_client_equipment oce on oce.id_operation = operation.id "+
                "LEFT JOIN " + Lystore.lystoreSchema +".order o on o.id = oce.id_order "+
                "LEFT JOIN " + Lystore.lystoreSchema +".contract c on c.id = oce.id_contract " +
                getTextFilter(filters) +
                " GROUP BY (operation.id, label.*)";

        Sql.getInstance().prepared(queryOperation, params, SqlResult.validResultHandler(operationsEither -> {
            try {
                if (operationsEither.isRight()) {
                    JsonArray operations = operationsEither.right().getValue();
                    if (operations.size() == 0) {
                        handler.handle(new Either.Right<>(operations));
                        return;
                    }
                    JsonArray idsOperations = SqlQueryUtils.getArrayAllIdsResult(operations);

                    Future<JsonArray> getCountOrderInOperationFuture = Future.future();
                    Future<JsonArray> getInstructionForOperationFuture = Future.future();
                    Future<JsonArray> getAllPriceOperationFuture = Future.future();

                    CompositeFuture.all(getCountOrderInOperationFuture, getInstructionForOperationFuture, getAllPriceOperationFuture).setHandler(asyncEvent -> {
                        if (asyncEvent.failed()) {
                            String message = "Failed to retrieve operation";
                            handler.handle(new Either.Left<>(message));
                            return;
                        }

                        JsonArray getOrderCount = getCountOrderInOperationFuture.result();
                        JsonArray getInstruction = getInstructionForOperationFuture.result();
                        JsonArray getSumPriceOperation = getAllPriceOperationFuture.result();
                        JsonArray operationFinalSend = new JsonArray();
                        for (int i = 0; i < operations.size(); i++) {
                            JsonObject operation = operations.getJsonObject(i);
                            for (int j = 0; j < getOrderCount.size(); j++) {
                                JsonObject countOrders = getOrderCount.getJsonObject(j);
                                if (operation.getInteger("id").equals(countOrders.getInteger("id"))) {
                                    operation.put("nbr_sub", countOrders.getString("nbr_sub"));
                                }
                            }
                            for (int k = 0; k < getInstruction.size(); k++) {
                                JsonObject instruction = getInstruction.getJsonObject(k);
                                if (operation.getInteger("id").equals(instruction.getInteger("id_operation"))) {
                                    operation.put("instruction", instruction.getString("instruction"));
                                }
                            }
                            for (int j = 0; j < getSumPriceOperation.size(); j++) {
                                JsonObject sumPriceOperation = getSumPriceOperation.getJsonObject(j);
                                if (operation.getInteger("id").equals(sumPriceOperation.getInteger("id"))) {
                                    operation.put("amount", sumPriceOperation.getString("amount"));
                                }
                            }
                            operationFinalSend.add(operation);
                        }
                        handler.handle(new Either.Right<>(operationFinalSend));
                    });

                    SqlUtils.getCountOrderInOperation(idsOperations, FutureHelper.handlerJsonArray(getCountOrderInOperationFuture));
                    getInstructionForOperation(idsOperations, FutureHelper.handlerJsonArray(getInstructionForOperationFuture));
                    getAllPriceOperation(idsOperations, FutureHelper.handlerJsonArray(getAllPriceOperationFuture));


                } else {
                    handler.handle(new Either.Left<>("404"));
                }
            } catch( Exception e){
                LOGGER.error("An error when you want get all operation", e);
                handler.handle(new Either.Left<>(""));
            }
        }));
    }



    private void getInstructionForOperation(JsonArray idsOperations, Handler<Either<String, JsonArray>> handler){
        String queryGetTotalOperation = "SELECT " +
                "o.id AS id_operation, " +
                "to_json(i.*) AS instruction " +
                "FROM " + Lystore.lystoreSchema + ".instruction AS i " +
                "LEFT JOIN " + Lystore.lystoreSchema + ".operation o on i.id = o.id_instruction " +
                "WHERE o.id IN " +
                Sql.listPrepared(idsOperations.getList());

        Sql.getInstance().prepared(queryGetTotalOperation, idsOperations, SqlResult.validResultHandler(handler));
    }


    public void create(JsonObject operation, Handler<Either<String, JsonObject>> handler){
        String query = "INSERT INTO " +
                Lystore.lystoreSchema + ".operation(id_label, status, date_cp) " +
                "VALUES (?, ?, ?) RETURNING id;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(operation.getInteger("id_label"))
                .add(operation.getBoolean("status"))
                .add(operation.getString("date_cp"));

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    public  void updateOperation(Integer id, JsonObject operation, Handler<Either<String, JsonObject>> handler){
        String query = "UPDATE " + Lystore.lystoreSchema + ".operation " +
                "SET id_label = ?, " +
                "status = ?, " +
                "id_instruction = ?, " +
                "date_cp = ? " +
                "WHERE id = ? " +
                "RETURNING id";
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray()
                .add(operation.getInteger("id_label"))
                .add(operation.getBoolean("status"))
                .add(operation.getInteger("id_instruction"))
                .add(operation.getString("date_cp"))
                .add(id);
        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }

    public  void addInstructionId(Integer instructionId, JsonArray operationIds, Handler<Either<String, JsonObject>> handler){
        String query = " UPDATE " + Lystore.lystoreSchema + ".operation " +
                "SET id_instruction = " +
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

        Future<JsonArray> getOrderRegionByOperationFuture = Future.future();
        Future<JsonArray> getOrderClientByOperationFuture = Future.future();

        getOrderRegionByOperation(operationId, FutureHelper.handlerJsonArray(getOrderRegionByOperationFuture));
        getOrderClientByOperation(operationId, FutureHelper.handlerJsonArray(getOrderClientByOperationFuture));

        CompositeFuture.all( getOrderRegionByOperationFuture, getOrderClientByOperationFuture).setHandler(asyncEvent -> {
            if (asyncEvent.failed()) {
                String message = "Failed to retrieve order of operation";
                handler.handle(new Either.Left<>(message));
                return;
            }

            JsonArray ordersRegionsByOperation = getOrderRegionByOperationFuture.result();
            JsonArray ordersClientsByOperation = getOrderClientByOperationFuture.result();

            for (int i = 0 ; i<ordersClientsByOperation.size() ; i++){
                ordersClientsByOperation.getJsonObject(i).put("isOrderRegion", false);
            }

            for (int i = 0 ; i<ordersRegionsByOperation.size() ; i++){
                ordersClientsByOperation.add(ordersRegionsByOperation.getJsonObject(i).put("isOrderRegion", true));
            }

            handler.handle(new Either.Right<>(ordersClientsByOperation));
        });
    }
    private void getOrderRegionByOperation(int idOperation, Handler<Either<String, JsonArray>> handler){
        String queryGetOrderRegion = "" +
                "SELECT ore.id, " +
                "       ore.id_order_client_equipment, " +
                "       ore.creation_date, " +
                "       ore.amount, " +
                "       ore.name, " +
                "       ore.id_structure, " +
                "       ore.status, " +
                "     ROUND( ore.price * ore.amount, 2 ) AS price, " +
                "       c.name AS contract_name " +
                "FROM  " + Lystore.lystoreSchema +".\"order-region-equipment\" ore " +
                "INNER JOIN  " + Lystore.lystoreSchema +".contract c ON ore.id_contract = c.id " +
                "INNER JOIN  " + Lystore.lystoreSchema +".operation o ON (ore.id_operation = o.id) " +
                "WHERE o.id = ? " +
                "GROUP BY (ore.id, " +
                "          ore.price, " +
                "          ore.name, " +
                "          ore.id_structure, " +
                "          c.name);";

        Sql.getInstance().prepared(queryGetOrderRegion, new JsonArray().add(idOperation), SqlResult.validResultHandler(handler));
    }
    private void getOrderClientByOperation(int idOperation, Handler<Either<String, JsonArray>> handler){
        String queryGOrderClient = "" +
                "SELECT oce.id,  " +
                "       (  " +
                "               (SELECT " +
                "                  CASE " +
                "                  WHEN ROUND(SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount), 2)  IS NULL THEN 0 " +
                "                  WHEN oce.price_proposal IS NOT NULL THEN 0 " +
                "                  ELSE  ROUND(SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount), 2) " +
                "                  END " +
                "               FROM " + Lystore.lystoreSchema +".order_client_options oco " +
                "               WHERE id_order_client_equipment = oce.id) + " +
                "                                                         (CASE  " +
                "                                                             WHEN oce.price_proposal IS NOT NULL THEN (oce.price_proposal)  " +
                "                                                             ELSE (ROUND(oce.price + ((oce.price * oce.tax_amount) /100), 2))  " +
                "                                                         END))  * oce.amount AS price,  " +
                "       oce.creation_date,  " +
                "       oce.amount,  " +
                "       oce.name,  " +
                "       oce.id_structure,  " +
                "       oce.status,  " +
                "       c.name AS contract_name  " +
                "FROM   " + Lystore.lystoreSchema +".order_client_equipment oce  " +
                "INNER JOIN   " + Lystore.lystoreSchema +".contract c ON oce.id_contract = c.id  " +
                "INNER JOIN   " + Lystore.lystoreSchema +".operation o ON (oce.id_operation = o.id)  " +
                "WHERE oce.status = 'IN PROGRESS' " +
                "  AND o.id = ? " +
                "  AND oce.override_region IS FALSE  " +
                "GROUP BY (oce.id,  " +
                "          oce.price,  " +
                "          oce.name,  " +
                "          oce.id_structure,  " +
                "          c.name);";

        Sql.getInstance().prepared(queryGOrderClient, new JsonArray().add(idOperation), SqlResult.validResultHandler(handler));
    }

}
