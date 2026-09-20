package io.github.renanbacheschi.aguiabranca.status;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/status")
public class StatusController {

    private final String applicationName;

    public StatusController(@Value("${spring.application.name}") String applicationName) {
        this.applicationName = applicationName;
    }

    @GetMapping
    public StatusResponse status() {
        return new StatusResponse(applicationName, "UP", Instant.now());
    }
}
