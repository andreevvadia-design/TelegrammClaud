@echo off
setlocal
set "CLAUD_JAVA=java.exe"
if defined JAVA_HOME set "CLAUD_JAVA=%JAVA_HOME%\bin\java.exe"
"%CLAUD_JAVA%" -classpath "%~dp0gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
