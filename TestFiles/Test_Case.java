
//- decalaring an Abstract(Specifier specified) Class

public abstract class TestCase1 {

	public static int Addition(int total) {
		
		int status = 5;
		int num = 7;
		int num2 = 5;
		
		for(int j = 0;j < 5; j++) {
			if(status > 5) {
				total = num + num2;
				System.out.println(total);	
			}else {
				System.out.println("Status not qualified");
			}
			num++; num2++;
			status++;
		}
		return total;
	}
}

//- Class implementing a certain Interface
//- Class containing a Method with Specified SIGNATURE that calculates fibonacci sequence 
// using Recursion / using a for loop

//public abstract class TestCase2 extends Einstein implements Darwin, Farhaday {
//	public static long fibonacci(long number) {
//	    if ((number == 0) || (number == 1)) // base cases
//	      return number;
//	    else
//	      // recursion step
//	      return fibonacci(number - 1) + fibonacci(number - 2);
//	  }
//
//	public static void implem(){
//		for (int counter = 0; counter <= 10; counter++) {
//			System.out.printf("Fibonacci of %d is: %d\n", counter, fibonacci(counter));
//		}
//	}
//}
//
////- Class inheriting from a certain base class
////- does NOT use certain function calls (say from Math)
//// i.e. Restricting a Certain Method Call and/or from a Certain Class
//
//public abstract class TestCase3 extends Einstein implements Darwin, Farhaday{
//	public static long multiply(int a, int b) {
//    for (int i=0; i < 2; i++) {}
//    return Math.multiplyExact(a, b);
//  }
//}

//a Method that uses ternary operator but NO if statement

//public abstract class TestCase4 {
//	
//	public static void TenaryOperator(){	
//	    int a = 6; int b = 5;
//	    int minVal = (a < b) ? a : b;
////	    if(true) {}
//	    System.out.println(minVal);
//		}
//}
//
////- Method with specified signature that sums up of values using while loop but not for loop
//public abstract class TestCase5 {
//	public static void SummationUsingWhileLoop(){
//		
//		int num = 0;
//		int sum = 0;
//
//		while (num <= 10) {
//			sum += num ;
//			num++;
//		}
//		System.out.println("Sum = " + sum);
//	    
//		}
//}
//
////- Method with specified signature that sums up of values using for loop but not while loop
//
//public abstract class TestCase6 {
//	public static void SummationUsingForLoop(){
//		int sum = 0;
//
//		for (int num = 0;num <= 10; num++) {
//			sum += num ;
//		}
//		System.out.println("Sum = " + sum);
//	    
//		}
//}
//
////- Computing sum NOT using Java Arrays but ArrayLists instead
////- Using iterator and excluding for loop in While Loop
////
//public abstract class TestCase7 {
//	public static void IteratorWithoutForLoop(){
//		
//		ArrayList num = new ArrayList();
//		num.add(1);
//		num.add(5);
//		num.add(7);
//
//		double sum = 0;
//		Iterator iter = num.iterator();
//
//		while (iter.hasNext()) {
//			sum += iter.next();
//		}
//		System.out.println("Sum = " + sum);
//	    
//		}
//}
//
//
////- Method with specified signature that uses a certain Method call within a particular Construct (Like For)
//public abstract class TestCase8 {
//    public static long fibonacci(long number) {
//        if ((number == 0) || (number == 1)) // base cases
//          return number;
//        else
//          // recursion step
//          return fibonacci(number - 1) + fibonacci(number - 2);
//      }
//
//    public static void implem(){
//        for (int counter = 0; counter <= 10; counter++) {
//            System.out.printf("Fibonacci of %d is: %d\n", counter, fibonacci(counter));
//        }
//    }
//}
