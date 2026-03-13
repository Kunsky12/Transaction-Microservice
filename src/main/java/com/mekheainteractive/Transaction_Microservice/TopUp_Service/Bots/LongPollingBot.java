package com.mekheainteractive.Transaction_Microservice.TopUp_Service.Bots;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

public class TelegramPollingBot extends TelegramLongPollingBot {

    private final String botToken;
    private final String botUsername;

    public TelegramPollingBot(String botUsername, String botToken) {
        this.botUsername = botUsername;
        this.botToken = botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // Handle messages here
        if(update.hasMessage() && update.getMessage().hasText()){
            System.out.println("Received: " + update.getMessage().getText());
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}