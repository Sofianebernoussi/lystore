package fr.openent.lystore.export;

import fr.openent.lystore.Lystore;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.storage.Storage;
import org.vertx.java.busmods.BusModBase;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static fr.openent.lystore.Lystore.CONFIG;
import static fr.openent.lystore.Lystore.STORAGE;

public class ExportWorker extends BusModBase implements Handler<Message<JsonObject>> {
    private Instruction instruction;
    private Storage storage;

    @Override
    public void start() {
        super.start();
        vertx.eventBus().localConsumer(ExportWorker.class.getSimpleName(), this);
        this.config = CONFIG;
        this.storage = STORAGE;
    }

    @Override
    public void handle(Message<JsonObject> event) {
        final String action = event.body().getString("action", "");
        String fileNamIn = "";
        if(event.body().containsKey("type")){
            fileNamIn = getDate() + event.body().getString("titleFile") + event.body().getString("type") + ".xlsx";
        } else {
            fileNamIn = getDate() + event.body().getString("titleFile") + ".xlsx";
        }
        switch (action) {
            case "exportEQU":
                exportEquipment(
                        event.body().getInteger("id"),
                        event.body().getString("type"),
                        fileNamIn,
                        event.body().getString("userId"));
                break;
            case "exportRME":
                exportRME(
                        event.body().getInteger("id"),
                        fileNamIn,
                        event.body().getString("userId"));
                break;
            case "exportNotificationCP":
                exportNotificationCp(event.body().getInteger("id"),
                        fileNamIn,
                        event.body().getString("userId"));
                break;
            case "exportPublipostage":
                exportPublipostage(event.body().getInteger("id"),
                        fileNamIn,
                        event.body().getString("userId"));
                break;
            default:
                logger.error("Invalid action in worker");
                break;

        }
    }

    private void exportNotificationCp(Integer instructionId, String titleFile, String userId) {
        this.instruction = new Instruction(instructionId);

        this.instruction.exportNotficationCp(event1 -> {
            if (event1.isLeft()) {
                logger.error("error when creating xlsx");
            } else {
                Buffer xlsx = event1.right().getValue();
                saveBuffer(userId, xlsx, titleFile);
            }
        });
    }

    private void exportPublipostage(Integer instructionId, String titleFile, String userId) {
        this.instruction = new Instruction(instructionId);

        this.instruction.exportPublipostage( file  -> {
            if (file .isLeft()) {
                logger.error("error when creating xlsx");
            } else {
                Buffer xlsx = file .right().getValue();
                saveBuffer(userId, xlsx, titleFile);
            }
        });
    }

    private String getDate() {
        java.util.Date date = Calendar.getInstance().getTime();
        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        return formatter.format(date);
    }

    private void exportRME(Integer instructionId, String titleFile, String userId) {
        this.instruction = new Instruction(instructionId);

        this.instruction.exportInvestissement(event -> {
            if (event.isLeft()) {
                logger.error("error when creating xlsx");
            } else {
                Buffer xlsx = event.right().getValue();
                saveBuffer(userId, xlsx, titleFile);
            }
        });
    }

    private void saveBuffer(String userId, Buffer xlsx, String fileName) {
        storage.writeBuffer(xlsx, "application/vnd.ms-excel", fileName, file -> {
            if (!"ok".equals(file.getString("status"))) {
                logger.error("An error occurred when inserting xlsx ");
            } else {
                logger.info("Xlsx insert in storage");
                saveFile(file.getString("_id"),
                        fileName,
                        userId);
            }
        });
    }

    private void exportEquipment(int instructionId, String type, String titleFile, String userId) {
        this.instruction = new Instruction(instructionId);
        this.instruction.exportEquipmentRapp(event1 -> {
            if (event1.isLeft()) {
                logger.error("error when creating xlsx");
            } else {
                Buffer xlsx = event1.right().getValue();
                saveBuffer(userId, xlsx, titleFile);
            }
        }, type);
    }

    private void saveFile(String fileId, String filename, String userId) {
        String query = "INSERT into " + Lystore.lystoreSchema + ".export (fileid, filename, ownerid) " +
                "VALUES (?, ?, ?) ;";
        JsonArray params = new JsonArray().add(fileId)
                .add(filename)
                .add(userId);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isLeft()) {
                    logger.error("Fail to insert file in SQL");
                }
            }
        }));

    }
}
