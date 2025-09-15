@echo off
set JAVA_OPTS=-Dswing.defaultlaf=com.sun.java.swing.plaf.windows.WindowsLookAndFeel
java %JAVA_OPTS% -cp "target\classes;lib\*" com.iot.plc.MainApp
pause