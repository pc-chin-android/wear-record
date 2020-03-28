package com.pcchin.wearrecorder;

import android.app.Activity;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.tracks.AACTrackImpl;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private boolean isRecording;
    private MediaRecorder recorder;
    private String outputFileName;
    private File outputFolder;

    private Handler timeHandler = new Handler();
    private Runnable timeRunnable;

    private TextView text;
    private ImageButton record;
    private ImageButton pause;
    private ImageButton stop;

    private long startTime;
    private long totalPauseTime = 0;
    private long lastPause;
    private int totalPause = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

    public void onRecordPressed(View view) {
        record.setVisibility(View.GONE);
        pause.setVisibility(View.VISIBLE);
        pause.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause));
        pause.setContentDescription(getString(R.string.pause));
        stop.setVisibility(View.VISIBLE);

        // Set starting time
        totalPause = 0;
        startTime = System.currentTimeMillis();
        timeHandler.postDelayed(timeRunnable, 0);

        // Check for output folder
        outputFolder = new File("/storage/emulated/0/Music");
        if (! ((outputFolder.exists() && outputFolder.isDirectory()) && outputFolder.mkdir())) {
            // Alternate directory at AudioRecordings if folder failed to create file
            outputFolder = new File("/storage/emulated/0/AudioRecordings");
            if (! ((outputFolder.exists() && outputFolder.isDirectory()) && outputFolder.mkdir())) {
                // Root directory as storage if failed to create folder at alt directory
                outputFolder = new File("/storage/emulated/0");
            }
        }

        // Check if file name exists
        outputFileName = new SimpleDateFormat("yyyyMMdd_kkmmss", Locale.ENGLISH)
                .format(new Date());
        if (new File(outputFolder.getAbsolutePath() + "/" + outputFileName + ".aac").exists()) {
            String tempFileName = outputFileName;
            int index = 0;
            while (new File(tempFileName).exists()) {
                tempFileName = outputFileName + "_" + Integer.toString(index);
                index++;
            }
            outputFileName = tempFileName;
        }

        // Set up recorder
        recorder = new MediaRecorder();
        startRecording();
    }

    public void onPauseResumePressed(View view) {
        if (view.getContentDescription().toString().equals(getString(R.string.pause))) {
            // Pause recording
            lastPause = System.currentTimeMillis();
            ((ImageView) view).setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_play));
            view.setContentDescription(getString(R.string.resume));
            timeHandler.removeCallbacks(timeRunnable);

            // Save recording to temp location
            recorder.stop();
            isRecording = false;
        } else {
            // Resume recording
            totalPause++;
            totalPauseTime += System.currentTimeMillis() - lastPause;
            ((ImageView) view).setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause));
            view.setContentDescription(getString(R.string.pause));
            timeHandler.postDelayed(timeRunnable, 0);
            startRecording();
        }
    }

    public void onStopPressed(View view) {
        timeHandler.removeCallbacks(timeRunnable);
        record.setVisibility(View.VISIBLE);
        pause.setVisibility(View.GONE);
        stop.setVisibility(View.GONE);
        text.setText(R.string.zero);
        totalPauseTime = 0;

        // Merge all recordings
        if (isRecording) {
            recorder.stop();
            isRecording = false;
        }
        recorder.release();
        new Thread(new Runnable() {
            @Override
            public void run() {
                mergeAudio();
            }
        }).start();
    }

    private void startRecording() {
        try {
            System.out.println("MANUAL LOG: Recording: File name is " + getFilesDir().getAbsolutePath()
                    + "/" + outputFileName
                    + Integer.toString(totalPause) + ".aac");
            recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setOutputFile(getFilesDir().getAbsolutePath() + "/" + outputFileName
                    + Integer.toString(totalPause) + ".aac");
            recorder.prepare();
            recorder.start();
            isRecording = true;
        } catch (IOException e) {
            e.printStackTrace();
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
