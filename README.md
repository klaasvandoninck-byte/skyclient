# SkyClient (Fabric, Minecraft 1.21.11)

Client-side mod: lyrics in de lucht, sky glow + sterren, FPS-boost, info-HUD, freecam,
entity ESP en storage ESP.

## Bouwen (eenmalig)

Je hebt nodig: JDK 21 en internet.

**Optie A - IntelliJ IDEA (makkelijkst)**
1. Open de map `skyclient` als Gradle-project (IntelliJ downloadt Gradle zelf).
2. Gradle-paneel -> Tasks -> build -> `build`.

**Optie B - terminal** (met Gradle 9.2 of nieuwer geinstalleerd)
```
cd skyclient
gradle wrapper
./gradlew build        (Windows: gradlew.bat build)
```

Het jar staat daarna in `build/libs/skyclient-1.0.0.jar`
(NIET het `-sources.jar`). Zet dat in `.minecraft/mods`.

Je hebt in Minecraft ook nodig: Fabric Loader (0.18.1+) en Fabric API voor 1.21.11.

## Bediening

| Toets | Actie |
|---|---|
| Right Shift | SkyClient-menu (alle modules aan/uit) |
| V | Freecam aan/uit (WASD + spatie/shift, sprint = sneller) |
| G | Lyrics starten / opnieuw starten |
| C (vasthouden) | Zoom |

Toetsen aanpassen kan bij Opties -> Besturing -> SkyClient.

## Lyrics

Zet je eigen tekst in `.minecraft/config/skyclient/lyrics.txt` (de map opent ook via het menu).
- Gewone regels: wisselen elke 4 seconden (aanpasbaar met `#seconds=3.5`).
- Exacte timing: LRC-formaat, bijv. `[01:23.50] tekst`.
Start je nummer en druk tegelijk op G om te synchroniseren.
De regels hangen op een vaste plek in de lucht: kijk omhoog en ze blijven daar hangen.

## Modules

- Lyrics in lucht, Sky glow (kleurverloop per tijd van de dag), Sterren (twinkelend 's nachts)
- Info HUD: FPS, ping, coordinaten, kijkrichting
- FPS-boost: lagere render/simulation distance, geen wolken, minimale particles, geen schaduwen
  (je oude instellingen worden hersteld als je het uitzet)
- Fullbright, Auto sprint, Zoom
- Freecam, ESP (spelers / vijanden / dieren / items), Storage ESP (kisten, vaten, shulkers, ender chests)

## Let op

Freecam en ESP zijn bij veel SMP-/PvP-servers verboden en kunnen tot een ban leiden.
Gebruik ze alleen waar het mag (singleplayer of servers die het toestaan).
