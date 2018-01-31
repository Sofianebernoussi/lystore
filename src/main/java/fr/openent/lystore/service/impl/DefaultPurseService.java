package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.PurseService;
import fr.wseduc.webutils.Either;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class DefaultPurseService implements PurseService {
    @Override
    public void launchImport(Integer campaignId, JsonObject statementsValues,
                             final Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new JsonArray();
        String[] fields = statementsValues.getFieldNames().toArray(new String[0]);
        for (int i = 0; i < fields.length; i++) {
            statements.addObject(getImportStatement(campaignId, fields[i],
                    statementsValues.getString(fields[i])));
        }
        Sql.getInstance().transaction(statements, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> message) {
                if(message.body().containsField("status") &&
                        "ok".equals(message.body().getString("status"))) {
                    handler.handle(new Either.Right<String, JsonObject>(
                            new JsonObject().putString("status", "ok")));
                } else {
                    handler.handle(new Either.Left<String, JsonObject>
                            ("lystore.statements.error"));
                }
            }
        });
    }

    @Override
    public void getPursesByCampaignId(Integer campaignId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Lystore.LYSTORE_SCHEMA + ".purse" +
                " WHERE id_campaign = ?;";

        JsonArray params = new JsonArray()
                .addNumber(campaignId);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    private JsonObject getImportStatement(Integer campaignId, String structureId, String amount) {
        String statement = "INSERT INTO " + Lystore.LYSTORE_SCHEMA + ".purse(id_structure, amount, id_campaign) " +
                "VALUES (?, ?, ?) " +
                "ON CONFLICT (id_structure, id_campaign) DO UPDATE " +
                "SET amount = ? " +
                "WHERE purse.id_structure = ? " +
                "AND purse.id_campaign = ?;";

        JsonArray params = new JsonArray()
                .addString(structureId)
                .addNumber(Float.parseFloat(amount))
                .addNumber(campaignId)
                .addNumber(Float.parseFloat(amount))
                .addString(structureId)
                .addNumber(campaignId);


        return new JsonObject()
                .putString("statement", statement)
                .putArray("values", params)
                .putString("action", "prepared");
    }

    public void update(Integer id, JsonObject purse, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Lystore.LYSTORE_SCHEMA + ".purse " +
                "SET amount = ? WHERE id = ? RETURNING *;";

        JsonArray params = new JsonArray()
                .addNumber(purse.getNumber("amount"))
                .addNumber(id);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }
}
