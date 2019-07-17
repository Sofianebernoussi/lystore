package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.helpers.FutureHelper;
import fr.openent.lystore.service.InstructionService;
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
                "to_json(exercise.*) AS exercise " +
                "FROM  " + Lystore.lystoreSchema +".instruction " +
                "INNER JOIN " + Lystore.lystoreSchema +".exercise exercise ON exercise.id = instruction.id_exercise " +
                getTextFilter(filters);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(instructionsEither -> {
            try{
                if (instructionsEither.isRight()) {
                    JsonArray instructions = instructionsEither.right().getValue();
                    if(instructions.size() == 0){
                        handler.handle(new Either.Right<>(instructions));
                        return;
                    }
                    JsonArray idsInstructions = new JsonArray();
                    for (int i = 0; i < instructions.size(); i++) {
                        JsonObject instruction = instructions.getJsonObject(i);
                        idsInstructions.add(instruction.getInteger("id"));
                    }

                    Future<JsonArray> operationsWithIdInstructionFuture = Future.future();
                    Future<JsonArray> amountDemandOperationFuture = Future.future();

                    CompositeFuture.all(operationsWithIdInstructionFuture, amountDemandOperationFuture).setHandler(asyncEvent -> {
                        if (asyncEvent.failed()) {
                            String message = "Failed to retrieve instructions";
                            handler.handle(new Either.Left<>(message));
                            return;
                        }

                        JsonArray getOperations = operationsWithIdInstructionFuture.result();
                        JsonArray getAmountsDemands = amountDemandOperationFuture.result();

                        JsonArray getInstructions = new JsonArray();
                        for (int i = 0; i < instructions.size(); i++) {
                            JsonObject instruction = instructions.getJsonObject(i);
                            for (int j = 0; j < getAmountsDemands.size(); j++){
                                JsonObject amountDemand = getAmountsDemands.getJsonObject(j);
                                if(instruction.getInteger("id").equals(amountDemand.getInteger("id"))){
                                    instruction.put("amount", amountDemand.getString("amount"));
                                }
                            }
                            JsonArray operations = new JsonArray();
                            for (int k = 0; k < getOperations.size(); k++){
                                JsonObject operation = getOperations.getJsonObject(k);
                                if(instruction.getInteger("id").equals(operation.getInteger("id_instruction"))){
                                    operations.add(operation);
                                }
                            }
                            instruction.put("operations", operations);
                            getInstructions.add(instruction);
                        }
                        handler.handle(new Either.Right<>(getInstructions));
                    });

                    getOperationsWithIdInstruction(idsInstructions, FutureHelper.handlerJsonArray(operationsWithIdInstructionFuture));
                    getAmountDemandOperation(idsInstructions, FutureHelper.handlerJsonArray(amountDemandOperationFuture));

                } else {
                    handler.handle(new Either.Left<>("404"));
                }
            } catch( Exception e){
                LOGGER.error("An error when you want get all instructions", e);
                handler.handle(new Either.Left<>(""));
            }
        }));
    }

    private void getOperationsWithIdInstruction(JsonArray IdInstructions, Handler<Either<String, JsonArray>> handler) {
        String queryOperation = "SELECT operation.*, " +
                "to_json(label_operation.*) AS label, " +
                "count(oce.*) AS nbr_sub, " +
                "( " +
                "WITH value AS  " +
                "( " +
                "SELECT " +
                "CASE   " +
                "WHEN ore.price is not null THEN ( ore.price * ore.amount ) " +
                "WHEN oce.price_proposal is not NULL THEN ( oce.price_proposal * oce.amount ) " +
                "ELSE (ROUND(oce.price + ((oce.price *  oce.tax_amount) /100), 2) * oce.amount ) " +
                "END AS price_total " +
                "FROM  lystore.order_client_equipment oce " +
                "FULL JOIN  lystore.\"order-region-equipment\" ore on oce.id = ore.id_order_client_equipment  " +
                "WHERE oce.id_operation = operation.id  " +
                ") " +
                "SELECT SUM(price_total) AS amount FROM value " +
                ")" +
                "FROM " + Lystore.lystoreSchema +".operation " +
                "INNER JOIN lystore.label_operation ON operation.id_label = label_operation.id " +
                "LEFT JOIN " + Lystore.lystoreSchema +".order_client_equipment oce ON oce.id_operation = operation.id "+
                "WHERE id_instruction IN " +
                Sql.listPrepared(IdInstructions.getList()) +
                " GROUP BY (operation.id, label_operation.id)";

        Sql.getInstance().prepared(queryOperation, IdInstructions, SqlResult.validResultHandler(handler));
    }

    private void getAmountDemandOperation(JsonArray IdInstructions, Handler<Either<String, JsonArray>> handler) {
        String queryAmount = "WITH valueFinal AS " +
                "( " +
                "SELECT " +
                "instruction.id, " +
                "operation.id AS operation_id,  " +
                "( " +
                "WITH value AS  " +
                "( " +
                "SELECT " +
                "CASE   " +
                "WHEN ore.price is not null THEN ( ore.price * ore.amount ) " +
                "WHEN oce.price_proposal is not NULL THEN ( oce.price_proposal * oce.amount ) " +
                "ELSE (ROUND(oce.price + ((oce.price *  oce.tax_amount) /100), 2) * oce.amount ) " +
                "END AS price_total " +
                "FROM   " + Lystore.lystoreSchema +".order_client_equipment oce " +
                "FULL JOIN   " + Lystore.lystoreSchema +".\"order-region-equipment\" ore on oce.id = ore.id_order_client_equipment  " +
                "WHERE oce.id_operation = operation.id  " +
                ") " +
                "SELECT SUM(price_total) AS amountTempo FROM value " +
                ") " +
                "FROM  " + Lystore.lystoreSchema +".instruction  " +
                "INNER JOIN  " + Lystore.lystoreSchema +".operation ON (instruction.id = operation.id_instruction)  " +
                "INNER JOIN  " + Lystore.lystoreSchema +".order_client_equipment oce ON (oce.id_operation = operation.id)  " +
                "WHERE instruction.id IN " +
                Sql.listPrepared(IdInstructions.getList()) + "  " +
                "GROUP BY (instruction.id, operation_id ) " +
                ") " +
                "SELECT valueFinal.id, SUM (amountTempo) AS amount FROM valueFinal " +
                "GROUP BY (valueFinal.id)";


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
