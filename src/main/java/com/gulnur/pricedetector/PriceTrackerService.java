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

    @Scheduled(fixedRate = 120000) // Every 2 minutes
    public void checkPrices() {
        List<Product> products = productRepository.findAll();
        for (Product product : products) {
            checkAndNotify(product);
        }
    }

    public void checkPricesForUser(Long chatId) {
        List<Product> products = productRepository.findByChatId(chatId);
        for (Product product : products) {
            checkAndNotify(product);
        }
    }

    private void checkAndNotify(Product product) {
        String newPriceStr = priceScraper.scrapePrice(product.getUrl());
        String oldPriceStr = product.getLastPrice();

        if (isPriceLower(oldPriceStr, newPriceStr)) {
            sendNotification(product, newPriceStr);
        }
        
        // Update the last price even if it went up or stayed same, 
        // to stay in sync with the site's current state.
        product.setLastPrice(newPriceStr);
        productRepository.save(product);
    }

    Double parsePrice(String priceStr) {
        if (priceStr == null || priceStr.isEmpty()) return null;
        try {
            // Remove currency symbols, spaces, and anything except digits, dots, and commas
            String cleaned = priceStr.replaceAll("[^\\d.,]", "").trim();
            if (cleaned.isEmpty()) return null;

            int numDots = 0;
            int numCommas = 0;
            for (char c : cleaned.toCharArray()) {
                if (c == '.') numDots++;
                if (c == ',') numCommas++;
            }

            if (numDots == 1 && numCommas == 1) {
                int lastDot = cleaned.lastIndexOf('.');
                int lastComma = cleaned.lastIndexOf(',');
                if (lastComma > lastDot) {
                    // 1.234,56 -> 1234.56
                    cleaned = cleaned.replace(".", "").replace(",", ".");
                } else {
                    // 1,234.56 -> 1234.56
                    cleaned = cleaned.replace(",", "");
                }
            } else if (numDots == 1 && numCommas == 0) {
                // E.g., "1.000" (Turkish thousands separator) or "12.34" (decimal separator)
                int dotIdx = cleaned.lastIndexOf('.');
                String fraction = cleaned.substring(dotIdx + 1);
                if (fraction.length() == 3) {
                    // Thousands separator, e.g. 1.250 -> 1250
                    cleaned = cleaned.replace(".", "");
                } else {
                    // Decimal separator
                    // cleaned = cleaned; // keep dot
                }
            } else if (numDots == 0 && numCommas == 1) {
                // E.g., "1,000" (thousands separator) or "12,34" (Turkish decimal comma)
                int commaIdx = cleaned.lastIndexOf(',');
                String fraction = cleaned.substring(commaIdx + 1);
                if (fraction.length() == 3) {
                    // Thousands separator
                    cleaned = cleaned.replace(",", "");
                } else {
                    // Decimal separator
                    cleaned = cleaned.replace(",", ".");
                }
            } else {
                // Multiple dots/commas or none
                int lastDot = cleaned.lastIndexOf('.');
                int lastComma = cleaned.lastIndexOf(',');
                if (lastComma > lastDot && lastComma != -1) {
                    cleaned = cleaned.replace(".", "").replace(",", ".");
                } else if (lastDot > lastComma && lastDot != -1) {
                    cleaned = cleaned.replace(",", "");
                } else {
                    cleaned = cleaned.replace(".", "").replace(",", "");
                }
            }

            return Double.parseDouble(cleaned);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isPriceLower(String oldPriceStr, String newPriceStr) {
        Double oldPrice = parsePrice(oldPriceStr);
        Double newPrice = parsePrice(newPriceStr);

        if (oldPrice == null || newPrice == null) return false;
        
        // Return true only if new price is strictly lower
        return newPrice < oldPrice;
    }

    private void sendNotification(Product product, String newPrice) {
        SendMessage message = new SendMessage();
        message.setChatId(product.getChatId().toString());
        
        StringBuilder sb = new StringBuilder();
        sb.append("🎉 **İNDİRİM YAKALANDI!** 🎉\n\n");
        sb.append("🔍 Ürün: ").append(product.getUrl()).append("\n");
        sb.append("📉 Eski Fiyat: `").append(product.getLastPrice()).append("`\n");
        sb.append("💰 Yeni Fiyat: `").append(newPrice).append("`\n\n");
        sb.append("Hemen kontrol et! 🚀");
        
        message.setText(sb.toString());
        message.setParseMode("Markdown");
        telegramBot.gonder(message);
    }

    public void simulatePriceDropNotification(String url, Long chatId) {
        // Create a fictional drop for testing
        String oldPrice = "1.000,00 TL";
        String newPrice = "799,90 TL";
        
        Product dummy = new Product();
        dummy.setUrl(url);
        dummy.setChatId(chatId);
        dummy.setLastPrice(oldPrice);
        
        sendNotification(dummy, newPrice);
    }
}
