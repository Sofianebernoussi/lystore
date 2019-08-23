package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.helpers.FutureHelper;
import fr.openent.lystore.service.InstructionService;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.ArrayUtils;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.SqlResult;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;

import java.util.ArrayList;
import java.util.List;

public class DefaultInstructionService  extends SqlCrudService implements InstructionService {

    public DefaultInstructionService(String schema, String table) {
        super(schema, table);
    }
    private static final Logger LOGGER = LoggerFactory.getLogger (DefaultOrderService.class);

    public void getExercises (Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Lystore.lystoreSchema +".exercise";
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
                filter += "(LOWER(instruction.object) ~ LOWER(?) OR " +
                        "LOWER(instruction.service_number) ~ LOWER(?) OR " +
                        "LOWER(instruction.cp_number) ~ LOWER(?) OR " +
                        "LOWER(exercise.year) ~ LOWER(?)) ";
            }
        }
        return filter;
    }

    public void getInstructions(List<String> filters, Handler<Either<String, JsonArray>> handler){
        JsonArray params = new JsonArray();
        if (!filters.isEmpty()) {
            for (String filter : filters) {
                params.add(filter).add(filter).add(filter).add(filter);
            }
        }
        String query =  "SELECT instruction.*, " +
                "to_json(exercise.*) AS exercise, " +
                "array_to_json(array_agg( o.id )) AS operations " +
                "FROM  " + Lystore.lystoreSchema +".instruction " +
                "INNER JOIN " + Lystore.lystoreSchema +".exercise exercise ON exercise.id = instruction.id_exercise " +
                "LEFT JOIN " + Lystore.lystoreSchema +".operation o ON o.id_instruction = instruction.id " +
                getTextFilter(filters) +
                " GROUP BY (instruction.id, exercise.id);";

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(instructionsEither -> {
            try{
                if (instructionsEither.isRight()) {
                    JsonArray instructions = instructionsEither.right().getValue();
                    if(instructions.size() == 0){
                        handler.handle(new Either.Right<>(instructions));
                        return;
                    }
                    JsonArray idsInstructions = SqlQueryUtils.getArrayAllIdsResult(instructions);
                    Future<JsonArray> getSumOperationsFutur = Future.future();

                    List<Future> futursArray = new ArrayList<>();
                    futursArray.add(getSumOperationsFutur);

                    CompositeFuture.join( futursArray ).setHandler(asyncEvent -> {
                        if (asyncEvent.failed()) {
                            String message = "Failed to retrieve instructions";
                            handler.handle(new Either.Left<>(message));
                            return;
                        }

                        JsonArray getSumOperations = getSumOperationsFutur.result();
                        JsonArray instructionsResult =  SqlQueryUtils.addDataByIdJoin(instructions, getSumOperations,"amount");
                        handler.handle(new Either.Right<>(instructionsResult));
                    });

                    SqlUtils.getSumOperations(idsInstructions, FutureHelper.handlerJsonArray(getSumOperationsFutur));

                } else {
                    handler.handle(new Either.Left<>("404"));
                }
            } catch( Exception e){
                LOGGER.error("An error when you want get all instructions", e);
                handler.handle(new Either.Left<>(""));
            }
        }));
    }

    public void getOperationOfInstruction(Integer IdInstruction, Handler<Either<String, JsonArray>> handler) {
        JsonArray idInstructionParams = new JsonArray().add(IdInstruction);

        String queryOperation = "" +
                "SELECT operation.*,  " +
                "       to_json(label.*) AS label  " +
                "FROM " + Lystore.lystoreSchema +".operation  " +
                "INNER JOIN " + Lystore.lystoreSchema +".label_operation label ON label.id = operation.id_label  " +
                "WHERE id_instruction = ? " +
                "GROUP BY (operation.id,  " +
                "          label.*)";

        sql.getInstance().prepared(queryOperation, idInstructionParams, SqlResult.validResultHandler(eventOperation -> {
            try{
                if (eventOperation.isRight()) {
                    JsonArray operations = eventOperation.right().getValue();
                    if (operations.size() == 0) {
                        handler.handle(new Either.Right<>(operations));
                        return;
                    }
                    JsonArray idsOperations = SqlQueryUtils.getArrayAllIdsResult(operations);

                    Future<JsonArray> getCountOrderInOperationFuture = Future.future();
                    Future<JsonArray> getAllPriceOperationFuture = Future.future();
                    Future<JsonArray> getContractTypeOfOrderClientFuture = Future.future();
                    Future<JsonArray> getContractTypeOfOrderRegionFuture = Future.future();


                    CompositeFuture.all( getCountOrderInOperationFuture, getAllPriceOperationFuture, getContractTypeOfOrderClientFuture, getContractTypeOfOrderRegionFuture ).setHandler(asyncEvent -> {
                        if (asyncEvent.failed()) {
                            String message = "Failed to retrieve instructions";
                            handler.handle(new Either.Left<>(message));
                            return;
                        }

                        JsonArray operationsFinal = new JsonArray();
                        JsonArray getNbrOrder = getCountOrderInOperationFuture.result();
                        JsonArray getAmountsDemands = getAllPriceOperationFuture.result();
                        JsonArray getContractClient = getContractTypeOfOrderClientFuture.result();
                        JsonArray getContractRegion = getContractTypeOfOrderRegionFuture.result();

                        JsonObject mergeContractByIdOperation = new JsonObject();
                        JsonArray resultOfMerge = new JsonArray();

                        for (int i = 0 ; i<getContractClient.size() ; i++){
                            JsonObject contractTypeClient = getContractClient.getJsonObject(i);
                            for (int j = 0 ; j<getContractRegion.size() ; j++){
                                JsonObject contractTypeRegion = getContractRegion.getJsonObject(j);
                                if (contractTypeClient.getInteger("id").equals(contractTypeRegion.getInteger("id"))) {
                                    JsonArray temp;
                                    temp = SqlQueryUtils.mergeArraysInOne(new JsonArray(contractTypeRegion.getString("order_region_type_contract")), new JsonArray(contractTypeClient.getString("order_client_type_contract")));
                                    mergeContractByIdOperation.put("id", contractTypeClient.getInteger("id"))
                                            .put("order_contract_type", temp);
                                }
                            }
                            resultOfMerge.add(mergeContractByIdOperation);
                        }

                        for (int i = 0; i < operations.size(); i++) {
                            JsonObject operation = operations.getJsonObject(i);
                            for (int j = 0; j < getNbrOrder.size(); j++) {
                                JsonObject countOrders = getNbrOrder.getJsonObject(j);
                                if (operation.getInteger("id").equals(countOrders.getInteger("id"))) {
                                    operation.put("nb_orders", countOrders.getString("nb_orders"));
                                }
                            }
                            for (int j = 0; j < getAmountsDemands.size(); j++) {
                                JsonObject amountDemand = getAmountsDemands.getJsonObject(j);
                                if (operation.getInteger("id").equals(amountDemand.getInteger("id"))) {
                                    operation.put("amount", amountDemand.getString("amount"));
                                }
                            }
                            for (int k = 0; k < resultOfMerge.size(); k++) {
                                JsonObject contractArray = resultOfMerge.getJsonObject(k);
                                if (operation.getInteger("id").equals(contractArray.getInteger("id"))) {
                                    operation.put("order_contract_type", contractArray.getJsonArray("order_contract_type"));
                                }
                            }

                            operationsFinal.add(operation);
                        }
                        handler.handle(new Either.Right<>(operationsFinal));
                    });

                    SqlUtils.getCountOrderInOperation(idsOperations,  FutureHelper.handlerJsonArray(getCountOrderInOperationFuture));
                    SqlUtils.getAllPriceOperation(idsOperations,  FutureHelper.handlerJsonArray(getAllPriceOperationFuture));
                    getContractTypeOfOrderClient(idsOperations,  FutureHelper.handlerJsonArray(getContractTypeOfOrderClientFuture));
                    getContractTypeOfOrderRegion(idsOperations,  FutureHelper.handlerJsonArray(getContractTypeOfOrderRegionFuture));


                }
            } catch ( Exception e){
                LOGGER.error("An error when you want get all instructions", e);
                handler.handle(new Either.Left<>("404"));
            }

        }));
    }

    private void getContractTypeOfOrderClient (JsonArray idsOperations, Handler<Either<String, JsonArray>> handler ){
        String queryGetContractClient = "" +
                "SELECT o.id,  " +
                "       array_to_json(array_agg(ct.*)) AS order_client_type_contract  " +
                "FROM  " + Lystore.lystoreSchema +".operation o  " +
                "LEFT JOIN  " + Lystore.lystoreSchema +".order_client_equipment oce ON oce.id_operation = o.id  " +
                "AND oce.override_region IS FALSE  " +
                "AND oce.status = 'IN PROGRESS'  " +
                "LEFT JOIN  " + Lystore.lystoreSchema +".contract c_client ON c_client.id = oce.id_contract  " +
                "LEFT JOIN  " + Lystore.lystoreSchema +".contract_type ct ON ct.id = c_client.id_contract_type  " +
                "WHERE o.id IN " +
                Sql.listPrepared(idsOperations.getList()) + " " +
                "GROUP BY (o.id) ";

        Sql.getInstance().prepared(queryGetContractClient, idsOperations, SqlResult.validResultHandler(handler));
    }

    private void getContractTypeOfOrderRegion (JsonArray idsOperations, Handler<Either<String, JsonArray>> handler ){
        String queryGetContractRegion = "" +
                "SELECT o.id,  " +
                "       array_to_json(array_agg(ct.*)) AS order_region_type_contract  " +
                "FROM  " + Lystore.lystoreSchema +".operation o  " +
                "LEFT JOIN  " + Lystore.lystoreSchema +".\"order-region-equipment\" ore ON ore.id_operation = o.id  " +
                "LEFT JOIN  " + Lystore.lystoreSchema +".contract c_region ON c_region.id = ore.id_contract  " +
                "LEFT JOIN  " + Lystore.lystoreSchema +".contract_type ct ON ct.id = c_region.id_contract_type  " +
                "WHERE o.id IN " +
                Sql.listPrepared(idsOperations.getList()) + " " +
                "GROUP BY (o.id)";

        Sql.getInstance().prepared(queryGetContractRegion, idsOperations, SqlResult.validResultHandler(handler));
    }

    public void create(JsonObject instruction, Handler<Either<String, JsonObject>> handler){
        String query = "INSERT INTO " + Lystore.lystoreSchema +".instruction (" +
                "id_exercise," +
                "object, " +
                "service_number, " +
                "cp_number, " +
                "submitted_to_cp, " +
                "date_cp, " +
                "comment) " +
                "VALUES (? ,? ,? ,? ,? ,? ,? ) " +
                "RETURNING id; ";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(instruction.getInteger("id_exercise"))
                .add(instruction.getString("object"))
                .add(instruction.getString("service_number"))
                .add(instruction.getString("cp_number"))
                .add(instruction.getBoolean("submitted_to_cp"))
                .add(instruction.getString("date_cp"))
                .add(instruction.getString("comment"));

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }
    public  void updateInstruction(Integer id, JsonObject instruction, Handler<Either<String, JsonObject>> handler){
        String query = " UPDATE " + Lystore.lystoreSchema + ".instruction " +
                "SET " +
                "id_exercise = ? ," +
                "object = ? , " +
                "service_number = ? , " +
                "cp_number = ? , " +
                "submitted_to_cp = ? , " +
                "date_cp = ? , " +
                "comment = ? " +
                "WHERE id = ? " +
                "RETURNING id";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(instruction.getInteger("id_exercise"))
                .add(instruction.getString("object"))
                .add(instruction.getString("service_number"))
                .add(instruction.getString("cp_number"))
                .add(instruction.getBoolean("submitted_to_cp"))
                .add(instruction.getString("date_cp"))
                .add(instruction.getString("comment"))
                .add(id);
        sql.prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }

    public  void deleteInstruction(JsonArray instructionIds, Handler<Either<String, JsonObject>> handler){
        JsonArray values = new JsonArray();
        for (int i = 0; i < instructionIds.size(); i++) {
            values.add(instructionIds.getValue(i));
        }
        String query = "DELETE FROM " +
                Lystore.lystoreSchema +
                ".instruction " +
                "WHERE id IN " +
                Sql.listPrepared(instructionIds.getList()) +
                " RETURNING id";
        sql.prepared(query, values, SqlResult.validRowsResultHandler(handler));
    }
}
