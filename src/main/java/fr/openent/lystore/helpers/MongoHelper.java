package fr.openent.lystore.helpers;

import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.mongodb.MongoQueryBuilder;
import fr.wseduc.mongodb.MongoUpdateBuilder;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.CrudService;
import org.entcore.common.service.impl.MongoDbCrudService;

public class MongoHelper extends MongoDbCrudService {
    private final EventBus eb;
    public MongoHelper(String collection ,EventBus eb) {
        super(collection);
        this.eb = eb;
    }


    public void addExport(JsonObject export, Handler<String> handler) {
        try {
            mongo.insert(this.collection, export, jsonObjectMessage -> handler.handle(jsonObjectMessage.body().getString("_id")));
        } catch (Exception e) {
            handler.handle("mongoinsertfailed");
        }
    }
}
