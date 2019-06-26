package fr.openent.lystore.export;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.helpers.ExcelHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.ArrayList;

public class LyceeTab {
    private Workbook wb;
    private Sheet sheet;
    private JsonObject instruction;
    private ExcelHelper excel;
    private int operationsRowNumber = 9;
    final int yTab = 9;
    final int xTab = 1;
    private int cellColumn = 1;
    /**
     * Format : H-code
     */
    private JsonObject tabx;
    private JsonArray taby;

    private String TITLE = "Récapitulatif des mesures engagées";
    private String SUBTITLE = "Récapitulatif investissement";
    private ArrayList<ArrayList<Float>> priceTab;

    public LyceeTab(Workbook wb, JsonObject instruction) {
        this.wb = wb;
        this.tabx = new JsonObject();
        this.taby = new JsonArray();
        this.instruction = instruction;
        this.sheet = wb.getSheet("Investissement-LYCEES");
        this.excel = new ExcelHelper(wb, sheet);
        priceTab = new ArrayList<ArrayList<Float>>();


    }


    public void create(Handler<Either<String, Boolean>> handler) {
        excel.setDefaultFont();
        excel.setCPNumber(instruction.getString("cp_number"));
        setLabels();
        getPrograms(event -> {
            if (event.isLeft()) {
                handler.handle(new Either.Left<>("Failed to retrieve programs"));
                return;
            }

            JsonArray programs = event.right().getValue();
            setPrograms(programs);
            excel.fillTab(xTab, this.cellColumn, yTab, this.operationsRowNumber);
            initTabValue(xTab, this.cellColumn, yTab, this.operationsRowNumber);
            getPrices(event1 -> {
                if (event.isLeft()) {
                    handler.handle(new Either.Left<>("Failed to retrieve prices"));
                    return;
                }
                handler.handle(new Either.Right<>(true));
            });

        });
    }

    /**
     * Init all the tab
     *
     * @param i
     * @param cellColumn
     * @param j
     * @param operationsRowNumber
     */
    private void initTabValue(int i, int cellColumn, int j, int operationsRowNumber) {
        for (int ii = 0; ii < cellColumn - i; ii++) {
            priceTab.add(ii, new ArrayList<Float>());
            for (int jj = 0; jj < operationsRowNumber - j; jj++) {
                priceTab.get(ii).add(jj, 0.f);
            }

        }
    }

    /**
     * Set labels of the tabs
     */
    private void setLabels() {
        int cellLabelColumn = 0;

        if (this.instruction.getJsonArray("operations").isEmpty()) {
            return;
        }
        JsonArray operations = this.instruction.getJsonArray("operations");
        for (int i = 0; i < operations.size(); i++) {
            JsonObject operation = operations.getJsonObject(i);
            taby.add(operation.getInteger("id"));
            Row operationRow = sheet.createRow(this.operationsRowNumber);
            excel.insertLabel(operationRow, cellLabelColumn, operation.getString("label"));
            this.operationsRowNumber++;
        }
        excel.insertHeader(sheet.createRow(this.operationsRowNumber), cellLabelColumn, excel.totalLabel);
    }

    /**
     * Set the headers of tab
     *
     * @param programs
     */
    private void setPrograms(JsonArray programs) {
        int posx = 0;
        int programRowNumber = 6;
        if (programs.isEmpty()) {
            return;
        }
        Row programRow = sheet.createRow(programRowNumber);
        Row actionDescRow = sheet.getRow(programRowNumber + 1);
        Row actionNumRow = sheet.getRow(programRowNumber + 2);
        for (int i = 0; i < programs.size(); i++) {
            JsonObject program = programs.getJsonObject(i);

            JsonArray actions = program.getJsonArray("actions", new JsonArray());
            if (actions.isEmpty()) continue;
            excel.insertHeader(programRow, cellColumn, program.getString("name"));
            if (actions.size() != 1) {
                CellRangeAddress merge = new CellRangeAddress(programRowNumber, programRowNumber, cellColumn, cellColumn + actions.size() - 1);
                sheet.addMergedRegion(merge);
                excel.setRegionHeader(merge, sheet);
            }
            for (int j = 0; j < actions.size(); j++) {
                JsonObject action = actions.getJsonObject(j);
                if (!tabx.containsKey(action.getInteger("id_program").toString() + "-" + action.getString("code"))) {
                    tabx.put(action.getInteger("id_program").toString() + "-" + action.getString("code"), posx);
                    posx++;
                }

                excel.insertHeader(actionDescRow, cellColumn, action.getString("description"));
                excel.insertHeader(actionNumRow, cellColumn, action.getString("code"));
                this.cellColumn++;
            }


        }
        //addin total
        CellRangeAddress totalMerge = new CellRangeAddress(programRowNumber, programRowNumber + 2, cellColumn, cellColumn);
        sheet.addMergedRegion(totalMerge);
        excel.setRegionHeader(totalMerge, sheet);
        excel.insertHeader(sheet.getRow(programRowNumber), cellColumn, excel.totalLabel);
    }

    /**
     * Insert prices into the tab
     */
    private void setPrices() {
        for (int i = 0; i < priceTab.size(); i++) {
            for (int j = 0; j < priceTab.get(i).size(); j++) {
                if (priceTab.get(i).get(j) != 0.f)
                    excel.insertCellTabFloat(i + xTab, j + yTab, priceTab.get(i).get(j));
            }
        }
        excel.setTotal(cellColumn, operationsRowNumber, xTab, yTab);
    }


    /**
     * Get all the prices of equipments
     *
     * @param handler
     */
    private void getPrices(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT oce.price, oce.amount, oce.tax_amount ,contract_type.code as code, program_action.id_program as id_program ,oce.id_operation " +
                "FROM " + Lystore.lystoreSchema + ".order_client_equipment oce  " +
                "INNER JOIN " + Lystore.lystoreSchema + ".operation ON (oce.id_operation = operation.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".instruction ON (operation.id_instruction = instruction.id)   " +
                "INNER JOIN " + Lystore.lystoreSchema + ".contract ON (oce.id_contract = contract.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".contract_type ON (contract.id_contract_type = contract_type.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".structure_program_action ON (structure_program_action.contract_type_id = contract_type.id) " +
                "INNER JOIN " + Lystore.lystoreSchema + ".program_action ON (structure_program_action.program_action_id = program_action.id)   " +
                "WHERE instruction.id = 1    AND structure_program_action.structure_type = 'LYC'   " +
                "AND oce.id_structure NOT IN (    SELECT id    FROM " + Lystore.lystoreSchema + ".specific_structures    ) " +
                "order by id_program,code,oce.id_operation";

        Sql.getInstance().prepared(query, new JsonArray(), SqlResult.validResultHandler(event -> {
            if (event.isLeft()) {
                handler.handle(event.left());
            } else {
                JsonArray commands = event.right().getValue();
                for (int i = 0; i < commands.size(); i++) {
                    JsonObject command = commands.getJsonObject(i);
                    float priceCommand = (Float.parseFloat(command.getString("price")) +
                            Float.parseFloat(command.getString("price")) * Float.parseFloat(command.getString("tax_amount")) / 100) * command.getLong("amount");

                    for (int y = 0; y < taby.size(); y++) {
                        if (command.getInteger("id_operation") == taby.getInteger(y)) {
                            priceTab.get(tabx.getInteger(command.getInteger("id_program").toString() + "-" + command.getString("code"))).set(y, priceTab.get(0).get(y) + priceCommand);
                        }
                    }
                }
                setPrices();
                handler.handle(new Either.Right<>(commands));
            }
        }));

    }

    /**
     * Get header of the tab
     *
     * @param handler
     */
    private void getPrograms(Handler<Either<String, JsonArray>> handler) {
        String query = "WITH values AS (" +
                "   SELECT distinct contract_type.code, program_action.description, program_action.id_program  " +
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
