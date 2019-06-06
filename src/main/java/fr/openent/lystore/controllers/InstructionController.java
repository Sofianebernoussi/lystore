package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.logging.Actions;
import fr.openent.lystore.logging.Contexts;
import fr.openent.lystore.logging.Logging;
import fr.openent.lystore.security.AdministratorRight;
import fr.openent.lystore.service.InstructionService;
import fr.openent.lystore.service.impl.DefaultInstructionService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

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

    @Get("/instructions/")
    @ApiDoc("List all instructions in database")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getInstructions(HttpServerRequest request) {
        instructionService.getInstructions(request.params().getAll("q"), arrayResponseHandler(request));
    }

/*    @Post("/operation")
    @ApiDoc("Create an operation")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    @Override
    public void create(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "operation", operation -> operationService.create(operation, Logging.defaultResponseHandler(eb,
                request,
                Contexts.OPERATION.toString(),
                Actions.CREATE.toString(),
                null,
                operation)));
    }
/*
    @Put("/operation/:idOperation")
    @ApiDoc("Uptdate an operation")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void updateOperation(final HttpServerRequest request) {
        final Integer idOperation = Integer.parseInt(request.params().get("idOperation"));
        RequestUtils.bodyToJson(request, pathPrefix + "operation", operation -> operationService.updateOperation(idOperation, operation, Logging.defaultResponseHandler(eb,
                request,
                Contexts.OPERATION.toString(),
                Actions.UPDATE.toString(),
                null,
                operation)));
    }
*/
    @Delete("/instructions")
    @ApiDoc("Delete instructions")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void deleteInstruction(final HttpServerRequest request) {
        RequestUtils.bodyToJsonArray(request, instructionIds -> instructionService.deleteInstruction(instructionIds, Logging.defaultResponseHandler(eb,
                request,
                Contexts.INSTRUCTION.toString(),
                Actions.DELETE.toString(),
                null,
                new JsonObject().put("ids", instructionIds))));
    }
}
