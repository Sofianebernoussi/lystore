package fr.openent.lystore.service;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public interface ExportPDFService {
    /**
     * Generation of a PDF from a XHTML template
     * @param request Http request
     * @param templateProps  JSON object
     * @param templateName  template's name
     * @param prefixPdfName prefix the PDF's name (it will be completed by the date)
     * @param handler Function handler returning data
     */
    void generatePDF(final HttpServerRequest request, final JsonObject templateProps, final String templateName,
                     final String prefixPdfName, Handler<Buffer> handler );
}
