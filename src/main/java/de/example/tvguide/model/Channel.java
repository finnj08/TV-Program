package de.example.tvguide.model;

/** Ein Sender aus der XMLTV-Datei (id z. B. "DasErste.de"). */
public record Channel(String id, String name) {
}
