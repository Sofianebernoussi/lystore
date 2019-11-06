package fr.openent.lystore.helpers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.logging.Actions;
import fr.openent.lystore.logging.Contexts;
import fr.openent.lystore.logging.Logging;
import fr.openent.lystore.service.ExportService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.user.UserUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;

public class ExportHelper {

    protected static Logger log = LoggerFactory.getLogger(ExportHelper.class);
    public static String makeTheNameExport(String nameFile,String extension) {
        return getDate() + nameFile + "." +extension;
    }

    public static String makeTheNameExport(String nameFile,String extension, String type) {
        return getDate() + nameFile + type + "." +extension;
    }

    public static void catchError(ExportService exportService, String idFile, Exception errorCatch) {
        exportService.updateWhenError(idFile, makeError -> {
            if (makeError.isLeft()) {
                log.error("Error for create file export excel " + makeError.left() + errorCatch);
            }
        });
        log.error("Error for create file export excel " + errorCatch);
    }
    public static void catchError(ExportService exportService, String idFile, String errorCatchTextOutput) {
        exportService.updateWhenError(idFile, makeError -> {
            if (makeError.isLeft()) {
                log.error("Error for create file export excel " + makeError.left() + errorCatchTextOutput);
            }
        });
        log.error("Error for create file export excel " + errorCatchTextOutput);
    }
    public static void catchError(ExportService exportService, String idFile, String errorCatchTextOutput, Handler<Either<String,Boolean>> handler) {
        exportService.updateWhenError(idFile,handler);
        log.error("Error for create file export excel " + errorCatchTextOutput);
    }

    private static String getDate() {
        java.util.Date date = Calendar.getInstance().getTime();
        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        return formatter.format(date);
    }

    public static void makeExport(HttpServerRequest request, EventBus eb, ExportService exportService, String typeObject, String extension, String
            action, String name) {
        String id="-1";
        JsonObject params = new JsonObject();

        boolean withType = request.getParam("type") != null;
        if(request.getParam("id")!=null && typeObject.equals(Lystore.INSTRUCTIONS))
            id = request.getParam("id");
        if(request.params().getAll("number_validation") != null && !request.params().getAll("number_validation").isEmpty() ){
            id = request.params().getAll("number_validation").get(0);
            params.put("numberValidations",new JsonArray(request.params().getAll("number_validation")));
        }
        String type = "";
        JsonObject infoFile = new JsonObject();
        if (withType) {
            type = request.getParam("type");
            infoFile.put("type", type);
        }

        String titleFile = withType ? makeTheNameExport(name,extension, type) : makeTheNameExport(name,extension);
        log.info("makeExportExcel");
        String finalId = id;




        UserUtils.getUserInfos(eb, request, user -> {
            exportService.createWhenStart(typeObject,extension, infoFile, finalId, titleFile, user.getUserId(), action,params, newExport -> {
                if (newExport.isRight()) {
                    String idExport = newExport.right().getValue().getString("id");
                    try {
                        Logging.insert(eb,
                                request,
                                Contexts.EXPORT.toString(),
                                Actions.CREATE.toString(),
                                idExport.toString(),
                                new JsonObject().put("ids", idExport).put("fileName", titleFile));
                        log.info("J'envoie la demande d export");
                        Lystore.launchWorker(eb);
                        request.response().setStatusCode(201).end("Import started " + idExport);
                    } catch (Exception error) {
                        catchError(exportService, idExport, error);
                    }
                } else {
                    log.error("Fail to insert file in SQL " + newExport.left());
                }
            });
        });
    }

    ;



}
