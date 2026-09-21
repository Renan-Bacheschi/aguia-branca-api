package io.github.renanbacheschi.aguiabranca.project;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import io.github.renanbacheschi.aguiabranca.error.ConflictException;
import io.github.renanbacheschi.aguiabranca.error.InvalidRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectResultServiceTests {

    private static final Instant NOW = Instant.parse("2026-09-20T12:00:00Z");

    @Mock
    private ProjectResultRepository resultRepository;
    @Mock
    private ProjectService projectService;
    @Mock
    private MongoTemplate mongoTemplate;

    private ProjectResultService resultService;

    @BeforeEach
    void setUp() {
        resultService = new ProjectResultService(
                resultRepository, projectService, mongoTemplate, Clock.fixed(NOW, ZoneOffset.UTC));
        lenient().when(resultRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @ParameterizedTest
    @EnumSource(ResultNature.class)
    void financialForecastAndActualResultsAreRecordedFromAuthenticatedManager(ResultNature nature) {
        ProjectResultResponse response = resultService.create("project-1", new ProjectResultCreateRequest(
                ResultType.COST_SAVING, nature, "Saving", LocalDate.parse("2026-10-01"),
                LocalDate.parse("2026-10-31"), null, null, null, new BigDecimal("500.00")), "manager-1");

        assertThat(response.nature()).isEqualTo(nature);
        assertThat(response.financialAmount()).isEqualByComparingTo("500.00");
        assertThat(response.recordedByUserId()).isEqualTo("manager-1");
    }

    @Test
    void operationalResultRequiresCompleteMetricAndNoFinancialAmount() {
        ProjectResultCreateRequest invalid = new ProjectResultCreateRequest(
                ResultType.PRODUCTIVITY_GAIN, ResultNature.ACTUAL, "Gain",
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"),
                "items/hour", BigDecimal.TEN, null, null);

        assertThatThrownBy(() -> resultService.create("project-1", invalid, "manager-1"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void invalidPeriodAndNegativeFinancialAmountAreRejected() {
        ProjectResultCreateRequest invalidPeriod = new ProjectResultCreateRequest(
                ResultType.ADDITIONAL_REVENUE, ResultNature.ACTUAL, "Revenue",
                LocalDate.parse("2026-10-31"), LocalDate.parse("2026-10-01"),
                null, null, null, BigDecimal.ONE);
        ProjectResultCreateRequest negative = new ProjectResultCreateRequest(
                ResultType.ADDITIONAL_REVENUE, ResultNature.ACTUAL, "Revenue",
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"),
                null, null, null, new BigDecimal("-1"));

        assertThatThrownBy(() -> resultService.create("project-1", invalidPeriod, "manager-1"))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> resultService.create("project-1", negative, "manager-1"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void cancelledOrArchivedProjectRejectsNewResults() {
        when(projectService.requireResultMutationAllowed("project-1"))
                .thenThrow(new ConflictException("Projetos cancelados não aceitam resultados."));

        assertThatThrownBy(() -> resultService.create("project-1", new ProjectResultCreateRequest(
                ResultType.COST_SAVING, ResultNature.ACTUAL, "Saving",
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"),
                null, null, null, BigDecimal.TEN), "manager-1"))
                .isInstanceOf(ConflictException.class);
    }
}
