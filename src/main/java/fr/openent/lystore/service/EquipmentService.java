package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

public interface EquipmentService {

    /**
     * List all equipments in database
     * @param handler function handler returning data
     */
    void listEquipments(Handler<Either<String, JsonArray>> handler);

    /**
     * List equipments of Campaign and a structure  in database
     * @param idCampaign campaign identifier
     * @param idStructure structure identifier
     * @param handler function handler returning data
     */
    void listEquipments( Integer idCampaign, String idStructure,
                        Handler<Either<String, JsonArray>> handler);

    /**
     * Get an Equipment informations
     * @param idEquipment equipment identifier
     * @param handler function handler returning data
     */
    void equipment(Integer idEquipment,  Handler<Either<String, JsonArray>> handler);

    /**
     * Create an equipment
     * @param equipment equipment to create
     * @param handler function handler returning data
     */
    void create(JsonObject equipment, Handler<Either<String, JsonObject>> handler);

    /**
     * Update an equipment
     * @param id equipment id to update
     * @param equipment equipment to update
     * @param handler function handler returning data
     */
    void update(Integer id, JsonObject equipment, Handler<Either<String, JsonObject>> handler);

    /**
     * Delete an equipment
     * @param ids equipment ids to delete
     * @param handler function handler returning data
     */
    void delete(List<Integer> ids, Handler<Either<String, JsonObject>> handler);

    /**
     * Update status for ids list
     * @param ids Ids list
     * @param status Status
     * @param handler Function handler returning data
     */
    void setStatus(List<Integer> ids, String status, Handler<Either<String, JsonObject>> handler);
}
