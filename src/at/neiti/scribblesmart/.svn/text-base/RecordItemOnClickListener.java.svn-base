package at.neiti.scribblesmart;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import at.neiti.scribblesmart.overlay.NotesOverlayService;

/**
 * OnClickListener for ListView displaying recordings. Handles playing of
 * recordings and deletion of recordings
 * 
 * @author markus
 * 
 */
public class RecordItemOnClickListener implements OnClickListener {

    private ListView recordings;
    private String path;
    private Context context;
    private List<RecordInfo> recordingInfos;

    private MediaPlayer player;
    private int stream = AudioManager.STREAM_VOICE_CALL;

    public RecordItemOnClickListener(Context context, String path,
            ListView recordings, List<RecordInfo> recordingInfos) {
        this.path = path;
        this.context = context;
        this.recordings = recordings;
        this.recordingInfos = recordingInfos;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.record_play) {
            handlePlay((ImageButton) v);
        } else if (v.getId() == R.id.record_cancel) {
            handleCancel();
        }

    }

    /**
     * Delete recording and remove it from the list
     */
    private void handleCancel() {
        new File(path).delete();

        Iterator<RecordInfo> it = recordingInfos.iterator();
        while (it.hasNext()) {
            RecordInfo record = it.next();
            if (record.getPath().equals(path)) {
                it.remove();
            }
        }

        if (context instanceof NotesOverlayService) {
            ((NotesOverlayService) context).setModified(true);
        }

        NoteUtils.refreshRecordings(context, recordings, recordingInfos);
    }

    /**
     * Play the recording and change the buttons icon to stop
     * 
     * @param button
     */
    private void handlePlay(final ImageButton button) {
        // Get the AudioManager
        final AudioManager audioManager = (AudioManager) context
                .getSystemService(Context.AUDIO_SERVICE);

        if (player == null) {
            button.setImageResource(R.drawable.stop);
            player = new MediaPlayer();

            // Add OnCompletionListener to reset button icon and mode of the
            // audio manager
            player.setOnCompletionListener(new OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    audioManager.setMode(AudioManager.MODE_IN_CALL);
                    audioManager.setSpeakerphoneOn(false);
                    stop(button);
                }

            });

            // Set the volume of played media to maximum.
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.setStreamVolume(stream,
                    audioManager.getStreamMaxVolume(stream),
                    AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            audioManager.setSpeakerphoneOn(true);

            player.setAudioStreamType(stream);
            try {
                new File(path).setReadable(true, false);
                player.setDataSource(path);
                player.prepare();
                player.start();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            stop(button);
        }
    }

    /**
     * Stop playback of the recording and reset buttton icon
     * 
     * @param button
     */
    private void stop(ImageButton button) {
        button.setImageResource(R.drawable.play);
        player.stop();
        player.release();
        player = null;
    }

}