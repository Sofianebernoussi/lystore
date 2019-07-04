package fr.openent.lystore.export.investissement;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;

public class RecapImputationBud extends TabHelper {
    public RecapImputationBud(Workbook workbook, JsonObject instruction) {
        super(workbook, instruction, TabName.IMPUTATION_BUDG.toString());
    }

    @Override
    public void create(Handler<Either<String, Boolean>> handler) {
        handler.handle(new Either.Right<>(true));
    }

    @Override
    public void getPrograms(Handler<Either<String, JsonArray>> handler) {

    }

}
