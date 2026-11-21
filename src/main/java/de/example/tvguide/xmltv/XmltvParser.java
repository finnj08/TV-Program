package de.example.tvguide.xmltv;

import de.example.tvguide.model.Channel;
import de.example.tvguide.model.Programme;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;

/**
 * Liest XMLTV (https://wiki.xmltv.org/index.php/XMLTVFormat) per StAX.
 * Streaming, damit auch große Dateien (mehrere 10 MB) wenig Speicher brauchen.
 */
public class XmltvParser {

    public record Result(List<Channel> channels, List<Programme> programmes) {
    }

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Berlin");
    // XMLTV: "20261121201500 +0100"
    private static final DateTimeFormatter WITH_ZONE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss Z");
    private static final DateTimeFormatter WITHOUT_ZONE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public Result parse(InputStream rawInput) throws IOException, XMLStreamException {
        return parse(rawInput, id -> true);
    }

    /**
     * Wie {@link #parse(InputStream)}, behält aber nur Sender (und deren Sendungen), für die
     * {@code channelFilter} true liefert. Der Filter wirkt schon beim Lesen, so entstehen für
     * uninteressante Sender gar keine Objekte. Das spart Speicher (wichtig bei kleinen Containern).
     */
    public Result parse(InputStream rawInput, Predicate<String> channelFilter)
            throws IOException, XMLStreamException {
        try (InputStream in = maybeGunzip(rawInput)) {
            XMLInputFactory factory = XMLInputFactory.newFactory();
            // Schutz gegen XXE: keine DTDs / externen Entities aus fremden Dateien laden
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

            XMLStreamReader r = factory.createXMLStreamReader(in);
            List<Channel> channels = new ArrayList<>();
            List<Programme> programmes = new ArrayList<>();

            while (r.hasNext()) {
                if (r.next() == XMLStreamConstants.START_ELEMENT) {
                    switch (r.getLocalName()) {
                        case "channel" -> {
                            Channel c = readChannel(r);
                            if (channelFilter.test(c.id())) {
                                channels.add(c);
                            }
                        }
                        case "programme" -> {
                            String channelId = r.getAttributeValue(null, "channel");
                            // Unerwünschte Sendungen werden nicht gelesen; ihre Kindelemente
                            // (title, desc, ...) ignoriert die Schleife einfach.
                            if (channelId != null && channelFilter.test(channelId)) {
                                Programme p = readProgramme(r);
                                if (p != null) {
                                    programmes.add(p);
                                }
                            }
                        }
                        default -> { /* tv-Wurzelelement etc. */ }
                    }
                }
            }
            return new Result(channels, programmes);
        }
    }

    private Channel readChannel(XMLStreamReader r) throws XMLStreamException {
        String id = r.getAttributeValue(null, "id");
        String name = id;
        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.START_ELEMENT && r.getLocalName().equals("display-name")
                    && name.equals(id)) {
                name = r.getElementText().trim();
            } else if (ev == XMLStreamConstants.END_ELEMENT && r.getLocalName().equals("channel")) {
                break;
            }
        }
        return new Channel(id, name);
    }

    private Programme readProgramme(XMLStreamReader r) throws XMLStreamException {
        String channel = r.getAttributeValue(null, "channel");
        ZonedDateTime start = parseTime(r.getAttributeValue(null, "start"));
        ZonedDateTime stop = parseTime(r.getAttributeValue(null, "stop"));
        String title = null;
        String subtitle = null;
        String desc = null;
        List<String> categories = new ArrayList<>();

        while (r.hasNext()) {
            int ev = r.next();
            if (ev == XMLStreamConstants.START_ELEMENT) {
                switch (r.getLocalName()) {
                    case "title" -> { String t = r.getElementText().trim(); if (title == null) title = t; }
                    case "sub-title" -> { String t = r.getElementText().trim(); if (subtitle == null) subtitle = t; }
                    case "desc" -> { String t = r.getElementText().trim(); if (desc == null) desc = t; }
                    case "category" -> categories.add(r.getElementText().trim());
                    default -> { }
                }
            } else if (ev == XMLStreamConstants.END_ELEMENT && r.getLocalName().equals("programme")) {
                break;
            }
        }
        if (channel == null || start == null || title == null) {
            return null; // unbrauchbarer Eintrag, überspringen
        }
        return new Programme(channel, title, subtitle, desc, List.copyOf(categories), start, stop);
    }

    static ZonedDateTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        try {
            if (v.contains(" ")) {
                return ZonedDateTime.parse(v, WITH_ZONE);
            }
            // ohne Zeitzone: deutsche Zeit annehmen
            String padded = v.length() == 12 ? v + "00" : v;
            return LocalDateTime.parse(padded, WITHOUT_ZONE).atZone(DEFAULT_ZONE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Erkennt .gz-Dateien am Magic Number (0x1f 0x8b), unabhängig von der Dateiendung. */
    private static InputStream maybeGunzip(InputStream raw) throws IOException {
        BufferedInputStream in = new BufferedInputStream(raw);
        in.mark(2);
        int b1 = in.read();
        int b2 = in.read();
        in.reset();
        return (b1 == 0x1f && b2 == 0x8b) ? new GZIPInputStream(in) : in;
    }
}