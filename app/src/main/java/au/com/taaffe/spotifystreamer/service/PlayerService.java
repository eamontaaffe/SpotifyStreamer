package au.com.taaffe.spotifystreamer.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;
import java.util.ArrayList;

import au.com.taaffe.spotifystreamer.ParcelableTrack;
import au.com.taaffe.spotifystreamer.PlayerActivity;
import au.com.taaffe.spotifystreamer.R;

/**
 * Created by eamon on 30/07/15.
 */
public class PlayerService extends Service {
    private static final String LOG_TAG = PlayerService.class.getSimpleName();

    public static final String PLAYLIST = "track_list";
    public static final String TRACK_INDEX = "track_index";

    private static final int PLAYER_NOTIFICATION_ID = 876;
    private static final String MEDIA_SESSION_TAG = "PST";

    private final IBinder mBinder = new PlayerBinder();

    private ArrayList<ParcelableTrack> mPlaylist;
    private int mTrackIndex = -1;
    private MediaPlayer mMediaPlayer;
    private PlayerServiceListener mListener;
    private Bitmap mAlbumBitmap;
    private NotificationManager mNotificationManager;
    private MediaSessionManager mMediaManager;
    private MediaSession mMediaSession;

    public class PlayerBinder extends Binder {
        public PlayerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return PlayerService.this;
        }
        public void setListener(PlayerServiceListener listener) {
            mListener = listener;
        }
    }



    // Callback for views to implement
    public interface PlayerServiceListener {
        //used to notify the view of a change in track
        void updateTrack();

        //used to notify the view of a change in the MediaPlayer state
        void updateStatus();

        //used to notify the view when the artist cover is loaded
        void updateAlbumImage(Bitmap albumImage);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(LOG_TAG, "onHandleIntent");

        Bundle extras = intent.getExtras();
        int trackIndex = -1;

        if (extras == null || !extras.containsKey(PLAYLIST)) {
            // if this information was not supplied then the service has nothing to do
            Log.d(LOG_TAG,"Did not recieve a playlist, cannot initiate onPlay");
            return Service.START_NOT_STICKY;
        }

        mPlaylist = extras.getParcelableArrayList(PLAYLIST);

        if (!extras.containsKey(TRACK_INDEX)) {
            mTrackIndex = 0;
        } else {
            mTrackIndex = extras.getInt(TRACK_INDEX);
        }

        startPlayer();

        mMediaManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);

        return super.onStartCommand(intent, flags, startId);
    }


    private void showPlayerNotification() {
        // NotificationCompatBuilder is a very convenient way to build backward-compatible
        // notifications.  Just throw in some data.
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setColor(getResources().getColor(R.color.primary))
                        .setSmallIcon(R.mipmap.ic_spotify_launcher)
                        .setContentTitle("PlayerTitle")
                        .setContentText("PlayerContent");

        // Make something interesting happen when the user clicks on the notification.
        // In this case, opening the app is sufficient.
        Intent resultIntent = new Intent(this, PlayerActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        builder.setContentIntent(resultPendingIntent);

        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // WEATHER_NOTIFICATION_ID allows you to update the notification later on.
        mNotificationManager.notify(PLAYER_NOTIFICATION_ID, builder.build());
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
                nextTrack();
            }
        });

        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mMediaPlayer.start();
                if(mListener != null) {
                    mListener.updateStatus();
                }
            }
        });

        playTrack(mTrackIndex);
    }

    private void nextTrack() {
        if (mPlaylist.size() > mTrackIndex + 1) {
            mMediaPlayer.reset();
            if(mListener != null) {
                mListener.updateStatus();
            }
            playTrack(mTrackIndex ++);
        } else {
            Toast.makeText(this,"This is the last track!",Toast.LENGTH_SHORT).show();
        }
    }
    private void previousTrack(){
        if (mTrackIndex - 1 >= 0) {
            mMediaPlayer.reset();
            playTrack(mTrackIndex --);
        } else {
            Toast.makeText(this,"This is the first track!",Toast.LENGTH_SHORT).show();
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

        // if a view is listening tell it to update itself
        if (mListener != null) {
            mListener.updateTrack();
        }

        // load the album image
        Picasso.with(this).load(mPlaylist.get(mTrackIndex).track_image_url).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                mAlbumBitmap = bitmap;
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                Log.d(LOG_TAG,"Image load failed");
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mNotificationManager.cancel(PLAYER_NOTIFICATION_ID);
    }
// Public methods to interact with views

    public void onPlay(){
        if(mMediaPlayer != null) {
            mMediaPlayer.start();
            if(mListener != null) {
                mListener.updateStatus();
            }
        }
    }
    public void onPause(){
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
            if(mListener != null) {
                mListener.updateStatus();
            }
        }
    }
    public void onNext(){
        nextTrack();
    }
    public void onPrevious() {
        previousTrack();
    }
    public boolean isPlaying(){
        if(mMediaPlayer != null) {
            return mMediaPlayer.isPlaying();
        }
        return false;
    }
    public String getArtistText(){
        return mPlaylist.get(mTrackIndex).artist;
    }

    public String getAlbumText(){
        return mPlaylist.get(mTrackIndex).album;
    }
    public String getTrackText(){
        return mPlaylist.get(mTrackIndex).track_name;
    }
    public String getAlbumImageUrl(){
        return mPlaylist.get(mTrackIndex).track_image_url;
    }
}
