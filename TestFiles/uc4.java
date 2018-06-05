
// Use Case 4 : Class with specific Method implementing
// simple mutlplication of two +ve number without using * or Math.multiplyFull method.
// Method signature is given and call to other user defined method is restricted

public class DemoMath {
    public static long multiply(int a, int b) {
      long result = 0;
      for (int i=0; i < a; i++) {
        result += b;
      }
      return b;
    }
}


/* Following examples will result in error */

// Problems: static modifier, function signature, using forbidden operator, not using required construct
// public class DemoMath {
//     int mutliply(int a, double b) {
//       return a*b;
//     }
// }

// Problems: using forbidden builtin method
// public class DemoMath {
//     static long mutliply(int a, int b) {
//       for (int i=0; i < 2; i++) {}
//       return Math.multiplyFull(a, b);
//     }
// }

// Problems: implementing in user defined method
// public class DemoMath {
//     static long mutliply(int a, int b) {
//       for (int i=0; i < 2; i++) {}
//       return MyMultiply(a, b);
//     }
//     static long MyMutliply(int a, int b) {
//       return a*b;
//     }
// }
