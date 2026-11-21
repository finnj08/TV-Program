package de.example.tvguide.model;

import java.time.ZonedDateTime;
import java.util.List;

/** Eine einzelne Sendung. */
public record Programme(
        String channelId,
        String title,
        String subtitle,
        String description,
        List<String> categories,
        ZonedDateTime start,
        ZonedDateTime stop) {
}
