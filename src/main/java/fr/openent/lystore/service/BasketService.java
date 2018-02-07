package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public interface BasketService {
    /**
     * Create a basket item
     * @param basket basket item to create
     * @param handler function handler returning data
     */
     void create(JsonObject basket, Handler<Either<String, JsonObject>> handler);

    /**
     * List  basket liste of a campaigne and a structure
     * @param idCampaign
     * @param idStructure
     * @param handler
     */
     void listBasket(Integer idCampaign, String idStructure, Handler<Either<String, JsonArray>> handler);

    /**
     * Delete a basket item
     * @param idBasket id of the basket item
     * @param handler
     */
     void delete(Integer idBasket, Handler<Either<String, JsonObject>> handler);

    /**
     * Update a basket's amount
     * @param idBasket id of a basket item
     * @param amount the new amount
     * @param handler
     */
     void updateAmount(Integer idBasket, Integer amount,Handler<Either<String, JsonObject>> handler );
}
