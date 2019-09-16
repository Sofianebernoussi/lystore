package fr.openent.lystore.export.investissement;

import fr.openent.lystore.export.TabHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public abstract class Investissement extends TabHelper {

    JsonArray operations;
    private String actionStr = "actions";
    public Investissement(Workbook wb, JsonObject instruction, String TabName) {
        super(wb, instruction, TabName);
    }


    @Override
    public void create(Handler<Either<String, Boolean>> handler) {
        excel.setDefaultFont();
        excel.setCPNumber(instruction.getString("cp_number"));
        try {
            setLabels();
        }catch(Exception e){
            logger.error(e.getMessage());
            logger.error(e.getStackTrace());
            handler.handle(new Either.Left<>("error when creating excel"));
        }
        getDatas(event -> {
            try{
                if (event.isLeft()) {
                    log.error("Failed to retrieve programs");
                    handler.handle(new Either.Left<>("Failed to retrieve programs"));
                } else {

                    JsonArray programs = event.right().getValue();
                    //Delete tab if empty

                    setArray(programs);
                    if (programs.size() == 0) {
                        wb.removeSheetAt(wb.getSheetIndex(sheet));
                    }
                    handler.handle(new Either.Right<>(true));
                }
            }catch(Exception e){
                logger.error(e.getMessage());
                logger.error(e.getStackTrace());
                handler.handle(new Either.Left<>("error when creating excel"));
            }
        });
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
            excel.insertLabel(cellLabelColumn, this.operationsRowNumber,operation.getString("label"));
            this.operationsRowNumber++;
        }
        excel.insertHeader(this.operationsRowNumber, cellLabelColumn, excel.totalLabel);

    }

    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        Sql.getInstance().prepared(query, new JsonArray().add(instruction.getInteger("id")), SqlResult.validResultHandler(event -> {
            if (event.isLeft()) {
                handler.handle(event.left());
            } else {
                JsonArray programs = event.right().getValue();
                for (int i = 0; i < programs.size(); i++) {
                    JsonObject program = programs.getJsonObject(i);
                    program.put(actionStr, new JsonArray(program.getString(actionStr)));

                }

                handler.handle(new Either.Right<>(programs));
            }
        }));
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

        for (int i = 0; i < programs.size(); i++) {
            JsonObject program = programs.getJsonObject(i);

            JsonArray actions = program.getJsonArray(actionStr, new JsonArray());
            if (actions.isEmpty()) continue;
            excel.insertHeader(programRowNumber, cellColumn, program.getString("name"));
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
        //TODO faire un rework
//        excel.setTotal(cellColumn, operationsRowNumber, xTab, yTab);
        CellRangeAddress totalMerge = new CellRangeAddress(programRowNumber, programRowNumber + 2, cellColumn, cellColumn);
        sheet.addMergedRegion(totalMerge);
        excel.setRegionHeader(totalMerge, sheet);
        excel.insertHeader(programRowNumber, cellColumn, excel.totalLabel);

    }

    private int treatActions(JsonArray actions, String code, int posx, int programRowNumber) {
        for (int j = 0; j < actions.size(); j++) {
            JsonObject action = actions.getJsonObject(j);
            if (!code.equals(action.getString("code"))) {
                code = action.getString("code");
                if (!tabx.containsKey(action.getInteger("id_program").toString() + "-" + action.getString("code"))) {
                    tabx.put(action.getInteger("id_program").toString() + "-" + action.getString("code"), posx);
                    posx++;
                }

                excel.insertHeader(programRowNumber + 1, cellColumn, action.getString("name"));
                excel.insertHeader(programRowNumber + 2, cellColumn, action.getString("code"));
                this.cellColumn++;

            }

            for (int oIndex = 0; oIndex < operations.size(); oIndex++) {
                JsonObject operation = operations.getJsonObject(oIndex);
                if (action.getInteger("id_operation").equals(operation.getInteger("id"))) {
                    excel.insertCellTabDouble(posx, oIndex + yTab, action.getDouble("total"));
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
}
