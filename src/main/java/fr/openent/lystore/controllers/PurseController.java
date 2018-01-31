package fr.openent.lystore.controllers;

import com.opencsv.CSVReader;
import fr.openent.lystore.logging.Actions;
import fr.openent.lystore.logging.Contexts;
import fr.openent.lystore.logging.Logging;
import fr.openent.lystore.security.AdministratorRight;
import fr.openent.lystore.security.WorkflowActionUtils;
import fr.openent.lystore.security.WorkflowActions;
import fr.openent.lystore.service.PurseService;
import fr.openent.lystore.service.StructureService;
import fr.openent.lystore.service.impl.DefaultPurseService;
import fr.openent.lystore.service.impl.DefaultStructureService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.DefaultAsyncResult;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.entcore.directory.exceptions.ImportException;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.VoidHandler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerFileUpload;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

import static org.entcore.common.utils.FileUtils.deleteImportPath;

public class PurseController extends ControllerHelper {

    private StructureService structureService;
    private PurseService purseService;

    public PurseController () {
        super();
        this.structureService = new DefaultStructureService();
        this.purseService = new DefaultPurseService();
    }

    @Post("/campaign/:id/purses/import")
    @ApiDoc("Import purse for a specific campaign")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void purse(final HttpServerRequest request) {
        final String importId = UUID.randomUUID().toString();
        final String path = container.config().getString("import-folder", "/tmp") + File.separator + importId;
        uploadImport(request, path, new Handler<AsyncResult>() {
            @Override
            public void handle(AsyncResult event) {
                if (event.succeeded()) {
                    readCsv(request, path);
                } else {
                    badRequest(request, event.cause().getMessage());
                }
            }
        });
    }

    /**
     * Upload import with multipart
     * @param request Http request containing files
     * @param handler Function handler returning data
     */
    private void uploadImport(final HttpServerRequest request, final String path, final Handler<AsyncResult> handler) {
        request.pause();
        request.expectMultiPart(true);
        request.endHandler(getEndHandler(request, path, handler));
        request.exceptionHandler(getExceptionHandler(path, handler));
        request.uploadHandler(getUploadHandler(path, handler));
        vertx.fileSystem().mkdir(path, new Handler<AsyncResult<Void>>() {
            @Override
            public void handle(AsyncResult<Void> event) {
                if (event.succeeded()) {
                    request.resume();
                } else {
                    handler.handle(new DefaultAsyncResult(
                            new ImportException("mkdir.error", event.cause())));
                }
            }
        });
    }

    /**
     * Get end upload handler
     * @param request Http Server Request
     * @param path Upload directory path
     * @param handler Function handler
     * @return VoidHandler
     */
    private VoidHandler getEndHandler(final HttpServerRequest request, final String path,
                                      final Handler<AsyncResult> handler) {
        return new VoidHandler() {
            @Override
            protected void handle() {
                UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
                    @Override
                    public void handle(UserInfos user) {
                        if (WorkflowActionUtils.hasRight(user, WorkflowActions.ADMINISTRATOR_RIGHT.toString())) {
                            handler.handle(new DefaultAsyncResult(null));
                        } else {
                            handler.handle(new DefaultAsyncResult(new ImportException("invalid.admin")));
                            deleteImportPath(vertx, path);
                        }
                    }
                });
            }
        };
    }

    /**
     * Get exception handler. It return a handler that catch error while the request upload the file.
     * In case of exception, the handler delete the directory.
     * @param path Temp directory path
     * @param handler Function handler
     * @return Handler<Throwable>
     */
    private Handler<Throwable> getExceptionHandler(final String path, final Handler<AsyncResult> handler) {
        return new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                handler.handle(new DefaultAsyncResult(event));
                deleteImportPath(vertx, path);
            }
        };
    }

    /**
     * Get chunk upload handler
     * @param path Upload directory path
     * @param handler Function handler
     * @return Upload handler
     */
    private static Handler<HttpServerFileUpload> getUploadHandler(final String path,
                                                                  final Handler<AsyncResult> handler) {
        return new Handler<HttpServerFileUpload>() {
            @Override
            public void handle(final HttpServerFileUpload upload) {
                if (!upload.filename().toLowerCase().endsWith(".csv")) {
                    handler.handle(new DefaultAsyncResult(
                            new ImportException("invalid.file.extension")
                    ));
                    return;
                }

                final String filename = path + File.separator + upload.filename();
                upload.endHandler(new Handler<Void>() {
                    @Override
                    public void handle(Void event) {
                        log.info("File " + upload.filename() + " uploaded as " + upload.filename());
                    }
                });
                upload.streamToFileSystem(filename);
            }
        };
    }

    /**
     * Read CSV file
     * @param request Http request
     * @param path Temp directory path
     */
    private void readCsv(final HttpServerRequest request, final String path) {
        vertx.fileSystem().readDir(path, new Handler<AsyncResult<String[]>>() {
            @Override
            public void handle(final AsyncResult<String[]> event) {
                if (event.succeeded()) {
                    String file = event.result()[0];
                    vertx.fileSystem().readFile(file, new Handler<AsyncResult<Buffer>>() {
                        @Override
                        public void handle(AsyncResult<Buffer> eventBuffer) {
                            if (eventBuffer.succeeded()) {
                                parseCsv(request, path, eventBuffer.result());
                            } else {
                                returnErrorMessage(request, event.cause(), path);
                            }
                        }
                    });
                } else {
                    returnErrorMessage(request, event.cause(), path);
                }
            }
        });
    }

    /**
     * Parse CSV file
     * @param request Http request
     * @param path Directory path
     */
    private void parseCsv(final HttpServerRequest request, final String path, Buffer content) {
        try {
            CSVReader csv = new CSVReader(new InputStreamReader(
                    new ByteArrayInputStream(content.getBytes())),
                    ';', '"', 1);
            String[] values;
            JsonArray uais = new JsonArray();
            JsonObject amounts = new JsonObject();
            while ((values = csv.readNext()) != null) {
                amounts.putString(values[0], values[1]);
                uais.addString(values[0]);
            }
            if (uais.size() > 0) {
                matchUAIID(request, path, uais, amounts);
            } else {
                returnErrorMessage(request, new Throwable("missing.uai"), path);
            }
        } catch (IOException e) {
            log.error("[Lystore@CSVImport]: csv exception", e);
            returnErrorMessage(request, e.getCause(), path);
        }
    }

    /**
     * Match structure UAI with its Neo4j id.
     * @param request Http request
     * @param path Directory path
     * @param uais UAIs list
     * @param amount Object containing UAI as key and purse amount as value
     */
    private void matchUAIID(final HttpServerRequest request, final String path, JsonArray uais,
                            final JsonObject amount) {
        structureService.getStructureByUAI(uais, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    JsonArray ids = event.right().getValue();
                    JsonObject statementsValues = new JsonObject();
                    JsonObject id;
                    for (int i = 0; i < ids.size(); i++) {
                        id = ids.get(i);
                        statementsValues.putString(id.getString("id"),
                                amount.getString(id.getString("uai")));
                    }
                    launchImport(request, path, statementsValues);
                } else {
                    returnErrorMessage(request, new Throwable(event.left().getValue()), path);
                }
            }
        });
    }

    /**
     * Launch database import
     * @param request Http request
     * @param path Directory path
     * @param statementsValues Object containing statement values
     */
    private void launchImport(final HttpServerRequest request, final String path, JsonObject statementsValues) {
        try {
            purseService.launchImport(Integer.parseInt(request.params().get("id")),
                    statementsValues, new Handler<Either<String, JsonObject>>() {
                @Override
                public void handle(Either<String, JsonObject> event) {
                    if (event.isRight()) {
                        Renders.renderJson(request, event.right().getValue());
                        deleteImportPath(vertx, path);
                    } else {
                        returnErrorMessage(request, new Throwable(event.left().getValue()), path);
                    }
                }
            });
        } catch (ClassCastException e) {
            log.error("[Lystore@launchImport] : An error occurred when parsing campaign id", e);
            returnErrorMessage(request, e.getCause(), path);
        }
    }

    /**
     * End http request and returns message error. It delete the directory.
     * @param request Http request
     * @param cause Throwable message
     * @param path Directory path to delete
     */
    private void returnErrorMessage(HttpServerRequest request, Throwable cause, String path) {
        renderErrorMessage(request, cause);
        deleteImportPath(vertx, path);
    }

    /**
     * Render a message error based on cause message
     * @param request Http request
     * @param cause Cause error
     */
    private static void renderErrorMessage(HttpServerRequest request, Throwable cause) {
        renderError(request, new JsonObject().putString("message", cause.getMessage()));
    }

    @Get("/campaign/:id/purses/export")
    @ApiDoc("Export purses for a specific campaign")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void export(final HttpServerRequest request) {
        try {
            Integer idCampaign = Integer.parseInt(request.params().get("id"));
            purseService.getPursesByCampaignId(idCampaign, new Handler<Either<String, JsonArray>>() {
                @Override
                public void handle(Either<String, JsonArray> event) {
                    if (event.isRight()) {
                        JsonArray ids = new JsonArray();
                        JsonObject exportValues = new JsonObject();
                        JsonArray purses = event.right().getValue();
                        JsonObject purse;
                        for (int i = 0; i < purses.size(); i++) {
                            purse = purses.get(i);
                            exportValues.putNumber(purse.getString("id_structure"),
                                    Float.parseFloat(purse.getString("amount")));
                            ids.addString(purse.getString("id_structure"));
                        }
                        retrieveUAIs(ids, exportValues, request);
                    } else {
                        badRequest(request);
                    }
                }
            });
        } catch (ClassCastException e) {
            log.error("[Lystore@CSVExport] : An error occurred when casting campaign id", e);
            badRequest(request);
        }
    }

    @Put("/purse/:id")
    @ApiDoc("Update a purse based on his id")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void updateHolder (final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "purse", new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject body) {
                try {
                    purseService.update(Integer.parseInt(request.params().get("id")), body,
                            Logging.defaultResponseHandler(eb,
                                    request,
                                    Contexts.PURSE.toString(),
                                    Actions.UPDATE.toString(),
                                    request.params().get("id"),
                                    body));
                } catch (ClassCastException e) {
                    log.error("An error occurred when casting purse id", e);
                    badRequest(request);
                }
            }
        });
    }

    @Get("/campaign/:id/purses/list")
    @ApiDoc("Get purses for a specific campaign")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    @Override
    public void list(final HttpServerRequest request) {
        try {
            Integer idCampaign = Integer.parseInt(request.params().get("id"));
            purseService.getPursesByCampaignId(idCampaign, new Handler<Either<String, JsonArray>>() {
                @Override
                public void handle(Either<String, JsonArray> event) {
                    if (event.isRight()) {
                        JsonArray ids = new JsonArray();
                        JsonArray purses = event.right().getValue();
                        JsonObject purse;
                        for (int i = 0; i < purses.size(); i++) {
                            purse = purses.get(i);
                            ids.addString(purse.getString("id_structure"));
                        }
                        retrieveStructuresData(ids, purses, request);
                    } else {
                        badRequest(request);
                    }
                }
            });
        } catch (ClassCastException e) {
            log.error("[Lystore@purses] : An error occurred when casting campaign id", e);
            badRequest(request);
        }
    }

    /**
     * Retrieve structure uais and name based on ids list
     * @param ids JsonArray containing ids list
     * @param purses JsonArray containing purses list
     * @param request Http request
     */
    private void retrieveStructuresData(JsonArray ids, final JsonArray purses, final HttpServerRequest request) {
        structureService.getStructureById(ids, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    JsonArray structures = event.right().getValue();
                    JsonObject structure;
                    JsonObject purse;

                    // put structure name / uai on the purse according to structure id
                    for (int i = 0; i < structures.size(); i++) {
                        structure = structures.get(i);
                        for (int j = 0; j < purses.size(); j++) {
                            purse = purses.get(j);

                            if(purse.getField("id_structure").equals(structure.getField("id"))) {
                                purse.putString("name", structure.getField("name").toString());
                                purse.putString("uai", structure.getField("uai").toString());

                                // we also convert amount to get a number instead of a string
                                String amount = purse.getString("amount");
                                purse.removeField("amount");
                                purse.putNumber("amount",Double.parseDouble(amount));
                            }
                        }
                    }

                    Renders.renderJson(request, purses);

                } else {
                    renderError(request, new JsonObject().putString("message",
                            event.left().getValue()));
                }
            }
        });
    }

    /**
     * Retrieve structure uais based on ids list
     * @param ids JsonArray containing ids list
     * @param exportValues Values to exports
     * @param request Http request
     */
    private void retrieveUAIs(JsonArray ids, final JsonObject exportValues,
                              final HttpServerRequest request) {
        structureService.getStructureById(ids, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    JsonObject values = new JsonObject();
                    JsonArray uais = event.right().getValue();
                    JsonObject uai;
                    for (int i = 0; i < uais.size(); i++) {
                        uai = uais.get(i);
                        values.putNumber(uai.getString("uai"),
                                exportValues.getNumber(uai.getString("id")));
                    }
                    launchExport(values, request);
                } else {
                    renderError(request, new JsonObject().putString("message",
                            event.left().getValue()));
                }
            }
        });
    }

    /**
     * Launch export. Build CSV based on values parameter
     * @param values values to export
     * @param request Http request
     */
    private static void launchExport(JsonObject values, HttpServerRequest request) {
        String[] uais = values.getFieldNames().toArray(new String[0]);
        StringBuilder exportString = new StringBuilder(getCSVHeader(request));
        for (String uai : uais) {
            exportString.append(getCSVLine(uai, values.getNumber(uai)));
        }
        request.response()
                .putHeader("Content-Type", "text/csv; charset=utf-8")
                .putHeader("Content-Disposition", "attachment; filename=" + getFileExportName(request))
                .end(exportString.toString());
    }

    /**
     * Get CSV Header using internationalization
     * @param request Http request
     * @return CSV file Header
     */
    private static String getCSVHeader(HttpServerRequest request) {
        return I18n.getInstance().translate("UAI", getHost(request), I18n.acceptLanguage(request)) +
                ";" +
                I18n.getInstance().translate("purse", getHost(request), I18n.acceptLanguage(request)) +
                "\n";
    }

    /**
     * Get CSV Line
     * @param uai Structure UAI
     * @param amount Structure purse amount
     * @return CSV Line
     */
    private static String getCSVLine(String uai, Number amount) {
        return uai + ";" + amount + "\n";
    }

    /**
     * Get File Export Name. It use internationalization to build the name.
     * @param request Http request
     * @return File name
     */
    private static String getFileExportName(HttpServerRequest request) {
        return I18n.getInstance().translate("campaign", getHost(request), I18n.acceptLanguage(request)) +
                "-" + request.params().get("id") + "-" +
                I18n.getInstance().translate("purse", getHost(request), I18n.acceptLanguage(request)) +
                ".csv";
    }
}
