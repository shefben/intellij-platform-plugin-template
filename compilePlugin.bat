@echo off
rem Build the Tkinter Designer plugin using Gradle wrapper
call "%~dp0gradlew.bat" buildPlugin -x signPlugin --no-daemon
