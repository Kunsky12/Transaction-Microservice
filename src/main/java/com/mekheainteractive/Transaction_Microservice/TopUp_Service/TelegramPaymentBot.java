package com.mekheainteractive.Transaction_Microservice.TopUp_Service;

import com.mekheainteractive.Transaction_Microservice.TopUp_Service.Service.TopUpService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.regex.*;

public class TelegramPaymentBot extends TelegramLongPollingBot {

    private final TopUpService topUpService;

    public TelegramPaymentBot(TopUpService topUpService){
        this.topUpService = topUpService;
    }

    @Override
    public void onUpdateReceived(Update update) {

        if(!update.hasMessage()) return;
        if(!update.getMessage().hasText()) return;

        String text = update.getMessage().getText();

        System.out.println("Telegram message: " + text);

        parsePayment(text);
    }

    private void parsePayment(String text){

        Pattern amountPattern = Pattern.compile("\\$([0-9.]+)");
        Pattern codePattern = Pattern.compile("(Remark|Purpose):\\s*(KK\\d+)");

        Matcher amountMatch = amountPattern.matcher(text);
        Matcher codeMatch = codePattern.matcher(text);

        if(amountMatch.find() && codeMatch.find()){

            double amount = Double.parseDouble(amountMatch.group(1));
            String code = codeMatch.group(2);

            topUpService.verifyPayment(code, amount);
        }
    }

    @Override
    public String getBotUsername(){
        return "kunkhmer_payment_bot";
    }

    @Override
    public String getBotToken(){
        return "YOUR_BOT_TOKEN";
    }
}