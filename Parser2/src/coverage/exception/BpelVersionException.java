package coverage.exception;


public class BpelVersionException extends BpelException {
	
	public static final String WRONG_VERSION="Wrong version"; 
	public BpelVersionException(String version){
		super(version);
	}
}