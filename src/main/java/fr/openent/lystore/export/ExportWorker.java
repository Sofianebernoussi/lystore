package fr.openent.lystore.export;

import fr.openent.lystore.Lystore;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.collections.PersistantBuffer;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.storage.Storage;
import org.vertx.java.busmods.BusModBase;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static fr.openent.lystore.Lystore.CONFIG;
import static fr.openent.lystore.Lystore.STORAGE;

public class ExportWorker extends BusModBase implements Handler<Message<JsonObject>> {
    private final Map<String, PersistantBuffer> buffers = new HashMap<>();
    private final Map<String, MessageConsumer<byte[]>> consumers = new HashMap<>();
    private JsonObject sftpGarConfig = null;
    private JsonObject garRessourcesConfig = null;
    private EventBus eb = null;
    private JsonObject config;
    private Instruction instruction;
    private Storage storage;

    @Override
    public void start() {
        super.start();
        vertx.eventBus().localConsumer(ExportWorker.class.getSimpleName(), this);
        this.config = CONFIG;
        this.vertx = vertx;
        this.storage = STORAGE;
        this.sftpGarConfig = config.getJsonObject("gar-sftp");
        this.garRessourcesConfig = config.getJsonObject("gar-ressources");
    }

    @Override
    public void handle(Message<JsonObject> event) {
        final String action = event.body().getString("action", "");
        switch (action) {
            case "exportEQU":
                exportEquipment(
                        event.body().getInteger("id"),
                        event.body().getString("type"),
                        event.body().getString("userId"));
                break;
            default:
                logger.error("Invalid action in worker");
        }
    }

    private void exportEquipment(int instructionId, String type, String userId) {
        this.instruction = new Instruction(instructionId);

        this.instruction.exportEquipmentRapp(event1 -> {
            if (event1.isLeft()) {
                logger.error("error when creating csv");
            } else {
                java.util.Date date = Calendar.getInstance().getTime();
                DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
                String today = formatter.format(date);
                Buffer xlsx = event1.right().getValue();
                String fileName = today + "_EQUIPEMENT_RAPPORT" + ".xlsx";
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
