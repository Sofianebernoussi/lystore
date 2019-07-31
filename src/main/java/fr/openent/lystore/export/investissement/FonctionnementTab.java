package fr.openent.lystore.export.investissement;

import fr.openent.lystore.Lystore;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;

public class FonctionnementTab extends Investissement {

    /**
     * Format : H-code
     */

    public FonctionnementTab(Workbook wb, JsonObject instruction) {
        super(wb, instruction, TabName.FONCTIONNEMENT.toString());
        query = "WITH values AS (" +
                "    (SELECT SUM(" +
                " CASE WHEN oce.price_proposal is not null" +
                "  THEN oce.price_proposal *  oce.amount ELSE (oce.price * oce.amount) +" +
                " ((oce.price*oce.amount)*oce.tax_amount)/100 + " +
                " ( " +
                "           SELECT CASE WHEN  ROUND(SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount), 2)  IS NULL " +
                "           THEN 0  " +
                "           ELSE  ROUND(SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount), 2)  " +
                "           END " +
                "           from lystore.order_client_options oco  " +
                "          WHERE id_order_client_equipment = oce.id  " +
                " )  " +
                "END) as Total  ," +
                "   contract_type.code as code, program_action.id_program as id_program ,oce.id_operation , contract_type.name " +
                "   FROM " + Lystore.lystoreSchema + ".order_client_equipment oce " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".operation ON (oce.id_operation = operation.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".contract ON (oce.id_contract = contract.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".structure_program_action ON (structure_program_action.contract_type_id = contract_type.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".program_action ON (structure_program_action.program_action_id = program_action.id) " +
                "   WHERE instruction.id = ?" +
                "   AND oce.override_region = false " +
                "   Group by  contract_type.code, contract_type.name , program_action.id, oce.id_operation order by id_program,code,oce.id_operation) " +
                " UNION " +
                "    (SELECT SUM(ore.price *  ore.amount ) as Total  ," +
                "   contract_type.code as code, program_action.id_program as id_program ,ore.id_operation , contract_type.name " +
                " FROM  " + Lystore.lystoreSchema + ".\"order-region-equipment\" ore   " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".operation ON (ore.id_operation = operation.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".contract ON (ore.id_contract = contract.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".structure_program_action ON (structure_program_action.contract_type_id = contract_type.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".program_action ON (structure_program_action.program_action_id = program_action.id) " +
                "   WHERE instruction.id = ?" +
                "   Group by  contract_type.code, contract_type.name , program_action.id, ore.id_operation order by id_program,code,ore.id_operation)) " +
                "SELECT program.*, array_to_json(array_agg(values)) as actions " +
                "FROM " + Lystore.lystoreSchema + ".program " +
                "INNER JOIN values ON (values.id_program = program.id) " +
                "WHERE program.section = '" + FONCTIONNEMENT + "'" +
                "GROUP BY program.id";

    }
}
