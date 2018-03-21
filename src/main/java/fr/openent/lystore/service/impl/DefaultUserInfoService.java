package fr.openent.lystore.service.impl;


import fr.openent.lystore.service.UserInfoService;
import fr.wseduc.webutils.Either;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public class DefaultUserInfoService implements UserInfoService {


    private Neo4j neo4j;

    public DefaultUserInfoService(){
        this.neo4j = Neo4j.getInstance();
    }

    @Override
    public void getUserInfo(String idUser,Handler<Either<String, JsonArray>> handler) {
        String query = "Match (u:User{id:{idUser}}) return u limit 1";
        neo4j.execute(query, new JsonObject().putString("idUser",idUser), Neo4jResult.validResultHandler(handler));
    }
}
