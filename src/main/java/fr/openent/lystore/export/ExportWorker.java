package fr.openent.lystore.export;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.helpers.ExcelHelper;
import fr.openent.lystore.service.ExportService;
import fr.openent.lystore.service.impl.DefaultExportServiceService;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.dns.DnsClient;
import io.vertx.core.dns.DnsClientOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.shareddata.SharedData;
import io.vertx.core.spi.VerticleFactory;
import org.entcore.common.storage.Storage;
import org.vertx.java.busmods.BusModBase;

import java.util.Queue;
import java.util.Set;
import java.util.function.Supplier;
import java.util.concurrent.ConcurrentLinkedQueue;

import static fr.openent.lystore.Lystore.CONFIG;
import static fr.openent.lystore.Lystore.STORAGE;

public class ExportWorker extends BusModBase implements Handler<Message<JsonObject>> {
    private Instruction instruction;
    private Storage storage;
    private ExportService exportService = new DefaultExportServiceService(Lystore.lystoreSchema, "export", storage);
    private Number idNewFile;
    private boolean isWorking = false;
    Queue<Message<JsonObject>> MessagesQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void start() {
        super.start();
        vertx.eventBus().localConsumer(ExportWorker.class.getSimpleName(), this);
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
            Handler<Either<String,Boolean>> exportHandler = event -> {
                logger.info("exportHandler");
                if (event.isRight()) {
                    logger.info("end");
                    processExport();
                } else {
                    ExcelHelper.catchError(exportService, idNewFile, "error when creating xlsx" + event.left());
                }
            };

            Message<JsonObject> event = MessagesQueue.poll();
            logger.info("Doing export "+ MessagesQueue.size() +" in waitinglist");
            final String action = event.body().getString("action", "");
            String fileNamIn = event.body().getString("titleFile");
            idNewFile = event.body().getInteger("idFile");
            switch (action) {
                case "exportEQU":
                    exportEquipment(
                            event.body().getInteger("id"),
                            event.body().getString("type"),
                            fileNamIn,exportHandler );
                    break;
                case "exportRME":
                    exportRME(
                            event.body().getInteger("id"),
                            fileNamIn,
                            exportHandler);
                    break;
                case "exportNotificationCP":
                    exportNotificationCp(event.body().getInteger("id"),
                            fileNamIn,
                            exportHandler);
                    break;
                case "exportPublipostage":
                    exportPublipostage(event.body().getInteger("id"),
                            fileNamIn,
                            exportHandler);
                    break;
                case "exportSubvention":
                    exportSubvention(event.body().getInteger("id"),
                            fileNamIn,
                            exportHandler);
                    break;
                default:
                    ExcelHelper.catchError(exportService, idNewFile, "Invalid action in worker",exportHandler);
                    break;



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
