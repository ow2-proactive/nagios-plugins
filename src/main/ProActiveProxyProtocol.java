package main;
public enum ProActiveProxyProtocol  
{
	UNKNOWN,
	JAVAPA,
	REST;
	
	
	protected static final String PROTOCOL_JAVAPA_STR = "JAVAPA";
	protected static final String PROTOCOL_REST_STR = "REST";
	
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
