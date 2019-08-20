package fr.openent.lystore.export.notificationEquipCP;

import fr.openent.lystore.export.TabHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

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

    // doing \n when the str is too long
    protected String formatStrToCell(String str) {
        try {
            String[] words = str.split(" ");
            String resultStr = "";
            if (words.length <= 5) {
                return str;
            } else {
                for (int i = 0; i < words.length; i++) {
                    resultStr += words[i] + " ";
                    if (i % 5 == 0 && i != 0) {
                        resultStr += "\n";
                    }
                }
            }
            return resultStr;
        } catch (NullPointerException e) {
            return str;
        }
    }

    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {

    }


    public void sqlHandler(Handler<Either<String, JsonArray>> handler) {
        Sql.getInstance().prepared(query, new JsonArray().add(instruction.getInteger("id")), SqlResult.validResultHandler(event -> {
            if (event.isLeft()) {
                handler.handle(event.left());
            } else {
                datas = event.right().getValue();
                handler.handle(new Either.Right<>(datas));
            }
        }));

    }

}
