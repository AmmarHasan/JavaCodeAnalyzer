//- Method with specified signature that sums up of values using for loop but not while loop
//
public abstract class TestCase6 {
	public static void SummationUsingForLoop(){
		int sum = 0;

		for (int num = 0;num <= 10; num++) {
			sum += num ;
		}
		System.out.println("Sum = " + sum);
	    
		}
}