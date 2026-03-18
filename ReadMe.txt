MotorPH Payroll System – Milestone 2
This repository contains the Java source code for the MotorPH Payroll System as part of our course requirements. The system calculates employee hours worked, computes semi‑monthly gross pay, applies government deductions (SSS, PhilHealth, Pag‑IBIG, withholding tax), and reads employee data from both CSV and text files.

Tasks Implemented
Task 7 – Calculate Hours Worked
Reads attendance.csv (Employee #, Last Name, First Name, Date, Log In, Log Out).

Computes daily hours worked according to company rules:

Work period: 8:00 AM – 5:00 PM only.

Grace period: logins between 8:00 and 8:30 are treated as 8:00.

Logout grace: logouts between 17:00 and 17:30 are treated as 17:00.

1‑hour unpaid lunch deduction (12:00–13:00).

Outputs total hours per employee for the period June–December 2024.

Task 8 – Compute Semi‑Monthly Salary
Reads employees.csv (9 columns: Employee #, Last Name, First Name, Hourly Rate, …).

Groups hours by cutoff (1st–15th and 16th–end of month).

Interactive: prompts for employee number and month (6–12, or 0 for all months).

Displays hours and gross pay for each cutoff, plus monthly total.

Task 9 – Apply Government Deductions
Extends Task 8 by adding deduction methods using official tables:

SSS – based on monthly salary brackets.

PhilHealth – 3% of monthly salary (employee share = half, with floor ₱300 and cap ₱1800).

Pag‑IBIG – 1% for salary ₱1,000–1,500, 2% for >₱1,500.

Withholding Tax – based on TRAIN law brackets (applied after other deductions).

Outputs a detailed payroll summary including hours per cutoff, gross pay, each deduction, and net pay.

Task 10 – Read from Text File
Reads a plain text file employee_data.txt (each line: Name, Gross Salary).

Reuses the deduction methods from Task 9.

Validates file existence, line format, and positive salary.

Prints a clean summary table with name, gross salary, all deductions, and net pay.

Project Structure

text
MotorPH/
├── src/
│   └── motorph/
│       ├── CalculateHoursWorkedAll.java      (Task 7)
│       ├── ComputeSemiMonthlySalaryAll.java  (Tasks 8 & 9)
│       └── ReadFromTextFile.java              (Task 10)
├── employees.csv          (employee master data – 9 columns)
├── attendance.csv         (daily attendance – 6 columns)
├── employee_data.txt      (text file for Task 10 – name, gross salary)
└── README.md              (this file)

CSV File Formats

employees.csv (9 columns)

text
Employee #,Last Name,First Name,Hourly Rate,Gross Semi-monthly Rate,Basic Salary,Rice Subsidy,Phone Allowance,Clothing Allowance
10001,Garcia,Manuel III,535.71,45000,90000,1500,2000,1000
...
Commas in numbers (e.g., 45,000) are automatically handled.

attendance.csv (6 columns)

text
Employee #,Last Name,First Name,Date,Log In,Log Out
10001,Garcia,Manuel III,06/03/2024,8:59,18:31
...
Date format: MM/DD/YYYY (e.g., 06/03/2024)

Time format: H:mm (e.g., 8:59, 18:31)

employee_data.txt

text
Manuel III Garcia, 90000
Antonio Lim, 60000
...
One employee per line, name and gross salary separated by a comma.

Salary may include commas (e.g., 90,000) – they are stripped automatically.