# tv-guide

Kleine Spring-Boot-API, die eine XMLTV-Datei einliest und das Fernsehprogramm
als JSON für einen Tag oder eine Woche ausliefert.

## Starten

```sh
mvn spring-boot:run
```

Danach im Browser http://localhost:8080/ öffnen: einfache Tages- und Wochenansicht
(`src/main/resources/static/index.html`).

API-Beispiele (mit den mitgelieferten Testdaten):

```sh
curl "localhost:8080/api/channels"
curl "localhost:8080/api/programme?date=2026-11-21"
curl "localhost:8080/api/programme?date=2026-11-21&channel=Sender-B.de"
curl "localhost:8080/api/programme/week?from=2026-11-21&days=7"
curl "localhost:8080/api/programme/now"
```

## Echte Daten verwenden

In `src/main/resources/application.properties` `epg.source` ändern, z. B.

```properties
epg.source=file:/home/finn/epg/guide.xml
```

Die XMLTV-Datei kannst du z. B. mit [iptv-org/epg](https://github.com/iptv-org/epg)
selbst erzeugen. Lizenz und Nutzungsbedingungen der jeweiligen Quelle beachten,
besonders wenn die App öffentlich wird.

## Aufbau

| Klasse | Aufgabe |
| --- | --- |
| `XmltvParser` | liest XMLTV (auch .gz) per StAX |
| `EpgService` | hält Daten im Speicher, lädt täglich um 04:00 neu |
| `ProgrammeController` | REST-Endpunkte `/api/...` |

## Nächste Schritte

- Daten in einer Datenbank speichern (Spring Data JPA) statt im Speicher
- Eine Sendung gehört bis ca. 5 Uhr morgens oft noch zum "TV-Tag" des Vortags; aktuell zählt der Kalendertag
- Frontend (z. B. React oder Thymeleaf) mit Raster-Ansicht
