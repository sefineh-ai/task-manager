package com.safaricom.taskmanager.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Cleans rich-text descriptions coming from the editor.
 * Only the formatting the editor produces is kept; scripts, event handlers, links and any other
 * markup are removed. Inline styles are reduced to font-size and font-family with safe values,
 * and images may only point at this API's own image endpoint.
 */
public final class HtmlSanitizer {

    private static final Safelist SAFELIST = Safelist.none()
            .addTags("p", "h1", "h2", "h3", "blockquote", "hr", "br", "strong", "em", "u", "s", "code", "pre",
                    "ul", "ol", "li", "span", "img")
            .addAttributes("span", "style")
            .addAttributes("img", "src", "alt")
            .addProtocols("img", "src", "http", "https");

    private static final Pattern FONT_SIZE = Pattern.compile("\\d{1,2}px");
    private static final Pattern FONT_FAMILY = Pattern.compile("[\\w\\s,'\"-]{1,80}");
    private static final Pattern IMAGE_SRC = Pattern.compile("https?://[\\w.-]+(:\\d{1,5})?/api/images/\\d+");

    private HtmlSanitizer() {
    }

    /** Returns safe HTML, or null when the content has no visible text and no image. */
    public static String sanitize(String html) {
        if (html == null) {
            return null;
        }
        Document clean = new Cleaner(SAFELIST).clean(Jsoup.parseBodyFragment(html));
        clean.select("img").forEach(img -> {
            if (!IMAGE_SRC.matcher(img.attr("src")).matches()) {
                img.remove();
            }
        });
        if (clean.body().text().isBlank() && clean.select("img").isEmpty()) {
            return null;
        }
        for (Element span : clean.select("span[style]")) {
            String style = allowedStyle(span.attr("style"));
            if (style.isEmpty()) {
                span.removeAttr("style");
            } else {
                span.attr("style", style);
            }
        }
        clean.outputSettings().prettyPrint(false);
        return clean.body().html();
    }

    /** Number of visible characters, ignoring markup. Blocks are separated by one space. */
    public static int textLength(String html) {
        return html == null ? 0 : Jsoup.parseBodyFragment(html).body().text().length();
    }

    private static String allowedStyle(String style) {
        List<String> kept = new ArrayList<>();
        for (String declaration : style.split(";")) {
            int colon = declaration.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String property = declaration.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = declaration.substring(colon + 1).trim();
            boolean safe = switch (property) {
                case "font-size" -> FONT_SIZE.matcher(value).matches();
                case "font-family" -> FONT_FAMILY.matcher(value).matches();
                default -> false;
            };
            if (safe) {
                kept.add(property + ": " + value);
            }
        }
        return String.join("; ", kept);
    }
}
