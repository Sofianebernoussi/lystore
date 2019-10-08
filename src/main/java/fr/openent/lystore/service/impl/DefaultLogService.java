package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.LogService;
import fr.wseduc.webutils.Either;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class DefaultLogService implements LogService {

    private static final int NB_OCCURRENCES_PAGE = 100;

    public void list(Integer page, Handler<Either<String, JsonArray>> handler)  {
        String query = "SELECT id, date, action, context , CASE " +
                " WHEN value is null THEN "+
                " (SELECT value FROM " + Lystore.lystoreSchema +
                ".logs where logs.item = LOGGER.item AND logs.context = LOGGER.context " +
                "AND logs.value is not null  ORDER BY date DESC  LIMIT 1)::json " +
                " ELSE value END, " +
                " id_user, username, item "+
                "FROM " + Lystore.lystoreSchema + ".logs LOGGER ";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        if (page != null) {
            query += " ORDER BY date DESC LIMIT 100 OFFSET ?";
            params.add(NB_OCCURRENCES_PAGE * page);
        }
//TODO à retirer une fois le test de barre de chargement fait
        String finalQuery = query;
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        Sql.getInstance().prepared(finalQuery, params, SqlResult.validResultHandler(handler));
                        ;
                    }
                },
                30*1000
        );
    }

    @Override
    public void getLogsNumber(Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT count(id) as number_logs FROM " + Lystore.lystoreSchema + ".logs";

        Sql.getInstance().prepared(query, new fr.wseduc.webutils.collections.JsonArray(), SqlResult.validUniqueResultHandler(handler));
    }
}
