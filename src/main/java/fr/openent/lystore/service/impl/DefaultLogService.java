package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.LogService;
import fr.wseduc.webutils.Either;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class DefaultLogService implements LogService {

    private static final int NB_OCCURRENCES_PAGE = 100;

    public void list(Integer page, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT id, date, action, context , CASE " +
                " WHEN value is null THEN "+
                " (SELECT value FROM " + Lystore.lystoreSchema +
                ".logs where logs.item = LOGGER.item AND logs.context = LOGGER.context " +
                "AND logs.value is not null  ORDER BY date DESC  LIMIT 1)::json " +
                " ELSE value END, " +
                " id_user, username, item "+
                "FROM " + Lystore.lystoreSchema + ".logs LOGGER ";
        JsonArray params = new JsonArray();

        if (page != null) {
            query += " ORDER BY date DESC LIMIT 100 OFFSET ?";
            params.addNumber(NB_OCCURRENCES_PAGE * page);
        }

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getLogsNumber(Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT count(id) as number_logs FROM " + Lystore.lystoreSchema + ".logs";

        Sql.getInstance().prepared(query, new JsonArray(), SqlResult.validUniqueResultHandler(handler));
    }
}
