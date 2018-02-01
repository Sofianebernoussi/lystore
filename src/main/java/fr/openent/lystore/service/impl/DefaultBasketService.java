package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.BasketService;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.SqlResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

public class DefaultBasketService  extends SqlCrudService implements BasketService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEquipmentService.class);

    public DefaultBasketService(String schema, String table) {
        super(schema, table);
    }
    public void create(final JsonObject basket, final Handler<Either<String, JsonObject>> handler) {
        String getIdQuery = "SELECT nextval('" + Lystore.lystoreSchema + ".basket_equipment_id_seq') as id";
        sql.raw(getIdQuery, SqlResult.validUniqueResultHandler(new Handler<Either<String, JsonObject>>() {
            @Override
            public void handle(Either<String, JsonObject> event) {
                if (event.isRight()) {
                    try {
                        final Number id = event.right().getValue().getNumber("id");
                        JsonArray statements = new JsonArray()
                                .add(getBasketEquipmentCreationStatement(id, basket));

                        JsonArray options = basket.getArray("options");
                        int i = 0;
                        while (null != options && i < options.size() ) {
                            statements.add(getBasketEquipmentOptionCreationStatement(id, (Number) options.get(i)));
                            i++;
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
    /**
     * Returns a basket equipment insert statement
     *
     * @param id    basket Id
     * @param basket basket Object
     * @return basket equipment relationship transaction statement
     */
    private JsonObject getBasketEquipmentCreationStatement(Number id, JsonObject basket) {
        String insertBasketEquipmentRelationshipQuery =
                "INSERT INTO " + Lystore.lystoreSchema + ".basket_equipment(" +
                        "id, amount, processing_date, id_equipment, id_campaign, id_structure)" +
                        "VALUES (?, ?, ?, ?, ?, ?);";

        JsonArray params = new JsonArray()
                .addNumber(id)
                .addNumber(basket.getInteger("amount"))
                .addString(basket.getString("processing_date"))
                .addNumber(basket.getInteger("equipment"))
                .addNumber(basket.getInteger("id_campaign"))
                .addString(basket.getString("id_structure"));

        return new JsonObject()
                .putString("statement", insertBasketEquipmentRelationshipQuery)
                .putArray("values", params)
                .putString("action", "prepared");
    }

    /**
     * Returns a basket equipment-option insert statement
     * @param id id of equipment-basket
     * @param option id of options
     * @return basket equipment-option relationship transaction statement
     */
    private JsonObject getBasketEquipmentOptionCreationStatement(Number id, Number option) {
        String insertBasketEquipmentOptionRelationshipQuery =
                "INSERT INTO " + Lystore.lystoreSchema + ".basket_option " +
                        "(id_basket_equipment, id_option) " +
                        " VALUES (?, ?);";
        JsonArray params = new JsonArray()
                .addNumber(id)
                .addNumber(option);
        return new JsonObject()
                .putString("statement", insertBasketEquipmentOptionRelationshipQuery)
                .putArray("values", params)
                .putString("action", "prepared");
    }

}
