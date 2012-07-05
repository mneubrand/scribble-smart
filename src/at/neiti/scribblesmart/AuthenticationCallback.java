package at.neiti.scribblesmart;

/**
 * Callback interface which gets used to call EverNote credentials
 * validation results
 * 
 * @author markus
 *
 */
public interface AuthenticationCallback {
    
    int AUTHENTICATION_SUCCESSFUL = 0;
    int AUTHENTICATION_FAILED = 1;
    int AUTHENTICATION_UNKNOWN = 2;

	void onResult(int result);
	
}
