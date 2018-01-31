package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

public interface CampaignService {

    /**
     * List all campaigns in database
     * @param handler function handler returning data
     */
    void listCampaigns(Handler<Either<String, JsonArray>> handler);

    /**
     * List all campaigns of a structure
     * @param  idStructure of the structure
     * @param handler function handler returning data
     */
    void listCampaigns(String idStructure, Handler<Either<String, JsonArray>> handler);
    /**
     * Get a campaign information
     * @param id id campaign to get
     * @param handler function handler returning data
     */
    void getCampaign(Integer id, Handler<Either<String, JsonObject>> handler);

    /**
     * Create a campaign
     * @param campaign campaign to create
     * @param handler function handler returning data
     */
    void create(JsonObject campaign, Handler<Either<String, JsonObject>> handler);

    /**
     * Update a campaign
     * @param id campaign id to update
     * @param campaign campaign to update
     * @param handler function handler returning data
     */
    void update(Integer id, JsonObject campaign, Handler<Either<String, JsonObject>> handler);
    /**
     * Update an accessibility campaign
     * @param id campaign id to update
     * @param campaign campaign object
     * @param handler function handler returning data
     */
    void updateAccessibility(Integer id, JsonObject campaign, Handler<Either<String, JsonObject>> handler);
    /**
     * Delete an campaign
     * @param ids campaign ids to delete
     * @param handler function handler returning data
     */
    void delete(List<Integer> ids, Handler<Either<String, JsonObject>> handler);
}
