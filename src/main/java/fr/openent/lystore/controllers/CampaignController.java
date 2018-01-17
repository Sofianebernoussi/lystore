package fr.openent.lystore.controllers;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import fr.openent.lystore.Lystore;
import fr.openent.lystore.logging.Actions;
import fr.openent.lystore.logging.Contexts;
import fr.openent.lystore.logging.Logging;
import fr.openent.lystore.security.AdministratorRight;
import fr.openent.lystore.security.ManagerRight ;
import fr.openent.lystore.service.CampaignService;
import fr.openent.lystore.service.impl.DefaultCampaignService;
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
import fr.openent.lystore.service.CampaignService;
import fr.openent.lystore.service.impl.DefaultCampaignService;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;

public class CampaignController extends ControllerHelper {

    private final CampaignService campaignService;

    public CampaignController () {
        super();
        this.campaignService = new DefaultCampaignService(Lystore.LYSTORE_SCHEMA, "campagne");
    }

    @Get("/campaigns")
    @ApiDoc("List all campaigns in database")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    @ResourceFilter(ManagerRight.class )
    public void list(HttpServerRequest request) {
        campaignService.listCampaigns(arrayResponseHandler(request));
    }

    @Get("/campaigns/:id")
    @ApiDoc("List all campaigns in database")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    @ResourceFilter(ManagerRight.class )
    public void campaign(HttpServerRequest request) {
        try {
            Integer id = Integer.parseInt(request.params().get("id"));
        campaignService.getCampaign(id, defaultResponseHandler(request));
        } catch (ClassCastException e) {
            log.error(" An error occurred when casting campaign id");
        }
    }

    @Post("/campaign")
    @ApiDoc("Create a campaign")
    @SecuredAction(value =  "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void create(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "campaign", new Handler<JsonObject>() {
            public void handle(JsonObject campaign) {
                campaignService.create(campaign, Logging.defaultResponseHandler(eb,
                        request,
                        Contexts.CAMPAIGN.toString(),
                        Actions.CREATE.toString(),
                        null,
                        campaign));
            }
        });
    }

    @Put("/campaign/accessibility/:id")
    @ApiDoc("Update an accessibility campaign")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void updateAccessibility(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "campaign", new Handler<JsonObject>() {
            public void handle(JsonObject campaign) {
                try {
                    Integer id = Integer.parseInt(request.params().get("id"));
                    campaignService.updateAccessibility(id, campaign, Logging.defaultResponseHandler(eb,
                            request,
                            Contexts.CAMPAIGN.toString(),
                            Actions.UPDATE.toString(),
                            request.params().get("id"),
                            campaign));
                } catch (ClassCastException e) {
                    log.error(" An error occurred when casting campaign id");
                }
            }
        });
    }

    @Put("/campaign/:id")
    @ApiDoc("Update a campaign")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void update(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "campaign", new Handler<JsonObject>() {
            public void handle(JsonObject campaign) {
                try {
                    Integer id = Integer.parseInt(request.params().get("id"));
                    campaignService.update(id, campaign, Logging.defaultResponseHandler(eb,
                            request,
                            Contexts.CAMPAIGN.toString(),
                            Actions.UPDATE.toString(),
                            request.params().get("id"),
                            campaign));
                } catch (ClassCastException e) {
                    log.error(" An error occurred when casting campaign id");
                }
            }
        });
    }
    @Delete("/campaign")
    @ApiDoc("Delete a campaign")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void delete(HttpServerRequest request) {
        try{
            List<String> params = request.params().getAll("id");
            if (params.size() > 0) {
                List<Integer> ids = SqlQueryUtils.getIntegerIds(params);
                campaignService.delete(ids, Logging.defaultResponsesHandler(eb,
                        request,
                        Contexts.CAMPAIGN.toString(),
                        Actions.DELETE.toString(),
                        params,
                        null));
            } else {
                badRequest(request);
            }
        } catch (ClassCastException e) {
            log.error(" An error occurred when casting campaign(s) id(s)");
            badRequest(request);
        }
    }
}
