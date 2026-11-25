package de.anasjabaly.fokus;

import android.content.Context;

/** Installierte Version (kommt aus app/build.gradle, auf GitHub mit der Build-Nummer). */
final class BuildInfo {

    private BuildInfo() { }

    static String version(Context ctx) {
        try {
            return ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }
}
