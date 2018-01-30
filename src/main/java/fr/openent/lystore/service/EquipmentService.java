package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Json;

import java.util.List;

public interface EquipmentService {

    /**
     * List all equipments in database
     * @param handler function handler returning data
     */
    public void listEquipments(Handler<Either<String, JsonArray>> handler);

    /**
     * List equipments of Campaign in database
     * @param handler function handler returning data
     */
    public void listEquipments(UserInfos user, Integer idCampaign,String idStructure, Handler<Either<String, JsonArray>> handler);

    /**
     * Create an equipment
     * @param equipment equipment to create
     * @param handler function handler returning data
     */
    public void create(JsonObject equipment, Handler<Either<String, JsonObject>> handler);

    /**
     * Update an equipment
     * @param id equipment id to update
     * @param equipment equipment to update
     * @param handler function handler returning data
     */
    public void update(Integer id, JsonObject equipment, Handler<Either<String, JsonObject>> handler);

    /**
     * Delete an equipment
     * @param ids equipment ids to delete
     * @param handler function handler returning data
     */
    public void delete(List<Integer> ids, Handler<Either<String, JsonObject>> handler);
}
