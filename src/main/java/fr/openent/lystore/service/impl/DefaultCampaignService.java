package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.CampaignService;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.List;
import java.util.Map;

public class DefaultCampaignService extends SqlCrudService implements CampaignService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCampaignService.class);


    public DefaultCampaignService(String schema, String table) {
        super(schema, table);
    }

    public void listCampaigns(Handler<Either<String, JsonArray>> handler) {
        Future<JsonArray> campaignFuture = Future.future();
        Future<JsonArray> equipmentFuture = Future.future();
        Future<JsonArray> purseFuture = Future.future();
        Future<JsonArray> orderFuture = Future.future();

        CompositeFuture.all(campaignFuture, equipmentFuture, purseFuture, orderFuture).setHandler(event -> {
            if (event.succeeded()) {
                JsonArray campaigns = campaignFuture.result();
                JsonArray equipments = equipmentFuture.result();
                JsonArray purses = purseFuture.result();
                JsonArray orders = orderFuture.result();

                JsonObject campaignMap = new JsonObject();
                JsonObject object, campaign;
                for (int i = 0; i < campaigns.size(); i++) {
                    object = campaigns.getJsonObject(i);
                    object.put("nb_orders_waiting", 0).put("nb_orders_valid", 0).put("nb_orders_sent", 0);
                    campaignMap.put(object.getInteger("id").toString(), object);
                }

                for (int i = 0; i < purses.size(); i++) {
                    object = purses.getJsonObject(i);
                    campaign = campaignMap.getJsonObject(object.getInteger("id_campaign").toString());
                    campaign.put("purse_amount", object.getString("purse"));
                }

                for (int i = 0; i < orders.size(); i++) {
                    object = orders.getJsonObject(i);
                    campaign = campaignMap.getJsonObject(object.getInteger("id_campaign").toString());
                    campaign.put("nb_orders_" + object.getString("status").toLowerCase(), object.getLong("count"));
                }

                for (int i = 0; i < equipments.size(); i++) {
                    object = equipments.getJsonObject(i);
                    campaign = campaignMap.getJsonObject(object.getInteger("id").toString());
                    campaign.put("nb_equipments", object.getLong("nb_equipments"));
                }

                JsonArray campaignList = new JsonArray();
                for (Map.Entry<String, Object> aCampaign : campaignMap) {
                    campaignList.add(aCampaign.getValue());
                }

                handler.handle(new Either.Right<>(campaignList));

            } else {
                handler.handle(new Either.Left<>("An error occurred when retrieving campaigns"));
            }
        });

        getCampaignsInfo(handlerFuture(campaignFuture));
        getCampaignEquipmentCount(handlerFuture(equipmentFuture));
        getCampaignsPurses(handlerFuture(purseFuture));
        getCampaignOrderStatusCount(handlerFuture(orderFuture));
    }

    private Handler<Either<String, JsonArray>> handlerFuture(Future<JsonArray> future) {
        return event -> {
            if (event.isRight()) {
                future.complete(event.right().getValue());
            } else {
                LOGGER.error(event.left().getValue());
                future.fail(event.left().getValue());
            }
        };
    }

    private void getCampaignEquipmentCount(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT campaign.id, COUNT(DISTINCT rel_equipment_tag.id_equipment) as nb_equipments " +
                "FROM " + Lystore.lystoreSchema + ".campaign " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_group_campaign ON (campaign.id = rel_group_campaign.id_campaign) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".tag ON (rel_group_campaign.id_tag = tag.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_equipment_tag ON (tag.id = rel_equipment_tag.id_tag) " +
                "GROUP BY campaign.id";
        Sql.getInstance().prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }

    private void getCampaignsPurses(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT SUM(amount) as purse, purse.id_campaign " +
                "FROM " + Lystore.lystoreSchema + ".purse " +
                "GROUP BY id_campaign;";

        Sql.getInstance().prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }

    private void getCampaignOrderStatusCount(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT count(*), id_campaign, status " +
                "FROM " + Lystore.lystoreSchema + ".order_client_equipment " +
                "GROUP BY id_campaign, status;";

        Sql.getInstance().prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }

    private void getCampaignsInfo(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT campaign.*, COUNT(DISTINCT rel_group_structure.id_structure) as nb_structures " +
                "FROM " + Lystore.lystoreSchema + ".campaign " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_group_campaign ON (campaign.id = rel_group_campaign.id_campaign) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_group_structure ON (rel_group_structure.id_structure_group = rel_group_campaign.id_structure_group) " +
                "GROUP BY campaign.id;";

        Sql.getInstance().prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }

    public void listCampaigns(String idStructure,  Handler<Either<String, JsonArray>> handler) {
        StringBuilder query = new StringBuilder()
                .append(" SELECT ")
                .append(" campaign.*, purse.amount purse_amount, COUNT(distinct rel_group_structure.id_structure)")
                .append(" as nb_structures, COUNT(distinct rel_equipment_tag.id_equipment) as nb_equipments")
                .append(" , count(DISTINCT  basket_equipment.id) nb_panier")
                .append(" , count(DISTINCT  order_c_e.id) nb_order")
                .append(" FROM " + Lystore.lystoreSchema + ".campaign")
                .append(" LEFT JOIN " + Lystore.lystoreSchema + ".basket_equipment ")
                .append(" ON (basket_equipment.id_campaign = campaign.id AND basket_equipment.id_structure = ? ) ")
                .append("LEFT JOIN Lystore.order_client_equipment order_c_e ")
                .append(" ON (order_c_e.id_campaign = campaign.id AND order_c_e.id_structure = ? AND order_c_e.status != 'VALID' ) ")
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
                new fr.wseduc.webutils.collections.JsonArray().add(idStructure).add(idStructure)
                        .add(idStructure).add(idStructure),
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

        sql.prepared(query, new fr.wseduc.webutils.collections.JsonArray().add(id).add(id), SqlResult.validUniqueResultHandler(handler));
    }
    public void create(final JsonObject campaign, final Handler<Either<String, JsonObject>> handler) {
        String getIdQuery = "SELECT nextval('" + Lystore.lystoreSchema + ".campaign_id_seq') as id";
        sql.raw(getIdQuery, SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    try {
                        final Number id = event.right().getValue().getInteger("id");
                        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                                .add(getCampaignCreationStatement(id, campaign));


                        JsonArray groups = campaign.getJsonArray("groups");
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
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                .add(getCampaignUpdateStatement(id, campaign))
                .add(getCampaignTagGroupRelationshipDeletion(id));
        JsonArray groups = campaign.getJsonArray("groups");
        statements.add(getCampaignTagsGroupsRelationshipStatement(id, (JsonArray) groups));

        sql.transaction(statements, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(getTransactionHandler(event, id));
            }
        });
    }

    public void delete(final List<Integer> ids, final Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                .add(getCampaignsGroupRelationshipDeletion(ids))
                .add(getCampaignsDeletion(ids));

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

        sql.prepared(query, new fr.wseduc.webutils.collections.JsonArray().add(campaignId), SqlResult.validResultHandler(handler));
    }

    @Override
    public void updatePreference(Integer campaignId,Integer projectId, String structureId,
                                 JsonArray projectOrders, Handler<Either<String, JsonObject>> handler) {
        String query= "UPDATE " + Lystore.lystoreSchema + ".project SET "+
                "preference = ? " +
                "WHERE id = ?; " +
                "UPDATE " + Lystore.lystoreSchema + ".project SET "+
                "preference = ? " +
                "WHERE id = ?; ";


        int size=projectOrders.getList().size();
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        for(int i=0;i<size;i++) {
            if (i == 0) {
                values.add(projectOrders.getJsonObject(i + 1).getInteger("preference"));
            } else {
                values.add(projectOrders.getJsonObject(i - 1).getInteger("preference"));
            }
            values.add(projectOrders.getJsonObject(i).getInteger("id"));

        }
            JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
            statements.add(new JsonObject()
                    .put("statement",query)
                    .put("values",values)
                    .put("action","prepared"));
            sql.transaction(statements,new Handler<Message<JsonObject>>() {

                @Override
                public void handle(Message<JsonObject> jsonObjectMessage) {
                    handler.handle(getTransactionHandler(jsonObjectMessage,projectId));
                }
            });
    }

    @Override
    public void getStructures(Integer idCampaign, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT id_structure as id " +
                "FROM " + Lystore.lystoreSchema + ".campaign " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_group_campaign ON (campaign.id = rel_group_campaign.id_campaign) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_group_structure ON (rel_group_campaign.id_structure_group = rel_group_structure.id_structure_group) " +
                "WHERE id = ? " +
                "GROUP BY id_structure";
        JsonArray params = new JsonArray()
                .add(idCampaign);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    public void updateAccessibility(final Integer id,final JsonObject campaign,
                                    final Handler<Either<String, JsonObject>> handler){
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
        String query = "UPDATE " + Lystore.lystoreSchema + ".campaign SET " +
                "accessible= ? " +
                "WHERE id = ?";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(campaign.getBoolean("accessible"))
                .add(id);
        statements.add(new JsonObject()
                .put("statement", query)
                .put("values",params)
                .put("action", "prepared"));
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
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        for(int j = 0; j < groups.size(); j++ ){
            JsonObject group =  groups.getJsonObject(j);
            JsonArray tags = group.getJsonArray("tags");
            for (int i = 0; i < tags.size(); i++) {
                insertTagCampaignRelationshipQuery.append(" (?, ?, ?)");
                if(i!=tags.size()-1 || j!= groups.size()-1){
                    insertTagCampaignRelationshipQuery.append(",");
                }
                params.add(id)
                        .add(group.getInteger("id"))
                        .add(tags.getInteger(i));
            }
        }
        return new JsonObject()
                .put("statement", insertTagCampaignRelationshipQuery.toString())
                .put("values", params)
                .put("action", "prepared");
    }

    private JsonObject getCampaignTagGroupRelationshipDeletion(Number id) {
        String query = "DELETE FROM " + Lystore.lystoreSchema + ".rel_group_campaign " +
                " WHERE id_campaign = ?;";

        return new JsonObject()
                .put("statement", query)
                .put("values", new fr.wseduc.webutils.collections.JsonArray().add(id))
                .put("action", "prepared");
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
                "SET  name=?, description=?, image=?, purse_enabled=?, priority_enabled=? " +
                "WHERE id = ?";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(campaign.getString("name"))
                .add(campaign.getString("description"))
                .add(campaign.getString("image"))
                .add(campaign.getBoolean("purse_enabled"))
                .add(campaign.getBoolean("priority_enabled"))
                .add(id);

        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }
    private JsonObject getCampaignCreationStatement(Number id, JsonObject campaign) {
        String insertCampaignQuery =
                "INSERT INTO " + Lystore.lystoreSchema + ".campaign(id, name, description, image, accessible, purse_enabled, priority_enabled )" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id; ";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(id)
                .add(campaign.getString("name"))
                .add(campaign.getString("description"))
                .add(campaign.getString("image"))
                .add(campaign.getBoolean("accessible"))
                .add(campaign.getBoolean("purse_enabled"))
                .add(campaign.getBoolean("priority_enabled"));

        return new JsonObject()
                .put("statement", insertCampaignQuery)
                .put("values", params)
                .put("action", "prepared");
    }
    private JsonObject getCampaignsGroupRelationshipDeletion(List<Integer> ids) {
        StringBuilder query = new StringBuilder();
        JsonArray value = new fr.wseduc.webutils.collections.JsonArray();
        query.append("DELETE FROM " + Lystore.lystoreSchema + ".rel_group_campaign ")
                .append(" WHERE id_campaign in  ")
                .append(Sql.listPrepared(ids.toArray()));

        for (Integer id : ids) {
            value.add(id);
        }

        return new JsonObject()
                .put("statement", query.toString())
                .put("values", value)
                .put("action", "prepared");
    }

    private JsonObject getCampaignsDeletion(List<Integer> ids) {
        StringBuilder query = new StringBuilder();
        JsonArray value = new fr.wseduc.webutils.collections.JsonArray();
        query.append("DELETE FROM " + Lystore.lystoreSchema + ".campaign ")
                .append(" WHERE id in  ")
                .append(Sql.listPrepared(ids.toArray()));

        for (Integer id : ids) {
            value.add(id);
        }

        return new JsonObject()
                .put("statement", query.toString())
                .put("values", value)
                .put("action", "prepared");
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
        if (result.containsKey("status") && "ok".equals(result.getString("status"))) {
            JsonObject returns = new JsonObject()
                    .put("id", id);
            either = new Either.Right<>(returns);
        } else {
            LOGGER.error("An error occurred when launching campaign transaction");
            either = new Either.Left<>("");
        }
        return either;
    }

}
