package com.pcchin.wearrecorder;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.pcchin.wearrecorder.republicofgavin.PauseResumeAudioRecorder;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private PauseResumeAudioRecorder recorder;

    private Handler timeHandler = new Handler();
    private Runnable timeRunnable;

    private TextView text;
    private ImageButton record, pause, stop;

    private long startTime;
    private long totalPauseTime = 0;
    private long lastPause;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Modified from https://github.com/republicofgavin/PauseResumeAudioRecorder
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        timeRunnable = new Runnable() {
            @Override
            public void run() {
                ((TextView) findViewById(R.id.time)).setText(millisToString(
                        System.currentTimeMillis() - startTime - totalPauseTime));
                timeHandler.postDelayed(this, 0);
            }
        };

        text = findViewById(R.id.time);
        record = findViewById(R.id.record);
        pause = findViewById(R.id.pause);
        stop = findViewById(R.id.stop);

        record.setVisibility(View.VISIBLE);
        pause.setVisibility(View.GONE);
        stop.setVisibility(View.GONE);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void onRecordPressed(View view) {
        Log.i("FUNCTION CALL", "Record pressed");
        record.setVisibility(View.GONE);
        pause.setVisibility(View.VISIBLE);
        pause.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause));
        pause.setContentDescription(getString(R.string.pause));
        stop.setVisibility(View.VISIBLE);

        // Set starting time
        startTime = System.currentTimeMillis();
        timeHandler.postDelayed(timeRunnable, 0);

        // Check for output folder
        File outputFolder = new File("/storage/emulated/0/Recordings");
        if (! ((outputFolder.exists() && outputFolder.isDirectory()))) {
            outputFolder.mkdirs();
        }

        // Check if file name exists
        String outputFileName = new SimpleDateFormat("yyyyMMdd_kkmmss", Locale.ENGLISH)
                .format(new Date());
        if (new File(outputFolder.getAbsolutePath() + "/" + outputFileName + ".wav").exists()) {
            String tempFileName = outputFileName;
            int index = 0;
            while (new File(tempFileName).exists()) {
                tempFileName = outputFileName + "_" + Integer.toString(index);
                index++;
            }
            outputFileName = tempFileName;
        }

        // Set up recorder
        recorder = new PauseResumeAudioRecorder();
        recorder.setAudioFile(outputFolder.getAbsolutePath() + "/" + outputFileName + ".wav");
        recorder.startRecording();
    }

    public void onPauseResumePressed(View view) {
        if (view.getContentDescription().toString().equals(getString(R.string.pause))) {
            Log.i("FUNCTION CALL", "Pause pressed");
            // Pause recording
            lastPause = System.currentTimeMillis();
            ((ImageView) view).setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_play));
            view.setContentDescription(getString(R.string.resume));
            timeHandler.removeCallbacks(timeRunnable);

            // Save recording to temp location
            recorder.pauseRecording();
        } else {
            Log.i("FUNCTION CALL", "Resume pressed");
            // Resume recording
            totalPauseTime += System.currentTimeMillis() - lastPause;
            ((ImageView) view).setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause));
            view.setContentDescription(getString(R.string.pause));
            timeHandler.postDelayed(timeRunnable, 0);
            recorder.resumeRecording();
        }
    }

    public void onStopPressed(View view) {
        Log.i("FUNCTION CALL", "Stop pressed");
        timeHandler.removeCallbacks(timeRunnable);
        record.setVisibility(View.VISIBLE);
        pause.setVisibility(View.GONE);
        stop.setVisibility(View.GONE);
        text.setText(R.string.zero);
        totalPauseTime = 0;

        // Stop recording
        if (recorder.getCurrentState() == PauseResumeAudioRecorder.PAUSED_STATE) {
            recorder.resumeRecording();
        }
        recorder.stopRecording();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (recorder.getCurrentState()==PauseResumeAudioRecorder.RECORDING_STATE
                || recorder.getCurrentState()==PauseResumeAudioRecorder.PAUSED_STATE) {
            recorder.stopRecording();
        }
    }

    @NotNull
    private String millisToString(long original) {
        double millis = original % 1000;
        original = (int) Math.floor((original - millis) / 1000);
        double secs = original % 60;
        original = (int) Math.floor((original - secs) / 60);
        double mins = (int) original % 60;
        int hrs = (int) Math.floor((original - mins) / 60);
        return String.format(Locale.ENGLISH, "%d:%02d:%02d:%03d",
                hrs, (int) mins, (int) secs,(int) millis);
    }
}
