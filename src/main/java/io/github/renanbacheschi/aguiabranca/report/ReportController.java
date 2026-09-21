package io.github.renanbacheschi.aguiabranca.report;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@PreAuthorize("hasRole('LEADER')")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    SummaryReportResponse getSummary() {
        return reportService.getSummaryReport();
    }

    @GetMapping("/strategies/{strategyId}")
    StrategyReportResponse getStrategy(@PathVariable String strategyId) {
        return reportService.getStrategyReport(strategyId);
    }

    @GetMapping("/projects/{projectId}")
    ProjectReportResponse getProject(@PathVariable String projectId) {
        return reportService.getProjectReport(projectId);
    }
}
