package io.github.renanbacheschi.aguiabranca.status;

import java.time.Instant;

public record StatusResponse(String application, String status, Instant timestamp) {
}
