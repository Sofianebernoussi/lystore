package fr.openent.lystore.service.impl;

import fr.openent.lystore.service.ContractTypeService;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;

public class DefaultContractTypeService extends SqlCrudService implements ContractTypeService {
    private Sql sql;

    public DefaultContractTypeService(String schema, String table) {
        super(schema, table);
        this.sql = Sql.getInstance();
    }

    public void lisContractTypes(Handler<Either<String, JsonArray>> handler) {
        super.list(handler);
    }
}
