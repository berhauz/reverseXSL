@echo off
SET input=Sample3_VATdeclare.csv.txt

ECHO Before you can run this sample transformation, you shall have a copy
ECHO   of the reverseXSL.jar software archive in this same directory,
ECHO and ensure that a java virtual machine of at least version 1.4
ECHO   is available on the command line PATH
ECHO .
ECHO you are in:
cd
ECHO .
ECHO Here is the input file... (%input%)
pause
ECHO .
ECHO .
type %input%
ECHO .
ECHO .
ECHO Executing the transformation with:
ECHO 	java -jar reverseXSL.jar %input%
ECHO .
ECHO Ready?
pause

java -jar reverseXSL.jar %input%

ECHO .
ECHO .
pause
