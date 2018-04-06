package fr.openent.lystore.service.impl;

import fr.openent.lystore.service.ExportPDFService;
import fr.wseduc.webutils.http.Renders;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.vertx.java.platform.Container;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import static fr.wseduc.webutils.http.Renders.badRequest;
import static fr.wseduc.webutils.http.Renders.getScheme;



public class DefaultExportPDFService  implements ExportPDFService {
    private static final Logger LOGGER = LoggerFactory.getLogger (DefaultOrderService.class);
    private String node;
    private Container container;
    private Vertx vertx;
    private EventBus eb;
    private Renders renders ;

    public DefaultExportPDFService(EventBus eb, Vertx vertx, Container container ) {
        super();
        this.eb = eb;
        this.container = container;
        this.vertx = vertx;
        this.renders = new Renders(this.vertx, this.container);
    }

    public void generatePDF(final HttpServerRequest request, final JsonObject templateProps, final String templateName,
                            final String prefixPdfName,  final Handler<Buffer> handler) {

        final String dateDebut = new SimpleDateFormat("dd.MM.yyyy").format(new Date().getTime());
        final String templatePath = container.config().getObject("exports").getString("template-path");
        final String baseUrl = getScheme(request) + "://" + Renders.getHost(request) +
                container.config().getString("app-address") + "/public/";

        node = (String) vertx.sharedData().getMap("server").get("node");
        if (node == null) {
            node = "";
        }
        vertx.fileSystem().readFile(templatePath + templateName, new Handler<AsyncResult<Buffer>>() {

            @Override
            public void handle(AsyncResult<Buffer> result) {
                if (!result.succeeded()) {
                    badRequest(request);
                    return;
                }
                StringReader reader = new StringReader(result.result().toString("UTF-8"));
                renders.processTemplate(request, templateProps, templateName, reader, new Handler<Writer>() {

                    @Override
                    public void handle(Writer writer) {
                        String processedTemplate = ((StringWriter) writer).getBuffer().toString();
                        if (processedTemplate == null) {
                            badRequest(request);
                            return;
                        }
                        JsonObject actionObject = new JsonObject();
                        byte[] bytes;
                        try {
                            bytes = processedTemplate.getBytes("UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            bytes = processedTemplate.getBytes();
                            LOGGER.error(e.getMessage(), e);
                        }

                        actionObject
                                .putBinary("content", bytes)
                                .putString("baseUrl", baseUrl);
                        eb.send(node + "entcore.pdf.generator", actionObject, new Handler<Message<JsonObject>>() {
                            @Override
                            public void handle(Message<JsonObject> reply) {
                                JsonObject pdfResponse = reply.body();
                                if (!"ok".equals(pdfResponse.getString("status"))) {
                                    badRequest(request, pdfResponse.getString("message"));
                                    return;
                                }
                                byte[] pdf = pdfResponse.getBinary("content");
                                Buffer either = new Buffer(pdf);
                                handler.handle(either);
                            }
                        });
                    }
                });

            }
        });

    }
}
