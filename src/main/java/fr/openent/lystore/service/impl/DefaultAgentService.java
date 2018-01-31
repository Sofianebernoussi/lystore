package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.AgentService;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.SqlResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

public class DefaultAgentService extends SqlCrudService implements AgentService {

    public DefaultAgentService(String schema, String table) {
        super(schema, table);
    }

    public void getAgents(Handler<Either<String, JsonArray>> handler) {
        super.list(handler);
    }

    public void createAgent(JsonObject agent, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Lystore.lystoreSchema + ".agent(email, department, name, phone) " +
                "VALUES (?, ?, ?, ?) RETURNING id;";

        JsonArray params = new JsonArray()
                .addString(agent.getString("email"))
                .addString(agent.getString("department"))
                .addString(agent.getString("name"))
                .addString(agent.getString("phone"));

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    public void updateAgent(Integer id, JsonObject agent, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Lystore.lystoreSchema + ".agent " +
                "SET email = ?, department = ?, name = ?, phone = ? " +
                "WHERE id = ? RETURNING *;";

        JsonArray params = new JsonArray()
                .addString(agent.getString("email"))
                .addString(agent.getString("department"))
                .addString(agent.getString("name"))
                .addString(agent.getString("phone"))
                .addNumber(id);

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    public void deleteAgent(List<Integer> ids, Handler<Either<String, JsonObject>> handler) {
        SqlUtils.deleteIds("agent", ids, handler);
    }
}
