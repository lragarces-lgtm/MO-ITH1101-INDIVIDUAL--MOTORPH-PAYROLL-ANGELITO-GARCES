/**
 * MotorPH Payroll System – Milestone 2 (All Tasks Combined)
 *
 * This program integrates:
 * - Task 7: Calculate total hours worked from attendance.csv
 * - Tasks 8 & 9: Compute semi‑monthly salary with government deductions (interactive)
 * - Task 10: Read employee_data.txt and compute net pay
 *
 * All deduction methods (SSS, PhilHealth, Pag‑IBIG, withholding tax) are reused.
 */
package motorphpayrollsystem;

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

public class MotorPHPayrollSystem {

    // Work schedule constants
    private static final LocalTime WORK_START = LocalTime.of(8, 0);
    private static final LocalTime WORK_END = LocalTime.of(17, 0);
    private static final LocalTime GRACE_START = LocalTime.of(8, 0);
    private static final LocalTime GRACE_END = LocalTime.of(8, 30);
    private static final LocalTime LUNCH_START = LocalTime.of(12, 0);
    private static final LocalTime LUNCH_END = LocalTime.of(13, 0);

    // Data stores (used by Tasks 7, 8, 9)
    private static Map<String, Map<String, Double>> hoursByCutoff = new HashMap<>();
    private static Map<String, Double> hourlyRateMap = new HashMap<>();
    private static Map<String, String> employeeNameMap = new HashMap<>();
    private static Map<String, Double> totalHoursMap = new HashMap<>(); // for Task 7

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int choice;

        do {
            System.out.println("\n========================================");
            System.out.println("MOTORPH PAYROLL SYSTEM – MILESTONE 2");
            System.out.println("========================================");
            System.out.println("1. Calculate Total Hours Worked (Task 7)");
            System.out.println("2. Compute Semi‑Monthly Salary with Deductions (Tasks 8 & 9)");
            System.out.println("3. Read from Text File (Task 10)");
            System.out.println("4. Exit");
            System.out.print("Enter your choice: ");

            try {
                choice = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                choice = 0;
            }

            switch (choice) {
                case 1:
                    task7CalculateHours();
                    break;
                case 2:
                    task8And9SalaryWithDeductions(scanner);
                    break;
                case 3:
                    task10ReadTextFile();
                    break;
                case 4:
                    System.out.println("Exiting...");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        } while (choice != 4);

        scanner.close();
    }

    // -------------------- Task 7: Calculate Total Hours Worked --------------------
    private static void task7CalculateHours() {
        System.out.println("\n--- TASK 7: TOTAL HOURS WORKED ---");
        totalHoursMap.clear();
        readAttendanceForTask7("attendance.csv");

        if (totalHoursMap.isEmpty()) {
            System.out.println("No attendance records found.");
            return;
        }

        System.out.printf("%-10s %-25s %-15s%n", "Emp #", "Employee Name", "Total Hours");
        System.out.println("---------------------------------------------");
        for (Map.Entry<String, Double> entry : totalHoursMap.entrySet()) {
            System.out.printf("%-10s %-25s %-15.5f%n",
                    entry.getKey(),
                    employeeNameMap.get(entry.getKey()),
                    entry.getValue());
        }
    }

    private static void readAttendanceForTask7(String filename) {
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
                String lastName = data[1].replace("\"", "").trim();
                String firstName = data[2].replace("\"", "").trim();
                String dateStr = data[3].replace("\"", "").trim();
                String timeInStr = data[4].replace("\"", "").trim();
                String timeOutStr = data[5].replace("\"", "").trim();

                LocalDate date = LocalDate.parse(dateStr, dateFormatter);
                if (date.getYear() != 2024 || date.getMonthValue() < 6 || date.getMonthValue() > 12) {
                    continue;
                }

                LocalTime timeIn = LocalTime.parse(timeInStr, timeFormatter);
                LocalTime timeOut = LocalTime.parse(timeOutStr, timeFormatter);

                double dailyHours = calculateDailyHours(timeIn, timeOut);
                if (dailyHours <= 0) continue;

                String fullName = firstName + " " + lastName;
                employeeNameMap.put(empId, fullName);
                totalHoursMap.merge(empId, dailyHours, Double::sum);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error parsing data: " + e.getMessage());
        }
    }

    // -------------------- Tasks 8 & 9: Salary with Deductions --------------------
    private static void task8And9SalaryWithDeductions(Scanner scanner) {
        System.out.println("\n--- TASKS 8 & 9: SEMI‑MONTHLY SALARY WITH DEDUCTIONS ---");
        // Read employee data and attendance
        readEmployees("employees.csv");
        readAttendanceForTasks89("attendance.csv");

        // Ask for employee number and month
        System.out.print("Enter Employee Number: ");
        String empId = scanner.nextLine().trim();

        System.out.println("Note: Payroll data is available only for months June to December (6–12).");
        System.out.print("Enter Month (6-12, or 0 for all months): ");
        int month;
        try {
            month = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            month = -1;
        }

        if (month != 0 && (month < 6 || month > 12)) {
            System.out.println("Invalid month. Please enter a number between 6 and 12, or 0 for all.");
            return;
        }

        displayPayrollSummary(empId, month);
    }

    private static void readEmployees(String filename) {
        String line;
        String csvSplitBy = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            br.readLine(); // skip header

            while ((line = br.readLine()) != null) {
                String[] data = line.split(csvSplitBy, -1);
                if (data.length < 5) {
                    continue;
                }

                String empId = data[0].replace("\"", "").trim();
                String lastName = data[1].replace("\"", "").trim();
                String firstName = data[2].replace("\"", "").trim();
                String hourlyRateStr = data[3].replace("\"", "").trim();

                hourlyRateStr = hourlyRateStr.replace(",", "");
                double hourlyRate = Double.parseDouble(hourlyRateStr);

                hourlyRateMap.put(empId, hourlyRate);
                employeeNameMap.put(empId, firstName + " " + lastName);
            }
        } catch (IOException e) {
            System.err.println("Error reading employees.csv: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Error parsing number in employees.csv: " + e.getMessage());
        }
    }

    private static void readAttendanceForTasks89(String filename) {
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
        } catch (Exception e) {
            System.err.println("Error parsing attendance data: " + e.getMessage());
        }
    }

    private static String getCutoffKey(LocalDate date) {
        int month = date.getMonthValue();
        int year = date.getYear();
        int half = date.getDayOfMonth() <= 15 ? 1 : 2;
        return year + "-" + String.format("%02d", month) + "-" + half;
    }

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

    // -------------------- Deduction Methods --------------------
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
        else return 1125.00;
    }

    private static double computePhilHealth(double monthlySalary) {
        double totalPremium = monthlySalary * 0.03;
        if (totalPremium < 300) totalPremium = 300;
        else if (totalPremium > 1800) totalPremium = 1800;
        return totalPremium / 2;
    }

    private static double computePagIbig(double monthlySalary) {
        if (monthlySalary >= 1000 && monthlySalary <= 1500) {
            return monthlySalary * 0.01;
        } else if (monthlySalary > 1500) {
            return monthlySalary * 0.02;
        } else {
            return 0;
        }
    }

    private static double computeIncomeTax(double taxableIncome) {
        if (taxableIncome <= 20832) return 0;
        else if (taxableIncome < 33333) return (taxableIncome - 20833) * 0.20;
        else if (taxableIncome < 66667) return 2500 + (taxableIncome - 33333) * 0.25;
        else if (taxableIncome < 166667) return 10833 + (taxableIncome - 66667) * 0.30;
        else if (taxableIncome < 666667) return 40833.33 + (taxableIncome - 166667) * 0.32;
        else return 200833.33 + (taxableIncome - 666667) * 0.35;
    }

    // -------------------- Display Payroll Summary (Tasks 8 & 9) --------------------
    private static void displayPayrollSummary(String empId, int month) {
        if (!hourlyRateMap.containsKey(empId)) {
            System.out.println("Employee #" + empId + " not found.");
            return;
        }

        double hourlyRate = hourlyRateMap.get(empId);
        String name = employeeNameMap.get(empId);
        Map<String, Double> empHours = hoursByCutoff.get(empId);

        System.out.println("\n==================== PAYROLL SUMMARY ====================");
        System.out.println("Employee #: " + empId);
        System.out.println("Name: " + name);
        System.out.printf("Hourly Rate: Php %.2f%n", hourlyRate);
        if (month != 0) System.out.println("Month: " + month);
        else System.out.println("Months: June – December");
        System.out.println("---------------------------------------------------------");
        System.out.printf("%-6s %-10s %-10s %-10s %-10s %-10s %-7s %-9s %-8s %-8s %-10s%n",
                "Month", "Cut1 hrs", "Cut1 pay", "Cut2 hrs", "Cut2 pay",
                "Monthly Gross", "SSS", "PhilHealth", "PagIBIG", "Tax", "Net Pay");
        System.out.println("---------------------------------------------------------");

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
                double sss = computeSSS(monthlyGross);
                double philHealth = computePhilHealth(monthlyGross);
                double pagIbig = computePagIbig(monthlyGross);
                double taxableIncome = monthlyGross - (sss + philHealth + pagIbig);
                double tax = computeIncomeTax(taxableIncome);
                double netPay = monthlyGross - (sss + philHealth + pagIbig + tax);

                System.out.printf("%-6s %-10.2f %-10.2f %-10.2f %-10.2f %-10.2f %-7.2f %-9.2f %-8.2f %-8.2f %-10.2f%n",
                        String.format("%02d", m),
                        hours1, gross1, hours2, gross2,
                        monthlyGross, sss, philHealth, pagIbig, tax, netPay);
                hasData = true;
            }
        }

        if (!hasData) {
            System.out.println("No attendance records found for this employee in the selected period.");
        }
        System.out.println("=========================================================");
    }

    // -------------------- Task 10: Read from Text File --------------------
    private static void task10ReadTextFile() {
        System.out.println("\n--- TASK 10: READ FROM TEXT FILE ---");
        String filename = "employee_data.txt";

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineNumber = 0;

            System.out.printf("%-20s %-12s %-8s %-10s %-8s %-10s %-12s%n",
                    "Employee Name", "Gross Salary", "SSS", "PhilHealth", "PagIBIG", "Income Tax", "Net Pay");
            System.out.println("---------------------------------------------------------------------------");

            while ((line = br.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",");
                if (parts.length < 2) {
                    System.err.println("Line " + lineNumber + " is malformed: " + line);
                    continue;
                }

                String name = parts[0].trim();
                String salaryStr = parts[1].trim().replace(",", "").replace("Php", "").replace("PHP", "").trim();
                double grossSalary;
                try {
                    grossSalary = Double.parseDouble(salaryStr);
                } catch (NumberFormatException e) {
                    System.err.println("Line " + lineNumber + " invalid salary: " + salaryStr);
                    continue;
                }

                if (grossSalary <= 0) {
                    System.err.println("Line " + lineNumber + " salary must be positive.");
                    continue;
                }

                double sss = computeSSS(grossSalary);
                double philHealth = computePhilHealth(grossSalary);
                double pagIbig = computePagIbig(grossSalary);
                double taxableIncome = grossSalary - (sss + philHealth + pagIbig);
                double tax = computeIncomeTax(taxableIncome);
                double netPay = grossSalary - (sss + philHealth + pagIbig + tax);

                System.out.printf("%-20s %-12.2f %-8.2f %-10.2f %-8.2f %-10.2f %-12.2f%n",
                        name, grossSalary, sss, philHealth, pagIbig, tax, netPay);
            }
            System.out.println("===========================================================================");
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }
}