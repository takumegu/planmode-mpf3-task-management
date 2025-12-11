package com.taskmanagement.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WorkingDayCalculatorTest {

    private WorkingDayCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new WorkingDayCalculator();
    }

    @Test
    void testDefaultWorkingDays() {
        // Monday-Friday should be working days by default
        assertTrue(calculator.isWorkingDay(LocalDate.of(2025, 12, 1))); // Monday
        assertTrue(calculator.isWorkingDay(LocalDate.of(2025, 12, 2))); // Tuesday
        assertTrue(calculator.isWorkingDay(LocalDate.of(2025, 12, 3))); // Wednesday
        assertTrue(calculator.isWorkingDay(LocalDate.of(2025, 12, 4))); // Thursday
        assertTrue(calculator.isWorkingDay(LocalDate.of(2025, 12, 5))); // Friday

        // Saturday and Sunday should not be working days
        assertFalse(calculator.isWorkingDay(LocalDate.of(2025, 12, 6))); // Saturday
        assertFalse(calculator.isWorkingDay(LocalDate.of(2025, 12, 7))); // Sunday
    }

    @Test
    void testCustomWorkingDays() {
        // Set working days to Monday-Thursday only
        calculator.setWorkingDays(EnumSet.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY
        ));

        assertTrue(calculator.isWorkingDay(LocalDate.of(2025, 12, 1))); // Monday
        assertFalse(calculator.isWorkingDay(LocalDate.of(2025, 12, 5))); // Friday
    }

    @Test
    void testHolidays() {
        LocalDate holiday = LocalDate.of(2025, 12, 25); // Christmas (Thursday)
        calculator.addHoliday(holiday);

        // Thursday is normally a working day, but this one is a holiday
        assertFalse(calculator.isWorkingDay(holiday));
    }

    @Test
    void testNextWorkingDay() {
        // Friday -> next working day is Monday
        LocalDate friday = LocalDate.of(2025, 12, 5);
        LocalDate monday = LocalDate.of(2025, 12, 8);
        assertEquals(monday, calculator.nextWorkingDay(friday));

        // Monday -> next working day is Tuesday
        LocalDate tuesday = LocalDate.of(2025, 12, 9);
        assertEquals(tuesday, calculator.nextWorkingDay(monday));
    }

    @Test
    void testNextWorkingDayWithHoliday() {
        LocalDate monday = LocalDate.of(2025, 12, 8);
        calculator.addHoliday(LocalDate.of(2025, 12, 9)); // Tuesday is holiday

        // Next working day after Monday should be Wednesday (skipping holiday Tuesday)
        LocalDate wednesday = LocalDate.of(2025, 12, 10);
        assertEquals(wednesday, calculator.nextWorkingDay(monday));
    }

    @Test
    void testPreviousWorkingDay() {
        // Monday -> previous working day is Friday
        LocalDate monday = LocalDate.of(2025, 12, 8);
        LocalDate friday = LocalDate.of(2025, 12, 5);
        assertEquals(friday, calculator.previousWorkingDay(monday));
    }

    @Test
    void testAddWorkingDays() {
        LocalDate monday = LocalDate.of(2025, 12, 1);

        // Add 0 days
        assertEquals(monday, calculator.addWorkingDays(monday, 0));

        // Add 1 working day: Monday + 1 = Tuesday
        assertEquals(LocalDate.of(2025, 12, 2), calculator.addWorkingDays(monday, 1));

        // Add 5 working days: Monday + 5 = Monday next week
        assertEquals(LocalDate.of(2025, 12, 8), calculator.addWorkingDays(monday, 5));
    }

    @Test
    void testAddWorkingDaysStartingFromNonWorkingDay() {
        LocalDate saturday = LocalDate.of(2025, 12, 6);

        // Adding working days from Saturday should start counting from Monday
        assertEquals(LocalDate.of(2025, 12, 9), calculator.addWorkingDays(saturday, 1));
    }

    @Test
    void testCountWorkingDays() {
        LocalDate start = LocalDate.of(2025, 12, 1); // Monday
        LocalDate end = LocalDate.of(2025, 12, 5); // Friday

        // Monday-Friday inclusive = 5 working days
        assertEquals(5, calculator.countWorkingDays(start, end));
    }

    @Test
    void testCountWorkingDaysIncludingWeekend() {
        LocalDate start = LocalDate.of(2025, 12, 1); // Monday
        LocalDate end = LocalDate.of(2025, 12, 7); // Sunday

        // Monday-Sunday = 5 working days (Mon-Fri only)
        assertEquals(5, calculator.countWorkingDays(start, end));
    }

    @Test
    void testCountWorkingDaysWithHoliday() {
        LocalDate start = LocalDate.of(2025, 12, 1); // Monday
        LocalDate end = LocalDate.of(2025, 12, 5); // Friday

        calculator.addHoliday(LocalDate.of(2025, 12, 3)); // Wednesday is holiday

        // Monday-Friday minus Wednesday = 4 working days
        assertEquals(4, calculator.countWorkingDays(start, end));
    }

    @Test
    void testAdjustToWorkingDay() {
        // Monday is already a working day
        LocalDate monday = LocalDate.of(2025, 12, 1);
        assertEquals(monday, calculator.adjustToWorkingDay(monday));

        // Saturday should be adjusted to Monday
        LocalDate saturday = LocalDate.of(2025, 12, 6);
        LocalDate nextMonday = LocalDate.of(2025, 12, 8);
        assertEquals(nextMonday, calculator.adjustToWorkingDay(saturday));
    }

    @Test
    void testNullDateThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> calculator.isWorkingDay(null));
        assertThrows(IllegalArgumentException.class, () -> calculator.nextWorkingDay(null));
        assertThrows(IllegalArgumentException.class, () -> calculator.previousWorkingDay(null));
        assertThrows(IllegalArgumentException.class, () -> calculator.addWorkingDays(null, 1));
        assertThrows(IllegalArgumentException.class, () -> calculator.countWorkingDays(null, LocalDate.now()));
        assertThrows(IllegalArgumentException.class, () -> calculator.countWorkingDays(LocalDate.now(), null));
    }

    @Test
    void testInvalidDateRangeThrowsException() {
        LocalDate start = LocalDate.of(2025, 12, 10);
        LocalDate end = LocalDate.of(2025, 12, 1);

        assertThrows(IllegalArgumentException.class, () -> calculator.countWorkingDays(start, end));
    }

    @Test
    void testSetEmptyWorkingDaysThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> calculator.setWorkingDays(Set.of()));
    }
}
