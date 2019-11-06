package fr.openent.lystore.export.validOrders;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.ExportObject;
import fr.openent.lystore.export.validOrders.listLycee.ListLycee;
import fr.openent.lystore.export.validOrders.listLycee.RecapListLycee;
import fr.openent.lystore.helpers.ExportHelper;
import fr.openent.lystore.service.ExportService;
import fr.openent.lystore.service.SupplierService;
import fr.openent.lystore.service.impl.DefaultSupplierService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.ArrayList;
import java.util.List;

public class ValidOrders extends ExportObject {
    private String numberValidation="";
    private JsonObject params;
    private ExportService exportService;
    private Logger log = LoggerFactory.getLogger(ValidOrders.class);
    private String idFile;
    private SupplierService supplierService;


    public ValidOrders(ExportService exportService, String numberValidation, String idNewFile) {
        super(exportService,idNewFile);
        this.numberValidation = numberValidation;
        this.supplierService = new DefaultSupplierService(Lystore.lystoreSchema, "supplier");

    }
    public ValidOrders(ExportService exportService, JsonObject params, String idNewFile) {
        super(exportService,idNewFile);
        this.params = params;
        this.supplierService = new DefaultSupplierService(Lystore.lystoreSchema, "supplier");
    }

    public void exportListLycee(Handler<Either<String, Buffer>> handler) {
        if (this.numberValidation == null || this.numberValidation.equals("")) {
            ExportHelper.catchError(exportService, idFile, "number validation is not nullable");
            handler.handle(new Either.Left<>("number validation is not nullable"));
        }
        Workbook workbook = new XSSFWorkbook();
        List<Future> futures = new ArrayList<>();
        Future<Boolean> ListLyceeFuture = Future.future();
        Future<Boolean> RecapListLyceeFuture = Future.future();

        futures.add(ListLyceeFuture);
        futures.add(RecapListLyceeFuture);
        futureHandler(handler, workbook, futures);
        new ListLycee(workbook, this.numberValidation).create(getHandler(ListLyceeFuture));
        new RecapListLycee(workbook, this.numberValidation).create(getHandler(RecapListLyceeFuture));

    }

    private void getOrdersData(final HttpServerRequest request, final String nbrBc,
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

    public void exportBC(Handler<Either<String, Buffer>> handler) {
        if (this.params == null || this.params.isEmpty()) {
            ExportHelper.catchError(exportService, idFile, "number validations is not nullable");
            handler.handle(new Either.Left<>("number validations is not nullable"));
        }
        supplierService.getSupplierByValidationNumbers(new JsonArray(params.getJsonArray("numberValidations").getList()), new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    JsonObject supplier = event.right().getValue();
                    System.out.println(supplier);
                    getOrdersData(request, "", "", "", supplier.getInteger("id"), new fr.wseduc.webutils.collections.JsonArray(validationNumbers),
                            new Handler<JsonObject>() {
                                @Override
                                public void handle(JsonObject data) {
                                    data.put("print_order", printOrder);
                                    data.put("print_certificates", printCertificates);
                                    exportPDFService.generatePDF(request, data,
                                            "BC.xhtml", "CSF_",
                                            new Handler<Buffer>() {
                                                @Override
                                                public void handle(final Buffer pdf) {
                                                    request.response()
                                                            .putHeader("Content-Type", "application/pdf; charset=utf-8")
                                                            .putHeader("Content-Disposition", "attachment; filename="
                                                                    + generateExportName(validationNumbers, "" +
                                                                    (printOrder ? "BC" : "") + (printCertificates ? "CSF" : "")) + ".pdf")
                                                            .end(pdf);
                                                }
                                            }
                                    );
                                }
                            });
                } else {
                    log.error("An error occurred when collecting supplier Id", new Throwable(event.left().getValue()));
                    badRequest(request);
                }
            }
        });

    }
}
