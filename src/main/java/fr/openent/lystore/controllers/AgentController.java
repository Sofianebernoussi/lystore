package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.AgentService;
import fr.openent.lystore.service.impl.DefaultAgentService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.*;

public class AgentController extends ControllerHelper {

    private AgentService agentService;

    public AgentController () {
        super();
        this.agentService = new DefaultAgentService(Lystore.LYSTORE_SCHEMA, "agent");
    }

    @Get("/agents")
    @ApiDoc("Returns all agents in database")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getAgents (HttpServerRequest request) {
        agentService.getAgents(arrayResponseHandler(request));
    }

    //TODO Gérer la sécurité
    @Post("/agent")
    @ApiDoc("Create an agent")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void createAgent (final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            public void handle(final UserInfos user) {
                RequestUtils.bodyToJson(request, pathPrefix + "agent", new Handler<JsonObject>() {
                    public void handle(JsonObject body) {
                        agentService.createAgent(body, defaultResponseHandler(request));
                    }
                });
            }
        });
    }

    //TODO Gérer la sécurité
    @Put("/agent/:id")
    @ApiDoc("Update an agent based on provided id")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void updateAgent (final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "agent", new Handler<JsonObject>() {
            public void handle(JsonObject body) {
                try {
                    agentService.updateAgent(Integer.parseInt(request.params().get("id")), body,
                            defaultResponseHandler(request));
                } catch (ClassCastException e) {
                    badRequest(request);
                }
            }
        });
    }

    //TODO Gérer la sécurité
    @Delete("/agent")
    @ApiDoc("Delete and agent based on provided id")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void deleteAgent (HttpServerRequest request) {
        try{
            List<String> params = request.params().getAll("id");
            if (params.size() > 0) {
                List<Integer> ids = new ArrayList<Integer>();
                for (int i = 0; i < params.size(); i++) {
                    ids.add(Integer.parseInt(params.get(i)));
                }
                agentService.deleteAgent(ids, defaultResponseHandler(request));
            } else {
                badRequest(request);
            }
        }
        catch (ClassCastException e) {
            badRequest(request);
        }
    }
}
