package com.pocketminder.transaction.analytics.controller;

import com.pocketminder.transaction.analytics.dto.CategorySummaryDTO;
import com.pocketminder.transaction.analytics.dto.FinancialSummaryDTO;
import com.pocketminder.transaction.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    public FinancialSummaryDTO getSummary(){
        return analyticsService.getFinancialSummary();
    }

    @GetMapping("/expenses/categories")
    public List<CategorySummaryDTO>
    getExpenseCategories() {

        return analyticsService
                .getExpenseCategoriesSummary();
    }
}
