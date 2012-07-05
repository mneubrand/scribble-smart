package at.neiti.scribblesmart.overlay;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import at.neiti.scribblesmart.R;
import at.neiti.scribblesmart.RecordInfo;

/**
 * Recorder class representing the audio recording dialog. Uses MediaRecorder to
 * record from the microphone to files in amr format.
 * 
 * @author markus
 * 
 */
public class AudioRecorder implements OnClickListener {

    private static final String TAG = "AudioRecorder";

    private NotesOverlayService service;

    private View audioDialog;

    private ImageButton dialogConfirm;
    private ImageButton dialogCancel;

    private TextView recordTime;

    private MediaRecorder recorder;

    private boolean isRecording;

    private List<RecordInfo> recordings;

    // Path to file which is currently recorded to
    private String currentFile;

    // Timer to handle UI updates of recording time
    private Timer timer = new Timer();

    // Time (System.currentTimeInMillis()) when the last recording started
    private long recordingStart;

    // Handler used to post UI updates of recording time from background timer
    private Handler handler;

    public AudioRecorder(NotesOverlayService service, View mainView,
            List<RecordInfo> recordings) {
        this.service = service;
        this.recordings = recordings;

        this.handler = new Handler();

        // Initialize UI
        audioDialog = mainView.findViewById(R.id.audio_dialog);

        dialogConfirm = (ImageButton) mainView
                .findViewById(R.id.dialog_confirm);
        dialogConfirm.setOnClickListener(this);
        dialogCancel = (ImageButton) mainView.findViewById(R.id.dialog_cancel);
        dialogCancel.setOnClickListener(this);

        recordTime = (TextView) mainView.findViewById(R.id.record_time);
    }

    /**
     * Start recording to file
     */
    private void startRecording() {
        resetRecordTimer();

        // Release old recorder
        if (recorder != null) {
            recorder.release();
        }

        // Prepare recorder
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        // Create temporary file to record to in application cache directory
        File temp = new File(service.getFilesDir(), UUID.randomUUID()
                .toString());
        currentFile = temp.getAbsolutePath();

        recorder.setOutputFile(currentFile);
        Log.i(TAG, "Recording to " + currentFile);

        try {
            // Start recording
            recorder.prepare();
            recorder.start();
            recordingStart = System.currentTimeMillis();
            isRecording = true;
            Log.i(TAG, "Recording started");
        } catch (IllegalStateException e) {
            Log.i(TAG, "Recording failed", e);
        } catch (IOException e) {
            Log.i(TAG, "Recording failed", e);
        }

        // Start background task to update recording time UI.
        startRecordTimer();
    }

    /**
     * Stops recording and deletes the generated file
     */
    private void removeCurrentRecording() {
        if (isRecording) {
            recorder.stop();
            new File(currentFile).delete();
        }
    }

    /**
     * Starts timer task to update UI with elapsed seconds since recording start
     */
    private void startRecordTimer() {
        TimerTask update = new TimerTask() {
            @Override
            public void run() {
                long total = (System.currentTimeMillis() - recordingStart) / 1000;
                final String seconds = total % 60 >= 10 ? "" + (total % 60)
                        : "0" + (total % 60);
                final String minutes = total / 60l >= 10 ? "" + (total / 60l)
                        : "0" + (total / 60l);

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        recordTime.setText(minutes + ":" + seconds);
                    }
                });
            }
        };
        // Update UI every second
        timer.schedule(update, 1000, 1000);
    }

    /**
     * Reset record time UI to 00:00
     */
    private void resetRecordTimer() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                recordTime.setText("00:00");
            }
        });
        timer.cancel();
        timer = new Timer();
    }

    /**
     * Stop recording and add info of the recording to the recordings list
     */
    private void stopRecording() {
        if (recorder != null && isRecording) {
            recorder.stop();
            String total = Long
                    .toString((System.currentTimeMillis() - recordingStart) / 1000);
            service.setModified(true);
            recordings.add(new RecordInfo(currentFile, total));
            isRecording = false;
        }
    }

    @Override
    public void onClick(View v) {
        if (dialogConfirm.equals(v)) {
            Log.i(TAG, "Stop recording");
            stopRecording();
        } else if (dialogCancel.equals(v)) {
            Log.i(TAG, "Cancel recording");
            removeCurrentRecording();
        }
        dismissAudioDialog();
    }

    /**
     * Dismiss the audio dialog and inform NotesOverlayService of it
     */
    private void dismissAudioDialog() {
        audioDialog.setVisibility(View.GONE);
        service.dismissedAudioDialog();
    }

    /**
     * Show the audio dialog and inform NotesOverlayService of it
     */
    public void showAudioDialog() {
        audioDialog.setVisibility(View.VISIBLE);
        startRecording();
    }

    public List<RecordInfo> getRecordings() {
        return this.recordings;
    }

    /**
     * Free all resources
     */
    public void stop() {
        if (recorder != null) {
            recorder.reset();
            recorder.release();
        }
    }

}
