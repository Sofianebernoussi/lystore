package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.LogService;
import fr.wseduc.webutils.Either;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;

public class DefaultLogService implements LogService {
    public void list(Integer page, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Lystore.LYSTORE_SCHEMA + ".logs";
        JsonArray params = new JsonArray();

        if (page != null) {
            query += " ORDER BY date DESC LIMIT 100 OFFSET ?";
            params.addNumber(100 * page);
        }

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }
}
