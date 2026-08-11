package com.ecotrack.service.impl;

import com.ecotrack.dto.CategoryBreakdownResponse;
import com.ecotrack.dto.MonthlyTrendResponse;
import com.ecotrack.dto.ReportSummaryResponse;
import com.ecotrack.exception.ResourceNotFoundException;
import com.ecotrack.repository.CarbonEmissionRepository;
import com.ecotrack.repository.UserRepository;
import com.ecotrack.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final CarbonEmissionRepository carbonEmissionRepository;
    private final UserRepository userRepository;

    @Override
    public ReportSummaryResponse getSummary(String userEmail) {
        Long userId = getUserIdByEmail(userEmail);

        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate startOfYear = today.withDayOfYear(1);
        LocalDate startOfPrevMonth = startOfMonth.minusMonths(1);
        LocalDate endOfPrevMonth = startOfMonth.minusDays(1);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime startOfTodayTime = today.atTime(LocalTime.MIN);
        LocalDateTime startOfWeekDateTime = startOfWeek.atStartOfDay();
        LocalDateTime startOfMonthDateTime = startOfMonth.atStartOfDay();
        LocalDateTime startOfYearDateTime = startOfYear.atStartOfDay();
        LocalDateTime startOfPrevMonthDateTime = startOfPrevMonth.atStartOfDay();
        LocalDateTime endOfPrevMonthDateTime = endOfPrevMonth.atTime(LocalTime.MAX);

        BigDecimal todayCarbon = carbonEmissionRepository.sumEmissionBetween(
                userId, startOfTodayTime, now);
        BigDecimal weeklyCarbon = carbonEmissionRepository.sumEmissionBetween(
                userId, startOfWeekDateTime, now);
        BigDecimal monthlyCarbon = carbonEmissionRepository.sumEmissionBetween(
                userId, startOfMonthDateTime, now);
        BigDecimal yearlyCarbon = carbonEmissionRepository.sumEmissionBetween(
                userId, startOfYearDateTime, now);

        BigDecimal prevMonthCarbon = carbonEmissionRepository.sumEmissionBetween(
                userId, startOfPrevMonthDateTime, endOfPrevMonthDateTime);

        double monthly = monthlyCarbon.doubleValue();
        int daysInMonth = today.lengthOfMonth();
        double avgDaily = daysInMonth > 0 ? monthly / daysInMonth : 0.0;

        double momReduction;
        if (prevMonthCarbon.doubleValue() > 0) {
            momReduction = ((prevMonthCarbon.doubleValue() - monthly) / prevMonthCarbon.doubleValue()) * 100;
        } else {
            momReduction = 0.0;
        }

        return new ReportSummaryResponse(
                todayCarbon.doubleValue(),
                weeklyCarbon.doubleValue(),
                monthly,
                yearlyCarbon.doubleValue(),
                Math.round(avgDaily * 100.0) / 100.0,
                Math.round(momReduction * 100.0) / 100.0
        );
    }

    @Override
    public List<CategoryBreakdownResponse> getCategoryBreakdown(String userEmail) {
        Long userId = getUserIdByEmail(userEmail);

        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDateTime startOfMonthDateTime = startOfMonth.atStartOfDay();

        BigDecimal transport = carbonEmissionRepository.sumTransportationEmission(userId, startOfMonthDateTime);
        BigDecimal electricity = carbonEmissionRepository.sumElectricityEmission(userId, startOfMonthDateTime);
        BigDecimal food = carbonEmissionRepository.sumFoodEmission(userId, startOfMonthDateTime);
        BigDecimal waste = carbonEmissionRepository.sumWasteEmission(userId, startOfMonthDateTime);

        double t = transport.doubleValue();
        double e = electricity.doubleValue();
        double f = food.doubleValue();
        double w = waste.doubleValue();
        double total = t + e + f + w;

        List<CategoryBreakdownResponse> breakdown = new ArrayList<>();

        breakdown.add(new CategoryBreakdownResponse("Transport", t, total > 0 ? Math.round((t / total) * 10000.0) / 100.0 : 0.0));
        breakdown.add(new CategoryBreakdownResponse("Energy", e, total > 0 ? Math.round((e / total) * 10000.0) / 100.0 : 0.0));
        breakdown.add(new CategoryBreakdownResponse("Food", f, total > 0 ? Math.round((f / total) * 10000.0) / 100.0 : 0.0));
        breakdown.add(new CategoryBreakdownResponse("Waste", w, total > 0 ? Math.round((w / total) * 10000.0) / 100.0 : 0.0));

        return breakdown;
    }

    @Override
    public List<MonthlyTrendResponse> getMonthlyTrends(String userEmail) {
        Long userId = getUserIdByEmail(userEmail);

        LocalDate twelveMonthsAgo = LocalDate.now().minusMonths(12).withDayOfMonth(1);
        LocalDateTime startDate = twelveMonthsAgo.atStartOfDay();

        List<String> monthKeys = carbonEmissionRepository.findDistinctMonthKeys(userId, startDate);

        if (monthKeys.isEmpty()) {
            return Collections.emptyList();
        }

        List<MonthlyTrendResponse> trends = new ArrayList<>();
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("MMM yyyy");

        for (String monthKey : monthKeys) {
            LocalDate monthDate = LocalDate.parse(monthKey + "-01", inputFormatter);
            String monthLabel = monthDate.format(outputFormatter);

            BigDecimal total = carbonEmissionRepository.sumEmissionByMonthKey(userId, monthKey);
            trends.add(new MonthlyTrendResponse(monthLabel, total.doubleValue()));
        }

        return trends;
    }

    private Long getUserIdByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
    }
}
