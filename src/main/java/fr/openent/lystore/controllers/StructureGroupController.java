package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.logging.Actions;
import fr.openent.lystore.logging.Contexts;
import fr.openent.lystore.logging.Logging;
import fr.openent.lystore.security.AdministratorRight;
import fr.openent.lystore.service.StructureGroupService;
import fr.openent.lystore.service.impl.DefaultStructureGroupService;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.http.filter.ResourceFilter;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;

import java.util.List;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

/**
 * Created by agnes.lapeyronnie on 04/01/2018.
 */
public class StructureGroupController extends ControllerHelper{

    private StructureGroupService structureGroupService;

    public StructureGroupController (){
        super();
        this.structureGroupService = new DefaultStructureGroupService(Lystore.LYSTORE_SCHEMA,"structure_group");
    }

    @Get("/structure/groups")
    @ApiDoc("List all goups of structures")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void list(final HttpServerRequest request){
        structureGroupService.listStructureGroups(arrayResponseHandler(request));
    }

    @Post("/structure/group")
    @ApiDoc("Create a group of Structures")
    @SecuredAction(value="", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void create(final HttpServerRequest request){
        RequestUtils.bodyToJson(request, pathPrefix + "structureGroup", new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject structureGroup) {
                structureGroupService.create(structureGroup, Logging.defaultResponseHandler(eb,
                        request,
                        Contexts.STRUCTUREGROUP.toString(),
                        Actions.CREATE.toString(),
                        null,
                        structureGroup));

            }
        });
    }

    @Put("/structure/group/:id")
    @ApiDoc("Update a group of strctures")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void update (final HttpServerRequest request){
        RequestUtils.bodyToJson(request, pathPrefix + "structureGroup", new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject structureGroup) {
                try {
                    Integer id = Integer.parseInt(request.params().get("id"));
                    structureGroupService.update(id, structureGroup, Logging.defaultResponseHandler(eb,
                            request,
                            Contexts.STRUCTUREGROUP.toString(),
                            Actions.UPDATE.toString(),
                            request.params().get("id"),
                            structureGroup));
                }catch (ClassCastException e){
                    //TODO
                    log.error("An error occured when casting structureGroup id");
                    badRequest(request);
                }
            }
        });
    }

    @Delete("/structure/group")
    @ApiDoc("Delete a group of Structures")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void delete(final HttpServerRequest request){
        try{
            List<String> params = request.params().getAll("id");
            if(params.size() > 0){
                List<Integer> ids = SqlQueryUtils.getIntegerIds(params);
                structureGroupService.delete(ids,Logging.defaultResponsesHandler(eb,
                        request,
                        Contexts.STRUCTUREGROUP.toString(),
                        Actions.DELETE.toString(),
                       params,
                        null));
            }
        }catch(ClassCastException e){
            //TODO
            log.error("An error occurred when casting group(s) of structures id(s)");
            badRequest(request);
        }
    }

}
