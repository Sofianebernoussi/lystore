package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.logging.Actions;
import fr.openent.lystore.logging.Contexts;
import fr.openent.lystore.logging.Logging;
import fr.openent.lystore.security.AdministratorRight;
import fr.openent.lystore.service.AgentService;
import fr.openent.lystore.service.impl.DefaultAgentService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;

public class AgentController extends ControllerHelper {

    private AgentService agentService;

    public AgentController () {
        super();
        this.agentService = new DefaultAgentService(Lystore.LYSTORE_SCHEMA, "agent");
    }

    @Get("/agents")
    @ApiDoc("Returns all agents in database")
    @SecuredAction(Lystore.MANAGER_RIGHT)
    public void getAgents (HttpServerRequest request) {
        agentService.getAgents(arrayResponseHandler(request));
    }

    @Post("/agent")
    @ApiDoc("Create an agent")
    @SecuredAction(Lystore.ADMINISTRATOR_RIGHT)
    public void createAgent (final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            public void handle(final UserInfos user) {
                RequestUtils.bodyToJson(request, pathPrefix + "agent", new Handler<JsonObject>() {
                    public void handle(JsonObject body) {
                        agentService.createAgent(body, Logging.defaultResponseHandler(eb,
                                request,
                                Contexts.AGENT.toString(),
                                Actions.CREATE.toString(),
                                null,
                                body));
                    }
                });
            }
        });
    }

    @Put("/agent/:id")
    @ApiDoc("Update an agent based on provided id")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void updateAgent (final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "agent", new Handler<JsonObject>() {
            public void handle(JsonObject body) {
                try {
                    agentService.updateAgent(Integer.parseInt(request.params().get("id")), body,
                            Logging.defaultResponseHandler(eb,
                                    request,
                                    Contexts.AGENT.toString(),
                                    Actions.UPDATE.toString(),
                                    request.params().get("id"),
                                    body));
                } catch (ClassCastException e) {
                    log.error("An error occurred when casting agent id");
                    badRequest(request);
                }
            }
        });
    }


    @Delete("/agent")
    @ApiDoc("Delete and agent based on provided id")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void deleteAgent (HttpServerRequest request) {
        try{
            List<String> params = request.params().getAll("id");
            if (params.size() > 0) {
                List<Integer> ids = new ArrayList<Integer>();
                for (String param : params) {
                    ids.add(Integer.parseInt(param));
                }
                agentService.deleteAgent(ids, Logging.defaultResponseHandler(eb,
                        request,
                        Contexts.AGENT.toString(),
                        Actions.DELETE.toString(),
                        Logging.mergeItemsIds(params),
                        null));
            } else {
                badRequest(request);
            }
        }
        catch (ClassCastException e) {
            log.error("An error occurred when casting agent id");
            badRequest(request);
        }
    }
}
