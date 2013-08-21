package org.cketcham.lasciel;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by cketcham on 8/18/13.
 */
public class AudioRecorder extends Service {
    private static final String TAG = "AudioRecorder";
    private static boolean mRunning;
    private MediaRecorder mRecorder;
    private AudioManager am;
    private BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
            Log.d(TAG, "Audio SCO state: " + state);

            if (AudioManager.SCO_AUDIO_STATE_CONNECTED == state) {
                if(mRecorder == null) {
                    initRecorder();
                }
            } else if (AudioManager.SCO_AUDIO_STATE_DISCONNECTED == state) {
                if (mRecorder != null) {
                    mRecorder.stop();
                    mRecorder.release();
                    mRecorder = null;
                }
            }
        }
    };

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

        am.stopBluetoothSco();
        unregisterReceiver(bluetoothStateReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        registerReceiver(bluetoothStateReceiver,
                new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));

        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.startBluetoothSco();

        return START_STICKY;
    }

    private void initRecorder() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC);
        mRecorder.setOutputFile(getAudioFile());
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

    private String getAudioFile() {
        File dir = new File(Environment.getExternalStorageDirectory(), "Laciel");

        if(!dir.exists()) {
            dir.mkdirs();
        }

        String name = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(dir, name + ".mp4").toString();
    }
}
