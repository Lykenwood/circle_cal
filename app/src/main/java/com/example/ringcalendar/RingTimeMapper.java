package com.example.ringcalendar;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** Maps a real event interval onto one visible calendar revolution. */
final class RingTimeMapper {
    static final class Span {
        final float startAngle;
        final float sweepAngle;

        Span(float startAngle, float sweepAngle) {
            this.startAngle = startAngle;
            this.sweepAngle = sweepAngle;
        }
    }

    static Span map(CalendarEvent event, LocalDate first, int days) {
        LocalDateTime windowStart = first.atStartOfDay();
        LocalDateTime windowEnd = first.plusDays(days).atStartOfDay();
        LocalDateTime from = event.start.isBefore(windowStart) ? windowStart : event.start;
        LocalDateTime to = event.end.isAfter(windowEnd) ? windowEnd : event.end;
        if (!to.isAfter(from)) return null;
        double total = ChronoUnit.SECONDS.between(windowStart, windowEnd);
        float startAngle = (float)(-90 + ChronoUnit.SECONDS.between(windowStart, from) * 360 / total);
        float sweepAngle = (float)(ChronoUnit.SECONDS.between(from, to) * 360 / total);
        return new Span(startAngle, sweepAngle);
    }
}
