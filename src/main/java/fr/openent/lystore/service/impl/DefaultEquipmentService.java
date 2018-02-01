package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.EquipmentService;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.util.List;

public class DefaultEquipmentService extends SqlCrudService implements EquipmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEquipmentService.class);

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
    public void listEquipments(UserInfos user, Integer idCampaign, String idStructure,
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
                        JsonArray options = equipment.getArray("options");
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

    public void update(final Integer id, JsonObject equipment, final Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new JsonArray()
                .addObject(getEquipmentUpdateStatement(id, equipment))
                .addObject(getEquipmentTagRelationshipDeletion(id))
                .addObject(getEquipmentOptionsRelationshipDeletion(id));

        JsonArray tags = equipment.getArray("tags");
        for (int i = 0; i < tags.size(); i++) {
            statements.add(getEquipmentTagRelationshipStatement(id, (Number) tags.get(i)));
        }
        JsonArray options = equipment.getArray("options");
        for (int j = 0; j < options.size(); j++) {
            statements.add(getEquipmentOptionRelationshipStatement(id, (JsonObject) options.get(j)));
        }
        sql.transaction(statements, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> event) {
                handler.handle(SqlQueryUtils.getTransactionHandler(event, id));
            }
        });
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
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, to_json(?)) RETURNING id;";
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
                .putString("statement", insertEquipmentQuery)
                .putArray("values", params)
                .putString("action", "prepared");
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
                .putString("statement", insertTagEquipmentRelationshipQuery)
                .putArray("values", params)
                .putString("action", "prepared");
    }

    /**
     * Create an option for an equipment
     * @param id of the equipment
     * @param option
     * @return
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
                .putString("statement", insertTagEquipmentRelationshipQuery)
                .putArray("values", params)
                .putString("action", "prepared");
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
                "id_contract = ?, status = ?, technical_specs = to_json(?) " +
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
                .putString("statement", query)
                .putArray("values", params)
                .putString("action", "prepared");
    }

    /**
     * Delete options of an equipment
     * @param id : equipment id
     * @return
     */
    private JsonObject getEquipmentOptionsRelationshipDeletion(Number id) {
        String query = "DELETE FROM " + Lystore.lystoreSchema + ".equipment_option " +
                " WHERE id_equipment = ?;";

        return new JsonObject()
                .putString("statement", query)
                .putArray("values", new JsonArray().addNumber(id))
                .putString("action", "prepared");
    }
    /**
     * Delete options of  equipments
     * @param ids : equipment ids
     * @return
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
                .putString("statement", query.toString())
                .putArray("values", value)
                .putString("action", "prepared");
    }
    /**
     * Delete equipments
     * @param ids : equipment ids
     * @return
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
                .putString("statement", query.toString())
                .putArray("values", value)
                .putString("action", "prepared");
    }
    private JsonObject getEquipmentTagRelationshipDeletion(Number id) {
        String query = "DELETE FROM " + Lystore.lystoreSchema + ".rel_equipment_tag " +
                " WHERE id_equipment = ?;";

        return new JsonObject()
                .putString("statement", query)
                .putArray("values", new JsonArray().addNumber(id))
                .putString("action", "prepared");
    }
}
