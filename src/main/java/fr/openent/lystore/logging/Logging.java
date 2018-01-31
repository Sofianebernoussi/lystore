package fr.openent.lystore.logging;

import fr.openent.lystore.Lystore;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Renders;
import org.entcore.common.sql.Sql;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

public final class Logging {

    private static final int BAD_REQUEST_STATUS = 400;
    private static final int OK_STATUS = 200;

    private Logging() {
        throw new IllegalAccessError("Utility class");
    }

    public static JsonObject add(EventBus eb, HttpServerRequest request, final String context,
                                 final String action, final String item, final JsonObject object) {
       final JsonObject statment = new JsonObject();
        UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
            @Override
            public void handle(UserInfos user) {
                StringBuilder query = new StringBuilder("INSERT INTO ")
                        .append(Lystore.lystoreSchema)
                .append(".logs(id_user, username, action, context, item" );
                if (object != null) {
                    query.append(", value");
                }
                query.append(") VALUES (?, ?, ?, ?, ?");
                if (object != null) {
                    query.append(", to_json(?)");
                }
                query.append(");");

                JsonArray params = new JsonArray()
                        .addString(user.getUserId())
                        .add(user.getUsername())
                        .addString(action)
                        .addString(context)
                        .addString(item.contains("id = ") ? item : ("id = " + item));
                        if (object != null) {
                            params.addObject(object);
                        }
                statment.putString("statement", query.toString())
                        .putArray("values",params)
                        .putString("action", "prepared");
            }
        });
        return statment;
    }

    public static Handler<Either<String, JsonObject>> defaultResponseHandler (final EventBus eb,
                      final HttpServerRequest request, final String context, final String action,
                      final String item, final JsonObject object) {
        return new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    Renders.renderJson(request, event.right().getValue(), OK_STATUS);
                    JsonObject statement = add(eb, request, context, action,
                            item == null ? event.right().getValue().getNumber("id").toString() : item, object);
                    Sql.getInstance().prepared(statement.getString("statement"),statement.getArray("values"),null);

                } else {
                    JsonObject error = new JsonObject()
                            .putString("error", event.left().getValue());
                    Renders.renderJson(request, error, BAD_REQUEST_STATUS);
                }
            }
        };
    }
    public static Handler<Either<String, JsonObject>> defaultResponsesHandler
            (final EventBus eb, final HttpServerRequest request, final String context, final String action,
             final  List<String> items, final JsonObject object) {
        return new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    Renders.renderJson(request, event.right().getValue(), OK_STATUS);
                    JsonArray statements = new JsonArray();
                    for(int i=0; i<items.size(); i++){

                        statements.add( add(eb, request, context, action,items.get(i), object));
                    }
                    Sql.getInstance().transaction(statements, null);
                } else {
                    JsonObject error = new JsonObject()
                            .putString("error", event.left().getValue());
                    Renders.renderJson(request, error, BAD_REQUEST_STATUS);
                }
            }
        };
    }

}
