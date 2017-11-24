package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.AgentService;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

public class DefaultAgentService extends SqlCrudService implements AgentService {

    private Sql sql;

    public DefaultAgentService(String schema, String table) {
        super(schema, table);
        this.sql = Sql.getInstance();
    }

    public void getAgents(Handler<Either<String, JsonArray>> handler) {
        super.list(handler);
    }

    public void createAgent(JsonObject agent, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Lystore.LYSTORE_SCHEMA + ".agent(email, department, name, phone) " +
                "VALUES (?, ?, ?, ?) RETURNING id;";

        JsonArray params = new JsonArray()
                .addString(agent.getString("email"))
                .addString(agent.getString("department"))
                .addString(agent.getString("name"))
                .addString(agent.getString("phone"));

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    public void updateAgent(Integer id, JsonObject agent, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Lystore.LYSTORE_SCHEMA + ".agent " +
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
        StringBuilder query = new StringBuilder("DELETE FROM " + Lystore.LYSTORE_SCHEMA + ".agent WHERE ");
        JsonArray params = new JsonArray();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) {
                query.append("OR ");
            }
            query.append("id = ? ");
            params.addNumber(ids.get(i));
        }

        sql.prepared(query.toString(), params, SqlResult.validUniqueResultHandler(handler));
    }
}
