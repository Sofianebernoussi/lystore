package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.InstructionService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.SqlResult;

public class DefaultInstructionService  extends SqlCrudService implements InstructionService {

    public DefaultInstructionService(String schema, String table) {
        super(schema, table);
    }

    public void getExercises (Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Lystore.lystoreSchema +".exercise";
        sql.prepared(query, new JsonArray(), SqlResult.validResultHandler(handler) );
    }

}
