package com.gulnur.pricedetector;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Override
    public String getBotUsername() {
        return "Deal_detector_gulnur_bot";
    }

    @Override
public String getBotToken() {
    return System.getProperty("telegram.bot.token") != null ? 
           System.getProperty("telegram.bot.token") : 
           "8616978715:AAE-rL5ksUewWUgIY1o3UhOsL5-_8440ieA"; 
}

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));

            if (messageText.equalsIgnoreCase("/start") || messageText.equalsIgnoreCase("Hello")) {
                message.setText("Hi! Price Detector is ready! 🕵️‍♀️💖");
            } else {
                message.setText("I got your message! System is processing: " + messageText);
            }

            try {
                execute(message); // Mesajı telefona gönder!
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
}