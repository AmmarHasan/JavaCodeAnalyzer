////- Class inheriting from a certain base class
////- does NOT use certain function calls (say from Math)
//// i.e. Restricting a Certain Method Call and/or from a Certain Class
//
public abstract class TestCase3 extends Einstein implements Darwin, Farhaday{
	public static long multiply(int a, int b) {
    for (int i=0; i < 2; i++) {}
    return Math.multiplyExact(a, b);
  }
}