package qosprober.main;

public class TimeouterThread extends Thread{
	private int timeoutsec;
	private int errorcode;
	private String message;
	
	public TimeouterThread(int timeoutsec, int errorcode, String message){
		this.timeoutsec = timeoutsec;
		this.errorcode = errorcode;
		this.message = message;
	}
	
	public void run(){
		try {
			Thread.sleep(timeoutsec * 1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Main.printAndExit(errorcode, message);
	
	}
}
