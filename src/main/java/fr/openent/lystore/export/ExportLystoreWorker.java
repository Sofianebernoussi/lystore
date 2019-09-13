package fr.openent.lystore.export;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.helpers.ExcelHelper;
import fr.openent.lystore.helpers.MongoHelper;
import fr.openent.lystore.service.ExportService;
import fr.openent.lystore.service.impl.DefaultExportServiceService;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.storage.Storage;
import org.vertx.java.busmods.BusModBase;

import javax.print.DocFlavor;
import java.util.Queue;
import java.util.Timer;
import java.util.concurrent.ConcurrentLinkedQueue;

import static fr.openent.lystore.Lystore.CONFIG;
import static fr.openent.lystore.Lystore.STORAGE;

public class ExportLystoreWorker extends BusModBase implements Handler<Message<JsonObject>> {
    private Instruction instruction;
    private Storage storage;
    private ExportService exportService = new DefaultExportServiceService(Lystore.lystoreSchema, "export", storage);
    private String idNewFile;
    private boolean isWorking = false;

    @Override
    public void start() {
        super.start();
        vertx.eventBus().localConsumer(ExportLystoreWorker.class.getSimpleName(), this);
        this.config = CONFIG;
        this.storage = STORAGE;
        this.vertx = vertx;

    }

    @Override
    public void handle(Message<JsonObject> event) {
        if(!isWorking){
            logger.info("Calling Worker");
            isWorking = true;
            processExport();
        }
    }




    private void processExport(){

        Handler<Either<String,Boolean>> exportHandler = event -> {
                logger.info("exportHandler");
                if (event.isRight()) {
                    logger.info("export to Waiting");
                    processExport();
                } else {
                    ExcelHelper.catchError(exportService, idNewFile, "error when creating xlsx " + event.left().getValue());
                }
            };
            exportService.getWaitingExport(new Handler<Either<String, JsonObject>>() {
                @Override
                public void handle(Either<String, JsonObject> event) {
                    if(event.isRight()){
                        JsonObject waitingOrder = event.right().getValue();
                        chooseExport( waitingOrder,exportHandler);
                    }else{
                        logger.info("no more waiting");
                        new java.util.Timer().schedule(
                                new java.util.TimerTask() {
                                    @Override
                                    public void run() {
                                        processExport();
                                    }
                                },
                                1000
                        );
                    }
                }
            });
    }

    private void chooseExport(JsonObject body, Handler<Either<String, Boolean>> exportHandler) {
        final String action = body.getString("action", "");
        String fileNamIn = body.getString("filename");
        idNewFile = body.getString("_id");
        logger.info(idNewFile);
        Integer instruction_id = -1;
        try {
            instruction_id = Integer.parseInt(body.getString("instruction_id"));
        }catch (ClassCastException ce){
            instruction_id =body.getInteger("instruction_id");
        }
        switch (action) {
            case "exportEQU":
                exportEquipment(
                        instruction_id,
                        body.getString("type"),
                        fileNamIn,exportHandler );
                break;
            case "exportRME":
                exportRME(
                        instruction_id,
                        fileNamIn,
                        exportHandler);
                break;
            case "exportNotificationCP":
                exportNotificationCp(
                        instruction_id,
                        fileNamIn,
                        exportHandler);
                break;
            case "exportPublipostage":
                exportPublipostage(
                        instruction_id,
                        fileNamIn,
                        exportHandler);
                break;
            case "exportSubvention":
                exportSubvention(
                        instruction_id,
                        fileNamIn,
                        exportHandler);
                break;
            default:
                ExcelHelper.catchError(exportService, idNewFile, "Invalid action in worker",exportHandler);
                break;
        }
    }


    private void exportNotificationCp(Integer instructionId, String titleFile, Handler<Either<String, Boolean>> handler) {
        logger.info("Export NotificationCP started");

        this.instruction = new Instruction(exportService, idNewFile, instructionId);
        this.instruction.exportNotficationCp(event1 -> {
            if (event1.isLeft()) {
                ExcelHelper.catchError(exportService, idNewFile, "error when creating xlsx" + event1.left(),handler);
            } else {
                logger.info("Export NotificationCP ended");

                Buffer xlsx = event1.right().getValue();
                saveBuffer(xlsx, titleFile,handler);

            }
        });
    }

    private void exportSubvention(Integer instructionId, String titleFile, Handler<Either<String, Boolean>> handler) {
        logger.info("Export Subvention started");

        this.instruction = new Instruction(exportService, idNewFile, instructionId);
        this.instruction.exportSubvention(event1 -> {
            if (event1.isLeft()) {
                ExcelHelper.catchError(exportService, idNewFile, "error when creating xlsx" + event1.left(),handler);
            } else {
                logger.info("Export Subvention ended");

                Buffer xlsx = event1.right().getValue();
                saveBuffer(xlsx, titleFile,handler);

            }
        });
    }

    private void exportPublipostage(Integer instructionId, String titleFile, Handler<Either<String, Boolean>> handler) {
        logger.info("Export Publipostage started");

        this.instruction = new Instruction(exportService, idNewFile, instructionId);
        this.instruction.exportPublipostage(file -> {
            if (file.isLeft()) {
                ExcelHelper.catchError(exportService, idNewFile, "error when creating xlsx" + file.left(),handler);
            } else {
                logger.info("Export Publipostage ended");

                Buffer xlsx = file.right().getValue();
                saveBuffer(xlsx, titleFile,handler);

            }
        });
    }

    private void exportRME(Integer instructionId, String titleFile, Handler<Either<String, Boolean>> handler) {
        logger.info("Export RME started");

        this.instruction = new Instruction(exportService, idNewFile, instructionId);
        this.instruction.exportInvestissement(event -> {
            if (event.isLeft()) {
                ExcelHelper.catchError(exportService, idNewFile, "error when creating xlsx" + event.left(),handler);
            } else {
                logger.info("Export RME ended");

                Buffer xlsx = event.right().getValue();
                saveBuffer(xlsx, titleFile,handler);

            }
        });
    }

    private void saveBuffer(Buffer xlsx, String fileName,Handler<Either<String,Boolean>> handler) {
        storage.writeBuffer(xlsx, "application/vnd.ms-excel", fileName, file -> {
            if (!"ok".equals(file.getString("status"))) {
                ExcelHelper.catchError(exportService, idNewFile, "An error occurred when inserting xlsx ",handler);
                handler.handle(new Either.Left<>("An error occurred when inserting xlsx"));
            } else {
                logger.info("Xlsx insert in storage");
                exportService.updateWhenSuccess(file.getString("_id"), idNewFile,handler);
            }
        });
    }

    private void exportEquipment(int instructionId, String type, String titleFile, Handler<Either<String, Boolean>> handler) {
        logger.info("Export Equipment started");
        this.instruction = new Instruction(exportService, idNewFile, instructionId);
        this.instruction.exportEquipmentRapp(event1 -> {
            if (event1.isLeft()) {
                ExcelHelper.catchError(exportService, idNewFile, "error when creating xlsx",handler);
                handler.handle(new Either.Left<>("An error occurred when creating xlsx"));
            } else {
                Buffer xlsx = event1.right().getValue();
                logger.info("Export Equipment ended");
                saveBuffer(xlsx, titleFile,handler);
            }
        }, type);
    }


}
