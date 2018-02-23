package fr.openent.lystore.service.impl;




import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.OrderService;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;

import org.entcore.common.sql.SqlResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

public class DefaultOrderService extends SqlCrudService implements OrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger (DefaultOrderService.class);


    public DefaultOrderService(String schema, String table){ super(schema,table);}

    @Override
    public void listOrder(Integer idCampaign, String idStructure, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new JsonArray();
        String query = "SELECT oe.id, oe.price, oe.tax_amount, oe.amount,oe.creation_date, oe.id_campaign," +
                " oe.id_structure, oe.name, oe.summary, oe.image, oe.status, oe.id_contract," +
                " array_to_json(array_agg(order_opts)) as options, c.name as name_supplier  " +
                "FROM "+ Lystore.lystoreSchema + ".order_client_equipment  oe " +
                "LEFT JOIN "+ Lystore.lystoreSchema + ".order_client_options order_opts ON " +
                "oe.id = order_opts.id_order_client_equipment " +
                "INNER JOIN (SELECT supplier.name, contract.id FROM " + Lystore.lystoreSchema + ".supplier INNER JOIN "
                + Lystore.lystoreSchema + ".contract ON contract.id_supplier = supplier.id) c " +
                "ON oe.id_contract = c.id WHERE id_campaign = ? AND id_structure = ? " +
                "GROUP BY (oe.id, c.name) ORDER BY creation_date";

        values.addNumber(idCampaign).addString(idStructure);
        sql.prepared(query, values, SqlResult.validResultHandler(handler));

    }
}
