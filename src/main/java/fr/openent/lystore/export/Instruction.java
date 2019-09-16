package fr.openent.lystore.export;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.equipmentRapp.*;
import fr.openent.lystore.export.investissement.FonctionnementTab;
import fr.openent.lystore.export.investissement.LyceeTab;
import fr.openent.lystore.export.investissement.RecapEPLETab;
import fr.openent.lystore.export.investissement.RecapImputationBud;
import fr.openent.lystore.export.notificationEquipCP.LinesBudget;
import fr.openent.lystore.export.notificationEquipCP.NotificationLycTab;
import fr.openent.lystore.export.notificationEquipCP.RecapMarketGestion;
import fr.openent.lystore.export.publipostage.Publipostage;
import fr.openent.lystore.export.subventionEquipment.Market;
import fr.openent.lystore.export.subventionEquipment.Subventions;
import fr.openent.lystore.helpers.ExcelHelper;
import fr.openent.lystore.service.ExportService;
import fr.openent.lystore.service.impl.DefaultProjectService;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.data.FileResolver;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.buffer.impl.BufferImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Instruction {
    private final String operationsId = "WITH operations AS (" +
            "SELECT operation.id, label_operation.label, operation.id_instruction " +
            "FROM " + Lystore.lystoreSchema + ".operation " +
            "INNER JOIN " + Lystore.lystoreSchema + ".label_operation ON (operation.id_label = label_operation.id) " +
            "WHERE id_instruction = ? " +
            ")" +
            "SELECT instruction.*, array_to_json(array_agg(operations)) as operations " +
            "FROM " + Lystore.lystoreSchema + ".instruction " +
            "INNER JOIN operations ON (operations.id_instruction = instruction.id) " +
            "WHERE instruction.id = ? " +
            "GROUP BY instruction.id";
    private Integer id;
    private Number idFile;
    private ExportService exportService;
    private Logger log = LoggerFactory.getLogger(DefaultProjectService.class);

    public Instruction(ExportService exportService, Number idFile, Integer instructionId) {
        this.idFile = idFile;
        this.exportService = exportService;
        this.id = instructionId;
    }

    public void exportInvestissement(Handler<Either<String, Buffer>> handler) {
        if (this.id == null) {
            ExcelHelper.catchError(exportService, idFile, "Instruction identifier is not nullable");
            handler.handle(new Either.Left<>("Instruction identifier is not nullable"));
        }


        Sql.getInstance().prepared(operationsId, new JsonArray().add(this.id).add(this.id), SqlResult.validUniqueResultHandler(either -> {
            if (either.isLeft()) {
                ExcelHelper.catchError(exportService, idFile, "Error when getting sql datas ");
                handler.handle(new Either.Left<>("Error when getting sql datas "));
            } else {

                JsonObject instruction = either.right().getValue();
                String operationStr = "operations";
                if (!instruction.containsKey(operationStr)) {
                    ExcelHelper.catchError(exportService, idFile, "Error when getting operations");
                    handler.handle(new Either.Left<>("Error when getting operations"));
                } else {
                    instruction.put(operationStr, new JsonArray(instruction.getString(operationStr)));
                    String path = FileResolver.absolutePath("public/template/excel/templateInvestissement.xlsx");

                    try {
                        FileInputStream templateInputStream = new FileInputStream(path);
                        Workbook workbook = new XSSFWorkbook(templateInputStream);
                        List<Future> futures = new ArrayList<>();
                        Future<Boolean> lyceeFuture = Future.future();
                        Future<Boolean> CMRFuture = Future.future();
                        Future<Boolean> CMDfuture = Future.future();
                        Future<Boolean> Fonctionnementfuture = Future.future();
                        Future<Boolean> RecapEPLEfuture = Future.future();
                        Future<Boolean> RecapImputationBudfuture = Future.future();
//                        futures.add(lyceeFuture);
//                        futures.add(CMRFuture);
//                        futures.add(CMDfuture);
//                        futures.add(Fonctionnementfuture);
//                        futures.add(RecapEPLEfuture);
                        futures.add(RecapImputationBudfuture);
//
                        futureHandler(handler, workbook, futures);

//                        new LyceeTab(workbook, instruction).create(getHandler(lyceeFuture));
//                        new CMRTab(workbook, instruction).create(getHandler(CMRFuture));
//                        new CMDTab(workbook, instruction).create(getHandler(CMDfuture));
//                        new FonctionnementTab(workbook, instruction).create(getHandler(Fonctionnementfuture));
//                        new RecapEPLETab(workbook, instruction).create(getHandler(RecapEPLEfuture));
                        new RecapImputationBud(workbook, instruction).create(getHandler(RecapImputationBudfuture));
                    } catch (IOException e) {
                        ExcelHelper.catchError(exportService, idFile, "Xlsx Failed to read template");
                        handler.handle(new Either.Left<>("Xlsx Failed to read template"));
                    }
                }
            }
        }));


    }

    public void exportEquipmentRapp(Handler<Either<String, Buffer>> handler, String type) {
        if (this.id == null) {
            ExcelHelper.catchError(exportService, idFile, "Instruction identifier is not nullable");
            handler.handle(new Either.Left<>("Instruction identifier is not nullable"));
        }


        Sql.getInstance().prepared(operationsId, new JsonArray().add(this.id).add(this.id), SqlResult.validUniqueResultHandler(either -> {
            if (either.isLeft()) {
                ExcelHelper.catchError(exportService, idFile, "Error when getting sql datas ");
                handler.handle(new Either.Left<>("Error when getting sql datas "));
            } else {

                JsonObject instruction = either.right().getValue();
                String operationStr = "operations";
                if (!instruction.containsKey(operationStr)) {
                    ExcelHelper.catchError(exportService, idFile, "Error when getting operations");
                    handler.handle(new Either.Left<>("Error when getting operations"));
                } else {
                    instruction.put(operationStr, new JsonArray(instruction.getString(operationStr)));

                    Workbook workbook = new XSSFWorkbook();
                    List<Future> futures = new ArrayList<>();
                    Future<Boolean> ListForTextFuture = Future.future();
                    Future<Boolean> RecapFuture = Future.future();
                    Future<Boolean> ComptaFuture = Future.future();
                    Future<Boolean> AnnexeDelibFuture = Future.future();
                    Future<Boolean> RecapMarketFuture = Future.future();
                    Future<Boolean> VerifBudgetFuture = Future.future();
                    futures.add(ListForTextFuture);
                    futures.add(RecapFuture);
                    futures.add(ComptaFuture);
                    futures.add(AnnexeDelibFuture);
                    futures.add(RecapMarketFuture);
                    futures.add(VerifBudgetFuture);

                    futureHandler(handler, workbook, futures);

                    new ComptaTab(workbook, instruction, type).create(getHandler(ComptaFuture));
                    new ListForTextTab(workbook, instruction, type).create(getHandler(ListForTextFuture));
                    new RecapTab(workbook, instruction, type).create(getHandler(RecapFuture));
                    new AnnexeDelibTab(workbook, instruction, type).create(getHandler(AnnexeDelibFuture));
                    new RecapMarket(workbook, instruction, type).create(getHandler(RecapMarketFuture));
                    new VerifBudgetTab(workbook, instruction, type).create(getHandler(VerifBudgetFuture));
                }
            }
        }));


    }

    public void exportSubvention(Handler<Either<String, Buffer>> handler) {
        if (this.id == null) {
            log.error("Instruction identifier is not nullable");
            handler.handle(new Either.Left<>("Instruction identifier is not nullable"));
        }
        Sql.getInstance().prepared(operationsId, new JsonArray().add(this.id).add(this.id), SqlResult.validUniqueResultHandler(eitherInstruction -> {
            if (eitherInstruction.isLeft()) {
                log.error("Error when getting sql datas for subvention");
                handler.handle(new Either.Left<>("Error when getting sql datas for subvention"));
            } else {

               JsonObject instruction = eitherInstruction.right().getValue();
                String operationStr = "operations";
                if (!instruction.containsKey(operationStr)) {
                    log.error("Error when getting operations");
                    handler.handle(new Either.Left<>("Error when getting operations"));
                } else {
                    instruction.put(operationStr, new JsonArray(instruction.getString(operationStr)));

                    Workbook workbook = new XSSFWorkbook();
                    List<Future> futures = new ArrayList<>();
                    Future<Boolean> CmrSubventions = Future.future();
                    Future<Boolean> PublicsSubventionsFuture = Future.future();
                    Future<Boolean> CmrMarchés = Future.future();
                    Future<Boolean> PublicsMarchésFuture = Future.future();

                    futures.add(CmrSubventions);
                    futures.add(PublicsSubventionsFuture);
                    futures.add(CmrMarchés);
                    futures.add(PublicsMarchésFuture);

                    futureHandler(handler, workbook, futures);

                    new Subventions(workbook, instruction, true).create(getHandler(CmrSubventions));
                    new Subventions(workbook, instruction, false).create(getHandler(PublicsSubventionsFuture));
                    new Market(workbook, instruction, true).create(getHandler(CmrMarchés));
                    new Market(workbook, instruction, false).create(getHandler(PublicsMarchésFuture));
                }
            }
        }));


    }

    public void exportPublipostage(Handler<Either<String, Buffer>> handler) {
        if (this.id == null) {
            ExcelHelper.catchError(exportService, idFile, "Instruction identifier is not nullable");
            handler.handle(new Either.Left<>("Instruction identifier is not nullable"));
        }
        Sql.getInstance().prepared(operationsId, new JsonArray().add(this.id).add(this.id), SqlResult.validUniqueResultHandler(eitherInstruction -> {
            if (eitherInstruction.isLeft()) {
                ExcelHelper.catchError(exportService, idFile, "Error when getting sql datas ");
                handler.handle(new Either.Left<>("Error when getting sql datas "));
            } else {
                JsonObject instruction = eitherInstruction.right().getValue();
                String operationStr = "operations";
                if (!instruction.containsKey(operationStr)) {
                    ExcelHelper.catchError(exportService, idFile, "Error when getting operations");
                    handler.handle(new Either.Left<>("Error when getting operations"));
                } else {
                    instruction.put(operationStr, new JsonArray(instruction.getString(operationStr)));

                    Workbook workbook = new XSSFWorkbook();
                    List<Future> futures = new ArrayList<>();
                    Future<Boolean> PublipostageFuture = Future.future();

                    futures.add(PublipostageFuture);

                    futureHandler(handler, workbook, futures);

                    new Publipostage(workbook, instruction).create(getHandler(PublipostageFuture));
                }
            }
        }));
    }

    public void exportNotficationCp(Handler<Either<String, Buffer>> handler) {
        if (this.id == null) {
            ExcelHelper.catchError(exportService, idFile, "Instruction identifier is not nullable");
            handler.handle(new Either.Left<>("Instruction identifier is not nullable"));
        }


        Sql.getInstance().prepared(operationsId, new JsonArray().add(this.id).add(this.id), SqlResult.validUniqueResultHandler(either -> {
            if (either.isLeft()) {
                ExcelHelper.catchError(exportService, idFile, "Error when getting sql datas ");
                handler.handle(new Either.Left<>("Error when getting sql datas "));
            } else {

                JsonObject instruction = either.right().getValue();
                String operationStr = "operations";
                if (!instruction.containsKey(operationStr)) {
                    ExcelHelper.catchError(exportService, idFile, "Error when getting operations");
                    handler.handle(new Either.Left<>("Error when getting operations"));
                } else {
                    instruction.put(operationStr, new JsonArray(instruction.getString(operationStr)));

                    Workbook workbook = new XSSFWorkbook();
                    List<Future> futures = new ArrayList<>();
                    Future<Boolean> LinesBudgetFuture = Future.future();
                    Future<Boolean> RecapMarketGestionFuture = Future.future();
                    Future<Boolean> NotifcationLyceeFuture = Future.future();

                    futures.add(LinesBudgetFuture);
                    futures.add(RecapMarketGestionFuture);
                    futures.add(NotifcationLyceeFuture);

                    futureHandler(handler, workbook, futures);
                    new NotificationLycTab(workbook, instruction).create(getHandler(NotifcationLyceeFuture));
                    new RecapMarketGestion(workbook, instruction).create(getHandler(RecapMarketGestionFuture));
                    new LinesBudget(workbook, instruction).create(getHandler(LinesBudgetFuture));

                }
            }
        }));
    }

    private void futureHandler(Handler<Either<String, Buffer>> handler, Workbook workbook, List<Future> futures) {
        CompositeFuture.all(futures).setHandler(event -> {
            if (event.succeeded()) {
                try {
                    ByteArrayOutputStream fileOut = new ByteArrayOutputStream();
                    workbook.write(fileOut);
                    Buffer buff = new BufferImpl();
                    buff.appendBytes(fileOut.toByteArray());
                    handler.handle(new Either.Right<>(buff));
                } catch (IOException e) {
                    ExcelHelper.catchError(exportService, idFile, e.getMessage());
                    handler.handle(new Either.Left<>(e.getMessage()));
                }
            } else {
                ExcelHelper.catchError(exportService, idFile, "Error when resolving futures");
                handler.handle(new Either.Left<>("Error when resolving futures"));
            }
        });
    }

    private Handler<Either<String, Boolean>> getHandler(Future<Boolean> future) {
        return event -> {
            if (event.isRight()) {
                future.complete(event.right().getValue());
            } else {
                future.fail(event.left().getValue());
            }
        };
    }
}
