input="Sample1_MT101.txt"

echo "Before you can run this sample transformation, you shall have a copy"
echo "  of the ReverseXSL.jar software archive in this same directory,"
echo "and ensure that a java virtual machine of at least version 1.4"
echo "  is available on the command line PATH"
echo ""
echo "you are in:"
pwd
echo ""
echo "Here is the input file... ($input)"
echo "ready?"
read rep
echo ""
echo ""
cat $input
echo ""
echo ""
echo "Executing the transformation with:"
echo "	java -jar ReverseXSL.jar $input"
echo ""
echo "ready?"
read rep

java -jar ReverseXSL.jar $input

echo ""
echo ""

