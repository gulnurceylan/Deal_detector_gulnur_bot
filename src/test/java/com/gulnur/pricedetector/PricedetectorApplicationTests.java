package com.gulnur.pricedetector;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
class PricedetectorApplicationTests {

    @Autowired
    private PriceTrackerService priceTrackerService;

    @Autowired
    private PriceScraper priceScraper;

    @Test
    void contextLoads() {
    }

    @Test
    void testHMScrape() {
        String url = "https://www2.hm.com/tr_tr/productpage.1203028001.html";
        String price = priceScraper.scrapePrice(url);
        System.out.println("\n>>> H&M SCRAPE RESULT: " + price + "\n");
    }

    @Test
    void testHMWebservices() {
        try {
            String url = "https://www2.hm.com/hmwebservices/v1/tr_tr/products/1203028001";
            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36")
                .header("Accept", "application/json")
                .ignoreContentType(true)
                .timeout(10000)
                .get();
            System.out.println("\n>>> API RESPONSE: " + doc.text() + "\n");
        } catch (Exception e) {
            System.out.println("\n>>> API FAILED: " + e.getMessage() + "\n");
        }
    }

    @Test
    void testParsePrice() {
        // Turkish/EU thousands dot and decimals comma
        assertEquals(1000.0, priceTrackerService.parsePrice("1.000,00 TL"));
        assertEquals(1000.0, priceTrackerService.parsePrice("1.000 TL"));
        assertEquals(1250.5, priceTrackerService.parsePrice("1.250,5 TL"));
        
        // English formats
        assertEquals(1000.0, priceTrackerService.parsePrice("1,000.00"));
        assertEquals(1000.0, priceTrackerService.parsePrice("1,000"));
        
        // Decimals only
        assertEquals(12.34, priceTrackerService.parsePrice("12.34"));
        assertEquals(12.34, priceTrackerService.parsePrice("12,34"));
        
        // Currencies and spaces
        assertEquals(799.9, priceTrackerService.parsePrice(" 799,90 TL "));
        assertEquals(150.0, priceTrackerService.parsePrice("₺ 150"));
        assertEquals(25000.0, priceTrackerService.parsePrice("25.000,00 TL"));
        
        // Null and invalid cases
        assertNull(priceTrackerService.parsePrice(null));
        assertNull(priceTrackerService.parsePrice(""));
        assertNull(priceTrackerService.parsePrice("stokta yok"));
    }
}
