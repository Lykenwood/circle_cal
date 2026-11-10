package com.example.ringcalendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int BG = 0xFFFFFFFF, INK = 0xFF202124, MUTED = 0xFF898F95, ACCENT = 0xFFFF135C;
    private static final String[] WEEKDAYS = {"一", "二", "三", "四", "五", "六", "日"};
    private static final String[] COLORS = {"暖橙", "蓝紫", "薄荷", "珊瑚"};
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA);
    private final Handler clock = new Handler();
    private EventStore store;
    private LocalDate selected = LocalDate.now();
    private RingCalendarView.Mode mode = RingCalendarView.Mode.MONTH;
    private TextView dateText, subtitle, todayButton, agendaCount, modeLabel;
    private TextView ringHint;
    private LinearLayout weekRow, agendaList, modeRow;
    private RingCalendarView ring;
    private final Runnable tick = new Runnable() {
        @Override public void run() { if (ring != null) ring.invalidate(); clock.postDelayed(this, 30_000); }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            selected = LocalDate.parse(savedInstanceState.getString("date", LocalDate.now().toString()));
            mode = RingCalendarView.Mode.valueOf(savedInstanceState.getString("mode", "MONTH"));
        }
        store = new EventStore(this);
        buildUi();
        refresh();
    }

    @Override protected void onStart() { super.onStart(); clock.post(tick); }
    @Override protected void onStop() { clock.removeCallbacks(tick); super.onStop(); }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("date", selected.toString());
        out.putString("mode", mode.name());
        super.onSaveInstanceState(out);
    }

    private void buildUi() {
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        scroll.setClipToPadding(false);
        LinearLayout body = column();
        body.setPadding(dp(12), dp(12), dp(12), dp(24));
        scroll.addView(body);
        setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            int top, bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                top = bars.top;
                bottom = bars.bottom;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            scroll.setPadding(0, top, 0, bottom);
            return insets;
        });
        scroll.requestApplyInsets();

        LinearLayout dateNav = row();
        dateNav.setGravity(Gravity.CENTER_VERTICAL);
        body.addView(dateNav, new LinearLayout.LayoutParams(-1, dp(62)));
        TextView previous = navigationButton("‹");
        dateNav.addView(previous);
        previous.setContentDescription("上一时段");
        previous.setOnClickListener(v -> { movePeriod(-1); refresh(); });
        LinearLayout titleColumn = column();
        titleColumn.setGravity(Gravity.CENTER);
        dateNav.addView(titleColumn, new LinearLayout.LayoutParams(0, -1, 1));
        modeLabel = text("月份", 18, INK, Typeface.NORMAL);
        modeLabel.setGravity(Gravity.CENTER);
        titleColumn.addView(modeLabel);
        dateText = text("", 11, MUTED, Typeface.NORMAL);
        dateText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(-1, -2);
        dateParams.topMargin = dp(4);
        titleColumn.addView(dateText, dateParams);
        dateText.setOnClickListener(v -> openDatePicker());
        dateText.setContentDescription("选择日期");
        TextView next = navigationButton("›");
        dateNav.addView(next);
        next.setContentDescription("下一时段");
        next.setOnClickListener(v -> { movePeriod(1); refresh(); });
        subtitle = text("", 12, MUTED, Typeface.NORMAL);
        todayButton = text("回到今天", 12, ACCENT, Typeface.NORMAL);
        todayButton.setGravity(Gravity.CENTER);
        body.addView(todayButton, new LinearLayout.LayoutParams(-1, dp(24)));
        todayButton.setOnClickListener(v -> { selected = LocalDate.now(); refresh(); });
        weekRow = row();
        LinearLayout ringCard = column();
        ringCard.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams ringParams = new LinearLayout.LayoutParams(-1, -2);
        ringParams.topMargin = dp(2);
        body.addView(ringCard, ringParams);
        ring = new RingCalendarView(this);
        ringCard.addView(ring, new LinearLayout.LayoutParams(-1, -2));
        ring.setOnEventClickListener(this::showEventEditor);
        ringHint = text("", 12, MUTED, Typeface.NORMAL);
        ringHint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, dp(20));
        ringCard.addView(ringHint, hintParams);
        ring.setOnDateClickListener(date -> { selected = date; refresh(); });

        modeRow = row();
        modeRow.setPadding(dp(4), dp(4), dp(4), dp(4));
        modeRow.setBackground(round(0xFFF5F5F5, 20));
        LinearLayout.LayoutParams modeParams = new LinearLayout.LayoutParams(-1, dp(46));
        modeParams.leftMargin = dp(70);
        modeParams.rightMargin = dp(70);
        modeParams.topMargin = dp(7);
        body.addView(modeRow, modeParams);

        LinearLayout agendaHeader = row();
        agendaHeader.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams agendaHeaderParams = new LinearLayout.LayoutParams(-1, -2);
        agendaHeaderParams.topMargin = dp(18);
        body.addView(agendaHeader, agendaHeaderParams);
        TextView agendaTitle = text("日程", 18, INK, Typeface.BOLD);
        agendaHeader.addView(agendaTitle);
        agendaCount = text("", 12, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(0, -2, 1);
        countParams.leftMargin = dp(10);
        agendaHeader.addView(agendaCount, countParams);
        TextView add = text("＋", 22, INK, Typeface.NORMAL);
        add.setGravity(Gravity.CENTER);
        add.setPadding(dp(17), dp(11), dp(17), dp(11));
        add.setBackground(round(0xFFF5F5F5, 22));
        add.setOnClickListener(v -> showEventEditor(null));
        agendaHeader.addView(add);
        agendaList = column();
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(-1, -2);
        listParams.topMargin = dp(14);
        body.addView(agendaList, listParams);
    }

    private void refresh() {
        List<CalendarEvent> events = store.forDate(selected);
        LocalDate first = mode == RingCalendarView.Mode.WEEK
                ? selected.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                : selected.withDayOfMonth(1);
        if (mode == RingCalendarView.Mode.DAY) dateText.setText(selected.format(dateFormat));
        else if (mode == RingCalendarView.Mode.WEEK) dateText.setText(first.getMonthValue() + "月" + first.getDayOfMonth() + "日 — "
                + first.plusDays(6).getMonthValue() + "月" + first.plusDays(6).getDayOfMonth() + "日");
        else dateText.setText(selected.getYear() + "年" + selected.getMonthValue() + "月");
        dateText.setTextColor(mode == RingCalendarView.Mode.DAY && RestDayCalendar.isRestDay(selected) ? 0xFFD32F2F : MUTED);
        modeLabel.setText(mode == RingCalendarView.Mode.DAY ? "天" : mode == RingCalendarView.Mode.WEEK ? "周" : "月份");
        subtitle.setText(mode == RingCalendarView.Mode.DAY ? "一天的时间脉络" :
                mode == RingCalendarView.Mode.WEEK ? "一周日程，一目了然" : "整月尽在眼前");
        todayButton.setVisibility(selected.equals(LocalDate.now()) ? View.INVISIBLE : View.VISIBLE);
        modeRow.removeAllViews();
        RingCalendarView.Mode[] modes = {RingCalendarView.Mode.DAY, RingCalendarView.Mode.WEEK, RingCalendarView.Mode.MONTH};
        String[] names = {"天", "周", "月"};
        for (int i = 0; i < modes.length; i++) {
            RingCalendarView.Mode option = modes[i];
            TextView tab = text(names[i], 14, option == mode ? Color.WHITE : MUTED, Typeface.BOLD);
            tab.setGravity(Gravity.CENTER);
            tab.setBackground(round(option == mode ? ACCENT : Color.TRANSPARENT, 17));
            modeRow.addView(tab, new LinearLayout.LayoutParams(0, -1, 1));
            tab.setOnClickListener(v -> { mode = option; refresh(); });
        }
        weekRow.setVisibility(mode == RingCalendarView.Mode.DAY ? View.VISIBLE : View.GONE);
        weekRow.removeAllViews();
        LocalDate monday = selected.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        for (int i = 0; i < 7; i++) {
            LocalDate date = monday.plusDays(i);
            LinearLayout item = column();
            item.setGravity(Gravity.CENTER);
            item.setPadding(0, dp(7), 0, dp(7));
            item.setBackground(round(date.equals(selected) ? ACCENT : Color.TRANSPARENT, 18));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(66), 1);
            if (i > 0) params.leftMargin = dp(2);
            weekRow.addView(item, params);
            TextView weekday = text(WEEKDAYS[i], 11, date.equals(selected) ? 0xFFDDE2FF : MUTED, Typeface.NORMAL);
            weekday.setGravity(Gravity.CENTER);
            item.addView(weekday);
            TextView day = text(String.valueOf(date.getDayOfMonth()), 17, date.equals(selected) ? Color.WHITE : INK, Typeface.BOLD);
            day.setGravity(Gravity.CENTER);
            item.addView(day);
            item.setOnClickListener(v -> { selected = date; refresh(); });
        }
        List<CalendarEvent> ringEvents = mode == RingCalendarView.Mode.DAY ? events :
                mode == RingCalendarView.Mode.WEEK ? store.forRange(first, first.plusDays(7)) :
                        store.forRange(first, first.plusMonths(1));
        ring.setData(selected, mode, ringEvents);
        ringHint.setText(!RestDayCalendar.hasOfficialSchedule(selected.getYear())
                ? "此年份尚无调休数据，仅按双休标红" :
                mode == RingCalendarView.Mode.DAY ? "点按彩色时段查看日程" :
                "红字为休息日 · 点按圆环选择日期");
        List<CalendarEvent> upcoming = store.forRange(selected, selected.plusDays(7));
        agendaCount.setText("未来7天 · " + upcoming.size() + " 项");
        agendaList.removeAllViews();
        if (upcoming.isEmpty()) {
            TextView empty = text("接下来 7 天没有日程\n点右上角添加第一项安排", 14, MUTED, Typeface.NORMAL);
            empty.setGravity(Gravity.CENTER);
            empty.setLineSpacing(dp(5), 1);
            empty.setBackground(round(0xFFF6F6F6, 20));
            agendaList.addView(empty, new LinearLayout.LayoutParams(-1, dp(112)));
        } else {
            for (int offset = 0; offset < 7; offset++) {
                LocalDate day = selected.plusDays(offset);
                List<CalendarEvent> dayEvents = store.forDate(day);
                if (dayEvents.isEmpty()) continue;
                TextView dayHeader = text(day.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)),
                        12, RestDayCalendar.isRestDay(day) ? 0xFFD32F2F : day.equals(selected) ? ACCENT : MUTED, Typeface.BOLD);
                dayHeader.setPadding(dp(6), dp(12), 0, dp(8));
                agendaList.addView(dayHeader);
                for (CalendarEvent event : dayEvents) agendaList.addView(eventRow(event));
            }
        }
    }

    private View eventRow(CalendarEvent event) {
        LinearLayout card = row();
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(round(0xFFF7F7F7, 16));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, dp(72));
        cardParams.bottomMargin = dp(9);
        card.setLayoutParams(cardParams);
        View stripe = new View(this);
        stripe.setBackground(round(RingCalendarView.eventColor(event.color), 3));
        card.addView(stripe, new LinearLayout.LayoutParams(dp(5), -1));
        LinearLayout content = column();
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(0, -2, 1);
        contentParams.leftMargin = dp(13);
        card.addView(content, contentParams);
        TextView title = text(event.title, 15, INK, Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        content.addView(title);
        String range = event.start.toLocalDate().equals(event.end.toLocalDate())
                ? formatTime(event.start) + " — " + formatTime(event.end)
                : formatDateTime(event.start) + " — " + formatDateTime(event.end);
        TextView time = text(range, 12, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(-1, -2);
        timeParams.topMargin = dp(4);
        content.addView(time, timeParams);
        TextView arrow = text("›", 25, 0xFFB1BACC, Typeface.NORMAL);
        card.addView(arrow);
        card.setOnClickListener(v -> showEventEditor(event));
        return card;
    }

    private void openDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, day) -> {
            selected = LocalDate.of(year, month + 1, day);
            refresh();
        }, selected.getYear(), selected.getMonthValue() - 1, selected.getDayOfMonth());
        dialog.show();
    }

    private void movePeriod(int amount) {
        if (mode == RingCalendarView.Mode.DAY) selected = selected.plusDays(amount);
        else if (mode == RingCalendarView.Mode.WEEK) selected = selected.plusWeeks(amount);
        else selected = selected.plusMonths(amount);
    }

    private void showEventEditor(CalendarEvent existing) {
        final LocalDate[] startDate = {existing == null ? selected : existing.start.toLocalDate()};
        final LocalDate[] endDate = {existing == null ? selected : existing.end.toLocalDate()};
        final int[] start = {existing == null ? 9 * 60 : existing.start.getHour() * 60 + existing.start.getMinute()};
        final int[] end = {existing == null ? 10 * 60 : existing.end.getHour() * 60 + existing.end.getMinute()};
        final int[] color = {existing == null ? 0 : existing.color};
        LinearLayout form = column();
        form.setPadding(dp(24), dp(8), dp(24), dp(2));
        EditText title = new EditText(this);
        title.setSingleLine(true);
        title.setTextSize(16);
        title.setHint("日程名称");
        if (existing != null) title.setText(existing.title);
        form.addView(title, new LinearLayout.LayoutParams(-1, dp(52)));
        TextView startDateButton = formButton("开始日期  " + formatDate(startDate[0]));
        form.addView(startDateButton);
        startDateButton.setOnClickListener(v -> chooseDate(startDate[0], date -> {
            startDate[0] = date; startDateButton.setText("开始日期  " + formatDate(date));
        }));
        TextView startButton = formButton("开始时间  " + formatTime(start[0]));
        form.addView(startButton);
        startButton.setOnClickListener(v -> chooseTime(start[0], minute -> {
            start[0] = minute; startButton.setText("开始时间  " + formatTime(minute));
        }));
        TextView endDateButton = formButton("结束日期  " + formatDate(endDate[0]));
        form.addView(endDateButton);
        endDateButton.setOnClickListener(v -> chooseDate(endDate[0], date -> {
            endDate[0] = date; endDateButton.setText("结束日期  " + formatDate(date));
        }));
        TextView endButton = formButton("结束时间  " + formatTime(end[0]));
        form.addView(endButton);
        endButton.setOnClickListener(v -> chooseTime(end[0], minute -> {
            end[0] = minute;
            if (endDate[0].equals(startDate[0]) && end[0] <= start[0]) {
                endDate[0] = startDate[0].plusDays(1);
                endDateButton.setText("结束日期  " + formatDate(endDate[0]));
            }
            endButton.setText("结束时间  " + formatTime(minute));
        }));
        TextView colorButton = formButton("颜色  " + COLORS[color[0]]);
        form.addView(colorButton);
        colorButton.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("选择颜色").setSingleChoiceItems(COLORS, color[0], (dialog, which) -> {
            color[0] = which; colorButton.setText("颜色  " + COLORS[which]); dialog.dismiss();
        }).show());
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "添加日程" : "编辑日程")
                .setView(form)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null);
        if (existing != null) builder.setNeutralButton("删除", (dialog, which) -> new AlertDialog.Builder(this)
                .setTitle("删除这项日程？").setNegativeButton("取消", null)
                .setPositiveButton("删除", (d, w) -> { store.remove(existing.id); refresh(); }).show());
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = title.getText().toString().trim();
            if (name.isEmpty()) { title.setError("请输入日程名称"); return; }
            LocalDateTime from = startDate[0].atStartOfDay().plusMinutes(start[0]);
            LocalDateTime until = endDate[0].atStartOfDay().plusMinutes(end[0]);
            if (!until.isAfter(from)) { Toast.makeText(this, "结束日期和时间要晚于开始", Toast.LENGTH_SHORT).show(); return; }
            store.put(new CalendarEvent(existing == null ? store.nextId() : existing.id,
                    name, from, until, color[0]));
            dialog.dismiss(); refresh();
        }));
        dialog.show();
    }

    private interface TimeChosen { void accept(int minutes); }
    private interface DateChosen { void accept(LocalDate date); }
    private void chooseDate(LocalDate current, DateChosen callback) {
        new DatePickerDialog(this, (picker, year, month, day) -> callback.accept(LocalDate.of(year, month + 1, day)),
                current.getYear(), current.getMonthValue() - 1, current.getDayOfMonth()).show();
    }
    private void chooseTime(int current, TimeChosen callback) {
        new TimePickerDialog(this, (picker, hour, minute) -> callback.accept(hour * 60 + minute), current / 60, current % 60, true).show();
    }
    private TextView formButton(String text) {
        TextView button = text(text, 15, INK, Typeface.NORMAL);
        button.setGravity(Gravity.CENTER_VERTICAL);
        button.setPadding(dp(2), 0, 0, 0);
        formButtonMargin(button);
        return button;
    }
    private void formButtonMargin(TextView button) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(52));
        params.topMargin = dp(4);
        button.setLayoutParams(params);
    }
    private TextView navigationButton(String value) {
        TextView button = text(value, 28, INK, Typeface.NORMAL);
        button.setGravity(Gravity.CENTER);
        button.setBackground(round(Color.WHITE, 14));
        button.setLayoutParams(new LinearLayout.LayoutParams(dp(42), dp(42)));
        return button;
    }
    private String formatTime(int minute) { return String.format(Locale.CHINA, "%02d:%02d", minute / 60, minute % 60); }
    private String formatTime(LocalDateTime time) { return formatTime(time.getHour() * 60 + time.getMinute()); }
    private String formatDate(LocalDate date) { return date.format(DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)); }
    private String formatDateTime(LocalDateTime time) { return time.getMonthValue() + "月" + time.getDayOfMonth() + "日 " + formatTime(time); }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }
    private TextView text(String value, int size, int color, int style) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(Typeface.create("sans-serif", style));
        t.setIncludeFontPadding(false);
        return t;
    }
    private GradientDrawable round(int color, float radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
}
