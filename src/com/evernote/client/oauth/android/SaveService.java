package com.evernote.client.oauth.android;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import at.neiti.scribblesmart.NoteUtils;
import at.neiti.scribblesmart.RecordInfo;
import at.neiti.scribblesmart.main.SettingsActivity;
import at.neiti.scribblesmart.overlay.NotesOverlayService;

import com.evernote.client.conn.ApplicationInfo;
import com.evernote.client.conn.mobile.FileData;
import com.evernote.client.oauth.EvernoteAuthToken;
import com.evernote.edam.error.EDAMErrorCode;
import com.evernote.edam.error.EDAMNotFoundException;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.NoteAttributes;
import com.evernote.edam.type.Resource;
import com.evernote.edam.util.EDAMUtil;

/**
 * Background service for persisting notes through the EverNote API
 * 
 * @author markus
 * 
 */
public class SaveService extends IntentService {

    public SaveService() {
        super("SaveService");
    }

    public static final String EXTRA_DRAWING = "EXTRA_DRAWING";
    public static final String EXTRA_NOTE_GUID = "EXTRA_NOTE_GUID";
    public static final String EXTRA_AUDIO = "EXTRA_AUDIO";
    public static final String EXTRA_INCOMING = "EXTRA_INCOMING";
    public static final String EXTRA_PREF_TITLE = "EXTRA_PREF_TITLE";

    private static final String CONSUMER_KEY = "neiti";
    private static final String CONSUMER_SECRET = "XXX";

    // Name of this application, for logging
    private static final String TAG = "SaveService";

    // Change to "www.evernote.com" to use the Evernote production service
    // instead of the sandbox
    // private static final String EVERNOTE_HOST = "sandbox.evernote.com";
    private static final String EVERNOTE_HOST = "www.evernote.com";

    private static final String APP_NAME = "Scribble Smart";
    private static final String APP_VERSION = "1.0";

    // Used to interact with the Evernote web service
    private EvernoteSession session;

    // The ENML preamble to every Evernote note.
    // Note content goes between <en-note> and </en-note>
    private static final String NOTE_PREFIX = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml2.dtd\">"
            + "<en-note>";

    // The ENML postamble to every Evernote note
    private static final String NOTE_SUFFIX = "</en-note>";

    // Custom content class so other EverNote apps can't edit Scribble Smart
    // notes
    private static final String CONTENT_CLASS = "at.neiti.scribblesmart";

    // Supported actions for Intents
    public static final String ACTION_UPLOAD = "upload";
    public static final String ACTION_DELETE = "delete";

    private Handler mHandler;

    @Override
    public void onCreate() {
        super.onCreate();

        // Workaround for
        // http://code.google.com/p/android/issues/detail?id=20915
        mHandler = new Handler();

        setupSession();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getAction().equals(ACTION_UPLOAD)) {
            // Get note infos from intent
            String drawing = intent.getStringExtra(EXTRA_DRAWING);
            String guid = intent.getStringExtra(EXTRA_NOTE_GUID);
            String incomingNumber = intent.getStringExtra(EXTRA_INCOMING);
            String title = NoteUtils.buildTitle(this, incomingNumber);
            String prefTitle = intent.getStringExtra(EXTRA_PREF_TITLE);
            Parcelable[] infos = intent.getParcelableArrayExtra(EXTRA_AUDIO);

            Note newNote = null;
            Log.i(TAG, "Uploading note");
            try {
                // Get paths for files of drawing and recordings
                File drawingFile = new File(getFilesDir(), drawing);
                List<String> audio = new ArrayList<String>();
                for (Parcelable info : infos) {
                    audio.add(((RecordInfo) info).getPath());
                }

                // Upload note
                newNote = uploadNote(title, drawingFile, audio, guid);
            } catch (EDAMUserException e) {
                // Remove Evernote credentials if authentication failed
                if (e.getErrorCode().equals(EDAMErrorCode.AUTH_EXPIRED)) {
                    mHandler.post(new DisplayToast(
                            "Evernote authentication expired. Please validate authentication in the Scribble Smart settings"));
                    clearEvernoteAuthentication();
                } else if (e.getErrorCode().equals(EDAMErrorCode.INVALID_AUTH)) {
                    mHandler.post(new DisplayToast(
                            "Evernote authentication invalid. Please validate authentication in the Scribble Smart settings"));
                    clearEvernoteAuthentication();
                } else if (e.getErrorCode().equals(EDAMErrorCode.QUOTA_REACHED)) {
                    mHandler.post(new DisplayToast(
                            "You reached your Evernote data quota for this month. Try again next month or update to premium"));
                } else {
                    mHandler.post(new DisplayToast("Couldn't upload note"));
                }
                Log.e(TAG, "Upload failed!", e);
            } catch (Exception e) {
                mHandler.post(new DisplayToast("Couldn't upload note"));
                Log.e(TAG, "Upload failed!", e);
            }

            // Save GUID if upload was successful
            if (newNote != null) {
                SharedPreferences settings = PreferenceManager
                        .getDefaultSharedPreferences(this);
                Editor editor = settings.edit();
                editor.putString(prefTitle
                        + NotesOverlayService.PREF_NOTE_GUID_SUFFIX,
                        newNote.getGuid());
                editor.commit();
            }
        } else if (intent.getAction().equals(ACTION_DELETE)) {
            String guid = intent.getStringExtra(EXTRA_NOTE_GUID);
            if (guid != null) {
                try {
                    // Delete note
                    session.createNoteStore().deleteNote(
                            session.getAuthToken(), guid);
                } catch (Exception e) {
                    Log.e(TAG, "Couldn't delete note");
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Remove Evernote credentials from preferences
     */
    private void clearEvernoteAuthentication() {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(this);
        Editor editor = settings.edit();
        editor.remove(SettingsActivity.PREF_AUTH_TOKEN);
        editor.remove(SettingsActivity.PREF_WEB_API_PREFIX);
        editor.remove(SettingsActivity.PREF_USER_ID);
        editor.remove(SettingsActivity.PREF_NOTE_STORE_URL);
        editor.commit();
    }

    /**
     * Setup the EvernoteSession used to access the Evernote API.
     */
    private void setupSession() {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(this);

        String authToken = settings.getString(SettingsActivity.PREF_AUTH_TOKEN,
                null);
        String noteStoreUrl = settings.getString(
                SettingsActivity.PREF_NOTE_STORE_URL, null);
        String webApiPrefix = settings.getString(
                SettingsActivity.PREF_WEB_API_PREFIX, null);
        int userId = settings.getInt(SettingsActivity.PREF_USER_ID, -1);

        ApplicationInfo info = getEvernoteApplicationInfo();

        AuthenticationResult authResult = new AuthenticationResult(authToken,
                noteStoreUrl, webApiPrefix, userId);
        session = new EvernoteSession(info, authResult, getCacheDir());
    }

    /**
     * Upload
     * 
     * @param title
     *            Title of the note
     * @param drawing
     *            File containing the drawing
     * @param audio
     *            List containing paths to recordings
     * @param guid
     *            Optional GUID of the note for update. null will create new
     *            note
     * @return newly created or updated note
     */
    public Note uploadNote(String title, File drawing, List<String> audio,
            String guid) throws IOException, TTransportException,
            EDAMUserException, EDAMSystemException, EDAMNotFoundException,
            TException {
        if (session.isLoggedIn()) {
            Map<Resource, String> resources = new HashMap<Resource, String>();

            // Hash the data in the image file. The hash is used to reference
            // the file in the ENML note content.
            InputStream in = new BufferedInputStream(new FileInputStream(
                    drawing));
            FileData data = new FileData(EDAMUtil.hash(in), drawing);
            in.close();

            // Create a new Resource for the image
            Resource drawingResource = new Resource();
            drawingResource.setData(data);
            drawingResource.setMime("image/png");
            resources.put(
                    drawingResource,
                    "<en-media type=\"image/png\" hash=\""
                            + EDAMUtil.bytesToHex(drawingResource.getData()
                                    .getBodyHash()) + "\"/>");

            if (audio != null) {
                for (String path : audio) {
                    File audioFile = new File(path);

                    // Hash the audio file. The hash is used to reference the
                    // file in the ENML note content.
                    in = new BufferedInputStream(new FileInputStream(audioFile));
                    data = new FileData(EDAMUtil.hash(in), audioFile);
                    in.close();

                    // Create a new Resource for each audio recording
                    Resource audioResource = new Resource();
                    audioResource.setData(data);
                    audioResource.setMime("audio/amr");
                    resources.put(
                            audioResource,
                            "<en-media type=\"audio/amr\" hash=\""
                                    + EDAMUtil.bytesToHex(audioResource
                                            .getData().getBodyHash()) + "\"/>");
                }
            }

            // Create a new Note
            Note note = new Note();
            if (note.getAttributes() == null) {
                note.setAttributes(new NoteAttributes());
            }
            note.getAttributes().setContentClass(CONTENT_CLASS);
            note.setTitle("Scribble Smart: " + title);

            StringBuilder resourceString = new StringBuilder();
            for (Entry<Resource, String> entry : resources.entrySet()) {
                resourceString.append(entry.getValue());
                note.addToResources(entry.getKey());
            }

            // Set the note's ENML content. Learn about ENML at
            // http://dev.evernote.com/documentation/cloud/chapters/ENML.php
            String content = NOTE_PREFIX + resourceString.toString()
                    + NOTE_SUFFIX;

            Log.i(TAG, "Created note with content: " + content);
            note.setContent(content);

            if (guid != null) {
                try {
                    session.createNoteStore().getNote(session.getAuthToken(),
                            guid, false, false, false, false);

                    // If we found the note update
                    note.setGuid(guid);
                    return session.createNoteStore().updateNote(
                            session.getAuthToken(), note);
                } catch (EDAMNotFoundException e) {
                    // If we couldn't find the note create a new one
                    return session.createNoteStore().updateNote(
                            session.getAuthToken(), note);
                }
            } else {
                // Create the note on the server. The returned Note object
                // will contain server-generated attributes such as the note's
                // unique ID (GUID), the Resource's GUID, and the creation and
                // update time.
                return session.createNoteStore().createNote(
                        session.getAuthToken(), note);
            }
        }
        return null;
    }

    /**
     * Construct ApplicationInfo needed by the Evernote API
     * 
     * @return
     */
    public static ApplicationInfo getEvernoteApplicationInfo() {
        return new ApplicationInfo(CONSUMER_KEY, CONSUMER_SECRET,
                EVERNOTE_HOST, APP_NAME, APP_VERSION);
    }

    /**
     * Get the auth token after successful authentication with Evernote This
     * method is here because SaveService is in the
     * com.evernote.client.oauth.android namespace and authtoken has default
     * access level
     * 
     * @return authtoken for Evernote API
     */
    public static EvernoteAuthToken getAuthToken() {
        return EvernoteOAuthActivity.authToken;
    }

    /**
     * Runnable for displaying a toast
     * 
     * @author markus
     *
     */
    private class DisplayToast implements Runnable {
        String mText;

        public DisplayToast(String text) {
            mText = text;
        }

        public void run() {
            Toast.makeText(SaveService.this, mText, Toast.LENGTH_SHORT).show();
        }
    }

}
