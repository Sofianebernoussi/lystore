package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.PurseService;
import fr.wseduc.webutils.Either;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class DefaultPurseService implements PurseService {
    @Override
    public void launchImport(Integer campaignId, JsonObject statementsValues,
                             final Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
        String[] fields = statementsValues.fieldNames().toArray(new String[0]);
        for (String field : fields) {
            statements.add(getImportStatement(campaignId, field,
                    statementsValues.getString(field)));
        }
        if (statements.size() > 0) {
            Sql.getInstance().transaction(statements, new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> message) {
                    if (message.body().containsKey("status") &&
                            "ok".equals(message.body().getString("status"))) {
                        handler.handle(new Either.Right<String, JsonObject>(
                                new JsonObject().put("status", "ok")));
                    } else {
                        handler.handle(new Either.Left<String, JsonObject>
                                ("lystore.statements.error"));
                    }
                }
            });
        } else {
            handler.handle(new Either.Left<String, JsonObject>
                    ("lystore.statements.empty"));
        }
    }

    @Override
    public void getPursesByCampaignId(Integer campaignId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Lystore.lystoreSchema + ".purse" +
                " WHERE id_campaign = ?;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(campaignId);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    private JsonObject getImportStatement(Integer campaignId, String structureId, String amount) {
        String statement = "INSERT INTO " + Lystore.lystoreSchema + ".purse(id_structure, amount, id_campaign) " +
                "VALUES (?, ?, ?) " +
                "ON CONFLICT (id_structure, id_campaign) DO UPDATE " +
                "SET amount = ? " +
                "WHERE purse.id_structure = ? " +
                "AND purse.id_campaign = ?;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(structureId)
                .add(Float.parseFloat(amount))
                .add(campaignId)
                .add(Float.parseFloat(amount))
                .add(structureId)
                .add(campaignId);


        return new JsonObject()
                .put("statement", statement)
                .put("values", params)
                .put("action", "prepared");
    }

    public void update(Integer id, JsonObject purse, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Lystore.lystoreSchema + ".purse " +
                "SET amount = ? WHERE id = ? RETURNING *;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(purse.getInteger("amount"))
                .add(id);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public JsonObject updatePurseAmountStatement(Float price, Integer idCampaign, String idStructure,String operation) {
        final double cons = 100.0;
        String updateQuery = "UPDATE lystore.purse " +
                "SET amount = amount " +  operation + " ?  " +
                "WHERE id_campaign = ? " +
                "AND id_structure = ? ;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(Math.round(price * cons)/cons)
                .add(idCampaign)
                .add(idStructure);

        return new JsonObject()
                .put("statement", updateQuery)
                .put("values", params)
                .put("action", "prepared");
    }
}
