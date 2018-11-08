package fr.openent.lystore.controllers;

import fr.openent.lystore.service.GradeService;
import fr.openent.lystore.service.impl.DefaultGradeService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

public class GradeController extends ControllerHelper {

    GradeService gradeService = new DefaultGradeService();

    @Get("/grades")
    @ApiDoc("List all grades in database")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void listGrades(HttpServerRequest request) {
        gradeService.listGrades(arrayResponseHandler(request));
    }
}
