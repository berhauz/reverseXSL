@echo off

IF "%1%" == "" (
	ECHO Error: missing input file argument
	ECHO Invoke as
	ECHO     Transform myInputFile
	ECHO .
	pause
	GOTO END
)

java -cp ReverseXSL.jar com.reverseXSL.Transform 2>NUL:
IF ERRORLEVEL 1 ( 
	ECHO Error: missing software jar or JVM
	ECHO Before you can run this transformation command, you shall have a copy
	ECHO   of the reverseXSL.jar software archive in this same directory,
	ECHO and ensure that a java virtual machine of at least version 1.4
	ECHO   is available on the command line PATH
	ECHO .
	ECHO you are in:
	cd
	ECHO .
	pause
	GOTO END
)

java -cp ReverseXSL.jar 	com.reverseXSL.Transform 	%1% 	2>transform.log

ECHO .
ECHO .
SET /P REP="Show logs (Y or N)? "
IF /I "%REP%" == "Y" (
	ECHO .
	TYPE transform.log
	ECHO .
	ECHO .
	pause
)

:END

