package fr.openent.lystore.export;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.ArrayList;

public abstract class Investissement extends TabHelper {

    JsonArray operations;
    public Investissement(Workbook wb, JsonObject instruction, String TabName) {
        super(wb, instruction, TabName);


    }


    @Override
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
            //Delete tab if empty
            if (programs.size() == 0) {
                wb.removeSheetAt(wb.getSheetIndex(sheet));
                handler.handle(new Either.Right<>(true));
                return;
            }
            setArray(programs);
                handler.handle(new Either.Right<>(true));
        });
    }



    /**
     * Init all the tab
     *
     * @param i                   xInit
     * @param cellColumn          xMax
     * @param j                   yInit
     * @param operationsRowNumber yMax
     */
    @Override
    protected void initTabValue(int i, int cellColumn, int j, int operationsRowNumber) {
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
    @Override
    protected void setLabels() {
        int cellLabelColumn = 0;

        if (this.instruction.getJsonArray("operations").isEmpty()) {
            return;
        }
        operations = this.instruction.getJsonArray("operations");
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
     * Set the headers of tab for investissement
     *
     * @param programs
     */
    @Override
    protected void setArray(JsonArray programs) {
        int posx = 0;
        String code = "-1";

        int numberActions;
        int programRowNumber = 6;
        if (programs.isEmpty()) {
            return;
        }
        Row programRow = sheet.createRow(programRowNumber);

        for (int i = 0; i < programs.size(); i++) {
            JsonObject program = programs.getJsonObject(i);

            JsonArray actions = program.getJsonArray("actions", new JsonArray());
            if (actions.isEmpty()) continue;
            excel.insertHeader(programRow, cellColumn, program.getString("name"));
            numberActions = nbAction(actions);
            //check if merged region necessary
            if (numberActions != 1) {
                CellRangeAddress merge = new CellRangeAddress(programRowNumber, programRowNumber, cellColumn, cellColumn + numberActions - 1);
                sheet.addMergedRegion(merge);
                excel.setRegionHeader(merge, sheet);
            }

            posx += treatActions(actions, code, posx, programRowNumber);
            excel.fillTab(xTab, this.cellColumn, yTab, this.operationsRowNumber);
        }
        excel.setTotal(cellColumn, operationsRowNumber, xTab, yTab);
        //addin total
        CellRangeAddress totalMerge = new CellRangeAddress(programRowNumber, programRowNumber + 2, cellColumn, cellColumn);
        sheet.addMergedRegion(totalMerge);
        excel.setRegionHeader(totalMerge, sheet);
        excel.insertHeader(sheet.getRow(programRowNumber), cellColumn, excel.totalLabel);

    }

    private int treatActions(JsonArray actions, String code, int posx, int programRowNumber) {
        Row actionDescRow = sheet.getRow(programRowNumber + 1);
        Row actionNumRow = sheet.getRow(programRowNumber + 2);
        for (int j = 0; j < actions.size(); j++) {
            JsonObject action = actions.getJsonObject(j);
            if (!code.equals(action.getString("code"))) {
                code = action.getString("code");

                if (!tabx.containsKey(action.getInteger("id_program").toString() + "-" + action.getString("code"))) {
                    tabx.put(action.getInteger("id_program").toString() + "-" + action.getString("code"), posx);
                    posx++;
                    System.out.println(posx);
                }

                excel.insertHeader(actionDescRow, cellColumn, action.getString("name"));
                excel.insertHeader(actionNumRow, cellColumn, action.getString("code"));
                this.cellColumn++;

            }

            for (int oIndex = 0; oIndex < operations.size(); oIndex++) {
                JsonObject operation = operations.getJsonObject(oIndex);
                if (action.getInteger("id_operation").equals(operation.getInteger("id"))) {
                    excel.insertCellTabFloat(posx, oIndex + yTab, action.getFloat("total"));
                }

            }
        }
        return posx;
    }

    private int nbAction(JsonArray actions) {
        int nbActions = 0;
        String idAction = "-1";
        for (int i = 0; i < actions.size(); i++) {
            if (!idAction.equals(actions.getJsonObject(i).getString("code"))) {
                idAction = actions.getJsonObject(i).getString("code");
                nbActions++;
            }
        }
        return nbActions;
    }
    /**
     * Insert prices into the tab
     */
    @Override
    protected void setPrices() {
        for (int i = 0; i < priceTab.size(); i++) {
            for (int j = 0; j < priceTab.get(i).size(); j++) {
                if (priceTab.get(i).get(j) != 0.f)
                    excel.insertCellTabFloat(i + xTab, j + yTab, priceTab.get(i).get(j));
            }
        }
        excel.setTotal(cellColumn, operationsRowNumber, xTab, yTab);
    }


}
