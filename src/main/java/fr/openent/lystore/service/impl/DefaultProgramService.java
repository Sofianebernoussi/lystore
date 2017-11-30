package fr.openent.lystore.service.impl;

import fr.openent.lystore.service.ProgramService;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;

public class DefaultProgramService extends SqlCrudService implements ProgramService {
    private Sql sql;

    public DefaultProgramService(String schema, String table) {
        super(schema, table);
        this.sql = Sql.getInstance();
    }

    public void listPrograms(Handler<Either<String, JsonArray>> handler) {
        super.list(handler);
    }
}
