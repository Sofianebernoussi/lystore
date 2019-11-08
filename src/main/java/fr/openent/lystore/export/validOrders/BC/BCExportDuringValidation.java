package fr.openent.lystore.export.validOrders.BC;


import fr.openent.lystore.export.validOrders.PDF_OrderHElper;

import fr.wseduc.webutils.Either;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;



public class BCExportDuringValidation extends PDF_OrderHElper {
    private Logger log = LoggerFactory.getLogger(BCExport.class);
    public BCExportDuringValidation(EventBus eb, Vertx vertx, JsonObject config){
        super(eb,vertx,config);
    }





    public void create(JsonObject params, Handler<Either<String, Buffer>> exportHandler){
        log.info(params);
        final JsonArray ids = params.getJsonArray("ids");
        final String nbrBc = params.getString("nbrBc");
        final String nbrEngagement = params.getString("nbrEngagement");
        final String dateGeneration = params.getString("dateGeneration");
        Number supplierId = params.getInteger("supplierId");
        final Number programId = params.getInteger("programId");
        getOrdersData(exportHandler,nbrBc, nbrEngagement, dateGeneration, supplierId, ids,
                new Handler<JsonObject>() {
                    @Override
                    public void handle(JsonObject data) {
                        data.put("print_order", true);
                        data.put("print_certificates", false);

                        generatePDF( exportHandler,data,
                                "BC.xhtml", "Bon_Commande_",
                                new Handler<Buffer>() {
                                    @Override
                                    public void handle(final Buffer pdf) {
                                        exportHandler.handle(new Either.Right<>(pdf));
//                                                manageFileAndUpdateStatus(exportHandler, pdf, ids, nbrEngagement, programId, dateGeneration, nbrBc);
                                    }
                                }
                        );
                    }
                });
    }

//
//    private void manageFileAndUpdateStatus(final Handler<Either<String, Buffer>> exportHandler, final Buffer pdf,
//                                           final JsonArray ids, final String engagementNumber, final Number programId, final String dateCreation,
//                                           final String orderNumber) {
//        final String id = UUID.randomUUID().toString();
//        storage.writeBuffer(id, pdf, "application/pdf",
//                "BC_" + orderNumber, new Handler<JsonObject>() {
//                    @Override
//                    public void handle(final JsonObject file) {
//                        if ("ok".equals(file.getString("status"))) {
//                            UserUtils.getUserInfos(eb, request, new Handler<UserInfos>() {
//                                @Override
//                                public void handle(final UserInfos user) {
//                                    programService.getProgramById(programId, new Handler<Either<String, JsonObject>>() {
//                                        @Override
//                                        public void handle(Either<String, JsonObject> programEvent) {
//                                            if (programEvent.isRight()) {
//                                                JsonObject program = programEvent.right().getValue();
//                                                orderService.updateStatusToSent(ids.getList(), "SENT", engagementNumber, program.getString("name"),
//                                                        dateCreation, orderNumber, id, user.getUsername(), new Handler<Either<String, JsonObject>>() {
//                                                            @Override
//                                                            public void handle(Either<String, JsonObject> event) {
//                                                                if (event.isRight()) {
//                                                                    request.response().end(pdf);
//                                                                    logSendingOrder(ids, request);
//                                                                } else {
//                                                                    badRequest(request);
//                                                                }
//                                                            }
//                                                        });
//                                            } else {
//                                                renderError(request);
//                                            }
//                                        }
//                                    });
//                                }
//                            });
//                        } else {
//                            log.error("An error occurred when inserting pdf in mongo");
//                            badRequest(request);
//                        }
//                    }
//                });
//    }

}
