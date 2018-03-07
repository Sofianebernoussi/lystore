package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;

import org.vertx.java.core.json.JsonObject;
import java.util.List;
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

    void listExport(Integer idCampaign, String idStructure,Handler<Either<String, JsonArray>> handler);
    /**
     * Get the list of all orders
     * @param handler
     */
    void listOrder(Handler<Either<String, JsonArray>> handler);

    /**
     * Valid order ( change status to 'VALID', add validation number to the order,
     * then send mail to Agents )
     * @param request the request
     * @param user user informations
     * @param ids order's ids
     * @param url url to send in the mail
     * @param handler the Handler
     */
   void validateOrders(HttpServerRequest request, UserInfos user, List<Integer> ids, String url,
                       Handler<Either<String, JsonObject>> handler);
    /**
     * order to delete
     * @param idOrder id order
     * @param handler function handler returning idCampaign, price of Equipment
     */
    void orderForDelete(Integer idOrder, Handler<Either<String,JsonObject>> handler);

    /**
     * delete an order
     * @param idOrder id of the order item
     * @param order order to delete
     * @param idstructure id structure
     * @param handler function returning data
     */
    void deleteOrder( Integer idOrder, JsonObject order, String idstructure,
                      Handler<Either<String,JsonObject>> handler);
}
