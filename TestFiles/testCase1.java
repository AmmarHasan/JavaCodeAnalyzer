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