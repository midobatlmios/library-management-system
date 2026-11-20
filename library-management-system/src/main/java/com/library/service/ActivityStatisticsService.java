package com.library.service;

import com.library.repository.BookLoanRepository;
import com.library.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Day-by-day activity for the admin dashboard's charts: loans started, books returned,
 * and money collected (successful payments), one row per day with zeros for quiet days.
 */
@Service
@RequiredArgsConstructor
public class ActivityStatisticsService {

    public static final int MAX_DAYS = 365;

    private final BookLoanRepository loanRepository;
    private final PaymentRepository paymentRepository;

    @Value("${app.default-currency:MAD}")
    private String currency;

    public record DayActivity(LocalDate date, long checkouts, long returns, BigDecimal collected) {
    }

    public record DailyActivityResponse(LocalDate from, LocalDate to, String currency, List<DayActivity> days) {
    }

    /** The last `days` days, today included. */
    @Transactional(readOnly = true)
    public DailyActivityResponse daily(int days) {
        int n = Math.max(1, Math.min(days, MAX_DAYS));
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(n - 1L);

        Map<LocalDate, Long> checkouts = toMap(loanRepository.countCheckoutsByDay(from));
        Map<LocalDate, Long> returns = toMap(loanRepository.countReturnsByDay(from));
        Map<LocalDate, Long> collectedMinor = new HashMap<>();
        for (Object[] row : paymentRepository.findSucceededSince(from.atStartOfDay())) {
            LocalDate day = ((LocalDateTime) row[0]).toLocalDate();
            collectedMinor.merge(day, ((Number) row[1]).longValue(), Long::sum);
        }

        List<DayActivity> result = new ArrayList<>(n);
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            result.add(new DayActivity(d,
                    checkouts.getOrDefault(d, 0L),
                    returns.getOrDefault(d, 0L),
                    BigDecimal.valueOf(collectedMinor.getOrDefault(d, 0L), 2)));   // centimes -> MAD
        }
        return new DailyActivityResponse(from, to, currency, result);
    }

    private static Map<LocalDate, Long> toMap(List<Object[]> rows) {
        Map<LocalDate, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null) {
                map.put((LocalDate) row[0], ((Number) row[1]).longValue());
            }
        }
        return map;
    }
}
