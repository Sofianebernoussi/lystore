package fr.openent.lystore.service.impl;

import fr.openent.lystore.service.NotificationService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class SlackService implements NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SlackService.class);

    private Vertx vertx;
    private String apiUri;
    private String apiToken;
    private String botUsername;
    private String channel;
    private HttpClient httpClient;

    private static final Integer httpsPort = 443;
    private static final Integer httpPort = 80;

    public SlackService(Vertx vertx, String apiUri, String apiToken, String botUsername, String channel) {
        try {
            this.vertx = vertx;
            this.apiUri = apiUri;
            this.apiToken = apiToken;
            this.botUsername = botUsername;
            this.channel = channel;

            this.httpClient = generateHttpClient(new URI(apiUri));
        } catch (URISyntaxException e) {
            LOGGER.error("An error occurred when launching slack service", e);
        }
    }

    @Override
    public void sendMessage(String text) {
        String address = this.apiUri + "chat.postMessage?token=" + this.apiToken
                + "&channel=" + encodeParam(this.channel) + "&text=" + encodeParam(text)
                + "&username=" + encodeParam(this.botUsername)
                + "&pretty=1";
        final HttpClientRequest notification = httpClient.post(address,
                new Handler<HttpClientResponse>() {
            @Override
            public void handle(HttpClientResponse response) {
                if (response.statusCode() != 200) {
                    LOGGER.error("An error occurred when notify slack");
                }
            }
        }).putHeader("Content-Type", "application/json");

        notification.end();
    }

    private static String encodeParam(String param) {
        try {
            return URLEncoder.encode(param, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("An error occurred when encoding param", e);
            return "";
        }
    }

    private HttpClient generateHttpClient(URI uri) {
        return vertx.createHttpClient()
                .setHost(uri.getHost())
                .setPort("https".equals(uri.getScheme()) ? httpsPort : httpPort)
                .setVerifyHost(false)
                .setTrustAll(true)
                .setSSL("https".equals(uri.getScheme()))
                .setKeepAlive(true);
    }
}
