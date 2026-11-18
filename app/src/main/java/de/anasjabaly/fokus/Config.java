package de.anasjabaly.fokus;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Alle Einstellungen der App. Wird aus den SharedPreferences geladen. */
public final class Config {

    public static final String PREFS = "fokus";

    public static final String K_ENABLED = "enabled";
    public static final String K_BLOCK_SHORTS = "blockShorts";
    public static final String K_BLOCK_YT_MUSIC = "blockYtMusic";
    public static final String K_FILTER_SEARCH = "filterSearch";
    public static final String K_STRICT = "strictMode";
    public static final String K_WAIT_SECONDS = "waitSeconds";
    public static final String K_BYPASS_MINUTES = "bypassMinutes";
    public static final String K_LOCK_SETTINGS = "lockSettings";
    public static final String K_LOCK_SECONDS = "lockSeconds";
    public static final String K_KEYWORDS = "keywords";
    public static final String K_SITES = "blockedSites";
    public static final String K_APPS = "blockedApps";

    /** Gleiche Startliste wie in der Browser-Erweiterung. */
    public static final List<String> DEFAULT_KEYWORDS = Arrays.asList(
            "musik", "lied", "songtext", "konzert", "musikvideo",
            "music", "song", "lyrics", "lyric", "official video",
            "official audio", "music video", "=mv", "album", "=single",
            "remix", "=cover", "karaoke", "playlist musik",
            "=rap", "rapper", "hip hop", "hiphop", "pop hits", "=techno", "=edm",
            "deutschrap", "=charts", "lofi", "lo-fi", "beats to", "mix -", "mix –", "nasheed",
            "اغاني", "أغاني", "اغنية", "أغنية", "موسيقى");

    public boolean enabled;
    public boolean blockShorts;
    public boolean blockYtMusic;
    public boolean filterSearch;
    public boolean strictMode;
    public int waitSeconds;
    public int bypassMinutes;
    public boolean lockSettings;
    public int lockSeconds;
    public List<String> keywords;
    public List<String> blockedSites;
    public Set<String> blockedApps;
    public List<Matcher.Rule> blockRules;

    private Config() { }

    public static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static Config load(Context ctx) {
        SharedPreferences p = prefs(ctx);
        Config c = new Config();
        c.enabled = p.getBoolean(K_ENABLED, true);
        c.blockShorts = p.getBoolean(K_BLOCK_SHORTS, true);
        c.blockYtMusic = p.getBoolean(K_BLOCK_YT_MUSIC, true);
        c.filterSearch = p.getBoolean(K_FILTER_SEARCH, true);
        c.strictMode = p.getBoolean(K_STRICT, true);
        c.waitSeconds = clamp(p.getInt(K_WAIT_SECONDS, 15), 5, 600);
        c.bypassMinutes = clamp(p.getInt(K_BYPASS_MINUTES, 5), 1, 120);
        c.lockSettings = p.getBoolean(K_LOCK_SETTINGS, false);
        c.lockSeconds = clamp(p.getInt(K_LOCK_SECONDS, 60), 5, 600);
        c.keywords = p.contains(K_KEYWORDS) ? lines(p.getString(K_KEYWORDS, "")) : new ArrayList<>(DEFAULT_KEYWORDS);
        c.blockedSites = lines(p.getString(K_SITES, ""));
        c.blockedApps = new HashSet<>(lines(p.getString(K_APPS, "")));
        c.blockRules = Matcher.compile(c.keywords);
        return c;
    }

    public static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /** Zerlegt Text in Zeilen: ohne Leerzeilen, ohne Dopplungen (Groß/klein egal). */
    public static List<String> lines(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        Set<String> seen = new LinkedHashSet<>();
        for (String line : text.split("\n")) {
            String s = line.trim();
            if (s.isEmpty()) continue;
            if (seen.add(s.toLowerCase(Locale.ROOT))) out.add(s);
        }
        return out;
    }

    public static String join(Collection<String> list) {
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            if (sb.length() > 0) sb.append('\n');
            sb.append(s);
        }
        return sb.toString();
    }
}
