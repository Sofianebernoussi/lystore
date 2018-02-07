package fr.openent.lystore.service;

public interface NotificationService {

    /**
     * Send message to notification system
     * @param text Text to send
     * @param channel Channel used to send the message
     * @param username Username displayed in the channel
     * @param token Authentication token
     */
    void sendMessage(String text, String channel);
}
