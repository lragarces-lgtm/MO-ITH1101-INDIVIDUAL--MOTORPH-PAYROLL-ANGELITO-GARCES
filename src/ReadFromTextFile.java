/**
 * Task 10 – Read Employee Data from Text File and Compute Net Pay
 * MotorPH Payroll System
 *
 * Reads employee_data.txt (each line: name, gross salary),
 * computes SSS, PhilHealth, Pag-IBIG, and income tax using
 * the methods from Task 9, then displays a payroll summary.
 */
package motorrph;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ReadFromTextFile {

    public static void main(String[] args) {
        String filename = "employee_data.txt"; // file must be in project root

        // Step 1: Check if file exists and is readable
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineNumber = 0;

            System.out.println("==================== PAYROLL SUMMARY FROM TEXT FILE ====================");
            System.out.printf("%-20s %-12s %-8s %-10s %-8s %-10s %-12s%n",
                    "Employee Name", "Gross Salary", "SSS", "PhilHealth", "PagIBIG", "Income Tax", "Net Pay");
            System.out.println("---------------------------------------------------------------------------");

            while ((line = br.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty()) continue; // skip empty lines

                // Parse line: expected format "Name, GrossSalary"
                String[] parts = line.split(",");
                if (parts.length < 2) {
                    System.err.println("Line " + lineNumber + " is malformed (missing comma): " + line);
                    continue;
                }

                String name = parts[0].trim();
                String salaryStr = parts[1].trim();

                // Validate and parse salary
                double grossSalary;
                try {
                    // Remove any commas or currency symbols if present (e.g., "40,000" -> "40000")
                    salaryStr = salaryStr.replace(",", "").replace("Php", "").replace("PHP", "").trim();
                    grossSalary = Double.parseDouble(salaryStr);
                } catch (NumberFormatException e) {
                    System.err.println("Line " + lineNumber + " invalid salary format: " + salaryStr);
                    continue;
                }

                // Validate positive salary
                if (grossSalary <= 0) {
                    System.err.println("Line " + lineNumber + " salary must be positive: " + grossSalary);
                    continue;
                }

                // Compute deductions using same methods as Task 9
                double sss = computeSSS(grossSalary);
                double philHealth = computePhilHealth(grossSalary);
                double pagIbig = computePagIbig(grossSalary);
                double taxableIncome = grossSalary - (sss + philHealth + pagIbig);
                double incomeTax = computeIncomeTax(taxableIncome);
                double totalDeductions = sss + philHealth + pagIbig + incomeTax;
                double netPay = grossSalary - totalDeductions;

                // Display result
                System.out.printf("%-20s %-12.2f %-8.2f %-10.2f %-8.2f %-10.2f %-12.2f%n",
                        name, grossSalary, sss, philHealth, pagIbig, incomeTax, netPay);
            }

            System.out.println("===========================================================================");
            System.out.println("Processing complete.");

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.err.println("Please ensure 'employee_data.txt' exists in the project root folder.");
        }
    }

    // -------------------- Deduction Methods (copied from Task 9) --------------------

    /**
     * Computes SSS contribution based on monthly salary.
     * Uses the provided table of ranges.
     */
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

    /**
     * Computes PhilHealth contribution (employee share).
     * Total premium = 3% of monthly salary, with floor 300 and cap 1800.
     * Employee pays 50% of total premium.
     */
    private static double computePhilHealth(double monthlySalary) {
        double totalPremium = monthlySalary * 0.03;
        if (totalPremium < 300) totalPremium = 300;
        else if (totalPremium > 1800) totalPremium = 1800;
        return totalPremium / 2;
    }

    /**
     * Computes Pag‑IBIG contribution (employee share).
     * Based on monthly salary:
     * - 1,000 to 1,500: 1%
     * - Over 1,500: 2%
     * (No cap applied.)
     */
    private static double computePagIbig(double monthlySalary) {
        if (monthlySalary >= 1000 && monthlySalary <= 1500) {
            return monthlySalary * 0.01;
        } else if (monthlySalary > 1500) {
            return monthlySalary * 0.02;
        } else {
            return 0; // below 1000, no contribution
        }
    }

    /**
     * Computes withholding tax based on taxable income.
     * Taxable income = monthly gross - (SSS + PhilHealth + Pag‑IBIG)
     */
    private static double computeIncomeTax(double taxableIncome) {
        if (taxableIncome <= 20832) return 0;
        else if (taxableIncome < 33333) return (taxableIncome - 20833) * 0.20;
        else if (taxableIncome < 66667) return 2500 + (taxableIncome - 33333) * 0.25;
        else if (taxableIncome < 166667) return 10833 + (taxableIncome - 66667) * 0.30;
        else if (taxableIncome < 666667) return 40833.33 + (taxableIncome - 166667) * 0.32;
        else return 200833.33 + (taxableIncome - 666667) * 0.35;
    }
}