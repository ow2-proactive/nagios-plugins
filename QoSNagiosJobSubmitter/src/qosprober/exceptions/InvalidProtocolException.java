package qosprober.exceptions;

/** 
 * Exeption thrown whenever an invalid protocol is specified. */
public class InvalidProtocolException extends Exception{
	private static final long serialVersionUID = 1L;

	public InvalidProtocolException(String message){
		super(message);
	}
}
