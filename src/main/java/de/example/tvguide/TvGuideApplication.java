package de.example.tvguide;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TvGuideApplication {

    public static void main(String[] args) {
        SpringApplication.run(TvGuideApplication.class, args);
    }
}
