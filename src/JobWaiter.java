import org.ow2.proactive.scheduler.common.job.JobId;


public class JobWaiter extends Thread{
	private JobId jobId;
	private long timeoutms;
	
	
	public JobWaiter(JobId jobId, long timeoutms){
		this.jobId = jobId;
		this.timeoutms = timeoutms;
		this.setName("JobFinishedWaiter " + jobId);
	}

	public JobId getJobId(){
		return jobId;
	}
	
	public void run(){
		
		try {
			System.out.println("   JobWaiter waiting for " + jobId);
			Thread.sleep(timeoutms);
			/* It means that the job did not finish before the dead line. */
			System.out.println("   JobWaiter finished waiting for " + jobId);
			
		} catch (InterruptedException e) {
			System.out.println("   JobWaiter interrupted while waiting for " + jobId);
			/* It means that the job finished correctly. We do nothing here. */
			/**/
		}
	}
	
}
