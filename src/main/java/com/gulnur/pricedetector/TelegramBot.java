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
@org.springframework.context.annotation.Lazy
private PriceTrackerService priceTrackerService;
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
                message.setText("Hi! Price Detector is ready! 🕵️‍♀️💖\nCommands:\n/track [url] - Start tracking a product\n/list - List your tracked products");
                gonder(message);
            } 
            // 🔗 2. İhtimal: Track komutu
            else if (messageText.startsWith("/track")) {
                String[] parts = messageText.split(" ");
                if (parts.length > 1) {
                    String url = parts[1];
                    message.setText("🔗 Tracking started for: " + url + " ⏳");
                    gonder(message);
                    priceTrackerService.addProduct(url, chatId);
                    message.setText("✅ Product added to tracking list!");
                    gonder(message);
                } else {
                    message.setText("Please provide a URL: /track [url]");
                    gonder(message);
                }
            }
            // 📝 3. İhtimal: Liste komutu
            else if (messageText.equalsIgnoreCase("/list")) {
                java.util.List<Product> products = priceTrackerService.getTrackedProducts(chatId);
                if (products.isEmpty()) {
                    message.setText("You are not tracking any products yet. 🛍️");
                } else {
                    StringBuilder sb = new StringBuilder("📋 Your tracked products:\n");
                    for (Product p : products) {
                        sb.append("- ").append(p.getUrl()).append(" (").append(p.getLastPrice()).append(")\n");
                    }
                    message.setText(sb.toString());
                }
                gonder(message);
            }
            // 🔗 4. İhtimal: Sadece link atıldı (Hızlı kontrol)
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
            // 🤷‍♀️ 5. İhtimal: Alakasız mesaj
            else {
                message.setText("Invalid! 🥺 commands: /track [url], /list");
                gonder(message);
            }
        }
    }

    // 🚀 Hata vermemesi için güvenli mesaj gönderme metodumuz!
    public void gonder(SendMessage msg) {
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}