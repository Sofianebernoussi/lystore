package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.EquipmentService;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;

public class DefaultEquipmentService extends SqlCrudService implements EquipmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEquipmentService.class);
    private static final String STATEMENT = "statement" ;
    private static final String VALUES = "values" ;
    private static final String ACTION = "action" ;
    private static final String PREPARED = "prepared" ;

    public DefaultEquipmentService(String schema, String table) {
        super(schema, table);
    }

    public void listEquipments(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT equip.*, tax.value as tax_amount, array_to_json( " +
                "(SELECT array_agg(id_tag) " +
                "FROM " + Lystore.lystoreSchema + ".equipment INNER JOIN " + Lystore.lystoreSchema + ".rel_equipment_tag ON (equipment.id = rel_equipment_tag.id_equipment) " +
                "WHERE equip.id = rel_equipment_tag.id_equipment)) as tags, array_to_json(array_agg(opts.*)) as options " +
                "FROM " + Lystore.lystoreSchema + ".equipment equip " +
                "LEFT JOIN " + Lystore.lystoreSchema + ".equipment_option ON (equip.id = equipment_option.id_equipment) " +
                "LEFT JOIN (" +
                " SELECT equipment.id as id_equipment, equipment_option.id, equipment_option.id_option, equipment.name, equipment.price, equipment_option.amount, equipment_option.required, tax.value as tax_amount, equipment_option.id_equipment as master_equipment " +
                " FROM " + Lystore.lystoreSchema + ".equipment " +
                " INNER JOIN " + Lystore.lystoreSchema + ".tax ON (equipment.id_tax = tax.id) " +
                " INNER JOIN " + Lystore.lystoreSchema + ".equipment_option ON (equipment_option.id_option = equipment.id) " +
                ") opts ON (equipment_option.id_option = opts.id_equipment AND opts.master_equipment = equip.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".tax ON tax.id = equip.id_tax " +
                "WHERE equip.catalog_enabled = true " +
                "GROUP BY (equip.id, tax.id)";
        sql.prepared(query, new fr.wseduc.webutils.collections.JsonArray(), SqlResult.validResultHandler(handler));
    }
    public void equipment(Integer idEquipment,  Handler<Either<String, JsonArray>> handler){
        String query = "SELECT e.*, tax.value tax_amount, array_to_json(array_agg(opts)) as options " +
                "FROM " + Lystore.lystoreSchema + ".equipment e " +
                "LEFT JOIN (" +
                "SELECT equipment_option.id_equipment, opt.id, opt.name, opt.price, equipment_option.amount, equipment_option.required, tax.value as tax_amount " +
                "FROM " + Lystore.lystoreSchema + ".equipment " +
                "INNER JOIN " + Lystore.lystoreSchema + ".equipment_option ON (equipment.id = equipment_option.id_equipment) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".equipment opt ON (equipment_option.id_option = opt.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".tax ON (opt.id_tax = tax.id) " +
                "WHERE equipment_option.id_equipment = ? " +
                ") opts ON opts.id_equipment = e.id " +
                "INNER JOIN " + Lystore.lystoreSchema + ".tax on tax.id = e.id_tax WHERE e.id = ? " +
                "GROUP BY (e.id, tax.id)";

        this.sql.prepared(query, new fr.wseduc.webutils.collections.JsonArray().add(idEquipment).add(idEquipment), SqlResult.validResultHandler(handler));
    }
    public void listEquipments(Integer idCampaign, String idStructure,
                               Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new fr.wseduc.webutils.collections.JsonArray();
        String query = "SELECT e.*, tax.value tax_amount, array_to_json(array_agg(DISTINCT opts)) as options, array_to_json(array_agg(DISTINCT  rel_equipment_tag.id_tag)) tags " +
                "FROM " + Lystore.lystoreSchema + ".equipment e LEFT JOIN ( " +
                "SELECT option.*, equipment.name, equipment.price, tax.value tax_amount " +
                "FROM " + Lystore.lystoreSchema + ".equipment_option option " +
                "INNER JOIN " + Lystore.lystoreSchema + ".equipment ON (option.id_option = equipment.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".tax on tax.id = equipment.id_tax " +
                ") opts ON opts.id_equipment = e.id " +
                "INNER JOIN " + Lystore.lystoreSchema + ".tax on tax.id = e.id_tax " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_equipment_tag ON (e.id = rel_equipment_tag.id_equipment) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_group_campaign ON (" +
                "rel_group_campaign.id_tag = rel_equipment_tag.id_tag " +
                "AND rel_group_campaign.id_campaign = ? " +
                "AND rel_group_campaign.id_structure_group IN (" +
                "SELECT structure_group.id FROM " + Lystore.lystoreSchema + ".structure_group " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_group_structure ON rel_group_structure.id_structure_group = structure_group.id " +
                "WHERE rel_group_structure.id_structure = ?)) " +
                "GROUP BY (e.id, tax.id);";
        values.add(idCampaign).add(idStructure);

        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }

    public void create(final JsonObject equipment, final Handler<Either<String, JsonObject>> handler) {
        String getIdQuery = "SELECT nextval('" + Lystore.lystoreSchema + ".equipment_id_seq') as id";
        sql.raw(getIdQuery, SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    try {
                        final Number id = event.right().getValue().getInteger("id");
                        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                                .add(getEquipmentCreationStatement(id, equipment));

                        JsonArray tags = equipment.getJsonArray("tags");
                        for (int i = 0; i < tags.size(); i++) {
                            statements.add(getEquipmentTagRelationshipStatement(id, tags.getInteger(i)));
                        }
                        JsonArray options = equipment.getJsonArray("optionsCreate");
                        for (int j = 0; j < options.size(); j++) {
                            statements.add(getEquipmentOptionRelationshipStatement(id, (JsonObject) options.getJsonObject(j)));
                        }
                        sql.transaction(statements, new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(Message<JsonObject> event) {
                                handler.handle(SqlQueryUtils.getTransactionHandler(event, id));
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

    @Override
    public void updateEquipment(final Integer id, JsonObject equipment,
                                final Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                .add(getEquipmentUpdateStatement(id, equipment))
                .add(getEquipmentTagRelationshipDeletion(id));
        JsonArray tags = equipment.getJsonArray("tags");

        for (int i = 0; i < tags.size(); i++) {
            statements.add(getEquipmentTagRelationshipStatement(id, tags.getInteger(i)));
        }

        sql.transaction(statements, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(SqlQueryUtils.getTransactionHandler(event, id));
            }
        });
    }
    @Override
    public void updateOptions(final Number id, JsonObject equipment,  JsonObject  resultsObject,
                              final Handler<Either<String, JsonObject>> handler){
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
        JsonArray deletedOptions  = equipment.getJsonArray("deletedOptions");
        JsonArray optionsCreate =  equipment.getJsonArray("optionsCreate");
        JsonArray optionsUpdate =  equipment.getJsonArray("optionsUpdate");

        if( null != deletedOptions && deletedOptions.size() != 0 ) {
            statements.add(getEquipmentOptionsBasketRelationshipDeletion( deletedOptions));
            statements.add(getEquipmentOptionsRelationshipDeletion(deletedOptions));
        }

        for (int j = 0; j < optionsCreate.size(); j++) {
            JsonObject option = optionsCreate.getJsonObject(j);
            statements.add(createEquipmentOptionRelationshipStatement(id, option, resultsObject.getInteger("id"+j) ));
            if ( option.getBoolean("required") &&
                    resultsObject.getJsonArray("id_basket_equipments").size() > 0) {
                statements.add(addRequiredOptionToBasketStatement(
                        resultsObject.getJsonArray("id_basket_equipments"),
                        resultsObject.getInteger("id"+j)));
            }
        }
        for (int i = 0; i < optionsUpdate.size(); i++) {
            JsonObject option = optionsUpdate.getJsonObject(i);
            statements.add(updateEquipmentOptionRelationshipStatement(option));
            if ( option.getBoolean("required") &&
                    resultsObject.getJsonArray("id_basket_equipments").size() > 0) {
                statements.add(addRequiredOptionToBasketStatement(
                        resultsObject.getJsonArray("id_basket_equipments"),
                        option.getInteger("id")
                        ));
            }
        }
        if (statements.size() > 0) {
            sql.transaction(statements, new Handler<Message<JsonObject>>() {
                @Override
                public void handle(Message<JsonObject> event) {
                    handler.handle(SqlQueryUtils.getTransactionHandler(event, id));
                }
            });
        } else {
            handler.handle(new Either.Right<String, JsonObject>(new JsonObject().put("id", id)));
        }
    }
    public void delete(final List<Integer> ids, final Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                .add(getEquipmentsOptionsRelationshipDeletion(ids))
                .add(getEquipmentsDeletion(ids));

        sql.transaction(statements, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(SqlQueryUtils.getTransactionHandler(event,ids.get(0)));
            }
        });
    }

    @Override
    public void setStatus(List<Integer> ids, String status, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Lystore.lystoreSchema + ".equipment SET status = ? " +
                "WHERE equipment.id IN " + Sql.listPrepared(ids.toArray());
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(status);

        for (Integer id: ids) {
            params.add(id);
        }

        sql.prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void prepareUpdateOptions (Number optionCreate, Number idEquipment,
                                      final Handler<Either<String, JsonObject>> handler){
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
        statements.add(getBasketIds(idEquipment));
        for(int i = 0 ; i < (int) optionCreate ; i++) {
            statements.add(getOptionsSequences(i));
        }

        sql.transaction(statements, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(getTransactionHandler(event));
            }
        });
    }

    @Override
    public void search(String query, Handler<Either<String, JsonArray>> handler) {
        String sqlQuery = "SELECT e.id, e.name, e.summary, e.description, CAST(e.price AS FLOAT) as price, t.value as tax_amount, t.id as id_tax, e.image, e.reference, e.warranty " +
                "FROM " + Lystore.lystoreSchema + ".equipment as e " +
                "INNER JOIN " + Lystore.lystoreSchema + ".tax as t ON (e.id_tax = t.id) " +
                "WHERE e.status = 'AVAILABLE' " +
                "AND (LOWER(e.name) ~ LOWER(?) " +
                "OR LOWER(reference) ~ LOWER(?));";

        JsonArray params = new JsonArray().add(query).add(query);

        Sql.getInstance().prepared(sqlQuery, params, SqlResult.validResultHandler(handler));
    }

    /**
     * Returns transaction handler. Manage response based on PostgreSQL event
     *
     * @param event PostgreSQL event
     * @return id_basket_equipment : ids of baskets who contains the equipment
     *         a sequence allocation for each option to create
     */
    private static Either<String, JsonObject> getTransactionHandler(Message<JsonObject> event) {
        Either<String, JsonObject> either;
        JsonObject result = event.body();
        if (result.containsKey("status") && "ok".equals(result.getString("status"))) {
            either = new Either.Right<>(formatResults (event.body().getJsonArray("results")));
        } else {
            LOGGER.error("An error occurred when launching transaction");
            either = new Either.Left<>("");
        }
        return either;
    }

    private static JsonObject formatResults(JsonArray result){
        JsonObject returns = new JsonObject();
        for (int i=0; i<result.size() ; i++) {
            JsonObject object = result.getJsonObject(i);
            String fields = object.getJsonArray("fields").getString(0);
            if ("id_basket_equipments".equals(fields)) {
                returns.put(fields, object.getJsonArray("results"));
            } else {
                returns.put(fields, (Number) ((object.getJsonArray("results").getJsonArray(0))).getInteger(0));
            }
        }
        return returns;
    }
    private static JsonObject getBasketIds(Number id) {
        String query = "SELECT id id_basket_equipments " +
                "      FROM lystore.basket_equipment " +
                "    where id_equipment = ? ; ";
        return new JsonObject()
                .put(STATEMENT, query)
                .put(VALUES, new fr.wseduc.webutils.collections.JsonArray().add(id))
                .put(ACTION, PREPARED);
    }
    private static JsonObject getOptionsSequences(int i) {
        String query = "select nextval('lystore.equipment_option_id_seq') as id"+i;
        return new JsonObject()
                .put(STATEMENT, query)
                .put(VALUES, new fr.wseduc.webutils.collections.JsonArray())
                .put(ACTION, PREPARED);
    }
    /**
     * Returns an equipment creation statement
     *
     * @param id        equipment id
     * @param equipment equipment to create
     * @return equipment creation statement
     */
    private JsonObject getEquipmentCreationStatement(Number id, JsonObject equipment) {
        String insertEquipmentQuery =
                "INSERT INTO " + Lystore.lystoreSchema + ".equipment(id, name, summary, description, price, id_tax," +
                        " image, id_contract, status, technical_specs, warranty) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, to_json(?::text), ?) RETURNING id;";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(id)
                .add(equipment.getString("name"))
                .add(equipment.getString("summary"))
                .add(equipment.getString("description"))
                .add(equipment.getFloat("price"))
                .add(equipment.getInteger("id_tax"))
                .add(equipment.getString("image"))
                .add(equipment.getInteger("id_contract"))
                .add(equipment.getString("status"))
                .add(equipment.getJsonArray("technical_specs"))
                .add(equipment.getInteger("warranty"));

        return new JsonObject()
                .put(STATEMENT, insertEquipmentQuery)
                .put(VALUES, params)
                .put(ACTION, PREPARED);
    }

    /**
     * Returns an equipment tag relationship transaction statement
     *
     * @param id    equipment Id
     * @param tagId tag id
     * @return equipment tag relationship transaction statement
     */
    private JsonObject getEquipmentTagRelationshipStatement(Number id, Number tagId) {
        String insertTagEquipmentRelationshipQuery =
                "INSERT INTO " + Lystore.lystoreSchema + ".rel_equipment_tag(id_equipment, id_tag) " +
                        "VALUES (?, ?);";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(id)
                .add(tagId);

        return new JsonObject()
                .put(STATEMENT, insertTagEquipmentRelationshipQuery)
                .put(VALUES, params)
                .put(ACTION, PREPARED);
    }

    /**
     * Create an option for an equipment
     * @param id of the equipment
     * @param option
     * @return Insert statement
     */
    private JsonObject createEquipmentOptionRelationshipStatement(Number id, JsonObject option, Number optionId) {
        String insertTagEquipmentRelationshipQuery =
                "INSERT INTO " + Lystore.lystoreSchema + ".equipment_option" +
                        "(id, amount, required, id_equipment, id_option) " +
                        "VALUES (?, ?, ?, ?, ?);";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(optionId)
                .add(option.getInteger("amount"))
                .add(option.getBoolean("required"))
                .add(id)
                .add(option.getInteger("id_option"));


        return new JsonObject()
                .put(STATEMENT, insertTagEquipmentRelationshipQuery)
                .put(VALUES, params)
                .put(ACTION, PREPARED);
    }
    /**
     * Create an option for an equipment
     * @param id of the equipment
     * @param option
     * @return Insert statement
     */
    private JsonObject getEquipmentOptionRelationshipStatement(Number id, JsonObject option) {
        String insertTagEquipmentRelationshipQuery =
                "INSERT INTO " + Lystore.lystoreSchema + ".equipment_option" +
                        "( amount, required, id_equipment, id_option) " +
                        "VALUES (?, ?, ?, ?);";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(option.getInteger("amount"))
                .add(option.getBoolean("required"))
                .add(id)
                .add(option.getInteger("id_option"));


        return new JsonObject()
                .put(STATEMENT, insertTagEquipmentRelationshipQuery)
                .put(VALUES, params)
                .put(ACTION, PREPARED);
    }


    /**
     * Update an option for an equipment
     * @param option the option
     * @return Insert statement
     */
    private static JsonObject updateEquipmentOptionRelationshipStatement( JsonObject option) {
        String insertTagEquipmentRelationshipQuery =
                "UPDATE lystore.equipment_option " +
                        "SET amount=?, required=?, id_option = ?" +
                        "WHERE id=?;";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(option.getInteger("amount"))
                .add(option.getBoolean("required"))
                .add(option.getInteger("id_option"))
                .add(option.getInteger("id"));


        return new JsonObject()
                .put(STATEMENT, insertTagEquipmentRelationshipQuery)
                .put(VALUES, params)
                .put(ACTION, PREPARED);
    }

    /**
     * add a required option for all equipments in a basket
     * @return Insert statement
     */
    private static JsonObject addRequiredOptionToBasketStatement(JsonArray idsBasket, Number optionId) {
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        StringBuilder insertQuery = new StringBuilder()
                .append("INSERT INTO lystore.basket_option( ")
                .append(" id_basket_equipment, id_option) " )
                .append( " VALUES ");
        for (int i=0; i<idsBasket.size(); i++){
            insertQuery.append("( ?, ?)");
            insertQuery.append( i == idsBasket.size()-1 ? "; " : ", ");

            params.add((idsBasket.getJsonArray(i)).getInteger(0))
                    .add( optionId );
        }



        return new JsonObject()
                .put(STATEMENT, insertQuery.toString())
                .put(VALUES, params)
                .put(ACTION, PREPARED);
    }
    /**
     * Returns the update statement.
     *
     * @param id        resource Id
     * @param equipment equipment to update
     * @return Update statement
     */
    private JsonObject getEquipmentUpdateStatement(Number id, JsonObject equipment) {
        String query = "UPDATE " + Lystore.lystoreSchema + ".equipment SET " +
                "name = ?, summary = ?, description = ?, price = ?, id_tax = ?, image = ?, " +
                "id_contract = ?, status = ?, technical_specs = to_json(?::text) " +
                "WHERE id = ?";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(equipment.getString("name"))
                .add(equipment.getString("summary"))
                .add(equipment.getString("description"))
                .add(equipment.getFloat("price"))
                .add(equipment.getInteger("id_tax"))
                .add(equipment.getString("image"))
                .add(equipment.getInteger("id_contract"))
                .add(equipment.getString("status"))
                .add(equipment.getJsonArray("technical_specs"))
                .add(id);

        return new JsonObject()
                .put(STATEMENT, query)
                .put(VALUES, params)
                .put(ACTION, PREPARED);
    }

    /**
     * Delete options of an equipment
     * @return Delete statement
     */
    private JsonObject getEquipmentOptionsRelationshipDeletion(JsonArray deletedOptions) {
        JsonArray value = new fr.wseduc.webutils.collections.JsonArray();
        String query = "DELETE FROM " + Lystore.lystoreSchema + ".equipment_option " +
                " WHERE id in "+ Sql.listPrepared(deletedOptions.getList());
        for (int i =0 ; i<deletedOptions.size(); i++) {
            value.add((deletedOptions.getJsonObject(i)).getInteger("id"));
        }
        return new JsonObject()
                .put(STATEMENT, query)
                .put(VALUES, value)
                .put(ACTION, PREPARED);
    }
    /**
     * Delete options of an equipment from baskets
     * @return Delete statement
     */
    private JsonObject getEquipmentOptionsBasketRelationshipDeletion(JsonArray deletedOptions) {
        JsonArray value = new fr.wseduc.webutils.collections.JsonArray();
        String query = "DELETE FROM " + Lystore.lystoreSchema + ".basket_option " +
                " WHERE id_option in "+ Sql.listPrepared(deletedOptions.getList());
        for (int i =0 ; i<deletedOptions.size(); i++) {
            value.add((deletedOptions.getJsonObject(i)).getInteger("id") );
        }
        return new JsonObject()
                .put(STATEMENT, query)
                .put(VALUES, value)
                .put(ACTION, PREPARED);
    }
    /**
     * Delete options of  equipments
     * @param ids : equipment ids
     * @return Delete statement
     */
    private JsonObject getEquipmentsOptionsRelationshipDeletion(List<Integer> ids) {
        StringBuilder query = new StringBuilder();
        JsonArray value = new fr.wseduc.webutils.collections.JsonArray();
        query.append("DELETE FROM " + Lystore.lystoreSchema + ".equipment_option ")
                .append(" WHERE id_equipment in ")
                .append(Sql.listPrepared(ids.toArray()));

        for (Integer id : ids) {
            value.add(id);
        }

        return new JsonObject()
                .put(STATEMENT, query.toString())
                .put(VALUES, value)
                .put(ACTION, PREPARED);
    }

    /**
     * Delete equipments
     * @param ids : equipment ids
     * @return Delete statement
     */
    private JsonObject getEquipmentsDeletion(List<Integer> ids) {
        StringBuilder query = new StringBuilder();
        JsonArray value = new fr.wseduc.webutils.collections.JsonArray();
        query.append("DELETE FROM " + Lystore.lystoreSchema + ".equipment ")
                .append(" WHERE id in ")
                .append(Sql.listPrepared(ids.toArray()));

        for (Integer id : ids) {
            value.add(id);
        }

        return new JsonObject()
                .put(STATEMENT, query.toString())
                .put(VALUES, value)
                .put(ACTION, PREPARED);
    }
    private JsonObject getEquipmentTagRelationshipDeletion(Number id) {
        String query = "DELETE FROM " + Lystore.lystoreSchema + ".rel_equipment_tag " +
                " WHERE id_equipment = ?;";

        return new JsonObject()
                .put(STATEMENT, query)
                .put(VALUES, new fr.wseduc.webutils.collections.JsonArray().add(id))
                .put(ACTION, PREPARED);
    }
}
