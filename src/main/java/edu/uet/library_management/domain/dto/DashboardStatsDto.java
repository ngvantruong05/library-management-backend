package edu.uet.library_management.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    private long totalBooks;
    private long activeBooks;
    private long totalUsers;
    private long totalCategories;
    private List<TopLentBookDto> topLentBooks;
    private List<BookLoanDto> recentLoans;
}
