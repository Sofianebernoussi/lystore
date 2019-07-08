package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.investissement.Instruction;
import fr.openent.lystore.logging.Actions;
import fr.openent.lystore.logging.Contexts;
import fr.openent.lystore.logging.Logging;
import fr.openent.lystore.security.ManagerRight;
import fr.openent.lystore.service.InstructionService;
import fr.openent.lystore.service.impl.DefaultInstructionService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.storage.Storage;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

public class InstructionController extends ControllerHelper {
    private InstructionService instructionService ;
    private Storage storage;

    public InstructionController(Storage storage) {
        super();
        this.storage = storage;
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
        instructionService.getInstructions(request.params().getAll("q"), either -> {
            if (either.isLeft()) {
                if ("404".equals(either.left().getValue())) {
                    notFound(request);
                } else {
                    renderError(request);
                }
            } else {
                renderJson(request, either.right().getValue());
            }
        });
    }

    @Post("/instruction")
    @ApiDoc("Create an instruction")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    @Override
    public void create(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "instruction", instruction -> instructionService.create(instruction, Logging.defaultResponseHandler(eb,
                request,
                Contexts.INSTRUCTION.toString(),
                Actions.CREATE.toString(),
                instruction.toString(),
                instruction)));
    }

    @Put("/instruction/:idInstruction")
    @ApiDoc("Uptdate an instruction")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void updateInstruction(final HttpServerRequest request) {
        final Integer idInstruction = Integer.parseInt(request.params().get("idInstruction"));
        RequestUtils.bodyToJson(request, pathPrefix + "instruction", instruction -> instructionService.updateInstruction(idInstruction, instruction, Logging.defaultResponseHandler(eb,
                request,
                Contexts.INSTRUCTION.toString(),
                Actions.UPDATE.toString(),
                idInstruction.toString(),
                instruction)));
    }

    @Delete("/instructions")
    @ApiDoc("Delete instructions")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void deleteInstruction(final HttpServerRequest request) {
        RequestUtils.bodyToJsonArray(request, instructionIds -> instructionService.deleteInstruction(instructionIds, Logging.defaultResponseHandler(eb,
                request,
                Contexts.INSTRUCTION.toString(),
                Actions.DELETE.toString(),
                instructionIds.toString(),
                new JsonObject().put("ids", instructionIds))));
    }

    @Get("/instructions/:id/export")
    @ApiDoc("Export given instruction")
    public void exportInstruction(HttpServerRequest request) {
        new Instruction(Integer.parseInt(request.getParam("id")), config).export(event -> {
            if (event.isLeft()) {
                renderError(request);
            } else {
                Buffer file = event.right().getValue();
                request.response()
                        .putHeader("Content-Type", "application/vnd.ms-excel")
                        .putHeader("Content-Length", file.length() + "")
                        .putHeader("Content-Disposition", "attachment; filename=Récapitulatif_mesures_engagées.xlsx")
                        .write(file);
            }
        });
    }
}
