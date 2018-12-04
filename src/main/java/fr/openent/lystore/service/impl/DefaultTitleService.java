package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.TitleService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.SqlResult;

public class DefaultTitleService extends SqlCrudService implements TitleService {

    public DefaultTitleService(String schema, String table) {
        super(schema, table);
    }

    /**
     * List all the titles
     *
     * @param handler
     */
    @Override
    public void getTitles(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT title.id, title.name " +
                "FROM " + Lystore.lystoreSchema + ".title GROUP BY title.id  ORDER BY name ";

        sql.prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }


}
