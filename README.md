# Fokus (Android)

Die Handy-Version von „Fokus für YouTube": sperrt Apps, Shorts und Suchbegriffe
auf dem ganzen Gerät und zeigt stattdessen den **Moment der Achtsamkeit**.

> **Stand:** Version 0.1.0. Der Code ist gegen die Android-API 34 kompiliert und die
> Logik (Begriffe, Adressen) ist getestet. Auf einem echten Gerät ist die App noch
> **nicht gelaufen**. Siehe „Was noch geprüft werden muss".

## Was sie macht

| Funktion | Wie |
|---|---|
| **Apps sperren** | Liste aller Apps mit Haken. Gesperrte Apps werden beim Öffnen vom Sperrbildschirm überdeckt. |
| **Shorts blockieren** | Erkennt den Shorts-Player in der YouTube-App und `youtube.com/shorts` im Browser. |
| **YouTube Music sperren** | Die App und `music.youtube.com`. |
| **Suchbegriffe filtern** | Liest den Suchbegriff aus der Adresszeile (Google, YouTube, Bing, DuckDuckGo, Startpage) und aus der Suche der YouTube-App. Gleiche Begriffsliste und gleiche Regeln wie in der Browser-Erweiterung. |
| **Webseiten sperren** | Eigene Liste, z. B. `tiktok.com` oder `instagram.com/reels`. |
| **Strikter Modus** | Kein Durchkommen. Ohne ihn: „Trotzdem öffnen" nach Wartezeit, die Freigabe gilt einige Minuten. |
| **Einstellungen sperren** | Ändern erst nach einer Wartezeit auf dem offenen Bildschirm. |

Unterstützte Browser: Chrome, Samsung Internet, Firefox, Brave, Edge, Vivaldi, Opera, DuckDuckGo.

## Bauen

**Weg A: GitHub (kein Android Studio nötig)**

1. Diesen Ordner als eigenes Repository zu GitHub pushen.
2. Unter *Actions* läuft „APK bauen" automatisch.
3. Das Ergebnis `fokus-debug-apk` herunterladen und entpacken: `app-debug.apk`.

**Weg B: Android Studio**

Ordner öffnen, Gradle-Sync abwarten, *Run*. Oder im Terminal: `gradle assembleDebug`
(JDK 17, Gradle 8.9). Die APK liegt dann in `app/build/outputs/apk/debug/`.

## Installieren und einschalten

1. `app-debug.apk` aufs Handy kopieren und installieren (Installation aus unbekannten Quellen erlauben).
2. Ab Android 13 sperrt das System die Bedienungshilfe für APKs von außerhalb des Play Store:
   *Einstellungen → Apps → Fokus → ⋮ (oben rechts) → „Eingeschränkte Einstellungen zulassen"*.
3. Fokus öffnen → **„Bedienungshilfen öffnen"** → *Fokus* einschalten.
4. Zurück in Fokus: Apps wählen, Listen anpassen, **Speichern**.

## Datenschutz

Die App hat **keine Internet-Berechtigung**. Sie liest nur das Paket der App im
Vordergrund sowie die Adresszeile bzw. das Suchfeld und vergleicht sie mit deinen Listen.
Nichts wird gespeichert oder gesendet.

## Aufbau

| Datei | Aufgabe |
|---|---|
| `FokusService.java` | Bedienungshilfe-Dienst: erkennt App, Adresszeile, Shorts, Suche |
| `Targets.java` | **Alle** fremden Paketnamen und View-IDs an einer Stelle |
| `Matcher.java` | Begriffs-Abgleich (reines Java) |
| `UrlCheck.java` | Bewertet Adressen (reines Java) |
| `Config.java` | Einstellungen und Standardlisten |
| `Bypass.java` | Zeitlich begrenzte Freigaben |
| `BlockActivity.java` | Sperrbildschirm |
| `MainActivity.java` | Einstellungen |
| `AppPickerActivity.java` | App-Auswahl |
| `tests/LogicTest.java` | Tests für `Matcher` und `UrlCheck` |

Logik-Tests am PC:

```
javac -d build/logic app/src/main/java/de/anasjabaly/fokus/Matcher.java \
      app/src/main/java/de/anasjabaly/fokus/UrlCheck.java tests/LogicTest.java
java -cp build/logic LogicTest
```

## Was noch geprüft werden muss

- **View-IDs in `Targets.java`.** Sie gehören den fremden Apps (Chrome, YouTube …) und
  können sich mit deren Updates ändern. Greift eine Erkennung nicht, die aktuelle ID mit
  dem Layout Inspector oder `uiautomatorviewer` nachsehen und dort eintragen.
  Am unsichersten: Shorts-Player und Suchfeld der YouTube-App, Firefox.
- **Gradle-Build.** Er lief noch nicht durch (in der Entwicklungsumgebung war Googles
  Maven-Server nicht erreichbar). Der erste Lauf in GitHub Actions zeigt, ob alles passt.
- **Umgehen.** Wer die Bedienungshilfe abschaltet oder die App deinstalliert, ist die
  Sperre los. Die Einstellungs-Sperre schützt nur die Einstellungen in der App.

## Ideen für später

Tageslimits pro App, Zeitpläne (z. B. nur während der Vorlesungszeit), Titel-Erkennung
in der YouTube-App, Schutz vor dem Abschalten der Bedienungshilfe.
