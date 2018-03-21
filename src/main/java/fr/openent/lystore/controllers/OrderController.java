package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.logging.Actions;
import fr.openent.lystore.logging.Contexts;
import fr.openent.lystore.logging.Logging;
import fr.openent.lystore.security.AccessOrderRight;
import fr.openent.lystore.security.ManagerRight;
import fr.openent.lystore.service.ExportPDFService;
import fr.openent.lystore.service.OrderService;
import fr.openent.lystore.service.UserInfoService;
import fr.openent.lystore.service.impl.DefaultExportPDFService;
import fr.openent.lystore.service.impl.DefaultOrderService;
import fr.openent.lystore.service.impl.DefaultUserInfoService;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Put;
import fr.wseduc.webutils.security.XssHttpServerRequest;
import org.entcore.common.user.UserInfos;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.email.EmailSender;
import org.entcore.common.email.EmailFactory;
import fr.wseduc.webutils.request.RequestUtils;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;

import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.vertx.java.core.json.JsonArray;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;


import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;
public class OrderController extends ControllerHelper {
    private OrderService orderService;
    public static final String UTF8_BOM = "\uFEFF";
    private ExportPDFService exportPDFService;
    private UserInfoService userInfoService;

    @Override
    public void init(Vertx vertx, final Container container, RouteMatcher rm,
                     Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
        super.init(vertx, container, rm, securedActions);
        EmailFactory emailFactory = new EmailFactory(vertx, container, container.config());
        EmailSender emailSender = emailFactory.getSender();
        this.orderService = new DefaultOrderService(Lystore.lystoreSchema, "order_client_equipment", emailSender);
        this.exportPDFService = new DefaultExportPDFService( eb, vertx, container);
        this.userInfoService = new DefaultUserInfoService();
    }

    @Get("/orders/:idCampaign/:idStructure")
    @ApiDoc("Get the list of orders by idCampaign and idstructure")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessOrderRight.class)
    public void listOrdersByCampaignByStructure(final HttpServerRequest request){
        try {
            Integer idCampaign = Integer.parseInt(request.params().get("idCampaign"));
            String idStructure = request.params().get("idStructure");
            orderService.listOrder(idCampaign,idStructure, arrayResponseHandler(request));
        }catch (ClassCastException e ){
            log.error("An error occured when casting campaign id ",e);
        }
    }

    @Get("/orders")
    @ApiDoc("Get the list of orders")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void listOrders (HttpServerRequest request){
        orderService.listOrder(arrayResponseHandler(request));
    }

    @Delete("/order/:idOrder/:idStructure")
    @ApiDoc("Delete a order item")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessOrderRight.class)
    public void deleteOrder(final HttpServerRequest request){
        try {
            final Integer idOrder = Integer.parseInt(request.params().get("idOrder"));
            final String idStructure = request.params().get("idStructure");
            orderService.orderForDelete(idOrder, new Handler<Either<String, JsonObject>>() {
                @Override
                public void handle(Either<String, JsonObject> order) {
                    if(order.isRight()){
                        orderService.deleteOrder(idOrder,order.right().getValue(),idStructure,
                                Logging.defaultResponseHandler(eb,request, Contexts.ORDER.toString(),
                                        Actions.DELETE.toString(),"idOrder",order.right().getValue()));
                    }
                }
            });

        } catch (ClassCastException e){
            log.error("An error occurred when casting order id", e);
            badRequest(request);
        }
    }

    @Get("/orders/export/:idCampaign/:idStructure")
    @ApiDoc("Export list of custumer's orders as CSV")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessOrderRight.class)
    public void export (final HttpServerRequest request){
        Integer idCampaign = Integer.parseInt(request.params().get("idCampaign"));
        String idStructure = request.params().get("idStructure");
        orderService.listExport(idCampaign, idStructure, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if(event.isRight()){
                    request.response()
                            .putHeader("Content-Type", "text/csv; charset=utf-8")
                            .putHeader("Content-Disposition", "attachment; filename=orders.csv")
                            .end(generateExport(request, event.right().getValue()));

                }else{
                    badRequest(request);
                }
            }
        });

    }

    private static String generateExport (HttpServerRequest request, JsonArray orders)  {
        StringBuilder report = new StringBuilder(UTF8_BOM).append(getExportHeader(request));
        for (int i = 0; i < orders.size(); i++) {
            report.append(generateExportLine(request, (JsonObject) orders.get(i)));
        }
        return report.toString();
    }

    private static String getExportHeader(HttpServerRequest request){
        return I18n.getInstance().translate("creation.date",getHost(request), I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("name.equipment",getHost(request),I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("quantity",getHost(request),I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("price.equipment",getHost(request),I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate("status",getHost(request),I18n.acceptLanguage(request))
                + "\n";
    }


    private static String generateExportLine(HttpServerRequest request, JsonObject order)  {
        SimpleDateFormat formatterDate = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        SimpleDateFormat formatterDateCsv = new SimpleDateFormat("dd/MM/yyyy");
        Date orderDate = null;
        try {
            orderDate = formatterDate.parse( order.getString("equipment_creation_date"));

        } catch (ParseException e) {
            log.error( "Error current format date" + e);
        }
        return formatterDateCsv.format(orderDate) + ";" +
                order.getString("equipment_name") + ";" +
                order.getNumber("equipment_quantity") + ";" +
                order.getString("price_total_equipment") + " " + I18n.getInstance().
                translate("money.symbol",getHost(request),I18n.acceptLanguage(request)) + ";" +
                I18n.getInstance().translate(order.getString("equipment_status"),getHost(request),
                        I18n.acceptLanguage(request)) + ";" +
                "\n";
    }


    @Put("/orders/valid")
    @ApiDoc("validate orders ")
    @ResourceFilter(ManagerRight.class)
    public void validateOrders (final HttpServerRequest request){
        RequestUtils.bodyToJson(request, pathPrefix + "orderIds", new Handler<JsonObject>() {
            @Override
            public void handle(final JsonObject orders) {
                UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
                    @Override
                    public void handle(UserInfos userInfos) {
                        try {
                            List<String> params = new ArrayList<>();
                            for (Object id: orders.getArray("ids") ) {
                                params.add( id.toString());
                            }
                            final int urlNumber = 7;
                            List<Integer> ids = SqlQueryUtils.getIntegerIds(params);
                            orderService.validateOrders(request, userInfos , ids,
                                    (String)((XssHttpServerRequest) request)
                                            .headers().entries().get(urlNumber).getValue(),
                                    Logging.defaultResponsesHandler(eb,
                                            request,
                                            Contexts.ORDER.toString(),
                                            Actions.UPDATE.toString(),
                                            params,
                                            null));
                        } catch (ClassCastException e) {
                            log.error("An error occurred when casting order id", e);
                        }
                    }
                });
            }
        });

    }

    @Put("/orders/sent")
    @ApiDoc("send orders ")
    @ResourceFilter(ManagerRight.class)
    public void sendOrders (final HttpServerRequest request){
        RequestUtils.bodyToJson(request, pathPrefix + "orderIds", new Handler<JsonObject>() {
            @Override
            public void handle(final JsonObject orders) {
                userInfoService.getUserInfo(orders.getString("userId"), new Handler<Either<String, JsonArray>>() {
                    @Override
                    public void handle(final Either<String, JsonArray> user) {
                try {
                            final List<String> params = new ArrayList<>();
                            for (Object id: orders.getArray("ids") ) {
                                params.add( id.toString());
                            }
                            final String nbrBc = orders.getString("bc_number");
                            final String nbrEngagement = orders.getString("engagement_number");
                            final String dateGeneration = orders.getString("dateGeneration");
                            final JsonObject supplier = orders.getObject("supplier");
                            final List<Integer> ids = SqlQueryUtils.getIntegerIds(params);
                            orderService.sendOrders(ids,
                                    new Handler<Either<String, JsonObject>>() {
                                        @Override
                                        public void handle(Either<String, JsonObject> stringJsonObjectEither) {
                                            JsonObject object = stringJsonObjectEither.right().getValue()
                                                    .putString("nbr_bc", nbrBc)
                                                    .putString("nbr_engagement", nbrEngagement)
                                                    .putString("date_generation", dateGeneration)
                                                    .putObject("me",
                                                            makeUserObject((JsonObject) user.right().getValue().get(0)))
                                                    .putObject("supplier", supplier);
                                            exportPDFService.generatePDF(request, object,
                                                    "BC.xhtml", "Bon_Commande_",
                                                    new Handler<Buffer>() {
                                                        @Override
                                                        public void handle(final Buffer pdf) {
                                                            request.response().end(pdf);
                                                        }
                                                    }
                                            );
                                        }
                                    });
                        } catch (ClassCastException e) {
                            log.error("An error occurred when casting order id", e);
                        }
                    }
                });
            }
        });

    }
    private JsonObject makeUserObject(JsonObject user) {
        JsonObject dataUser = user.getObject("u").getObject("data");
     return   new JsonObject()
                .putString("name", dataUser.getString("firstName")+" "+ dataUser.getString("lastName") )
                .putString("email", dataUser.getString("emailAcademy"))
             .putString("phone", dataUser.getString("mobile"));
    }
    @Put("/orders/done")
    @ApiDoc("Wind up orders ")
    @ResourceFilter(ManagerRight.class)
    public void windUpOrders (final HttpServerRequest request){
        RequestUtils.bodyToJson(request, pathPrefix + "orderIds", new Handler<JsonObject>() {
            @Override
            public void handle(final JsonObject orders) {
                try {
                    List<String> params = new ArrayList<>();
                    for (Object id: orders.getArray("ids") ) {
                        params.add( id.toString());
                    }
                    List<Integer> ids = SqlQueryUtils.getIntegerIds(params);
                    orderService.windUpOrders(ids, Logging.defaultResponsesHandler(eb,
                            request,
                            Contexts.ORDER.toString(),
                            Actions.UPDATE.toString(),
                            params,
                            null)

                    );
                } catch (ClassCastException e) {
                    log.error("An error occurred when casting order id", e);
                }
            }
        });

    }
}
