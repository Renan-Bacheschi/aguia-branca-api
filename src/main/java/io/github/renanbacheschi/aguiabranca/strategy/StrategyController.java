package io.github.renanbacheschi.aguiabranca.strategy;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.renanbacheschi.aguiabranca.auth.AuthenticatedUserContext;
import io.github.renanbacheschi.aguiabranca.common.PageResponse;
import io.github.renanbacheschi.aguiabranca.error.InvalidRequestException;

@RestController
@RequestMapping("/api/v1/strategies")
public class StrategyController {

    private final StrategyService strategyService;

    public StrategyController(StrategyService strategyService) {
        this.strategyService = strategyService;
    }

    @PostMapping
    @PreAuthorize("hasRole('LEADER')")
    ResponseEntity<StrategyResponse> create(
            @Valid @RequestBody StrategyCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        StrategyResponse response = strategyService.create(request, AuthenticatedUserContext.from(jwt).userId());
        return ResponseEntity.status(HttpStatus.CREATED).eTag(etag(response.revision())).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OPERATOR','MANAGER','LEADER')")
    PageResponse<StrategyResponse> list(
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return strategyService.list(active, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATOR','MANAGER','LEADER')")
    ResponseEntity<StrategyResponse> get(@PathVariable String id) {
        StrategyResponse response = strategyService.get(id);
        return ResponseEntity.ok().eTag(etag(response.revision())).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LEADER')")
    ResponseEntity<StrategyResponse> update(
            @PathVariable String id,
            @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
            @Valid @RequestBody StrategyUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        StrategyResponse response = strategyService.update(
                id, parseRevision(ifMatch), request, AuthenticatedUserContext.from(jwt).userId());
        return ResponseEntity.ok().eTag(etag(response.revision())).body(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('LEADER')")
    ResponseEntity<Void> archive(
            @PathVariable String id,
            @RequestHeader(HttpHeaders.IF_MATCH) String ifMatch,
            @AuthenticationPrincipal Jwt jwt) {
        strategyService.archive(id, parseRevision(ifMatch), AuthenticatedUserContext.from(jwt).userId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('OPERATOR','MANAGER','LEADER')")
    List<StrategyHistoryResponse> history(@PathVariable String id) {
        return strategyService.history(id);
    }

    private long parseRevision(String ifMatch) {
        if (ifMatch == null || !ifMatch.matches("\\\"[1-9][0-9]*\\\"")) {
            throw new InvalidRequestException("O cabeçalho If-Match deve conter a revisão entre aspas.");
        }
        try {
            return Long.parseLong(ifMatch.substring(1, ifMatch.length() - 1));
        } catch (NumberFormatException exception) {
            throw new InvalidRequestException("A revisão informada no If-Match é inválida.");
        }
    }

    private String etag(long revision) {
        return "\"" + revision + "\"";
    }
}
