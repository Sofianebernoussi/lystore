package fr.openent.lystore.service.impl;

import fr.openent.lystore.service.OrderRegionService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public class DefaultOrderRegionService implements OrderRegionService {

    @Override
    public void updatOrderRegion(JsonObject order, Handler<Either<String, JsonObject>> handler) {
        handler.handle(new Either.Right<>(order));
    }
}
