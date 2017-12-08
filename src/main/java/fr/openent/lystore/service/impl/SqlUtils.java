package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.wseduc.webutils.Either;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

public class SqlUtils {

    public static void deleteIds (String table, List<Integer> ids, org.vertx.java.core.Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder("DELETE FROM " + Lystore.LYSTORE_SCHEMA + "." + table +" WHERE ");
        JsonArray params = new JsonArray();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                query.append("OR ");
            }
            query.append("id = ? ");
            params.addNumber(ids.get(i));
        }

        Sql.getInstance().prepared(query.toString(), params, SqlResult.validUniqueResultHandler(handler));
    }
}
