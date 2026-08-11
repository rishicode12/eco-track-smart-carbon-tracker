package com.ecotrack.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryBreakdownResponse {
    private String category;
    private Double co2Impact;
    private Double percentage;
}
