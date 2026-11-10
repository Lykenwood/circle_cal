package com.example.ringcalendar;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class EventStore {
    private static final String KEY_V1 = "events_v1", KEY_V2 = "events_v2";
    private final SharedPreferences prefs;
    private final List<CalendarEvent> events = new ArrayList<>();

    EventStore(Context context) {
        prefs = context.getSharedPreferences("ring_calendar", Context.MODE_PRIVATE);
        if (prefs.contains(KEY_V2)) {
            loadV2();
        } else if (prefs.contains(KEY_V1)) {
            migrateV1();
        } else {
            LocalDate today = LocalDate.now();
            events.add(new CalendarEvent(1, "晨间计划", today.atTime(8, 0), today.atTime(9, 0), 0));
            events.add(new CalendarEvent(2, "专注工作", today.atTime(10, 0), today.atTime(12, 0), 1));
            events.add(new CalendarEvent(3, "散步与休息", today.atTime(18, 0), today.atTime(19, 0), 2));
            events.add(new CalendarEvent(4, "跨天旅程", today.atTime(21, 0), today.plusDays(2).atTime(9, 0), 3));
            save();
        }
    }

    private void loadV2() {
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY_V2, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                events.add(new CalendarEvent(item.getLong("id"), item.getString("title"),
                        LocalDateTime.parse(item.getString("start")), LocalDateTime.parse(item.getString("end")), item.getInt("color")));
            }
        } catch (Exception ignored) { }
    }

    private void migrateV1() {
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY_V1, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                LocalDate date = LocalDate.parse(item.getString("date"));
                events.add(new CalendarEvent(item.getLong("id"), item.getString("title"),
                        date.atStartOfDay().plusMinutes(item.getInt("start")),
                        date.atStartOfDay().plusMinutes(item.getInt("end")), item.getInt("color")));
            }
            save(); // Keep v1 as a recovery copy; switch reads to v2 only after a successful write.
        } catch (Exception ignored) { }
    }

    List<CalendarEvent> forDate(LocalDate date) {
        List<CalendarEvent> result = new ArrayList<>();
        for (CalendarEvent event : events) if (event.intersects(date)) result.add(event);
        result.sort(Comparator.comparing(e -> e.start));
        return result;
    }

    List<CalendarEvent> forRange(LocalDate first, LocalDate afterLast) {
        List<CalendarEvent> result = new ArrayList<>();
        for (CalendarEvent event : events) {
            if (event.start.isBefore(afterLast.atStartOfDay()) && event.end.isAfter(first.atStartOfDay())) result.add(event);
        }
        result.sort(Comparator.comparing(e -> e.start));
        return result;
    }

    void put(CalendarEvent event) {
        events.removeIf(e -> e.id == event.id);
        events.add(event);
        save();
    }

    void remove(long id) { events.removeIf(e -> e.id == id); save(); }

    long nextId() {
        long max = 0;
        for (CalendarEvent event : events) max = Math.max(max, event.id);
        return max + 1;
    }

    private void save() {
        JSONArray array = new JSONArray();
        for (CalendarEvent event : events) {
            JSONObject item = new JSONObject();
            try {
                item.put("id", event.id);
                item.put("title", event.title);
                item.put("start", event.start.toString());
                item.put("end", event.end.toString());
                item.put("color", event.color);
                array.put(item);
            } catch (Exception ignored) { }
        }
        prefs.edit().putString(KEY_V2, array.toString()).apply();
    }
}
