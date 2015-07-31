package au.com.taaffe.spotifystreamer.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.Rating;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;

import au.com.taaffe.spotifystreamer.ParcelableTrack;
import au.com.taaffe.spotifystreamer.R;

/**
 * Created by eamon on 30/07/15.
 */
public class PlayerService2 extends Service {

    private static final String LOG_TAG = PlayerService2.class.getSimpleName();
    private static final String MEDIA_SESSION_TAG = "PST";
    private static final int INVALID_TRACK_ID = -1;

    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREVIOUS = "action_previous";
    public static final String ACTION_STOP = "action_stop";
    public static final String EXTRA_PLAYLIST = "track_list";
    public static final String EXTRA_TRACK_ID = "extra_track_id";

    private MediaPlayer mMediaPlayer;
    private MediaSessionManager mManager;
    private MediaSession mSession;
    private MediaController mController;
    private Bitmap mAlbumImage;
    private ArrayList<ParcelableTrack> mPlaylist;
    private int mTrackId = INVALID_TRACK_ID;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mManager == null) {
            initMediaSession();
        }
        if (intent != null && intent.hasExtra(EXTRA_PLAYLIST)) {
            mPlaylist = intent.getParcelableArrayListExtra(EXTRA_PLAYLIST);
        }
        if (intent != null && intent.hasExtra(EXTRA_TRACK_ID)) {
            mTrackId = intent.getIntExtra(EXTRA_TRACK_ID, INVALID_TRACK_ID);
        }
        if (intent != null && intent.getAction() != null) {
            handleAction(intent.getAction());
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public void handleAction(String action) {

        switch (action) {
            case ACTION_PLAY:
                Log.v(LOG_TAG,"ACTION_PLAY");
                break;
            case ACTION_PREVIOUS:
                Log.v(LOG_TAG,"ACTION_PREVIOUS");
                break;
            case ACTION_PAUSE:
                Log.v(LOG_TAG,"ACTION_PAUSE");
                break;
            case ACTION_NEXT:
                Log.v(LOG_TAG,"ACTION_NEXT");
                break;
            default:
                return;
        }

        createNotification();
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
                Log.v(LOG_TAG,"onPlay");
                super.onPlay();
            }
            @Override
            public void onPause() {
                //TODO onPause
                Log.v(LOG_TAG,"onPlause");
                super.onPause();
            }
            @Override
            public void onSkipToNext() {
                //TODO onSkipToNext
                Log.v(LOG_TAG,"onSkipToNext");
                super.onSkipToNext();
            }
            @Override
            public void onSkipToPrevious() {
                //TODO onSkipToPrevious
                Log.v(LOG_TAG,"onSkipToPrevious");
                super.onSkipToPrevious();
            }
        });

        mSession.setActive(true);

        // Indicate you want to receive transport controls via your Callback
        mSession.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
    }

    @TargetApi(21)
    private void createNotification() {
        // Create a new Notification
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
                .setColor(0xFFDB4437)
                        // Set the large and small icons
//                .setLargeIcon(mAlbumImage)
                .setSmallIcon(R.mipmap.ic_spotify_launcher)
                        // Set Notification content information
                .setContentText("Pink Floyd")
                .setContentInfo("Dark Side of the Moon")
                .setContentTitle("The Great Gig in the Sky")
                        // Add some playback controls
                .addAction(android.R.drawable.ic_media_previous, "prev", intentBuilder(ACTION_PREVIOUS))
                .addAction(android.R.drawable.ic_media_pause, "pause", intentBuilder(ACTION_PAUSE))
                .addAction(android.R.drawable.ic_media_next, "next", intentBuilder(ACTION_NEXT))
                .build();

        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(1, noti);
    }

    private PendingIntent intentBuilder(String action) {
        Intent intent = new Intent(this,PlayerService2.class);

        intent.setAction(action);

        PendingIntent pendingIntent = PendingIntent
                .getService(this, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);


        return pendingIntent;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
