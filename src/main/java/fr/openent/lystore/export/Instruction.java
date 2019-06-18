package fr.openent.lystore.export;

import fr.openent.lystore.Lystore;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.buffer.impl.BufferImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Instruction {

    private Integer id;
    private JsonObject instruction;
    private Workbook workbook;

    public Instruction(Integer instructionId) {
        this.id = instructionId;
    }

    public void export(Handler<Either<String, Buffer>> handler) {
        if (this.id == null) {
            handler.handle(new Either.Left<>("Instruction identifier is not nullable"));
            return;
        }

        String query = "WITH operations AS (" +
                "SELECT operation.id, label_operation.label, operation.id_instruction " +
                "FROM " + Lystore.lystoreSchema + ".operation " +
                "INNER JOIN " + Lystore.lystoreSchema + ".label_operation ON (operation.id_label = label_operation.id) " +
                "WHERE id_instruction = ? " +
                ")" +
                "SELECT instruction.*, array_to_json(array_agg(operations)) as operations " +
                "FROM " + Lystore.lystoreSchema + ".instruction " +
                "INNER JOIN operations ON (operations.id_instruction = instruction.id) " +
                "WHERE instruction.id = ? " +
                "GROUP BY instruction.id";

        Sql.getInstance().prepared(query, new JsonArray().add(this.id).add(this.id), SqlResult.validUniqueResultHandler(either -> {
            if (either.isLeft()) {
                handler.handle(new Either.Left<>(either.left().getValue()));
                return;
            }


            instruction = either.right().getValue();
            instruction.put("operations", new JsonArray(instruction.getString("operations")));

            Workbook workbook = new HSSFWorkbook();
            this.workbook = workbook;
            List<Future> futures = new ArrayList<>();
            Future<Boolean> lyceeFuture = Future.future();
            futures.add(lyceeFuture);
            CompositeFuture.all(futures).setHandler(event -> {
                if (event.succeeded()) {
                    try {
                        ByteArrayOutputStream fileOut = new ByteArrayOutputStream();
                        workbook.write(fileOut);
                        Buffer buff = new BufferImpl();
                        buff.appendBytes(fileOut.toByteArray());
                        handler.handle(new Either.Right<>(buff));
                    } catch (IOException e) {
                        handler.handle(new Either.Left<>(e.getMessage()));
                    }
                } else {
                    handler.handle(new Either.Left<>(event.cause().toString()));
                }
            });

            new LyceeTab(workbook, instruction).create(getHandler(lyceeFuture));
        }));
    }

    private Handler<Either<String, Boolean>> getHandler(Future<Boolean> future) {
        return event -> {
            if (event.isRight()) {
                future.complete(event.right().getValue());
            } else {
                future.fail(event.left().getValue());
            }
        };
    }
}
