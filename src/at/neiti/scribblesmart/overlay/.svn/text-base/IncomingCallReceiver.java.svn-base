package at.neiti.scribblesmart.overlay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import at.neiti.scribblesmart.main.SettingsActivity;

/**
 * BroadCastReceiver which starts the NotesOverlayService when an incoming call
 * is received and tells the service to stop when the call has ended. It doesn't
 * stop the service! Service stops itself after handling save/etc. operations.
 * 
 * @author markus
 * 
 */
public class IncomingCallReceiver extends BroadcastReceiver {

    private static String TAG = "IncomingCallReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, NotesOverlayService.class);
        Bundle extras = intent.getExtras();

        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(context);
        boolean enabled = settings.getBoolean(
                SettingsActivity.PREF_SCRIBBLE_SMART, true);

        // Check if overlay is enabled in the app preferences
        if (enabled) {
            if (extras != null) {
                String state = extras.getString(TelephonyManager.EXTRA_STATE);
                if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                    Log.i(TAG, "State ringing => starting service");

                    // Get phone number and start overlay service
                    String incomingNumber = extras
                            .getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    serviceIntent.putExtra(
                            TelephonyManager.EXTRA_INCOMING_NUMBER,
                            incomingNumber);
                    context.startService(serviceIntent);
                } else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                    Log.i(TAG, "State idle => stopping service");

                    // Tell service to stop overlay itself
                    NotesOverlayService.stopInstance();
                }
            }
        }
    }

}