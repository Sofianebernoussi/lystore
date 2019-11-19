package fr.openent.lystore.export;

import fr.openent.lystore.export.instructions.Instruction;
import fr.openent.lystore.export.validOrders.ValidOrders;
import fr.openent.lystore.helpers.ExcelHelper;
import fr.openent.lystore.helpers.ExportHelper;
import fr.openent.lystore.service.ExportService;
import fr.openent.lystore.service.impl.DefaultExportServiceService;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.entcore.common.storage.Storage;
import org.vertx.java.busmods.BusModBase;

import static fr.openent.lystore.Lystore.*;

public class ExportLystoreWorker extends BusModBase implements Handler<Message<JsonObject>> {
    private Instruction instruction;
    private ValidOrders validOrders;
    private Storage storage;
    private ExportService exportService = new DefaultExportServiceService(storage);
    private String idNewFile;
    private boolean isWorking = false;
    private boolean isSleeping = true;
    private final String XLSXHEADER= "application/vnd.ms-excel";
    private final String PDFHEADER = "application/pdf";

    @Override
    public void start() {
        super.start();
        vertx.eventBus().localConsumer(ExportLystoreWorker.class.getSimpleName(), this);
        this.config = CONFIG;
        this.storage = STORAGE;
        this.vertx = vertx;

    }

    @Override
    public void handle(Message<JsonObject> eventMessage) {
        eventMessage.reply(new JsonObject().put("status", "ok"));
        if (isSleeping) {
            logger.info("Calling Worker");
            isSleeping = false;
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
                ExportHelper.catchError(exportService, idNewFile, "error when creating xlsx " + event.left().getValue());
            }
        };
        exportService.getWaitingExport(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if(event.isRight()){
                    JsonObject waitingOrder = event.right().getValue();
                    chooseExport( waitingOrder,exportHandler);
                }else{
                    isSleeping = true;
                    new java.util.Timer().schedule(
                            new java.util.TimerTask() {
                                @Override
                                public void run() {
                                    processExport();
                                }
                            },
                            3600*1000
                    );
                }
            }
        });
    }

    private void chooseExport(JsonObject body, Handler<Either<String, Boolean>> exportHandler) {
        final String action = body.getString("action", "");
        String fileName = body.getString("filename");
        idNewFile = body.getString("_id");
        Integer object_id = -1;
        String string_object_id ="";
        JsonObject params = body.getJsonObject("externalParams");
        try {
            object_id = Integer.parseInt(body.getString("object_id"));
            string_object_id = object_id.toString();
        }catch (ClassCastException ce){
            object_id = body.getInteger("object_id");
            string_object_id = object_id.toString();
        }catch (NumberFormatException ce){
            string_object_id = body.getString("object_id");

        }
        switch (action) {
            case "exportEQU":
                exportEquipment(
                        object_id,
                        body.getString("type"),
                        fileName,exportHandler );
                break;
            case "exportRME":
                exportRME(
                        object_id,
                        fileName,
                        exportHandler);
                break;
            case "exportNotificationCP":
                exportNotificationCp(
                        object_id,
                        fileName,
                        exportHandler);
                break;
            case "exportPublipostage":
                exportPublipostage(
                        object_id,
                        fileName,
                        exportHandler);
                break;
            case "exportSubvention":
                exportSubvention(
                        object_id,
                        fileName,
                        exportHandler);
                break;
            case "exportIris":
                exportIris(object_id,
                        fileName,
                        exportHandler);
                break;
            case "exportListLycOrders":
                exportListLycOrders(string_object_id,
                        fileName,
                        exportHandler);
                break;
            case "exportBCOrders":
                exportBCOrders(params,
                        fileName,
                        exportHandler);
                break;
            case "exportBCOrdersDuringValidation":
                exportBCOrdersDuringValidation(params,
                        fileName,
                        exportHandler);
                break;
            case "exportBCOrdersAfterValidation":
                exportBCOrdersAfterValidation(string_object_id,
                        fileName,
                        exportHandler);
                break;

            case "exportBCOrdersAfterValidationStruct":
                exportBCOrdersAfterValidationStruct(string_object_id,fileName,exportHandler);
            default:
                ExportHelper.catchError(exportService, idNewFile, "Invalid action in worker : " + action,exportHandler);
                break;
        }
    }

    private void exportBCOrdersAfterValidationStruct(String object_id, String titleFile, Handler<Either<String, Boolean>> exportHandler) {

        logger.info("Export list lycee from Orders after validation started "+ object_id);

        this.validOrders = new ValidOrders(exportService,object_id,idNewFile,this.eb,this.vertx,this.config,false);
        this.validOrders.exportBCAfterValidation(event1 -> {
            if (event1.isLeft()) {
                ExportHelper.catchError(exportService, idNewFile, "error when creating PDF " + event1.left().getValue(),exportHandler);
            } else {
                Buffer xlsx = event1.right().getValue();
                saveBuffer(xlsx,  titleFile ,exportHandler,PDFHEADER);
            }
        });
    }

    private void exportBCOrdersAfterValidation(String object_id, String titleFile, Handler<Either<String, Boolean>> handler) {
        logger.info("Export BC per structures from Orders after validation started "+ object_id);

        this.validOrders = new ValidOrders(exportService,object_id,idNewFile,this.eb,this.vertx,this.config,false);
        this.validOrders.exportBCAfterValidation(event1 -> {
            if (event1.isLeft()) {
                ExportHelper.catchError(exportService, idNewFile, "error when creating PDF " + event1.left().getValue(),handler);
            } else {
                Buffer xlsx = event1.right().getValue();
                saveBuffer(xlsx,  titleFile ,handler,PDFHEADER);
            }
        });
    }

    private void exportBCOrdersDuringValidation(JsonObject params, String titleFile, Handler<Either<String, Boolean>> handler) {
        logger.info("Export BC from Orders during validation started");
        this.validOrders = new ValidOrders(exportService,params,idNewFile,this.eb,this.vertx,this.config);
        this.validOrders.exportBCDuringValidation(event1 -> {
            if (event1.isLeft()) {
                ExportHelper.catchError(exportService, idNewFile, "error when creating PDF " + event1.left().getValue(),handler);
            } else {
                Buffer xlsx = event1.right().getValue();
                saveBuffer(xlsx,  titleFile ,handler,PDFHEADER);
            }
        });
    }

    private void exportBCOrders(JsonObject params, String titleFile, Handler<Either<String, Boolean>> handler) {
        logger.info("Export BC from Orders started");
        this.validOrders = new ValidOrders(exportService,params,idNewFile,this.eb,this.vertx,this.config);
        this.validOrders.exportBC(event1 -> {
            if (event1.isLeft()) {
                ExportHelper.catchError(exportService, idNewFile, "error when creating PDF " + event1.left().getValue(),handler);
            } else {
                Buffer xlsx = event1.right().getValue();
                saveBuffer(xlsx,  titleFile ,handler,PDFHEADER);
            }
        });
    }

    private void exportListLycOrders(String object_id, String titleFile, Handler<Either<String, Boolean>> handler) {
        logger.info("Export list lycee from Orders started");
        this.validOrders = new ValidOrders(exportService,object_id,idNewFile,this.eb,this.vertx,this.config,true);
        this.validOrders.exportListLycee(event1 -> {
            if (event1.isLeft()) {
                ExportHelper.catchError(exportService, idNewFile, "error when creating xlsx" + event1.left(),handler);
            } else {
                Buffer xlsx = event1.right().getValue();
                saveBuffer(xlsx, titleFile,handler,XLSXHEADER);
            }
        });

    }

    private void exportIris(Integer instructionId, String titleFile, Handler<Either<String, Boolean>> handler) {
        logger.info("Export Iris started");
        this.instruction = new Instruction(exportService, idNewFile, instructionId);
        this.instruction.exportIris(event1 -> {
            if (event1.isLeft()) {
                ExportHelper.catchError(exportService, idNewFile, "error when creating xlsx" + event1.left(),handler);
            } else {
                Buffer xlsx = event1.right().getValue();
                saveBuffer(xlsx, titleFile,handler,XLSXHEADER);
            }
        });
    }

    private void exportNotificationCp(Integer instructionId, String titleFile, Handler<Either<String, Boolean>> handler) {
        logger.info("Export NotificationCP started");

        this.instruction = new Instruction(exportService, idNewFile, instructionId);
        this.instruction.exportNotficationCp(event1 -> {
            if (event1.isLeft()) {
                ExportHelper.catchError(exportService, idNewFile, "error when creating xlsx" + event1.left(),handler);
            } else {
                logger.info("Export NotificationCP ended");

                Buffer xlsx = event1.right().getValue();
                saveBuffer(xlsx, titleFile,handler,XLSXHEADER);

            }
        });
    }

    private void exportSubvention(Integer instructionId, String titleFile, Handler<Either<String, Boolean>> handler) {
        logger.info("Export Subvention started");

        this.instruction = new Instruction(exportService, idNewFile, instructionId);
        this.instruction.exportSubvention(event1 -> {
            if (event1.isLeft()) {
                ExportHelper.catchError(exportService, idNewFile, "error when creating xlsx" + event1.left(),handler);
            } else {
                logger.info("Export Subvention ended");

                Buffer xlsx = event1.right().getValue();
                saveBuffer(xlsx, titleFile,handler,XLSXHEADER);

            }
        });
    }

    private void exportPublipostage(Integer instructionId, String titleFile, Handler<Either<String, Boolean>> handler) {
        logger.info("Export Publipostage started");

        this.instruction = new Instruction(exportService, idNewFile, instructionId);
        this.instruction.exportPublipostage(file -> {
            if (file.isLeft()) {
                ExportHelper.catchError(exportService, idNewFile, "error when creating xlsx" + file.left(),handler);
            } else {
                logger.info("Export Publipostage ended");

                Buffer xlsx = file.right().getValue();
                saveBuffer(xlsx, titleFile,handler,XLSXHEADER);

            }
        });
    }

    private void exportRME(Integer instructionId, String titleFile, Handler<Either<String, Boolean>> handler) {
        logger.info("Export RME started");

        this.instruction = new Instruction(exportService, idNewFile, instructionId);
        this.instruction.exportInvestissement(event -> {
            if (event.isLeft()) {
                ExportHelper.catchError(exportService, idNewFile, "error when creating xlsx" + event.left(),handler);
            } else {
                logger.info("Export RME ended");

                Buffer xlsx = event.right().getValue();
                saveBuffer(xlsx, titleFile,handler,XLSXHEADER);

            }
        });
    }

    private void saveBuffer(Buffer buff, String fileName,Handler<Either<String,Boolean>> handler,String fileType) {
        storage.writeBuffer(buff, fileType, fileName, file -> {
            if (!"ok".equals(file.getString("status"))) {
                ExportHelper.catchError(exportService, idNewFile, "An error occurred when inserting xlsx ",handler);
                handler.handle(new Either.Left<>("An error occurred when inserting xlsx"));
            } else {
                logger.info(fileName + " insert in storage");
                exportService.updateWhenSuccess(file.getString("_id"), idNewFile,handler);
            }
        });
    }

    private void exportEquipment(int instructionId, String type, String titleFile, Handler<Either<String, Boolean>> handler) {
        logger.info("Export Equipment started");
        this.instruction = new Instruction(exportService, idNewFile, instructionId);
        this.instruction.exportEquipmentRapp(event1 -> {
            if (event1.isLeft()) {
                ExportHelper.catchError(exportService, idNewFile, "error when creating xlsx",handler);
                handler.handle(new Either.Left<>("An error occurred when creating xlsx"));
            } else {
                Buffer xlsx = event1.right().getValue();
                logger.info("Export Equipment ended");
                saveBuffer(xlsx, titleFile,handler,XLSXHEADER);
            }
        }, type);
    }


}
