package de.anasjabaly.fokus;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

/**
 * Herzstück der App: Der Bedienungshilfe-Dienst sieht, welche App im Vordergrund ist,
 * und liest gezielt einzelne Felder (Adresszeile, Suchfeld), um zu sperren.
 * Es wird nichts gespeichert und nichts gesendet.
 */
public class FokusService extends AccessibilityService
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    /** Gesperrte Apps werden nur überdeckt; "Zurück" auf dem Sperrbildschirm führt zum Startbildschirm. */
    private static final int NO_ACTION = -1;

    private Config cfg;
    private String lastKey = "";
    private long lastBlockAt = 0;
    private long lastContentCheck = 0;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        cfg = Config.load(this);
        Config.prefs(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        Config.prefs(this).unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        cfg = Config.load(this);
    }

    @Override
    public void onInterrupt() { }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            handle(event);
        } catch (RuntimeException e) {
            // Fremde Apps können jederzeit ihr Layout ändern; der Dienst darf deshalb nie abstürzen.
        }
    }

    private void handle(AccessibilityEvent event) {
        if (cfg == null || !cfg.enabled || event == null) return;
        CharSequence pkgSeq = event.getPackageName();
        if (pkgSeq == null) return;
        String pkg = pkgSeq.toString();
        if (pkg.equals(getPackageName())) return;

        boolean windowChange = event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;

        // 1) Gesperrte Apps
        boolean musicApp = cfg.blockYtMusic && Targets.YOUTUBE_MUSIC.equals(pkg);
        if (cfg.blockedApps.contains(pkg) || musicApp) {
            if (windowChange) {
                String reason = musicApp ? "YouTube Music ist blockiert." : appLabel(pkg) + " ist gesperrt.";
                block("app:" + pkg, reason, pkg, NO_ACTION);
            }
            return;
        }

        String urlBarId = Targets.BROWSERS.get(pkg);
        boolean youtube = Targets.YOUTUBE.equals(pkg);
        if (urlBarId == null && !youtube) return;

        // Inhalts-Ereignisse kommen sehr oft: höchstens alle 300 ms prüfen
        long now = SystemClock.elapsedRealtime();
        if (!windowChange && now - lastContentCheck < 300) return;
        lastContentCheck = now;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null || !pkg.contentEquals(root.getPackageName())) return;

        if (urlBarId != null) {
            checkBrowser(root, urlBarId);
        } else {
            checkYoutube(root);
        }
    }

    // ---- 2) Browser: Adresszeile prüfen ----
    private void checkBrowser(AccessibilityNodeInfo root, String urlBarId) {
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(urlBarId);
        if (nodes == null || nodes.isEmpty()) return;
        AccessibilityNodeInfo bar = nodes.get(0);
        if (bar == null || bar.getText() == null) return;
        if (bar.isFocused()) return;      // Nutzer tippt gerade: noch keine fertige Adresse

        UrlCheck.Verdict v = UrlCheck.verdict(bar.getText().toString(), cfg.blockShorts,
                cfg.blockYtMusic, cfg.filterSearch, cfg.blockedSites, cfg.blockRules);
        if (v != null) block(v.key, v.reason, null, GLOBAL_ACTION_BACK);
    }

    // ---- 3) YouTube-App: Shorts-Player und Suche ----
    private void checkYoutube(AccessibilityNodeInfo root) {
        if (cfg.blockShorts) {
            for (String id : Targets.YT_SHORTS_IDS) {
                if (anyVisible(root.findAccessibilityNodeInfosByViewId(id))) {
                    block("shorts", "Shorts sind blockiert — endloses Scrollen frisst am meisten Zeit.",
                            null, GLOBAL_ACTION_BACK);
                    return;
                }
            }
        }
        if (cfg.filterSearch) {
            for (String id : Targets.YT_SEARCH_IDS) {
                List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(id);
                if (nodes == null) continue;
                for (AccessibilityNodeInfo n : nodes) {
                    if (n == null || n.getText() == null || n.isFocused() || !n.isVisibleToUser()) continue;
                    String q = n.getText().toString();
                    if (Matcher.matches(q, cfg.blockRules)) {
                        block("q:" + Matcher.norm(q), "Deine Suche „" + q + "\" enthält blockierte Begriffe.",
                                null, GLOBAL_ACTION_BACK);
                        return;
                    }
                }
            }
        }
    }

    private static boolean anyVisible(List<AccessibilityNodeInfo> nodes) {
        if (nodes == null) return false;
        for (AccessibilityNodeInfo n : nodes) {
            if (n != null && n.isVisibleToUser()) return true;
        }
        return false;
    }

    // ---- Sperren ----
    private void block(String key, String reason, String appPackage, int globalAction) {
        if (Bypass.isActive(key)) return;
        long now = SystemClock.elapsedRealtime();
        if (key.equals(lastKey) && now - lastBlockAt < 1500) return;   // nicht doppelt auslösen
        lastKey = key;
        lastBlockAt = now;

        if (globalAction != NO_ACTION) performGlobalAction(globalAction);

        Intent i = new Intent(this, BlockActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra(BlockActivity.EXTRA_KEY, key);
        i.putExtra(BlockActivity.EXTRA_REASON, reason);
        if (appPackage != null) i.putExtra(BlockActivity.EXTRA_PACKAGE, appPackage);
        startActivity(i);
    }

    private String appLabel(String pkg) {
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }
}
