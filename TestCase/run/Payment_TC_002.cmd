@echo off
rem Runs one test case from the UMPay End-to-End Test Cases workbook.
rem Generated - to change how a run is made, change the generator rather than this file.

setlocal
cd /d "%~dp0..\.."

echo Running Payment_TC_002 ...
echo.

call mvn -o test -Dheadless=true -Dcucumber.filter.tags="@Payment_TC_002"

echo.
echo Finished. The report is in Reports\ and the run log above.
pause >nul
