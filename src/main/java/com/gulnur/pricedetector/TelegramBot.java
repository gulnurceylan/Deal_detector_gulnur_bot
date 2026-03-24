package com.gulnur.pricedetector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private PriceScraper priceScraper;

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

            // 🕵️‍♂️ 1. İhtimal: Selamlaşma
            if (messageText.equalsIgnoreCase("/start") || messageText.equalsIgnoreCase("Hello")) {
                message.setText("Hi! Price Detector is ready! 🕵️‍♀️💖 send me a link!");
                gonder(message);
            } 
            // 🔗 2. İhtimal: Link atıldı
            else if (messageText.startsWith("http")) {
                
                // İlk mesaj: Bilgilendirme
                message.setText("🔗 I got link! I am checking the price... ⏳");
                gonder(message);

                // Dedektif fiyatı çekiyor
                String priceResult = priceScraper.scrapePrice(messageText);

                // İkinci mesaj: Fiyat sonucu
                SendMessage priceMessage = new SendMessage();
                priceMessage.setChatId(String.valueOf(chatId));
                priceMessage.setText("💰 Current Price: " + priceResult);
                gonder(priceMessage);
            } 
            // 🤷‍♀️ 3. İhtimal: Alakasız mesaj
            else {
                message.setText("Invalid! 🥺 If you send me a product link that starts with 'http', I can check its price! 🛍️");
                gonder(message);
            }
        }
    }

    // 🚀 Hata vermemesi için güvenli mesaj gönderme metodumuz!
    private void gonder(SendMessage msg) {
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}