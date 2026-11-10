package com.example.ringcalendar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class RingCalendarView extends View {
    enum Mode { DAY, WEEK, MONTH }
    interface OnEventClickListener { void onEventClick(CalendarEvent event); }
    interface OnDateClickListener { void onDateClick(LocalDate date); }
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<CalendarEvent> events = new ArrayList<>();
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA);
    private LocalDate date = LocalDate.now();
    private Mode mode = Mode.MONTH;
    private OnEventClickListener eventListener;
    private OnDateClickListener dateListener;
    private float downX, downY;

    RingCalendarView(Context context) { super(context); }

    void setData(LocalDate date, Mode mode, List<CalendarEvent> events) {
        this.date = date;
        this.mode = mode;
        this.events.clear();
        this.events.addAll(events);
        invalidate();
    }
    void setOnEventClickListener(OnEventClickListener listener) { eventListener = listener; }
    void setOnDateClickListener(OnDateClickListener listener) { dateListener = listener; }
    static int eventColor(int index) { return new int[]{0xFFFFD32A, 0xFFFF5365, 0xFF18C8D5, 0xFF9C75EA}[Math.floorMod(index, 4)]; }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        setMeasuredDimension(width, Math.min(width, dp(370)));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float radius = Math.min(getWidth(), getHeight()) * .325f;
        if (mode == Mode.DAY) drawDay(canvas, cx, cy, radius);
        else drawCalendar(canvas, cx, cy, radius);
    }

    private void drawDay(Canvas canvas, float cx, float cy, float radius) {
        ring(canvas, cx, cy, radius, dp(30), 0xFFF2F3F5, -90, 360, false);
        // 06:00–18:00 is a fixed visual guide, not a calculated sunrise/sunset.
        ring(canvas, cx, cy, radius, dp(30), 0xFFFFF7E8, 0, 180, false);
        for (CalendarEvent item : events) {
            int first = item.firstMinuteOn(date), last = item.lastMinuteOn(date);
            if (last <= first) continue;
            float angle = -90 + first / 1440f * 360f;
            float sweep = Math.max(1.5f, (last - first) / 1440f * 360f - 2f);
            ring(canvas, cx, cy, radius, dp(20), eventColor(item.color), angle, sweep, true);
        }
        for (int hour = 0; hour < 24; hour++) {
            double angle = Math.toRadians(hour * 15 - 90);
            float inner = radius + dp(18), outer = radius + dp(hour % 6 == 0 ? 28 : 23);
            line(canvas, cx, cy, angle, inner, outer, hour % 6 == 0 ? 0xFF60666C : 0xFFB8BDC1, hour % 6 == 0 ? 2 : 1);
        }
        for (int hour = 0; hour < 24; hour++) {
            double angle = Math.toRadians(hour * 15 - 90);
            label(canvas, hour == 0 ? "24" : String.valueOf(hour),
                    cx + (float)Math.cos(angle) * (radius + dp(43)),
                    cy + (float)Math.sin(angle) * (radius + dp(43)) + dp(4), 13,
                    hour == LocalTime.now().getHour() && date.equals(LocalDate.now()) ? 0xFFFF135C : 0xFF202124, false);
        }
        if (date.equals(LocalDate.now())) {
            LocalTime now = LocalTime.now();
            double angle = Math.toRadians(-90 + (now.getHour() * 60 + now.getMinute()) / 1440d * 360);
            float x = cx + (float)Math.cos(angle) * radius, y = cy + (float)Math.sin(angle) * radius;
            circle(canvas, x, y, dp(9), 0xFFFFFFFF);
            circle(canvas, x, y, dp(5), 0xFF243253);
        }
        drawCenter(canvas, cx, cy);
    }

    private void drawCalendar(Canvas canvas, float cx, float cy, float radius) {
        LocalDate first = firstDate();
        int count = mode == Mode.WEEK ? 7 : first.lengthOfMonth();
        float sector = 360f / count;
        ring(canvas, cx, cy, radius, dp(mode == Mode.WEEK ? 42 : 30), 0xFFF6F6F6, -90, 360, false);
        int selectedIndex = (int)ChronoUnit.DAYS.between(first, date);
        int todayIndex = (int)ChronoUnit.DAYS.between(first, LocalDate.now());
        ring(canvas, cx, cy, radius - dp(mode == Mode.WEEK ? 27 : 21), dp(1.2f), 0xFF25282C,
                -90, 360, false);
        if (todayIndex >= 0 && todayIndex < count && selectedIndex != todayIndex) {
            int lo = Math.min(selectedIndex, todayIndex), hi = Math.max(selectedIndex, todayIndex);
            ring(canvas, cx, cy, radius - dp(mode == Mode.WEEK ? 27 : 21), dp(2), 0xFFFF135C,
                    -90 + (lo + .5f) * sector, (hi - lo) * sector, false);
        }
        for (int i = 0; i < count; i++) {
            double angle = Math.toRadians(-90 + (i + .5f) * sector);
            float labelRadius = radius + dp(mode == Mode.WEEK ? 33 : 27);
            LocalDate cell = first.plusDays(i);
            float lx = cx + (float)Math.cos(angle) * labelRadius;
            float ly = cy + (float)Math.sin(angle) * labelRadius;
            if (cell.equals(date)) circle(canvas, lx, ly, dp(mode == Mode.WEEK ? 16 : 12), 0xFFFF135C);
            int color = cell.equals(date) ? 0xFFFFFFFF :
                    RestDayCalendar.isRestDay(cell) ? 0xFFD32F2F : 0xFF202124;
            label(canvas, String.valueOf(cell.getDayOfMonth()),
                    lx, ly + dp(5), mode == Mode.WEEK ? 17 : 13, color, cell.equals(date));
            label(canvas, "一二三四五六日".substring(cell.getDayOfWeek().getValue() - 1, cell.getDayOfWeek().getValue()),
                    cx + (float)Math.cos(angle) * (radius - dp(mode == Mode.WEEK ? 3 : 2)),
                    cy + (float)Math.sin(angle) * (radius - dp(mode == Mode.WEEK ? 3 : 2)) + dp(3),
                    mode == Mode.WEEK ? 10 : 7, 0xFF92979D, false);
            if (mode == Mode.WEEK) {
                double borderAngle = Math.toRadians(-90 + i * sector);
                line(canvas, cx, cy, borderAngle, radius - dp(24), radius + dp(21), 0xFFFFFFFF, 2);
            }
            if (cell.equals(LocalDate.now())) {
                float dotRadius = radius + dp(mode == Mode.WEEK ? 53 : 43);
                circle(canvas, cx + (float)Math.cos(angle) * dotRadius,
                        cy + (float)Math.sin(angle) * dotRadius, dp(2.5f), 0xFFFF135C);
            }
        }
        // Calendar time is mapped continuously around the circle. A cross-midnight event
        // remains one arc, clipped only at the visible week or month boundary.
        int lane = 0;
        for (CalendarEvent item : events) {
            RingTimeMapper.Span span = RingTimeMapper.map(item, first, count);
            if (span == null) continue;
            float eventRadius = radius - dp((mode == Mode.WEEK ? 2 : 16) + (lane % 4) * (mode == Mode.WEEK ? 10 : 8));
            float startAngle = span.startAngle;
            float sweep = span.sweepAngle;
            if (sweep < 1.5f) {
                double a = Math.toRadians(startAngle + Math.max(sweep / 2, .5f));
                circle(canvas, cx + (float)Math.cos(a) * eventRadius,
                        cy + (float)Math.sin(a) * eventRadius, dp(mode == Mode.WEEK ? 3.5f : 2.5f), eventColor(item.color));
            } else {
                ring(canvas, cx, cy, eventRadius, dp(mode == Mode.WEEK ? 7 : 5), eventColor(item.color),
                        startAngle, Math.min(sweep, 359.9f), true);
            }
            lane++;
        }
        ring(canvas, cx, cy, radius - dp(mode == Mode.WEEK ? 82 : 66), dp(1), 0xFFDFE2E4, -90, 360, false);
        drawCenter(canvas, cx, cy);
    }

    private void drawCenter(Canvas canvas, float cx, float cy) {
        label(canvas, date.format(DateTimeFormatter.ofPattern("EEEE", Locale.CHINA)), cx, cy - dp(25), 12, 0xFF50565C, false);
        label(canvas, date.format(DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)), cx, cy + dp(2), 17,
                RestDayCalendar.isRestDay(date) ? 0xFFD32F2F : 0xFFFF135C, true);
        label(canvas, date.equals(LocalDate.now()) ? LocalTime.now().format(timeFormat) : "距今天 " + Math.abs(ChronoUnit.DAYS.between(LocalDate.now(), date)) + " 天",
                cx, cy + dp(29), 13, 0xFF25282C, false);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: downX = event.getX(); downY = event.getY(); return true;
            case MotionEvent.ACTION_UP:
                if (Math.hypot(event.getX() - downX, event.getY() - downY) > dp(12)) return true;
                float cx = getWidth() / 2f, cy = getHeight() / 2f;
                float radius = Math.min(getWidth(), getHeight()) * .325f;
                float distance = (float)Math.hypot(event.getX() - cx, event.getY() - cy);
                if (distance < radius - dp(88) || distance > radius + dp(62)) return true;
                double angle = (Math.toDegrees(Math.atan2(event.getY() - cy, event.getX() - cx)) + 450) % 360;
                if (mode == Mode.DAY) {
                    int minute = (int)(angle / 360 * 1440);
                    for (int i = events.size() - 1; i >= 0; i--) {
                        CalendarEvent item = events.get(i);
                        if (minute >= item.firstMinuteOn(date) && minute < item.lastMinuteOn(date)) {
                            if (eventListener != null) eventListener.onEventClick(item);
                            break;
                        }
                    }
                } else {
                    int count = mode == Mode.WEEK ? 7 : firstDate().lengthOfMonth();
                    int index = Math.min(count - 1, (int)(angle / 360 * count));
                    if (dateListener != null) dateListener.onDateClick(firstDate().plusDays(index));
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    private LocalDate firstDate() {
        return mode == Mode.WEEK ? date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) : date.withDayOfMonth(1);
    }
    private void ring(Canvas canvas, float cx, float cy, float radius, float width, int color, float start, float sweep, boolean rounded) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width);
        paint.setStrokeCap(rounded ? Paint.Cap.ROUND : Paint.Cap.BUTT);
        paint.setColor(color);
        canvas.drawArc(new RectF(cx - radius, cy - radius, cx + radius, cy + radius), start, sweep, false, paint);
        paint.setStyle(Paint.Style.FILL);
    }
    private void line(Canvas canvas, float cx, float cy, double angle, float r1, float r2, int color, float width) {
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(width)); paint.setColor(color);
        canvas.drawLine(cx + (float)Math.cos(angle) * r1, cy + (float)Math.sin(angle) * r1,
                cx + (float)Math.cos(angle) * r2, cy + (float)Math.sin(angle) * r2, paint);
        paint.setStyle(Paint.Style.FILL);
    }
    private void label(Canvas canvas, String value, float x, float y, float size, int color, boolean bold) {
        paint.setStyle(Paint.Style.FILL); paint.setColor(color); paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.create(bold ? "sans-serif-medium" : "sans-serif", 0));
        paint.setTextSize(dp(size)); canvas.drawText(value, x, y, paint);
    }
    private void circle(Canvas canvas, float x, float y, float radius, int color) {
        paint.setStyle(Paint.Style.FILL); paint.setColor(color); canvas.drawCircle(x, y, radius, paint);
    }
    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
}
