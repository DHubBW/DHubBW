package com.se.dhubbw.mensa.infrastructure;

import com.se.dhubbw.mensa.infrastructure.db.Mensa;
import com.se.dhubbw.mensa.infrastructure.util.Ernaehrung;
import com.se.dhubbw.shared.testing.UnitTest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@UnitTest
public class MensaScraperComponentTest {

    /**
     * Helper to build a mensatype_rows block with a given linie name
     * and one or more inner meal rows.
     */
    private static String mensatypeBlock(
            final String linie, final String... innerRows) {
        StringBuilder sb = new StringBuilder();
        sb.append("<tr class=\"mensatype_rows\">")
          .append("<td class=\"mensatype\"><div>")
          .append(linie).append("</div></td>")
          .append("<td class=\"mensadata\">")
          .append("<table class=\"meal-detail-table\">");
        for (String row : innerRows) {
            sb.append(row);
        }
        sb.append("</table></td></tr>");
        return sb.toString();
    }

    private static final String ROW_FLEISCH =
        "<tr class=\"mt-2\">"
        + "<td class=\"mtd-icon\">"
        + "<img src=\"/layout/icons/rindfleisch.svg\"></td>"
        + "<td class=\"first menu-title\">"
        + "<span><b>Chili con Carne</b></span><sup>[We,So]</sup>"
        + "</td>"
        + "<td><span class=\"bgp price_1\">3,60 &euro;</span></td>"
        + "</tr>";

    private static final String ROW_VEGAN =
        "<tr class=\"mt-6\">"
        + "<td class=\"mtd-icon\">"
        + "<img src=\"/layout/icons/veganes-gericht.svg\"></td>"
        + "<td class=\"first menu-title\">"
        + "<span><b>Chili con Soja</b></span><sup>[So]</sup>"
        + "</td>"
        + "<td><span class=\"bgp price_1\">3,60 &euro;</span></td>"
        + "</tr>";

    private static final String ROW_VEGETARISCH =
        "<tr class=\"mt-6\">"
        + "<td class=\"mtd-icon\">"
        + "<img src=\"/layout/icons/vegetarisches-gericht.svg\"></td>"
        + "<td class=\"first menu-title\">"
        + "<span><b>Pasta Arrabiata</b></span><sup>[Ei,We]</sup>"
        + "</td>"
        + "<td><span class=\"bgp price_1\">2,50 &euro;</span></td>"
        + "</tr>";

    private static final String MOCK_HTML_WEEK_1 =
        "<html><body>"
        + "<ul class=\"canteen-day-nav\">"
        + "<li><a rel=\"2026-05-06\">Mi</a></li>"
        + "<li><a rel=\"2026-05-07\">Do</a></li>"
        + "</ul>"
        + "<div id=\"canteen_day_1\" class=\"canteen-day\"><table>"
        + mensatypeBlock("Wahlessen 1", ROW_FLEISCH)
        + mensatypeBlock("Wahlessen 2", ROW_VEGAN, ROW_VEGETARISCH)
        + "</table></div>"
        + "<div id=\"canteen_day_2\" class=\"canteen-day\"></div>"
        + "</body></html>";

    private static final String ROW_SCHNITZEL =
        "<tr class=\"mt-2\">"
        + "<td class=\"mtd-icon\">"
        + "<img src=\"/layout/icons/schweinefleisch.svg\"></td>"
        + "<td class=\"first menu-title\">"
        + "<span><b>Schnitzel mit Pommes</b></span><sup>[We,Ei]</sup>"
        + "</td>"
        + "<td><span class=\"bgp price_1\">4,10 &euro;</span></td>"
        + "</tr>";

    private static final String ROW_LINSENSUPPE =
        "<tr class=\"mt-6\">"
        + "<td class=\"mtd-icon\">"
        + "<img src=\"/layout/icons/veganes-gericht.svg\"></td>"
        + "<td class=\"first menu-title\">"
        + "<span><b>Linsensuppe</b></span><sup>[Se]</sup>"
        + "</td>"
        + "<td><span class=\"bgp price_1\">2,80 &euro;</span></td>"
        + "</tr>";

    private static final String MOCK_HTML_WEEK_2 =
        "<html><body>"
        + "<ul class=\"canteen-day-nav\">"
        + "<li><a rel=\"2026-05-13\">Mi</a></li>"
        + "<li><a rel=\"2026-05-14\">Do</a></li>"
        + "</ul>"
        + "<div id=\"canteen_day_1\" class=\"canteen-day\"><table>"
        + mensatypeBlock("Wahlessen 1", ROW_SCHNITZEL)
        + mensatypeBlock("Wahlessen 2", ROW_LINSENSUPPE)
        + "</table></div>"
        + "<div id=\"canteen_day_2\" class=\"canteen-day\"></div>"
        + "</body></html>";

    private MensaScraperComponent scraper;

    @BeforeEach
    public void setUp() {
        scraper = new MensaScraperComponent("http://localhost");
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
        Mensa fleisch = scraper.parseDocument(doc).getFirst();

        Assertions.assertEquals(
            "Chili con Carne", fleisch.getEssensbezeichnung());
        Assertions.assertEquals("[We,So]", fleisch.getAllergene());
        Assertions.assertEquals(3.60, fleisch.getPreis(), 0.001);
        Assertions.assertEquals(Ernaehrung.FLEISCH, fleisch.getErnaehrung());
    }

    @Test
    public void givenMockHtml_whenParsed_thenLinieIsSetCorrectly() {
        Document doc = Jsoup.parse(MOCK_HTML_WEEK_1);
        List<Mensa> result = scraper.parseDocument(doc);

        Assertions.assertEquals("Wahlessen 1", result.get(0).getLinie());
        Assertions.assertEquals("Wahlessen 2", result.get(1).getLinie());
        Assertions.assertEquals("Wahlessen 2", result.get(2).getLinie());
    }

    @Test
    public void givenMockHtml_whenParsed_thenVeganMealHasCorrectErnaehrung() {
        Document doc = Jsoup.parse(MOCK_HTML_WEEK_1);
        Mensa vegan = scraper.parseDocument(doc).get(1);

        Assertions.assertEquals(Ernaehrung.VEGAN, vegan.getErnaehrung());
        Assertions.assertEquals("Chili con Soja", vegan.getEssensbezeichnung());
    }

    @Test
    public void givenMockHtml_whenParsed_thenVegetarianMealCorrect() {
        Document doc = Jsoup.parse(MOCK_HTML_WEEK_1);
        Mensa vegetarisch = scraper.parseDocument(doc).get(2);

        Assertions.assertEquals(
            Ernaehrung.VEGETARISCH, vegetarisch.getErnaehrung());
    }

    @Test
    public void givenMockHtml_whenParsed_thenMealsHaveCorrectDate() {
        Document doc = Jsoup.parse(MOCK_HTML_WEEK_1);
        List<Mensa> result = scraper.parseDocument(doc);

        result.forEach(m -> Assertions.assertNotNull(m.getDatum()));
        Assertions.assertEquals(
            LocalDate.of(2026, 5, 6),
            result.getFirst().getDatum()
        );
    }

    @Test
    public void givenTwoWeekHtml_whenBothParsed_thenCombined() {
        List<Mensa> week1 =
            scraper.parseDocument(Jsoup.parse(MOCK_HTML_WEEK_1));
        List<Mensa> week2 =
            scraper.parseDocument(Jsoup.parse(MOCK_HTML_WEEK_2));

        List<Mensa> combined = new ArrayList<>();
        combined.addAll(week1);
        combined.addAll(week2);

        Assertions.assertEquals(3, week1.size());
        Assertions.assertEquals(2, week2.size());
        Assertions.assertEquals(5, combined.size());

        Assertions.assertEquals(
            LocalDate.of(2026, 5, 6), week1.getFirst().getDatum());
        Assertions.assertEquals(
            LocalDate.of(2026, 5, 13), week2.getFirst().getDatum());
    }

    @Test
    public void givenRowWithNoIcon_whenParsed_thenErnaehrungIsFleisch() {
        String noIconRow =
            "<tr class=\"mt-2\">"
            + "<td class=\"mtd-icon\"></td>"
            + "<td class=\"first menu-title\">"
            + "<span><b>Schnitzel</b></span><sup>[We]</sup>"
            + "</td>"
            + "<td><span class=\"bgp price_1\">3,80 &euro;</span></td>"
            + "</tr>";
        String html = "<html><body>"
            + "<ul class=\"canteen-day-nav\">"
            + "<li><a rel=\"2026-05-06\">Mi</a></li>"
            + "</ul>"
            + "<div class=\"canteen-day\"><table>"
            + mensatypeBlock("Wahlessen 1", noIconRow)
            + "</table></div>"
            + "</body></html>";
        List<Mensa> result =
            scraper.parseDocument(Jsoup.parse(html));

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(
            Ernaehrung.FLEISCH, result.getFirst().getErnaehrung());
    }

    @Test
    public void givenRowWithNoPriceSpan_whenParsed_thenPreisIsZero() {
        String noPriceRow =
            "<tr class=\"mt-2\">"
            + "<td class=\"mtd-icon\">"
            + "<img src=\"/layout/icons/vegetarisches-gericht.svg\"></td>"
            + "<td class=\"first menu-title\">"
            + "<span><b>Suppe</b></span><sup>[Se]</sup>"
            + "</td>"
            + "<td></td>"
            + "</tr>";
        String html = "<html><body>"
            + "<ul class=\"canteen-day-nav\">"
            + "<li><a rel=\"2026-05-06\">Mi</a></li>"
            + "</ul>"
            + "<div class=\"canteen-day\"><table>"
            + mensatypeBlock("Wahlessen 1", noPriceRow)
            + "</table></div>"
            + "</body></html>";
        List<Mensa> result =
            scraper.parseDocument(Jsoup.parse(html));

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(0.0, result.getFirst().getPreis(), 0.001);
    }

    @Test
    public void givenRowWithUnparsablePrice_whenParsed_thenPreisIsZero() {
        String badPriceRow =
            "<tr class=\"mt-2\">"
            + "<td class=\"mtd-icon\">"
            + "<img src=\"/layout/icons/veganes-gericht.svg\"></td>"
            + "<td class=\"first menu-title\">"
            + "<span><b>Tofu Bowl</b></span>"
            + "</td>"
            + "<td><span class=\"bgp price_1\">N/A</span></td>"
            + "</tr>";
        String html = "<html><body>"
            + "<ul class=\"canteen-day-nav\">"
            + "<li><a rel=\"2026-05-06\">Mi</a></li>"
            + "</ul>"
            + "<div class=\"canteen-day\"><table>"
            + mensatypeBlock("Wahlessen 1", badPriceRow)
            + "</table></div>"
            + "</body></html>";
        List<Mensa> result =
            scraper.parseDocument(Jsoup.parse(html));

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(0.0, result.getFirst().getPreis(), 0.001);
    }

    @Test
    public void givenRowWithoutBoldInTitle_whenParsed_thenUsesFullText() {
        String noBoldRow =
            "<tr class=\"mt-2\">"
            + "<td class=\"mtd-icon\">"
            + "<img src=\"/layout/icons/veganes-gericht.svg\"></td>"
            + "<td class=\"first menu-title\">"
            + "<span>Gemüsesuppe</span>"
            + "</td>"
            + "<td><span class=\"bgp price_1\">2,10 &euro;</span></td>"
            + "</tr>";
        String html = "<html><body>"
            + "<ul class=\"canteen-day-nav\">"
            + "<li><a rel=\"2026-05-06\">Mi</a></li>"
            + "</ul>"
            + "<div class=\"canteen-day\"><table>"
            + mensatypeBlock("Wahlessen 1", noBoldRow)
            + "</table></div>"
            + "</body></html>";
        List<Mensa> result =
            scraper.parseDocument(Jsoup.parse(html));

        Assertions.assertEquals(1, result.size());
        Assertions.assertNotNull(result.getFirst().getEssensbezeichnung());
        Assertions.assertFalse(
            result.getFirst().getEssensbezeichnung().isEmpty());
    }

    @Test
    public void givenRowWithoutAllergene_whenParsed_thenAllergenIsEmpty() {
        String noAllergenRow =
            "<tr class=\"mt-2\">"
            + "<td class=\"mtd-icon\">"
            + "<img src=\"/layout/icons/veganes-gericht.svg\"></td>"
            + "<td class=\"first menu-title\">"
            + "<span><b>Reissalat</b></span>"
            + "</td>"
            + "<td><span class=\"bgp price_1\">2,90 &euro;</span></td>"
            + "</tr>";
        String html = "<html><body>"
            + "<ul class=\"canteen-day-nav\">"
            + "<li><a rel=\"2026-05-06\">Mi</a></li>"
            + "</ul>"
            + "<div class=\"canteen-day\"><table>"
            + mensatypeBlock("Wahlessen 1", noAllergenRow)
            + "</table></div>"
            + "</body></html>";
        List<Mensa> result =
            scraper.parseDocument(Jsoup.parse(html));

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("", result.getFirst().getAllergene());
    }

    @Test
    public void givenDayNavWithEmptyRelAttr_whenParsed_thenDaySkipped() {
        String mealRow =
            "<tr class=\"mt-2\">"
            + "<td class=\"mtd-icon\">"
            + "<img src=\"/layout/icons/veganes-gericht.svg\"></td>"
            + "<td class=\"first menu-title\">"
            + "<span><b>Meal A</b></span>"
            + "</td>"
            + "<td><span class=\"bgp price_1\">3,00 &euro;</span></td>"
            + "</tr>";
        String meal2Row =
            "<tr class=\"mt-2\">"
            + "<td class=\"mtd-icon\">"
            + "<img src=\"/layout/icons/veganes-gericht.svg\"></td>"
            + "<td class=\"first menu-title\">"
            + "<span><b>Meal B</b></span>"
            + "</td>"
            + "<td><span class=\"bgp price_1\">3,50 &euro;</span></td>"
            + "</tr>";
        String html = "<html><body>"
            + "<ul class=\"canteen-day-nav\">"
            + "<li><a rel=\"\">Mo</a></li>"
            + "<li><a rel=\"2026-05-07\">Di</a></li>"
            + "</ul>"
            + "<div class=\"canteen-day\"><table>"
            + mensatypeBlock("Wahlessen 1", mealRow)
            + "</table></div>"
            + "<div class=\"canteen-day\"><table>"
            + mensatypeBlock("Wahlessen 1", meal2Row)
            + "</table></div>"
            + "</body></html>";
        List<Mensa> result =
            scraper.parseDocument(Jsoup.parse(html));

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(
            LocalDate.of(2026, 5, 7), result.getFirst().getDatum());
    }
}
