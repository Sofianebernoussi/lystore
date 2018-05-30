package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.webutils.Either;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public final class SqlUtils {

    private SqlUtils() {
        throw new IllegalAccessError("Utility class");
    }

    public static void deleteIds (String table, List<Integer> ids,
                                  io.vertx.core.Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder("DELETE FROM " + Lystore.lystoreSchema + "." + table +" WHERE ")
                .append(SqlQueryUtils.prepareMultipleIds(ids));
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        for (Integer id : ids) {
            params.add(id);
        }

        Sql.getInstance().prepared(query.toString(), params, SqlResult.validUniqueResultHandler(handler));
    }
}
