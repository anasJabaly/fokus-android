package de.anasjabaly.fokus;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Liste aller startbaren Apps mit Haken: angehakte Apps werden gesperrt. */
public class AppPickerActivity extends Activity {

    private static final class App {
        final String label;
        final String pkg;

        App(String label, String pkg) {
            this.label = label;
            this.pkg = pkg;
        }
    }

    private final List<App> apps = new ArrayList<>();
    private ListView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);

        loadApps();
        Set<String> blocked = Config.load(this).blockedApps;

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setBackgroundColor(Ui.BG);
        int pad = Ui.dp(this, 18);
        col.setPadding(pad, pad, pad, pad);

        TextView title = Ui.text(this, "Gesperrte Apps", 22, Ui.CREAM);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        col.addView(title);
        col.addView(Ui.text(this, "Angehakte Apps lassen sich nicht mehr öffnen.", 14, Ui.CREAM_DIM), Ui.fill(this, 4));

        List<String> labels = new ArrayList<>();
        for (App a : apps) labels.add(a.label);

        list = new ListView(this);
        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        list.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, labels));
        for (int i = 0; i < apps.size(); i++) {
            if (blocked.contains(apps.get(i).pkg)) list.setItemChecked(i, true);
        }
        LinearLayout.LayoutParams listLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        listLp.topMargin = Ui.dp(this, 12);
        col.addView(list, listLp);

        Button save = Ui.button(this, "Übernehmen", true);
        save.setOnClickListener(v -> saveAndClose());
        col.addView(save, Ui.fill(this, 12));

        setContentView(col);
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        Intent launcher = new Intent(Intent.ACTION_MAIN);
        launcher.addCategory(Intent.CATEGORY_LAUNCHER);
        Set<String> seen = new HashSet<>();
        for (ResolveInfo info : pm.queryIntentActivities(launcher, 0)) {
            if (info.activityInfo == null) continue;
            String pkg = info.activityInfo.packageName;
            if (pkg == null || pkg.equals(getPackageName()) || !seen.add(pkg)) continue;
            CharSequence label = info.loadLabel(pm);
            apps.add(new App(label == null ? pkg : label.toString(), pkg));
        }
        final Collator collator = Collator.getInstance();
        Collections.sort(apps, (a, b) -> collator.compare(a.label, b.label));
    }

    private void saveAndClose() {
        List<String> chosen = new ArrayList<>();
        for (int i = 0; i < apps.size(); i++) {
            if (list.isItemChecked(i)) chosen.add(apps.get(i).pkg);
        }
        Config.prefs(this).edit().putString(Config.K_APPS, Config.join(chosen)).apply();
        Toast.makeText(this, chosen.size() + " Apps gesperrt", Toast.LENGTH_SHORT).show();
        finish();
    }
}
