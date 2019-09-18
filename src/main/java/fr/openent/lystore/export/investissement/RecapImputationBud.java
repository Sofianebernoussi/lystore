package fr.openent.lystore.export.investissement;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.TabHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class RecapImputationBud extends TabHelper {
    private JsonArray programs;
    private final int xTab = 0;
    private final int yTab = 7;
    private int nbToMerge = 1;

    public RecapImputationBud(Workbook workbook, JsonObject instruction) {

        super(workbook, instruction, TabName.IMPUTATION_BUDG.toString());
        excel.setDefaultFont();
        excel.setCPNumber(instruction.getString("cp_number"));
    }

    @Override
    public void create(Handler<Either<String, Boolean>> handler) {
        getDatas(event -> {
            try{
                if (event.isLeft()) {
                    handler.handle(new Either.Left<>("Failed to retrieve programs"));
                    return;
                }
                setArray(programs);
                handler.handle(new Either.Right<>(true));
            }catch(Exception e){
                logger.error(e.getMessage());
                logger.error(e.getStackTrace());
                handler.handle(new Either.Left<>("error when creating excel"));
            }
        });
    }

    @Override
    protected void setArray(JsonArray programs) {
        JsonObject program;
        String oldSection = "";
        for (int i = 0; i < programs.size(); i++) {
            program = programs.getJsonObject(i);
            oldSection = insertSetion(program.getString("section"), oldSection, xTab, yTab + i);
            excel.insertCellTab(xTab + 1, yTab + i, program.getInteger("chapter").toString() + " - " + program.getString("chapter_label"));
            excel.insertCellTab(xTab + 2, yTab + i, program.getInteger("functional_code").toString() + " - " + program.getString("code_label"));
            excel.insertCellTab(xTab + 3, yTab + i, program.getString("program_name"));
            excel.insertCellTab(xTab + 4, yTab + i, program.getString("program_label"));
            excel.insertCellTabCenter(xTab + 5, yTab + i, program.getString("action_code"));
            excel.insertCellTab(xTab + 6, yTab + i, program.getString("action_name"));
            excel.insertCellTabFloatWithPrice(xTab + 7, yTab + i, Float.parseFloat(program.getString("total")));


        }
    }

    private String insertSetion(String section, String oldSection, int xTab, int y) {
        if (!section.equals(oldSection)) {
            excel.insertHeader(xTab, y, section);
            if (y - nbToMerge != y - 1) {
//                CellRangeAddress merge = new CellRangeAddress(y - nbToMerge, y - 1, xTab, xTab);
//                sheet.addMergedRegion(merge);
//                nbToMerge = 1;
            }

            oldSection = section;

        } else {
            excel.insertHeader(xTab, y, section);

            nbToMerge++;
        }
        return oldSection;
    }

    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        query = " WITH values AS ( " +
                "   SELECT program.section AS section, program_action.description AS action_name, program_action.id AS action_id, " +
                "       program.name AS program_name, program.id AS program_id, program.label AS program_label, program.functional_code, program.chapter, " +
                "       chapter.label AS chapter_label, functional_code.label AS code_label, program_action.action AS action_code, ROUND((( " +
                "       SELECT CASE " +
                "           WHEN orders.price_proposal IS NOT NULL THEN 0 " +
                "           WHEN orders.override_region IS NULL THEN 0 " +
                "           WHEN SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount) IS NULL THEN 0 " +
                "           ELSE SUM(oco.price + ((oco.price * oco.tax_amount) /100) * oco.amount) " +
                "           END " +
                "       FROM " + Lystore.lystoreSchema + ".order_client_options oco " +
                "       WHERE oco.id_order_client_equipment = orders.id) + orders.\"price TTC\") * orders.amount, 2) AS total " +
                "   FROM (( " +
                "       SELECT ore.id, ore.price AS \"price TTC\", ore.amount, ore.creation_date, ore.modification_date, ore.description, " +
                "           ore.status, ore.id_contract, ore.equipment_key, ore.id_structure, ore.cause_status, ore.number_validation, ore.id_order, " +
                "           ore.rank AS \"prio\", NULL AS price_proposal, ore.id_project, ore.id_operation, NULL AS override_region " +
                "       FROM " + Lystore.lystoreSchema + ".\"order-region-equipment\" ore " +
                "   ) " +
                "   UNION ( " +
                "       SELECT oce.id, " +
                "           CASE WHEN price_proposal IS NULL THEN price + (price * tax_amount /100) " +
                "           ELSE price_proposal " +
                "           END AS \"price TTC\", amount, creation_date, NULL AS modification_date, description, status, id_contract, equipment_key, " +
                "           id_structure, cause_status, number_validation, id_order, rank AS \"prio\", price_proposal, id_project, id_operation, " +
                "           override_region " +
                "       FROM " + Lystore.lystoreSchema + ".order_client_equipment oce) " +
                "   ) AS orders " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".operation ON (orders.id_operation = operation.id AND (orders.override_region != true OR orders.override_region IS NULL)) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".label_operation AS label ON (operation.id_label = label.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id AND instruction.id = ?) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".contract ON (orders.id_contract = contract.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id) " +
                "   LEFT JOIN " + Lystore.lystoreSchema + ".specific_structures ON orders.id_structure = specific_structures.id " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".structure_program_action spa ON (spa.contract_type_id = contract_type.id) AND " +
                "       ((spa.structure_type = 'LYC' AND specific_structures.type IS NULL )) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".program_action ON (spa.program_action_id = program_action.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".program ON (program_action.id_program = program.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".chapter ON (chapter.code = program.chapter) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".functional_code ON (functional_code.code = program.functional_code) " +
                "   GROUP BY action_code, action_name, action_id, program.section, program_name, program_id, program_label, functional_code, " +
                "       chapter, chapter_label, code_label, total " +
                "   ORDER BY section) " +
                " SELECT action_code, action_name, action_id, section, program_name, program_id, program_label, functional_code, " +
                "   chapter, chapter_label, code_label, SUM(total) AS total " +
                " FROM values " +
                " GROUP BY action_code, action_name, action_id, section, program_name, program_id, program_label, functional_code, chapter, " +
                "   chapter_label, code_label;";

        Sql.getInstance().prepared(query, new JsonArray().add(instruction.getInteger("id")), SqlResult.validResultHandler(event -> {
            if (event.isLeft()) {
                handler.handle(event.left());
            } else {
                programs = event.right().getValue();
                handler.handle(new Either.Right<>(programs));
            }
        }));
    }

}
