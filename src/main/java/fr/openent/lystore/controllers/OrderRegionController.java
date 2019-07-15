package fr.openent.lystore.controllers;

import fr.openent.lystore.security.ManagerRight;
import fr.openent.lystore.service.OrderRegionService;
import fr.openent.lystore.service.impl.DefaultOrderRegionService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Post;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.BaseController;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;

public class OrderRegionController extends BaseController {


    private Storage storage;
    private OrderRegionService orderRegionService;


    public static final String UTF8_BOM = "\uFEFF";


    public OrderRegionController() {
        this.orderRegionService = new DefaultOrderRegionService("equipment");
    }


    @Put("/region/order/")
    @ApiDoc("update or create an order when admin or manager")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void updateAdminOrder(final HttpServerRequest request) {

        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos event) {
                RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject order) {
                        orderRegionService.setOrderRegion(order, event, defaultResponseHandler(request));

                    }
                });
            }

        });
    }

    @Post("/region/order/")
    @ApiDoc("Create a region from scratch")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void createAdminOrder(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos event) {
                RequestUtils.bodyToJson(request, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject order) {
                        orderRegionService.createOrderRegion(order, event, defaultResponseHandler(request));

                    }
                });
            }
        });
    }
}
