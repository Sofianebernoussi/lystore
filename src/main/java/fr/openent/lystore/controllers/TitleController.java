package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.TitleService;
import fr.openent.lystore.service.impl.DefaultTitleService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

public class TitleController extends ControllerHelper {

    private final TitleService titleService;

    public TitleController() {
        super();
        titleService = new DefaultTitleService(Lystore.lystoreSchema, "title");
    }

    @Get("/titles")
    @ApiDoc("Get list of the titles")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getTitles(HttpServerRequest request) {
        titleService.getTitles(arrayResponseHandler(request));
    }


}
