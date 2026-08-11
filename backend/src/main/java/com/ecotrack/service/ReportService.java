package com.ecotrack.service;

import com.ecotrack.dto.CategoryBreakdownResponse;
import com.ecotrack.dto.MonthlyTrendResponse;
import com.ecotrack.dto.ReportSummaryResponse;

import java.util.List;

public interface ReportService {
    ReportSummaryResponse getSummary(String userEmail);
    List<CategoryBreakdownResponse> getCategoryBreakdown(String userEmail);
    List<MonthlyTrendResponse> getMonthlyTrends(String userEmail);
}
