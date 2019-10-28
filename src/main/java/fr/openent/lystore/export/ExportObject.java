package fr.openent.lystore.export;

import fr.openent.lystore.export.instructions.Instruction;
import fr.openent.lystore.helpers.ExcelHelper;
import fr.openent.lystore.service.ExportService;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.buffer.impl.BufferImpl;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class ExportObject {
    protected String idFile;
    protected ExportService exportService;
    protected Logger log = LoggerFactory.getLogger(ExportObject.class);

    public ExportObject(ExportService exportService, String idNewFile) {
        this.exportService = exportService;
        this.idFile = idNewFile;
    }
    protected void futureHandler(Handler<Either<String, Buffer>> handler, Workbook workbook, List<Future> futures) {
        CompositeFuture.all(futures).setHandler(event -> {
            if (event.succeeded()) {
                try {
                    ByteArrayOutputStream fileOut = new ByteArrayOutputStream();
                    workbook.write(fileOut);
                    Buffer buff = new BufferImpl();
                    buff.appendBytes(fileOut.toByteArray());
                    handler.handle(new Either.Right<>(buff));
                } catch (IOException e) {
                    ExcelHelper.catchError(exportService, idFile, e.getMessage());
                    handler.handle(new Either.Left<>(e.getMessage()));
                }
            } else {
                ExcelHelper.catchError(exportService, idFile, "Error when resolving futures");
                handler.handle(new Either.Left<>("Error when resolving futures"));
            }
        });
    }

    protected Handler<Either<String, Boolean>> getHandler(Future<Boolean> future) {
        return event -> {
            if (event.isRight()) {
                future.complete(event.right().getValue());
            } else {
                future.fail(event.left().getValue());
            }
        };
    }
}
