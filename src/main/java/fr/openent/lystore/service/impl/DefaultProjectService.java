package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.ProjectService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.SqlResult;

public class DefaultProjectService extends SqlCrudService implements ProjectService {
    public DefaultProjectService(String schema, String table) {
        super(schema, table);
    }

    /**
     * List all the projects
     *
     * @param handler
     */
    @Override
    public void getProjects(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT  project.id,,project.name project.description, project.id_title, project.id_grade, project.building, project.stair, project.room, project.site " +
                "array_to_json( array_agg( tt.* ) as title , array_to_json ( array_agg ( gr.*) as grade" +
                "FROM " + Lystore.lystoreSchema + ".project " +
                "LEFT JOIN " + Lystore.lystoreSchema + ".title tt ON tt.id = project.id_title " +
                "LEFT JOIN " + Lystore.lystoreSchema + ".grade gr ON gr.id = project.id_grade" +
                " GROUP BY tt.name, project.id ,gr.name";

        sql.prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }


    @Override
    public void createProject(JsonObject projet, Handler<Either<String, JsonObject>> handler) {


        String query = "INSERT INTO " +
                Lystore.lystoreSchema + ".project (" +
                "id_title, id_grade, description, building, stair, room, site, name) " +
                "Values ( ?, ?, ?, ?, ?, ?, ?, ? ) RETURNING id";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(projet.getInteger("id_title"))
                .add(projet.getInteger("id_grade"))
                .add(projet.getString("description"))
                .add(projet.getString("building"))
                .add(projet.getInteger("stair"))
                .add(projet.getString("room"))
                .add(projet.getString("site"))
                .add(projet.getString("name"));


        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));

    }

    @Override
    public void getProject(Integer id, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT project.* " +
                "FROM " + Lystore.lystoreSchema + ".project " +
                "Where project.id = ?";

        JsonArray params = new JsonArray().add(id);

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }


}
