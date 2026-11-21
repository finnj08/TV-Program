package de.example.tvguide;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProgrammeControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void channelsAreListed() throws Exception {
        mvc.perform(get("/api/channels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Sender A"));
    }

    @Test
    void dayView() throws Exception {
        mvc.perform(get("/api/programme").param("date", "2026-11-21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].title").value("Beispiel-Nachrichten"));
    }

    @Test
    void dayViewFilteredByChannel() throws Exception {
        mvc.perform(get("/api/programme").param("date", "2026-11-21").param("channel", "Sender-B.de"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Beispiel-Quiz"));
    }

    @Test
    void weekView() throws Exception {
        mvc.perform(get("/api/programme/week").param("from", "2026-11-21").param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['2026-11-21'].length()").value(3))
                .andExpect(jsonPath("$['2026-11-22'].length()").value(1))
                .andExpect(jsonPath("$['2026-11-24'].length()").value(1));
    }

    @Test
    void weekViewRespectsRange() throws Exception {
        mvc.perform(get("/api/programme/week").param("from", "2026-11-22").param("days", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['2026-11-21']").doesNotExist())
                .andExpect(jsonPath("$['2026-11-22'].length()").value(1));
    }
}
