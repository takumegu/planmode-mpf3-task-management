package com.taskmanagement.util;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Service for calculating working days based on configurable working days and holidays.
 * Default configuration: Monday-Friday are working days, no holidays.
 */
@Component
public class WorkingDayCalculator {

    private Set<DayOfWeek> workingDays;
    private Set<LocalDate> holidays;

    public WorkingDayCalculator() {
        // Default: Monday-Friday are working days
        this.workingDays = EnumSet.of(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        );
        this.holidays = new HashSet<>();
    }

    /**
     * Set which days of the week are working days
     */
    public void setWorkingDays(Set<DayOfWeek> workingDays) {
        if (workingDays == null || workingDays.isEmpty()) {
            throw new IllegalArgumentException("At least one working day must be specified");
        }
        this.workingDays = EnumSet.copyOf(workingDays);
    }

    /**
     * Set holidays (non-working days)
     */
    public void setHolidays(Set<LocalDate> holidays) {
        this.holidays = holidays != null ? new HashSet<>(holidays) : new HashSet<>();
    }

    /**
     * Add a single holiday
     */
    public void addHoliday(LocalDate date) {
        if (date != null) {
            this.holidays.add(date);
        }
    }

    /**
     * Check if a given date is a working day
     */
    public boolean isWorkingDay(LocalDate date) {
        if (date == null) {
            return false;
        }
        return workingDays.contains(date.getDayOfWeek()) && !holidays.contains(date);
    }

    /**
     * Get the next working day after the given date (exclusive)
     */
    public LocalDate nextWorkingDay(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }

        LocalDate next = date.plusDays(1);
        while (!isWorkingDay(next)) {
            next = next.plusDays(1);
            // Safety check to prevent infinite loops
            if (next.isAfter(date.plusYears(1))) {
                throw new IllegalStateException("Could not find a working day within one year");
            }
        }
        return next;
    }

    /**
     * Get the previous working day before the given date (exclusive)
     */
    public LocalDate previousWorkingDay(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }

        LocalDate prev = date.minusDays(1);
        while (!isWorkingDay(prev)) {
            prev = prev.minusDays(1);
            // Safety check to prevent infinite loops
            if (prev.isBefore(date.minusYears(1))) {
                throw new IllegalStateException("Could not find a working day within one year");
            }
        }
        return prev;
    }

    /**
     * Add N working days to the given date
     * If the start date is not a working day, starts counting from the next working day
     */
    public LocalDate addWorkingDays(LocalDate start, int days) {
        if (start == null) {
            throw new IllegalArgumentException("Start date cannot be null");
        }
        if (days < 0) {
            throw new IllegalArgumentException("Days must be non-negative");
        }
        if (days == 0) {
            return start;
        }

        LocalDate result = isWorkingDay(start) ? start : nextWorkingDay(start);
        int added = 0;

        while (added < days) {
            result = nextWorkingDay(result);
            added++;
        }

        return result;
    }

    /**
     * Count the number of working days between two dates (inclusive)
     */
    public int countWorkingDays(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end dates cannot be null");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start date must not be after end date");
        }

        int count = 0;
        LocalDate current = start;

        while (!current.isAfter(end)) {
            if (isWorkingDay(current)) {
                count++;
            }
            current = current.plusDays(1);
        }

        return count;
    }

    /**
     * Adjust a date to the nearest working day
     * If the date is a working day, returns it unchanged
     * Otherwise, returns the next working day
     */
    public LocalDate adjustToWorkingDay(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        return isWorkingDay(date) ? date : nextWorkingDay(date);
    }

    // Getters for current configuration
    public Set<DayOfWeek> getWorkingDays() {
        return EnumSet.copyOf(workingDays);
    }

    public Set<LocalDate> getHolidays() {
        return new HashSet<>(holidays);
    }
}
