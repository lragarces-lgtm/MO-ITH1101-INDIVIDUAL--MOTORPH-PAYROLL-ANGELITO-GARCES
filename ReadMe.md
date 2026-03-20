# MotorPH Payroll System – Milestone 2

A Java payroll application that calculates employee hours, gross pay, and government deductions (SSS, PhilHealth, Pag‑IBIG, withholding tax) based on attendance data. Implements a login system for employees and payroll staff, and produces detailed payroll reports for June–December 2024.

## Features

- **Login System**
  - **Employee**: username `employee`, password `12345` – can view personal details (employee number, name, birthday).
  - **Payroll Staff**: username `payroll_staff`, password `12345` – can process payroll for one employee or all employees.

- **Payroll Processing**
  - Calculates daily hours worked using the following rules:
    - Work period: 8:00 AM – 5:00 PM only.
    - 10‑minute grace period (login between 8:00 and 8:10 treated as 8:00).
    - 1‑hour unpaid lunch deduction (12:00–13:00).
  - Groups hours by cutoff (1st–15th and 16th–end of month).
  - Computes semi‑monthly gross pay: `hours × hourly rate`.
  - Applies government deductions on the **monthly gross** (sum of both cutoffs) and deducts them from the **second cutoff** only.
  - Displays detailed output per month, with both cutoffs, deductions, and net pay.

- **Government Deductions**
  - **SSS** – based on official contribution table.
  - **PhilHealth** – 3% of monthly salary (employee pays half, floor ₱300, cap ₱1800 total premium).
  - **Pag‑IBIG** – 1% for salary ₱1,000–1,500, 2% for >₱1,500.
  - **Withholding Tax** – TRAIN law brackets applied to taxable income (monthly gross minus other deductions).

- **Data Sources**
  - `employees.csv` – contains employee data (ID, name, birthday, hourly rate, etc.).
  - `attendance.csv` – daily log‑in/log‑out records for June–December 2024.

## How to Run

1. **Clone the repository** or download the files.
2. Place the CSV files (`employees.csv`, `attendance.csv`) in the **project root** (same folder as `src`).
3. Open the project in **NetBeans** (or any Java IDE).
4. Compile and run `MotorPHPayrollSystem.java`.
5. Follow the login prompts.

## CSV File Formats

### `employees.csv` (19 columns – required columns)
| Column | Description |
|--------|-------------|
| 0 | Employee # |
| 1 | Last Name |
| 2 | First Name |
| 3 | Birthday (YYYY-MM-DD) |
| ... | ... |
| 18 | Hourly Rate (numeric, commas allowed) |

### `attendance.csv` (6 columns)
