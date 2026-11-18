package de.anasjabaly.fokus;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Der Sperrbildschirm: "Einen Moment." */
public class BlockActivity extends Activity {

    static final String EXTRA_KEY = "key";
    static final String EXTRA_REASON = "reason";
    static final String EXTRA_PACKAGE = "package";

    /** Ist der Sperrbildschirm gerade zu sehen? (Der Dienst löst dann nicht erneut aus.) */
    static volatile boolean showing;

    private CountDownTimer timer;
    private Button waitButton;
    private String waitLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        build(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        build(intent);
    }

    private void build(Intent intent) {
        cancelTimer();
        final Config cfg = Config.load(this);
        final String key = intent.getStringExtra(EXTRA_KEY);
        final String appPackage = intent.getStringExtra(EXTRA_PACKAGE);
        String reason = intent.getStringExtra(EXTRA_REASON);

        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        int pad = Ui.dp(this, 28);
        col.setPadding(pad, pad, pad, pad);

        ImageView mark = new ImageView(this);
        mark.setImageResource(R.drawable.ic_fokus);
        LinearLayout.LayoutParams markLp = new LinearLayout.LayoutParams(Ui.dp(this, 72), Ui.dp(this, 72));
        markLp.gravity = Gravity.CENTER_HORIZONTAL;
        col.addView(mark, markLp);

        TextView title = Ui.text(this, "Einen Moment.", 32, 0xFFF7F3E8);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        col.addView(title, Ui.fill(this, 24));

        TextView why = Ui.text(this, reason == null ? "" : reason, 16, Ui.GOLD);
        why.setGravity(Gravity.CENTER);
        col.addView(why, Ui.fill(this, 12));

        TextView body = Ui.text(this, "Du hast dir vorgenommen, deine Zeit bewusster zu nutzen. "
                + "Dieser Inhalt gehört nicht dazu — und das hier ist deine Erinnerung daran.", 16, 0xD9F0EBDE);
        body.setGravity(Gravity.CENTER);
        col.addView(body, Ui.fill(this, 18));

        TextView quote = Ui.text(this, "„Nutze fünf, bevor fünf kommen: deine Jugend vor dem Alter, "
                + "deine Gesundheit vor der Krankheit, deinen Reichtum vor der Armut, "
                + "deine freie Zeit vor der Beschäftigung und dein Leben vor dem Tod.\"", 14, Ui.CREAM_DIM);
        quote.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
        quote.setGravity(Gravity.CENTER);
        col.addView(quote, Ui.fill(this, 22));

        Button back = Ui.button(this, "Zurück", true);
        back.setOnClickListener(v -> leave(appPackage != null));
        col.addView(back, Ui.fill(this, 30));

        waitButton = null;
        if (!cfg.strictMode && key != null) {
            waitLabel = "Trotzdem öffnen (" + cfg.waitSeconds + " s warten)";
            final Button wait = Ui.button(this, waitLabel, false);
            waitButton = wait;
            wait.setOnClickListener(v -> {
                Ui.setEnabledDim(wait, false);
                timer = new CountDownTimer(cfg.waitSeconds * 1000L, 1000L) {
                    @Override
                    public void onTick(long msLeft) {
                        wait.setText("Noch " + ((msLeft + 999) / 1000) + " Sekunden …");
                    }

                    @Override
                    public void onFinish() {
                        timer = null;
                        Bypass.allow(key, cfg.bypassMinutes);
                        if (appPackage != null) {
                            Intent launch = getPackageManager().getLaunchIntentForPackage(appPackage);
                            if (launch != null) startActivity(launch);
                        }
                        finish();
                    }
                }.start();
            });
            col.addView(wait, Ui.fill(this, 12));
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.BG);
        scroll.addView(col, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);
    }

    @Override
    protected void onStart() {
        super.onStart();
        showing = true;
    }

    /** Wer den Bildschirm verlässt, muss die Wartezeit neu beginnen. */
    @Override
    protected void onStop() {
        super.onStop();
        showing = false;
        cancelTimer();
        if (waitButton != null) {
            waitButton.setText(waitLabel);
            Ui.setEnabledDim(waitButton, true);
        }
    }

    @Override
    public void onBackPressed() {
        Intent i = getIntent();
        leave(i != null && i.getStringExtra(EXTRA_PACKAGE) != null);
    }

    private void leave(boolean toHome) {
        cancelTimer();
        if (toHome) {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(home);
        }
        finish();
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
