package de.anasjabaly.fokus;

import android.content.Context;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Fehlersuche: zeichnet auf Wunsch des Nutzers 60 Sekunden lang den Aufbau der YouTube-App auf
 * (Ansicht, ID, Text, Position). Die Datei bleibt auf dem Gerät, bis der Nutzer sie selbst teilt.
 * Damit lassen sich die Kennungen in Targets.java passend zur installierten YouTube-Version eintragen.
 */
final class Diagnose {

    static final String K_UNTIL = "diagUntil";
    private static final String FILE = "youtube-aufbau.txt";
    private static final int MAX_FILE = 400_000;
    private static final int MAX_NODES = 1500;

    private Diagnose() { }

    static void start(Context ctx, long durationMs) {
        file(ctx).delete();
        Config.prefs(ctx).edit().putLong(K_UNTIL, System.currentTimeMillis() + durationMs).apply();
    }

    static String read(Context ctx) {
        File f = file(ctx);
        if (!f.exists()) return "";
        try {
            return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    static void append(Context ctx, AccessibilityNodeInfo root, Rect screen) {
        File f = file(ctx);
        if (root == null || f.length() > MAX_FILE) return;
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(new SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(new Date()))
                .append("  Bildschirm ").append(screen.width()).append('x').append(screen.height())
                .append("  Fokus ").append(BuildInfo.VERSION).append(" ===\n");
        int[] count = {0};
        dump(root, 0, sb, count);
        try (FileOutputStream out = new FileOutputStream(f, true)) {
            out.write(sb.append('\n').toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // Fehlersuche ist optional
        }
    }

    private static void dump(AccessibilityNodeInfo n, int depth, StringBuilder sb, int[] count) {
        if (n == null || count[0]++ > MAX_NODES) return;
        Rect b = new Rect();
        n.getBoundsInScreen(b);
        for (int i = 0; i < depth; i++) sb.append(' ');
        String cls = n.getClassName() == null ? "?" : n.getClassName().toString();
        sb.append(cls.substring(cls.lastIndexOf('.') + 1));
        if (n.getViewIdResourceName() != null) sb.append(" #").append(n.getViewIdResourceName());
        if (n.getText() != null) sb.append(" t=\"").append(cut(n.getText())).append('"');
        if (n.getContentDescription() != null) sb.append(" d=\"").append(cut(n.getContentDescription())).append('"');
        if (n.isSelected()) sb.append(" SEL");
        if (n.isClickable()) sb.append(" CLK");
        if (n.isScrollable()) sb.append(" SCR");
        if (!n.isVisibleToUser()) sb.append(" unsichtbar");
        sb.append(' ').append(b.toShortString()).append('\n');
        for (int i = 0; i < n.getChildCount(); i++) dump(n.getChild(i), depth + 1, sb, count);
    }

    private static String cut(CharSequence s) {
        String t = s.toString().replace('\n', ' ').replace('"', '\'');
        return t.length() > 40 ? t.substring(0, 40) + "…" : t;
    }

    private static File file(Context ctx) {
        return new File(ctx.getFilesDir(), FILE);
    }
}
