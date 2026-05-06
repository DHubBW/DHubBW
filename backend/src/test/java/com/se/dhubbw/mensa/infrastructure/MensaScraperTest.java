package com.se.dhubbw.mensa.infrastructure;

import com.se.dhubbw.mensa.infrastructure.util.Ernaehrung;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MensaScraperTest {

    private static final String MOCK_HTML_WEEK_1 =
        "<html><body>"
        + "<ul class=\"canteen-day-nav\">"
        + "<li><a rel=\"2026-05-06\">Mi</a></li>"
        + "<li><a rel=\"2026-05-07\">Do</a></li>"
        + "</ul>"
        + "<div id=\"canteen_day_1\" class=\"canteen-day\"><table>"
        + "<tr class=\"mt-2\">"
        + "<td class=\"mtd-icon\">"
        + "<img src=\"/layout/icons/rindfleisch.svg\"></td>"
        + "<td class=\"first menu-title\">"
        + "<span><b>Chili con Carne</b></span><sup>[We,So]</sup>"
        + "</td>"
        + "<td><span class=\"bgp price_1\">3,60 &euro;</span></td>"
        + "</tr>"
        + "<tr class=\"mt-6\">"
        + "<td class=\"mtd-icon\">"
        + "<img src=\"/layout/icons/veganes-gericht.svg\"></td>"
        + "<td class=\"first menu-title\">"
        + "<span><b>Chili con Soja</b></span><sup>[So]</sup>"
        + "</td>"
        + "<td><span class=\"bgp price_1\">3,60 &euro;</span></td>"
        + "</tr>"
        + "<tr class=\"mt-6\">"
        + "<td class=\"mtd-icon\">"
        + "<img src=\"/layout/icons/vegetarisches-gericht.svg\"></td>"
        + "<td class=\"first menu-title\">"
        + "<span><b>Pasta Arrabiata</b></span><sup>[Ei,We]</sup>"
        + "</td>"
        + "<td><span class=\"bgp price_1\">2,50 &euro;</span></td>"
        + "</tr>"
        + "</table></div>"
        + "<div id=\"canteen_day_2\" class=\"canteen-day\"></div>"
        + "</body></html>";

    private static final String MOCK_HTML_WEEK_2 =
        "<html><body>"
        + "<ul class=\"canteen-day-nav\">"
        + "<li><a rel=\"2026-05-13\">Mi</a></li>"
        + "<li><a rel=\"2026-05-14\">Do</a></li>"
        + "</ul>"
        + "<div id=\"canteen_day_1\" class=\"canteen-day\"><table>"
        + "<tr class=\"mt-2\">"
        + "<td class=\"mtd-icon\">"
        + "<img src=\"/layout/icons/schweinefleisch.svg\"></td>"
        + "<td class=\"first menu-title\">"
        + "<span><b>Schnitzel mit Pommes</b></span><sup>[We,Ei]</sup>"
        + "</td>"
        + "<td><span class=\"bgp price_1\">4,10 &euro;</span></td>"
        + "</tr>"
        + "<tr class=\"mt-6\">"
        + "<td class=\"mtd-icon\">"
        + "<img src=\"/layout/icons/veganes-gericht.svg\"></td>"
        + "<td class=\"first menu-title\">"
        + "<span><b>Linsensuppe</b></span><sup>[Se]</sup>"
        + "</td>"
        + "<td><span class=\"bgp price_1\">2,80 &euro;</span></td>"
        + "</tr>"
        + "</table></div>"
        + "<div id=\"canteen_day_2\" class=\"canteen-day\"></div>"
        + "</body></html>";

    private MensaScraper scraper;

    @BeforeEach
    public void setUp() {
        scraper = new MensaScraper("http://localhost");
    }

    @Test
    public void givenMockHtml_whenParsed_thenReturnsThreeMeals() {
        Document doc = Jsoup.parse(MOCK_HTML_WEEK_1);
        List<Mensa> result = scraper.parseDocument(doc);
        Assertions.assertEquals(3, result.size());
    }

    @Test
    public void givenMockHtml_whenParsed_thenFleschMealHasCorrectFields() {
        Document doc = Jsoup.parse(MOCK_HTML_WEEK_1);
        Mensa fleisch = scraper.parseDocument(doc).get(0);

        Assertions.assertEquals("Chili con Carne", fleisch.getEssensbezeichnung());
        Assertions.assertEquals("[We,So]", fleisch.getAllergene());
        Assertions.assertEquals(3.60, fleisch.getPreis(), 0.001);
        Assertions.assertEquals(Ernaehrung.FLEISCH, fleisch.getErnaehrung());
    }

    @Test
    public void givenMockHtml_whenParsed_thenVeganMealHasCorrectErnaehrung() {
        Document doc = Jsoup.parse(MOCK_HTML_WEEK_1);
        Mensa vegan = scraper.parseDocument(doc).get(1);

        Assertions.assertEquals(Ernaehrung.VEGAN, vegan.getErnaehrung());
        Assertions.assertEquals("Chili con Soja", vegan.getEssensbezeichnung());
    }

    @Test
    public void givenMockHtml_whenParsed_thenVegetarianMealHasCorrectErnaehrung() {
        Document doc = Jsoup.parse(MOCK_HTML_WEEK_1);
        Mensa vegetarisch = scraper.parseDocument(doc).get(2);

        Assertions.assertEquals(Ernaehrung.VEGETARISCH, vegetarisch.getErnaehrung());
    }

    @Test
    public void givenMockHtml_whenParsed_thenMealsHaveCorrectDate() {
        Document doc = Jsoup.parse(MOCK_HTML_WEEK_1);
        List<Mensa> result = scraper.parseDocument(doc);

        result.forEach(m -> Assertions.assertNotNull(m.getDatum()));
        Assertions.assertEquals(
            LocalDate.of(2026, 5, 6),
            result.get(0).getDatum()
        );
    }

    @Test
    public void givenTwoWeekHtml_whenBothParsed_thenCombinedMealsFromBothWeeks() {
        List<Mensa> week1 = scraper.parseDocument(Jsoup.parse(MOCK_HTML_WEEK_1));
        List<Mensa> week2 = scraper.parseDocument(Jsoup.parse(MOCK_HTML_WEEK_2));

        List<Mensa> combined = new ArrayList<>();
        combined.addAll(week1);
        combined.addAll(week2);

        Assertions.assertEquals(3, week1.size());
        Assertions.assertEquals(2, week2.size());
        Assertions.assertEquals(5, combined.size());

        Assertions.assertEquals(LocalDate.of(2026, 5, 6), week1.get(0).getDatum());
        Assertions.assertEquals(LocalDate.of(2026, 5, 13), week2.get(0).getDatum());
    }

    @Test
    public void givenRowWithNoIcon_whenParsed_thenErnaehrungIsFleisch() {
        String html = "<html><body>"
            + "<ul class=\"canteen-day-nav\">"
            + "<li><a rel=\"2026-05-06\">Mi</a></li>"
            + "</ul>"
            + "<div id=\"canteen_day_1\" class=\"canteen-day\"><table>"
            + "<tr class=\"mt-2\">"
            + "<td class=\"mtd-icon\"></td>"
            + "<td class=\"first menu-title\">"
            + "<span><b>Schnitzel</b></span><sup>[We]</sup>"
            + "</td>"
            + "<td><span class=\"bgp price_1\">3,80 &euro;</span></td>"
            + "</tr>"
            + "</table></div>"
            + "</body></html>";
        Document doc = Jsoup.parse(html);
        List<Mensa> result = scraper.parseDocument(doc);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(Ernaehrung.FLEISCH, result.get(0).getErnaehrung());
    }

    @Test
    public void givenRowWithNoPriceSpan_whenParsed_thenPreisIsZero() {
        String html = "<html><body>"
            + "<ul class=\"canteen-day-nav\">"
            + "<li><a rel=\"2026-05-06\">Mi</a></li>"
            + "</ul>"
            + "<div id=\"canteen_day_1\" class=\"canteen-day\"><table>"
            + "<tr class=\"mt-2\">"
            + "<td class=\"mtd-icon\">"
            + "<img src=\"/layout/icons/vegetarisches-gericht.svg\"></td>"
            + "<td class=\"first menu-title\">"
            + "<span><b>Suppe</b></span><sup>[Se]</sup>"
            + "</td>"
            + "<td></td>"
            + "</tr>"
            + "</table></div>"
            + "</body></html>";
        Document doc = Jsoup.parse(html);
        List<Mensa> result = scraper.parseDocument(doc);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(0.0, result.get(0).getPreis(), 0.001);
    }

    @Test
    public void givenRowWithUnparsablePrice_whenParsed_thenPreisIsZero() {
        String html = "<html><body>"
            + "<ul class=\"canteen-day-nav\">"
            + "<li><a rel=\"2026-05-06\">Mi</a></li>"
            + "</ul>"
            + "<div id=\"canteen_day_1\" class=\"canteen-day\"><table>"
            + "<tr class=\"mt-2\">"
            + "<td class=\"mtd-icon\">"
            + "<img src=\"/layout/icons/veganes-gericht.svg\"></td>"
            + "<td class=\"first menu-title\">"
            + "<span><b>Tofu Bowl</b></span>"
            + "</td>"
            + "<td><span class=\"bgp price_1\">N/A</span></td>"
            + "</tr>"
            + "</table></div>"
            + "</body></html>";
        Document doc = Jsoup.parse(html);
        List<Mensa> result = scraper.parseDocument(doc);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(0.0, result.get(0).getPreis(), 0.001);
    }

    @Test
    public void givenRowWithoutBoldInTitle_whenParsed_thenUsesFullText() {
        String html = "<html><body>"
            + "<ul class=\"canteen-day-nav\">"
            + "<li><a rel=\"2026-05-06\">Mi</a></li>"
            + "</ul>"
            + "<div id=\"canteen_day_1\" class=\"canteen-day\"><table>"
            + "<tr class=\"mt-2\">"
            + "<td class=\"mtd-icon\">"
            + "<img src=\"/layout/icons/veganes-gericht.svg\"></td>"
            + "<td class=\"first menu-title\">"
            + "<span>Gemüsesuppe</span>"
            + "</td>"
            + "<td><span class=\"bgp price_1\">2,10 &euro;</span></td>"
            + "</tr>"
            + "</table></div>"
            + "</body></html>";
        Document doc = Jsoup.parse(html);
        List<Mensa> result = scraper.parseDocument(doc);

        Assertions.assertEquals(1, result.size());
        Assertions.assertNotNull(result.get(0).getEssensbezeichnung());
        Assertions.assertFalse(result.get(0).getEssensbezeichnung().isEmpty());
    }

    @Test
    public void givenRowWithoutAllergene_whenParsed_thenAllergenIsEmpty() {
        String html = "<html><body>"
            + "<ul class=\"canteen-day-nav\">"
            + "<li><a rel=\"2026-05-06\">Mi</a></li>"
            + "</ul>"
            + "<div id=\"canteen_day_1\" class=\"canteen-day\"><table>"
            + "<tr class=\"mt-2\">"
            + "<td class=\"mtd-icon\">"
            + "<img src=\"/layout/icons/veganes-gericht.svg\"></td>"
            + "<td class=\"first menu-title\">"
            + "<span><b>Reissalat</b></span>"
            + "</td>"
            + "<td><span class=\"bgp price_1\">2,90 &euro;</span></td>"
            + "</tr>"
            + "</table></div>"
            + "</body></html>";
        Document doc = Jsoup.parse(html);
        List<Mensa> result = scraper.parseDocument(doc);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("", result.get(0).getAllergene());
    }

    @Test
    public void givenDayNavWithEmptyRelAttribute_whenParsed_thenDayIsSkipped() {
        String html = "<html><body>"
            + "<ul class=\"canteen-day-nav\">"
            + "<li><a rel=\"\">Mo</a></li>"
            + "<li><a rel=\"2026-05-07\">Di</a></li>"
            + "</ul>"
            + "<div class=\"canteen-day\"><table>"
            + "<tr class=\"mt-2\">"
            + "<td class=\"mtd-icon\">"
            + "<img src=\"/layout/icons/veganes-gericht.svg\"></td>"
            + "<td class=\"first menu-title\">"
            + "<span><b>Meal A</b></span>"
            + "</td>"
            + "<td><span class=\"bgp price_1\">3,00 &euro;</span></td>"
            + "</tr>"
            + "</table></div>"
            + "<div class=\"canteen-day\"><table>"
            + "<tr class=\"mt-2\">"
            + "<td class=\"mtd-icon\">"
            + "<img src=\"/layout/icons/veganes-gericht.svg\"></td>"
            + "<td class=\"first menu-title\">"
            + "<span><b>Meal B</b></span>"
            + "</td>"
            + "<td><span class=\"bgp price_1\">3,50 &euro;</span></td>"
            + "</tr>"
            + "</table></div>"
            + "</body></html>";
        Document doc = Jsoup.parse(html);
        List<Mensa> result = scraper.parseDocument(doc);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(LocalDate.of(2026, 5, 7), result.get(0).getDatum());
    }
}
