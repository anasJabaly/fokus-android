package de.anasjabaly.fokus;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Begriffs-Abgleich, gleiche Regeln wie in der Browser-Erweiterung:
 *   wort   bis 4 Zeichen: nur am Wortanfang ("lied" trifft "Lieder", nicht "applied")
 *          ab 5 Zeichen: überall ("musik" trifft auch "Hintergrundmusik")
 *   =wort  nur als ganzes Wort ("=rap" trifft "Rap", nicht "Graph")
 * Reines Java ohne Android, damit es sich am PC testen lässt.
 */
public final class Matcher {

    public static final int SUB = 0;
    public static final int START = 1;
    public static final int WORD = 2;

    public static final class Rule {
        public final String k;
        public final int mode;

        Rule(String k, int mode) {
            this.k = k;
            this.mode = mode;
        }
    }

    private Matcher() { }

    public static String norm(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFKC);
    }

    public static List<Rule> compile(Collection<String> list) {
        List<Rule> rules = new ArrayList<>();
        if (list == null) return rules;
        for (String raw : list) {
            String k = norm(raw).trim();
            if (k.isEmpty()) continue;
            int mode = -1;
            if (k.startsWith("=")) {
                mode = WORD;
                k = k.substring(1).trim();
            }
            if (k.isEmpty()) continue;
            if (mode == -1) {
                // Arabisch u.ä. immer überall, weil dort Vorsilben direkt am Wort hängen
                mode = (k.codePointCount(0, k.length()) <= 4 && latinOnly(k)) ? START : SUB;
            }
            rules.add(new Rule(k, mode));
        }
        return rules;
    }

    public static boolean matches(String text, List<Rule> rules) {
        String t = norm(text);
        if (t.isEmpty() || rules == null) return false;
        for (Rule r : rules) {
            if (hits(t, r)) return true;
        }
        return false;
    }

    private static boolean hits(String text, Rule rule) {
        int i = text.indexOf(rule.k);
        if (rule.mode == SUB) return i != -1;
        while (i != -1) {
            int end = i + rule.k.length();
            boolean startOk = i == 0 || !Character.isLetterOrDigit(text.codePointBefore(i));
            boolean endOk = rule.mode == START || end >= text.length()
                    || !Character.isLetterOrDigit(text.codePointAt(end));
            if (startOk && endOk) return true;
            i = text.indexOf(rule.k, i + 1);
        }
        return false;
    }

    private static boolean latinOnly(String k) {
        for (int i = 0; i < k.length(); ) {
            int cp = k.codePointAt(i);
            if (Character.isLetter(cp) && Character.UnicodeScript.of(cp) != Character.UnicodeScript.LATIN) {
                return false;
            }
            i += Character.charCount(cp);
        }
        return true;
    }
}
