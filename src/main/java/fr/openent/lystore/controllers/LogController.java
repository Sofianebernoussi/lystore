package fr.openent.lystore.controllers;

import fr.openent.lystore.security.AdministratorRight;
import fr.openent.lystore.service.LogService;
import fr.openent.lystore.service.impl.DefaultLogService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

public class LogController extends ControllerHelper {

    public static final String UTF8_BOM = "\uFEFF";

    private final LogService logService;

    public LogController () {
        this.logService = new DefaultLogService();
    }

    @Get("/logs")
    @ApiDoc("List all Logs")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    @Override
    public void list (HttpServerRequest request) {
        try {
            Integer page = request.params().contains("page")
                ? Integer.parseInt(request.params().get("page"))
                : null;
            logService.list(page, arrayResponseHandler(request));
        } catch (ClassCastException e) {
            log.error("An error occurred when casting page number", e);
        }
    }

    @Get("/logs/export")
    @ApiDoc("Export logs as CSV")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void export (final HttpServerRequest request) {
        logService.list(null, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    request.response()
                            .putHeader("Content-Type", "text/csv; charset=utf-8")
                            .putHeader("Content-Disposition", "attachment; filename=logs.csv")
                            .end(generateExport(request, event.right().getValue()));
                } else {
                    badRequest(request);
                }
            }
        });
    }

    private static String generateExport (HttpServerRequest request, JsonArray logs) {
        StringBuilder report = new StringBuilder(UTF8_BOM).append(getExportHeader(request));
        for (int i = 0; i < logs.size(); i++) {
          report.append(generateExportLine(request, (JsonObject) logs.get(i)));
        }
        return report.toString();
    }

    private static String getExportHeader (HttpServerRequest request) {
        return I18n.getInstance().translate("date", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("user", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("action", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("context", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("resource", getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("value", getHost(request), I18n.acceptLanguage(request))
                + "\n";
    }

    private static String generateExportLine (HttpServerRequest request, JsonObject log) {
        return log.getString("date") + ";" +
                log.getString("username") + ";" +
                log.getString("action") + ";" +
                I18n.getInstance().translate(log.getString("context"), getHost(request),
                        I18n.acceptLanguage(request)) + ";" +
                log.getString("item") + ";" +
                (log.getString("value") != null ? log.getString("value").replace("\\\"", "\"") : "")
                + "\n";
    }
}
