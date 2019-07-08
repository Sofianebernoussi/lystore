package fr.openent.lystore.export.investissement;

import fr.openent.lystore.Lystore;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;

public class LyceeTab extends Investissement {


    public LyceeTab(Workbook wb, JsonObject instruction) {
        super(wb, instruction, TabName.LYCEE.toString());
        query = "WITH values AS (" +
                "  SELECT SUM(CASE WHEN oce.price_proposal is not null THEN oce.price_proposal *  oce.amount ELSE (oce.price * oce.amount) + ((oce.price*oce.amount)*oce.tax_amount)/100 END) as Total  ," +
                " contract_type.code as code, program_action.id_program as id_program ,oce.id_operation , contract_type.name " +
                "   FROM " + Lystore.lystoreSchema + ".order_client_equipment oce " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".operation ON (oce.id_operation = operation.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".contract ON (oce.id_contract = contract.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".structure_program_action ON (structure_program_action.contract_type_id = contract_type.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".program_action ON (structure_program_action.program_action_id = program_action.id) " +
                "   WHERE instruction.id = ? " +
                "   AND structure_program_action.structure_type =  '" + LYCEE + "' " +
                "   AND oce.id_structure NOT IN ( " +
                "   SELECT id " +
                "   FROM " + Lystore.lystoreSchema + ".specific_structures " +
                "  )" +
                " Group by  contract_type.code, contract_type.name , program_action.id, oce.id_operation order by id_program,code,oce.id_operation) " +
                "SELECT program.*, array_to_json(array_agg(values)) as actions " +
                "FROM " + Lystore.lystoreSchema + ".program " +
                "INNER JOIN values ON (values.id_program = program.id) " +
                "WHERE program.section =  '" + INVESTISSEMENT + "'" +
                "GROUP BY program.id ";



    }


}
