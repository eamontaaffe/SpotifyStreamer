package au.com.taaffe.spotifystreamer.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

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
    private MediaSessionManager mManager;
    private MediaSession mSession;
    private MediaController mController;
    private Bitmap mAlbumImage;
    private ArrayList<ParcelableTrack> mPlaylist;
    private int mTrackId = INVALID_TRACK_ID;
    private PlayerServiceListener mListener;

    private Target mTarget = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            Log.v(LOG_TAG,"onBitmapLoaded");
            mAlbumImage = bitmap;
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
        if (mManager == null) {
            initMediaSession();
        }
        if (intent != null && intent.hasExtra(EXTRA_PLAYLIST)) {
            mPlaylist = intent.getParcelableArrayListExtra(EXTRA_PLAYLIST);

            // When a playlist is recieved start from the beginning unless a trackId has
            // been specified
            mTrackId = 0;
        }
        if (intent != null && intent.hasExtra(EXTRA_TRACK_ID)) {
            mTrackId = intent.getIntExtra(EXTRA_TRACK_ID, INVALID_TRACK_ID);
        }

        if (intent != null && intent.getAction() != null) {
            handleAction(intent.getAction());
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @TargetApi(21)
    public void handleAction(String action) {

        switch (action) {
            case ACTION_PLAY:
                Log.v(LOG_TAG, "ACTION_PLAY");
                mController.getTransportControls().play();
                break;
            case ACTION_PREVIOUS:
                Log.v(LOG_TAG,"ACTION_PREVIOUS");
                mController.getTransportControls().skipToPrevious();
                break;
            case ACTION_PAUSE:
                Log.v(LOG_TAG, "ACTION_PAUSE");
                mController.getTransportControls().pause();
                break;
            case ACTION_NEXT:
                Log.v(LOG_TAG,"ACTION_NEXT");
                mController.getTransportControls().skipToNext();
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

    @TargetApi(21)
    public void initMediaSession() {
        mMediaPlayer = new MediaPlayer();
        mManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        mSession = new MediaSession(this,MEDIA_SESSION_TAG);
        mController = mSession.getController();

        mSession.setCallback(new MediaSession.Callback() {
            private final String LOG_TAG = MediaSession.Callback.class.getSimpleName();

            @Override
            public void onPlay() {
                //TODO onPlay
                Log.v(LOG_TAG, "onPlay");
                loadTrack();
            }

            @Override
            public void onPause() {
                //TODO onPause
                Log.v(LOG_TAG, "onPlause");
                loadTrack();
            }

            @Override
            public void onSkipToNext() {
                //TODO onSkipToNext
                mTrackId ++;
                loadTrack();
                Log.v(LOG_TAG, "onSkipToNext, mTrackId: " + mTrackId);
            }

            @Override
            public void onSkipToPrevious() {
                //TODO onSkipToPrevious
                mTrackId--;
                loadTrack();
                Log.v(LOG_TAG, "onSkipToNext, mTrackId: " + mTrackId);
            }
        });

        mSession.setActive(true);

        // Indicate you want to receive transport controls via your Callback
        mSession.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
    }

    @TargetApi(21)
    private void createNotification() {
        Log.v(LOG_TAG, "createNotification");
        final Notification noti = new Notification.Builder(this)
            // Hide the timestamp
                .setShowWhen(false)
                    // Set the Notification style
                .setStyle(new Notification.MediaStyle()
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

    public String getArtist() {
        return mPlaylist.get(mTrackId).artist;
    }
    public String getAlbum() {
        return mPlaylist.get(mTrackId).album;
    }
    public String getTrack() {
        Log.v(LOG_TAG,"getTrack: " + mPlaylist.get(mTrackId).track_name);
        return mPlaylist.get(mTrackId).track_name;
    }
    public Bitmap getAlbumImage() { return mAlbumImage; }
}
