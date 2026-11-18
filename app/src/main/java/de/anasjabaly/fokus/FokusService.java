package de.anasjabaly.fokus;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Herzstück der App: Der Bedienungshilfe-Dienst sieht, welche App im Vordergrund ist,
 * und liest gezielt einzelne Felder (Adresszeile, Suchfeld, Shorts-Elemente), um zu sperren.
 * Es wird nichts gesendet. Nur die vom Nutzer gestartete Fehlersuche schreibt lokal eine Datei.
 */
public class FokusService extends AccessibilityService
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    /** Gesperrte Apps werden nur überdeckt; "Zurück" auf dem Sperrbildschirm führt zum Startbildschirm. */
    private static final int NO_ACTION = -1;
    /** Mindestabstand zwischen zwei Prüfungen (ms). */
    private static final long CHECK_GAP = 250;
    /** Regelmäßige Prüfung, solange YouTube oder ein Browser vorne ist (ms).
     *  Nötig, weil ein laufendes Video kaum Ereignisse meldet. */
    private static final long TICK = 600;
    private static final int MAX_NODES = 2500;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable trailingCheck = () -> { checkPending = false; checkNow(); };
    private final Runnable tick = () -> { ticking = false; checkNow(); };

    private Config cfg;
    private ShortsCover cover;
    private String lastKey = "";
    private long lastBlockAt = 0;
    private long lastCheck = 0;
    private boolean checkPending = false;
    private boolean ticking = false;
    private long lastDump = 0;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        cfg = Config.load(this);
        cover = new ShortsCover(this);
        Config.prefs(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        Config.prefs(this).unregisterOnSharedPreferenceChangeListener(this);
        handler.removeCallbacksAndMessages(null);
        if (cover != null) cover.clear();
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        cfg = Config.load(this);
        if (!cfg.enabled || !cfg.blockShorts) cover.clear();
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
        if (pkg.equals(getPackageName())) {
            cover.clear();
            return;
        }

        // 1) Gesperrte Apps
        boolean musicApp = cfg.blockYtMusic && Targets.YOUTUBE_MUSIC.equals(pkg);
        if (cfg.blockedApps.contains(pkg) || musicApp) {
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                String reason = musicApp ? "YouTube Music ist blockiert." : appLabel(pkg) + " ist gesperrt.";
                block("app:" + pkg, reason, pkg, NO_ACTION);
            }
            return;
        }

        if (!Targets.YOUTUBE.equals(pkg) && !Targets.BROWSERS.containsKey(pkg)) {
            // Andere App vorne: Abdeckungen weg (ein Fensterwechsel genügt als Anlass)
            if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) checkNow();
            return;
        }
        scheduleCheck();
    }

    /** Prüft sofort oder, falls gerade geprüft wurde, kurz danach (damit kein Ereignis verloren geht). */
    private void scheduleCheck() {
        long wait = lastCheck + CHECK_GAP - SystemClock.elapsedRealtime();
        if (wait <= 0) {
            checkNow();
        } else if (!checkPending) {
            checkPending = true;
            handler.postDelayed(trailingCheck, wait);
        }
    }

    private void checkNow() {
        if (cfg == null || !cfg.enabled) { cover.clear(); return; }
        try {
            lastCheck = SystemClock.elapsedRealtime();
            AccessibilityNodeInfo root = getRootInActiveWindow();
            CharSequence rp = root == null ? null : root.getPackageName();
            String pkg = rp == null ? "" : rp.toString();

            if (Targets.YOUTUBE.equals(pkg)) {
                maybeDump(root);
                checkYoutube(root);
            } else {
                cover.clear();
                String urlBarId = Targets.BROWSERS.get(pkg);
                if (urlBarId == null) return;            // keine regelmäßige Prüfung nötig
                checkBrowser(root, urlBarId);
            }
            if (!ticking) {
                ticking = true;
                handler.postDelayed(tick, TICK);
            }
        } catch (RuntimeException e) {
            cover.clear();
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

    // ---- 3) YouTube-App: Shorts-Player, Shorts-Einstiege und Suche ----
    private void checkYoutube(AccessibilityNodeInfo root) {
        List<AccessibilityNodeInfo> nodes = collect(root);
        Rect screen = screenBounds();

        if (cfg.blockShorts && shortsPlayerVisible(root, nodes, screen)) {
            cover.clear();
            block("shorts", "Shorts sind blockiert — endloses Scrollen frisst am meisten Zeit.",
                    null, GLOBAL_ACTION_BACK);
            return;
        }
        cover.update(cfg.blockShorts ? shortsEntries(nodes, screen) : new ArrayList<>());

        if (cfg.filterSearch) {
            for (String id : Targets.YT_SEARCH_IDS) {
                List<AccessibilityNodeInfo> found = root.findAccessibilityNodeInfosByViewId(id);
                if (found == null) continue;
                for (AccessibilityNodeInfo n : found) {
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

    /** Läuft gerade der Shorts-Player? Bekannte IDs, große "reel"-Ansicht oder gewählter Shorts-Tab. */
    private boolean shortsPlayerVisible(AccessibilityNodeInfo root, List<AccessibilityNodeInfo> nodes, Rect screen) {
        for (String id : Targets.YT_SHORTS_IDS) {
            if (anyVisible(root.findAccessibilityNodeInfosByViewId(id))) return true;
        }
        Rect b = new Rect();
        for (AccessibilityNodeInfo n : nodes) {
            if (!n.isVisibleToUser()) continue;
            String id = n.getViewIdResourceName();
            if (id != null && id.toLowerCase(Locale.ROOT).contains("reel")
                    && !id.toLowerCase(Locale.ROOT).contains("shelf")) {
                n.getBoundsInScreen(b);
                if (b.height() >= screen.height() * 0.6f) return true;
            }
            if (n.isSelected() && isShortsLabel(n)) return true;   // Shorts-Tab ist geöffnet
        }
        return false;
    }

    /** Bereiche, die abgedeckt werden: Shorts-Tab unten und Shorts-Regale im Feed. */
    private List<Rect> shortsEntries(List<AccessibilityNodeInfo> nodes, Rect screen) {
        List<Rect> out = new ArrayList<>();
        Rect b = new Rect();
        float navTop = screen.height() * 0.78f;
        for (AccessibilityNodeInfo n : nodes) {
            if (!n.isVisibleToUser()) continue;
            String id = n.getViewIdResourceName();
            String idl = id == null ? "" : id.toLowerCase(Locale.ROOT);
            n.getBoundsInScreen(b);
            if (b.isEmpty()) continue;

            if ((idl.contains("reel") && idl.contains("shelf")) || idl.contains("shorts_shelf")) {
                if (b.height() < screen.height() * 0.8f) addRect(out, b, screen);
                continue;
            }
            if (!isShortsLabel(n)) continue;

            if (b.top >= navTop) {
                // Tab in der unteren Leiste: die ganze antippbare Zelle abdecken
                AccessibilityNodeInfo cell = n;
                for (int i = 0; i < 3 && cell != null && !cell.isClickable(); i++) cell = cell.getParent();
                if (cell != null) cell.getBoundsInScreen(b);
                addRect(out, b, screen);
            } else {
                // Überschrift eines Shorts-Regals: den Regal-Container suchen
                Rect head = new Rect(b);
                AccessibilityNodeInfo a = n.getParent();
                Rect ab = new Rect();
                for (int i = 0; i < 6 && a != null; i++, a = a.getParent()) {
                    a.getBoundsInScreen(ab);
                    if (ab.width() >= screen.width() * 0.85f
                            && ab.height() > head.height() * 2.5f
                            && ab.height() < screen.height() * 0.75f
                            && hasScrollable(a, 4)) {
                        addRect(out, ab, screen);
                        break;
                    }
                }
            }
        }
        return out;
    }

    private static boolean isShortsLabel(AccessibilityNodeInfo n) {
        CharSequence t = n.getText();
        CharSequence d = n.getContentDescription();
        return (t != null && "shorts".equalsIgnoreCase(t.toString().trim()))
                || (d != null && "shorts".equalsIgnoreCase(d.toString().trim()));
    }

    private static boolean hasScrollable(AccessibilityNodeInfo n, int depth) {
        if (n == null) return false;
        if (n.isScrollable()) return true;
        if (depth == 0) return false;
        for (int i = 0; i < n.getChildCount(); i++) {
            if (hasScrollable(n.getChild(i), depth - 1)) return true;
        }
        return false;
    }

    private static void addRect(List<Rect> out, Rect r, Rect screen) {
        Rect c = new Rect(r);
        if (c.intersect(screen) && c.width() > 0 && c.height() > 0) out.add(c);
    }

    private static List<AccessibilityNodeInfo> collect(AccessibilityNodeInfo root) {
        List<AccessibilityNodeInfo> out = new ArrayList<>();
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        if (root != null) queue.add(root);
        while (!queue.isEmpty() && out.size() < MAX_NODES) {
            AccessibilityNodeInfo n = queue.poll();
            out.add(n);
            for (int i = 0; i < n.getChildCount(); i++) {
                AccessibilityNodeInfo c = n.getChild(i);
                if (c != null) queue.add(c);
            }
        }
        return out;
    }

    private Rect screenBounds() {
        android.view.WindowManager wm = (android.view.WindowManager) getSystemService(WINDOW_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            return wm.getCurrentWindowMetrics().getBounds();
        }
        android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(dm);
        return new Rect(0, 0, dm.widthPixels, dm.heightPixels);
    }

    private static boolean anyVisible(List<AccessibilityNodeInfo> nodes) {
        if (nodes == null) return false;
        for (AccessibilityNodeInfo n : nodes) {
            if (n != null && n.isVisibleToUser()) return true;
        }
        return false;
    }

    // ---- Fehlersuche: nur auf Wunsch des Nutzers, Datei bleibt auf dem Gerät ----
    private void maybeDump(AccessibilityNodeInfo root) {
        long until = Config.prefs(this).getLong(Diagnose.K_UNTIL, 0);
        long now = System.currentTimeMillis();
        if (now > until || now - lastDump < 2500) return;
        lastDump = now;
        Diagnose.append(this, root, screenBounds());
    }

    // ---- Sperren ----
    private void block(String key, String reason, String appPackage, int globalAction) {
        if (Bypass.isActive(key)) return;
        long now = SystemClock.elapsedRealtime();
        // Nur unterdrücken, solange der Sperrbildschirm dafür schon zu sehen ist
        if (key.equals(lastKey) && (BlockActivity.showing || now - lastBlockAt < 600)) return;
        lastKey = key;
        lastBlockAt = now;

        final Intent i = new Intent(this, BlockActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra(BlockActivity.EXTRA_KEY, key);
        i.putExtra(BlockActivity.EXTRA_REASON, reason);
        if (appPackage != null) i.putExtra(BlockActivity.EXTRA_PACKAGE, appPackage);

        if (globalAction == NO_ACTION) {
            startActivity(i);
        } else {
            // Erst den Player verlassen, dann den Sperrbildschirm zeigen.
            // Sonst läuft das Video weiter und YouTube macht daraus ein kleines Bild-im-Bild-Fenster.
            performGlobalAction(globalAction);
            handler.postDelayed(() -> startActivity(i), 250);
        }
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
