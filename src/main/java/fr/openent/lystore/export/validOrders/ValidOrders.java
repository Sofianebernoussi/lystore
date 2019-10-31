package fr.openent.lystore.export.validOrders;

import fr.openent.lystore.export.ExportObject;
import fr.openent.lystore.export.validOrders.lisyLycee.ListLycee;
import fr.openent.lystore.export.validOrders.lisyLycee.RecapListLycee;
import fr.openent.lystore.helpers.ExcelHelper;
import fr.openent.lystore.service.ExportService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.ArrayList;
import java.util.List;

public class ValidOrders extends ExportObject {
    private String numberValidation="";
    private ExportService exportService;
    private Logger log = LoggerFactory.getLogger(ValidOrders.class);
    private String idFile;



    public ValidOrders(ExportService exportService, String numberValidation, String idNewFile) {
        super(exportService,idNewFile);
        this.numberValidation = numberValidation;
    }

    public void exportListLycee(Handler<Either<String, Buffer>> handler) {
        if (this.numberValidation == null || this.numberValidation.equals("")) {
            ExcelHelper.catchError(exportService, idFile, "number validation is not nullable");
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
}
