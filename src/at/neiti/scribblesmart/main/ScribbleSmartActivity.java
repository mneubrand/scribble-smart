package at.neiti.scribblesmart.main;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.ViewFlipper;
import at.neiti.scribblesmart.NoteUtils;
import at.neiti.scribblesmart.R;
import at.neiti.scribblesmart.overlay.NotesOverlayService;
import at.neiti.scribblesmart.ui.NotesView;

import com.evernote.client.oauth.android.SaveService;

/**
 * Main activity to browse/display notes and display the about dialog
 * 
 * @author markus
 * 
 */
public class ScribbleSmartActivity extends ListActivity implements
        OnClickListener {

    private static final String TAG = "ScribbleSmartActivity";

    private SimpleDateFormat dateFormatter;

    private ViewFlipper main;
    private ImageButton back;
    private TextView title;
    private NotesView notesView;
    private ListView recordings;
    private ImageButton back2;
    private ImageButton delete;
    private WebView about;

    // Uncomment to start overlay from application menu
    // private MenuItem menuStart;
    // private MenuItem menuStop;

    private MenuItem menuPreferences;
    private MenuItem menuAbout;

    // Key of the currently loaded note in the detail view
    private String currentKey;

    private AnimationInProgressListener animationListener;

    // Indicates if the ViewFlipper is animating between views right now
    private boolean animating;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(this);

        // Check if it is the first run of the application
        if (settings.getBoolean(SettingsActivity.PREF_FIRST_START, true)) {
            // Populate settings with default values
            Editor editor = settings.edit();
            editor.putBoolean(SettingsActivity.PREF_FIRST_START, false);
            editor.putBoolean(SettingsActivity.PREF_EVERNOTE, false);
            editor.putBoolean(SettingsActivity.PREF_SCRIBBLE_SMART, true);
            editor.putBoolean(SettingsActivity.PREF_SPEAKER_PHONE, true);
            editor.commit();

            // Ask user if he wants to display the configuration options
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("This seems to be the first time you launched Scribble Smart. Do you want to see the configuration options?");
            builder.setCancelable(false);
            builder.setPositiveButton("Yes",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            launchSettings();
                        }
                    });
            builder.setNegativeButton("No",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }

        this.dateFormatter = new SimpleDateFormat("MM/dd/yy hh:mm aa");

        // Initialize UI
        setContentView(R.layout.main);

        main = (ViewFlipper) findViewById(R.id.main);
        animationListener = new AnimationInProgressListener();
        main.getInAnimation().setAnimationListener(animationListener);

        back = (ImageButton) findViewById(R.id.button_back);
        back.setOnClickListener(this);

        back2 = (ImageButton) findViewById(R.id.button_back2);
        back2.setOnClickListener(this);

        notesView = (NotesView) findViewById(R.id.notes_view);
        notesView.setEnabled(false);

        recordings = (ListView) findViewById(R.id.notes_recordings);
        recordings.addFooterView(new View(this));

        delete = (ImageButton) findViewById(R.id.button_delete);
        delete.setOnClickListener(this);

        title = (TextView) findViewById(R.id.title);

        // Load about.html
        about = (WebView) main.findViewById(R.id.about);
        about.getSettings().setJavaScriptEnabled(true);
        about.addJavascriptInterface(this, "android");
        about.loadUrl("file:///android_asset/about.html");
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshListView();
    }

    /**
     * Refresh the ListView containing all notes
     */
    private void refreshListView() {
        List<Map<String, String>> noteItems = new ArrayList<Map<String, String>>();

        // Load notes from settings
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(this);
        Map<String, ?> settingsMap = settings.getAll();
        for (Entry<String, ?> entry : settingsMap.entrySet()) {
            if (entry.getKey().startsWith(NotesOverlayService.PREF_NOTE_PREFIX)
                    && !entry.getKey().endsWith(
                            NotesOverlayService.PREF_NOTE_GUID_SUFFIX)) {
                Log.i(TAG, "Adding note for key " + entry.getKey());

                Map<String, String> map = new HashMap<String, String>();
                map.put("id", entry.getKey());
                Map<String, String> noteInfo = NoteUtils.getNoteInfo(this,
                        entry.getKey());
                String modified = noteInfo.get(NoteUtils.NOTE_MODIFIED);
                if (modified != null) {
                    modified = dateFormatter.format(new Date(Long
                            .parseLong(modified)));
                }
                String title = noteInfo.get(NoteUtils.NOTE_NUMBER)
                        + (modified != null ? " at " + modified : "");
                map.put("title", title);
                map.put("modified", modified);
                noteItems.add(map);
            }
        }

        // Uncomment to test scrolling
        // for (int i = 0; i < 20; i++) {
        // Map<String, String> map = new HashMap<String, String>();
        // map.put("id", "test");
        // map.put("title", "Test " + i);
        // map.put("modified", "05/22/12 05:04 PM");
        // noteItems.add(map);
        // }

        // Sort notes by date
        Collections.sort(noteItems, new Comparator<Map<String, String>>() {

            @Override
            public int compare(Map<String, String> lhs, Map<String, String> rhs) {
                try {
                    Long l1 = lhs.get("modified") != null ? dateFormatter
                            .parse(lhs.get("modified")).getTime() : 0;
                    Long l2 = rhs.get("modified") != null ? dateFormatter
                            .parse(rhs.get("modified")).getTime() : 0;
                    // Newer items first
                    return l2.compareTo(l1);
                } catch (ParseException e) {
                    return 0;
                }
            }

        });

        // Set new ListAdapter
        String[] from = new String[] { "id", "title" };
        int[] to = new int[] { R.id.note_info, R.id.note_title };
        setListAdapter(new NoteListAdapter(this, noteItems, R.layout.list_item,
                from, to));
    }

    @Override
    public void onClick(View v) {
        if (v.equals(delete)) {
            // Handle note deletion from detail screen
            deleteNote(currentKey);
            refreshListView();

            main.showPrevious();
            main.setInAnimation(this, R.animator.in_from_right);
            main.getInAnimation().setAnimationListener(animationListener);
            main.setOutAnimation(this, R.animator.out_to_left);
        } else if (v.equals(back) || v.equals(back2)) {
            // Go back from about screen or detail screen to main screen
            main.setDisplayedChild(0);
            main.setInAnimation(this, R.animator.in_from_right);
            main.getInAnimation().setAnimationListener(animationListener);
            main.setOutAnimation(this, R.animator.out_to_left);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.application_menu, menu);

        menuAbout = menu.findItem(R.id.menu_about);
        menuPreferences = menu.findItem(R.id.menu_preferences);
        // Uncomment to start overlay from application menu
        // menuStart = menu.findItem(R.id.menu_start);
        // menuStop = menu.findItem(R.id.menu_stop);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Uncomment to start overlay from application menu
        // if (menuStart.equals(item)) {
        // Intent serviceIntent = new Intent(this, NotesOverlayService.class);
        // serviceIntent.putExtra(TelephonyManager.EXTRA_INCOMING_NUMBER,
        // "1234");
        // this.startService(serviceIntent);
        // } else if (menuStop.equals(item)) {
        // NotesOverlayService.stopInstance();
        // refreshListView();
        // }
        if (menuPreferences.equals(item)) {
            launchSettings();
        } else if (menuAbout.equals(item)) {
            main.setDisplayedChild(2);
            main.setInAnimation(ScribbleSmartActivity.this,
                    R.animator.in_from_left);
            main.setOutAnimation(ScribbleSmartActivity.this,
                    R.animator.out_to_right);
        }
        return true;
    }

    /**
     * Launch SettingsActivity
     */
    private void launchSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Delete a note from preferences and all of its files (drawings and
     * recordings)
     * 
     * @param key
     *            preference key of the note
     */
    private void deleteNote(String key) {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(this);
        Map<String, String> noteInfo = NoteUtils.getNoteInfo(this, key);

        new File(getFilesDir(), noteInfo.get(NoteUtils.NOTE_DRAWING)).delete();
        for (Entry<String, String> entry : noteInfo.entrySet()) {
            if (entry.getKey().startsWith(NoteUtils.NOTE_AUDIO_FILE)) {
                String[] info = entry.getValue().split("\\|");
                new File(info[0]).delete();
            }
        }

        Editor editor = settings.edit();
        editor.remove(key);
        editor.remove(key + NotesOverlayService.PREF_NOTE_GUID_SUFFIX);
        editor.commit();

        Intent serviceIntent = new Intent(this, SaveService.class);
        serviceIntent.setAction(SaveService.ACTION_DELETE);
        if (noteInfo != null) {
            serviceIntent.putExtra(SaveService.EXTRA_NOTE_GUID,
                    noteInfo.get(NoteUtils.NOTE_GUID));
        }
        startService(serviceIntent);

        refreshListView();
    }

    /**
     * Open URL in Browser
     * 
     * @param url
     */
    public void openURL(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(i);
    }

    /**
     * Open compose email application with prepopulated destination
     * 
     * @param email
     */
    public void openEmail(String email) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:" + email));
        startActivity(i);
    }

    /**
     * Open the apps details page in Google Play
     */
    public void openMarket() {
        Intent i = new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=at.neiti.scribblesmart"));
        startActivity(i);
    }

    /**
     * ListAdapter for handling events on the notes overview ListView
     * 
     * @author markus
     * 
     */
    private class NoteListAdapter extends SimpleAdapter {

        public NoteListAdapter(Context context,
                List<? extends Map<String, ?>> data, int resource,
                String[] from, int[] to) {
            super(context, data, resource, from, to);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view = super.getView(position, convertView, parent);

            // Get note info
            final TextView noteInfoView = (TextView) view
                    .findViewById(R.id.note_info);
            final String key = noteInfoView.getText().toString();

            // Make item clickable and transition to next page if a row gets
            // pressed
            view.setClickable(true);
            view.setFocusable(true);
            view.setOnTouchListener(new OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // Prevent double tap
                    if (!animating) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            v.setBackgroundResource(R.color.pressed);
                        } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                            v.setBackgroundResource(R.color.transparent);
                        } else if (event.getAction() == MotionEvent.ACTION_UP) {
                            v.setBackgroundResource(R.color.transparent);

                            Map<String, String> noteInfo = NoteUtils
                                    .getNoteInfo(ScribbleSmartActivity.this,
                                            key);
                            NoteUtils.loadNote(ScribbleSmartActivity.this,
                                    noteInfo, notesView, recordings);

                            String text = ((TextView) view
                                    .findViewById(R.id.note_title)).getText()
                                    .toString();
                            text = text.substring(0, text.indexOf(" at "));
                            title.setText(text);

                            currentKey = key;

                            main.showNext();
                            main.setInAnimation(ScribbleSmartActivity.this,
                                    R.animator.in_from_left);
                            main.setOutAnimation(ScribbleSmartActivity.this,
                                    R.animator.out_to_right);
                        }
                        return true;
                    }
                    return false;
                }

            });

            // Handle deletion of a note
            ImageButton delete = (ImageButton) view
                    .findViewById(R.id.note_delete);
            delete.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    Log.i(TAG, "Deleting " + key);
                    deleteNote(key);
                }
            });

            return view;
        }

    }

    /**
     * AnimationListener to set animating boolean which is used to prevent
     * double taps
     * 
     * @author markus
     * 
     */
    private class AnimationInProgressListener implements AnimationListener {

        @Override
        public void onAnimationStart(Animation animation) {
            animating = true;
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            animating = false;
        }
    }

}