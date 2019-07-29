package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.ExportService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

public class DefaultExportServiceService implements ExportService {
    public DefaultExportServiceService(String lystoreSchema, String instruction) {
    }

    @Override
    public void getExports(Handler<Either<String, JsonArray>> handler, UserInfos user) {
        String query = "SELECT filename,fileid,created " +
                "FROM " + Lystore.lystoreSchema + ".export " +
                "WHERE ownerid = ?" +
                "order by created";
        Sql.getInstance().prepared(query, new JsonArray().add(user.getUserId()), SqlResult.validResultHandler(handler));

    }
}
