package fr.openent.lystore.service.impl;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.service.ContractService;
import fr.openent.lystore.utils.SqlQueryUtils;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

public class DefaultContractService extends SqlCrudService implements ContractService {
    private Sql sql;

    public DefaultContractService(String schema, String table) {
        super(schema, table);
        this.sql = Sql.getInstance();
    }

    public void getContracts(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT contract.id as id, contract.name as name, annual_min, annual_max, start_date, nb_renewal, " +
                "id_contract_type, max_brink, id_supplier, id_agent, id_program, reference, renewal_end, end_date, " +
                "supplier.name as supplier_display_name " +
                "FROM " + Lystore.LYSTORE_SCHEMA + ".contract INNER JOIN " + Lystore.LYSTORE_SCHEMA +
                ".supplier on (contract.id_supplier = supplier.id)";

        this.sql.prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }

    public void createContract(JsonObject contract, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Lystore.LYSTORE_SCHEMA + ".contract(name, annual_min, " +
                "annual_max, start_date, nb_renewal, id_contract_type, max_brink, id_supplier, id_agent, " +
                "id_program, reference, end_date, renewal_end) " +
                "VALUES (?, ?, ?, to_date(?, 'YYYY-MM-DD'), ?, ?, ?, ?, ?, ?, ?, to_date(?, 'YYYY-MM-DD'), to_date(?, 'YYYY-MM-DD')) " +
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
                .addNumber(contract.getNumber("id_program"))
                .addString(contract.getString("reference"))
                .addString(contract.getString("end_date"))
                .addString(contract.getString("renewal_end"));

        this.sql.prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    public void updateContract(JsonObject contract, Integer id, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Lystore.LYSTORE_SCHEMA + ".contract " +
                "SET name = ?, annual_min = ?, annual_max = ?, start_date = to_date(?, 'YYYY-MM-DD'), nb_renewal = ?," +
                "id_contract_type = ?, max_brink = ?, id_supplier = ?, id_agent = ?, id_program = ?," +
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
                .addNumber(contract.getNumber("id_program"))
                .addString(contract.getString("reference"))
                .addString(contract.getString("end_date"))
                .addString(contract.getString("renewal_end"))
                .addNumber(id);

        this.sql.prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }

    public void deleteContract(List<Integer> ids, Handler<Either<String, JsonObject>> handler) {
        StringBuilder query = new StringBuilder("DELETE FROM " + Lystore.LYSTORE_SCHEMA + ".contract WHERE ")
                .append(SqlQueryUtils.prepareMultipleIds(ids));
        JsonArray params = new JsonArray();
        for (Integer id : ids) {
            params.addNumber(id);
        }

        sql.prepared(query.toString(), params, SqlResult.validUniqueResultHandler(handler));
    }
}