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
        String query = "SELECT id, date, action, context , CASE " +
                " WHEN value is null THEN "+
                " (SELECT value FROM " + Lystore.LYSTORE_SCHEMA + ".logs where logs.item = log.item AND logs.context = log.context AND logs.value is not null  ORDER BY date DESC  LIMIT 1)::json " +
                " ELSE value END, " +
                " id_user, username, item "+
                "FROM " + Lystore.LYSTORE_SCHEMA + ".logs log ";
        JsonArray params = new JsonArray();

        if (page != null) {
            query += " ORDER BY date DESC LIMIT 100 OFFSET ?";
            params.addNumber(100 * page);
        }

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }
}
