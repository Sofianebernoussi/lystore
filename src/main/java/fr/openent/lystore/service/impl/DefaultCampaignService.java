package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.CampaignService;
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
public class DefaultCampaignService extends SqlCrudService implements CampaignService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCampaignService.class);


    public DefaultCampaignService(String schema, String table) {
        super(schema, table);
    }

    public void listCampaigns( Handler<Either<String, JsonArray>> handler) {
        String query = "WITH campaign_amounts AS (" +
                " SELECT SUM(amount) as sum, purse.id_campaign" +
                " FROM lystore.purse" +
                " GROUP BY id_campaign )" +
                " SELECT " +
                " campaign.*, COUNT(distinct rel_group_structure.id_structure) as nb_structures," +
                " campaign_amounts.sum as purse_amount," +
                " COUNT(distinct rel_equipment_tag.id_equipment) as nb_equipments" +
                " FROM " + Lystore.lystoreSchema + ".campaign" +
                " INNER JOIN campaign_amounts ON (campaign.id = campaign_amounts.id_campaign)" +
                " LEFT JOIN " + Lystore.lystoreSchema + ".rel_group_campaign" +
                " ON (campaign.id = rel_group_campaign.id_campaign)" +
                " LEFT JOIN " + Lystore.lystoreSchema + ".rel_group_structure" +
                " ON (rel_group_campaign.id_structure_group = rel_group_structure.id_structure_group)" +
                " LEFT JOIN " + Lystore.lystoreSchema + ".rel_equipment_tag" +
                " ON (rel_equipment_tag.id_tag = rel_group_campaign.id_tag) " +
                " GROUP BY campaign.id, campaign_amounts.sum;";
        sql.prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }

    public void listCampaigns(String idStructure,  Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder()
                .append(" SELECT ")
                .append(" campaign.*, purse.amount purse_amount, COUNT(distinct rel_group_structure.id_structure)")
                .append(" as nb_structures, COUNT(distinct rel_equipment_tag.id_equipment) as nb_equipments")
                .append(" , count(DISTINCT  basket_equipment.id) nb_panier")
                .append(" FROM " + Lystore.lystoreSchema + ".campaign")
                .append(" LEFT JOIN " + Lystore.lystoreSchema + ".basket_equipment ")
                .append(" ON (basket_equipment.id_campaign = campaign.id AND basket_equipment.id_structure = ? ) ")
                .append(" LEFT JOIN Lystore.purse ON purse.id_campaign = campaign.id AND purse.id_structure = ? ")
                .append(" LEFT JOIN " + Lystore.lystoreSchema + ".rel_group_campaign")
                .append(" ON (campaign.id = rel_group_campaign.id_campaign) ")
                .append(" INNER JOIN " + Lystore.lystoreSchema + ".rel_group_structure")
                .append(" ON (rel_group_campaign.id_structure_group = rel_group_structure.id_structure_group)")
                .append(" AND  rel_group_structure.id_structure = ? ")
                .append(" LEFT JOIN " + Lystore.lystoreSchema + ".rel_equipment_tag")
                .append(" ON (rel_equipment_tag.id_tag = rel_group_campaign.id_tag)")
                .append(" GROUP BY campaign.id, purse.amount ;" );
        sql.prepared(query.toString(),
                new JsonArray().addString(idStructure).addString(idStructure).addString(idStructure),
                SqlResult.validResultHandler(handler));
    }

    public void getCampaign(Integer id, Handler<Either<String, JsonObject>> handler){
        String query = "  SELECT campaign.*,array_to_json(array_agg(groupe)) as  groups     "+
                "FROM  " + Lystore.lystoreSchema + ".campaign campaign  "+
                "LEFT JOIN  "+
                "(SELECT rel_group_campaign.id_campaign, structure_group.*,  array_to_json(array_agg(id_tag))" +
                " as  tags FROM " + Lystore.lystoreSchema + ".structure_group " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_group_campaign" +
                " ON structure_group.id = rel_group_campaign.id_structure_group "+
                "WHERE rel_group_campaign.id_campaign = ?  "+
                "GROUP BY (rel_group_campaign.id_campaign, structure_group.id)) as groupe " +
                "ON groupe.id_campaign = campaign.id "+
                "where campaign.id = ?  "+
                "group By (campaign.id);  " ;

        sql.prepared(query, new JsonArray().addNumber(id).addNumber(id), SqlResult.validUniqueResultHandler(handler));
    }
    public void create(final JsonObject campaign, final Handler<Either<String, JsonObject>> handler) {
        String getIdQuery = "SELECT nextval('" + Lystore.lystoreSchema + ".campaign_id_seq') as id";
        sql.raw(getIdQuery, SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    try {
                        final Number id = event.right().getValue().getNumber("id");
                        JsonArray statements = new JsonArray()
                                .add(getCampaignCreationStatement(id, campaign));


                        JsonArray groups = campaign.getArray("groups");
                        statements.add(getCampaignTagsGroupsRelationshipStatement(id, (JsonArray) groups));
                        sql.transaction(statements, new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(Message<JsonObject> event) {
                                handler.handle(getTransactionHandler(event, id));
                            }
                        });
                    } catch (ClassCastException e) {
                        LOGGER.error("An error occurred when casting tags ids", e);
                        handler.handle(new Either.Left<String, JsonObject>(""));
                    }
                } else {
                    LOGGER.error("An error occurred when selecting next val");
                    handler.handle(new Either.Left<String, JsonObject>(""));
                }
            }
        }));
    }

    public void update(final Integer id, JsonObject campaign,final Handler<Either<String, JsonObject>> handler){
        JsonArray statements = new JsonArray()
                .addObject(getCampaignUpdateStatement(id, campaign))
                .addObject(getCampaignTagGroupRelationshipDeletion(id));
        JsonArray groups = campaign.getArray("groups");
        statements.add(getCampaignTagsGroupsRelationshipStatement(id, (JsonArray) groups));

        sql.transaction(statements, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(getTransactionHandler(event, id));
            }
        });
    }

    public void delete(final List<Integer> ids, final Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new JsonArray()
                .addObject(getCampaignsGroupRelationshipDeletion(ids))
                .addObject(getCampaignsDeletion(ids));

        sql.transaction(statements, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(getTransactionHandler(event,ids.get(0)));
            }
        });
    }

    @Override
    public void getCampaignStructures(Integer campaignId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT distinct id_structure FROM lystore.campaign " +
                "INNER JOIN lystore.rel_group_campaign ON (campaign.id = rel_group_campaign.id_campaign) " +
                "INNER JOIN lystore.rel_group_structure " +
                "ON (rel_group_structure.id_structure_group = rel_group_campaign.id_structure_group) " +
                "WHERE campaign.id = ?;";

        sql.prepared(query, new JsonArray().addNumber(campaignId), SqlResult.validResultHandler(handler));
    }


    public void updateAccessibility(final Integer id,final JsonObject campaign,
                                    final Handler<Either<String, JsonObject>> handler){
        JsonArray statements = new JsonArray();
        String query = "UPDATE " + Lystore.lystoreSchema + ".campaign SET " +
                "accessible= ? " +
                "WHERE id = ?";
        JsonArray params = new JsonArray()
                .addBoolean(campaign.getBoolean("accessible"))
                .addNumber(id);
        statements.addObject(new JsonObject()
                .putString("statement", query)
                .putArray("values",params)
                .putString("action", "prepared"));
        sql.transaction(statements, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(getTransactionHandler(event, id));
            }
        });
    }

    private JsonObject getCampaignTagsGroupsRelationshipStatement(Number id, JsonArray groups) {
        StringBuilder insertTagCampaignRelationshipQuery = new StringBuilder("INSERT INTO " +
                Lystore.lystoreSchema + ".rel_group_campaign" +
                        "(id_campaign, id_structure_group, id_tag) VALUES ");
        JsonArray params = new JsonArray();
        for(int j = 0; j < groups.size(); j++ ){
            JsonObject group =  groups.get(j);
            JsonArray tags = group.getArray("tags");
            for (int i = 0; i < tags.size(); i++) {
                insertTagCampaignRelationshipQuery.append(" (?, ?, ?)");
                if(i!=tags.size()-1 || j!= groups.size()-1){
                    insertTagCampaignRelationshipQuery.append(",");
                }
                params.addNumber(id)
                        .addNumber(group.getNumber("id"))
                        .addNumber((Number) tags.get(i));
            }
        }
        return new JsonObject()
                .putString("statement", insertTagCampaignRelationshipQuery.toString())
                .putArray("values", params)
                .putString("action", "prepared");
    }

    private JsonObject getCampaignTagGroupRelationshipDeletion(Number id) {
        String query = "DELETE FROM " + Lystore.lystoreSchema + ".rel_group_campaign " +
                " WHERE id_campaign = ?;";

        return new JsonObject()
                .putString("statement", query)
                .putArray("values", new JsonArray().addNumber(id))
                .putString("action", "prepared");
    }

    /**
     * Returns the update statement.
     *
     * @param id        resource Id
     * @param campaign campaign to update
     * @return Update statement
     */
    private JsonObject getCampaignUpdateStatement(Number id, JsonObject campaign) {
        String query = "UPDATE " + Lystore.lystoreSchema + ".campaign " +
                "SET  name=?, description=?, image=? " +
                "WHERE id = ?";

        JsonArray params = new JsonArray()
                .addString(campaign.getString("name"))
                .addString(campaign.getString("description"))
                .addString(campaign.getString("image"))
                .addNumber(id);

        return new JsonObject()
                .putString("statement", query)
                .putArray("values", params)
                .putString("action", "prepared");
    }
    private JsonObject getCampaignCreationStatement(Number id, JsonObject campaign) {
        String insertCampaignQuery =
                "INSERT INTO " + Lystore.lystoreSchema + ".campaign(id, name, description, image, accessible )"+
                        "VALUES (?, ?, ?, ?, ?) RETURNING id; ";
        JsonArray params = new JsonArray()
                .addNumber(id)
                .addString(campaign.getString("name"))
                .addString(campaign.getString("description"))
                .addString(campaign.getString("image"))
                .addBoolean(campaign.getBoolean("accessible"))
                ;

        return new JsonObject()
                .putString("statement", insertCampaignQuery)
                .putArray("values", params)
                .putString("action", "prepared");
    }
    private JsonObject getCampaignsGroupRelationshipDeletion(List<Integer> ids) {
        StringBuilder query = new StringBuilder();
        JsonArray value = new JsonArray();
        query.append("DELETE FROM " + Lystore.lystoreSchema + ".rel_group_campaign ")
                .append(" WHERE id_campaign in  ")
                .append(Sql.listPrepared(ids.toArray()));

        for (Integer id : ids) {
            value.addNumber(id);
        }

        return new JsonObject()
                .putString("statement", query.toString())
                .putArray("values", value)
                .putString("action", "prepared");
    }

    private JsonObject getCampaignsDeletion(List<Integer> ids) {
        StringBuilder query = new StringBuilder();
        JsonArray value = new JsonArray();
        query.append("DELETE FROM " + Lystore.lystoreSchema + ".campaign ")
                .append(" WHERE id in  ")
                .append(Sql.listPrepared(ids.toArray()));

        for (Integer id : ids) {
            value.addNumber(id);
        }

        return new JsonObject()
                .putString("statement", query.toString())
                .putArray("values", value)
                .putString("action", "prepared");
    }
    /**
     * Returns transaction handler. Manage response based on PostgreSQL event
     *
     * @param event PostgreSQL event
     * @param id    resource Id
     * @return Transaction handler
     */
    private static Either<String, JsonObject> getTransactionHandler(Message<JsonObject> event, Number id) {
        Either<String, JsonObject> either;
        JsonObject result = event.body();
        if (result.containsField("status") && "ok".equals(result.getString("status"))) {
            JsonObject returns = new JsonObject()
                    .putNumber("id", id);
            either = new Either.Right<>(returns);
        } else {
            LOGGER.error("An error occurred when launching campaign transaction");
            either = new Either.Left<>("");
        }
        return either;
    }

}
