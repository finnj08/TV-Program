package de.example.tvguide.service;

import de.example.tvguide.model.Channel;
import de.example.tvguide.model.Programme;
import de.example.tvguide.xmltv.XmltvParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Hält das Programm im Speicher und lädt es regelmäßig aus der konfigurierten XMLTV-Quelle neu.
 * Für den Anfang reicht In-Memory; später lässt sich das 1:1 durch eine Datenbank ersetzen.
 */
@Service
public class EpgService {

    private static final Logger log = LoggerFactory.getLogger(EpgService.class);
    public static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

    private final Resource source;
    private final XmltvParser parser = new XmltvParser();

    // volatile + unveränderliche Listen: Reload tauscht den Stand atomar aus
    private volatile List<Channel> channels = List.of();
    private volatile List<Programme> programmes = List.of();

    public EpgService(@Value("${epg.source}") Resource source) {
        this.source = source;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "${epg.refresh-cron:0 0 4 * * *}", zone = "Europe/Berlin")
    public void reload() {
        try (InputStream in = source.getInputStream()) {
            XmltvParser.Result result = parser.parse(in);
            this.channels = List.copyOf(result.channels());
            this.programmes = result.programmes().stream()
                    .sorted(Comparator.comparing(Programme::start))
                    .toList();
            log.info("EPG geladen: {} Sender, {} Sendungen (Quelle: {})",
                    channels.size(), programmes.size(), source.getDescription());
        } catch (Exception e) {
            // Alten Stand behalten, falls die Quelle gerade nicht erreichbar ist
            log.error("EPG konnte nicht geladen werden: {}", e.getMessage(), e);
        }
    }

    public List<Channel> channels() {
        return channels;
    }

    /** Alle Sendungen, die an diesem Kalendertag (deutsche Zeit) beginnen. */
    public List<Programme> forDay(LocalDate date, String channelId) {
        return programmes.stream()
                .filter(p -> p.start().withZoneSameInstant(ZONE).toLocalDate().equals(date))
                .filter(p -> channelId == null || p.channelId().equals(channelId))
                .toList();
    }

    /** Wochenansicht: Tag -> Sendungen, für {@code days} Tage ab {@code from}. */
    public Map<LocalDate, List<Programme>> forRange(LocalDate from, int days, String channelId) {
        LocalDate to = from.plusDays(days); // exklusiv
        Map<LocalDate, List<Programme>> byDay = programmes.stream()
                .filter(p -> channelId == null || p.channelId().equals(channelId))
                .collect(Collectors.groupingBy(
                        p -> p.start().withZoneSameInstant(ZONE).toLocalDate(),
                        TreeMap::new,
                        Collectors.toList()));
        byDay.keySet().removeIf(d -> d.isBefore(from) || !d.isBefore(to));
        return byDay;
    }

    /** Was läuft gerade auf welchem Sender? */
    public List<Programme> now() {
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        return programmes.stream()
                .filter(p -> p.stop() != null && !p.start().isAfter(now) && p.stop().isAfter(now))
                .toList();
    }
}
