package fr.openent.lystore.service.impl;

import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.email.EmailSender;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.impl.LoggerFactory;

public class EmailSendService {

    private Neo4j neo4j;

    private static final org.vertx.java.core.logging.Logger LOGGER = LoggerFactory.getLogger (EmailSendService.class);
    private final EmailSender emailSender;
    public EmailSendService(EmailSender emailSender){
        this.emailSender = emailSender;
        this.neo4j = Neo4j.getInstance();
    }
    public void sendMail(HttpServerRequest request, String eMail, String object , String body,final JsonObject result,
                          final Handler<Either<String, JsonObject>> handler){

        emailSender.sendEmail(request,
                eMail,
                null,
                null,
                object,
                body,
                null,
                true,
                new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(Message<JsonObject> jsonObjectMessage) {
                        LOGGER.info(" emailSender.sendEmail : handle");
                        handler.handle(new Either.Right<String, JsonObject>(result));
                    }
                });
        LOGGER.info(" sendEmail classEmailSendService FIN");
    }

    public void sendMails(HttpServerRequest request, JsonObject result, JsonArray rows, UserInfos user, String url,
                          JsonArray structureRows, Handler<Either<String, JsonObject>> handler ){
        final int contractNameIndex = 1;
        final int agentEmailIndex = 3;
        LOGGER.info(" début sendMails");
        for (int i = 0; i < rows.size(); i++) {
            JsonArray row = rows.get(i);
            String mailObject="[LyStore] Commandes "+row.get(contractNameIndex);
            String mailBody = getAgentBodyMail(row, user, result.getString("number_validation"), url);
            sendMail(request, (String) row.get(agentEmailIndex),
                    mailObject,
                    mailBody,
                    result,
                    handler);
            LOGGER.info(" sendEmails : bouclefor1");
        }
        for (int i = 0; i < structureRows.size(); i++) {
            String mailObject="[LyStore] Commandes ";
            JsonObject row = structureRows.get(i);
            String name = row.getString("name");
            JsonArray mailsRow = row.getArray("mails");
            LOGGER.error(" sendEmails : bouclefor2");
            for(int j = 0 ; j < mailsRow.size() ; j++){
                String mailBody = getStructureBodyMail( (JsonObject) mailsRow.get(j), user,
                        result.getString("number_validation"), url, name);
                sendMail(request, ((JsonObject) mailsRow.get(j)).getString("mail"),
                        mailObject,
                        mailBody,
                        result,
                        handler);
                LOGGER.info(" sendEmails : bouclefor3");
            }
        }
        LOGGER.info(" sendEmails Fin");
    }

    public void getPersonnelMailStructure (JsonArray structureIds, Handler<Either<String, JsonArray>> handler){

            String query = "MATCH (w:WorkflowAction)--(r:Role) with r , count((r)-->(w)) as NbrRows " +
                    " Match p = ((r)<--(mg:ManualGroup)-->(s:Structure)), (mg)<-[IN]-(u:User)  " +
                    "where NbrRows=1 AND s.id IN {ids} return s.id as id, s.name as name, " +
                    "collect(DISTINCT {mail : u.emailAcademy, name: u.displayName} ) as mails ";
            neo4j.execute(query, new JsonObject().putArray("ids", structureIds),
                    Neo4jResult.validResultHandler(handler));

    }

    private static String getStructureBodyMail(JsonObject row, UserInfos user, String numberOrder, String url,
                                               String name){
        String body = "Bonjour " + row.getString("name") + ", <br/> <br/>"
                + user.getFirstName() + " " + user.getLastName() + " vient de valider une commande sous le numéro \""
                + numberOrder + "\"."
                + " Une partie de la commande concerne l'établissement " + name + ". "
                + "Cette confirmation est visible sur l'interface de LyStore en vous rendant ici :  <br />"
                + "<br />" + url + "#/ <br />"
                + "<br /> Bien Cordialement, "
                + "<br /> L'équipe LyStore. ";

        return formatAccentedString(body);

    }
    private static String getAgentBodyMail(JsonArray row, UserInfos user, String numberOrder, String url){
        final int contractName = 2 ;
        String body = "Bonjour " + row.get(contractName) + ", <br/> <br/>"
                + user.getFirstName() + " " + user.getLastName() + " vient de valider une commande sous le numéro \""
                + numberOrder + "\"."
                + " Une partie de la commande concerne le marche " + row.get(1) + ". "
                + "<br /> Pour générer le bon de commande et les CSF associés, il suffit de se rendre ici : <br />"
                + "<br />" + url + "#/order/client/waiting <br />"
                + "<br /> Bien Cordialement, "
                + "<br /> L'équipe LyStore. ";

        return formatAccentedString(body);
    }

    private static String formatAccentedString (String body){
        return  body.replace("&","&amp;").replace("€","&euro;")
                .replace("à","&agrave;").replace("â","&acirc;")
                .replace("é","&eacute;").replace("è","&egrave;")
                .replace("ê","&ecirc;").replace("î","&icirc;")
                .replace("ï","&iuml;") .replace("œ","&oelig;")
                .replace("ù","&ugrave;").replace("û","&ucirc;")
                .replace("ç","&ccedil;").replace("À","&Agrave;")
                .replace("Â","&Acirc;").replace("É","&Eacute;")
                .replace("È","&Egrave;").replace("Ê","&Ecirc;")
                .replace("Î","&Icirc;").replace("Ï","&Iuml;")
                .replace("Œ","&OElig;").replace("Ù","&Ugrave;")
                .replace("Û","&Ucirc;").replace("Ç","&Ccedil;");

    }
}
