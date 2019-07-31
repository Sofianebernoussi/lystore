package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.security.AccessExportDownload;
import fr.openent.lystore.service.ExportService;
import fr.openent.lystore.service.impl.DefaultExportServiceService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

public class ExportController extends ControllerHelper {
    private ExportService exportService;
    private Storage storage;

    public ExportController(Storage storage) {
        super();
        this.storage = storage;
        this.exportService = new DefaultExportServiceService(Lystore.lystoreSchema, "export", storage);
    }

    @Get("/exports")
    @ApiDoc("Returns all exports in database filtered by owner")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getExercises(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                exportService.getExports(arrayResponseHandler(request), user);
            }
        });
    }

    @Get("/export/:fileId")
    @ApiDoc("Returns all exports in database filtered by owner")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessExportDownload.class)
    public void getExercise(HttpServerRequest request) {
        String fileId = request.getParam("fileId");
        exportService.getXlsxName(fileId, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                exportService.getXlsx(fileId, file ->
                        request.response()
                                .putHeader("Content-type", "application/vnd.ms-excel; charset=utf-8")
                                .putHeader("Content-Length", file.length() + "")
                                .putHeader("Content-Disposition", "filename=" + event.right().getValue().getJsonObject(0).getString("filename"))
                                .end(file));
            }
        });

    }
}
