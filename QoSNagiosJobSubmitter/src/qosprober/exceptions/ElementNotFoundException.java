package qosprober.exceptions;

/** 
 * General exception thrown whenever a particular needed element in a set is not found.  */
public class ElementNotFoundException extends Exception{
	private static final long serialVersionUID = 1L;

	public ElementNotFoundException(String message){
		super(message);
	}
}
