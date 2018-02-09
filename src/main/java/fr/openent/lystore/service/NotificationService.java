package fr.openent.lystore.service;

public interface NotificationService {

    /**
     * Send message to notification system
     * @param text Text to send
     * @param channel Channel used to send the message
     */
    void sendMessage(String text, String channel);
}
