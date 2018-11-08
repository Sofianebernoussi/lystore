package fr.openent.lystore.controllers;

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

    TitleService titleService = new DefaultTitleService();

    @Get("/titles")
    @ApiDoc("List all titles in databases")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getTitles(HttpServerRequest request) {
        titleService.getTitles(arrayResponseHandler(request));
    }
}
