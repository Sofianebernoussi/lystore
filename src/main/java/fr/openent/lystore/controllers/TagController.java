package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.security.AdministratorRight;
import fr.openent.lystore.service.TagService;
import fr.openent.lystore.service.impl.DefaultTagService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.*;

public class TagController extends ControllerHelper {

    private final TagService tagService;

    public TagController() {
        super();
        this.tagService = new DefaultTagService(Lystore.LYSTORE_SCHEMA, "tag");
    }

    @Get("/tags")
    @ApiDoc("List all tags in database")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getAll(HttpServerRequest request) {
        tagService.getAll(arrayResponseHandler(request));
    }

    @Post("/tag")
    @ApiDoc("Create a tag")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void create(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "tag", new Handler<JsonObject>() {
            public void handle(JsonObject tag) {
                tagService.create(tag, defaultResponseHandler(request));
            }
        });
    }

    @Put("/tag/:id")
    @ApiDoc("Update a tag")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void update(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "tag", new Handler<JsonObject>() {
            public void handle(JsonObject tag) {
                try {
                    Integer id = Integer.parseInt(request.params().get("id"));
                    tagService.update(id, tag, defaultResponseHandler(request));
                } catch (ClassCastException e) {
                    log.error("E013 : An error occurred when casting tag id");
                    badRequest(request);
                }
            }
        });
    }

    @Delete("/tag")
    @ApiDoc("Delete a tag")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void delete(HttpServerRequest request) {
        try{
            List<String> params = request.params().getAll("id");
            if (params.size() > 0) {
                List<Integer> ids = new ArrayList<Integer>();
                for (String param : params) {
                    ids.add(Integer.parseInt(param));
                }
                tagService.delete(ids, defaultResponseHandler(request));
            } else {
                badRequest(request);
            }
        }
        catch (ClassCastException e) {
            log.error("E020 : An error occurred when casting tag id");
            badRequest(request);
        }
    }

}
