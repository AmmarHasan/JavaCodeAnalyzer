
// Use Case 3 : Class with specific Parent class and implementing specific Interfaces

public class StudentAdvisor extends Teacher implements IAdvisor, IExample {
    public advise() {
        System.out.print("Advise students");
    }
} 

/* Following examples will result in error */

/* Problem: Does not extend Teacher */
// public class StudentAdvisor implements IAdvisor, IExample {
//     public advise() {
//         System.out.print("Advise students");
//     }
// } 

/* Problem: Does not extend Teacher instead extends forbidden class */
// public class StudentAdvisor extends Staff implements IAdvisor, IExample {
//     public advise() {
//         System.out.print("Advise students");
//     }
// }

/* Problem: Does not implement IAdvisor, IExample */
// public class StudentAdvisor extends Teacher {
//     public advise() {
//         System.out.print("Advise students");
//     }
// } 

/* Problem: Does not implement IExample */
// public class StudentAdvisor extends Teacher implements IAdvisor {
//     public advise() {
//         System.out.print("Advise students");
//     }
// } 

/* Problem: Does not implement IExample but implements forbidden interface i.e IForbiddenExample  */
// public class StudentAdvisor extends Teacher implements IAdvisor, IForbiddenExample {
//     public advise() {
//         System.out.print("Advise students");
//     }
// } 

/* Problem: Does not implement IAdvisor or extend Teacher */
// public class StudentAdvisor {
//     public advise() {
//         System.out.print("Advise students");
//     }
// } 