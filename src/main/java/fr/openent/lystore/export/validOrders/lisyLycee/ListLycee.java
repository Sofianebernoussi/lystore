package fr.openent.lystore.export.validOrders.lisyLycee;

import fr.openent.lystore.Lystore;
import fr.openent.lystore.export.TabHelper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.apache.poi.ss.usermodel.Workbook;

public class ListLycee extends TabHelper {
    private String numberValidation;
    public ListLycee(Workbook workbook, String numberValidation) {
        super(workbook,"Sommaire Commande");
        this.numberValidation = numberValidation;
    }

    @Override
    public void create(Handler<Either<String, Boolean>> handler) {
        excel.setDefaultFont();
        getDatas(event -> handleDatasDefault(event, handler));
    }


    @Override
    protected void initDatas(Handler<Either<String, Boolean>> handler) {

        logger.info("good");
//        ArrayList structuresId = new ArrayList<>();
//        for (int i = 0; i < datas.size(); i++) {
//            JsonObject data = datas.getJsonObject(i);
//            JsonArray actions = new JsonArray(data.getString("actions"));
//            for (int j = 0; j < actions.size(); j++) {
//                JsonObject action = actions.getJsonObject(j);
//                if(!structuresId.contains(action.getString("id_structure")))
//                    structuresId.add(structuresId.size(), action.getString("id_structure"));
//
//            }
//        }
//        getStructures(new JsonArray(structuresId), new Handler<Either<String, JsonArray>>() {
//            @Override
//            public void handle(Either<String, JsonArray> repStructures) {
//
//                boolean errorCatch= false;
//                if (repStructures.isRight()) {
//                    try {
//                        JsonArray structures = repStructures.right().getValue();
//                        setStructuresFromDatas(structures);
//                        if (datas.isEmpty()) {
//                            handler.handle(new Either.Left<>("No data in database"));
//                        } else {
//                            datas = sortByCity(datas);
//                            writeArray(handler);
//                        }
//                    }catch (Exception e){
//                        errorCatch = true;
//                    }
//                    if(errorCatch)
//                        handler.handle(new Either.Left<>("Error when writting files"));
//                    else
//                        handler.handle(new Either.Right<>(true));
//                } else {
//                    handler.handle(new Either.Left<>("Error when casting neo"));
//
//                }
//            }
//
//
//        });

    }

    private void writeArray(Handler<Either<String, Boolean>> handler) {

    }

    @Override
    public void getDatas(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT price, tax_amount, name, id_contract, " +
                "SUM(amount) as amount , id_structure  " +
                "FROM " + Lystore.lystoreSchema + ".order_client_equipment " +
                "WHERE number_validation = ? ";
        query += " GROUP BY equipment_key, price, tax_amount, name, id_contract  id_structure " +
                "UNION " +
                "SELECT opt.price, opt.tax_amount, opt.name, opt.id_contract, SUM(opt.amount) as amount " +
                ", equipment.id_structure " +
                "FROM (" +
                "SELECT options.price, options.tax_amount," +
                "options.name, equipment.id_contract," +
                "equipment.amount, options.id_order_client_equipment , equipment.id_structure "+
                "FROM " + Lystore.lystoreSchema + ".order_client_options options " +
                "INNER JOIN " + Lystore.lystoreSchema + ".order_client_equipment equipment " +
                "ON (options.id_order_client_equipment = equipment.id) " +
                "WHERE number_validation = ? "+
                ") as opt";
        query += " INNER JOIN lystore.order_client_equipment equipment ON (opt.id_order_client_equipment = equipment.id)" ;
        query += " GROUP BY opt.name, opt.price, opt.tax_amount, opt.id_contract , equipment.id_structure";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        params.add(this.numberValidation).add(this.numberValidation);
        sqlHandler(handler,params);
        }
}
