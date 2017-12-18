package fr.openent.lystore.service.impl;

import fr.openent.lystore.service.TaxService;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;

public class DefaultTaxService extends SqlCrudService implements TaxService {
    public DefaultTaxService(String schema, String table) {
        super(schema, table);
    }

    @Override
    public void list (Handler<Either<String, JsonArray>> handler) {
        super.list(handler);
    }
}
