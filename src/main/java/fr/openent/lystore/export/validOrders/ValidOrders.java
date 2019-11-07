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
import fr.wseduc.webutils.data.FileResolver;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static fr.wseduc.webutils.http.Renders.badRequest;
import static fr.wseduc.webutils.http.Renders.getScheme;

public class ValidOrders extends ExportObject {
    private String numberValidation="";
    private JsonObject params;
    private ExportService exportService;
    private Logger log = LoggerFactory.getLogger(ValidOrders.class);
    private String idFile;
    private SupplierService supplierService;
    private JsonObject config;
    private Vertx vertx;
    private EventBus eb;
    private String node;

    public ValidOrders(ExportService exportService, String idNewFile,EventBus eb, Vertx vertx, JsonObject config){
        super(exportService,idNewFile);
        this.vertx = vertx;
        this.config = config;
        this.eb = eb;
        this.supplierService = new DefaultSupplierService(Lystore.lystoreSchema, "supplier");

    }
    public ValidOrders(ExportService exportService, String numberValidation, String idNewFile,EventBus eb, Vertx vertx, JsonObject config) {
       this(exportService,idNewFile,eb,vertx,config);
        this.numberValidation = numberValidation;

    }
    public ValidOrders(ExportService exportService, JsonObject params, String idNewFile,EventBus eb, Vertx vertx, JsonObject config) {
        this(exportService,idNewFile,eb,vertx,config);
        this.params = params;
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



    public void generatePDF(final JsonObject templateProps, final String templateName,
                            final String prefixPdfName,  final Handler<Buffer> handler) {

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
                System.out.println("yeah");

                Buffer logoBuffer = vertx.fileSystem().readFileBlocking(logoPath);
                handler.handle(logoBuffer);
//                String encodedLogo = "";
//                try {
//                    encodedLogo = new String(Base64.getMimeEncoder().encode(logoBuffer.getBytes()), "UTF-8");
//                } catch (UnsupportedEncodingException e) {
//                    e.printStackTrace();
//                    log.error("[DefaultExportPDFService@generatePDF] An error occurred while encoding logo to base 64");
//                }
//                templateProps.put("logo-data", encodedLogo);


            }
        });

    }


    public void exportBC(Handler<Either<String, Buffer>> handler) {
        if (this.params == null || this.params.isEmpty()) {
            ExportHelper.catchError(exportService, idFile, "number validations is not nullable");
            handler.handle(new Either.Left<>("number validations is not nullable"));
        }else{
            generatePDF(new JsonObject(), "BC.xhtml", "Bon_Commande_", new Handler<Buffer>() {
                @Override
                public void handle(Buffer event) {
                    handler.handle(new Either.Right<>(event));
                }
            });
            log.info("good");
        }
    }
}
