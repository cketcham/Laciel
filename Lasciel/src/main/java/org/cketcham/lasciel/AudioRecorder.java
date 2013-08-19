package org.cketcham.lasciel;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

/**
 * Created by cketcham on 8/18/13.
 */
public class AudioRecorder extends Service {
    private static final String TAG = "AudioRecorder";
    private static boolean mRunning;
    private MediaRecorder mRecorder;

    public AudioRecorder() {
    }

    public static boolean isRunning() {
        return mRunning;
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        mRunning = true;

        startForeground(1, new Notification.Builder(this).build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRunning = false;

        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        initRecorder();
        return START_STICKY;
    }

    private void initRecorder() {
        mRecorder = new MediaRecorder();
        resetRecorder();
        mRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {

            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    mr.stop();
                    mr.reset();
                    resetRecorder();
                    startNextRecording();
                }
            }
        });

        startNextRecording();
    }

    private static int encoder = 0;
    private void resetRecorder() {
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        int e = encoder++%4 + 1;
        mRecorder.setAudioEncoder(e);
        mRecorder.setOutputFile(getAudioFile(e));
        mRecorder.setMaxDuration(5 * 60 * 1000);
    }

    private String getAudioFile(int e) {
        Calendar c = Calendar.getInstance();
        File day = new File(getExternalCacheDir(), String.format("%d-%d-%d", c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1, c.get(Calendar.DATE)));
        int name = 0;
        if (!day.exists()) {
            day.mkdirs();
        } else {
            name = day.list().length;
        }

        return new File(day, name + "-"+e+".mp4").toString();
    }

    private void startNextRecording() {
        try {
            mRecorder.prepare();
            mRecorder.start();
        } catch (IllegalStateException e) {
            Log.e(TAG, "error starting next recording", e);
            stopSelf();
        } catch (IOException e) {
            Log.e(TAG, "error starting next recording", e);
            stopSelf();
        }
    }
}
