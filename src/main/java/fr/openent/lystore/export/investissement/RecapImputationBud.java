package fr.openent.lystore.export.investissement;

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
            excel.insertHeader(y, xTab, section);
            if (y - nbToMerge != y - 1) {
                CellRangeAddress merge = new CellRangeAddress(y - nbToMerge, y - 1, xTab, xTab);
                sheet.addMergedRegion(merge);
                nbToMerge = 1;
            }

            oldSection = section;

        } else {
            excel.insertHeader(y, xTab, section);

            nbToMerge++;
        }
        return oldSection;
    }

    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        query = "With values as (" +
                "SELECT orders.id," +
                "contract_type.code AS code, program.name AS program, program.section as program_section," +
                "chapter.code as chapter, chapter.label as chapter_label, functional_code.label as code_label, program.label" +
                "as program_label," +
                "program_action.action as action_code, program_action.description as action_name" +
                "FROM(" +
                "(SELECT ore.id, true AS isregion, ore.price AS\"price TTC\", ore.amount, ore.creation_date, ore.modification_date," +
                "ore.name, ore.summary, ore.description, ore.image, ore.status, ore.id_contract, ore.equipment_key, ore.id_campaign," +
                "ore.id_structure, ore.cause_status, ore.number_validation, ore.id_order, ore.comment, ore.rank AS\"prio\"," +
                "NULL AS price_proposal, ore.id_project, ore.id_order_client_equipment, program, action, ore.id_operation," +
                "NULL AS override_region" +
                "FROM Lystore.\"order-region-equipment\"ore)" +
                "UNION" +
                "(SELECT oce.id, false AS isregion," +
                "CASE WHEN price_proposal IS NULL then price + (price * tax_amount / 100) else price_proposal end" +
                "AS \"price TTC\"," +
                "amount, creation_date, NULL" +
                "AS modification_date, name, summary, description, image, status, id_contract," +
                "equipment_key, id_campaign, id_structure, cause_status, number_validation, id_order, comment, rank AS \"prio\"," +
                "price_proposal, id_project, NULL" +
                "AS id_order_client_equipment, program, action, id_operation, override_region" +
                "FROM Lystore.order_client_equipment oce)" +
                ")AS orders" +
                "INNER JOIN Lystore.operation ON (orders.id_operation = operation.id AND(" +
                "orders.override_region != true OR orders.override_region IS NULL))" +
                "INNER JOIN Lystore.label_operation AS label ON (operation.id_label = label.id)" +
                "INNER JOIN Lystore.instruction ON (operation.id_instruction = instruction.id AND instruction.id = 1)" +
                "INNER JOIN Lystore.contract ON (orders.id_contract = contract.id)" +
                "INNER JOIN Lystore.contract_type ON contract.id_contract_type = contract_type.id" +
                "LEFT JOIN Lystore.specific_structures ON orders.id_structure = specific_structures.id" +
                "INNER JOIN Lystore.program_action ON (orders.action = program_action.action)" +
                "INNER JOIN Lystore.program ON orders.program = program.name" +
                "INNER JOIN Lystore.chapter ON (chapter.code = program.chapter)" +
                "INNER JOIN Lystore.functional_code ON (functional_code.code = program.functional_code)" +
                "GROUP BY" +
                "program.name, contract_type.code, program.functional_code, specific_structures.type, orders.amount, orders.name, orders.equipment_key," +
                "orders.id_operation, orders.id_structure, orders.id, contract.id, label.label, program_action.id_program," +
                "orders.id_order_client_equipment, orders.price_proposal, orders.override_region, orders.comment," +
                "orders.id, orders.isregion, program.section, chapter.code, chapter.label, functional_code.label, program.label," +
                "program_action.action, program_action.description" +
                "ORDER BY id_structure, code, id_structure, program, code)" +
                "SELECT *" +
                "from values";

        Sql.getInstance().prepared(query, new JsonArray().add(instruction.getInteger("id")).add(instruction.getInteger("id")), SqlResult.validResultHandler(event -> {
            if (event.isLeft()) {
                handler.handle(event.left());
            } else {
                programs = event.right().getValue();
                handler.handle(new Either.Right<>(programs));
            }
        }));
    }

}
