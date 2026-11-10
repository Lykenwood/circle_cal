package com.example.ringcalendar;

import java.time.DayOfWeek;
import java.time.LocalDate;

/** National holiday schedules; ordinary weekends apply outside the published years. */
final class RestDayCalendar {
    // 国务院办公厅 2025、2026 年部分节假日安排：
    // https://www.forestry.gov.cn/c/www/szxx/594663.jhtml
    // https://www.beijing.gov.cn/cs/gncs/zcwj/202603/t20260327_4568275.html
    static boolean isRestDay(LocalDate date) {
        if (isMakeupWorkday(date)) return false;
        if (isOfficialHoliday(date)) return true;
        DayOfWeek weekday = date.getDayOfWeek();
        return weekday == DayOfWeek.SATURDAY || weekday == DayOfWeek.SUNDAY;
    }

    static boolean hasOfficialSchedule(int year) { return year == 2025 || year == 2026; }

    private static boolean isMakeupWorkday(LocalDate d) {
        String value = d.toString();
        if (d.getYear() == 2025) {
            return value.equals("2025-01-26") || value.equals("2025-02-08")
                    || value.equals("2025-04-27") || value.equals("2025-09-28")
                    || value.equals("2025-10-11");
        }
        if (d.getYear() == 2026) {
            return value.equals("2026-01-04") || value.equals("2026-02-14")
                    || value.equals("2026-02-28") || value.equals("2026-05-09")
                    || value.equals("2026-09-20") || value.equals("2026-10-10");
        }
        return false;
    }

    private static boolean isOfficialHoliday(LocalDate d) {
        int y = d.getYear();
        if (y == 2025) {
            return between(d, 1, 1, 1, 1) || between(d, 1, 28, 2, 4)
                    || between(d, 4, 4, 4, 6) || between(d, 5, 1, 5, 5)
                    || between(d, 5, 31, 6, 2) || between(d, 10, 1, 10, 8);
        }
        if (y == 2026) {
            return between(d, 1, 1, 1, 3) || between(d, 2, 15, 2, 23)
                    || between(d, 4, 4, 4, 6) || between(d, 5, 1, 5, 5)
                    || between(d, 6, 19, 6, 21) || between(d, 9, 25, 9, 27)
                    || between(d, 10, 1, 10, 7);
        }
        return false;
    }

    private static boolean between(LocalDate d, int startMonth, int startDay, int endMonth, int endDay) {
        LocalDate start = LocalDate.of(d.getYear(), startMonth, startDay);
        LocalDate end = LocalDate.of(d.getYear(), endMonth, endDay);
        return !d.isBefore(start) && !d.isAfter(end);
    }
}
