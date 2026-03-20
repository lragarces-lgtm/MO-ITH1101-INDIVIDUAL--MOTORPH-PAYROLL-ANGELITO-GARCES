/**
 * MotorPH Payroll System – Milestone 2 (Final)
 * 
 * Credentials:
 *   employee / 12345
 *   payroll_staff / 12345
 * 
 * Features:
 * - Employee can view their own personal details.
 * - Staff can process payroll for one or all employees.
 * - Payroll shows both cutoffs per month (Jun–Dec) with deductions applied to second cutoff.
 * - Hours calculation: 8‑5 workday, 10‑minute grace, 1‑hour lunch.
 */
package motorphpayrollsystem;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class MotorPHPayrollSystem {

    // -------------------- Constants for work schedule --------------------
    private static final LocalTime WORK_START = LocalTime.of(8, 0);
    private static final LocalTime WORK_END = LocalTime.of(17, 0);
    // Grace period: login between 8:00 and 8:10 is treated as 8:00
    private static final LocalTime GRACE_START = LocalTime.of(8, 0);
    private static final LocalTime GRACE_END = LocalTime.of(8, 10);
    // Unpaid lunch break (12:00–13:00)
    private static final LocalTime LUNCH_START = LocalTime.of(12, 0);
    private static final LocalTime LUNCH_END = LocalTime.of(13, 0);

    // -------------------- Data Stores --------------------
    // hoursByCutoff: key = employee ID, value = map of cutoffKey -> total hours
    // cutoffKey format: "YYYY-MM-half" (half = 1 for 1st‑15th, 2 for 16th‑end)
    private static Map<String, Map<String, Double>> hoursByCutoff = new HashMap<>();
    private static Map<String, Double> hourlyRateMap = new HashMap<>();      // employee ID -> hourly rate
    private static Map<String, String> employeeNameMap = new HashMap<>();   // employee ID -> full name
    private static Map<String, String> employeeBirthdayMap = new HashMap<>(); // employee ID -> birthday

    // -------------------- Inner Class for Attendance Records --------------------
    private static class AttendanceRecord {
        String empId;
        LocalDate date;
        LocalTime timeIn;
        LocalTime timeOut;
        AttendanceRecord(String empId, LocalDate date, LocalTime timeIn, LocalTime timeOut) {
            this.empId = empId;
            this.date = date;
            this.timeIn = timeIn;
            this.timeOut = timeOut;
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Load employee data (required for all operations)
        readEmployees("employees.csv");

        // --- Login ---
        System.out.println("=== MOTORPH PAYROLL SYSTEM ===");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        // Validate credentials – only two valid usernames with password 12345
        if (!(username.equals("employee") || username.equals("payroll_staff")) || !password.equals("12345")) {
            System.out.println("Incorrect username and/or password.");
            scanner.close();
            return;
        }

        // Route based on username
        if (username.equals("employee")) {
            employeeMenu(scanner);
        } else {
            payrollStaffMenu(scanner);
        }

        scanner.close();
    }

    // -------------------- Employee Menu --------------------
    private static void employeeMenu(Scanner scanner) {
        int choice;
        do {
            System.out.println("\n--- Employee Menu ---");
            System.out.println("1. Enter your employee number");
            System.out.println("2. Exit");
            System.out.print("Choice: ");
            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                choice = 0;
            }

            switch (choice) {
                case 1:
                    System.out.print("Enter your employee number: ");
                    String empId = scanner.nextLine().trim();
                    if (!employeeNameMap.containsKey(empId)) {
                        System.out.println("Employee number does not exist.");
                    } else {
                        // Display personal details
                        System.out.println("Employee #: " + empId);
                        System.out.println("Employee Name: " + employeeNameMap.get(empId));
                        System.out.println("Birthday: " + employeeBirthdayMap.get(empId));
                    }
                    break;
                case 2:
                    System.out.println("Exiting...");
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        } while (choice != 2);
    }

    // -------------------- Payroll Staff Menu --------------------
    private static void payrollStaffMenu(Scanner scanner) {
        int choice;
        do {
            System.out.println("\n--- Payroll Staff Menu ---");
            System.out.println("1. Process Payroll");
            System.out.println("2. Exit");
            System.out.print("Choice: ");
            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                choice = 0;
            }

            if (choice == 1) {
                processPayrollSubmenu(scanner);
            } else if (choice == 2) {
                System.out.println("Exiting...");
            } else {
                System.out.println("Invalid choice.");
            }
        } while (choice != 2);
    }

    private static void processPayrollSubmenu(Scanner scanner) {
        int subChoice;
        do {
            System.out.println("\n--- Process Payroll ---");
            System.out.println("1. One employee");
            System.out.println("2. All employees");
            System.out.println("3. Exit");
            System.out.print("Choice: ");
            try {
                subChoice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                subChoice = 0;
            }

            switch (subChoice) {
                case 1:
                    System.out.print("Enter employee number: ");
                    String empId = scanner.nextLine().trim();
                    if (!employeeNameMap.containsKey(empId)) {
                        System.out.println("Employee number does not exist.");
                    } else {
                        displayDetailedPayroll(empId);
                    }
                    break;
                case 2:
                    // Loop through all employees (keys of employeeNameMap)
                    for (String id : employeeNameMap.keySet()) {
                        System.out.println("\n========================================");
                        displayDetailedPayroll(id);
                    }
                    break;
                case 3:
                    System.out.println("Returning to main menu...");
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        } while (subChoice != 3);
    }

    // -------------------- File Reading --------------------
    /**
     * Reads employees.csv – expected columns (0‑based):
     * 0: Employee #
     * 1: Last Name
     * 2: First Name
     * 3: Birthday
     * ...
     * 18: Hourly Rate (last column)
     */
    private static void readEmployees(String filename) {
        String line;
        // Regex to handle quoted fields (e.g., "Garcia, Manuel III")
        String csvSplitBy = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            br.readLine(); // skip header

            while ((line = br.readLine()) != null) {
                String[] data = line.split(csvSplitBy, -1);
                // Need at least 19 columns (indices 0..18)
                if (data.length < 19) {
                    System.err.println("Skipping line: insufficient columns - " + line);
                    continue;
                }

                String empId = data[0].replace("\"", "").trim();
                String lastName = data[1].replace("\"", "").trim();
                String firstName = data[2].replace("\"", "").trim();
                String birthday = data[3].replace("\"", "").trim();
                String hourlyRateStr = data[18].replace("\"", "").trim();

                // Remove commas from numbers (e.g., "45,000" -> "45000")
                hourlyRateStr = hourlyRateStr.replace(",", "");
                double hourlyRate = Double.parseDouble(hourlyRateStr);

                hourlyRateMap.put(empId, hourlyRate);
                employeeNameMap.put(empId, firstName + " " + lastName);
                employeeBirthdayMap.put(empId, birthday);
            }
        } catch (IOException e) {
            System.err.println("Error reading employees.csv: " + e.getMessage());
            System.exit(1);
        } catch (NumberFormatException e) {
            System.err.println("Error parsing number in employees.csv: " + e.getMessage());
            System.exit(1);
        }
    }

    /** Reads attendance.csv and returns a list of records for June–December 2024. */
    private static List<AttendanceRecord> readAttendanceRecords(String filename) {
        List<AttendanceRecord> records = new ArrayList<>();
        String line;
        String csvSplitBy = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("M/d/yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:mm");

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            br.readLine(); // skip header

            while ((line = br.readLine()) != null) {
                String[] data = line.split(csvSplitBy, -1);
                if (data.length < 6) continue; // need at least 6 columns

                String empId = data[0].replace("\"", "").trim();
                String dateStr = data[3].replace("\"", "").trim();
                String timeInStr = data[4].replace("\"", "").trim();
                String timeOutStr = data[5].replace("\"", "").trim();

                LocalDate date = LocalDate.parse(dateStr, dateFormatter);
                // Only keep records from June to December 2024
                if (date.getYear() != 2024 || date.getMonthValue() < 6 || date.getMonthValue() > 12) {
                    continue;
                }

                LocalTime timeIn = LocalTime.parse(timeInStr, timeFormatter);
                LocalTime timeOut = LocalTime.parse(timeOutStr, timeFormatter);

                records.add(new AttendanceRecord(empId, date, timeIn, timeOut));
            }
        } catch (IOException e) {
            System.err.println("Error reading attendance.csv: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error parsing attendance data: " + e.getMessage());
        }
        return records;
    }

    // -------------------- Hours Calculation --------------------
    /**
     * Calculates daily hours worked according to the rules:
     * - Work period: 8:00–17:00 only.
     * - 10‑minute grace period: login between 8:00 and 8:10 is treated as 8:00.
     * - 1‑hour unpaid lunch (12:00–13:00) is subtracted if the work interval overlaps it.
     */
    private static double calculateDailyHours(LocalTime timeIn, LocalTime timeOut) {
        // Apply grace period for login
        LocalTime adjustedIn = timeIn;
        if (!timeIn.isBefore(GRACE_START) && timeIn.isBefore(GRACE_END)) {
            adjustedIn = WORK_START;
        }

        // Cap to work hours – start cannot be before 8:00, end cannot be after 17:00
        LocalTime effectiveStart = adjustedIn.isBefore(WORK_START) ? WORK_START : adjustedIn;
        LocalTime effectiveEnd = timeOut.isAfter(WORK_END) ? WORK_END : timeOut;

        // If start is after or equal to end, no hours worked
        if (!effectiveEnd.isAfter(effectiveStart)) {
            return 0.0;
        }

        long totalMinutes = ChronoUnit.MINUTES.between(effectiveStart, effectiveEnd);

        // Subtract lunch period if it overlaps the work interval
        long lunchOverlap = 0;
        if (effectiveStart.isBefore(LUNCH_END) && effectiveEnd.isAfter(LUNCH_START)) {
            LocalTime overlapStart = effectiveStart.isAfter(LUNCH_START) ? effectiveStart : LUNCH_START;
            LocalTime overlapEnd = effectiveEnd.isBefore(LUNCH_END) ? effectiveEnd : LUNCH_END;
            lunchOverlap = ChronoUnit.MINUTES.between(overlapStart, overlapEnd);
        }

        double hours = (totalMinutes - lunchOverlap) / 60.0;
        return Math.max(hours, 0.0);
    }

    /** Returns cutoff key in format "YYYY-MM-half" (half = 1 for 1st‑15th, 2 for 16th‑end). */
    private static String getCutoffKey(LocalDate date) {
        int month = date.getMonthValue();
        int year = date.getYear();
        int half = date.getDayOfMonth() <= 15 ? 1 : 2;
        return year + "-" + String.format("%02d", month) + "-" + half;
    }

    // -------------------- Deduction Methods (from government tables) --------------------
    private static double computeSSS(double monthlySalary) {
        if (monthlySalary < 3250) return 135.00;
        else if (monthlySalary < 3750) return 157.50;
        else if (monthlySalary < 4250) return 180.00;
        else if (monthlySalary < 4750) return 202.50;
        else if (monthlySalary < 5250) return 225.00;
        else if (monthlySalary < 5750) return 247.50;
        else if (monthlySalary < 6250) return 270.00;
        else if (monthlySalary < 6750) return 292.50;
        else if (monthlySalary < 7250) return 315.00;
        else if (monthlySalary < 7750) return 337.50;
        else if (monthlySalary < 8250) return 360.00;
        else if (monthlySalary < 8750) return 382.50;
        else if (monthlySalary < 9250) return 405.00;
        else if (monthlySalary < 9750) return 427.50;
        else if (monthlySalary < 10250) return 450.00;
        else if (monthlySalary < 10750) return 472.50;
        else if (monthlySalary < 11250) return 495.00;
        else if (monthlySalary < 11750) return 517.50;
        else if (monthlySalary < 12250) return 540.00;
        else if (monthlySalary < 12750) return 562.50;
        else if (monthlySalary < 13250) return 585.00;
        else if (monthlySalary < 13750) return 607.50;
        else if (monthlySalary < 14250) return 630.00;
        else if (monthlySalary < 14750) return 652.50;
        else if (monthlySalary < 15250) return 675.00;
        else if (monthlySalary < 15750) return 697.50;
        else if (monthlySalary < 16250) return 720.00;
        else if (monthlySalary < 16750) return 742.50;
        else if (monthlySalary < 17250) return 765.00;
        else if (monthlySalary < 17750) return 787.50;
        else if (monthlySalary < 18250) return 810.00;
        else if (monthlySalary < 18750) return 832.50;
        else if (monthlySalary < 19250) return 855.00;
        else if (monthlySalary < 19750) return 877.50;
        else if (monthlySalary < 20250) return 900.00;
        else if (monthlySalary < 20750) return 922.50;
        else if (monthlySalary < 21250) return 945.00;
        else if (monthlySalary < 21750) return 967.50;
        else if (monthlySalary < 22250) return 990.00;
        else if (monthlySalary < 22750) return 1012.50;
        else if (monthlySalary < 23250) return 1035.00;
        else if (monthlySalary < 23750) return 1057.50;
        else if (monthlySalary < 24250) return 1080.00;
        else if (monthlySalary < 24750) return 1102.50;
        else return 1125.00; // 24750 and above
    }

    /** PhilHealth contribution (employee share). Total premium = 3% of monthly salary, floor 300, cap 1800. Employee pays half. */
    private static double computePhilHealth(double monthlySalary) {
        double totalPremium = monthlySalary * 0.03;
        if (totalPremium < 300) totalPremium = 300;
        else if (totalPremium > 1800) totalPremium = 1800;
        return totalPremium / 2;
    }

    /** Pag‑IBIG contribution (employee share): 1% for salary 1000–1500, 2% for >1500. */
    private static double computePagIbig(double monthlySalary) {
        if (monthlySalary >= 1000 && monthlySalary <= 1500) {
            return monthlySalary * 0.01;
        } else if (monthlySalary > 1500) {
            return monthlySalary * 0.02;
        } else {
            return 0; // below 1000, no contribution (though not expected)
        }
    }

    /** Withholding tax based on taxable income (monthly gross minus other deductions). Uses TRAIN law brackets. */
    private static double computeIncomeTax(double taxableIncome) {
        if (taxableIncome <= 20832) return 0;
        else if (taxableIncome < 33333) return (taxableIncome - 20833) * 0.20;
        else if (taxableIncome < 66667) return 2500 + (taxableIncome - 33333) * 0.25;
        else if (taxableIncome < 166667) return 10833 + (taxableIncome - 66667) * 0.30;
        else if (taxableIncome < 666667) return 40833.33 + (taxableIncome - 166667) * 0.32;
        else return 200833.33 + (taxableIncome - 666667) * 0.35;
    }

    // -------------------- Payroll Display --------------------
    /** Builds hoursByCutoff map from attendance records. */
    private static void buildHoursByCutoff() {
        hoursByCutoff.clear();
        List<AttendanceRecord> records = readAttendanceRecords("attendance.csv");
        for (AttendanceRecord rec : records) {
            double dailyHours = calculateDailyHours(rec.timeIn, rec.timeOut);
            if (dailyHours <= 0) continue;
            String cutoffKey = getCutoffKey(rec.date);
            // Add daily hours to the appropriate cutoff for this employee
            hoursByCutoff.computeIfAbsent(rec.empId, k -> new HashMap<>())
                         .merge(cutoffKey, dailyHours, Double::sum);
        }
    }

    /** Displays detailed payroll for one employee (all months June–December). */
    private static void displayDetailedPayroll(String empId) {
        // Ensure hoursByCutoff is built (could be called multiple times, but rebuild each time for simplicity)
        buildHoursByCutoff();

        double hourlyRate = hourlyRateMap.get(empId);
        String name = employeeNameMap.get(empId);
        String birthday = employeeBirthdayMap.get(empId);
        Map<String, Double> empHours = hoursByCutoff.get(empId);

        System.out.println("Employee #: " + empId);
        System.out.println("Employee Name: " + name);
        System.out.println("Birthday: " + birthday);
        System.out.println();

        if (empHours == null || empHours.isEmpty()) {
            System.out.println("No attendance records found for this employee.");
            return;
        }

        boolean hasData = false;
        // Loop through months June (6) to December (12)
        for (int monthIndex = 6; monthIndex <= 12; monthIndex++) {
            String monthStr = String.format("2024-%02d", monthIndex);
            String cutoff1Key = monthStr + "-1";
            String cutoff2Key = monthStr + "-2";

            double hours1 = empHours.getOrDefault(cutoff1Key, 0.0);
            double hours2 = empHours.getOrDefault(cutoff2Key, 0.0);
            double gross1 = hours1 * hourlyRate;
            double gross2 = hours2 * hourlyRate;

            // Skip months with no hours worked (optional – if you want to show zero, remove this condition)
            if (hours1 == 0 && hours2 == 0) continue;

            hasData = true;
            // Monthly gross is sum of both cutoffs – used for deduction calculation
            double monthlyGross = gross1 + gross2;

            // Compute all deductions based on monthly gross
            double sss = computeSSS(monthlyGross);
            double philHealth = computePhilHealth(monthlyGross);
            double pagIbig = computePagIbig(monthlyGross);
            double totalDeductionsBeforeTax = sss + philHealth + pagIbig;
            double taxableIncome = monthlyGross - totalDeductionsBeforeTax;
            double tax = computeIncomeTax(taxableIncome);
            double totalDeductions = totalDeductionsBeforeTax + tax;

            // First cutoff – no deductions applied
            System.out.println("Cutoff Date: " + getMonthName(monthIndex) + " 1 to " + getMonthName(monthIndex) + " 15");
            System.out.println("Total Hours Worked: " + hours1);
            System.out.printf("Gross Salary: Php %.2f%n", gross1);
            System.out.printf("Net Salary: Php %.2f%n", gross1); // same as gross
            System.out.println();

            // Second cutoff – all deductions applied
            System.out.println("Cutoff Date: " + getMonthName(monthIndex) + " 16 to " + getMonthName(monthIndex) + " 30");
            System.out.println("Total Hours Worked: " + hours2);
            System.out.printf("Gross Salary: Php %.2f%n", gross2);
            System.out.printf("SSS: Php %.2f%n", sss);
            System.out.printf("PhilHealth: Php %.2f%n", philHealth);
            System.out.printf("Pag-IBIG: Php %.2f%n", pagIbig);
            System.out.printf("Tax: Php %.2f%n", tax);
            System.out.printf("Total Deductions: Php %.2f%n", totalDeductions);
            double net2 = gross2 - totalDeductions;
            System.out.printf("Net Salary: Php %.2f%n", net2);
            System.out.println();
        }

        if (!hasData) {
            System.out.println("No attendance records found for this employee.");
        }
    }

    /** Helper to convert month number (1‑12) to full month name. */
    private static String getMonthName(int month) {
        String[] months = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        return months[month - 1];
    }
}
