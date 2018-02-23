package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.logging.Actions;
import fr.openent.lystore.logging.Contexts;
import fr.openent.lystore.logging.Logging;
import fr.openent.lystore.security.AccessOrderRight;
import fr.openent.lystore.security.ManagerRight;
import fr.openent.lystore.service.OrderService;
import fr.openent.lystore.service.impl.DefaultOrderService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

public class OrderController extends ControllerHelper {
    private final OrderService orderService;
    public static final String UTF8_BOM = "\uFEFF";

    public OrderController (){
        super();
        this.orderService = new DefaultOrderService(Lystore.lystoreSchema, "order_client_equipment");
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

    @Get("/orders")
    @ApiDoc("Get the list of orders")
    @ResourceFilter(ManagerRight.class)
    public void listOrders (HttpServerRequest request){
        orderService.listOrder(arrayResponseHandler(request));
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
}
