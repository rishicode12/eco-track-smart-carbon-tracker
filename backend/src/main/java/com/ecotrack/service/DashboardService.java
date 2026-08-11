package com.ecotrack.service;

import com.ecotrack.dto.DashboardResponse;

public interface DashboardService {
    DashboardResponse getDashboardData(Long userId);
}
