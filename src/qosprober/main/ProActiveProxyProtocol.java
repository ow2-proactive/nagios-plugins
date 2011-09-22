package qosprober.main;

/** 
 * Enum to establish the possible protocols to be used
 * when connected with the Scheduler. */
public enum ProActiveProxyProtocol  
{
	/** Unknown protocol. */
	UNKNOWN,	
	/** Java ProActive protocol. */
	JAVAPA,		
	/** RESTful protocol. */
	REST;		
	
	/** Representative Strings for each protocol. */
	protected static final String PROTOCOL_JAVAPA_STR = "JAVAPA";
	protected static final String PROTOCOL_REST_STR = "REST";
	
	/** 
	 * Convert a string to a value of this Enum. 
	 * @param string to convert, should be JAVAPA or REST (case non-sensitive). */
	public static ProActiveProxyProtocol parseProtocol(String str) throws IllegalArgumentException{
		str = str.trim().toUpperCase();
		if(str.equals(PROTOCOL_JAVAPA_STR)){
			return JAVAPA;
		}else if(str.equals(PROTOCOL_REST_STR)){
			return REST;
		}else{
			throw new IllegalArgumentException("ProActiveProxyProtocol unknown:" + str);
		}
	}
	
}
