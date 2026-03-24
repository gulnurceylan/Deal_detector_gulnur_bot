package com.gulnur.pricedetector;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class PriceScraper {

    public String scrapePrice(String url) {
        WebDriver driver = null;
        try {
            io.github.bonigarcia.wdm.WebDriverManager.chromedriver().setup();

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");
            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");

            driver = new ChromeDriver(options);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(10));

            driver.get(url);

            // 🛒 1. TRENDYOL İÇİN SADECE SENİN ESKİ YÖNTEMİN (DÜZ KOD ÇEKME)
            if (url.contains("trendyol.com")) {
                // Senin o eski takır takır çalışan düz etiket okuma kodun:
                WebElement trendyolPrice = driver.findElement(By.cssSelector(".prc-dsc"));
                return "🎯 Trendyol Fiyatı: " + trendyolPrice.getText().trim();
            }

            // 📦 2. AMAZON İÇİN DÜZ KOD ÇEKME
            if (url.contains("amazon.com")) {
                WebElement amazonPrice = driver.findElement(By.cssSelector(".a-price-whole"));
                return "🎯 Amazon Fiyatı: " + amazonPrice.getText().trim() + " TL";
            }

            // 👗 3. ZARA, MANGO, BERSHKA İÇİN SENİN YENİ AKILLI METODUN!
            Thread.sleep(2000); // Sayfa yüklensin diye 2 saniye bekle

            List<WebElement> elementsWithPrice = driver.findElements(By.xpath(
                "//*[contains(text(), 'TL') or contains(text(), '₺') or contains(text(), 'TRY') or contains(text(), 'try')]"
            ));

            for (WebElement el : elementsWithPrice) {
                try {
                    if (el.isDisplayed()) {
                        String text = el.getText().trim();

                        if (text.matches(".*\\d+.*")) { // Sadece rakam içeren mantıklı fiyatlar
                            int yPosition = el.getLocation().getY();

                            if (yPosition > 100 && yPosition < 1800) {
                                return "🎯 Fiyat: " + text;
                            }
                        }
                    }
                } catch (Exception e) {}
            }

            return "Maalesef bu site için geçerli bir fiyat bulunamadı 😢";

        } catch (Exception e) {
            e.printStackTrace();
            return "Fiyat çekilirken hata oluştu 🚨";
        } finally {
            if (driver != null) {
                driver.quit(); // Tarayıcıyı kapatıp RAM'i boşalt
            }
        }
    }
}