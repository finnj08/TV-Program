package de.example.tvguide.web;

import de.example.tvguide.model.Channel;
import de.example.tvguide.model.Programme;
import de.example.tvguide.service.EpgService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProgrammeController {

    private final EpgService epg;

    public ProgrammeController(EpgService epg) {
        this.epg = epg;
    }

    /** GET /api/channels */
    @GetMapping("/channels")
    public List<Channel> channels() {
        return epg.channels();
    }

    /** GET /api/programme?date=2026-11-21&channel=DasErste.de  (beide Parameter optional) */
    @GetMapping("/programme")
    public List<Programme> day(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String channel) {
        LocalDate d = date != null ? date : LocalDate.now(EpgService.ZONE);
        return epg.forDay(d, channel);
    }

    /** GET /api/programme/week?from=2026-11-21&days=7&channel=ZDF.de */
    @GetMapping("/programme/week")
    public Map<LocalDate, List<Programme>> week(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(required = false) String channel) {
        LocalDate start = from != null ? from : LocalDate.now(EpgService.ZONE);
        return epg.forRange(start, Math.min(Math.max(days, 1), 14), channel);
    }

    /** GET /api/programme/now */
    @GetMapping("/programme/now")
    public List<Programme> now() {
        return epg.now();
    }
}
