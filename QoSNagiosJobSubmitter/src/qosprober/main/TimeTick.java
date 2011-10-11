package qosprober.main;

import java.util.Date;

/** 
 * Class to get the time elapsed between events. */
public class TimeTick {
	private long init;
	
	public TimeTick(){
		tickSec();
	}
	
	/**
	 * Get the time elapsed between this call and the previous one. */
	public double tickSec(){
		long now = (new Date()).getTime();
		double interval = ((double)((double)now - (double)init)) / 1000;
		init = restart();
		return interval;
	}
	
	private long restart(){
		return (new Date()).getTime();
	}
	
	public static void main(String args[]) throws Exception{
		TimeTick tt = new TimeTick();
		System.out.println("tick " + tt.tickSec());
		Thread.sleep(150);
		System.out.println("tick " + tt.tickSec());
		Thread.sleep(250);
		System.out.println("tick " + tt.tickSec());
		Thread.sleep(350);
		System.out.println("tick " + tt.tickSec());
		Thread.sleep(1400);
		System.out.println("tick " + tt.tickSec());
	}
}
