package fr.openent.lystore.export.notificationEquipCP;

import fr.openent.lystore.export.TabHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;

public class LinesBudget extends TabHelper {
    public LinesBudget(Workbook workbook, JsonObject instruction) {
        super(workbook, instruction, "Lignes Budgetaires");
    }

    @Override
    public void create(Handler<Either<String, Boolean>> handler) {
        excel.setCPNumber(instruction.getString("cp_number"));

        handler.handle(new Either.Right<>(true));
    }

    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {

    }
}
