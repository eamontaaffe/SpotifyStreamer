package au.com.taaffe.spotifystreamer;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;

import android.os.Handler;

import com.squareup.picasso.Picasso;

import au.com.taaffe.spotifystreamer.service.PlayerService;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;


/**
 * A placeholder fragment containing a simple view.
 */
public class PlayerDialogFragment extends DialogFragment {

    private final String LOG_TAG = PlayerDialogFragment.class.getSimpleName();

    public static final String TRACK_LIST = "track_list";
    public static final String TRACK_INDEX = "track_index";
    private static String PLAY = "play";
    private static String PAUSE = "pause";

    private static final int UPDATE_PERIOD = 1000/24;

    @Bind(R.id.artist_textview) TextView mArtistTextView;
    @Bind(R.id.album_textview) TextView mAlbumTextView;
    @Bind(R.id.album_imageview) ImageView mAlbumImageView;
    @Bind(R.id.track_textview) TextView mTrackTextView;
    @Bind(R.id.play_pause_button) ImageButton mPlayPauseButton;
    @Bind(R.id.current_time_textview) TextView mCurrentTimeTextView;
    @Bind(R.id.total_time_textview) TextView mTotalTimeTextView;
    @Bind(R.id.scrub_bar) SeekBar mScrubBar;

    public static final String COLORS = "colors";
    public static final String VIBRANT_COLOR = "vibrant_color";
    public static final String DARK_VIBRANT_COLOR = "dark_vibrant_color";

    private PlayerService mPlayerService;
    private boolean mBound = false;

    private int mVibrantColor = -1;
    private int mDarkVibrantColor = -1;

    private PlayerDialogFragmentListener mPlayerDialogFragmentListener;

    private Handler mHandler = new Handler();

    private Runnable mUpdateScrubRunnable;

    public PlayerDialogFragment() {
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of track selection
     */
    public interface PlayerDialogFragmentListener {

        public void replacePlayerDialogFragment(Bundle bundle);
        public void onLastTrackComplete();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mPlayerDialogFragmentListener = (PlayerDialogFragmentListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.v(LOG_TAG, "onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_player, container, false);

        ButterKnife.bind(this, rootView);

        parseArguments(getArguments());

        if(getActivity().getClass().equals(PlayerActivity.class)) {
            Intent intent = new Intent(getActivity(), PlayerService.class);
            intent.putExtras(getArguments());
            getActivity().startService(intent);
            getActivity().bindService(intent, mConnection, Context.BIND_IMPORTANT);
        }

        return rootView;
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            PlayerService.PlayerBinder binder = (PlayerService.PlayerBinder) service;
            mPlayerService = binder.getService();
            updatePlayerView();
            mBound = true;
            binder.setListener(new PlayerService.PlayerServiceListener() {
                @Override
                public void updateTrack() {
                    updatePlayerView();
                }
                @Override
                public void updateStatus() {
                    updatePlayPause();
                }

                @Override
                public void updateAlbumImage(Bitmap albumImage) {
                    mAlbumImageView.setImageBitmap(albumImage);
                }

                @Override
                public void onClose() {
                    getActivity().finish();
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }


    };

    private void parseArguments(Bundle arguments) {
        if (arguments == null)
            return;

        if (arguments.containsKey(COLORS)) {
            Bundle colors = arguments.getBundle(COLORS);
            updateActivityColors(colors);
        }
    }

    private void updatePlayerView(){

        mArtistTextView.setText(mPlayerService.getArtist());
        mAlbumTextView.setText(mPlayerService.getAlbum());
        mTrackTextView.setText(mPlayerService.getTrack());

    };

    private void updateActivityColors(Bundle colors) {
        mVibrantColor = colors.getInt(VIBRANT_COLOR);
        mDarkVibrantColor = colors.getInt(DARK_VIBRANT_COLOR);

        android.support.v7.app.ActionBar mActionBar =
                ((ActionBarActivity) getActivity())
                        .getSupportActionBar();
        Window window = getActivity().getWindow();

        ColorDrawable vibrantColorDrawable = new ColorDrawable(
                mVibrantColor);


        if (mActionBar != null && window != null) {
            mActionBar.setBackgroundDrawable(vibrantColorDrawable);

            if (android.os.Build.VERSION.SDK_INT >= 21) {
                window.setStatusBarColor(mDarkVibrantColor);
            }

        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog newDialog = super.onCreateDialog(savedInstanceState);

        newDialog.getWindow().setLayout(1200, 1200);
        return newDialog;
    }

//    private void updateTime() {
//        mTotalTimeTextView.setText(
//                formatMillis(mMediaPlayer.getDuration()));
//        mCurrentTimeTextView.setText(
//                formatMillis(mMediaPlayer.getCurrentPosition()));
//    }

//    private String formatMillis(int time) {
//        return String.format(getResources().getString(R.string.time_format),
//                TimeUnit.MINUTES.convert(time,TimeUnit.MILLISECONDS),
//                TimeUnit.SECONDS.convert(time,TimeUnit.MILLISECONDS));
//    }
    private void updatePlayPause(){
        if(mPlayerService.isPlaying()) {
            mPlayPauseButton.setImageDrawable(
                    getResources().getDrawable(android.R.drawable.ic_media_pause));
            mPlayPauseButton.setTag(PLAY);
        } else {
            mPlayPauseButton.setImageDrawable(
                    getResources().getDrawable(android.R.drawable.ic_media_play));
            mPlayPauseButton.setTag(PAUSE);
        }
    }

    @OnClick(R.id.play_pause_button)
    public void onPlayPauseButton(View view) {
        if(!mBound && mPlayerService != null) {
            return;
        }

        if(mPlayPauseButton.getTag() == PLAY) {
            mPlayerService.onPause();
        } else if (mPlayPauseButton.getTag() == PAUSE){
            mPlayerService.onPlay();
        }
    }

    @OnClick(R.id.next_button)
    public void onNextButton(View view) {
        Log.v(LOG_TAG,"onNextButton");
        if(!mBound && mPlayerService != null) {
            return;
        }
        mPlayerService.onNext();
    }

    @OnClick(R.id.previous_button)
    public void onPreviousButton(View view) {
        if(!mBound && mPlayerService != null) {
            return;
        }
        mPlayerService.onPrevious();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
        if (mBound) {
            getActivity().unbindService(mConnection);
            mBound = false;
        }
    }

//    public void startScrubUpdate(){
//        mUpdateScrubRunnable = new Runnable() {
//            @Override
//            public void run() {
//                if(mMediaPlayer != null){
//                    int mCurrentPosition = mMediaPlayer.getCurrentPosition();
//                    mScrubBar.setProgress(mCurrentPosition);
//                }
//                mHandler.postDelayed(this, UPDATE_PERIOD);
//            }
//        };
//
//        getActivity().runOnUiThread(mUpdateScrubRunnable);
//    }

    @Override
    public void onPause() {
        super.onPause();
//
//        if(mUpdateScrubRunnable != null) {
//            mHandler.removeCallbacks(mUpdateScrubRunnable);
//            super.onPause();
//        }
    }

    @Override
    public void onResume() {
        super.onResume();
//
//        if(mUpdateScrubRunnable != null) {
//            mHandler.postDelayed(mUpdateScrubRunnable, UPDATE_PERIOD);
//            super.onResume();
//        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

//    private boolean isPlayerServiceRunning() {
//        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
//        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
//            if (PlayerService.class.getName().equals(service.service.getClassName())) {
//                return true;
//            }
//        }
//        return false;
//    }
}
