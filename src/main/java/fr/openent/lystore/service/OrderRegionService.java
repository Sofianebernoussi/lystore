package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public interface OrderRegionService {
    void updatOrderRegion(JsonObject order, Handler<Either<String, JsonObject>> handler);
}
