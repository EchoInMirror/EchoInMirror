@echo off

set JAVA_EXE="%~dp0\jre\bin\java.exe"
if not exist "%JAVA_EXE%" set JAVA_EXE=java.exe

echo %JAVA_EXE% -Xms1G -XX:+UseZGC --enable-preview --enable-native-access=ALL-UNNAMED -jar "%~dp0\EchoInMirror.jar" %*
%JAVA_EXE% -Xms1G -XX:+UseZGC --enable-preview --enable-native-access=ALL-UNNAMED -jar "%~dp0\EchoInMirror.jar" %*
