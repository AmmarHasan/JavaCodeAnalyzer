class Computer {
  Computer() {
    System.out.println("Constructor of Computer class.");
  }
 
  void shutDown() {
    System.out.println("Shuting down your PC soon...");
  }
 
public static class Laptop {
	
	private static int batteryPercentage;
	
  Laptop() {
	 batteryPercentage = 90;
    System.out.println("Constructor of Laptop class.");
  }
 
  void shutDown() {
	    System.out.println("Shuting down your PC soon...");
	  }
  
  void batteryStatus() {
	  int i;
	  System.out.println( batteryPercentage +"% Battery available.");
	  {if (this.batteryPercentage > 80) {
		  System.out.println( "You are good!");
	  } else {
		  System.out.println( "Charge now.");
	  }}
    
//	  for (int i=0 ; i< 3; i++) {
//		  
//	  }
  }
} 

public static void main(String[] args) {
    Computer my = new Computer();
    Laptop your = new Laptop();
 
    my.computer_method();
    your.batteryStatus();
  }
}