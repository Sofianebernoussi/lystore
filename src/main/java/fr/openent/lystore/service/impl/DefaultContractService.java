package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.ContractService;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

public class DefaultContractService extends SqlCrudService implements ContractService {

    public DefaultContractService(String schema, String table) {
        super(schema, table);
    }

    public void getContracts(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT contract.id as id, contract.name as name, annual_min, annual_max, " +
                "start_date, nb_renewal, " +
                "id_contract_type, max_brink, id_supplier, id_agent, reference, renewal_end, end_date, " +
                "supplier.name as supplier_display_name " +
                "FROM " + Lystore.lystoreSchema + ".contract INNER JOIN " + Lystore.lystoreSchema +
                ".supplier on (contract.id_supplier = supplier.id)";

        this.sql.prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }

    public void createContract(JsonObject contract, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Lystore.lystoreSchema + ".contract(name, annual_min, " +
                "annual_max, start_date, nb_renewal, id_contract_type, max_brink, id_supplier, id_agent, " +
                "reference, end_date, renewal_end) " +
                "VALUES (?, ?, ?, to_date(?, 'YYYY-MM-DD'), ?, ?, ?, ?, ?, ?, to_date(?, 'YYYY-MM-DD')," +
                " to_date(?, 'YYYY-MM-DD')) " +
                "RETURNING id;";
        JsonArray params = new JsonArray()
                .addString(contract.getString("name"))
                .addNumber(contract.getNumber("annual_min"))
                .addNumber(contract.getNumber("annual_max"))
                .addString(contract.getString("start_date"))
                .addNumber(contract.getNumber("nb_renewal"))
                .addNumber(contract.getNumber("id_contract_type"))
                .addNumber(contract.getNumber("max_brink"))
                .addNumber(contract.getNumber("id_supplier"))
                .addNumber(contract.getNumber("id_agent"))
                .addString(contract.getString("reference"))
                .addString(contract.getString("end_date"))
                .addString(contract.getString("renewal_end"));

        this.sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    public void updateContract(JsonObject contract, Integer id, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Lystore.lystoreSchema + ".contract " +
                "SET name = ?, annual_min = ?, annual_max = ?, start_date = to_date(?, 'YYYY-MM-DD'), nb_renewal = ?," +
                "id_contract_type = ?, max_brink = ?, id_supplier = ?, id_agent = ?, " +
                "reference = ?, end_date = to_date(?, 'YYYY-MM-DD'), renewal_end = to_date(?, 'YYYY-MM-DD') " +
                "WHERE id = ?;";

        JsonArray params = new JsonArray()
                .addString(contract.getString("name"))
                .addNumber(contract.getNumber("annual_min"))
                .addNumber(contract.getNumber("annual_max"))
                .addString(contract.getString("start_date"))
                .addNumber(contract.getNumber("nb_renewal"))
                .addNumber(contract.getNumber("id_contract_type"))
                .addNumber(contract.getNumber("max_brink"))
                .addNumber(contract.getNumber("id_supplier"))
                .addNumber(contract.getNumber("id_agent"))
                .addString(contract.getString("reference"))
                .addString(contract.getString("end_date"))
                .addString(contract.getString("renewal_end"))
                .addNumber(id);

        this.sql.prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }

    public void deleteContract(List<Integer> ids, Handler<Either<String, JsonObject>> handler) {
        SqlUtils.deleteIds(this.table, ids, handler);
    }

    @Override
    public void getContract(JsonArray ids, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT contract.* " +
                "FROM " + Lystore.lystoreSchema + ".order_client_equipment " +
                "INNER JOIN " + Lystore.lystoreSchema + ".contract " +
                "ON (order_client_equipment.id_contract = contract.id) " +
                "WHERE order_client_equipment.id IN " + Sql.listPrepared(ids.toArray()) +
                " GROUP BY contract.id";
        JsonArray params = new JsonArray();

        for (int i = 0; i < ids.size(); i++) {
            params.addNumber((Number) ids.get(i));
        }

        this.sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }
}
