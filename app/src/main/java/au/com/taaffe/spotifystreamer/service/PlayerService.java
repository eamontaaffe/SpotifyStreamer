package au.com.taaffe.spotifystreamer.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaController;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.NotificationCompat;
import android.support.v4.media.session.MediaControllerCompat;

import android.os.Binder;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;
import java.util.ArrayList;

import au.com.taaffe.spotifystreamer.MainActivity;
import au.com.taaffe.spotifystreamer.ParcelableTrack;
import au.com.taaffe.spotifystreamer.PlayerActivity;
import au.com.taaffe.spotifystreamer.PlayerDialogFragment;
import au.com.taaffe.spotifystreamer.R;
import butterknife.Bind;

/**
 * Created by eamon on 30/07/15.
 */
public class PlayerService extends Service {

    private static final String LOG_TAG = PlayerService.class.getSimpleName();
    private static final String MEDIA_SESSION_TAG = "PST";
    private static final int INVALID_TRACK_ID = -1;
    private static final int NOTIFICATION_ID = 3121;

    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    public static final String ACTION_STOP = "action_stop";
    public static final String ACTION_OPEN = "action_open";

    public static final String EXTRA_PLAYLIST = "track_list";
    public static final String EXTRA_TRACK_ID = "extra_track_id";
    public static final String EXTRA_COLORS = "colors";
    public static final String EXTRA_TWO_PANE = "extra_two_pane";

    private final IBinder mBinder = new PlayerBinder();

    private MediaPlayer mMediaPlayer;
    private boolean mPlayerPrepared = false;
    protected MediaSessionCompat mSession;
    private MediaControllerCompat mController;
    private Bitmap mAlbumImage;
    private ArrayList<ParcelableTrack> mPlaylist;
    private int mTrackId = INVALID_TRACK_ID;
    private Bundle mColors;
    private PlayerServiceListener mListener;
    private MediaControllerCompat.TransportControls mTransportControls;
    private Context mContext;

    // Single pane unless told otherwise
    private boolean mTwoPane = false;

    private Target mTarget = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            mAlbumImage = bitmap;
            if (mListener != null) {
                mListener.updateAlbumImage(mAlbumImage);
                mListener.updateStatus();
                mListener.updateTrack();
            }
            createNotification();
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            Log.d(LOG_TAG,"Image load failed");
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra(EXTRA_PLAYLIST)) {
            Log.d(LOG_TAG, "new playlist recieved");
            mPlaylist = intent.getParcelableArrayListExtra(EXTRA_PLAYLIST);

            // When a playlist is received, start from the beginning unless a trackId has
            // been specified
            if (intent.hasExtra(EXTRA_TRACK_ID)) {
                mTrackId = intent.getIntExtra(EXTRA_TRACK_ID, INVALID_TRACK_ID);
            } else {
                mTrackId = 0;
            }
            if(mMediaPlayer != null) {
                mTransportControls.sendCustomAction(ACTION_PLAY, null);
            }
        }
        if (intent != null && intent.hasExtra(EXTRA_TWO_PANE)) {
            mTwoPane = intent.getBooleanExtra(EXTRA_TWO_PANE, false);
        }
        if (intent != null && intent.hasExtra(EXTRA_COLORS)) {
            mColors = intent.getBundleExtra(EXTRA_COLORS);
        }

        if (intent != null && intent.getAction() != null) {
            handleAction(intent.getAction());
        }

        if (mMediaPlayer == null ) {
            initMediaSession();
            loadTrack();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public void handleAction(String action) {

        switch (action) {
            case ACTION_PLAY:
                mTransportControls.play();
                break;
            case ACTION_PREVIOUS:
                mTransportControls.skipToPrevious();
                break;
            case ACTION_PAUSE:
                mTransportControls.pause();
                break;
            case ACTION_NEXT:
                mTransportControls.skipToNext();
                break;
            case ACTION_STOP:
                stopSelf();
                break;
            case ACTION_OPEN:
                openPlayer();
                break;
            default:
                return;
        }
    }

    public void openPlayer() {
        Intent playerIntent;
        if(mTwoPane) {
            playerIntent = new Intent(this, MainActivity.class)
                    .putExtra(MainActivity.EXTRA_LAUNCH_PLAYER,true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } else {
            playerIntent = new Intent(this, PlayerActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        this.startActivity(playerIntent);
//        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
//        stackBuilder.addNextIntent(playerIntent);
//        PendingIntent playerPendingIntent =
//                stackBuilder.getPendingIntent(
//                        0,
//                        PendingIntent.FLAG_UPDATE_CURRENT
//                );
    }

    public void loadTrack() {
        if(mTrackId == INVALID_TRACK_ID)
            return;

        Picasso.with(this).load(mPlaylist.get(mTrackId).track_image_url).into(mTarget);
    }

    public void initMediaSession() {
        mContext = (Context) this;
        mMediaPlayer = new MediaPlayer();
        mSession = new MediaSessionCompat(this,
                MEDIA_SESSION_TAG,
                new ComponentName(getApplicationContext(),
                        MediaButtonEventReceiver.class),
                null);
        mController = mSession.getController();
        mTransportControls = mController.getTransportControls();
        mSession.setCallback(mediaSessionCallback);

        mSession.setActive(true);

        // Indicate you want to receive transport controls via your Callback
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

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
                mTransportControls.skipToNext();
            }
        });

        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mPlayerPrepared = true;
                mMediaPlayer.start();
                if (mListener != null) {
                    mListener.updateStatus();
                }
                createNotification();
            }
        });

        try {
            mMediaPlayer.setDataSource(getStreamUrl());
        } catch (IllegalArgumentException e) {
         Log.e(LOG_TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
        } catch (NullPointerException e) {
            Log.e(LOG_TAG,e.getMessage());
        }
            mPlayerPrepared = false;
            mMediaPlayer.prepareAsync();
        }

//This is required for the MediaSessionCompat constructor for API < 21
public class MediaButtonEventReceiver extends BroadcastReceiver {
    private final String LOG_TAG = MediaButtonEventReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
    }
}

private MediaSessionCompat.Callback mediaSessionCallback = new MediaSessionCompat.Callback() {
    private final String LOG_TAG = MediaSessionCompat.Callback.class.getSimpleName();

    @Override
    public void onPlay() {
        Log.d(LOG_TAG, "onPlay");

        if(mPlayerPrepared) {
            mMediaPlayer.start();
            loadTrack();
            if (mListener != null)
                mListener.updateStatus();
            createNotification();
        }
    }

    @Override
    public void onPause() {
        Log.d(LOG_TAG, "onPause");
        if(mPlayerPrepared) {
            mMediaPlayer.pause();
            loadTrack();
            if (mListener != null)
                mListener.updateStatus();
            createNotification();
        }
    }

    @Override
    public void onSkipToNext() {
        Log.d(LOG_TAG, "onSkipToNext");

        if (mPlaylist.size() > mTrackId + 1) {
            mMediaPlayer.reset();
            if(mListener != null) {
                mListener.updateStatus();
            }
            mTrackId ++;
            playTrack();
            loadTrack();
        } else {
            stopSelf();
        }
    }

    @Override
    public void onSkipToPrevious() {
        Log.d(LOG_TAG, "onSkipToPrevious");
        if (mTrackId > 0) {
            mMediaPlayer.reset();
            if(mListener != null) {
                mListener.updateStatus();
            }
            mTrackId --;
            playTrack();
        } else {
            Toast.makeText(mContext, R.string.first_track_warning, Toast.LENGTH_SHORT).show();
        }
        loadTrack();
    }

    @Override
    public void onCustomAction(String action, Bundle extras) {
        super.onCustomAction(action, extras);
        switch (action) {
            case ACTION_PLAY:
                playTrack();
        }
        loadTrack();
    }

    public void playTrack() {
        mMediaPlayer.reset();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mMediaPlayer.setDataSource(getStreamUrl());
            mPlayerPrepared = false;
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
    }
};

    private void createNotification() {
        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(this);

        String notificationsKey = getString(R.string.pref_notifications_key);
        boolean notificationsEnabled = preferences.getBoolean(notificationsKey,true);
        if (notificationsEnabled) {
            int playPauseDrawable;
            PendingIntent playPauseIntent;
            String playPauseString;

            if (isPlaying()) {
                playPauseDrawable = android.R.drawable.ic_media_pause;
                playPauseIntent = intentBuilder(ACTION_PAUSE);
                playPauseString = "play";
            } else {
                playPauseDrawable = android.R.drawable.ic_media_play;
                playPauseIntent = intentBuilder(ACTION_PLAY);
                playPauseString = "pause";
            }

            final Notification noti = new NotificationCompat.Builder(this)
                    // Hide the timestamp
                    .setShowWhen(false)
                            // Set the Notification style
                    .setStyle(new NotificationCompat.MediaStyle()
                            // Attach our MediaSession token
                            .setMediaSession(mSession.getSessionToken())
                                    // Show our playback controls in the compat view
                            .setShowActionsInCompactView(0, 1, 2))
                            // Set the large and small icons
                    .setLargeIcon(mAlbumImage)
                    .setSmallIcon(android.R.drawable.stat_notify_more)
                            // Set Notification content information
                    .setContentText(getArtist())
                    .setContentInfo(getAlbum())
                    .setContentTitle(getTrack())
                    .setContentIntent(intentBuilder(ACTION_OPEN))
                            // Add some playback controls
                    .addAction(android.R.drawable.ic_media_previous, "prev", intentBuilder(ACTION_PREVIOUS))
                    .addAction(playPauseDrawable, playPauseString, playPauseIntent)
                    .addAction(android.R.drawable.ic_media_next, "next", intentBuilder(ACTION_NEXT))
                    .setDeleteIntent(intentBuilder(ACTION_STOP))
                    .build();

            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, noti);
        }
    }

    private PendingIntent intentBuilder(String action) {
        Intent intent = new Intent(this,PlayerService.class);

        intent.setAction(action);

        PendingIntent pendingIntent = PendingIntent
                .getService(this, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        return pendingIntent;
    }

    @Override
    public void onDestroy() {
        mMediaPlayer.release();
        mSession.release();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(NOTIFICATION_ID);
        if (mListener != null) {
            mListener.onClose();
        }
        super.onDestroy();
    }

////////////////////////////////////////////////////////////////
///////// Code for interfacing with a view /////////////////////
////////////////////////////////////////////////////////////////

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

    //used notify a bound view that the service is closing
    void onClose();
}

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

//    public String getStreamUrl() {
//        try {
//            return mPlaylist.get(mTrackId).track_preview_url;
//        } catch (NullPointerException e) {
//            Log.e(LOG_TAG,e.getMessage());
//        }
//        return null;
//    }
    public String getStreamUrl() {
        try {
            return "https://play.spotify.com/track/" + mPlaylist.get(mTrackId).track_id;
        } catch (NullPointerException e) {
            Log.e(LOG_TAG,e.getMessage());
        }
        return null;
    }

    public String getArtist() {
        if (mPlaylist == null)
            return null;
        return mPlaylist.get(mTrackId).artist;
    }
    public String getAlbum() {
        if (mPlaylist == null)
            return null;
        return mPlaylist.get(mTrackId).album;
    }
    public String getTrack() {
        if (mPlaylist == null)
            return null;
        return mPlaylist.get(mTrackId).track_name;
    }
    public Bitmap getAlbumImage() {
        if(mAlbumImage != null) {
            return mAlbumImage;
        }
        return null;
    }
    public Bundle getColors() {
        return mColors;
    }
    public int getDuration() {
        if (mMediaPlayer != null && mPlayerPrepared) {
            return mMediaPlayer.getDuration();
        }
        Log.e(LOG_TAG, "getDuration = -1");
        return -1;
    }
    public int getCurrentPosition() {
        if (mMediaPlayer != null && mPlayerPrepared) {
            return mMediaPlayer.getCurrentPosition();
        }
        Log.e(LOG_TAG, "getCurrentPositon = -1");
        return -1;
    }
    public boolean isPlaying(){
        if(mMediaPlayer != null && mPlayerPrepared) {
            return mMediaPlayer.isPlaying();
        }
        return false;
    }
    public void onPlay(){
        mTransportControls.play();
    }
    public void onPause(){
        mTransportControls.pause();
    }
    public void onNext(){
        mTransportControls.skipToNext();
    }
    public void onPrevious() {
        mTransportControls.skipToPrevious();
    }
    public void seekTo(int progress) {
        if (mMediaPlayer != null && mPlayerPrepared) {
            mMediaPlayer.seekTo(progress);
        }
    }
}
