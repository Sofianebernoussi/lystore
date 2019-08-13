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

        String queryOperation = "SELECT " +
                "operation.*, " +
                "to_json(label_operation.*) AS label " +
                "FROM " + Lystore.lystoreSchema +".operation " +
                "INNER JOIN " + Lystore.lystoreSchema +".label_operation ON operation.id_label = label_operation.id " +
                "LEFT JOIN " + Lystore.lystoreSchema +".order_client_equipment oce ON oce.id_operation = operation.id " +
                "WHERE id_instruction = ? " +
                "GROUP BY (operation.id, label_operation.id)";

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

                    CompositeFuture.all( getCountOrderInOperationFuture, getAllPriceOperationFuture ).setHandler(asyncEvent -> {
                        if (asyncEvent.failed()) {
                            String message = "Failed to retrieve instructions";
                            handler.handle(new Either.Left<>(message));
                            return;
                        }
                        JsonArray operationsFinal = new JsonArray();
                        JsonArray getNbrOrder = getCountOrderInOperationFuture.result();
                        JsonArray getAmountsDemands = getAllPriceOperationFuture.result();

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
                            operationsFinal.add(operation);
                        }
                        handler.handle(new Either.Right<>(operationsFinal));
                    });

                    SqlUtils.getCountOrderInOperation(idsOperations,  FutureHelper.handlerJsonArray(getCountOrderInOperationFuture));
                    SqlUtils.getAllPriceOperation(idsOperations,  FutureHelper.handlerJsonArray(getAllPriceOperationFuture));

                }
            } catch ( Exception e){
                LOGGER.error("An error when you want get all instructions", e);
                handler.handle(new Either.Left<>("404"));
            }

        }));
    }

    private void getAmountDemandOperation(JsonArray IdInstructions, Handler<Either<String, JsonArray>> handler) {



        String queryAmount = "WITH value_instruction AS " +
                "( " +
                "SELECT " +
                "instruction.id, " +
                "operation.id AS operation_id,  " +
                "( " +
                "WITH value_operation AS " +
                "( " +
                "SELECT " +
                "( " +
                "SELECT " +
                "SUM(ROUND(oco.price + ((oco.price *  oco.tax_amount) /100), 2) * oco.amount ) AS price_total_option " +
                "FROM " + Lystore.lystoreSchema +".order_client_options oco WHERE id_order_client_equipment = oce.id ), " +
                "oce.id, " +
                "ore.id, " +
                "CASE " +
                "WHEN ore.price is not null THEN SUM( ore.price * ore.amount ) " +
                "WHEN oce.price_proposal is not NULL THEN SUM( oce.price_proposal * oce.amount) " +
                "ELSE SUM(ROUND(oce.price + ((oce.price *  oce.tax_amount) /100), 2) * oce.amount ) " +
                "END AS price_total_operation " +
                "FROM " + Lystore.lystoreSchema +".order_client_equipment oce " +
                "FULL JOIN " + Lystore.lystoreSchema +".\"order-region-equipment\" ore on oce.id = ore.id_order_client_equipment " +
                "WHERE oce.id_operation = operation.id OR ore.id_operation = operation.id " +
                "GROUP BY (oce.id, ore.id ) " +
                ") " +
                "SELECT (SUM(price_total_operation) + SUM(price_total_option)) AS price_total_operation FROM value_operation " +
                ") " +
                "FROM " + Lystore.lystoreSchema +".instruction  " +
                "INNER JOIN " + Lystore.lystoreSchema +".operation ON (instruction.id = operation.id_instruction)  " +
                "INNER JOIN " + Lystore.lystoreSchema +".order_client_equipment oce ON (oce.id_operation = operation.id)  " +
                "WHERE instruction.id IN  " +
                Sql.listPrepared(IdInstructions.getList()) + " " +
                "GROUP BY (instruction.id, operation_id ) " +
                ") " +
                "SELECT  " +
                "value_instruction.id,  " +
                "SUM (price_total_operation) AS amount  " +
                "FROM value_instruction " +
                "GROUP BY (value_instruction.id)";

        Sql.getInstance().prepared(queryAmount, IdInstructions, SqlResult.validResultHandler(handler));
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
