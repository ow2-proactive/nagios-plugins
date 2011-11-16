package qosprobercore.main;

public class NagiosReturnObject {
	/** Nagios exit codes. */
	public static final int RESULT_0_OK = 0; 				// Nagios code. Execution successfully. 
	public static final int RESULT_1_WARNING = 1; 			// Nagios code. Warning. 
	public static final int RESULT_2_CRITICAL = 2; 			// Nagios code. Critical problem in the tested entity.
	public static final int RESULT_3_UNKNOWN = 3; 			// Nagios code. Unknown state of the tested entity.
	
	private String errorMessage;
	private int errorCode;
	public NagiosReturnObject(int errorCode, String errorMessage){
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}
	public String getErrorMessage(){
		return errorMessage;
	}
	public int getErrorCode(){
		return errorCode;
	}
}
