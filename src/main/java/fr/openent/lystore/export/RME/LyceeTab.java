package fr.openent.lystore.export.RME;

import fr.openent.lystore.Lystore;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;

public class LyceeTab extends Investissement {

    public LyceeTab(Workbook wb, JsonObject instruction) {
        super(wb, instruction, TabName.LYCEE.toString());
        query = " WITH values AS ( " +
                "   SELECT action_code, action_name, action_id, id_program, chapter_label, code_label, type_code AS code, program_action_id_program, " +
                "       id_operation, SUM(price) AS total " +
                "   FROM ( " +
                "       SELECT program_action.action AS action_code, program_action.description AS action_name, program_action.id AS action_id, " +
                "           program_action.id_program AS id_program, chapter.label AS chapter_label, functional_code.label AS code_label, " +
                "           contract_type.code AS type_code, program_action.id_program AS program_action_id_program, orders.id_operation, ROUND(( " +
                "           (SELECT CASE " +
                "               WHEN orders.price_proposal IS NOT NULL THEN 0 " +
                "               WHEN orders.override_region IS NULL THEN 0 " +
                "               WHEN SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount) IS NULL THEN 0 " +
                "               ELSE SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount) " +
                "               END " +
                "           FROM " + Lystore.lystoreSchema + ".order_client_options oco " +
                "           WHERE oco.id_order_client_equipment = orders.id) + orders.\"price TTC\") * orders.amount, 2) AS price " +
                "       FROM (" +
                "           (SELECT ore.id, ore.price AS \"price TTC\", ore.amount, ore.creation_date, ore.modification_date, ore.description, " +
                "               ore.status, ore.id_contract, ore.equipment_key, ore.id_structure, ore.cause_status, ore.number_validation, ore.id_order, " +
                "               ore.rank AS \"prio\", NULL AS price_proposal, ore.id_project, ore.id_operation, NULL AS override_region " +
                "           FROM " + Lystore.lystoreSchema + ".\"order-region-equipment\" ore) " +
                "           UNION ( " +
                "           SELECT oce.id, " +
                "               CASE WHEN price_proposal IS NULL THEN price + (price * tax_amount /100) " +
                "               ELSE price_proposal " +
                "               END AS \"price TTC\", amount, creation_date, NULL AS modification_date, description, status, id_contract, equipment_key, " +
                "               id_structure, cause_status, number_validation, id_order, rank AS \"prio\", price_proposal, id_project, id_operation, " +
                "               override_region " +
                "           FROM " + Lystore.lystoreSchema + ".order_client_equipment oce) " +
                "       ) AS orders " +
                "       INNER JOIN " + Lystore.lystoreSchema + ".operation ON (orders.id_operation = operation.id AND (orders.override_region != true " +
                "           OR orders.override_region IS NULL)) " +
                "       INNER JOIN " + Lystore.lystoreSchema + ".label_operation AS label ON (operation.id_label = label.id) " +
                "       INNER JOIN " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id AND instruction.id = ?) " +
                "       INNER JOIN " + Lystore.lystoreSchema + ".contract ON (orders.id_contract = contract.id) " +
                "       INNER JOIN " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id) " +
                "       LEFT JOIN " + Lystore.lystoreSchema + ".specific_structures ON orders.id_structure = specific_structures.id " +
                "       INNER JOIN " + Lystore.lystoreSchema + ".structure_program_action spa ON (spa.contract_type_id = contract_type.id) AND " +
                "           ((spa.structure_type = '" + LYCEE + "' AND specific_structures.type IS NULL )) " +
                "       INNER JOIN " + Lystore.lystoreSchema + ".program_action ON (spa.program_action_id = program_action.id) " +
                "       INNER JOIN " + Lystore.lystoreSchema + ".program ON (program_action.id_program = program.id) " +
                "       INNER JOIN " + Lystore.lystoreSchema + ".chapter ON (chapter.code = program.chapter) " +
                "       INNER JOIN " + Lystore.lystoreSchema + ".functional_code ON (functional_code.code = program.functional_code) " +
                "       GROUP BY action_code, action_name, action_id, functional_code, chapter_label, code_label, program_action_id_program, " +
                "           id_program, price, type_code, orders.id_operation) AS lycee " +
                "   GROUP BY action_code, action_name, action_id, id_program, chapter_label, code_label, code, program_action_id_program, " +
                "       id_operation)" +
                " SELECT program.*, array_to_json(array_agg(values)) AS actions " +
                " FROM " + Lystore.lystoreSchema + ".program " +
                " INNER JOIN values ON (values.id_program = program.id) " +
                " WHERE program.section =  '" + INVESTISSEMENT + "'" +
                " GROUP BY program.id;";
    }
}
