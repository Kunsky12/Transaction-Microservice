package com.mekheainteractive.Transaction_Microservice.TopUp_Service;

import com.mekheainteractive.Transaction_Microservice.TopUp_Service.Bots.TelegramPollingBot;
import com.mekheainteractive.Transaction_Microservice.TopUp_Service.Service.TopUpService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class TelegramConfig {

    private final String BOT_USERNAME = "your_bot_username";
    private final String BOT_TOKEN = "YOUR_BOT_TOKEN";

    @Bean
    public TelegramPollingBot telegramPollingBot(TopUpService topUpService) throws Exception {

        TelegramPollingBot bot = new TelegramPollingBot(BOT_USERNAME, BOT_TOKEN);

        // Register bot with Telegram API
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(bot);

        System.out.println("Telegram bot registered and running!");
        return bot;
    }
}