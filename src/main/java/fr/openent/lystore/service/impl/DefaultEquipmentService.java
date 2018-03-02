package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.EquipmentService;
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
        String query = "SELECT e.*, tax.value tax_amount, array_to_json(array_agg(opts)) as options , array_to_json((" +
                "SELECT array_agg(id_tag) " +
                "FROM " + Lystore.lystoreSchema + ".equipment " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_equipment_tag ON " +
                "(equipment.id = rel_equipment_tag.id_equipment) " +
                "WHERE e.id = rel_equipment_tag.id_equipment " +
                ")) as tags " +
                "FROM " + Lystore.lystoreSchema + ".equipment e " +
                "Left join ( select option.*, tax.value tax_amount from " + Lystore.lystoreSchema +
                ".equipment_option option " +
                "INNER JOIN  " + Lystore.lystoreSchema + ".tax on tax.id = option.id_tax  ) opts " +
                "ON opts.id_equipment = e.id " +
                "INNER JOIN " + Lystore.lystoreSchema + ".tax on tax.id = e.id_tax "+
                "group by (e.id, tax.id)";

        sql.prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }
    public void equipment(Integer idEquipment,  Handler<Either<String, JsonArray>> handler){
        String query = "SELECT e.*, tax.value tax_amount, array_to_json(array_agg(opts)) as options \n" +
                "                FROM " + Lystore.lystoreSchema + ".equipment e " +
                "                Left join " +
                "                ( select option.*, tax.value tax_amount " +
                "                from " + Lystore.lystoreSchema + ".equipment_option option " +
                "                INNER JOIN  " + Lystore.lystoreSchema + ".tax on tax.id = option.id_tax  )" +
                "                 opts ON opts.id_equipment = e.id " +
                "                INNER JOIN " + Lystore.lystoreSchema + ".tax on tax.id = e.id_tax " +
                "                where e.id = ? " +
                "                group by (e.id, tax.id) ";

        this.sql.prepared(query, new JsonArray().addNumber(idEquipment), SqlResult.validResultHandler(handler));
    }
    public void listEquipments(Integer idCampaign, String idStructure,
                               Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new JsonArray();
        String query = "SELECT e.*, tax.value tax_amount, array_to_json(array_agg(DISTINCT opts)) as options," +
                " array_to_json(array_agg(DISTINCT  rel_equipment_tag.id_tag)) tags " +
                "FROM " + Lystore.lystoreSchema + ".equipment e " +
                "Left join " +
                "( select option.*, tax.value tax_amount from " + Lystore.lystoreSchema + ".equipment_option option " +
                "INNER JOIN " + Lystore.lystoreSchema + ".tax on tax.id = option.id_tax  ) opts " +
                "ON opts.id_equipment = e.id " +
                "INNER JOIN " + Lystore.lystoreSchema + ".tax on tax.id = e.id_tax " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_equipment_tag " +
                "ON (e.id = rel_equipment_tag.id_equipment) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_group_campaign ON " +
                "(rel_group_campaign.id_tag = rel_equipment_tag.id_tag " +
                "AND rel_group_campaign.id_campaign = ? " +
                "AND rel_group_campaign.id_structure_group in " +
                "(select structure_group.id from " + Lystore.lystoreSchema + ".structure_group " +
                "INNER JOIN " + Lystore.lystoreSchema + ".rel_group_structure " +
                "ON rel_group_structure.id_structure_group = structure_group.id " +
                "WHERE rel_group_structure.id_structure = ?))  " +
                "group by (e.id, tax.id) ";
        values.addNumber(idCampaign).addString(idStructure);

        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }

    public void create(final JsonObject equipment, final Handler<Either<String, JsonObject>> handler) {
        String getIdQuery = "SELECT nextval('" + Lystore.lystoreSchema + ".equipment_id_seq') as id";
        sql.raw(getIdQuery, SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    try {
                        final Number id = event.right().getValue().getNumber("id");
                        JsonArray statements = new JsonArray()
                                .add(getEquipmentCreationStatement(id, equipment));

                        JsonArray tags = equipment.getArray("tags");
                        for (int i = 0; i < tags.size(); i++) {
                            statements.add(getEquipmentTagRelationshipStatement(id, (Number) tags.get(i)));
                        }
                        JsonArray options = equipment.getArray("optionsCreate");
                        for (int j = 0; j < options.size(); j++) {
                            statements.add(getEquipmentOptionRelationshipStatement(id, (JsonObject) options.get(j)));
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
        JsonArray statements = new JsonArray()
                .addObject(getEquipmentUpdateStatement(id, equipment))
                .addObject(getEquipmentTagRelationshipDeletion(id));
        JsonArray tags = equipment.getArray("tags");

        for (int i = 0; i < tags.size(); i++) {
            statements.add(getEquipmentTagRelationshipStatement(id, (Number) tags.get(i)));
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
        JsonArray statements = new JsonArray();
        JsonArray deletedOptions  = equipment.getArray("deletedOptions");
        JsonArray optionsCreate =  equipment.getArray("optionsCreate");
        JsonArray optionsUpdate =  equipment.getArray("optionsUpdate");

        if( null != deletedOptions && deletedOptions.size() != 0 ) {
            statements.addObject(getEquipmentOptionsBasketRelationshipDeletion( deletedOptions));
            statements.addObject(getEquipmentOptionsRelationshipDeletion(deletedOptions));
        }

        for (int j = 0; j < optionsCreate.size(); j++) {
            JsonObject option = optionsCreate.get(j);
            statements.add(createEquipmentOptionRelationshipStatement(id, option, resultsObject.getNumber("id"+j) ));
            if ( option.getBoolean("required") &&
                    resultsObject.getArray("id_basket_equipments").size() > 0) {
                statements.add(addRequiredOptionToBasketStatement(
                        resultsObject.getArray("id_basket_equipments"),
                        resultsObject.getNumber("id"+j)));
            }
        }
        for (int i = 0; i < optionsUpdate.size(); i++) {
            JsonObject option = optionsUpdate.get(i);
            statements.add(updateEquipmentOptionRelationshipStatement(option));
            if ( option.getBoolean("required") &&
                    resultsObject.getArray("id_basket_equipments").size() > 0) {
                statements.add(addRequiredOptionToBasketStatement(
                        resultsObject.getArray("id_basket_equipments"),
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
            handler.handle(new Either.Right<String, JsonObject>(new JsonObject().putNumber("id", id)));
        }
    }
    public void delete(final List<Integer> ids, final Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new JsonArray()
                .addObject(getEquipmentsOptionsRelationshipDeletion(ids))
                .addObject(getEquipmentsDeletion(ids));

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
        JsonArray params = new JsonArray()
                .addString(status);

        for (Integer id: ids) {
            params.addNumber(id);
        }

        sql.prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void prepareUpdateOptions (Number optionCreate, Number idEquipment,
                                      final Handler<Either<String, JsonObject>> handler){
        JsonArray statements = new JsonArray();
        statements.addObject(getBasketIds(idEquipment));
        for(int i = 0 ; i < (int) optionCreate ; i++) {
            statements.addObject(getOptionsSequences(i));
        }

        sql.transaction(statements, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(getTransactionHandler(event));
            }
        });
    }

    /**
     * Returns transaction handler. Manage response based on PostgreSQL event
     *
     * @param event PostgreSQL event
     * @param id    resource Id
     * @return id_basket_equipment : ids of baskets who contains the equipment
     *         a sequence allocation for each option to create
     */
    private static Either<String, JsonObject> getTransactionHandler(Message<JsonObject> event) {
        Either<String, JsonObject> either;
        JsonObject result = event.body();
        if (result.containsField("status") && "ok".equals(result.getString("status"))) {
            either = new Either.Right<>(formatResults (event.body().getArray("results")));
        } else {
            LOGGER.error("An error occurred when launching transaction");
            either = new Either.Left<>("");
        }
        return either;
    }

    private static JsonObject formatResults(JsonArray result){
        JsonObject returns = new JsonObject();
        for (int i=0; i<result.size() ; i++) {
            JsonObject object = result.get(i);
            String fields = object.getArray("fields").get(0);
            if ("id_basket_equipments".equals(fields)) {
                returns.putArray(fields, (JsonArray) object.getArray("results"));
            } else {
                returns.putNumber(fields, (Number) ((JsonArray) (object.getArray("results").get(0))).get(0));
            }
        }
        return returns;
    }
    private static JsonObject getBasketIds(Number id) {
        String query = "SELECT id id_basket_equipments " +
                "      FROM lystore.basket_equipment " +
                "    where id_equipment = ? ; ";
        return new JsonObject()
                .putString(STATEMENT, query)
                .putArray(VALUES, new JsonArray().addNumber(id))
                .putString(ACTION, PREPARED);
    }
    private static JsonObject getOptionsSequences(int i) {
        String query = "select nextval('lystore.equipment_option_id_seq') as id"+i;
        return new JsonObject()
                .putString(STATEMENT, query)
                .putArray(VALUES, new JsonArray())
                .putString(ACTION, PREPARED);
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
                        " image, id_contract, status, technical_specs) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, to_json(?::text)) RETURNING id;";
        JsonArray params = new JsonArray()
                .addNumber(id)
                .addString(equipment.getString("name"))
                .addString(equipment.getString("summary"))
                .addString(equipment.getString("description"))
                .addNumber(equipment.getNumber("price"))
                .addNumber(equipment.getNumber("id_tax"))
                .addString(equipment.getString("image"))
                .addNumber(equipment.getNumber("id_contract"))
                .addString(equipment.getString("status"))
                .addArray(equipment.getArray("technical_specs"));

        return new JsonObject()
                .putString(STATEMENT, insertEquipmentQuery)
                .putArray(VALUES, params)
                .putString(ACTION, PREPARED);
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

        JsonArray params = new JsonArray()
                .addNumber(id)
                .addNumber(tagId);

        return new JsonObject()
                .putString(STATEMENT, insertTagEquipmentRelationshipQuery)
                .putArray(VALUES, params)
                .putString(ACTION, PREPARED);
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
                        "(id, name, price, amount, required, id_tax, id_equipment) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?);";

        JsonArray params = new JsonArray()
                .addNumber(optionId)
                .addString(option.getString("name"))
                .addNumber(option.getNumber("price"))
                .addNumber(option.getNumber("amount"))
                .addBoolean(option.getBoolean("required"))
                .addNumber(option.getNumber("id_tax"))
                .addNumber(id);


        return new JsonObject()
                .putString(STATEMENT, insertTagEquipmentRelationshipQuery)
                .putArray(VALUES, params)
                .putString(ACTION, PREPARED);
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
                        "(name, price, amount, required, id_tax, id_equipment) " +
                        "VALUES ( ?, ?, ?, ?, ?, ?);";

        JsonArray params = new JsonArray()
                .addString(option.getString("name"))
                .addNumber(option.getNumber("price"))
                .addNumber(option.getNumber("amount"))
                .addBoolean(option.getBoolean("required"))
                .addNumber(option.getNumber("id_tax"))
                .addNumber(id);


        return new JsonObject()
                .putString(STATEMENT, insertTagEquipmentRelationshipQuery)
                .putArray(VALUES, params)
                .putString(ACTION, PREPARED);
    }


    /**
     * Update an option for an equipment
     * @param option the option
     * @return Insert statement
     */
    private static JsonObject updateEquipmentOptionRelationshipStatement( JsonObject option) {
        String insertTagEquipmentRelationshipQuery =
                "UPDATE lystore.equipment_option " +
                        "SET  name=?, price=?, amount=?, required=?, id_tax=? " +
                        "WHERE id=?;";

        JsonArray params = new JsonArray()
                .addString(option.getString("name"))
                .addNumber(option.getNumber("price"))
                .addNumber(option.getNumber("amount"))
                .addBoolean(option.getBoolean("required"))
                .addNumber(option.getNumber("id_tax"))
                .addNumber(option.getNumber("id"));


        return new JsonObject()
                .putString(STATEMENT, insertTagEquipmentRelationshipQuery)
                .putArray(VALUES, params)
                .putString(ACTION, PREPARED);
    }

    /**
     * add a required option for all equipments in a basket
     * @param option the option
     * @return Insert statement
     */
    private static JsonObject addRequiredOptionToBasketStatement(JsonArray idsBasket, Number optionId) {
        JsonArray params = new JsonArray();
        StringBuilder insertQuery = new StringBuilder()
                .append("INSERT INTO lystore.basket_option( ")
                .append(" id_basket_equipment, id_option) " )
                .append( " VALUES ");
        for (int i=0; i<idsBasket.size(); i++){
            insertQuery.append("( ?, ?)");
            insertQuery.append( i == idsBasket.size()-1 ? "; " : ", ");

            params.addNumber( (Number) ((JsonArray) idsBasket.get(i)).get(0))
                    .addNumber( optionId );
        }



        return new JsonObject()
                .putString(STATEMENT, insertQuery.toString())
                .putArray(VALUES, params)
                .putString(ACTION, PREPARED);
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

        JsonArray params = new JsonArray()
                .addString(equipment.getString("name"))
                .addString(equipment.getString("summary"))
                .addString(equipment.getString("description"))
                .addNumber(equipment.getNumber("price"))
                .addNumber(equipment.getNumber("id_tax"))
                .addString(equipment.getString("image"))
                .addNumber(equipment.getNumber("id_contract"))
                .addString(equipment.getString("status"))
                .addArray(equipment.getArray("technical_specs"))
                .addNumber(id);

        return new JsonObject()
                .putString(STATEMENT, query)
                .putArray(VALUES, params)
                .putString(ACTION, PREPARED);
    }

    /**
     * Delete options of an equipment
     * @param id : ids of Options to delete
     * @return Delete statement
     */
    private JsonObject getEquipmentOptionsRelationshipDeletion(JsonArray deletedOptions) {
        JsonArray value = new JsonArray();
        String query = "DELETE FROM " + Lystore.lystoreSchema + ".equipment_option " +
                " WHERE id in "+ Sql.listPrepared(deletedOptions.toArray());
        for (int i =0 ; i<deletedOptions.size(); i++) {
            value.addNumber((Number)((JsonObject) deletedOptions.get(i)).getNumber("id") );
        }
        return new JsonObject()
                .putString(STATEMENT, query)
                .putArray(VALUES, value)
                .putString(ACTION, PREPARED);
    }
    /**
     * Delete options of an equipment from baskets
     * @param id :  ids of Options to delete
     * @return Delete statement
     */
    private JsonObject getEquipmentOptionsBasketRelationshipDeletion(JsonArray deletedOptions) {
        JsonArray value = new JsonArray();
        String query = "DELETE FROM " + Lystore.lystoreSchema + ".basket_option " +
                " WHERE id_option in "+ Sql.listPrepared(deletedOptions.toArray());
        for (int i =0 ; i<deletedOptions.size(); i++) {
            value.addNumber((Number)((JsonObject) deletedOptions.get(i)).getNumber("id") );
        }
        return new JsonObject()
                .putString(STATEMENT, query)
                .putArray(VALUES, value)
                .putString(ACTION, PREPARED);
    }
    /**
     * Delete options of  equipments
     * @param ids : equipment ids
     * @return Delete statement
     */
    private JsonObject getEquipmentsOptionsRelationshipDeletion(List<Integer> ids) {
        StringBuilder query = new StringBuilder();
        JsonArray value = new JsonArray();
        query.append("DELETE FROM " + Lystore.lystoreSchema + ".equipment_option ")
                .append(" WHERE id_equipment in ")
                .append(Sql.listPrepared(ids.toArray()));

        for (Integer id : ids) {
            value.addNumber(id);
        }

        return new JsonObject()
                .putString(STATEMENT, query.toString())
                .putArray(VALUES, value)
                .putString(ACTION, PREPARED);
    }

    /**
     * Delete equipments
     * @param ids : equipment ids
     * @return Delete statement
     */
    private JsonObject getEquipmentsDeletion(List<Integer> ids) {
        StringBuilder query = new StringBuilder();
        JsonArray value = new JsonArray();
        query.append("DELETE FROM " + Lystore.lystoreSchema + ".equipment ")
                .append(" WHERE id in ")
                .append(Sql.listPrepared(ids.toArray()));

        for (Integer id : ids) {
            value.addNumber(id);
        }

        return new JsonObject()
                .putString(STATEMENT, query.toString())
                .putArray(VALUES, value)
                .putString(ACTION, PREPARED);
    }
    private JsonObject getEquipmentTagRelationshipDeletion(Number id) {
        String query = "DELETE FROM " + Lystore.lystoreSchema + ".rel_equipment_tag " +
                " WHERE id_equipment = ?;";

        return new JsonObject()
                .putString(STATEMENT, query)
                .putArray(VALUES, new JsonArray().addNumber(id))
                .putString(ACTION, PREPARED);
    }
}
