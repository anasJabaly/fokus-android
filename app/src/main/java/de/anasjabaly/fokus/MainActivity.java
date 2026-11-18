package de.anasjabaly.fokus;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Einstellungen. */
public class MainActivity extends Activity {

    private LinearLayout col;
    private TextView status;
    private Button appsButton;
    private Button unlockButton;
    private TextView lockInfo;
    private CountDownTimer lockTimer;
    private boolean unlocked;

    private Switch enabled, blockShorts, blockYtMusic, filterSearch, strictMode, lockSettings;
    private EditText waitSeconds, bypassMinutes, lockSeconds, keywords, sites;

    /** Alles, was bei aktiver Sperre erst nach der Wartezeit bedienbar ist. */
    private final List<View> lockable = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);

        Config cfg = Config.load(this);

        col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 20);
        col.setPadding(pad, pad, pad, Ui.dp(this, 40));

        TextView title = Ui.text(this, "Fokus", 26, Ui.CREAM);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        col.addView(title);
        col.addView(Ui.text(this, "Deine Zeit gehört dir.", 14, Ui.CREAM_DIM));

        // ---- Status des Bedienungshilfe-Dienstes ----
        status = Ui.text(this, "", 15, Ui.GOLD);
        col.addView(status, Ui.fill(this, 18));
        Button openA11y = Ui.button(this, "Bedienungshilfen öffnen", false);
        openA11y.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        col.addView(openA11y, Ui.fill(this, 8));

        // ---- Sperrhinweis ----
        lockInfo = Ui.text(this, "Die Einstellungen sind gesperrt. Zum Ändern musst du erst warten "
                + "und diesen Bildschirm offen lassen.", 14, Ui.CREAM);
        col.addView(lockInfo, Ui.fill(this, 18));
        unlockButton = Ui.button(this, "", false);
        col.addView(unlockButton, Ui.fill(this, 8));

        // ---- Schalter ----
        section("Sperren");
        enabled = toggle("Aktiviert", null, cfg.enabled);
        blockShorts = toggle("Shorts blockieren", "YouTube-App und Browser", cfg.blockShorts);
        blockYtMusic = toggle("YouTube Music sperren", "App und music.youtube.com", cfg.blockYtMusic);
        filterSearch = toggle("Suchbegriffe filtern", "Google, YouTube und andere Suchen im Browser, Suche in der YouTube-App", cfg.filterSearch);

        appsButton = Ui.button(this, "", false);
        appsButton.setOnClickListener(v -> startActivity(new Intent(this, AppPickerActivity.class)));
        col.addView(appsButton, Ui.fill(this, 14));
        lockable.add(appsButton);

        section("Strenge");
        strictMode = toggle("Strikter Modus", "Kein „Trotzdem öffnen\"", cfg.strictMode);
        waitSeconds = number("Wartezeit vor „Trotzdem öffnen\" (Sekunden)", cfg.waitSeconds);
        bypassMinutes = number("Freigabe gilt danach (Minuten)", cfg.bypassMinutes);
        lockSettings = toggle("Einstellungen sperren", "Ändern erst nach Wartezeit möglich", cfg.lockSettings);
        lockSeconds = number("Sperrzeit vor dem Ändern (Sekunden)", cfg.lockSeconds);

        section("Blockierte Begriffe");
        col.addView(Ui.text(this, "Einer pro Zeile. Kurze Begriffe (bis 4 Zeichen) zählen nur am Wortanfang. "
                + "Mit = davor zählt nur das ganze Wort: =rap trifft „Rap\", aber nicht „Graph\".", 13, Ui.CREAM_DIM));
        keywords = multiline(Config.join(cfg.keywords), 7);

        section("Gesperrte Webseiten");
        col.addView(Ui.text(this, "Eine pro Zeile, z. B. tiktok.com oder instagram.com/reels", 13, Ui.CREAM_DIM));
        sites = multiline(Config.join(cfg.blockedSites), 4);

        Button save = Ui.button(this, "Speichern", true);
        save.setOnClickListener(v -> save());
        col.addView(save, Ui.fill(this, 22));
        lockable.add(save);

        Button reset = Ui.button(this, "Standardbegriffe wiederherstellen", false);
        reset.setOnClickListener(v -> {
            keywords.setText(Config.join(Config.DEFAULT_KEYWORDS));
            Toast.makeText(this, "Eingesetzt. Jetzt „Speichern\" drücken.", Toast.LENGTH_SHORT).show();
        });
        col.addView(reset, Ui.fill(this, 10));
        lockable.add(reset);

        // ---- Fehlersuche ----
        section("Fehlersuche");
        col.addView(Ui.text(this, "Erkennt Fokus Shorts in der YouTube-App nicht richtig? Starte die Aufzeichnung, "
                + "öffne YouTube und zeig dort 60 Sekunden lang Startseite, Shorts-Regal, Shorts-Tab und ein Short. "
                + "Die Aufzeichnung enthält den Aufbau der Bildschirme samt sichtbarer Texte und bleibt auf dem Handy, "
                + "bis du sie selbst teilst.", 13, Ui.CREAM_DIM));
        Button diagStart = Ui.button(this, "YouTube 60 Sekunden aufzeichnen", false);
        diagStart.setOnClickListener(v -> {
            Diagnose.start(this, 60_000);
            Toast.makeText(this, "Aufzeichnung läuft. Öffne jetzt YouTube.", Toast.LENGTH_LONG).show();
        });
        col.addView(diagStart, Ui.fill(this, 10));
        Button diagShare = Ui.button(this, "Aufzeichnung teilen", false);
        diagShare.setOnClickListener(v -> {
            String text = Diagnose.read(this);
            if (text.isEmpty()) {
                Toast.makeText(this, "Noch keine Aufzeichnung vorhanden.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (text.length() > 150_000) text = text.substring(0, 150_000);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_SUBJECT, "Fokus: YouTube-Aufbau");
            send.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(send, "Aufzeichnung teilen"));
        });
        col.addView(diagShare, Ui.fill(this, 10));

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.BG);
        scroll.addView(col);
        setContentView(scroll);

        applyLock(cfg);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean on = serviceEnabled();
        status.setText(on
                ? "✓ Fokus ist aktiv."
                : "Fokus ist noch nicht aktiv. Öffne die Bedienungshilfen und schalte „Fokus\" ein.");
        status.setTextColor(on ? 0xFF9FD8A8 : Ui.GOLD);
        int n = Config.load(this).blockedApps.size();
        appsButton.setText("Gesperrte Apps wählen (" + n + ")");
    }

    /** Wer den Bildschirm verlässt, muss die Sperrzeit neu abwarten. */
    @Override
    protected void onStop() {
        super.onStop();
        if (lockTimer != null) {
            lockTimer.cancel();
            lockTimer = null;
            applyLock(Config.load(this));
        }
    }

    // ---- Einstellungs-Sperre ----
    private void applyLock(final Config cfg) {
        boolean locked = cfg.lockSettings && !unlocked;
        lockInfo.setVisibility(locked ? View.VISIBLE : View.GONE);
        unlockButton.setVisibility(locked ? View.VISIBLE : View.GONE);
        for (View v : lockable) Ui.setEnabledDim(v, !locked);
        if (!locked) return;

        Ui.setEnabledDim(unlockButton, true);
        unlockButton.setText("Entsperren (" + cfg.lockSeconds + " s warten)");
        unlockButton.setOnClickListener(v -> {
            Ui.setEnabledDim(unlockButton, false);
            lockTimer = new CountDownTimer(cfg.lockSeconds * 1000L, 1000L) {
                @Override
                public void onTick(long msLeft) {
                    unlockButton.setText("Noch " + ((msLeft + 999) / 1000) + " Sekunden …");
                }

                @Override
                public void onFinish() {
                    lockTimer = null;
                    unlocked = true;
                    applyLock(cfg);
                }
            }.start();
        });
    }

    // ---- Speichern ----
    private void save() {
        SharedPreferences.Editor e = Config.prefs(this).edit();
        e.putBoolean(Config.K_ENABLED, enabled.isChecked());
        e.putBoolean(Config.K_BLOCK_SHORTS, blockShorts.isChecked());
        e.putBoolean(Config.K_BLOCK_YT_MUSIC, blockYtMusic.isChecked());
        e.putBoolean(Config.K_FILTER_SEARCH, filterSearch.isChecked());
        e.putBoolean(Config.K_STRICT, strictMode.isChecked());
        e.putBoolean(Config.K_LOCK_SETTINGS, lockSettings.isChecked());
        e.putInt(Config.K_WAIT_SECONDS, Config.clamp(intOf(waitSeconds, 15), 5, 600));
        e.putInt(Config.K_BYPASS_MINUTES, Config.clamp(intOf(bypassMinutes, 5), 1, 120));
        e.putInt(Config.K_LOCK_SECONDS, Config.clamp(intOf(lockSeconds, 60), 5, 600));
        e.putString(Config.K_KEYWORDS, Config.join(Config.lines(keywords.getText().toString())));
        e.putString(Config.K_SITES, Config.join(Config.lines(sites.getText().toString())));
        e.apply();
        Bypass.clear();
        Toast.makeText(this, "✓ Gespeichert", Toast.LENGTH_SHORT).show();
    }

    private static int intOf(EditText field, int fallback) {
        try {
            return Integer.parseInt(field.getText().toString().trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private boolean serviceEnabled() {
        String on = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (on == null) return false;
        ComponentName me = new ComponentName(this, FokusService.class);
        for (String entry : on.split(":")) {
            ComponentName cn = ComponentName.unflattenFromString(entry);
            if (me.equals(cn)) return true;
        }
        return false;
    }

    // ---- Bausteine ----
    private void section(String name) {
        TextView t = Ui.text(this, name, 13, Ui.GOLD);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setAllCaps(true);
        t.setLetterSpacing(0.08f);
        col.addView(t, Ui.fill(this, 28));
    }

    private Switch toggle(String label, String hint, boolean checked) {
        Switch s = new Switch(this);
        s.setText(label);
        s.setTextSize(16);
        s.setTextColor(Ui.CREAM);
        s.setChecked(checked);
        s.setMinHeight(Ui.dp(this, 48));
        col.addView(s, Ui.fill(this, 6));
        if (hint != null) col.addView(Ui.text(this, hint, 13, Ui.CREAM_DIM));
        lockable.add(s);
        return s;
    }

    private EditText number(String label, int value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = Ui.text(this, label, 15, Ui.CREAM);
        row.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        EditText e = field();
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setText(String.valueOf(value));
        e.setGravity(Gravity.CENTER);
        row.addView(e, new LinearLayout.LayoutParams(Ui.dp(this, 84), LinearLayout.LayoutParams.WRAP_CONTENT));
        col.addView(row, Ui.fill(this, 10));
        lockable.add(e);
        return e;
    }

    private EditText multiline(String value, int minLines) {
        EditText e = field();
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        e.setSingleLine(false);
        e.setMinLines(minLines);
        e.setGravity(Gravity.TOP | Gravity.START);
        e.setTypeface(Typeface.MONOSPACE);
        e.setText(value);
        col.addView(e, Ui.fill(this, 8));
        lockable.add(e);
        return e;
    }

    private EditText field() {
        EditText e = new EditText(this);
        e.setTextColor(Ui.CREAM);
        e.setTextSize(14);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Ui.BG_DARK);
        bg.setCornerRadius(Ui.dp(this, 8));
        bg.setStroke(Ui.dp(this, 1), 0x33F0EBDE);
        e.setBackground(bg);
        int p = Ui.dp(this, 10);
        e.setPadding(p, p, p, p);
        return e;
    }
}
