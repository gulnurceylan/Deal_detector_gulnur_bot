package com.gulnur.pricedetector;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PriceScraper {

    public String scrapePrice(String url) {
        System.out.println("🤖 Tarama başlatılıyor: " + url);

        // --- 1. ADIM: Jsoup ile Hızlı Tarama (Trendyol / Dolap / Zara-Meta) ---
        String result = tryJsoup(url);

        if (result != null) {
            System.out.println("✅ Jsoup ile fiyat bulundu.");
            return result;
        }

        // --- 2. ADIM: Jsoup Başarısızsa (Özellikle Amazon için) Selenium ---
        // Amazon Robot kontrolü yaptığı için Selenium (Chrome) şart.
        System.out.println("🏎️ Jsoup başarısız! Amazon/Inditex için Chrome Selenium devreye giriyor...");
        return trySelenium(url);
    }

    private String tryJsoup(String url) {
        try {
            Connection conn = Jsoup.connect(url)
                    .userAgent(
                            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
                    .header("Accept-Language", "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7")
                    .timeout(10000)
                    .ignoreHttpErrors(true);

            Document doc = conn.get();
            String html = doc.html();
            String title = doc.title();

            // --- Trendyol / Dolap ---
            if (url.contains("trendyol.com") || url.contains("ty.gl") || url.contains("dolap.com")) {
                Matcher m = Pattern.compile("(\"product_discounted_price\"|\"selling_price\"):\\s*([0-9.]+)")
                        .matcher(html);
                if (m.find())
                    return formatPrice(m.group(2), "TL");
            }

            // --- Zara / Bershka Meta Title ---
            Matcher titleMatch = Pattern.compile("([0-9]{1,3}(\\.[0-9]{3})*(,[0-9]{2})?)\\s*(TL|TRY|₺)").matcher(title);
            if (titleMatch.find())
                return titleMatch.group(0);

            // JSON-LD (Standard Fallback)
            Elements scripts = doc.select("script[type='application/ld+json']");
            for (Element s : scripts) {
                Matcher m = Pattern.compile("\"price\"\\s*:\\s*\"?([0-9.,]+)\"?").matcher(s.html());
                if (m.find()) {
                    String clean = m.group(1).replace(".", "").replace(",", ".");
                    return formatPrice(clean, "TL");
                }
            }

        } catch (Exception e) {
            System.err.println("Jsoup Hatası: " + e.getMessage());
        }
        return null;
    }

    private String trySelenium(String url) {
        WebDriver driver = null;
        try {
            io.github.bonigarcia.wdm.WebDriverManager.chromedriver().setup();
            ChromeOptions opts = new ChromeOptions();
            String chromePath = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
            if (new File(chromePath).exists()) {
                opts.setBinary(chromePath);
            }
            opts.addArguments("--headless=new", "--disable-gpu", "--no-sandbox");

            driver = new ChromeDriver(opts);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(20)); // Sayfa yükleme zaman aşımı
            driver.get(url);

            Thread.sleep(4000); // Sayfanın render olması için kısa bekleme

            // Amazon CSS Selektörleri
            String[] amazonSelectors = { ".a-price-whole", "#priceblock_ourprice", "#price_inside_buybox" };
            // Inditex (Zara/Bershka) Selektörleri
            String[] inditexSelectors = { ".price-current__amount", ".money-amount__main" };

            // Tüm selektörleri dene
            for (String s : amazonSelectors) {
                try {
                    WebElement el = driver.findElement(By.cssSelector(s));
                    if (el != null && !el.getText().isEmpty())
                        return el.getText() + " TL";
                } catch (Exception ignored) {
                }
            }

            for (String s : inditexSelectors) {
                try {
                    WebElement el = driver.findElement(By.cssSelector(s));
                    if (el != null && !el.getText().isEmpty())
                        return el.getText();
                } catch (Exception ignored) {
                }
            }

            // Son çare: Regex
            Matcher m = Pattern.compile("([0-9]{1,3}(\\.[0-9]{3})*(,[0-9]{2})?)\\s*(TL|TRY|₺)")
                    .matcher(driver.getPageSource());
            if (m.find())
                return m.group(0);

            return "Fiyat şimdilik gizli (Site engelliyor olabilir) 😢";
        } catch (Exception e) {
            return "Hata 🚨: " + e.getMessage();
        } finally {
            if (driver != null)
                driver.quit();
        }
    }

    private String formatPrice(String val, String currency) {
        try {
            double p = Double.parseDouble(val.replace(",", "."));
            if (p == (long) p)
                return String.format("%,d", (long) p).replace(",", ".") + " " + currency;
            return String.format("%,.2f", p).replace(".", "x").replace(",", ".").replace("x", ",") + " " + currency;
        } catch (Exception e) {
            return val + " " + currency;
        }
    }
}