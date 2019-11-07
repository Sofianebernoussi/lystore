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


    public void exportBC(Handler<Either<String, Buffer>> handler) {
        if (this.params == null || this.params.isEmpty()) {
            ExportHelper.catchError(exportService, idFile, "number validations is not nullable");
            handler.handle(new Either.Left<>("number validations is not nullable"));
        }else{
            log.info("good");
        }
    }
}
