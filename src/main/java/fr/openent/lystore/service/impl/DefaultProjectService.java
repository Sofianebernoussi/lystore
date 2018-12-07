package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.ProjectService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.SqlResult;


public class DefaultProjectService extends SqlCrudService implements ProjectService {
    private Logger log = LoggerFactory.getLogger(DefaultProjectService.class);
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
                " GROUP BY tt.name, project.id ,gr.name ;";

        sql.prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }


    @Override
    public void createProject(JsonObject project, Handler<Either<String, JsonObject>> handler) {


        String query = "INSERT INTO " +
                Lystore.lystoreSchema + ".project (" +
                "id_title, id_grade, description, building, stair, room, site, name) " +
                "Values ( ?, ?, ?, ?, ?, ?, ?, ? ) RETURNING id ;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(project.getInteger("id_title"))
                .add(project.getInteger("id_grade"))
                .add(project.getString("description"))
                .add(project.getString("building"))
                .add(project.getInteger("stair"))
                .add(project.getString("room"))
                .add(project.getString("site"))
                .add(project.getString("name"));


        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));

    }

    @Override
    public void getProject(Integer id, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT project.* " +
                "FROM " + Lystore.lystoreSchema + ".project " +
                "Where project.id = ? ;";

        JsonArray params = new JsonArray().add(id);

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }


    @Override
    public void revertOrderAndDeleteProject(JsonArray orders, Integer id, Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
        JsonObject order;
        for (int i = 0; i < orders.size(); i++) {
            order = orders.getJsonObject(i);

            statements.add(getInsertBasketEquipmentOrderStatement(order));
            if (order.containsKey("options")) {
                statements.add(getInsertBasketOptionOrderStatement(order));
            }
        }
        statements.add(deleteProject(id));

        sql.transaction(statements, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                if (!"ok".equals(event.body().getString("status"))) {
                    String message = "An error occurred when deleting project " + id;
                    log.error(message);
                    handler.handle(new Either.Left<>(message));
                } else {
                    handler.handle(new Either.Right<>(new JsonObject().put("status", "ok")));
                }
            }
        });
    }

    public void selectOrdersToBaskets(Integer id, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT oe.equipment_key as id_equipment, array_to_json(array_agg(DISTINCT bo.*)) as options, oe.price as price_equipment, oe.comment as comment, oe.id_campaign as id_campaign," +
                "oe.id_structure as id_structure , nextval( '" + Lystore.lystoreSchema + ".basket_equipment_id_seq') as id_basket_equipment, " +
                "oe.price_proposal as price_proposal, oe.amount as amount  " +
                "FROM " + Lystore.lystoreSchema + ".order_client_equipment as oe " +
                "LEFT JOIN ( " +
                "SELECT  equipment_option.id_equipment as id,equipment_option.id_option as id_option " +
                "FROM " + Lystore.lystoreSchema + ".equipment_option " +
                "INNER JOIN " + Lystore.lystoreSchema + ".equipment ON equipment_option.id_equipment= equipment.id " +
                ") bo ON oe.equipment_key = bo.id " +
                "WHERE oe.id_project = ? " +
                "GROUP BY oe.id ;";
        JsonArray params = new JsonArray().add(id);


        sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void updateProject(JsonObject project, Handler<Either<String, JsonObject>> handler, Integer id) {
        JsonArray params = new JsonArray();
        String query;
        try {
            params.add(project.getInteger("id_title"))
                    .add(project.getInteger("id_grade"))
                    .add(project.getString("description"))
                    .add(project.getString("building"))
                    .add(project.getInteger("stair"))
                    .add(project.getString("room"))
                    .add(project.getString("site"))
                    .add(project.getString("name"))
                    .add(id);

            query = "UPDATE " + Lystore.lystoreSchema + ".project " +
                    "SET id_title = ?, " +
                    "id_grade = ?, " +
                    "description = ?, " +
                    "building = ?, " +
                    "stair = ?, " +
                    "room = ?, " +
                    "site = ?, " +
                    "name = ? " +
                    "WHERE id = ? ;";


        } catch (NullPointerException e) {

            params.add(project.getInteger("id_title"))
                    .add(project.getInteger("id_grade"))
                    .add(project.getString("description"))
                    .add(project.getString("building"))
                    .add(project.getString("room"))
                    .add(project.getString("site"))
                    .add(project.getString("name"))
                    .add(id);


            query = "UPDATE " + Lystore.lystoreSchema + ".project " +
                    "SET id_title = ?, " +
                    "id_grade = ?, " +
                    "description = ?, " +
                    "building = ?, " +
                    "stair = null, " +
                    "room = ?, " +
                    "site = ?, " +
                    "name = ? " +
                    "WHERE id = ? ;";
        }
        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void deletableProject(Integer id, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT count(project.id) as count" +
                " FROM " + Lystore.lystoreSchema + ".project " +
                "INNER JOIN " + Lystore.lystoreSchema + ".order_client_equipment oce " +
                "ON project.id = oce.id_project " +
                "WHERE oce.status != 'WAITING' AND project.id = ? ;";
        JsonArray params = new JsonArray().add(id);

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    private JsonObject deleteProject(Integer id) {
        JsonArray params = new JsonArray();
        String queryDeletionProject = " DELETE FROM " + Lystore.lystoreSchema + ".project " +
                "WHERE project.id = ? ;";

        params.add(id);
        return new JsonObject()
                .put("statement", queryDeletionProject)
                .put("values", params)
                .put("action", "prepared");

    }

    private static JsonObject getInsertBasketEquipmentOrderStatement(JsonObject order) {
        StringBuilder queryBasketEquipment;
        JsonArray params;

        try {
            queryBasketEquipment = new StringBuilder()
                    .append(" INSERT INTO lystore.basket_equipment ")
                    .append(" ( id, amount,  id_equipment, id_campaign, id_structure, comment, price_proposal ) VALUES ")
                    .append(" (?, ?, ?, ?, ?, ?, ?); ");
            params = new fr.wseduc.webutils.collections.JsonArray();

            params.add(order.getInteger("id_basket_equipment"))
                    .add(order.getInteger("amount"))
                    .add(order.getInteger("amount"))
                    .add(order.getInteger("id_campaign"))
                    .add(order.getString("id_structure"))
                    .add(order.getString("comment"))
                    .add(Float.valueOf(order.getString("price_proposal")));

        } catch (NullPointerException e) {
            queryBasketEquipment = new StringBuilder()
                    .append(" INSERT INTO lystore.basket_equipment ")
                    .append(" ( id, amount,  id_equipment, id_campaign, id_structure, comment, price_proposal ) VALUES ")
                    .append(" (?, ?, ?, ?, ?, ?, null); ");
            params = new fr.wseduc.webutils.collections.JsonArray();

            params.add(order.getInteger("id_basket_equipment"))
                    .add(order.getInteger("amount"))
                    .add(order.getInteger("amount"))
                    .add(order.getInteger("id_campaign"))
                    .add(order.getString("id_structure"))
                    .add(order.getString("comment"));


        }
        return new JsonObject()
                .put("statement", queryBasketEquipment.toString())
                .put("values", params)
                .put("action", "prepared");
    }

    private static JsonObject getInsertBasketOptionOrderStatement(JsonObject order) {

        JsonObject option;
        JsonArray options = new JsonArray(order.getString("options"));
        String buffer = "";
        for (int i = 0; i < options.size(); i++) {
            buffer += " (?, ?),";

        }
        buffer = buffer.substring(0, buffer.length() - 1);

        String queryBasketOption = "INSERT INTO " + Lystore.lystoreSchema + ".basket_option " +
                " ( id_option, id_basket_equipment)" +
                "VALUES " + buffer + ";";

        JsonArray params = new JsonArray();

        for (int i = 0; i < options.size(); i++) {
            option = options.getJsonObject(i);
            params.add(option.getInteger("id")).add(order.getInteger("id_basket_equipment"));
        }
        return new JsonObject()
                .put("statement", queryBasketOption.toString())
                .put("values", params)
                .put("action", "prepared");


    }


}
