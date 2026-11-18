package de.anasjabaly.fokus;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Locale;

/**
 * Bewertet den Text aus der Adresszeile eines Browsers.
 * Reines Java ohne Android, damit es sich am PC testen lässt.
 */
public final class UrlCheck {

    /** Ergebnis einer Prüfung: Schlüssel (für Freigaben) und Text für den Sperrbildschirm. */
    public static final class Verdict {
        public final String key;
        public final String reason;

        Verdict(String key, String reason) {
            this.key = key;
            this.reason = reason;
        }
    }

    private static final String[] QUERY_PARAMS = {"q", "search_query", "query", "p", "text"};

    private UrlCheck() { }

    /** Entfernt Schema und "www."/"m.", alles klein. Ergebnis: host/pfad?query */
    public static String normalize(String urlText) {
        if (urlText == null) return "";
        String u = urlText.trim().toLowerCase(Locale.ROOT);
        int scheme = u.indexOf("://");
        if (scheme != -1 && scheme < 10) u = u.substring(scheme + 3);
        if (u.startsWith("www.")) u = u.substring(4);
        else if (u.startsWith("m.")) u = u.substring(2);
        return u;
    }

    public static String host(String normalized) {
        int end = normalized.length();
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c == '/' || c == '?' || c == '#') {
                end = i;
                break;
            }
        }
        return normalized.substring(0, end);
    }

    /** Sieht der Text wie eine Adresse aus (und nicht wie eine getippte Suche)? */
    public static boolean looksLikeUrl(String urlText) {
        if (urlText == null) return false;
        String u = urlText.trim();
        if (u.isEmpty() || u.indexOf(' ') != -1) return false;
        return host(normalize(u)).indexOf('.') != -1;
    }

    /** Liest den Suchbegriff aus der Adresse (Google, YouTube, Bing, DuckDuckGo, Startpage ...). */
    public static String searchQuery(String urlText) {
        if (urlText == null) return "";
        int q = urlText.indexOf('?');
        if (q == -1) return "";
        String query = urlText.substring(q + 1);
        int hash = query.indexOf('#');
        if (hash != -1) query = query.substring(0, hash);
        for (String part : query.split("&")) {
            int eq = part.indexOf('=');
            if (eq <= 0) continue;
            String name = part.substring(0, eq).toLowerCase(Locale.ROOT);
            for (String wanted : QUERY_PARAMS) {
                if (wanted.equals(name)) return decode(part.substring(eq + 1));
            }
        }
        return "";
    }

    private static String decode(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            return s.replace('+', ' ');
        }
    }

    /** true, wenn die Adresse zu einem Eintrag der Sperrliste gehört ("tiktok.com", "instagram.com/reels"). */
    public static boolean siteBlocked(String normalized, List<String> blockedSites) {
        if (blockedSites == null) return false;
        String host = host(normalized);
        for (String raw : blockedSites) {
            String e = normalize(raw);
            while (e.endsWith("/")) e = e.substring(0, e.length() - 1);
            if (e.isEmpty()) continue;
            if (e.indexOf('/') == -1) {
                if (host.equals(e) || host.endsWith("." + e)) return true;
            } else {
                if (normalized.equals(e) || normalized.startsWith(e + "/") || normalized.startsWith(e + "?")) return true;
            }
        }
        return false;
    }

    /**
     * @return Sperrgrund oder null, wenn die Adresse in Ordnung ist
     */
    public static Verdict verdict(String urlText, boolean blockShorts, boolean blockYtMusic,
                                  boolean filterSearch, List<String> blockedSites,
                                  List<Matcher.Rule> blockRules) {
        if (!looksLikeUrl(urlText)) return null;
        String n = normalize(urlText);
        String host = host(n);
        String path = n.substring(host.length());

        if (siteBlocked(n, blockedSites)) {
            return new Verdict("site:" + host, "Die Seite " + host + " ist gesperrt.");
        }
        boolean youtube = host.equals("youtube.com") || host.endsWith(".youtube.com");
        if (blockYtMusic && host.equals("music.youtube.com")) {
            return new Verdict("ytmusic", "YouTube Music ist blockiert.");
        }
        if (blockShorts && youtube && path.startsWith("/shorts")) {
            return new Verdict("shorts", "Shorts sind blockiert — endloses Scrollen frisst am meisten Zeit.");
        }
        if (filterSearch) {
            String q = searchQuery(urlText.trim());
            if (!q.isEmpty() && Matcher.matches(q, blockRules)) {
                return new Verdict("q:" + Matcher.norm(q), "Deine Suche „" + q + "\" enthält blockierte Begriffe.");
            }
        }
        return null;
    }
}
