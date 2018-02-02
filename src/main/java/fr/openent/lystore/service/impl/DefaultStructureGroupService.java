package fr.openent.lystore.service.impl;


import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.StructureGroupService;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.util.List;

/**
 * Created by agnes.lapeyronnie on 28/12/2017.
 */
public class DefaultStructureGroupService extends SqlCrudService implements StructureGroupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStructureGroupService.class);

    public DefaultStructureGroupService(String schema, String table){
        super(schema, table);
    }

    @Override
    public void listStructureGroups(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT id, name, description, array_to_json(array_agg(id_structure)) as structures FROM "
                + Lystore.lystoreSchema + ".structure_group " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_group_structure" +
                " on structure_group.id = rel_group_structure.id_structure_group group by (id, name , description ) " +
                "ORDER BY id;";

        this.sql.prepared(query,new JsonArray(), SqlResult.validResultHandler(handler));
    }

    @Override
    public void create(final JsonObject structureGroup, final Handler<Either<String, JsonObject>> handler) {
        String getIdQuery = "Select nextval('"+ Lystore.lystoreSchema + ".structure_group_id_seq') as id";
        sql.raw(getIdQuery, SqlResult.validUniqueResultHandler( new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if(event.isRight()) {
                    try{
                        final Number id = event.right().getValue().getNumber("id");
                        JsonArray statements = new JsonArray()
                                .add(getStructureGroupCreationStatement(id,structureGroup));

                        JsonArray idsStructures = structureGroup.getArray("structures");
                        statements.add(getGroupStructureRelationshipStatement(id,idsStructures));

                        sql.transaction(statements, new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(Message<JsonObject> event) {
                                handler.handle(SqlQueryUtils.getTransactionHandler(event,id));
                            }
                        });

                    }catch(ClassCastException e){
                        LOGGER.error("An error occured when casting structures ids " + e);
                        handler.handle(new Either.Left<String, JsonObject>(""));
                    }
                }else{
                   LOGGER.error("An error occurred when selecting next val");
                    handler.handle(new Either.Left<String, JsonObject>(""));
                }
            }
        }));
    }

    @Override
    public void update(final Integer id, JsonObject structureGroup,final Handler<Either<String, JsonObject>> handler) {
        JsonArray idsStructures = structureGroup.getArray("structures");
        JsonArray statements = new JsonArray()
                .addObject(getStructureGroupUpdateStatement(id,structureGroup))
                .addObject(getStrctureGroupRelationshipDeletion(id))
                .add(getGroupStructureRelationshipStatement(id,idsStructures));
        sql.transaction(statements, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(SqlQueryUtils.getTransactionHandler(event, id));
            }
        });
    }

    @Override
    public void delete(final List<Integer> ids, final Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new JsonArray()
                .addObject(getStrctureGroupRelationshipDeletion(ids))
                .addObject(getStructureGroupDeletion(ids));

        sql.transaction(statements, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(SqlQueryUtils.getTransactionHandler(event,ids.get(0)));
            }
        });
    }

    /**
     * Returns a structureGroup creation statement
     * @param id             structureGroup id
     * @param structureGroup structureGroup to create
     * @return structureGroup creation statement
     */
    private JsonObject getStructureGroupCreationStatement(Number id, JsonObject structureGroup){
        String insertStructureGroupQuery = "INSERT INTO "+ Lystore.lystoreSchema +
                ".structure_group(id, name, description) VALUES (?,?,?) RETURNING id;";
        JsonArray params = new JsonArray()
       .addNumber(id)
       .addString(structureGroup.getString("name"))
       .addString(structureGroup.getString("description"));
        return new JsonObject()
                .putString("statement", insertStructureGroupQuery)
                .putArray("values",params)
                .putString("action","prepared");
    }

    /**
     * Returns  a structureGroup idStructure relationship transaction statement
     * @param id_structure_group group id
     * @param idsStructure structure ids
     * @return structureGroup idStructure relationship transaction statement
     */
    private JsonObject getGroupStructureRelationshipStatement(Number id_structure_group, JsonArray idsStructure) {
        StringBuilder insertGroupStructureRelationshipQuery = new StringBuilder();
        JsonArray params = new JsonArray();
        insertGroupStructureRelationshipQuery.append("INSERT INTO ").append(Lystore.lystoreSchema)
        .append(".rel_group_structure(id_structure,id_structure_group) VALUES ");

        for(int i = 0; i < idsStructure.size();i++ ){
            String idStructure = idsStructure.get(i);
            insertGroupStructureRelationshipQuery.append("(?,?)");
            params.addString(idStructure)
                    .addNumber(id_structure_group);
            if(i != idsStructure.size()-1){
                insertGroupStructureRelationshipQuery.append(",");
            }else{
                insertGroupStructureRelationshipQuery.append(";");
            }
        }
        return new JsonObject()
                .putString("statement",insertGroupStructureRelationshipQuery.toString())
                .putArray("values",params)
                .putString("action","prepared");
    }

    /**
     * Returns the update statement
     * @param id structure_group
     * @param structureGroup to update
     * @return update statement
     */
    private JsonObject getStructureGroupUpdateStatement(Number id, JsonObject structureGroup){
        String query = "UPDATE "+ Lystore.lystoreSchema + ".structure_group " +
                "SET name = ?, description = ? WHERE id = ?;";
        JsonArray params = new JsonArray()
                .addString(structureGroup.getString("name"))
                .addString(structureGroup.getString("description"))
                .addNumber(id);
        return new JsonObject()
        .putString("statement", query)
        .putArray("values",params)
        .putString("action","prepared");
    }

    /**
     * Delete in rel_group_structure
     * @param id_structure_group of structureGroup
     * @return Delete statement
     */
    private JsonObject getStrctureGroupRelationshipDeletion(Number id_structure_group){
        String query = "DELETE FROM " + Lystore.lystoreSchema + ".rel_group_structure WHERE id_structure_group = ?;";

        return new JsonObject()
                .putString("statement", query)
                .putArray("values", new JsonArray().addNumber(id_structure_group))
                .putString("action", "prepared");

    }

    /**
     * Delete all ids group in rel_group_structure
     * @param ids list of id group
     * @return Delete statement
     */
    private JsonObject getStrctureGroupRelationshipDeletion(List<Integer> ids){
        String query = "DELETE FROM " + Lystore.lystoreSchema + ".rel_group_structure " +
                "WHERE id_structure_group IN " +Sql.listPrepared(ids.toArray());
        JsonArray params = new JsonArray();

        for (Integer id : ids) {
            params.addNumber(id);
        }
        return new JsonObject()
                .putString("statement", query)
                .putArray("values",params)
                .putString("action","prepared");

    }

    /**
     * Delete all ids structureGroup in structure_group
     * @param ids list of id_group_structure
     * @return Delete statement
     */
    private JsonObject getStructureGroupDeletion(List<Integer> ids){
        String query = "DELETE FROM "+ Lystore.lystoreSchema +".structure_group " +
                "WHERE id IN "+Sql.listPrepared(ids.toArray());
        JsonArray params = new JsonArray();

        for (Integer id : ids) {
            params.addNumber(id);
        }
        return new JsonObject()
                .putString("statement", query)
                .putArray("values",params)
                .putString("action","prepared");
    }

}
