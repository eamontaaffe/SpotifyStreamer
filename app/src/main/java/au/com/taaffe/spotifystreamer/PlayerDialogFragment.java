package au.com.taaffe.spotifystreamer;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

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

    private Bundle mColors;
    private int mVibrantColor = -1;
    private int mDarkVibrantColor = -1;
    private boolean mAttached = false;
    private Handler mHandler = new Handler();
    private Runnable mUpdateScrubRunnable;
    private PlayerDialogFragmentListener mPlayerDialogFragmentListener;

    public static String SPOTIFY_SHARE_HASHTAG = " #SpotifyStreamerP2";

    public PlayerDialogFragment() {}

    public interface PlayerDialogFragmentListener {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        void onPlayerServiceComplete();

        // Since the player is opened in a dialog fragment in twoPane mode,
        // it would be silly to only show the share widget when it is open.
        // Instead use an interface to update the activity actionBar
        void updateShareIntent(String previewUrl);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Verify that the host activity implements the callback interface
        try {
            mPlayerDialogFragmentListener = (PlayerDialogFragmentListener) getActivity();
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(getActivity().toString()
                    + " must implement NoticeDialogListener");
        }

        View rootView = inflater.inflate(R.layout.fragment_player, container, false);

        ButterKnife.bind(this, rootView);

        mScrubBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mBound && fromUser) {
                    mPlayerService.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        parseArguments(getArguments());
        startPlayerService();
        bindToPlayerService();

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private void parseArguments(Bundle arguments) {
        if (arguments == null)
            return;

        if (arguments.containsKey(COLORS)) {
            mColors = arguments.getBundle(COLORS);
            updateActivityColors(mColors);
        }
    }

    private void startPlayerService() {
        if(!mBound) {
            Intent intent = new Intent(getActivity(), PlayerService.class);
            Bundle arguments = getArguments();
            if (arguments != null)
                intent.putExtras(getArguments());
            getActivity().startService(intent);
        }
    }

    private void bindToPlayerService() {
        if (mBound) {
            updatePlayerView();
            updatePlayPause();
        } else {
            Intent intent = new Intent(getActivity(), PlayerService.class);
            Bundle arguments = getArguments();
            if (arguments != null)
                intent.putExtras(getArguments());
            getActivity().bindService(intent, mConnection, Context.BIND_IMPORTANT);
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            PlayerService.PlayerBinder binder = (PlayerService.PlayerBinder) service;
            mPlayerService = binder.getService();
            mBound = true;
            updatePlayerView();
            updatePlayPause();
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
                    if (mBound)
                        mAlbumImageView.setImageBitmap(albumImage);
                }

                @Override
                public void onClose() {
                    if (mPlayerDialogFragmentListener != null) {
                        mPlayerDialogFragmentListener.onPlayerServiceComplete();
                    }
                }
            });
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    private void updatePlayerView(){
        if (mBound) {
            mArtistTextView.setText(mPlayerService.getArtist());
            mAlbumTextView.setText(mPlayerService.getAlbum());
            mTrackTextView.setText(mPlayerService.getTrack());
            updateActivityColors(mPlayerService.getColors());
            mAlbumImageView.setImageBitmap(mPlayerService.getAlbumImage());
            startScrubUpdate();
            if(mPlayerDialogFragmentListener != null && mPlayerService.getStreamUrl() != null) {
                mPlayerDialogFragmentListener.updateShareIntent(mPlayerService.getStreamUrl());
            }
        }
    };

    private void updateActivityColors(Bundle colors) {
        if(colors == null)
            return;

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
        updatePlayerView();
        return newDialog;
    }

    private void updatePlayPause(){
        if(mBound) {
            if (mPlayerService.isPlaying()) {
                mPlayPauseButton.setImageDrawable(
                        getResources().getDrawable(android.R.drawable.ic_media_pause));
                mPlayPauseButton.setTag(PLAY);
            } else {
                mPlayPauseButton.setImageDrawable(
                        getResources().getDrawable(android.R.drawable.ic_media_play));
                mPlayPauseButton.setTag(PAUSE);
            }
        }
    }

    @OnClick(R.id.play_pause_button)
    public void onPlayPauseButton(View view) {
        if(!mBound) {
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
        if(!mBound) {
            return;
        }
        mPlayerService.onNext();
    }

    @OnClick(R.id.previous_button)
    public void onPreviousButton(View view) {
        if(!mBound) {
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

    private void updateTime(int duration, int currentPosition) {
        mTotalTimeTextView.setText(
                formatMillis(duration));
        mCurrentTimeTextView.setText(
                formatMillis(currentPosition));
        mScrubBar.setMax(duration);
        mScrubBar.setProgress(currentPosition);
    }

    private String formatMillis(int time) {
        return String.format(getResources().getString(R.string.time_format),
                TimeUnit.MINUTES.convert(time,TimeUnit.MILLISECONDS),
                TimeUnit.SECONDS.convert(time,TimeUnit.MILLISECONDS));
    }

    public void startScrubUpdate(){
        mUpdateScrubRunnable = new Runnable() {
            @Override
            public void run() {
                if(mBound && mPlayerService.isPlaying() && mAttached){
                    updateTime(mPlayerService.getDuration(),mPlayerService.getCurrentPosition());
                }
                mHandler.postDelayed(this, UPDATE_PERIOD);
            }
        };

        getActivity().runOnUiThread(mUpdateScrubRunnable);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mAttached = true;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mAttached = false;
    }

    @Override
    public void onPause() {

        if(mUpdateScrubRunnable != null) {
            mHandler.removeCallbacks(mUpdateScrubRunnable);
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        if(mUpdateScrubRunnable != null) {
            mHandler.postDelayed(mUpdateScrubRunnable, UPDATE_PERIOD);
            super.onResume();
        }
    }

    private boolean isPlayerServiceRunning() {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (PlayerService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
