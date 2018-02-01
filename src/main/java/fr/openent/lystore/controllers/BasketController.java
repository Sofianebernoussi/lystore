package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.logging.Actions;
import fr.openent.lystore.logging.Contexts;
import fr.openent.lystore.logging.Logging;
import fr.openent.lystore.security.PersonnelRight;
import fr.openent.lystore.service.BasketService;
import fr.openent.lystore.service.impl.DefaultBasketService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.http.filter.ResourceFilter;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.vertx.java.core.json.JsonObject;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

public class BasketController extends ControllerHelper {
    private final BasketService basketService;

    public BasketController () {
        super();
        this.basketService = new DefaultBasketService(Lystore.lystoreSchema, "basket");
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
                basketService.create(basket, Logging.defaultResponseHandler(eb,
                        request,
                        Contexts.BASKET.toString(),
                        Actions.CREATE.toString(),
                        null,
                        basket));
            }
        });
    }
}
