package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.webutils.Either;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

public class SqlUtils {

    public static void deleteIds (String table, List<Integer> ids, org.vertx.java.core.Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder("DELETE FROM " + Lystore.LYSTORE_SCHEMA + "." + table +" WHERE ")
                .append(SqlQueryUtils.prepareMultipleIds(ids));
        JsonArray params = new JsonArray();
        for (int i = 0; i < ids.size(); i++) {
            params.addNumber(ids.get(i));
        }

        Sql.getInstance().prepared(query.toString(), params, SqlResult.validUniqueResultHandler(handler));
    }
}
