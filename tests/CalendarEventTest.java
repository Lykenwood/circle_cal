package com.example.ringcalendar;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class CalendarEventTest {
    public static void main(String[] args) {
        CalendarEvent overnight = new CalendarEvent(1, "跨午夜", LocalDateTime.of(2026, 11, 30, 23, 0),
                LocalDateTime.of(2026, 12, 1, 2, 0), 0);
        check(overnight.intersects(LocalDate.of(2026, 11, 30)), "开始日应显示");
        check(overnight.intersects(LocalDate.of(2026, 12, 1)), "次日应显示");
        check(!overnight.intersects(LocalDate.of(2026, 12, 2)), "结束后不应显示");
        check(overnight.firstMinuteOn(LocalDate.of(2026, 11, 30)) == 1380, "开始日裁剪错误");
        check(overnight.lastMinuteOn(LocalDate.of(2026, 11, 30)) == 1440, "开始日终点错误");
        check(overnight.firstMinuteOn(LocalDate.of(2026, 12, 1)) == 0, "次日起点错误");
        check(overnight.lastMinuteOn(LocalDate.of(2026, 12, 1)) == 120, "次日终点错误");
        CalendarEvent midnightEnd = new CalendarEvent(2, "整天", LocalDateTime.of(2026, 11, 1, 0, 0),
                LocalDateTime.of(2026, 11, 2, 0, 0), 1);
        check(!midnightEnd.intersects(LocalDate.of(2026, 11, 2)), "午夜结束不应占用次日");
        RingTimeMapper.Span november = RingTimeMapper.map(overnight, LocalDate.of(2026, 11, 1), 30);
        RingTimeMapper.Span december = RingTimeMapper.map(overnight, LocalDate.of(2026, 12, 1), 31);
        check(november != null && november.startAngle > 269 && november.startAngle < 270,
                "跨月日程应从 11 月末真实时间开始");
        check(november.startAngle + november.sweepAngle == 270f, "11 月弧线应在月末边界结束");
        check(december != null && december.startAngle == -90f && december.sweepAngle > 0,
                "12 月弧线应从月初边界延续");
        RingTimeMapper.Span absent = RingTimeMapper.map(overnight, LocalDate.of(2026, 10, 1), 31);
        check(absent == null, "不在可见月份内的日程不应绘制");
        check(RestDayCalendar.isRestDay(LocalDate.of(2026, 5, 4)), "劳动节调休的周一应标红");
        check(RestDayCalendar.isRestDay(LocalDate.of(2026, 11, 7)), "普通周六应标红");
        check(!RestDayCalendar.isRestDay(LocalDate.of(2026, 1, 4)), "元旦调休周日应正常显示");
        check(!RestDayCalendar.isRestDay(LocalDate.of(2026, 2, 14)), "春节调休周六应正常显示");
        check(!RestDayCalendar.isRestDay(LocalDate.of(2026, 9, 20)), "国庆调休周日应正常显示");
        check(!RestDayCalendar.isRestDay(LocalDate.of(2025, 10, 11)), "2025 年国庆调休周六应正常显示");
        System.out.println("CalendarEvent cross-day boundaries OK");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
