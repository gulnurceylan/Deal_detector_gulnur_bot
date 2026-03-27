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
        String jsoupResult = scrapeWithJsoup(url);
        if (jsoupResult != null) return jsoupResult;

        WebDriver driver = null;
        try {
            io.github.bonigarcia.wdm.WebDriverManager.chromedriver().setup();

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless=new"); 
            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-blink-features=AutomationControlled");
            options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
            options.setExperimentalOption("useAutomationExtension", false);
            options.addArguments("user-agent=Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1");

            driver = new ChromeDriver(options);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(20));
            
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

            driver.get(url);
            Thread.sleep(5000); 

            dismissOverlays(driver);

            // 2. ADIM: Siteye Özel Hızlı Seçiciler (Amazon, H&M vb.)
            String specificPrice = extractWithSpecificSelectors(driver, url);
            if (specificPrice != null) return specificPrice;

            // 3. ADIM: Metadata (SEO/LD+JSON) Taraması
            String metaPrice = extractFromMetadata(driver);
            if (metaPrice != null) return metaPrice;

            // 4. ADIM: Görsel Heuristik Tarama
            String visualPrice = extractVisually(driver);
            if (visualPrice != null) return visualPrice;

            return "Maalesef geçerli fiyat bulunamadı 😢";

        } catch (Exception e) {
            return "Hata oluştu! 🚨 (" + e.getMessage() + ")";
        } finally {
            if (driver != null) driver.quit();
        }
    }

    private String scrapeWithJsoup(String url) {
        try {
            // H&M ve benzerleri için daha güçlü headerlar
            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .header("Accept-Language", "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .timeout(15000)
                .get();

            // 1. Meta Tagları Kontrol Et (Daha geniş kapsam)
            String[] metaProps = {
                "product:price:amount", "og:price:amount", "twitter:data1", "price",
                "priceCurrency", "schema:price"
            };
            for (String prop : metaProps) {
                String price = doc.select("meta[property='" + prop + "']").attr("content");
                if (price.isEmpty()) price = doc.select("meta[name='" + prop + "']").attr("content");
                if (!price.isEmpty()) {
                    System.out.println("[Debug] Jsoup found Meta: " + prop + " = " + price);
                    return price;
                }
            }

            // 2. LD+JSON Kontrol Et
            org.jsoup.select.Elements scripts = doc.select("script[type=application/ld+json]");
            for (org.jsoup.nodes.Element script : scripts) {
                String content = script.data();
                if (content.contains("\"price\"")) {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"price\"\\s*:\\s*\"?([\\d\\.\\,]+)\"?").matcher(content);
                    if (m.find()) return m.group(1).replace(",", ".");
                }
            }
            
            // 3. Trendyol'a özel window.__INITIAL_STATE__ taraması
            if (url.contains("trendyol.com")) {
                org.jsoup.select.Elements scriptTags = doc.select("script");
                for (org.jsoup.nodes.Element s : scriptTags) {
                    if (s.data().contains("window.__INITIAL_STATE__")) {
                        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"sellingPrice\"\\s*:\\s*([\\d\\.]+)").matcher(s.data());
                        if (m.find()) return m.group(1);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[Debug] Jsoup failed: " + e.getMessage());
        }
        return null;
    }

    private void dismissOverlays(WebDriver driver) {
        try {
            // Yaygın çerez ve pop-up butonlarını bulup tıkla veya gizle
            String cleanJs = 
                "var selectors = ['#onetrust-accept-btn-handler', '#cookie-accept', '.cookie-accept-btn', '.modal-close', '.overlay-close']; " +
                "selectors.forEach(s => { " +
                "  var el = document.querySelector(s); " +
                "  if (el) el.click(); " +
                "}); " +
                "// Modalları arka planda sakla " +
                "var overlays = document.querySelectorAll('.modal, .overlay, .cookie-banner'); " +
                "overlays.forEach(o => o.style.display = 'none');";
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(cleanJs);
            Thread.sleep(500);
        } catch (Exception ignored) {}
    }

    private String extractFromMetadata(WebDriver driver) {
        try {
            // LD+JSON Taraması
            List<WebElement> scripts = driver.findElements(By.xpath("//script[@type='application/ld+json']"));
            for (WebElement script : scripts) {
                String content = script.getAttribute("innerHTML");
                if (content.contains("\"price\"")) {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"price\"\\s*:\\s*\"?([\\d\\.\\,]+)\"?").matcher(content);
                    if (m.find()) return m.group(1).replace(",", ".");
                }
            }
            // Meta tag Taraması
            String[] metaProps = {"product:price:amount", "og:price:amount", "twitter:data1"};
            for (String prop : metaProps) {
                try {
                    WebElement meta = driver.findElement(By.xpath("//meta[@property='" + prop + "' or @name='" + prop + "']"));
                    return meta.getAttribute("content");
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String extractWithSpecificSelectors(WebDriver driver, String url) {
        try {
            if (url.contains("amazon.com.tr")) {
                // Amazon ana fiyat seçicisi (Whole + Fraction birleşimi)
                List<WebElement> priceWhole = driver.findElements(By.cssSelector(".a-price-whole"));
                List<WebElement> priceFraction = driver.findElements(By.cssSelector(".a-price-fraction"));
                if (!priceWhole.isEmpty()) {
                    String core = priceWhole.get(0).getText().replaceAll("[^\\d]", "");
                    String fraction = priceFraction.isEmpty() ? "00" : priceFraction.get(0).getText().replaceAll("[^\\d]", "");
                    return core + "," + fraction + " TL";
                }
            } else if (url.contains("hm.com")) {
                // H&M için doğrudan fiyat alanı
                List<WebElement> hmPrice = driver.findElements(By.cssSelector(".price-value, .primary-price, [data-test-id='product-price']"));
                if (!hmPrice.isEmpty()) return hmPrice.get(0).getText().trim();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String extractVisually(WebDriver driver) {
        try {
            List<WebElement> candidates = driver.findElements(By.xpath(
                "//*[(contains(text(), 'TL') or contains(text(), '₺') or contains(text(), 'TRY') or contains(text(), 'try')) and not(self::script) and not(self::style)]"
            ));

            WebElement bestElement = null;
            double maxScore = -1;

            for (WebElement el : candidates) {
                String text = "";
                try {
                    if (!el.isDisplayed()) continue;
                    text = el.getText().trim();
                } catch (Exception e) { continue; }

                if (!text.matches(".*\\d+.*")) continue;
                
                // --- GELİŞMİŞ PUANLAMA ---
                double score = 0;
                
                // 1. Font Size
                String fontSizeStr = el.getCssValue("font-size").replace("px", "");
                try {
                    score += Double.parseDouble(fontSizeStr) * 3; 
                } catch (Exception ignored) {}

                // 2. Font Weight
                String fontWeight = el.getCssValue("font-weight");
                if (fontWeight.matches("bold|[789]00")) score += 60;

                // 3. Class Name Check (En kritik iyileştirme: Reklamları ekarte eder)
                String className = el.getAttribute("class").toLowerCase();
                if (className.contains("price") || className.contains("amount") || className.contains("value")) {
                    score += 100; // Fiyat içeren sınıflara büyük öncelik
                }
                if (className.contains("ad") || className.contains("sponsored") || className.contains("banner")) {
                    score -= 200; // Reklamları cezalandır
                }

                // 4. Konum
                int y = el.getLocation().getY();
                if (y > 150 && y < 900) score += 40;

                if (score > maxScore) {
                    maxScore = score;
                    bestElement = el;
                }
            }
            if (bestElement != null) return bestElement.getText().trim();
        } catch (Exception ignored) {}
        return null;
    }
}