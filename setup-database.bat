@echo off
REM ==================================================================
REM  Creates the ev_charging_db database and loads the demo data
REM  using the MySQL command line client (alternative to running
REM  com.abcgreen.evcharging.tools.DatabaseSetup from Eclipse).
REM ==================================================================
setlocal
set MYSQL=mysql
where mysql >nul 2>nul
if errorlevel 1 (
  for %%V in ("9.4" "9.3" "9.2" "9.1" "9.0" "8.4" "8.0") do (
    if exist "C:\Program Files\MySQL\MySQL Server %%~V\bin\mysql.exe" set MYSQL="C:\Program Files\MySQL\MySQL Server %%~V\bin\mysql.exe"
  )
)
echo Using MySQL client: %MYSQL%
set /p DBUSER=MySQL user [root]: 
if "%DBUSER%"=="" set DBUSER=root
echo Creating schema... (you will be asked for the MySQL password)
%MYSQL% -u %DBUSER% -p --default-character-set=utf8mb4 < "%~dp0database\01_schema.sql"
if errorlevel 1 goto failed
echo Loading demo data... (password again)
%MYSQL% -u %DBUSER% -p --default-character-set=utf8mb4 < "%~dp0database\02_seed_data.sql"
if errorlevel 1 goto failed
echo.
echo Database ev_charging_db is ready.
echo Logins: admin / Admin@123   operator / Operator@123   aarav.sharma / Customer@123
pause
exit /b 0
:failed
echo.
echo FAILED - check that MySQL is running and the password is correct.
echo Tip: in Eclipse run com.abcgreen.evcharging.tools.DatabaseSetup instead.
pause
exit /b 1
