@REM ----------------------------------------------------------------------------
@REM Maven Wrapper Batch Script
@REM ----------------------------------------------------------------------------
@echo off
setlocal

set "DIR=%~dp0"
if "%DIR:~-1%"=="\" set "DIR=%DIR:~0,-1%"

if not "%JAVA_HOME%"=="" (
    set "JAVACMD=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVACMD=java.exe"
)

if not exist "%JAVACMD%" (
    echo Error: JAVA_HOME is not set correctly.
    echo We cannot find java.exe in "%JAVA_HOME%\bin".
    exit /b 1
)

set "WRAPPER_JAR=%DIR%\.mvn\wrapper\maven-wrapper.jar"
if not exist "%WRAPPER_JAR%" (
    echo Error: Could not find %WRAPPER_JAR%
    exit /b 1
)

"%JAVACMD%" -cp "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%DIR%" org.apache.maven.wrapper.MavenWrapperMain %*
exit /b %ERRORLEVEL%
