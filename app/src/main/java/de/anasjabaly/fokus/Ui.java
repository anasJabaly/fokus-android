package de.anasjabaly.fokus;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Farben und kleine Helfer, damit alle Bildschirme gleich aussehen (wie die Browser-Erweiterung). */
final class Ui {

    static final int BG = 0xFF0D3B3B;
    static final int BG_DARK = 0xFF0B3232;
    static final int CREAM = 0xFFF0EBDE;
    static final int CREAM_DIM = 0x99F0EBDE;
    static final int GOLD = 0xFFD8B46A;
    static final int INK = 0xFF10312F;

    private Ui() { }

    static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    static TextView text(Context c, String s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setLineSpacing(0f, 1.25f);
        return t;
    }

    static Button button(Context c, String s, boolean primary) {
        Button b = new Button(c);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(primary ? INK : CREAM);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(c, 100));
        if (primary) {
            bg.setColor(GOLD);
        } else {
            bg.setColor(0x00000000);
            bg.setStroke(dp(c, 1.5f), 0x66F0EBDE);
        }
        b.setBackground(bg);
        b.setStateListAnimator(null);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(c, 22), dp(c, 12), dp(c, 22), dp(c, 12));
        b.setMinHeight(dp(c, 48));
        return b;
    }

    static LinearLayout.LayoutParams fill(Context c, int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, topDp);
        return lp;
    }

    static void setEnabledDim(View v, boolean enabled) {
        v.setEnabled(enabled);
        v.setAlpha(enabled ? 1f : 0.45f);
    }
}
