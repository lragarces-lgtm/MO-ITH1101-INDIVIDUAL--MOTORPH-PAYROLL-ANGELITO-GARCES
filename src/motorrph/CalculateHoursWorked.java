/**
 * Task 7 – Calculate Hours Worked (All Employees)
 * MotorPH Payroll System
 *
 * Reads attendance.csv, computes daily hours worked for each employee
 * using the 8:00–17:00 rule (with grace period and lunch deduction),
 * and displays total hours per employee for the entire period.
 */
package motorrph;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class CalculateHoursWorked {

    // Work schedule constants
    private static final LocalTime WORK_START = LocalTime.of(8, 0);
    private static final LocalTime WORK_END = LocalTime.of(17, 0);
    private static final LocalTime GRACE_START = LocalTime.of(8, 0);
    private static final LocalTime GRACE_END = LocalTime.of(8, 30);
    private static final LocalTime LUNCH_START = LocalTime.of(12, 0);
    private static final LocalTime LUNCH_END = LocalTime.of(13, 0);

    public static void main(String[] args) {
        String attendanceFile = "attendance.csv";
        String line;
        String csvSplitBy = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"; // handles quoted commas
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("M/d/yyyy"); // e.g., 6/3/2024
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:mm");     // e.g., 8:59

        Map<String, Double> totalHoursMap = new HashMap<>();
        Map<String, String> employeeNameMap = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(attendanceFile))) {
            // Skip header
            br.readLine();

            while ((line = br.readLine()) != null) {
                String[] data = line.split(csvSplitBy, -1);
                if (data.length < 6) continue;

                String empId = data[0].replace("\"", "").trim();
                String lastName = data[1].replace("\"", "").trim();
                String firstName = data[2].replace("\"", "").trim();
                String dateStr = data[3].replace("\"", "").trim();
                String timeInStr = data[4].replace("\"", "").trim();
                String timeOutStr = data[5].replace("\"", "").trim();

                // Parse date and times
                LocalDate date = LocalDate.parse(dateStr, dateFormatter);
                // Only keep June to December
                if (date.getYear() != 2024 || date.getMonthValue() < 6 || date.getMonthValue() > 12) {
                    continue;
                }

                LocalTime timeIn = LocalTime.parse(timeInStr, timeFormatter);
                LocalTime timeOut = LocalTime.parse(timeOutStr, timeFormatter);

                // Calculate daily hours
                double dailyHours = calculateDailyHours(timeIn, timeOut);

                // Accumulate total hours for this employee
                String fullName = firstName + " " + lastName;
                employeeNameMap.put(empId, fullName);
                totalHoursMap.merge(empId, dailyHours, Double::sum);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return;
        } catch (Exception e) {
            System.err.println("Error parsing data: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Display results
        System.out.println("========== TASK 7: TOTAL HOURS WORKED (JUNE–DECEMBER 2024) ==========");
        System.out.printf("%-10s %-25s %-15s%n", "Emp #", "Employee Name", "Total Hours");
        System.out.println("---------------------------------------------------------------------");
        for (Map.Entry<String, Double> entry : totalHoursMap.entrySet()) {
            System.out.printf("%-10s %-25s %-15.5f%n",
                    entry.getKey(),
                    employeeNameMap.get(entry.getKey()),
                    entry.getValue());
        }
        System.out.println("=====================================================================");
        System.out.println("Computation completed.");
    }

    /**
     * Calculates daily hours worked according to the rules:
     * - Grace period: if login between 8:00 and 8:30, treat as 8:00.
     * - If logout between 17:00 and 17:30, treat as 17:00.
     * - Only count time within 8:00–17:00.
     * - Subtract 1 hour lunch (12:00–13:00) if the interval overlaps it.
     */
    private static double calculateDailyHours(LocalTime timeIn, LocalTime timeOut) {
        // Apply grace period
        LocalTime adjustedIn = timeIn;
        if (!timeIn.isBefore(GRACE_START) && timeIn.isBefore(GRACE_END)) {
            adjustedIn = WORK_START;
        }

        LocalTime adjustedOut = timeOut;
        if (!timeOut.isBefore(WORK_END) && timeOut.isBefore(WORK_END.plusMinutes(30))) {
            adjustedOut = WORK_END;
        }

        // Cap to work hours
        LocalTime effectiveStart = adjustedIn.isBefore(WORK_START) ? WORK_START : adjustedIn;
        LocalTime effectiveEnd = adjustedOut.isAfter(WORK_END) ? WORK_END : adjustedOut;

        // If no time worked
        if (!effectiveEnd.isAfter(effectiveStart)) {
            return 0.0;
        }

        // Total minutes between start and end
        long totalMinutes = ChronoUnit.MINUTES.between(effectiveStart, effectiveEnd);

        // Subtract lunch overlap if any
        long lunchOverlap = 0;
        if (effectiveStart.isBefore(LUNCH_END) && effectiveEnd.isAfter(LUNCH_START)) {
            LocalTime overlapStart = effectiveStart.isAfter(LUNCH_START) ? effectiveStart : LUNCH_START;
            LocalTime overlapEnd = effectiveEnd.isBefore(LUNCH_END) ? effectiveEnd : LUNCH_END;
            lunchOverlap = ChronoUnit.MINUTES.between(overlapStart, overlapEnd);
        }

        double hours = (totalMinutes - lunchOverlap) / 60.0;
        return Math.max(hours, 0.0);
    }
}