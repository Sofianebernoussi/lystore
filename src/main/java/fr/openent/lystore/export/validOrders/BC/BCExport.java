package fr.openent.lystore.export.validOrders.BC;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.controllers.OrderController;
//import fr.openent.lystore.helpers.RendersHelper;
import fr.openent.lystore.helpers.RendersHelper;
import fr.openent.lystore.service.AgentService;
import fr.openent.lystore.service.OrderService;
import fr.openent.lystore.service.StructureService;
import fr.openent.lystore.service.SupplierService;
import fr.openent.lystore.service.impl.*;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.data.FileResolver;
import fr.wseduc.webutils.email.EmailSender;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.Http2ServerRequestImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.email.EmailFactory;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.*;

import static fr.wseduc.webutils.http.Renders.badRequest;


public class BCExport {
    private SupplierService supplierService;
    private JsonObject config;
    private Vertx vertx;
    private EventBus eb;
    private String node;
    private Logger log = LoggerFactory.getLogger(BCExport.class);
    private OrderService orderService;
    private DefaultContractService contractService;
    private StructureService structureService;
    private AgentService agentService;
    private RendersHelper renders ;

    public BCExport(EventBus eb, Vertx vertx, JsonObject config){
        this.vertx = vertx;
        this.config = config;
        EmailFactory emailFactory = new EmailFactory(vertx, config);
        EmailSender emailSender = emailFactory.getSender();
        this.eb = eb;
        this.supplierService = new DefaultSupplierService(Lystore.lystoreSchema, "supplier");
        this.orderService = new DefaultOrderService(Lystore.lystoreSchema, "order_client_equipment", emailSender);
        this.structureService = new DefaultStructureService(Lystore.lystoreSchema);
        this.supplierService = new DefaultSupplierService(Lystore.lystoreSchema, "supplier");
        this.contractService = new DefaultContractService(Lystore.lystoreSchema, "contract");
        this.agentService = new DefaultAgentService(Lystore.lystoreSchema, "agent");
        this.renders = new RendersHelper(this.vertx, config);

    }


    public void create(JsonArray validationNumbersArray, Handler<Either<String, Buffer>> exportHandler){
       List<String> validationNumbers = validationNumbersArray.getList();
        supplierService.getSupplierByValidationNumbers(new fr.wseduc.webutils.collections.JsonArray(validationNumbers), new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    JsonObject supplier = event.right().getValue();
                    getOrdersData( exportHandler,"", "", "", supplier.getInteger("id"), new fr.wseduc.webutils.collections.JsonArray(validationNumbers),
                            new Handler<JsonObject>() {
                                @Override
                                public void handle(JsonObject data) {
                                    data.put("print_order", true);
                                    data.put("print_certificates", false);
                                    generatePDF(exportHandler, data,
                                            "BC.xhtml", "CSF_",
                                            new Handler<Buffer>() {
                                                @Override
                                                public void handle(final Buffer pdf) {
                                                    exportHandler.handle(new Either.Right(pdf));
                                                }
                                            }
                                    );
                                }
                            });
                }else {
                    log.error("error when getting supplier");
                }
            }
        });
    }


    private void addStructureToOrders(JsonArray orders, JsonObject structure) {
        JsonObject order;
        for (int i = 0; i < orders.size(); i++) {
            order = orders.getJsonObject(i);
            order.put("structure", structure);
        }
    }

    private void retrieveOrderDataForCertificate(final Handler<Either<String, Buffer>> exportHandler, final JsonObject structures,
                                                 final Handler<JsonArray> handler) {
        JsonObject structure;
        String structureId;
        Iterator<String> structureIds = structures.fieldNames().iterator();
        final JsonArray result = new fr.wseduc.webutils.collections.JsonArray();
        while (structureIds.hasNext()) {
            structureId = structureIds.next();
            structure = structures.getJsonObject(structureId);
            orderService.getOrders(structure.getJsonArray("orderIds"), structureId, false, true,
                    new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isRight() && event.right().getValue().size() > 0) {
                                JsonObject order = event.right().getValue().getJsonObject(0);
                                result.add(new JsonObject()
                                        .put("id_structure", order.getString("id_structure"))
                                        .put("structure", structures.getJsonObject(order.getString("id_structure"))
                                                .getJsonObject("structureInfo"))
                                        .put("orders", OrderController.formatOrders(event.right().getValue()))
                                );
                                if (result.size() == structures.size()) {
                                    handler.handle(result);
                                }
                            } else {

                                log.error("An error occurred when collecting orders for certificates");
                                exportHandler.handle(new Either.Left<>("An error occured when collecting orders for certificates"));

                                return;
                            }
                        }
                    });
        }
    }

    private Map<String, String> retrieveUaiNameStructure(JsonArray structures) {
        final Map<String, String> structureMap = new HashMap<String, String>();

        for (int i = 0; i < structures.size(); i++) {
            JsonObject structure = structures.getJsonObject(i);
            String uaiNameStructure = structure.getString("uai") + " - " + structure.getString("name");
            structureMap.put(structure.getString("id"), uaiNameStructure);
        }

        return structureMap;
    }

    private void retrieveContract(final Handler<Either<String, Buffer>> exportHandler, JsonArray ids,
                                  final Handler<JsonObject> handler) {
        contractService.getContract(ids, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight() && event.right().getValue().size() == 1) {
                    handler.handle(event.right().getValue().getJsonObject(0));
                } else {
                    exportHandler.handle(new Either.Left<>("An error occured when collecting contract data"));
                    log.error("An error occured when collecting contract data");
                }
            }
        });
    }

    private void retrieveStructures(final Handler<Either<String, Buffer>> exportHandler, JsonArray ids,
                                    final Handler<JsonObject> handler) {
        orderService.getStructuresId(ids, new Handler<Either<String, JsonArray>>() {

            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    JsonArray structures = event.right().getValue();
                    JsonArray structuresList = new fr.wseduc.webutils.collections.JsonArray();
                    final JsonObject structureMapping = new JsonObject();
                    JsonObject structure;
                    JsonObject structureInfo;
                    JsonArray orderIds;
                    for (int i = 0; i < structures.size(); i++) {
                        structure = structures.getJsonObject(i);
                        if (!structuresList.contains(structure.getString("id_structure"))) {
                            structuresList.add(structure.getString("id_structure"));
                            structureInfo = new JsonObject();
                            structureInfo.put("orderIds", new fr.wseduc.webutils.collections.JsonArray());
                        } else {
                            structureInfo = structureMapping.getJsonObject(structure.getString("id_structure"));
                        }
                        orderIds = structureInfo.getJsonArray("orderIds");
                        orderIds.add(structure.getInteger("id"));
                        structureMapping.put(structure.getString("id_structure"), structureInfo);
                    }
                    structureService.getStructureById(structuresList, new Handler<Either<String, JsonArray>>() {
                        @Override
                        public void handle(Either<String, JsonArray> event) {
                            if (event.isRight()) {
                                JsonArray structures = event.right().getValue();
                                JsonObject structure;
                                for (int i = 0; i < structures.size(); i++) {
                                    structure = structures.getJsonObject(i);
                                    JsonObject structureObject = structureMapping.getJsonObject(structure.getString("id"));
                                    structureObject.put("structureInfo", structure);
                                }
                                handler.handle(structureMapping);
                            } else {
                                log.error("An error occurred when collecting structures based on ids");
                                exportHandler.handle(new Either.Left<>("An error occurred when collecting structures based on ids"));

                            }
                        }
                    });
                } else {
                    log.error("An error occurred when getting structures id based on order ids.");
                    exportHandler.handle(new Either.Left<>("An error occurred when getting structures id based on order ids."));

                }
            }
        });
    }


    private Double getTaxesTotal(JsonArray orders) {
        Double sum = 0D;
        JsonObject order;
        for (int i = 0; i < orders.size(); i++) {
            order = orders.getJsonObject(i);
            sum += Double.parseDouble(order.getString("price")) * Integer.parseInt(order.getString("amount"))
                    * (Double.parseDouble(order.getString("tax_amount")) / 100);
        }

        return sum;
    }

    private Double getSumWithoutTaxes(JsonArray orders) {
        JsonObject order;
        Double sum = 0D;
        for (int i = 0; i < orders.size(); i++) {
            order = orders.getJsonObject(i);
            sum += Double.parseDouble(order.getString("price")) * Integer.parseInt(order.getString("amount"));
        }

        return sum;
    }
    private void retrieveOrderData(final Handler<Either<String, Buffer>> exportHandler, JsonArray ids,
                                   final Handler<JsonObject> handler) {
        orderService.getOrders(ids, null, true, false, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                if (event.isRight()) {
                    JsonObject order = new JsonObject();
                    JsonArray orders = OrderController.formatOrders(event.right().getValue());
                    order.put("orders", orders);
                    Double sumWithoutTaxes = getSumWithoutTaxes(orders);
                    Double taxTotal = getTaxesTotal(orders);
                    order.put("sumLocale",
                            OrderController.getReadableNumber(OrderController.roundWith2Decimals(sumWithoutTaxes)));
                    order.put("totalTaxesLocale",
                            OrderController.getReadableNumber(OrderController.roundWith2Decimals(taxTotal)));
                    order.put("totalPriceTaxeIncludedLocal",
                            OrderController.getReadableNumber(OrderController.roundWith2Decimals(taxTotal + sumWithoutTaxes)));
                    handler.handle(order);
                } else {
                    log.error("An error occurred when retrieving order data");
                    exportHandler.handle(new Either.Left<>("An error occurred when retrieving order data"));
                }
            }
        });
    }
    private void retrieveManagementInfo(final Handler<Either<String, Buffer>> exportHandler, JsonArray ids,
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
                                                .put("userInfo", userObject)
                                                .put("supplierInfo", supplierObject)
                                );
                            } else {
                                log.error("An error occurred when collecting supplier data");
                                exportHandler.handle(new Either.Left<>("An error occurred when collecting supplier data"));

                                return;
                            }
                        }
                    });
                } else {
                    log.error("An error occured when collecting user information");
                    exportHandler.handle(new Either.Left<>("An error occured when collecting user information"));

                    return;
                }
            }
        });
    }
    private void getOrdersData(final Handler<Either<String, Buffer>> exportHandler, final String nbrBc,
                               final String nbrEngagement, final String dateGeneration,
                               final Number supplierId, final JsonArray ids,
                               final Handler<JsonObject> handler) {
        final JsonObject data = new JsonObject();
        retrieveManagementInfo(exportHandler, ids, supplierId, new Handler<JsonObject>() {
            @Override
            public void handle(final JsonObject managmentInfo) {
                retrieveStructures(exportHandler, ids, new Handler<JsonObject>() {
                    @Override
                    public void handle(final JsonObject structures) {
                        retrieveOrderData(exportHandler, ids, new Handler<JsonObject>() {
                            @Override
                            public void handle(final JsonObject order) {
                                retrieveOrderDataForCertificate(exportHandler, structures, new Handler<JsonArray>() {
                                    @Override
                                    public void handle(final JsonArray certificates) {
                                        retrieveContract(exportHandler, ids, new Handler<JsonObject>() {
                                            @Override
                                            public void handle(JsonObject contract) {
                                                JsonObject certificate;
                                                for (int i = 0; i < certificates.size(); i++) {
                                                    certificate = certificates.getJsonObject(i);
                                                    certificate.put("agent", managmentInfo.getJsonObject("userInfo"));
                                                    certificate.put("supplier",
                                                            managmentInfo.getJsonObject("supplierInfo"));
                                                    addStructureToOrders(certificate.getJsonArray("orders"),
                                                            certificate.getJsonObject("structure"));
                                                }
                                                data.put("supplier", managmentInfo.getJsonObject("supplierInfo"))
                                                        .put("agent", managmentInfo.getJsonObject("userInfo"))
                                                        .put("order", order)
                                                        .put("certificates", certificates)
                                                        .put("contract", contract)
                                                        .put("nbr_bc", nbrBc)
                                                        .put("nbr_engagement", nbrEngagement)
                                                        .put("date_generation", dateGeneration);

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





    public void generatePDF( Handler<Either<String, Buffer>> exportHandler,final JsonObject templateProps, final String templateName,
                            final String prefixPdfName, final Handler<Buffer> handler) {

        final JsonObject exportConfig = config.getJsonObject("exports");
        final String templatePath = exportConfig.getString("template-path");
        final String baseUrl = config.getString("host") +
                config.getString("app-address") + "/public/";
        final String logo = exportConfig.getString("logo-path");

        node = (String) vertx.sharedData().getLocalMap("server").get("node");
        if (node == null) {
            node = "";
        }

        final String path = FileResolver.absolutePath(templatePath + templateName);
        final String logoPath = FileResolver.absolutePath(logo);

        vertx.fileSystem().readFile(path, new Handler<AsyncResult<Buffer>>() {

            @Override
            public void handle(AsyncResult<Buffer> result) {
                if (!result.succeeded()) {
                    return;
                }

                Buffer logoBuffer = vertx.fileSystem().readFileBlocking(logoPath);
                String encodedLogo = "";
                try {
                    encodedLogo = new String(Base64.getMimeEncoder().encode(logoBuffer.getBytes()), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    log.error("[DefaultExportPDFService@generatePDF] An error occurred while encoding logo to base 64");
                }
                templateProps.put("logo-data", encodedLogo);

                StringReader reader = new StringReader(result.result().toString("UTF-8"));
                log.info(config.getString("host"));
                log.info(config.getString("host").split("//")[1]);
                renders.processTemplate(exportHandler, templateProps, templateName, reader, new Handler<Writer>() {
//
                    @Override
                    public void handle(Writer writer) {
                        String processedTemplate = ((StringWriter) writer).getBuffer().toString();
                        if (processedTemplate == null) {
                            exportHandler.handle(new Either.Left<>("processed template is null"));
                            return;
                        }
                        JsonObject actionObject = new JsonObject();
                        byte[] bytes;
                        try {
                            bytes = processedTemplate.getBytes("UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            bytes = processedTemplate.getBytes();
                            log.error(e.getMessage(), e);
                        }

                        actionObject
                                .put("content", bytes)
                                .put("baseUrl", baseUrl);
                        eb.send(node + "entcore.pdf.generator", actionObject, new Handler<AsyncResult<Message<JsonObject>>>() {
                            @Override
                            public void handle(AsyncResult<Message<JsonObject>> reply) {
                                JsonObject pdfResponse = reply.result().body();
                                if (!"ok".equals(pdfResponse.getString("status"))) {
                                    exportHandler.handle(new Either.Left<>("wrong status when calling bus (pdf) "));
                                    return;
                                }
                                byte[] pdf = pdfResponse.getBinary("content");
                                Buffer either = Buffer.buffer(pdf);
                                handler.handle(either);
                            }
                        });
                    }
                });
            }
        });

    }
}
