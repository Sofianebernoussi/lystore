package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.security.AdministratorRight;
import fr.openent.lystore.service.EquipmentService;
import fr.openent.lystore.service.impl.DefaultEquipmentService;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;

public class EquipmentController extends ControllerHelper {

    private final EquipmentService equipmentService;

    public EquipmentController () {
        super();
        this.equipmentService = new DefaultEquipmentService(Lystore.LYSTORE_SCHEMA, "equipment");
    }

    @Get("/equipments")
    @ApiDoc("List all equipments in database")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void list(HttpServerRequest request) {
        equipmentService.listEquipments(arrayResponseHandler(request));
    }

    @Post("/equipment")
    @ApiDoc("Create an equipment")
    @SecuredAction(value =  "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void create(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "equipment", new Handler<JsonObject>() {
            public void handle(JsonObject equipment) {
                equipmentService.create(equipment, defaultResponseHandler(request));
            }
        });
    }

    @Put("/equipment/:id")
    @ApiDoc("Update an equipment")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void update(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "equipment", new Handler<JsonObject>() {
            public void handle(JsonObject equipment) {
                try {
                    Integer id = Integer.parseInt(request.params().get("id"));
                    equipmentService.update(id, equipment, defaultResponseHandler(request));
                } catch (ClassCastException e) {
                    log.error("E026 : An error occurred when casting equipment id");
                }
            }
        });
    }

    @Delete("/equipment")
    @ApiDoc("Delete an equipment")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void delete(HttpServerRequest request) {
        try{
            List<String> params = request.params().getAll("id");
            if (params.size() > 0) {
                List<Integer> ids = SqlQueryUtils.getIntegerIds(params);
                equipmentService.delete(ids, defaultResponseHandler(request));
            } else {
                badRequest(request);
            }
        } catch (ClassCastException e) {
            log.error("E027 : An error occurred when casting equipment(s) id(s)");
            badRequest(request);
        }
    }
}
