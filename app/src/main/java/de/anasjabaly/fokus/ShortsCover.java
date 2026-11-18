package de.anasjabaly.fokus;

import android.accessibilityservice.AccessibilityService;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Deckt Shorts-Tab und Shorts-Regale in der YouTube-App mit einer Fläche in Hintergrundfarbe ab.
 * Die Fläche fängt Berührungen ab, Shorts lassen sich darüber also nicht mehr öffnen.
 * Benötigt keine zusätzliche Berechtigung (Fenster der Bedienungshilfe).
 */
final class ShortsCover {

    private final AccessibilityService service;
    private final WindowManager wm;
    private final List<View> views = new ArrayList<>();
    private final List<Rect> shown = new ArrayList<>();

    ShortsCover(AccessibilityService service) {
        this.service = service;
        this.wm = (WindowManager) service.getSystemService(AccessibilityService.WINDOW_SERVICE);
    }

    void update(List<Rect> rects) {
        if (rects.equals(shown)) return;
        int color = dark() ? 0xFF0F0F0F : 0xFFFFFFFF;
        float density = service.getResources().getDisplayMetrics().density;

        for (int i = 0; i < rects.size(); i++) {
            Rect r = rects.get(i);
            WindowManager.LayoutParams lp = params(r);
            boolean big = r.height() > 90 * density;
            if (i < views.size()) {
                View v = views.get(i);
                v.setBackgroundColor(color);
                if (v instanceof TextView) ((TextView) v).setText(big ? "Shorts ausgeblendet · Fokus" : "");
                try { wm.updateViewLayout(v, lp); } catch (RuntimeException e) { /* Fenster schon weg */ }
            } else {
                TextView v = new TextView(service);
                v.setBackgroundColor(color);
                v.setTextColor(dark() ? 0x66FFFFFF : 0x66000000);
                v.setTextSize(13);
                v.setGravity(Gravity.CENTER);
                v.setText(big ? "Shorts ausgeblendet · Fokus" : "");
                v.setClickable(true);                       // Berührungen abfangen
                try {
                    wm.addView(v, lp);
                    views.add(v);
                } catch (RuntimeException e) {
                    return;
                }
            }
        }
        while (views.size() > rects.size()) {
            View v = views.remove(views.size() - 1);
            try { wm.removeView(v); } catch (RuntimeException e) { /* ignorieren */ }
        }
        shown.clear();
        shown.addAll(rects);
    }

    void clear() {
        for (View v : views) {
            try { wm.removeView(v); } catch (RuntimeException e) { /* ignorieren */ }
        }
        views.clear();
        shown.clear();
    }

    private WindowManager.LayoutParams params(Rect r) {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                r.width(), r.height(),
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.OPAQUE);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = r.left;
        lp.y = r.top;
        if (Build.VERSION.SDK_INT >= 28) {
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        return lp;
    }

    private boolean dark() {
        int mode = service.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }
}
