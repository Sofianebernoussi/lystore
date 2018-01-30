package fr.openent.lystore.controllers;

import fr.openent.lystore.service.impl.DefaultStructureService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import org.entcore.common.controller.ControllerHelper;
import org.vertx.java.core.http.HttpServerRequest;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;


/**
 * Created by agnes.lapeyronnie on 09/01/2018.
 */
public class StructureController extends ControllerHelper {

    private DefaultStructureService structureService;

    public StructureController(){
        super();
        this.structureService = new DefaultStructureService();
    }

    @Get("/structures")
    @ApiDoc("Returns all structures")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getStructures(HttpServerRequest request){
        structureService.getStructures(arrayResponseHandler(request));
    }

}
