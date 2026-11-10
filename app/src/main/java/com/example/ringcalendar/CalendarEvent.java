package com.example.ringcalendar;

import java.time.LocalDate;
import java.time.LocalDateTime;

final class CalendarEvent {
    final long id;
    final String title;
    final LocalDateTime start;
    final LocalDateTime end;
    final int color;

    CalendarEvent(long id, String title, LocalDateTime start, LocalDateTime end, int color) {
        if (!end.isAfter(start)) throw new IllegalArgumentException("结束时间必须晚于开始时间");
        this.id = id;
        this.title = title;
        this.start = start;
        this.end = end;
        this.color = color;
    }

    boolean intersects(LocalDate date) {
        return start.isBefore(date.plusDays(1).atStartOfDay()) && end.isAfter(date.atStartOfDay());
    }

    int firstMinuteOn(LocalDate date) {
        return start.toLocalDate().isBefore(date) ? 0 : start.getHour() * 60 + start.getMinute();
    }

    int lastMinuteOn(LocalDate date) {
        return end.toLocalDate().isAfter(date) ? 1440 : end.getHour() * 60 + end.getMinute();
    }
}
