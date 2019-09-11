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
import io.vertx.core.json.JsonObject;
import org.entcore.common.storage.Storage;
import org.vertx.java.busmods.BusModBase;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static fr.openent.lystore.Lystore.CONFIG;
import static fr.openent.lystore.Lystore.STORAGE;

public class ExportLystoreWorker extends BusModBase implements Handler<Message<JsonObject>> {
    private Instruction instruction;
    private Storage storage;
    private ExportService exportService = new DefaultExportServiceService(Lystore.lystoreSchema, "export", storage);
    private String idNewFile;
    private boolean isWorking = false;
    Queue<Message<JsonObject>> MessagesQueue = new ConcurrentLinkedQueue<>();

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
        logger.info("Calling Worker");
        MessagesQueue.add(event);
        if(!isWorking && !MessagesQueue.isEmpty()){
            isWorking = true;
            processExport();
        }
    }




    private void processExport(){
        logger.info("Process export nb Queue:" + MessagesQueue.size());
        if(MessagesQueue.isEmpty()){
            isWorking =  false;
        }else {
            Message<JsonObject> eventMessage = MessagesQueue.poll();
            Handler<Either<String,Boolean>> exportHandler = event -> {
                logger.info("exportHandler");
                if (event.isRight()) {
                    eventMessage.reply(new JsonObject().put("status", "ok"));
                    logger.info("end");
                    processExport();
                } else {
                    eventMessage.reply(new JsonObject().put("status", "ko"));
                    ExcelHelper.catchError(exportService, idNewFile, "error when creating xlsx " + event.left().getValue());
                }
            };
            try{
                logger.info("Doing export "+ MessagesQueue.size() +" in waitinglist");
                final String action = eventMessage.body().getString("action", "");
                String fileNamIn = eventMessage.body().getString("titleFile");
                idNewFile = eventMessage.body().getString("idFile");
                switch (action) {
                    case "exportEQU":
                        exportEquipment(
                                Integer.parseInt(eventMessage.body().getString("id")),
                                eventMessage.body().getString("type"),
                                fileNamIn,exportHandler );
                        break;
                    case "exportRME":
                        exportRME(
                                Integer.parseInt(eventMessage.body().getString("id")),
                                fileNamIn,
                                exportHandler);
                        break;
                    case "exportNotificationCP":
                        exportNotificationCp(
                                Integer.parseInt(eventMessage.body().getString("id")),
                                fileNamIn,
                                exportHandler);
                        break;
                    case "exportPublipostage":
                        exportPublipostage(
                                Integer.parseInt(eventMessage.body().getString("id")),
                                fileNamIn,
                                exportHandler);
                        break;
                    case "exportSubvention":
                        exportSubvention(
                                Integer.parseInt(eventMessage.body().getString("id")),
                                fileNamIn,
                                exportHandler);
                        break;
                    default:
                        ExcelHelper.catchError(exportService, idNewFile, "Invalid action in worker",exportHandler);
                        break;
                }
            } catch (Exception error) {
                logger.error("Error in switch -> " + error);
            }
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
