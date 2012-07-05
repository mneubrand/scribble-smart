package at.neiti.scribblesmart.main;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;
import at.neiti.scribblesmart.AuthenticationCallback;
import at.neiti.scribblesmart.NoteUtils;
import at.neiti.scribblesmart.R;
import at.neiti.scribblesmart.ui.AuthenticationPreference;

import com.evernote.client.oauth.android.EvernoteSession;
import com.evernote.client.oauth.android.SaveService;

/**
 * PreferenceActivity for editing the applications preferences
 * 
 * @author markus
 * 
 */
public class SettingsActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {

    public static final String PREF_SCRIBBLE_SMART = "PREF_SCRIBBLE_SMART";
    public static final String PREF_EVERNOTE = "PREF_EVERNOTE";
    public static final String PREF_REAUTHENTICATE = "PREF_REAUTHENTICATE";
    public static final String PREF_AUTH_TOKEN = "PREF_AUTH_TOKEN";
    public static final String PREF_USER_ID = "PREF_USER_ID";
    public static final String PREF_WEB_API_PREFIX = "PREF_WEB_API_PREFIX";
    public static final String PREF_NOTE_STORE_URL = "PREF_NOTE_STORE_URL";
    public static final String PREF_SPEAKER_PHONE = "PREF_SPEAKER_PHONE";
    public static final String PREF_FIRST_START = "PREF_FIRST_START";

    private EvernoteSession session;
    private boolean isAuthenticating;

    private ProgressDialog progressDialog;

    private AuthenticationPreference authenticationPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize UI
        addPreferencesFromResource(R.layout.settings);

        // Register preference change listener
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(this);

        // Set authenticationPreference accordingly
        authenticationPreference = (AuthenticationPreference) findPreference(PREF_REAUTHENTICATE);

        boolean enableEvernote = preferences.getBoolean(PREF_EVERNOTE, false);
        if (!enableEvernote) {
            authenticationPreference.setEnabled(false);
        }

        // Initialize progress dialog for Evernote authentication
        // Doesn't display dialog!
        progressDialog = new ProgressDialog(this);
        progressDialog
                .setMessage("Authenticating with Evernote. Please wait...");
        progressDialog.setCancelable(false);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Dismiss dialogs if any are displayed
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        } else if (authenticationPreference.getDialog() != null
                && authenticationPreference.getDialog().isShowing()) {
            authenticationPreference.getDialog().dismiss();
        }

        // If resume is happening because of Evernote authentication complete
        // the authentication
        if (this.isAuthenticating) {
            if (!session.completeAuthentication()) {
                Toast.makeText(
                        this,
                        "Temporarily couldn't authenticate with Evernote. Please try again later",
                        Toast.LENGTH_SHORT).show();
            } else {
                // Save credentials in preferences
                SharedPreferences settings = PreferenceManager
                        .getDefaultSharedPreferences(this);
                Editor editor = settings.edit();

                editor.putString(PREF_AUTH_TOKEN, SaveService.getAuthToken()
                        .getToken());
                editor.putString(PREF_NOTE_STORE_URL, SaveService
                        .getAuthToken().getNoteStoreUrl());
                editor.putString(PREF_WEB_API_PREFIX, SaveService
                        .getAuthToken().getWebApiUrlPrefix());
                editor.putInt(PREF_USER_ID, SaveService.getAuthToken()
                        .getUserId());

                editor.commit();
            }

            isAuthenticating = false;
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        if (key.equals(PREF_EVERNOTE)) {
            // If Evernote gets enabled validate authentication
            if (sharedPreferences.getBoolean(PREF_EVERNOTE, false)) {
                validateEvernoteAuthentication(sharedPreferences);
                authenticationPreference.setEnabled(true);
            } else {
                authenticationPreference.setEnabled(false);
            }
        } else if (key.equals(PREF_REAUTHENTICATE)) {
            // Validate Evernote authentication as requested by user
            session = new EvernoteSession(
                    SaveService.getEvernoteApplicationInfo(), getCacheDir());
            session.authenticate(this);
            this.isAuthenticating = true;
        }
    }

    /**
     * Validate Evernote authentication
     * 
     * @param sharedPreferences
     */
    private void validateEvernoteAuthentication(
            SharedPreferences sharedPreferences) {
        progressDialog.show();
        String authToken = sharedPreferences.getString(PREF_AUTH_TOKEN, null);
        if (authToken != null) {
            NoteUtils.validateEvernoteAuthentication(this,
                    new AuthenticationCallback() {

                        @Override
                        public void onResult(int result) {
                            if (result == AUTHENTICATION_FAILED) {
                                session = new EvernoteSession(SaveService
                                        .getEvernoteApplicationInfo(),
                                        getCacheDir());
                                session.authenticate(SettingsActivity.this);
                                SettingsActivity.this.isAuthenticating = true;
                            } else if (result == AUTHENTICATION_UNKNOWN) {
                                progressDialog.dismiss();
                                Toast.makeText(
                                        SettingsActivity.this,
                                        "Temporarily couldn't authenticate with Evernote. Please try again later",
                                        Toast.LENGTH_SHORT).show();
                            } else if (result == AUTHENTICATION_SUCCESSFUL) {
                                Toast.makeText(
                                        SettingsActivity.this,
                                        "Authentication with Evernote successful",
                                        Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            }
                        }

                    });
        } else {
            session = new EvernoteSession(
                    SaveService.getEvernoteApplicationInfo(), getCacheDir());
            session.authenticate(this);
            this.isAuthenticating = true;
        }
    }

}