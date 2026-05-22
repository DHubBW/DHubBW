package com.se.dhubbw.mensa.infrastructure;

import com.se.dhubbw.mensa.infrastructure.db.Mensa;
import com.se.dhubbw.mensa.infrastructure.util.Ernaehrung;
import com.se.dhubbw.shared.infrastructure.logger.ColorLogger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Scraper that fetches and parses the mensa meal plan from sw-ka.de.
 *
 * <p>Parses the HTML structure of the Studierendenwerk Karlsruhe meal plan
 * and converts each meal row into a {@link Mensa} entity. The student price
 * ({@code price_1}) is used for the {@code preis} field.
 *
 * <p><b>Allergen-Legende (Zusatzstoffe):</b><br>
 * (1) Farbstoff, (2) Konservierungsstoff, (3) Antioxidationsmittel,
 * (4) Geschmacksverstärker, (5) Phosphat, (6) Oberfläche gewachst,
 * (7) geschwefelt, (8) Oliven geschwärzt, (9) Süßungsmittel,
 * (10) kann bei übermäßigem Verzehr abführend wirken,
 * (11) Phenylalaninquelle, (12) Restalkohol, (14) aus Fleischstücken
 * zusammengefügt, (15) kakaohaltiger Fettglasur,
 * (27) aus Fischstücken zusammengefügt.<br>
 * <b>Allergene:</b>
 * (Ca) Cashewnüsse, (Di) Dinkel, (Ei) Eier, (Er) Erdnüsse, (Fi) Fisch,
 * (Ge) Gerste, (Gl) Glutenhaltiges Getreide, (Ha) Haselnüsse, (Hf) Hafer,
 * (Ka) Kamut, (Kr) Krebstiere, (Lu) Lupine, (Ma) Mandeln,
 * (ML) Milch/Laktose, (Nu) Schalenfrüchte/Nüsse, (Pa) Paranüsse,
 * (Pe) Pekanüsse, (Pi) Pistazie, (Qu) Macadamianüsse, (Ro) Roggen,
 * (Sa) Sesam, (Se) Sellerie, (Sf) Schwefeldioxid/Sulfit, (Sn) Senf,
 * (So) Soja, (Wa) Walnüsse, (We) Weizen, (Wt) Weichtiere.<br>
 * <b>Icons:</b> Rindfleisch, regionales Rindfleisch, Schweinefleisch,
 * regionales Schweinefleisch, Geflügel, regionales Geflügel,
 * vegetarisches Gericht, veganes Gericht, MSC-Fisch, MensaVital,
 * (LAB) tierisches Lab, (GEL) Gelatine.
 */
@Component
public class MensaScraperComponent {

    /**
     * CSS selector for day navigation anchors.
     */
    private static final String SEL_DAY_NAV = "ul.canteen-day-nav li a";

    /**
     * CSS selector for day container divs.
     */
    private static final String SEL_CANTEEN_DAY = "div.canteen-day";

    /**
     * CSS selector for the outer mensatype row containing one line.
     */
    private static final String SEL_MENSATYPE_ROW = "tr.mensatype_rows";

    /**
     * CSS selector for the line name div inside a mensatype row.
     */
    private static final String SEL_MENSATYPE_NAME = "td.mensatype div";

    /**
     * CSS selector for individual meal title cells.
     */
    private static final String SEL_MEAL_TITLE = "td.menu-title";

    /**
     * CSS selector for the dietary icon inside a meal row.
     */
    private static final String SEL_ICON_IMG = "td.mtd-icon img";

    /**
     * CSS selector for the student price span.
     */
    private static final String SEL_PRICE_1 = "span.price_1";

    /**
     * SVG path fragment identifying vegan meals.
     */
    private static final String ICON_VEGAN = "veganes-gericht";

    /**
     * SVG path fragment identifying vegetarian meals.
     */
    private static final String ICON_VEGETARISCH = "vegetarisches-gericht";

    /**
     * Query parameter name for the ISO calendar week.
     */
    private static final String PARAM_KW = "kw";

    /**
     * URL of the mensa meal plan page.
     */
    private final String mensaUrl;

    /**
     * Logger instance.
     */
    private final ColorLogger colorLogger =
        new ColorLogger(MensaScraperComponent.class);

    /**
     * Construct the scraper with the configured mensa URL.
     *
     * @param url the mensa plan URL injected from {@code mensa.url}
     */
    public MensaScraperComponent(@Value("${mensa.url}") final String url) {
        this.mensaUrl = url;
    }

    /**
     * Fetch meals for the current and the following ISO calendar week.
     *
     * <p>Both weeks are requested separately using the {@code ?kw=} parameter.
     * Returns an empty list and logs an error if a page cannot be reached.
     *
     * @return parsed {@link Mensa} entries for both weeks, never {@code null}
     */
    public List<Mensa> scrapeCurrentWeek() {
        int currentKw = LocalDate.now().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int nextKw = currentKw + 1;

        List<Mensa> result = new ArrayList<>();
        result.addAll(fetchWeek(currentKw));
        result.addAll(fetchWeek(nextKw));
        return result;
    }

    /**
     * Fetch and parse the meal plan for a single ISO calendar week.
     *
     * @param kw the ISO calendar week number
     * @return parsed meals, or an empty list on network error
     */
    private List<Mensa> fetchWeek(final int kw) {
        String url = mensaUrl + "?" + PARAM_KW + "=" + kw;
        try {
            Document doc = Jsoup.connect(url).get();
            return parseDocument(doc);
        } catch (IOException e) {
            colorLogger.colorError(
                "Could not fetch mensa data for KW{} from {}: {}",
                kw, url, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Parse an already-fetched JSoup {@link Document} into a list of meals.
     *
     * <p>This method is package-private to allow direct testing with
     * pre-built HTML documents without requiring network access.
     *
     * @param doc the parsed HTML document
     * @return list of {@link Mensa} entities extracted from the document
     */
    List<Mensa> parseDocument(final Document doc) {
        Elements dayNavLinks = doc.select(SEL_DAY_NAV);
        Elements canteenDays = doc.select(SEL_CANTEEN_DAY);

        List<Mensa> result = new ArrayList<>();
        int dayCount = Math.min(dayNavLinks.size(), canteenDays.size());

        for (int i = 0; i < dayCount; i++) {
            String dateStr = dayNavLinks.get(i).attr("rel");
            if (dateStr.isEmpty()) {
                continue;
            }
            LocalDate datum = LocalDate.parse(dateStr);
            Element canteenDay = canteenDays.get(i);
            result.addAll(parseDayMeals(canteenDay, datum));
        }
        return result;
    }

    /**
     * Parse all meals from a single day container.
     *
     * <p>Iterates over {@code tr.mensatype_rows} elements. Each such
     * row represents one serving line (e.g. "Wahlessen 1") and
     * contains one or more meal entries in a nested table.</p>
     *
     * @param canteenDay the day container element
     * @param datum      the date this container represents
     * @return list of meals for that day
     */
    private List<Mensa> parseDayMeals(
        final Element canteenDay,
        final LocalDate datum) {
        List<Mensa> meals = new ArrayList<>();
        Elements mensatypeRows = canteenDay.select(SEL_MENSATYPE_ROW);

        for (Element mensatypeRow : mensatypeRows) {
            Element linieDiv =
                mensatypeRow.selectFirst(SEL_MENSATYPE_NAME);
            String linie =
                linieDiv != null ? linieDiv.text().trim() : "";

            Elements titleCells =
                mensatypeRow.select(SEL_MEAL_TITLE);
            for (Element titleCell : titleCells) {
                Element row = titleCell.parent();
                if (row == null) {
                    continue;
                }

                String name = extractName(titleCell);
                String allergene = extractAllergene(titleCell);
                double preis = extractPreis(row);
                Ernaehrung ernaehrung = extractErnaehrung(row);

                Mensa mensa = new Mensa();
                mensa.setDatum(datum);
                mensa.setLinie(linie);
                mensa.setEssensbezeichnung(name);
                mensa.setAllergene(allergene);
                mensa.setPreis(preis);
                mensa.setErnaehrung(ernaehrung);
                meals.add(mensa);
            }
        }
        return meals;
    }

    /**
     * Extract the meal name from the menu-title cell.
     *
     * @param titleCell the {@code td.menu-title} element
     * @return trimmed meal name
     */
    private String extractName(final Element titleCell) {
        Element bold = titleCell.selectFirst("b");
        return bold != null ? bold.text().trim() : titleCell.text().trim();
    }

    /**
     * Extract allergen codes from the superscript tag inside the title cell.
     *
     * @param titleCell the {@code td.menu-title} element
     * @return allergen string, e.g. {@code [We,So]}, or empty string if absent
     */
    private String extractAllergene(final Element titleCell) {
        Element sup = titleCell.selectFirst("sup");
        return sup != null ? sup.text().trim() : "";
    }

    /**
     * Extract the student price from the meal row.
     *
     * @param row the {@code <tr>} element containing the meal
     * @return parsed student price, or {@code 0.0} if not found
     */
    private double extractPreis(final Element row) {
        Element priceSpan = row.selectFirst(SEL_PRICE_1);
        if (priceSpan == null) {
            return 0.0;
        }
        String raw = priceSpan.text()
            .replace("€", "")
            .replace(",", ".")
            .trim();
        if (raw.isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            colorLogger.colorError(
                "Could not parse price '{}': {}", raw, e.getMessage());
            return 0.0;
        }
    }

    /**
     * Determine the dietary classification from the meal icon.
     *
     * @param row the {@code <tr>} element containing the meal
     * @return {@link Ernaehrung#VEGAN}, {@link Ernaehrung#VEGETARISCH},
     *         or {@link Ernaehrung#FLEISCH}
     */
    private Ernaehrung extractErnaehrung(final Element row) {
        Element icon = row.selectFirst(SEL_ICON_IMG);
        if (icon == null) {
            return Ernaehrung.FLEISCH;
        }
        String src = icon.attr("src");
        if (src.contains(ICON_VEGAN)) {
            return Ernaehrung.VEGAN;
        }
        if (src.contains(ICON_VEGETARISCH)) {
            return Ernaehrung.VEGETARISCH;
        }
        return Ernaehrung.FLEISCH;
    }
}
