package edu.uet.library_management.infrastructure.scheduler;

import edu.uet.library_management.domain.service.BookLoanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookLoanScheduler {

    private final BookLoanService bookLoanService;

    // Run automatically every 5 minutes (300,000 milliseconds)
    @Scheduled(fixedRate = 300000)
    public void autoRefreshLoans() {
        log.info("[Scheduler] Starting automatic refresh of book loans status...");
        try {
            boolean success = bookLoanService.refreshDatabase();
            if (success) {
                log.info("[Scheduler] Automatic refresh of book loans status completed successfully.");
            } else {
                log.warn("[Scheduler] Automatic refresh of book loans status failed.");
            }
        } catch (Exception e) {
            log.error("[Scheduler] Error occurred during book loans auto refresh: ", e);
        }
    }
}
