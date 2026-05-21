@ECHO OFF
SETLOCAL

SET MAVEN_PROJECTBASEDIR=%~dp0
IF "%MAVEN_PROJECTBASEDIR:~-1%"=="\" SET MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%
SET WRAPPER_DIR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper
SET WRAPPER_JAR=%WRAPPER_DIR%\maven-wrapper.jar

IF NOT EXIST "%WRAPPER_JAR%" (
  IF EXIST "%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar -OutFile $env:WRAPPER_JAR"
  ) ELSE (
    ECHO Error: PowerShell required to download Maven Wrapper jar.
    EXIT /B 1
  )
)

SET JAVA_CMD=java
"%JAVA_CMD%" -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR%" -cp "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
ENDLOCAL
