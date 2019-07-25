package fr.openent.lystore.export.subventionRapp;

import fr.openent.lystore.export.TabHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;

public class RapportCMR extends TabHelper {
    public RapportCMR(Workbook workbook, JsonObject instruction) {
        super(workbook, instruction, "ANNEXE RAPPORT - CMR");
    }

    @Override
    public void create(Handler<Either<String, Boolean>> handler) {

    }

    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {

    }
}
