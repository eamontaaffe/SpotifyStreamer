package au.com.taaffe.spotifystreamer.service;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

import au.com.taaffe.spotifystreamer.ParcelableTrack;

/**
 * Created by eamon on 30/07/15.
 */
public class PlayerService extends Service {
    private static final String LOG_TAG = PlayerService.class.getSimpleName();

    public static final String PLAYLIST = "track_list";
    public static final String TRACK_INDEX = "track_index";

    private ArrayList<ParcelableTrack> mPlaylist;
    private int mTrackIndex = -1;
    private MediaPlayer mMediaPlayer;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(LOG_TAG, "onHandleIntent");

        Bundle extras = intent.getExtras();
        int trackIndex = -1;

        if (extras == null || !extras.containsKey(PLAYLIST)) {
            // if this information was not supplied then the service has nothing to do
            Log.d(LOG_TAG,"Did not recieve a playlist, cannot initiate play");
            return Service.START_NOT_STICKY;
        }

        mPlaylist = extras.getParcelableArrayList(PLAYLIST);

        if (!extras.containsKey(TRACK_INDEX)) {
            mTrackIndex = 0;
        } else {
            mTrackIndex = extras.getInt(TRACK_INDEX);
        }

        startPlayer();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startPlayer(){
        Log.v(LOG_TAG, "initialisePlayer");

        mMediaPlayer = new MediaPlayer();

        // Since the preparation is asynchronous we need to listen for error so that they can
        // be handled rather than crashing the application.
        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e(LOG_TAG, String.format("Error(%s%s)", what, extra));
                return false;
            }
        });

        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mMediaPlayer.reset();
                nextTrack();
            }
        });

        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mMediaPlayer.start();
            }
        });

        playTrack(mTrackIndex);
    }

    private void nextTrack() {
        if (mPlaylist.size() > mTrackIndex + 1) {
            playTrack(mTrackIndex ++);
        } else {
            mMediaPlayer.release();
        }
    }

    private void playTrack(int trackIndex) {
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        Log.v(LOG_TAG,"trackIndex is: " + trackIndex);
        Log.v(LOG_TAG,"mPlaylist.size(): " + mPlaylist.size());
        String previewUrl = mPlaylist.get(mTrackIndex).track_preview_url;

        try {
            mMediaPlayer.setDataSource(previewUrl);
            mMediaPlayer.prepareAsync();
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }
}
