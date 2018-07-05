//- Class implementing a certain Interface
//- Class containing a Method with Specified SIGNATURE that calculates fibonacci sequence 
// using Recursion / using a for loop

public abstract class TestCase2 extends Einstein implements Darwin, Farhaday {
	public static long fibonacci(long number) {
	    if ((number == 0) || (number == 1)) // base cases
	      return number;
	    else
	      // recursion step
	      return fibonacci(number - 1) + fibonacci(number - 2);
	  }

	public static void implem(){
		for (int counter = 0; counter <= 10; counter++) {
			System.out.printf("Fibonacci of %d is: %d\n", counter, fibonacci(counter));
		}
	}
}