package at.neiti.scribblesmart.ui;

import android.app.ProgressDialog;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.widget.Toast;
import at.neiti.scribblesmart.AuthenticationCallback;
import at.neiti.scribblesmart.NoteUtils;

/**
 * Custom preference which acts like a button. Used to validate
 * Evernote credentials 
 * 
 * @author markus
 *
 */
public class AuthenticationPreference extends Preference {

	private Context context;
	private ProgressDialog progressDialog;

	public AuthenticationPreference(Context context, AttributeSet attrs) {
		super(context, attrs);

		this.context = context;
		
		//Initialize progress dialog
		progressDialog = new ProgressDialog(context);
		progressDialog.setMessage("Authenticating with Evernote. Please wait...");
		progressDialog.setCancelable(false);
	}

	@Override
	protected void onClick() {
		progressDialog.show();

		NoteUtils.validateEvernoteAuthentication(context, new AuthenticationCallback() {

			@Override
			public void onResult(int result) {
				if (result == AUTHENTICATION_FAILED) {
					callChangeListener(null);
				} else if (result == AUTHENTICATION_UNKNOWN) {
					progressDialog.dismiss();
					Toast.makeText(context, "Temporarily couldn't authenticate with Evernote. Please try again later",
							Toast.LENGTH_SHORT).show();
				} else if (result == AUTHENTICATION_SUCCESSFUL) {
					progressDialog.dismiss();
					Toast.makeText(context, "Authentication with Evernote successful", Toast.LENGTH_SHORT).show();
				}
			}

		});
	}

	public ProgressDialog getDialog() {
		return progressDialog;
	}

}
