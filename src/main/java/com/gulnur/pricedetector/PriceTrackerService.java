package com.gulnur.pricedetector;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;

@Service
public class PriceTrackerService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PriceScraper priceScraper;

    @Autowired
    private TelegramBot telegramBot;

    public void addProduct(String url, Long chatId) {
        Product product = new Product();
        product.setUrl(url);
        product.setChatId(chatId);
        // Initial scrape to get current price
        String currentPrice = priceScraper.scrapePrice(url);
        product.setLastPrice(currentPrice);
        productRepository.save(product);
    }

    public List<Product> getTrackedProducts(Long chatId) {
        return productRepository.findByChatId(chatId);
    }

    public void removeProduct(Long id) {
        productRepository.deleteById(id);
    }

    @Scheduled(fixedRate = 3600000) // Every 1 hour
    public void checkPrices() {
        List<Product> products = productRepository.findAll();
        for (Product product : products) {
            String newPrice = priceScraper.scrapePrice(product.getUrl());
            if (isPriceLower(product.getLastPrice(), newPrice)) {
                sendNotification(product, newPrice);
            }
            product.setLastPrice(newPrice);
            productRepository.save(product);
        }
    }

    private boolean isPriceLower(String oldPrice, String newPrice) {
        try {
            // Simple string comparison for now, can be improved to parse numbers
            // This is a placeholder logic
            if (oldPrice == null || newPrice == null) return false;
            return !oldPrice.equals(newPrice);
        } catch (Exception e) {
            return false;
        }
    }

    private void sendNotification(Product product, String newPrice) {
        SendMessage message = new SendMessage();
        message.setChatId(product.getChatId().toString());
        message.setText("🚨 Price Drop Alert!\nProduct: " + product.getUrl() + "\nOld Price: " + product.getLastPrice() + "\nNew Price: " + newPrice);
        telegramBot.gonder(message);
    }
}
