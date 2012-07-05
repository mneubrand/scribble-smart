package at.neiti.scribblesmart;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import at.neiti.scribblesmart.main.SettingsActivity;
import at.neiti.scribblesmart.overlay.NotesOverlayService;
import at.neiti.scribblesmart.ui.NotesView;

import com.evernote.client.conn.ApplicationInfo;
import com.evernote.client.oauth.android.AuthenticationResult;
import com.evernote.client.oauth.android.EvernoteSession;
import com.evernote.client.oauth.android.SaveService;
import com.evernote.edam.error.EDAMErrorCode;
import com.evernote.edam.error.EDAMUserException;

/**
 * Helper class for dealing with notes, recordings and EverNote API
 * 
 * @author markus
 * 
 */
public class NoteUtils {

    private static final String TAG = "NoteUtils";

    public static final String NOTE_MODIFIED = "NOTE_MODIFIED";
    public static final String NOTE_NUMBER = "NOTE_NUMBER";
    public static final String NOTE_DRAWING = "NOTE_DRAWING";
    public static final String NOTE_AUDIO_FILE = "NOTE_AUDIO_FILE";
    public static final String NOTE_GUID = "NOTE_GUID";

    /**
     * Create title of a note containing contact name if available, phone number
     * if available or "Unknown Number" if neither is available
     * 
     * @param context
     * @param incomingNumber
     *            optional phone number of the call for which the note was
     *            created
     * @return title
     */
    public static String buildTitle(Context context, String incomingNumber) {
        if (incomingNumber == null) {
            return "Unknown Number";
        }

        String name = incomingNumber;

        // define the columns I want the query to return
        String[] projection = new String[] {
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup._ID };

        // encode the phone number and build the filter URI
        Uri contactUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(incomingNumber));

        // query time
        Cursor cursor = context.getContentResolver().query(contactUri,
                projection, null, null, null);

        if (cursor.moveToFirst()) {
            // Get values from contacts database:
            name = cursor.getString(cursor
                    .getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
            Log.i(TAG, "Found contact result for number: " + name);
        }
        return name;
    }

    /**
     * Creates String which contains all information for a note for persistence
     * 
     * @param context
     * @param incomingNumber
     *            phone number of the call for which the note was created
     * @param drawing
     *            path to drawing for the note
     * @param infos
     *            array of RecordInfo objects for recordings of the note
     * @return note info
     */
    public static String createNoteInfo(Context context, String incomingNumber,
            String drawing, Parcelable[] infos) {
        StringBuilder ret = new StringBuilder();
        ret.append(NOTE_MODIFIED + ":" + System.currentTimeMillis());
        ret.append("," + NOTE_NUMBER + ":"
                + buildTitle(context, incomingNumber));
        ret.append("," + NOTE_DRAWING + ":" + drawing);

        if (infos != null) {
            int i = 0;
            for (Parcelable info : infos) {
                String description = ((RecordInfo) info).getPath() + "|"
                        + ((RecordInfo) info).getTotal();
                ret.append("," + NOTE_AUDIO_FILE + "_" + (i++) + ":"
                        + description);
            }
        }

        return ret.toString();
    }

    /**
     * Parses a persisted note info String and returns a map for all its
     * properties
     * 
     * @param context
     * @param key
     *            key under which the note was persisted
     * @return
     * 
     * @see NoteUtils#createNoteInfo(Context, String, String, Parcelable[])
     */
    public static Map<String, String> getNoteInfo(Context context, String key) {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(context);
        String noteInfo = settings.getString(key, null);

        if (noteInfo != null) {
            Map<String, String> properties = new HashMap<String, String>();
            String[] pairs = noteInfo.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split(":");
                properties.put(keyValue[0], keyValue[1]);
            }

            String guid = settings.getString(key
                    + NotesOverlayService.PREF_NOTE_GUID_SUFFIX, null);
            if (guid != null) {
                properties.put(NOTE_GUID, guid);
            }

            return properties;
        } else {
            return null;
        }
    }

    /**
     * Refresh the given list ListView with the info from the RecordInfo List
     * 
     * @param context
     * @param recordings
     *            ListView to be refreshed
     * @param recordingInfos
     *            List of RecordInfo objects which should populate the ListView
     */
    public static void refreshRecordings(final Context context,
            final ListView recordings, final List<RecordInfo> recordingInfos) {
        String[] from = new String[] { "index", "path", "length" };
        int[] to = new int[] { R.id.record_index, R.id.record_path,
                R.id.record_length };

        // Convert to a format which can be used by a SimpleAdapter
        List<Map<String, String>> recordingItems = new ArrayList<Map<String, String>>();
        int i = 1;
        for (RecordInfo item : recordingInfos) {
            Map<String, String> map = new HashMap<String, String>();
            map.put("index", (i++) + ".");
            map.put("path", item.getPath());
            String sec = item.getTotal();
            while (sec.length() < 2) {
                sec = " " + sec;
            }
            map.put("length", sec + " sec");
            recordingItems.add(map);
        }

        // Uncomment to test scrolling
        // for (int j = 0; j < 20; j++) {
        // Map<String, String> map = new HashMap<String, String>();
        // map.put("index", "Test " + j);
        // map.put("path", "test");
        // map.put("modified", "xyz");
        // recordingItems.add(map);
        // }

        recordings.setAdapter(new SimpleAdapter(context, recordingItems,
                R.layout.record_item, from, to) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                final TextView pathView = (TextView) view
                        .findViewById(R.id.record_path);

                RecordItemOnClickListener onClickListener = new RecordItemOnClickListener(
                        context, pathView.getText().toString(), recordings,
                        recordingInfos);

                ImageButton play = (ImageButton) view
                        .findViewById(R.id.record_play);
                play.setOnClickListener(onClickListener);

                ImageButton cancel = (ImageButton) view
                        .findViewById(R.id.record_cancel);

                if (context instanceof NotesOverlayService) {
                    cancel.setOnClickListener(onClickListener);
                } else {
                    cancel.setVisibility(View.GONE);
                }

                return view;
            }

        });

        recordings.getParent().requestLayout();
    }

    /**
     * Load a given note into the NotesView and ListView given
     * 
     * @param context
     * @param noteInfo
     *            note info map from getNoteInfo
     * @param notesView
     *            NotesView to display the drawing
     * @param recordings
     *            ListView to display all recordings of the note
     * 
     * @see NoteUtils#getNoteInfo(Context, String)
     * 
     * @return Parsed RecordInfos from the noteInfo map
     */
    public static List<RecordInfo> loadNote(Context context,
            Map<String, String> noteInfo, NotesView notesView,
            ListView recordings) {
        // Load drawing if it exists
        File drawingFile = new File(context.getFilesDir(),
                noteInfo.get(NoteUtils.NOTE_DRAWING));
        Bitmap immutableDrawing = BitmapFactory.decodeFile(drawingFile
                .getAbsolutePath());
        if (immutableDrawing != null) {
            Bitmap mutableDrawing = immutableDrawing.copy(
                    Bitmap.Config.ARGB_8888, true);
            notesView.setDrawing(mutableDrawing);
        }

        // Add audio files if they exist
        List<RecordInfo> recordingInfos = new ArrayList<RecordInfo>();
        for (Entry<String, String> entry : noteInfo.entrySet()) {
            if (entry.getKey().startsWith(NoteUtils.NOTE_AUDIO_FILE)) {
                String[] info = entry.getValue().split("\\|");
                if (new File(info[0]).exists()) {
                    recordingInfos.add(new RecordInfo(info[0], info[1]));
                }
            }
        }

        NoteUtils.refreshRecordings(context, recordings, recordingInfos);
        return recordingInfos;
    }

    /**
     * Method to validate the EverNote credentials stored in the preferences on
     * a AsyncTask in the background
     * 
     * @param context
     * @param callback
     *            callback to be invoked when validation of the credentials
     *            finished
     */
    public static void validateEvernoteAuthentication(Context context,
            AuthenticationCallback callback) {
        new AuthenticationTask(callback).execute(context);
    }

    /**
     * AsyncTask to validate Evernote credentials
     * 
     * @author markus
     * 
     */
    private static class AuthenticationTask extends
            AsyncTask<Context, Void, Integer> {

        private AuthenticationCallback callback;

        public AuthenticationTask(AuthenticationCallback callback) {
            this.callback = callback;
        }

        @Override
        protected Integer doInBackground(Context... params) {
            try {
                //Load credentials from preferences
                SharedPreferences settings = PreferenceManager
                        .getDefaultSharedPreferences(params[0]);
                String authToken = settings.getString(
                        SettingsActivity.PREF_AUTH_TOKEN, null);
                String noteStoreUrl = settings.getString(
                        SettingsActivity.PREF_NOTE_STORE_URL, null);
                String webApiPrefix = settings.getString(
                        SettingsActivity.PREF_WEB_API_PREFIX, null);
                int userId = settings.getInt(SettingsActivity.PREF_USER_ID,
                        -1);

                //Generate session
                ApplicationInfo info = SaveService.getEvernoteApplicationInfo();
                AuthenticationResult authResult = new AuthenticationResult(
                        authToken, noteStoreUrl, webApiPrefix, userId);
                EvernoteSession session = new EvernoteSession(info, authResult,
                        params[0].getCacheDir());

                // Invoke getSyncState method to trigger authentication failure
                // in case something goes wrong
                session.createNoteStore().getSyncState(session.getAuthToken());
                return AuthenticationCallback.AUTHENTICATION_SUCCESSFUL;
            } catch (EDAMUserException e) {
                // Only return authentication failed if the received error code
                // is AUTH_EXPIRED or INVALID_AUTH. Return
                // AUTHENTICATION_UNKNOWN otherwise
                if (e.getErrorCode().equals(EDAMErrorCode.AUTH_EXPIRED)
                        || e.getErrorCode().equals(EDAMErrorCode.INVALID_AUTH)) {
                    return AuthenticationCallback.AUTHENTICATION_FAILED;
                }
                return AuthenticationCallback.AUTHENTICATION_UNKNOWN;
            } catch (Exception e) {
                return AuthenticationCallback.AUTHENTICATION_UNKNOWN;
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            //Invoke callback after finished
            callback.onResult(result);
        }

    }

}
