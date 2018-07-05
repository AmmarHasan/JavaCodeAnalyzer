
//- Computing sum NOT using Java Arrays but ArrayLists instead
//- Using iterator and excluding for loop in While Loop

public abstract class TestCase7 {
	public static void IteratorWithoutForLoop(){
		
		ArrayList num = new ArrayList();
		num.add(1);
		num.add(5);
		num.add(7);

		double sum = 0;
		Iterator iter = num.iterator();

		while (iter.hasNext()) {
			sum += iter.next();21
		}
		System.out.println("Sum = " + sum);
	    
		}
}