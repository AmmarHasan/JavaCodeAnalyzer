//- Method with specified signature that sums up of values using while loop but not for loop
public abstract class TestCase5 {
	public static void SummationUsingWhileLoop(){
		
		int num = 0;
		int sum = 0;

		while (num <= 10) {
			sum += num ;
			num++;
		}
		System.out.println("Sum = " + sum);
	    
		}
}