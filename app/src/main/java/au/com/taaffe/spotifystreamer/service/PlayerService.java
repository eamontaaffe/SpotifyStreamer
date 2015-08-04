package au.com.taaffe.spotifystreamer.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaController;
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

import au.com.taaffe.spotifystreamer.ParcelableTrack;

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
    public static final String EXTRA_PLAYLIST = "track_list";
    public static final String EXTRA_TRACK_ID = "extra_track_id";

    private final IBinder mBinder = new PlayerBinder();

    private MediaPlayer mMediaPlayer;
    protected MediaSessionCompat mSession;
    private MediaControllerCompat mController;
    private Bitmap mAlbumImage;
    private ArrayList<ParcelableTrack> mPlaylist;
    private int mTrackId = INVALID_TRACK_ID;
    private PlayerServiceListener mListener;
    private MediaControllerCompat.TransportControls mTransportControls;
    private Context mContext;

    private Target mTarget = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            Log.v(LOG_TAG, "onBitmapLoaded");
            mAlbumImage = bitmap;
            if (mListener != null) {
                mListener.updateAlbumImage(mAlbumImage);
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
            mPlaylist = intent.getParcelableArrayListExtra(EXTRA_PLAYLIST);

            // When a playlist is received, start from the beginning unless a trackId has
            // been specified
            mTrackId = 0;
        }
        if (intent != null && intent.hasExtra(EXTRA_TRACK_ID)) {
            mTrackId = intent.getIntExtra(EXTRA_TRACK_ID, INVALID_TRACK_ID);
        }

        if (intent != null && intent.getAction() != null) {
            handleAction(intent.getAction());
        }

        if (mMediaPlayer == null) {
            initMediaSession();
            loadTrack();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public void handleAction(String action) {

        switch (action) {
            case ACTION_PLAY:
                Log.v(LOG_TAG, "ACTION_PLAY");
                mTransportControls.play();
                break;
            case ACTION_PREVIOUS:
                Log.v(LOG_TAG,"ACTION_PREVIOUS");
                mTransportControls.skipToPrevious();
                break;
            case ACTION_PAUSE:
                Log.v(LOG_TAG, "ACTION_PAUSE");
                mTransportControls.pause();
                break;
            case ACTION_NEXT:
                Log.v(LOG_TAG,"ACTION_NEXT");
                mTransportControls.skipToNext();
                break;
            default:
                return;
        }
    }

    public void loadTrack() {
        Log.v(LOG_TAG,"loadTrack");
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
                mMediaPlayer.start();
                if (mListener != null) {
                    mListener.updateStatus();
                }
            }
        });

        try {
            mMediaPlayer.setDataSource(getStreamUrl());
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
        mMediaPlayer.prepareAsync();
    }

    public class MediaButtonEventReceiver extends BroadcastReceiver {
        private final String LOG_TAG = MediaButtonEventReceiver.class.getSimpleName();

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(LOG_TAG,"onRecieve");
        }
    }

    private MediaSessionCompat.Callback mediaSessionCallback = new MediaSessionCompat.Callback() {
        private final String LOG_TAG = MediaSessionCompat.Callback.class.getSimpleName();

        @Override
        public void onPlay() {
            //TODO onPlay
            Log.v(LOG_TAG, "onPlay");
            mMediaPlayer.start();
            loadTrack();
            if(mListener != null)
                mListener.updateStatus();

        }

        @Override
        public void onPause() {
            //TODO onPause
            Log.v(LOG_TAG, "onPlause");
            mMediaPlayer.pause();
            loadTrack();
            if(mListener != null)
                mListener.updateStatus();
        }

        @Override
        public void onSkipToNext() {
            if (mPlaylist.size() > mTrackId + 1) {
                mMediaPlayer.reset();
                if(mListener != null) {
                    mListener.updateStatus();
                }
                playTrack(mTrackId ++);
            } else {
                Toast.makeText(mContext, "This is the last track!", Toast.LENGTH_SHORT).show();
            }
            loadTrack();
            Log.v(LOG_TAG, "onSkipToNext, mTrackId: " + mTrackId);
        }

        @Override
        public void onSkipToPrevious() {
            //TODO onSkipToPrevious
            mTrackId--;
            loadTrack();
            Log.v(LOG_TAG, "onSkipToPrevious, mTrackId: " + mTrackId);
        }

        public void playTrack(int trackId) {
            mMediaPlayer.reset();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            Log.v(LOG_TAG,"trackIndex is: " + trackId);
            Log.v(LOG_TAG,"mPlaylist.size(): " + mPlaylist.size());

            try {
                mMediaPlayer.setDataSource(getStreamUrl());
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
        Log.v(LOG_TAG, "createNotification");

        final Notification noti = new NotificationCompat.Builder(this)
            // Hide the timestamp
                .setShowWhen(false)
                    // Set the Notification style
                .setStyle(new NotificationCompat.MediaStyle()
                        // Attach our MediaSession token
                        .setMediaSession(mSession.getSessionToken())
                                // Show our playback controls in the compat view
                        .setShowActionsInCompactView(0, 1, 2))
                    // Set the Notification color
//                .setColor(0xFFDB4437)
                    // Set the large and small icons
                .setLargeIcon(mAlbumImage)
                .setSmallIcon(android.R.drawable.stat_notify_more)
                    // Set Notification content information
                .setContentText(getArtist())
                .setContentInfo(getAlbum())
                .setContentTitle(getTrack())
                    // Add some playback controls
                .addAction(android.R.drawable.ic_media_previous, "prev", intentBuilder(ACTION_PREVIOUS))
                .addAction(android.R.drawable.ic_media_pause, "pause", intentBuilder(ACTION_PAUSE))
                .addAction(android.R.drawable.ic_media_next, "next", intentBuilder(ACTION_NEXT))
                .build();

            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, noti);
    }

    private PendingIntent intentBuilder(String action) {
        Intent intent = new Intent(this,PlayerService.class);

        intent.setAction(action);

        PendingIntent pendingIntent = PendingIntent
                .getService(this, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        return pendingIntent;
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
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public String getStreamUrl() {return mPlaylist.get(mTrackId).track_preview_url; }
    public String getArtist() {
        return mPlaylist.get(mTrackId).artist;
    }
    public String getAlbum() {
        return mPlaylist.get(mTrackId).album;
    }
    public String getTrack() {
        return mPlaylist.get(mTrackId).track_name;
    }
    public boolean isPlaying(){
        if(mMediaPlayer != null) {
            Log.v(LOG_TAG,"isPlaying: " + mMediaPlayer.isPlaying());
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
}
