package fr.openent.lystore.controllers;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.logging.Actions;
import fr.openent.lystore.logging.Contexts;
import fr.openent.lystore.logging.Logging;
import fr.openent.lystore.security.AccessOrderRight;
import fr.openent.lystore.security.ManagerRight;
import fr.openent.lystore.service.*;
import fr.openent.lystore.service.impl.*;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;


public class OrderController extends ControllerHelper {

    private Storage storage;
    private OrderService orderService;
    private StructureService structureService;
    private SupplierService supplierService;
    private ExportPDFService exportPDFService;
    private ContractService contractService;
    private AgentService agentService;

    public static final String UTF8_BOM = "\uFEFF";


    public OrderController (Storage storage) {
        this.storage = storage;
    }
    public void init(Vertx vertx, final Container container, RouteMatcher rm,
                     Map<String, fr.wseduc.webutils.security.SecuredAction> securedActions) {
        super.init(vertx, container, rm, securedActions);
        EmailFactory emailFactory = new EmailFactory(vertx, container, container.config());
        EmailSender emailSender = emailFactory.getSender();
        this.orderService = new DefaultOrderService(Lystore.lystoreSchema, "order_client_equipment", emailSender);
        this.exportPDFService = new DefaultExportPDFService( eb, vertx, container);
        this.structureService = new DefaultStructureService();
        this.exportPDFService = new DefaultExportPDFService( eb, vertx, container);
        this.supplierService = new DefaultSupplierService(Lystore.lystoreSchema, "supplier");
        this.contractService = new DefaultContractService(Lystore.lystoreSchema, "contract");
        this.agentService = new DefaultAgentService(Lystore.lystoreSchema, "agent");
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
    public void listOrders (final HttpServerRequest request){
        if (request.params().contains("status")) {
            final String status = request.params().get("status");
            if ("valid".equals(status.toLowerCase())) {
                orderService.getOrdersGroupByValidationNumber("VALID", new Handler<Either<String, JsonArray>>() {
                    @Override
                    public void handle(Either<String, JsonArray> event) {
                        if (event.isRight()) {
                            final JsonArray orders = event.right().getValue();
                            orderService.getOrdersDetailsIndexedByValidationNumber(status, new Handler<Either<String, JsonArray>>() {
                                @Override
                                public void handle(Either<String, JsonArray> event) {
                                    if (event.isRight()) {
                                        JsonArray equipments = event.right().getValue();
                                        JsonObject mapNumberEquipments = initNumbersMap(orders);
                                        mapNumberEquipments = mapNumbersEquipments(equipments, mapNumberEquipments);
                                        JsonObject order;
                                        for (int i = 0; i < orders.size(); i++) {
                                            order = orders.get(i);
                                            order.putString("price",
                                                    String.valueOf(
                                                            roundWith2Decimals(getTotalOrder(mapNumberEquipments.getArray(order.getString("number_validation")))))
                                                            .replace(".", ","));
                                        }
                                        renderJson(request, orders);
                                    } else {
                                        badRequest(request);
                                    }
                                }
                            });
                        } else {
                            badRequest(request);
                        }
                    }
                });
            } else {
                orderService.listOrder(status, arrayResponseHandler(request));
            }
        } else {
            badRequest(request);
        }
    }

    /**
     * Init map with numbers validation
     * @param orders order list containing numbers
     * @return Map containing numbers validation as key and an empty array as value
     */
    private JsonObject initNumbersMap (JsonArray orders) {
        JsonObject map = new JsonObject();
        JsonObject item;
        for (int i = 0; i < orders.size(); i++) {
            item = orders.get(i);
            map.putArray(item.getString("number_validation"), new JsonArray());
        }
        return map;
    }

    /**
     * Map equipments with numbers validation
     * @param equipments Equipments list
     * @param numbers Numbers maps
     * @return Map containing number validations as key and an array containing equipments as value
     */
    private JsonObject mapNumbersEquipments (JsonArray equipments, JsonObject numbers) {
        JsonObject equipment;
        JsonArray equipmentList;
        for (int i = 0; i < equipments.size(); i++) {
            equipment = equipments.get(i);
            equipmentList = numbers.getArray(equipment.getString("number_validation"));
            numbers.putArray(equipment.getString("number_validation"), equipmentList.addObject(equipment));
        }

        return numbers;
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
                    log.error("An error occurred when collecting orders");
                    renderError(request);
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
        if(request.params().contains("idCampaign")) {
            return I18n.getInstance().translate("creation.date", getHost(request), I18n.acceptLanguage(request)) + ";" +
                    I18n.getInstance().translate("name.equipment", getHost(request), I18n.acceptLanguage(request)) + ";" +
                    I18n.getInstance().translate("quantity", getHost(request), I18n.acceptLanguage(request)) + ";" +
                    I18n.getInstance().translate("price.equipment", getHost(request), I18n.acceptLanguage(request)) + ";" +
                    I18n.getInstance().translate("status", getHost(request), I18n.acceptLanguage(request))
                    + "\n";
        }else if (request.params().contains("id")) {
            return I18n.getInstance().translate("Structure", getHost(request), I18n.acceptLanguage(request)) + ";" +
                    I18n.getInstance().translate("contract", getHost(request), I18n.acceptLanguage(request)) + ";" +
                    I18n.getInstance().translate("supplier", getHost(request), I18n.acceptLanguage(request)) + ";" +
                    I18n.getInstance().translate("quantity", getHost(request), I18n.acceptLanguage(request)) + ";" +
                    I18n.getInstance().translate("creation.date", getHost(request), I18n.acceptLanguage(request)) + ";" +
                    I18n.getInstance().translate("campaign", getHost(request), I18n.acceptLanguage(request)) + ";" +
                    I18n.getInstance().translate("price.equipment", getHost(request), I18n.acceptLanguage(request))
                    + "\n";

        }else{ return ""; }
    }


    private static String generateExportLine(HttpServerRequest request, JsonObject order)  {
        SimpleDateFormat formatterDate = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        SimpleDateFormat formatterDateCsv = new SimpleDateFormat("dd/MM/yyyy");
        Date orderDate = null;

        if(request.params().contains("idCampaign")) {
            try {
                orderDate = formatterDate.parse( order.getString("equipment_creation_date"));

            } catch (ParseException e) {
                log.error( "Error current format date" + e);
            }
            return formatterDateCsv.format(orderDate) + ";" +
                    order.getString("equipment_name") + ";" +
                    order.getNumber("equipment_quantity") + ";" +
                    order.getString("price_total_equipment") + " " + I18n.getInstance().
                    translate("money.symbol", getHost(request), I18n.acceptLanguage(request)) + ";" +
                    I18n.getInstance().translate(order.getString("equipment_status"), getHost(request),
                            I18n.acceptLanguage(request)) + ";" +
                    "\n";
        }else if (request.params().contains("id")){
            try {
                orderDate = formatterDate.parse( order.getString("date"));

            } catch (ParseException e) {
                log.error( "Error current format date" + e);
            }
            return order.getString("uaiNameStructure") + ";" +
                    order.getString("namecontract") + ";" +
                    order.getString("namesupplier") + ";" +
                    order.getNumber("qty") + ";" +
                    formatterDateCsv.format(orderDate) + ";" +
                    order.getString("namecampaign") + ";" +
                    order.getString("pricetotal") + " " + I18n.getInstance().
                    translate("money.symbol", getHost(request), I18n.acceptLanguage(request)) + ";" +
                    "\n";
        }else {return " ";}
    }


    @Put("/orders/valid")
    @ApiDoc("validate orders ")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
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

                            List<Integer> ids = SqlQueryUtils.getIntegerIds(params);
                            final String url = request.headers().get("Referer") ;
                            orderService.validateOrders(request, userInfos , ids, url,
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
    @ApiDoc("send orders")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void sendOrders (final HttpServerRequest request){
        RequestUtils.bodyToJson(request, pathPrefix + "orderIds", new Handler<JsonObject>() {
            @Override
            public void handle(final JsonObject orders) {
                final JsonArray ids = orders.getArray("ids");
                final String nbrBc = orders.getString("bc_number");
                final String nbrEngagement = orders.getString("engagement_number");
                final String dateGeneration = orders.getString("dateGeneration");
                Number supplierId = orders.getNumber("supplierId");

                getOrdersData(request, nbrBc, nbrEngagement, dateGeneration, supplierId, ids,
                        new Handler<JsonObject>() {
                            @Override
                            public void handle(JsonObject data) {
                                exportPDFService.generatePDF(request, data,
                                        "BC.xhtml", "Bon_Commande_",
                                        new Handler<Buffer>() {
                                            @Override
                                            public void handle(final Buffer pdf) {
                                                manageFileAndUpdateStatus(request, pdf, ids, nbrBc);
                                            }
                                        }
                                );
                            }
                        });
            }
        });
    }

    @Get("/orders/valid/export/csv")
    @ApiDoc("Export valid orders as CSV based on validation number")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void csvExport (final HttpServerRequest request) {
        if (request.params().contains("number_validation")) {
            List<String> validationNumbers = request.params().getAll("number_validation");
            orderService.getOrdersForCSVExportByValidationNumbers(new JsonArray(validationNumbers.toArray()), new Handler<Either<String, JsonArray>>() {
                @Override
                public void handle(Either<String, JsonArray> event) {
                    if (event.isRight()) {
                        final JsonArray equipments = event.right().getValue();
                        JsonArray structures = new JsonArray();
                        final JsonObject[] equipment = new JsonObject[1];
                        for (int i = 0; i < equipments.size(); i++) {
                            equipment[0] = equipments.get(i);
                            if (!structures.contains(equipment[0].getString("id_structure"))) {
                                structures.addString(equipment[0].getString("id_structure"));
                            }

                        }

                        structureService.getStructureById(structures, new Handler<Either<String, JsonArray>>() {
                            @Override
                            public void handle(Either<String, JsonArray> event) {
                                if (event.isRight()) {
                                    JsonObject structureMap = new JsonObject(), structure;
                                    JsonArray structures = event.right().getValue();
                                    for (int i = 0; i < structures.size(); i++) {
                                        structure = structures.get(i);
                                        structureMap.putString(structure.getString("id"),
                                                structure.getString("uai"));
                                    }

                                    for (int e = 0; e < equipments.size(); e++) {
                                        equipment[0] = equipments.get(e);
                                        equipment[0].putString("uai", structureMap.getString(equipment[0].getString("id_structure")));
                                    }

                                    renderValidOrdersCSVExport(request, equipments);
                                } else {
                                    renderError(request);
                                }
                            }
                        });
                    } else {
                        renderError(request);
                    }
                }
            });
        } else {
            badRequest(request);
        }
    }

    private void renderValidOrdersCSVExport(HttpServerRequest request, JsonArray equipments) {
        StringBuilder export = new StringBuilder(UTF8_BOM).append(getValidOrdersCSVExportHeader(request));
        for (int i = 0; i < equipments.size(); i++) {
            export.append(getValidOrdersCSVExportline((JsonObject) equipments.get(i)));
        }

        request.response()
                .putHeader("Content-Type", "text/csv; charset=utf-8")
                .putHeader("Content-Disposition", "attachment; filename=orders.csv")
                .end(export.toString());
    }

    private String getValidOrdersCSVExportline (JsonObject equipment) {
        return equipment.getString("uai")
                + ";"
                + equipment.getString("name")
                + ";"
                + equipment.getNumber("amount")
                + "\n";
    }

    private String getValidOrdersCSVExportHeader(HttpServerRequest request) {
        return I18n.getInstance().
                translate("UAI", getHost(request), I18n.acceptLanguage(request)) +
                ";" +
                I18n.getInstance().
                        translate("EQUIPMENT", getHost(request), I18n.acceptLanguage(request)) +
                ";" +
                I18n.getInstance().
                        translate("lystore.amount", getHost(request), I18n.acceptLanguage(request)) +
                "\n";
    }

    private void manageFileAndUpdateStatus(final HttpServerRequest request, final Buffer pdf,
                                           final JsonArray ids, final String nbrBc) {
        final String id = UUID.randomUUID().toString();
        storage.writeBuffer(id, pdf, "application/pdf",
                "BC_" + nbrBc, new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject file) {
                        if ("ok".equals(file.getString("status"))) {
                            orderService.addFileId(ids, id);
                            orderService.updateStatus(ids.toList(), "SENT",
                                    new Handler<Either<String, JsonObject>>() {
                                        @Override
                                        public void handle(Either<String, JsonObject> event) {
                                            if (event.isRight()) {
                                                request.response().end(pdf);
                                                logSendingOrder(ids, request);
                                            } else {
                                                badRequest(request);
                                            }
                                        }
                                    });
                        } else {
                            log.error("An error occurred when inserting pdf in mongo");
                            badRequest(request);
                        }
                    }
                });
    }

    private void logSendingOrder (JsonArray ids, final HttpServerRequest request) {
        orderService.getOrderByIds(ids, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    JsonArray orders = event.right().getValue();
                    JsonObject order;
                    for (int i = 0; i < orders.size(); i++) {
                        order = orders.get(i);
                        Logging.insert(eb, request, Contexts.ORDER.toString(), Actions.UPDATE.toString(),
                               order.getNumber("id").toString(), order);
                    }
                }
            }
        });
    }

    private void retrieveContract(final HttpServerRequest request, JsonArray ids,
                                  final Handler<JsonObject> handler) {
        contractService.getContract(ids, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight() && event.right().getValue().size() == 1) {
                    handler.handle((JsonObject) event.right().getValue().get(0));
                } else {
                    log.error("An error occured when collecting contract data");
                    badRequest(request);
                }
            }
        });
    }

    private void retrieveStructures (final HttpServerRequest request, JsonArray ids,
                                     final Handler<JsonObject> handler) {
        orderService.getStructuresId(ids, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    JsonArray structures = event.right().getValue();
                    JsonArray structuresList = new JsonArray();
                    final JsonObject structureMapping = new JsonObject();
                    JsonObject structure;
                    JsonObject structureInfo;
                    JsonArray orderIds;
                    for (int i = 0; i < structures.size(); i++) {
                        structure = structures.get(i);
                        if (!structuresList.contains(structure.getString("id_structure"))) {
                            structuresList.addString(structure.getString("id_structure"));
                            structureInfo = new JsonObject();
                            structureInfo.putArray("orderIds", new JsonArray());
                        } else {
                            structureInfo = structureMapping.getObject(structure.getString("id_structure"));
                        }
                        orderIds = structureInfo.getArray("orderIds");
                        orderIds.addNumber(structure.getNumber("id"));
                        structureMapping.putObject(structure.getString("id_structure"), structureInfo);
                    }
                    structureService.getStructureById(structuresList, new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isRight()) {
                                JsonArray structures = event.right().getValue();
                                JsonObject structure;
                                for (int i = 0; i < structures.size(); i++) {
                                    structure = structures.get(i);
                                    JsonObject structureObject = structureMapping.getObject(structure.getString("id"));
                                    structureObject.putObject(  "structureInfo", structure);
                                }
                                handler.handle(structureMapping);
                            } else {
                                log.error("An error occurred when collecting structures based on ids");
                                badRequest(request);
                            }
                        }
                    });
                } else {
                    log.error("An error occurred when getting structures id based on order ids.");
                    renderError(request);
                }
            }
        });
    }

    private void retrieveOrderData (final HttpServerRequest request, JsonArray ids, final Handler<JsonObject> handler) {
        orderService.getOrders(ids, null, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    JsonObject order = new JsonObject();
                    JsonArray orders = formatOrders(event.right().getValue());
                    order.putArray("orders", orders);
                    order.putString("sumLocale",
                            String.valueOf(roundWith2Decimals(getSumWithoutTaxes(orders))).replace(".", ","));
                    order.putString("totalTaxesLocale",
                            String.valueOf(roundWith2Decimals(getTaxesTotal(orders))).replace(".", ","));
                    order.putString("totalPriceTaxeIncludedLocal",
                            String.valueOf(roundWith2Decimals(getTotalOrder(orders))).replace(".", ","));
                    handler.handle(order);
                } else {
                    log.error("An error occurred when retrieving order data");
                    badRequest(request);
                }
            }
        });
    }

    private Float getTotalOrder(JsonArray orders) {
        Float sum = 0F;
        JsonObject order;
        for (int i = 0; i < orders.size(); i++) {
            order = orders.get(i);
            sum += (Float.parseFloat(order.getString("price")) * Integer.parseInt(order.getString("amount"))
                    * (Float.parseFloat(order.getString("tax_amount")) / 100 + 1));
        }

        return sum;
    }

    private Float getTaxesTotal(JsonArray orders) {
        Float sum = 0F;
        JsonObject order;
        for (int i = 0; i < orders.size(); i++) {
            order = orders.get(i);
            sum += Float.parseFloat(order.getString("price")) * Integer.parseInt(order.getString("amount"))
                    * (Float.parseFloat(order.getString("tax_amount")) / 100);
        }

        return sum;
    }

    private Float getSumWithoutTaxes(JsonArray orders) {
        JsonObject order;
        Float sum = 0F;
        for (int i = 0; i < orders.size(); i++) {
            order = orders.get(i);
            sum += Float.parseFloat(order.getString("price")) * Integer.parseInt(order.getString("amount"));
        }

        return sum;
    }

    private static JsonArray formatOrders (JsonArray orders) {
        JsonObject order;
        for (int i = 0; i < orders.size(); i++) {
            order = orders.get(i);
            order.putString("priceLocale",
                    String.valueOf(roundWith2Decimals(Float.parseFloat(order.getString("price")))).replace(".", ","));
            order.putString("unitPriceTaxIncluded",
                    String.valueOf(roundWith2Decimals(getTaxIncludedPrice(Float.parseFloat(order.getString("price")),
                            Float.parseFloat(order.getString("tax_amount"))))).replace(".", ","));
            order.putString("unitPriceTaxIncludedLocale",
                    String.valueOf(roundWith2Decimals(getTaxIncludedPrice(Float.parseFloat (order.getString("price")),
                            Float.parseFloat(order.getString("tax_amount"))))).replace(".", ","));
            order.putNumber("totalPrice",
                    roundWith2Decimals(getTotalPrice(Float.parseFloat(order.getString("price")),
                            Long.parseLong(order.getString("amount")))));
            order.putString("totalPriceLocale",
                    String.valueOf(roundWith2Decimals(Float.parseFloat(order.getNumber("totalPrice").toString()))).replace(".", ","));
            order.putString("totalPriceTaxIncluded",
                    String.valueOf(roundWith2Decimals(getTaxIncludedPrice((Float) order.getNumber("totalPrice"),
                            Float.parseFloat(order.getString("tax_amount"))))).replace(".", ","));
        }
        return orders;
    }

    private static Float getTotalPrice (Float price, Long amount) {
        return price * amount;
    }

    private static Float getTaxIncludedPrice(Float price, Float taxAmount) {
        Float multiplier = taxAmount / 100 + 1;
        return roundWith2Decimals(price) * multiplier;
    }

    private static Float roundWith2Decimals(Float numberToRound) {
        BigDecimal bd = new BigDecimal(numberToRound);
        return bd.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
    }


    @Get("/orders/preview")
    @ApiDoc("Get orders preview data")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(ManagerRight.class)
    public void getOrdersPreviewData (final HttpServerRequest request) {
        MultiMap params = request.params();
        if (!params.contains("ids") && !params.contains("bc_number")
                && !params.contains("engagement_number") && !params.contains("dateGeneration")
                && !params.contains("supplierId")) {
            badRequest(request);
        } else {
            final List<String> ids = params.getAll("ids");
            final List<Integer> integerIds = new ArrayList<>();
            final String nbrBc = params.get("bc_number");
            final String nbrEngagement = params.get("engagement_number");
            final String dateGeneration = params.get("dateGeneration");
            Number supplierId = Integer.parseInt(params.get("supplierId"));

            try {
                for (String id : ids) {
                    integerIds.add(Integer.parseInt(id));
                }
            } catch (NumberFormatException e) {
                log.error("An error occured when casting order id to Integer", e);
                badRequest(request);
                return;
            }

            getOrdersData(request, nbrBc, nbrEngagement, dateGeneration, supplierId,
                    new JsonArray(integerIds.toArray()), new Handler<JsonObject>() {
                        @Override
                        public void handle(JsonObject data) {
                            renderJson(request, data);
                        }
                    });
        }
    }

    private void getOrdersData (final HttpServerRequest request, final String nbrBc,
                                final String nbrEngagement, final String dateGeneration,
                                final Number supplierId, final JsonArray ids,
                                final Handler<JsonObject> handler) {
        final JsonObject data = new JsonObject();
        retrieveManagementInfo(request, ids, supplierId, new Handler<JsonObject>() {
            @Override
            public void handle(final JsonObject managmentInfo) {
                retrieveStructures(request, ids, new Handler<JsonObject>() {
                    @Override
                    public void handle(final JsonObject structures) {
                        retrieveOrderData(request, ids, new Handler<JsonObject>() {
                            @Override
                            public void handle(final JsonObject order) {
                                retrieveOrderDataForCertificate(request, structures, new Handler<JsonArray>() {
                                    @Override
                                    public void handle(final JsonArray certificates) {
                                        retrieveContract(request, ids, new Handler<JsonObject>() {
                                            @Override
                                            public void handle(JsonObject contract) {
                                                JsonObject certificate;
                                                for (int i = 0; i < certificates.size(); i++) {
                                                    certificate = certificates.get(i);
                                                    certificate.putObject("agent", managmentInfo.getObject("userInfo"));
                                                    certificate.putObject("supplier",
                                                            managmentInfo.getObject("supplierInfo"));
                                                    addStructureToOrders(certificate.getArray("orders"),
                                                            certificate.getObject("structure"));
                                                }
                                                data.putObject("supplier", managmentInfo.getObject("supplierInfo"))
                                                        .putObject("agent", managmentInfo.getObject("userInfo"))
                                                        .putObject("order", order)
                                                        .putArray("certificates", certificates)
                                                        .putObject("contract", contract)
                                                        .putString("nbr_bc", nbrBc)
                                                        .putString("nbr_engagement", nbrEngagement)
                                                        .putString("date_generation", dateGeneration);

                                                handler.handle(data);
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    private void addStructureToOrders(JsonArray orders, JsonObject structure) {
        JsonObject order;
        for (int i = 0; i < orders.size(); i++) {
            order = orders.get(i);
            order.putObject("structure", structure);
        }
    }

    private void retrieveOrderDataForCertificate(final HttpServerRequest request, final JsonObject structures,
                                                 final Handler<JsonArray> handler) {
        JsonObject structure;
        String structureId;
        Iterator<String> structureIds = structures.getFieldNames().iterator();
        final JsonArray result = new JsonArray();
        while (structureIds.hasNext()) {
            structureId = structureIds.next();
            structure = structures.getObject(structureId);
            orderService.getOrders(structure.getArray("orderIds"), structureId,
                    new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isRight() && event.right().getValue().size() > 0) {
                                JsonObject order = event.right().getValue().get(0);
                                result.addObject(new JsonObject()
                                        .putString("id_structure", order.getString("id_structure"))
                                        .putObject("structure", structures.getObject(order.getString("id_structure"))
                                                .getObject("structureInfo"))
                                        .putArray("orders", formatOrders(event.right().getValue()))
                                );
                                if (result.size() == structures.size()) {
                                    handler.handle(result);
                                }
                            } else {
                                log.error("An error occurred when collecting orders for certificates");
                                badRequest(request);
                                return;
                            }
                        }
                    });
        }
    }

    private void retrieveManagementInfo(final HttpServerRequest request, JsonArray ids,
                                        final Number supplierId, final Handler<JsonObject> handler) {
        agentService.getAgentByOrderIds(ids, new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> user) {
                if (user.isRight()) {
                    final JsonObject userObject = user.right().getValue();
                    supplierService.getSupplier(supplierId.toString(), new Handler<Either<String, JsonObject>>() {
                        @Override
                        public void handle(Either<String, JsonObject> supplier) {
                            if (supplier.isRight()) {
                                JsonObject supplierObject = supplier.right().getValue();
                                handler.handle(
                                        new JsonObject()
                                                .putObject("userInfo", userObject)
                                                .putObject("supplierInfo", supplierObject)
                                );
                            } else {
                                log.error("An error occurred when collecting supplier data");
                                badRequest(request);
                                return;
                            }
                        }
                    });
                } else {
                    log.error("An error occured when collecting user information");
                    badRequest(request);
                    return;
                }
            }
        });
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

    @Get("/orders/export")
    @ApiDoc("Export list of waiting orders as CSV")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessOrderRight.class)
    public void exportCSVordersSelected (final HttpServerRequest request){
        List<String> params =  request.params().getAll("id");
        List<Integer> idsOrders = SqlQueryUtils.getIntegerIds(params);
        if(!idsOrders.isEmpty()){
            orderService.getExportCsvOrdersAdmin(idsOrders, new Handler<Either<String, JsonArray>>() {
                @Override
                public void handle(Either<String, JsonArray> ordersWithIdStructure) {
                    if(ordersWithIdStructure.isRight()) {
                        final JsonArray orders = ordersWithIdStructure.right().getValue();
                        JsonArray idsStructures = new JsonArray();
                        for(int i = 0; i <orders.size();i++){
                            JsonObject order = orders.get(i);
                            idsStructures.addString(order.getString("idstructure"));
                        }
                        structureService.getStructureById(idsStructures, new Handler<Either<String, JsonArray>>() {
                            @Override
                            public void handle(Either<String, JsonArray> repStructures) {
                                if (repStructures.isRight()) {
                                    JsonArray structures = repStructures.right().getValue();

                                    Map<String, String> structuresMap = retrieveUaiNameStructure(structures);
                                    for (int i = 0; i < orders.size(); i++) {
                                        JsonObject order = orders.get(i);
                                        order.putString("uaiNameStructure", structuresMap.get(order.getString("idstructure")));
                                    }

                                    request.response()
                                            .putHeader("Content-Type", "text/csv; charset=utf-8")
                                            .putHeader("Content-Disposition", "attachment; filename=orders.csv")
                                            .end(generateExport(request, orders));

                                }else{
                                    log.error("An error occured when collecting StructureById");
                                    renderError(request);
                                }
                            }
                        });
                    }else{
                        log.error("An error occurred when collecting ordersSqlwithIdStructure");
                        renderError(request);
                    }
                }
            });
        }else{
            badRequest(request);
        }


    }
    private Map<String,String> retrieveUaiNameStructure(JsonArray structures) {
        final Map<String, String> structureMap = new HashMap<String, String>();

        for (int i = 0; i < structures.size(); i++) {
            JsonObject structure = structures.get(i);
            String uaiNameStructure = structure.getString("uai") + " - " + structure.getString("name");
            structureMap.put(structure.getString("id"), uaiNameStructure);
        }

        return structureMap;
    }

}
