package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.security.PersonnelRight;
import fr.openent.lystore.service.BasketService;
import fr.openent.lystore.service.impl.DefaultBasketService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.http.filter.ResourceFilter;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.vertx.java.core.json.JsonObject;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;

public class BasketController extends ControllerHelper {
    private final BasketService basketService;

    public BasketController () {
        super();
        this.basketService = new DefaultBasketService(Lystore.lystoreSchema, "basket");
    }
    @Get("/basket/:idCampaign/:idStructure")
    @ApiDoc("List  basket liste of a campaigne and a structure")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    @Override
    public void list(HttpServerRequest request) {
        try {
            Integer idCampaign = request.params().contains("idCampaign")
                    ? Integer.parseInt(request.params().get("idCampaign"))
                    : null;
            String idStructure = request.params().contains("idStructure")
                    ? request.params().get("idStructure")
                    : null;
            basketService.listBasket( idCampaign, idStructure, arrayResponseHandler(request));
        } catch (ClassCastException e) {
            log.error("An error occurred casting campaign id", e);
        }
    }

    @Post("/basket")
    @ApiDoc("Create a basket item")
    @SecuredAction(value =  "", type = ActionType.RESOURCE)
    @ResourceFilter(PersonnelRight.class)
    @Override
    public void create(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "basket", new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject basket) {
                basketService.create(basket, defaultResponseHandler(request) );
            }
        });
    }

    @Delete("/basket/:idBasket")
    @ApiDoc("Delete a basket item")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(PersonnelRight.class)
    @Override
    public void delete(HttpServerRequest request) {
        try {
            Integer idCampaign = request.params().contains("idBasket")
                    ? Integer.parseInt(request.params().get("idBasket"))
                    : null;
            basketService.delete( idCampaign, defaultResponseHandler(request));

        } catch (ClassCastException e) {
            log.error("An error occurred when casting equipment(s) id(s)", e);
            badRequest(request);
        }
    }
    @Put("/basket/:idBasket/amount")
    @ApiDoc("Update a basket's amount")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(PersonnelRight.class)
    public void updateAmount(final HttpServerRequest  request){
        RequestUtils.bodyToJson(request, pathPrefix + "basket", new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject basket) {
                try {
                    Integer id = Integer.parseInt(request.params().get("idBasket"));
                    Integer amount = basket.getInteger("amount") ;
                    basketService.updateAmount(id, amount,  defaultResponseHandler(request));
                } catch (ClassCastException e) {
                    log.error("An error occurred when casting equipment id", e);
                }
            }
        });
    }
}
