package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;

/**
 * Created by agnes.lapeyronnie on 20/02/2018.
 */
public interface OrderService {
    /**
     * List orders of a campaign and a structure in data base
     * @param idCampaign campaign identifier
     * @param idStructure structure identifier
     * @param handler function handler returning data
     */
    void listOrder(Integer idCampaign, String idStructure, Handler<Either<String, JsonArray>> handler);


}
