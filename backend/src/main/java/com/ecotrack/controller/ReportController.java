package com.ecotrack.controller;

import com.ecotrack.dto.ApiResponse;
import com.ecotrack.dto.CategoryBreakdownResponse;
import com.ecotrack.dto.MonthlyTrendResponse;
import com.ecotrack.dto.ReportSummaryResponse;
import com.ecotrack.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ReportSummaryResponse>> getSummary(
            Authentication authentication
    ) {
        String email = authentication.getName();
        ReportSummaryResponse summary = reportService.getSummary(email);
        return ResponseEntity.ok(new ApiResponse<>(true, "Report summary fetched", summary));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryBreakdownResponse>>> getCategories(
            Authentication authentication
    ) {
        String email = authentication.getName();
        List<CategoryBreakdownResponse> breakdown = reportService.getCategoryBreakdown(email);
        return ResponseEntity.ok(new ApiResponse<>(true, "Category breakdown fetched", breakdown));
    }

    @GetMapping("/trends")
    public ResponseEntity<ApiResponse<List<MonthlyTrendResponse>>> getTrends(
            Authentication authentication
    ) {
        String email = authentication.getName();
        List<MonthlyTrendResponse> trends = reportService.getMonthlyTrends(email);
        return ResponseEntity.ok(new ApiResponse<>(true, "Monthly trends fetched", trends));
    }
}
