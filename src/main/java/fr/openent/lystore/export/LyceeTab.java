package fr.openent.lystore.export;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.helpers.ExcelHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class LyceeTab {
    private Workbook wb;
    private Sheet sheet;
    private JsonObject instruction;
    private ExcelHelper excel;

    private String TITLE = "Récapitulatif des mesures engagées";
    private String SUBTITLE = "Récapitulatif investissement";

    public LyceeTab(Workbook wb, JsonObject instruction) {
        this.wb = wb;
        this.instruction = instruction;
        this.sheet = wb.createSheet("Investissement - CMD");
        this.excel = new ExcelHelper(wb, sheet);
    }


    public void create(Handler<Either<String, Boolean>> handler) {
        excel.setDefaultFont();
        excel.setCPNumber(instruction.getString("cp_number"));
        excel.setTitle(TITLE);
        excel.setSubTitle(SUBTITLE);
        setTableTitle();

        getPrograms(event -> {
            if (event.isLeft()) {
                handler.handle(new Either.Left<>("Failed to retrieve programs"));
                return;
            }

            JsonArray programs = event.right().getValue();
            setPrograms(programs);
        });
        handler.handle(new Either.Right<>(true));
    }

    private void setPrograms(JsonArray programs) {
        int cell = 1;
        int programRowNumber = 6;
        if (programs.isEmpty()) {
            return;
        }
        Row programRow = sheet.createRow(programRowNumber);
        for (int i = 0; i < programs.size(); i++) {
            JsonObject program = programs.getJsonObject(i);
            JsonArray actions = program.getJsonArray("actions", new JsonArray());
            Cell programCell;
            if (actions.isEmpty()) continue;
            programCell = programRow.createCell(cell);
            programCell.setCellValue(program.getString("name"));
            if (actions.size() == 1) {
                JsonObject action = actions.getJsonObject(0);

                cell++;
            } else {
                CellRangeAddress merge = new CellRangeAddress(programRowNumber, programRowNumber, cell, cell + actions.size());
                sheet.addMergedRegion(merge);
                cell += actions.size();
            }
        }
    }

    private void setTableTitle() {
        Font font = wb.createFont();
        Row row = sheet.createRow(7);
        Cell cell = row.createCell(0);
        cell.setCellValue("LYCEES");
        CellRangeAddress merge = new CellRangeAddress(7, 8, 0, 0);
        sheet.addMergedRegion(merge);
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.SKY_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setBold(true);
        style.setFont(font);
        cell.setCellStyle(style);
    }

    private void getPrograms(Handler<Either<String, JsonArray>> handler) {
        String query = "WITH values AS (" +
                "   SELECT distinct contract_type.code, program_action.description, program_action.id_program " +
                "   FROM " + Lystore.lystoreSchema + ".order_client_equipment oce " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".operation ON (oce.id_operation = operation.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".contract ON (oce.id_contract = contract.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".structure_program_action ON (structure_program_action.contract_type_id = contract_type.id) " +
                "   INNER JOIN " + Lystore.lystoreSchema + ".program_action ON (structure_program_action.program_action_id = program_action.id) " +
                "   WHERE instruction.id = ? " +
                "   AND structure_program_action.structure_type = 'LYC' " +
                "   AND oce.id_structure NOT IN ( " +
                "   SELECT id " +
                "   FROM " + Lystore.lystoreSchema + ".specific_structures " +
                "   )" +
                ") " +
                "SELECT program.*, array_to_json(array_agg(values)) as actions " +
                "FROM " + Lystore.lystoreSchema + ".program " +
                "INNER JOIN values ON (values.id_program = program.id) " +
                "GROUP BY program.id";

        Sql.getInstance().prepared(query, new JsonArray().add(instruction.getInteger("id")), SqlResult.validResultHandler(event -> {
            if (event.isLeft()) {
                handler.handle(event.left());
            } else {
                JsonArray programs = event.right().getValue();
                for (int i = 0; i < programs.size(); i++) {
                    JsonObject program = programs.getJsonObject(i);
                    program.put("actions", new JsonArray(program.getString("actions")));
                }

                handler.handle(new Either.Right<>(programs));
            }
        }));
    }
}
