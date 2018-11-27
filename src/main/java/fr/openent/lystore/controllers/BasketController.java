package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.logging.Actions;
import fr.openent.lystore.logging.Contexts;
import fr.openent.lystore.logging.Logging;
import fr.openent.lystore.security.AccessOrderCommentRight;
import fr.openent.lystore.security.AccessPriceProposalRight;
import fr.openent.lystore.security.PersonnelRight;
import fr.openent.lystore.service.BasketService;
import fr.openent.lystore.service.impl.DefaultBasketService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;

public class BasketController extends ControllerHelper {
    private final BasketService basketService;

    public BasketController (Vertx vertx, JsonObject slackConfiguration) {
        super();
        this.basketService = new DefaultBasketService(Lystore.lystoreSchema, "basket", vertx, slackConfiguration);
    }
    @Get("/basket/:idCampaign/:idStructure")
    @ApiDoc("List  basket liste of a campaigne and a structure")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
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
    public void delete(HttpServerRequest request) {
        try {
            Integer idBasket = request.params().contains("idBasket")
                    ? Integer.parseInt(request.params().get("idBasket"))
                    : null;
            basketService.delete( idBasket, defaultResponseHandler(request));

        } catch (ClassCastException e) {
            log.error("An error occurred when casting basket id", e);
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
                    log.error("An error occurred when casting basket id", e);
                }
            }
        });
    }

    @Put("/basket/:idBasket/comment")
    @ApiDoc("Update a basket's comment")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessOrderCommentRight.class)
    public void updateComment(final HttpServerRequest request){
        RequestUtils.bodyToJson(request,  new Handler<JsonObject>(){
            @Override
            public void handle(JsonObject basket){
                if (!basket.containsKey("comment")) {
                    badRequest(request);
                    return;
                }
                try {
                    Integer id = Integer.parseInt(request.params().get("idBasket"));
                    String comment = basket.getString("comment");
                    basketService.updateComment(id, comment, defaultResponseHandler(request));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Put("/basket/:idBasket/priceProposal")
    @ApiDoc("Update the price proposal of a basket")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessPriceProposalRight.class)
    public void updatePriceProposal(final HttpServerRequest request) {

        RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject basket) {
                if (!basket.containsKey("price_proposal")) {
                    badRequest(request);
                    return;
                }
                try {
                    Integer id = Integer.parseInt(request.params().get("idBasket"));
                    Float price_proposal = basket.getFloat("price_proposal");
                    basketService.updatePriceProposal(id, price_proposal, defaultResponseHandler(request));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                } catch (java.lang.NullPointerException e) {
                    Integer id = Integer.parseInt(request.params().get("idBasket"));
                    basketService.updatePriceProposal(id, null, defaultResponseHandler(request));
                }
            }
        });
    }

    @Post("/baskets/to/orders")
    @ApiDoc("crearte an order liste from basket")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(PersonnelRight.class)
    public void takeOrder(final HttpServerRequest  request){
        RequestUtils.bodyToJson( request, pathPrefix + "basketToOrder", new Handler<JsonObject>() {
            @Override
            public void handle(JsonObject object) {
                try {
                    final Integer idCampaign = object.getInteger("id_campaign");
                    final String idStructure = object.getString("id_structure");
                    final String nameStructure = object.getString("structure_name");
                    basketService.listebasketItemForOrder(idCampaign, idStructure,
                            new Handler<Either<String, JsonArray>>() {
                                @Override
                                public void handle(Either<String, JsonArray> listBasket) {
                                    if(listBasket.isRight() && listBasket.right().getValue().size() > 0){
                                        basketService.takeOrder(request , listBasket.right().getValue(),
                                                idCampaign , idStructure, nameStructure,
                                                Logging.defaultCreateResponsesHandler(eb,
                                                        request,
                                                        Contexts.ORDER.toString(),
                                                        Actions.CREATE.toString(),
                                                        "id_order",
                                                        listBasket.right().getValue()));

                                    }else{
                                        log.error("An error occurred when listing Baskets");
                                        badRequest(request);
                                    }
                                }
                            });

                } catch (ClassCastException e) {
                    log.error("An error occurred when casting Basket information", e);
                    renderError(request);
                }
            }
        });
    }
}
