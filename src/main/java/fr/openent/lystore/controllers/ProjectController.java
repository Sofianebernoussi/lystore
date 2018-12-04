package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.logging.Actions;
import fr.openent.lystore.logging.Contexts;
import fr.openent.lystore.logging.Logging;
import fr.openent.lystore.service.ProjectService;
import fr.openent.lystore.service.impl.DefaultProjectService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

public class ProjectController extends ControllerHelper {

    private final ProjectService projectService;

    public ProjectController() {
        super();
        projectService = new DefaultProjectService(Lystore.lystoreSchema, "project");
    }

    @Get("/projects")
    @ApiDoc("Get list of the projects")
    @SecuredAction(value = " ", type = ActionType.AUTHENTICATED)
    public void getProjects(HttpServerRequest request) {
        projectService.getProjects(arrayResponseHandler(request));
    }

    @Get("/project/:id")
    @ApiDoc("Get one project")
    @SecuredAction(value = " ", type = ActionType.AUTHENTICATED)
    public void getProject(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject project) {
                try {
                    Integer id = Integer.parseInt(request.params().get("id"));
                    projectService.getProject(id, defaultResponseHandler(request));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Post("/project")
    @ApiDoc("Create a project")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void createProject(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "project", new Handler<JsonObject>() {

            @Override
            public void handle(JsonObject projet) {
                projectService.createProject(projet, Logging.defaultResponseHandler(eb,
                        request,
                        Contexts.PROJECT.toString(),
                        Actions.CREATE.toString(),
                        null,
                        projet));
            }
        });
    }

}
