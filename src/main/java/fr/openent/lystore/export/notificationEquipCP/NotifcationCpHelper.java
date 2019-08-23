package fr.openent.lystore.export.notificationEquipCP;

import fr.openent.lystore.export.TabHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;

public class NotifcationCpHelper extends TabHelper {
    /**
     * open the tab or create it if it doesn't exists
     *
     * @param wb
     * @param instruction
     * @param TabName
     */
    public NotifcationCpHelper(Workbook wb, JsonObject instruction, String TabName) {
        super(wb, instruction, TabName);
    }

    @Override
    public void create(Handler<Either<String, Boolean>> handler) {

    }


    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {

    }





}
