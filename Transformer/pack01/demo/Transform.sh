if test "$1" = "" 
then
	echo "Error: missing input file argument"
	echo "Invoke as"
	echo "    sh Transform.sh myInputFile"
	echo ""
	exit 1
fi

java -cp ReverseXSL.jar com.reverseXSL.Transform 2>/dev/null
if test "$?" -ne 0 
then
	echo "Error: missing software jar or JVM"
	echo "Before you can run this transformation command, you shall have a copy"
	echo "  of the reverseXSL.jar software archive in this same directory,"
	echo "and ensure that a java virtual machine of at least version 1.4"
	echo "  is available on the shell command PATH"
	echo ""
	echo "you are in:"
	pwd
	echo ""
	exit 1
fi

java -cp ReverseXSL.jar 	com.reverseXSL.Transform 	$1	2>Transform.log

echo ""
echo ""
echo "Show logs (Y or N)? "
read rep
if (test "$rep" = "Y" || test "$rep" = "y")
then 
	echo ""
	cat Transform.log
	echo ""
	echo ""
fi
exit 0



