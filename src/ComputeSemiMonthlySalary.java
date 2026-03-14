/**
 * Task 8 – Compute Semi-Monthly Salary (Single Employee, Optional Month)
 * MotorPH Payroll System
 *
 * Reads employees.csv and attendance.csv, calculates hours per cutoff,
 * then asks the user for an employee number and a month (6-12, or 0 for all),
 * and displays the semi-monthly gross pay for that employee for the selected month(s).
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
import java.util.Scanner;

public class ComputeSemiMonthlySalary {

    // Work schedule constants
    private static final LocalTime WORK_START = LocalTime.of(8, 0);
    private static final LocalTime WORK_END = LocalTime.of(17, 0);
    private static final LocalTime GRACE_START = LocalTime.of(8, 0);
    private static final LocalTime GRACE_END = LocalTime.of(8, 30);
    private static final LocalTime LUNCH_START = LocalTime.of(12, 0);
    private static final LocalTime LUNCH_END = LocalTime.of(13, 0);

    // Data maps
    private static Map<String, Map<String, Double>> hoursByCutoff = new HashMap<>();
    private static Map<String, Double> hourlyRateMap = new HashMap<>();
    private static Map<String, String> employeeNameMap = new HashMap<>();

    public static void main(String[] args) {
        // Step 1: Read employee master data
        readEmployees("employees.csv");

        // Step 2: Read attendance and accumulate hours per cutoff
        readAttendance("attendance.csv");

        // Step 3: Ask user for employee number and month
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter Employee Number: ");
        String empId = scanner.nextLine().trim();

        System.out.println("Note: Payroll data is available only for months June to December (6–12).");
        System.out.print("Enter Month (6-12, or 0 for all months): ");
        int month = scanner.nextInt();
        scanner.nextLine(); // consume newline

        // Validate month input
        if (month != 0 && (month < 6 || month > 12)) {
            System.out.println("Invalid month. Please enter a number between 6 and 12, or 0 for all.");
            return;
        }

        // Step 4: Display summary for that employee and month
        displaySummaryForEmployee(empId, month);

        scanner.close();
    }

    /**
     * Reads employees.csv (9 columns) and stores hourly rate and name.
     */
    private static void readEmployees(String filename) {
        String line;
        String csvSplitBy = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            br.readLine(); // skip header

            while ((line = br.readLine()) != null) {
                String[] data = line.split(csvSplitBy, -1);
                if (data.length < 5) {
                    System.out.println("Skipping line (less than 5 columns): " + line);
                    continue;
                }

                String empId = data[0].replace("\"", "").trim();
                String lastName = data[1].replace("\"", "").trim();
                String firstName = data[2].replace("\"", "").trim();
                String hourlyRateStr = data[3].replace("\"", "").trim();

                // Remove commas from numbers (e.g., "45,000" -> "45000")
                hourlyRateStr = hourlyRateStr.replace(",", "");
                double hourlyRate = Double.parseDouble(hourlyRateStr);

                hourlyRateMap.put(empId, hourlyRate);
                employeeNameMap.put(empId, firstName + " " + lastName);
            }
        } catch (IOException e) {
            System.err.println("Error reading employees.csv: " + e.getMessage());
            System.exit(1);
        } catch (NumberFormatException e) {
            System.err.println("Error parsing number in employees.csv: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Reads attendance.csv (6 columns) and accumulates hours per cutoff.
     */
    private static void readAttendance(String filename) {
        String line;
        String csvSplitBy = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("M/d/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:mm");

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            br.readLine(); // skip header

            while ((line = br.readLine()) != null) {
                String[] data = line.split(csvSplitBy, -1);
                if (data.length < 6) continue;

                String empId = data[0].replace("\"", "").trim();
                String dateStr = data[3].replace("\"", "").trim();
                String timeInStr = data[4].replace("\"", "").trim();
                String timeOutStr = data[5].replace("\"", "").trim();

                LocalDate date = LocalDate.parse(dateStr, dateFormatter);
                // Only June to December 2024
                if (date.getYear() != 2024 || date.getMonthValue() < 6 || date.getMonthValue() > 12) {
                    continue;
                }

                LocalTime timeIn = LocalTime.parse(timeInStr, timeFormatter);
                LocalTime timeOut = LocalTime.parse(timeOutStr, timeFormatter);

                double dailyHours = calculateDailyHours(timeIn, timeOut);
                if (dailyHours <= 0) continue;

                String cutoffKey = getCutoffKey(date);

                hoursByCutoff.computeIfAbsent(empId, k -> new HashMap<>())
                             .merge(cutoffKey, dailyHours, Double::sum);
            }
        } catch (IOException e) {
            System.err.println("Error reading attendance.csv: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error parsing attendance data: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String getCutoffKey(LocalDate date) {
        int month = date.getMonthValue();
        int year = date.getYear();
        int half = date.getDayOfMonth() <= 15 ? 1 : 2;
        return year + "-" + String.format("%02d", month) + "-" + half;
    }

    /**
     * Calculates daily hours worked according to the rules.
     */
    private static double calculateDailyHours(LocalTime timeIn, LocalTime timeOut) {
        LocalTime adjustedIn = timeIn;
        if (!timeIn.isBefore(GRACE_START) && timeIn.isBefore(GRACE_END)) {
            adjustedIn = WORK_START;
        }

        LocalTime adjustedOut = timeOut;
        if (!timeOut.isBefore(WORK_END) && timeOut.isBefore(WORK_END.plusMinutes(30))) {
            adjustedOut = WORK_END;
        }

        LocalTime effectiveStart = adjustedIn.isBefore(WORK_START) ? WORK_START : adjustedIn;
        LocalTime effectiveEnd = adjustedOut.isAfter(WORK_END) ? WORK_END : adjustedOut;

        if (!effectiveEnd.isAfter(effectiveStart)) {
            return 0.0;
        }

        long totalMinutes = ChronoUnit.MINUTES.between(effectiveStart, effectiveEnd);

        long lunchOverlap = 0;
        if (effectiveStart.isBefore(LUNCH_END) && effectiveEnd.isAfter(LUNCH_START)) {
            LocalTime overlapStart = effectiveStart.isAfter(LUNCH_START) ? effectiveStart : LUNCH_START;
            LocalTime overlapEnd = effectiveEnd.isBefore(LUNCH_END) ? effectiveEnd : LUNCH_END;
            lunchOverlap = ChronoUnit.MINUTES.between(overlapStart, overlapEnd);
        }

        double hours = (totalMinutes - lunchOverlap) / 60.0;
        return Math.max(hours, 0.0);
    }

    /**
     * Displays the semi‑monthly salary summary for a given employee and month.
     * @param empId employee ID
     * @param month 0 for all months (June–Dec), or a specific month (6–12)
     */
    private static void displaySummaryForEmployee(String empId, int month) {
        if (!hourlyRateMap.containsKey(empId)) {
            System.out.println("Employee #" + empId + " not found.");
            return;
        }

        double hourlyRate = hourlyRateMap.get(empId);
        String name = employeeNameMap.get(empId);

        System.out.println("\n========== SEMI-MONTHLY SALARY SUMMARY ==========");
        System.out.println("Employee #: " + empId);
        System.out.println("Name: " + name);
        System.out.printf("Hourly Rate: Php %.2f%n", hourlyRate);
        if (month != 0) {
            System.out.println("Month: " + month);
        } else {
            System.out.println("Months: June – December");
        }
        System.out.println("--------------------------------------------------");
        System.out.printf("%-7s %-12s %-12s %-12s %-12s %-12s%n",
                "Month", "Cutoff1 hrs", "Cutoff1 pay", "Cutoff2 hrs", "Cutoff2 pay", "Monthly Gross");
        System.out.println("--------------------------------------------------");

        Map<String, Double> empHours = hoursByCutoff.get(empId);
        if (empHours == null || empHours.isEmpty()) {
            System.out.println("No attendance records found for this employee.");
            return;
        }

        boolean hasData = false;
        int startMonth = (month == 0) ? 6 : month;
        int endMonth = (month == 0) ? 12 : month;

        for (int m = startMonth; m <= endMonth; m++) {
            String monthStr = String.format("2024-%02d", m);
            String cutoff1Key = monthStr + "-1";
            String cutoff2Key = monthStr + "-2";

            double hours1 = empHours.getOrDefault(cutoff1Key, 0.0);
            double hours2 = empHours.getOrDefault(cutoff2Key, 0.0);
            double gross1 = hours1 * hourlyRate;
            double gross2 = hours2 * hourlyRate;
            double monthlyGross = gross1 + gross2;

            if (hours1 > 0 || hours2 > 0) {
                System.out.printf("%-7s %-12.2f %-12.2f %-12.2f %-12.2f %-12.2f%n",
                        String.format("%02d", m), hours1, gross1, hours2, gross2, monthlyGross);
                hasData = true;
            }
        }

        if (!hasData) {
            if (month == 0) {
                System.out.println("No attendance records found for this employee in June–December.");
            } else {
                System.out.println("No attendance records found for this employee in month " + month + ".");
            }
        }
        System.out.println("==================================================");
    }
}