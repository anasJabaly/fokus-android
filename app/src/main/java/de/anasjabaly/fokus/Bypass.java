package de.anasjabaly.fokus;

import android.os.SystemClock;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Zeitlich begrenzte Freigaben nach der Wartezeit ("Trotzdem öffnen").
 * Nur im Speicher: Nach einem Neustart der App gelten wieder alle Sperren.
 */
public final class Bypass {

    private static final Map<String, Long> UNTIL = new HashMap<>();

    private Bypass() { }

    public static synchronized void allow(String key, int minutes) {
        UNTIL.put(key, SystemClock.elapsedRealtime() + minutes * 60_000L);
    }

    public static synchronized boolean isActive(String key) {
        long now = SystemClock.elapsedRealtime();
        Iterator<Map.Entry<String, Long>> it = UNTIL.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue() <= now) it.remove();
        }
        return UNTIL.containsKey(key);
    }

    public static synchronized void clear() {
        UNTIL.clear();
    }
}
