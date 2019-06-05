package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.InstructionService;
import fr.openent.lystore.service.impl.DefaultInstructionService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

public class InstructionController extends ControllerHelper {
    private InstructionService instructionService ;

    public InstructionController () {
        super();
        this.instructionService = new DefaultInstructionService(Lystore.lystoreSchema, "instruction");
    }

    @Get("/exercises")
    @ApiDoc("Returns all exercises in database")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getExercises (HttpServerRequest request) { instructionService.getExercises(arrayResponseHandler(request));
    }

}
