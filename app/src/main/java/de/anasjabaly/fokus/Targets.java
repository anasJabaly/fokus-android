package de.anasjabaly.fokus;

import java.util.HashMap;
import java.util.Map;

/**
 * Alles, was von fremden Apps abhängt, steht an EINER Stelle.
 *
 * Die View-IDs gehören zu den jeweiligen Apps und können sich mit deren Updates ändern.
 * Wenn eine Erkennung nicht mehr greift: mit "uiautomatorviewer" oder dem Layout Inspector
 * die aktuelle ID nachsehen und hier eintragen.
 */
public final class Targets {

    private Targets() { }

    public static final String YOUTUBE = "com.google.android.youtube";
    public static final String YOUTUBE_MUSIC = "com.google.android.apps.youtube.music";

    /** Browser-Paket -> View-ID der Adresszeile. */
    public static final Map<String, String> BROWSERS = new HashMap<>();

    static {
        BROWSERS.put("com.android.chrome", "com.android.chrome:id/url_bar");
        BROWSERS.put("com.chrome.beta", "com.chrome.beta:id/url_bar");
        BROWSERS.put("com.brave.browser", "com.brave.browser:id/url_bar");
        BROWSERS.put("com.microsoft.emmx", "com.microsoft.emmx:id/url_bar");
        BROWSERS.put("com.vivaldi.browser", "com.vivaldi.browser:id/url_bar");
        BROWSERS.put("com.sec.android.app.sbrowser", "com.sec.android.app.sbrowser:id/location_bar_edit_text");
        BROWSERS.put("org.mozilla.firefox", "org.mozilla.firefox:id/mozac_browser_toolbar_url_view");
        BROWSERS.put("org.mozilla.firefox_beta", "org.mozilla.firefox_beta:id/mozac_browser_toolbar_url_view");
        BROWSERS.put("com.opera.browser", "com.opera.browser:id/url_field");
        BROWSERS.put("com.duckduckgo.mobile.android", "com.duckduckgo.mobile.android:id/omnibarTextInput");
    }

    /** YouTube-App: Ist eine dieser Views sichtbar, läuft gerade der Shorts-Player. */
    public static final String[] YT_SHORTS_IDS = {
            YOUTUBE + ":id/reel_recycler",
            YOUTUBE + ":id/reel_watch_player",
            YOUTUBE + ":id/reel_player_page_container"
    };

    /** YouTube-App: Suchfeld bzw. angezeigter Suchbegriff. */
    public static final String[] YT_SEARCH_IDS = {
            YOUTUBE + ":id/search_edit_text",
            YOUTUBE + ":id/search_query"
    };
}
