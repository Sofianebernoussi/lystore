package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

public interface TagService {

    /**
     * List all tags in database
     * @param handler function handler returning data
     */
    public void getAll(Handler<Either<String, JsonArray>> handler);

    /**
     * Create a tag
     * @param tag tag to create
     * @param handler function handler retuning data
     */
    public void create(JsonObject tag, Handler<Either<String, JsonObject>> handler);

    /**
     * Update a tag
     * @param id tag id to update
     * @param tag tag data to update
     * @param handler function handler returning data
     */
    public void update(Integer id, JsonObject tag, Handler<Either<String, JsonObject>> handler);

    /**
     * Delete a tag
     * @param ids tags id to delete
     * @param handler function handler returning data
     */
    public void delete(List<Integer> ids, Handler<Either<String, JsonObject>> handler);
}
