package fr.openent.lystore.controllers;

import fr.openent.lystore.logging.Actions;
import fr.openent.lystore.logging.Contexts;
import fr.openent.lystore.logging.Logging;
import fr.openent.lystore.security.ManagerRight;
import fr.openent.lystore.service.OrderRegionService;
import fr.openent.lystore.service.impl.DefaultOrderRegionService;
import fr.openent.lystore.service.impl.DefaultOrderService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;

public class OrderRegionController extends BaseController {


    private OrderRegionService orderRegionService;

    private static final Logger LOGGER = LoggerFactory.getLogger (DefaultOrderService.class);

    public static final String UTF8_BOM = "\uFEFF";


    public OrderRegionController() {
        this.orderRegionService = new DefaultOrderRegionService("equipment");

    }


    @Post("/region/order")
    @ApiDoc("Create an order with id order client when admin or manager")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void createWithOrderClientAdminOrder(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos event) {
                RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject order) {
                        RequestUtils.bodyToJson(request, orderRegion ->  orderRegionService.setOrderRegion(order, event, Logging.defaultResponseHandler(eb,
                                request,
                                Contexts.ORDERREGION.toString(),
                                Actions.CREATE.toString(),
                                null,
                                orderRegion)));
                    }
                });
            }

        });
    }

    @Put("/region/order/:id")
    @ApiDoc("Update an order when admin or manager")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void updateAdminOrder(final HttpServerRequest request) {
        Integer idOrder = Integer.parseInt(request.getParam("id"));
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos event) {
                RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject order) {
                        RequestUtils.bodyToJson(request, orderRegion ->  orderRegionService.updateOrderRegion(order, idOrder, event, Logging.defaultResponseHandler(eb,
                                request,
                                Contexts.ORDERREGION.toString(),
                                Actions.UPDATE.toString(),
                                idOrder.toString(),
                                new JsonObject().put("orderRegion", orderRegion))));
                    }
                });
            }
        });
    }

    @Post("/region/orders/")
    @ApiDoc("Create orders from a region")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void createAdminOrder(final HttpServerRequest request) {
        try{
        UserUtils.getUserInfos(eb, request, user -> {
            RequestUtils.bodyToJson(request, orders -> {
                //todo make project with id tile in order
                if (!orders.isEmpty()) {
                    JsonArray ordersList = orders.getJsonArray("orders");
                    Integer id_title = ordersList.getJsonObject(0).getInteger("title_id");
                    orderRegionService.createProject(id_title, idProject -> {
                        //todo get id of project created
                        if(idProject.isRight()){
                            Integer idProjectRight = idProject.right().getValue().getInteger("id");
                            //todo async create log project
                            Logging.insert(eb,
                                    request,
                                    Contexts.PROJECT.toString(),
                                    Actions.CREATE.toString(),
                                    idProjectRight.toString(),
                                    new JsonObject().put("id", idProjectRight).put("id_title", id_title));
                            //todo async create multi orders with loop and create log order
                            //todo loop orderList with request createOrdersRegion and request when exist

                            // todo send response
                            request.response().setStatusCode(204).end();

                        } else {
                            LOGGER.error("An error when you want get id after create project ");
                        }
                    });
                }
            });
        });
        } catch( Exception e){
            LOGGER.error("An error when you want create order region and project", e);
        }
    };

    @Delete("/region/:id/order")
    @ApiDoc("delete order by id order region ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void deleteOrderRegion(final HttpServerRequest request) {
        Integer idRegion = Integer.parseInt(request.getParam("id"));
        orderRegionService.deleteOneOrderRegion(idRegion, Logging.defaultResponseHandler(eb,
                request,
                Contexts.ORDERREGION.toString(),
                Actions.DELETE.toString(),
                idRegion.toString(),
                new JsonObject().put("idRegion", idRegion)));
    }

    @Get("/orderRegion/:id/order")
    @ApiDoc("get order by id order region ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void getOneOrder(HttpServerRequest request) {
        Integer idOrder = Integer.parseInt(request.getParam("id"));
        orderRegionService.getOneOrderRegion(idOrder, defaultResponseHandler(request));
    }

    @Put("/order/region/:idOperation/operation")
    @ApiDoc("update operation in orders region")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void updateOperation(final HttpServerRequest request) {
        final Integer idOperation = Integer.parseInt(request.params().get("idOperation"));
        RequestUtils.bodyToJsonArray(request, idsOrders -> orderRegionService.updateOperation(idOperation, idsOrders, Logging.defaultResponseHandler(eb,
                request,
                Contexts.ORDER.toString(),
                Actions.UPDATE.toString(),
                idOperation.toString(),
                new JsonObject().put("ids", idsOrders))));
    }
}
