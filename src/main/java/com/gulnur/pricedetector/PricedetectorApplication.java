package com.gulnur.pricedetector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.ApplicationContext;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
@EnableScheduling
public class PricedetectorApplication {

    public static void main(String[] args) {
        // 1. Projeyi başlatıyoruz (Dependency Injection için)
        ApplicationContext context = SpringApplication.run(PricedetectorApplication.class, args);

        try {
            // 2. Telegram API'sini başlatıyoruz
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

            // 3. Spring Boot'un arka planda oluşturduğu botu alıp Telegram'a kaydediyoruz
            TelegramBot myBot = context.getBean(TelegramBot.class);
            botsApi.registerBot(myBot);

            System.out.println("\n🚀 Bot is active right now! 🚀\n");
            System.out.println("✅ Spring Boot ve Telegram bağlantısı başarıyla kuruldu.\n");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}