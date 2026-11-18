import de.anasjabaly.fokus.Matcher;
import de.anasjabaly.fokus.UrlCheck;

import java.util.Arrays;
import java.util.List;

/** Tests für die reine Logik (ohne Android): javac + java genügen. */
public class LogicTest {

    static int pass = 0, fail = 0;

    static void ok(boolean cond, String name) {
        if (cond) {
            pass++;
        } else {
            fail++;
            System.out.println("FAIL " + name);
        }
    }

    public static void main(String[] args) {
        List<Matcher.Rule> block = Matcher.compile(Arrays.asList(
                "musik", "lied", "music", "song", "official video", "=mv", "=single", "=cover",
                "=rap", "rapper", "=techno", "=charts", "lofi", "mix -", "اغاني", "", "  ", "="));
        ok(block.size() == 15, "leere Einträge werden ignoriert");

        ok(!Matcher.matches("Graph Algorithmus Dijkstra", block), "Graph frei");
        ok(!Matcher.matches("GraphQL Tutorial", block), "GraphQL frei");
        ok(!Matcher.matches("Singleton Pattern", block), "Singleton frei");
        ok(!Matcher.matches("Disaster Recovery", block), "Recovery frei");
        ok(!Matcher.matches("Applied Mathematics", block), "applied frei");
        ok(!Matcher.matches("Informationstechnologie", block), "Technologie frei");
        ok(!Matcher.matches("Rapid Prototyping", block), "Rapid frei");
        ok(Matcher.matches("deutscher rap 2026", block), "rap gesperrt");
        ok(Matcher.matches("Best RAP Songs", block), "Großschreibung egal");
        ok(Matcher.matches("Hintergrundmusik", block), "musik im Wortinneren");
        ok(Matcher.matches("Lieder für Kinder", block), "lied am Wortanfang");
        ok(Matcher.matches("احلى الاغاني", block), "Arabisch mit Vorsilbe");
        ok(!Matcher.matches("", block) && !Matcher.matches(null, block), "leerer Text");

        List<String> sites = Arrays.asList("tiktok.com", "https://www.instagram.com/reels/", "9gag.com");

        ok(v("google.com/search?q=rap+songs&sca_esv=1", sites, block) != null, "Google-Suche gesperrt");
        ok(v("https://www.google.de/search?q=graph+algorithmus", sites, block) == null, "Google-Suche frei");
        ok(v("m.youtube.com/results?search_query=official%20video", sites, block) != null, "YouTube-Suche gesperrt");
        ok(v("duckduckgo.com/?q=lofi+beats&t=h_", sites, block) != null, "DuckDuckGo gesperrt");
        ok(v("youtube.com/shorts/abc123", sites, block) != null, "Shorts gesperrt");
        ok(v("m.youtube.com/watch?v=abc", sites, block) == null, "normales Video frei");
        ok(v("music.youtube.com", sites, block) != null, "YouTube Music gesperrt");
        ok(v("tiktok.com/@jemand", sites, block) != null, "tiktok.com gesperrt");
        ok(v("vm.tiktok.com/x", sites, block) != null, "Subdomain gesperrt");
        ok(v("nottiktok.com", sites, block) == null, "ähnliche Domain frei");
        ok(v("instagram.com/reels/xyz", sites, block) != null, "instagram.com/reels gesperrt");
        ok(v("instagram.com/direct/inbox", sites, block) == null, "instagram.com sonst frei");
        ok(v("rap songs", sites, block) == null, "getippter Text ist keine Adresse");
        ok(v("rap", sites, block) == null, "einzelnes Wort ist keine Adresse");
        ok(v("", sites, block) == null && v(null, sites, block) == null, "leer");
        ok(v("example.com/?q=%E0%A4%A", sites, block) == null, "kaputte Kodierung stürzt nicht ab");

        UrlCheck.Verdict off = UrlCheck.verdict("youtube.com/shorts/abc", false, false, false, sites, block);
        ok(off == null, "Schalter aus: Shorts frei");
        ok(UrlCheck.verdict("google.com/search?q=rap", true, true, false, sites, block) == null, "Schalter aus: Suche frei");

        ok("rap songs".equals(UrlCheck.searchQuery("google.com/search?q=rap+songs#x")), "Suchbegriff wird gelesen");
        ok("youtube.com".equals(UrlCheck.host(UrlCheck.normalize("https://www.YouTube.com/shorts/x"))), "Host wird normalisiert");

        System.out.println(pass + " bestanden, " + fail + " fehlgeschlagen");
        if (fail > 0) System.exit(1);
    }

    static UrlCheck.Verdict v(String url, List<String> sites, List<Matcher.Rule> block) {
        return UrlCheck.verdict(url, true, true, true, sites, block);
    }
}
