package fr.openent.lystore.export;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.poi.ss.usermodel.Workbook;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;


public class RecapEPLETab extends TabHelper {

    private int xProgramLabel = 6;
    private final int yProgramLabel = 0;
    private String programLabel = "Programme : ";
    private String totalLabel = "Montant : ";
    private String contractType = "Nature comptable : ";
    private String actionLabel = "Action : ";
    private JsonArray programs, orders;

    public RecapEPLETab(Workbook workbook, JsonObject instruction) {
        super(workbook, instruction, TabName.EPLE.toString());
    }

    @Override
    public void create(Handler<Either<String, Boolean>> handler) {
        excel.setDefaultFont();
        excel.setCPNumber(instruction.getString("cp_number"));
        getPrograms(event -> {
            if (event.isLeft()) {
                handler.handle(new Either.Left<>("Failed to retrieve programs"));
                return;
            }
            getOrders(event1 -> {
                if (event.isLeft()) {
                    handler.handle(new Either.Left<>("Failed to retrieve prices"));
                    return;
                }
                setLabelHead();
                handler.handle(new Either.Right<>(true));
            });
        });
    }

    public void setLabelHead() {
        JsonObject program;
        for (int i = 0; i < programs.size(); i++) {
            program = programs.getJsonObject(i);
            excel.insertLabelHead(sheet.createRow(xProgramLabel), yProgramLabel,
                    programLabel + program.getString("program_name") + " " + program.getString("program_label"));
            excel.insertLabelHead(sheet.createRow(xProgramLabel + 1), yProgramLabel,
                    actionLabel + program.getString("action_code") + " - " + program.getString("action_name"));
            excel.insertLabelHead(sheet.getRow(xProgramLabel), yProgramLabel + 1,
                    contractType + program.getString("contract_code") + " - " + program.getString("contract_name"));

            excel.insertLabelHead(sheet.getRow(xProgramLabel), yProgramLabel + 2, totalLabel +
                    setTotalHeader(program.getInteger("contract_type_id"), program.getInteger("action_id"), program.getInteger("program_id")));
            xProgramLabel += 4;
        }
    }

    private String setTotalHeader(Integer contract_type_id, Integer action_id, Integer program_id) {
        Float price, tax, total = 0.f;
        int amount;
        JsonObject order;
        for (int i = 0; i < orders.size(); i++) {
            order = orders.getJsonObject(i);
            if (order.getInteger("contract_type_id") == contract_type_id && order.getInteger("action_id") == action_id && order.getInteger("program_id") == program_id) {
                amount = order.getInteger("amount");
                tax = Float.parseFloat(order.getString("tax_amount"));
                price = Float.parseFloat(order.getString("price"));
                total += amount * (price + price * tax);
            }
        }
        return total.toString();
    }


    public void getOrders(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT distinct oce.id, oce.price,oce.amount, oce.tax_amount,contract_type.id as contract_type_id, program_action.id as action_id ,program.id as program_id " +
                "FROM lystore.order_client_equipment oce    " +
                " INNER JOIN lystore.operation ON (oce.id_operation = operation.id)   " +
                " INNER JOIN lystore.instruction ON (operation.id_instruction = instruction.id)  " +
                " INNER JOIN lystore.contract ON (oce.id_contract = contract.id)  " +
                " INNER JOIN lystore.contract_type ON (contract.id_contract_type = contract_type.id)   " +
                " INNER JOIN lystore.structure_program_action ON (structure_program_action.contract_type_id = contract_type.id)   " +
                " INNER JOIN lystore.program_action ON (structure_program_action.program_action_id = program_action.id)  " +
                " INNER JOIN lystore.program ON (program_action.id_program = program.id) " +
                " where instruction.id = ? ";


        Sql.getInstance().prepared(query, new JsonArray().add(instruction.getInteger("id")), SqlResult.validResultHandler(event -> {
            if (event.isLeft()) {
                handler.handle(event.left());
            } else {
                orders = event.right().getValue();
                handler.handle(new Either.Right<>(orders));
            }
        }));
    }

    @Override
    public void getPrograms(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT distinct contract_type.code as contract_code,  contract_type.name as contract_name, contract_type.id as contract_type_id, " +
                "program_action.action as action_code, program_action.description as action_name,program_action.id as action_id , " +
                "program.name as program_name,program.id as program_id, program.label as program_label " +
                "FROM lystore.order_client_equipment oce    " +
                "INNER JOIN lystore.operation ON (oce.id_operation = operation.id)   " +
                " INNER JOIN lystore.instruction ON (operation.id_instruction = instruction.id)  " +
                " INNER JOIN lystore.contract ON (oce.id_contract = contract.id)  " +
                " INNER JOIN lystore.contract_type ON (contract.id_contract_type = contract_type.id)   " +
                " INNER JOIN lystore.structure_program_action ON (structure_program_action.contract_type_id = contract_type.id)   " +
                " INNER JOIN lystore.program_action ON (structure_program_action.program_action_id = program_action.id)  " +
                " INNER JOIN lystore.program ON (program_action.id_program = program.id) " +
                " WHERE instruction.id = ?";


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
