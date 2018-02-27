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

    @Override
    public void listExport(Integer idCampaign, String idStructure, Handler<Either<String, JsonArray>> handler) {
        JsonArray values = new JsonArray();
        String query = "SELECT oe.name as equipment_name, oe.amount as equipment_quantity, " +
                "oe.creation_date as equipment_creation_date, oe.summary as equipment_summary, " +
                "oe.status as equipment_status,cause_status, price_all_options, CASE count(price_all_options) " +
                "WHEN 0 THEN ROUND ((oe.price+( oe.tax_amount*oe.price)/100)*oe.amount,2) "+
                "ELSE ROUND(price_all_options +(oe.price+(oe.tax_amount*oe.price)/100)*oe.amount,2) END as price_total_equipment "+
                "FROM "+ Lystore.lystoreSchema + ".order_client_equipment  oe " +
                "LEFT JOIN (SELECT SUM(( price +( tax_amount*price)/100)*amount) as price_all_options," +
                " id_order_client_equipment FROM "+ Lystore.lystoreSchema + ".order_client_options GROUP BY id_order_client_equipment)" +
                " opts ON oe.id = opts.id_order_client_equipment WHERE id_campaign = ? AND id_structure = ?" +
                " GROUP BY oe.id, price_all_options ORDER BY creation_date";

        values.addNumber(idCampaign).addString(idStructure);
        sql.prepared(query, values, SqlResult.validResultHandler(handler));
    }

    @Override
    public  void listOrder(Handler<Either<String, JsonArray>> handler){
        String query = "SELECT oce.* , to_json(contract.*) contract ,to_json(supplier.*) supplier, " +
                "to_json(campaign.* ) campaign,  array_to_json(array_agg( DISTINCT oco.*)) as options " +
                "FROM lystore.order_client_equipment oce " +
                "LEFT JOIN lystore.order_client_options oco ON oco.id_order_client_equipment = oce.id " +
                "LEFT JOIN lystore.contract ON oce.id_contract = contract.id " +
                "INNER JOIN lystore.supplier ON contract.id_supplier = supplier.id  " +
                "INNER JOIN lystore.campaign ON oce.id_campaign = campaign.id " +
                "GROUP BY (oce.id, contract.id, supplier.id, campaign.id); ";
        sql.prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }
}
