package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.logging.Actions;
import fr.openent.lystore.logging.Contexts;
import fr.openent.lystore.logging.Logging;
import fr.openent.lystore.security.AdministratorRight;
import fr.openent.lystore.security.ManagerRight;
import fr.openent.lystore.service.OperationService;
import fr.openent.lystore.service.impl.DefaultOperationService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;


import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

public class OperationController  extends ControllerHelper {
    private OperationService operationService;

    public OperationController () {
        super();
        this.operationService = new DefaultOperationService(Lystore.lystoreSchema, "operation");
    }

    @Get("/labels")
    @ApiDoc("Returns all labels in database")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getLabels (HttpServerRequest request) {
        operationService.getLabels(arrayResponseHandler(request));
    }

    @Get("/operations")
    @ApiDoc("List all operations in database")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getOperations(HttpServerRequest request) {
        operationService.getOperations(arrayResponseHandler(request));
    }

    @Post("/operation")
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

//    @Delete("/operation")
//    @ApiDoc("Delete operations")
//    @SecuredAction(value = "", type = ActionType.RESOURCE)
//    @ResourceFilter(AdministratorRight.class)
//    @Override
//    public void create(final HttpServerRequest request) {
//        RequestUtils.bodyToJson(request, pathPrefix + "operation", operation -> operationService.create(operation, Logging.defaultResponseHandler(eb,
//                request,
//                Contexts.OPERATION.toString(),
//                Actions.CREATE.toString(),
//                null,
//                operation)));
//    }
}
