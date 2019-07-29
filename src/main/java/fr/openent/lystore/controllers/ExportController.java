package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.ExportService;
import fr.openent.lystore.service.impl.DefaultExportServiceService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
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
        this.exportService = new DefaultExportServiceService(Lystore.lystoreSchema, "instruction");
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
}
