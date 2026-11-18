@echo off
cd /d "%~dp0"
if not exist build\classes mkdir build\classes
echo Compiling BUDGETLY for NetBeans (build\classes)...
javac -cp ".;lib/*" -d build\classes src\budgetly\Main.java src\budgetly\database\*.java src\budgetly\model\*.java src\budgetly\dao\*.java src\budgetly\service\*.java src\budgetly\gui\*.java
if errorlevel 1 (
    echo Compilation failed.
    pause
    exit /b 1
)
if not exist build\classes\budgetly\resources mkdir build\classes\budgetly\resources
copy /Y src\budgetly\resources\logo.png build\classes\budgetly\resources\logo.png >nul
echo Launching BUDGETLY...
java -cp "build\classes;lib/*" budgetly.Main
