package au.com.taaffe.spotifystreamer;

import android.app.Activity;
import android.app.Dialog;
import android.media.AudioManager;
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
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import android.os.Handler;

import com.squareup.picasso.Picasso;

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

    private MediaPlayer mMediaPlayer;
    private ArrayList<ParcelableTrack> mParcelableTrackList;
    private int mTrackIndex;

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

        Bundle arguments = getArguments();

        if (arguments != null) {
            parseArguments(arguments);
        }

        return rootView;
    }

    private void parseArguments(Bundle arguments) {
        Bundle colors = arguments.getBundle(COLORS);

        if (colors != null) {
            updateActivityColors(colors);
        }

        mParcelableTrackList = arguments.getParcelableArrayList(TRACK_LIST);
        mTrackIndex = arguments.getInt(TRACK_INDEX, 5);

        if (mParcelableTrackList != null && mTrackIndex != -1) {
            initialisePlayer();
        }
    }

    private void initialisePlayer(){
        ParcelableTrack track = mParcelableTrackList.get(mTrackIndex);
        Log.v(LOG_TAG,track.toString());
        mArtistTextView.setText(track.artist);
        mAlbumTextView.setText(track.album);
        mTrackTextView.setText(track.track_name);

        Log.v(LOG_TAG, "Index: " + Integer.toString(mTrackIndex));
        Log.v(LOG_TAG, "Current Track: " + track.track_name);

        for (int i = 0; i < mParcelableTrackList.size(); i++) {
            Log.v(LOG_TAG, mParcelableTrackList.get(i).track_name);
        }

        Picasso.with(getActivity())
                .load(track.track_image_url)
                .fit()
                .centerCrop()
                .into(mAlbumImageView);

        String previewUrl = track.track_preview_url;

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        try {
            mMediaPlayer.setDataSource(previewUrl);
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        mMediaPlayer.setOnPreparedListener(mOnPreparedListener);

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
                trackComplete();
            }
        });

        mMediaPlayer.prepareAsync();
    }

    private void trackComplete() {
        if (mTrackIndex == mParcelableTrackList.size()-1) {
            mPlayerDialogFragmentListener.onLastTrackComplete();
        }   else {
            playTrackIndex(mTrackIndex + 1);
        }
    }

    MediaPlayer.OnPreparedListener mOnPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            Log.v(LOG_TAG, Integer.toString(mp.getDuration()));
            updateTime();

            // Set the upperbound of the scrub bar now that you know what it is
            mScrubBar.setMax(mp.getDuration());

            mScrubBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        mMediaPlayer.seekTo(progress);
                    }
                    mCurrentTimeTextView.setText(
                            formatMillis(mMediaPlayer.getCurrentPosition()));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

            // TODO it shouldn't start the player if it was purposely paused
            startPlayer();

            startScrubUpdate();
        }
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

    private void updateTime() {
        mTotalTimeTextView.setText(
                formatMillis(mMediaPlayer.getDuration()));
        mCurrentTimeTextView.setText(
                formatMillis(mMediaPlayer.getCurrentPosition()));
    }

    private String formatMillis(int time) {
        return String.format(getResources().getString(R.string.time_format),
                TimeUnit.MINUTES.convert(time,TimeUnit.MILLISECONDS),
                TimeUnit.SECONDS.convert(time,TimeUnit.MILLISECONDS));
    }

    private void startPlayer() {
        Log.v(LOG_TAG,"startPlayer()");
        mMediaPlayer.start();
        mPlayPauseButton.setImageDrawable(
                getResources().getDrawable(android.R.drawable.ic_media_pause));
    }

    private void pausePlayer() {
        Log.v(LOG_TAG,"pausePlayer()");
        mMediaPlayer.pause();
        mPlayPauseButton.setImageDrawable(
                getResources().getDrawable(android.R.drawable.ic_media_play));
    }

    @OnClick(R.id.play_pause_button)
    public void onPlayPauseButton(View view) {
        if(mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            pausePlayer();
        } else if (mMediaPlayer != null) {
            startPlayer();
        }
    }

    @OnClick(R.id.next_button)
    public void onNextButton(View view) {
        if (mTrackIndex == mParcelableTrackList.size()-1) {
            Toast.makeText(getActivity(),
                    "This is the last track",Toast.LENGTH_SHORT)
                    .show();
        }   else {
            playTrackIndex(mTrackIndex + 1);
        }
    }

    @OnClick(R.id.previous_button)
    public void onPreviousButton(View view) {
        if (mTrackIndex == 0) {
            Toast.makeText(getActivity(),
                    "This is the first track",Toast.LENGTH_SHORT)
                    .show();
        }   else {
            playTrackIndex(mTrackIndex -
                    1);
        }
    }

    public void playTrackIndex(int trackIndex) {
        if (mParcelableTrackList == null) {
            return;
        }

        Intent playTrackIntent = new Intent(getActivity(), PlayerActivity.class);

        Bundle bundle = new Bundle();

        bundle.putParcelableArrayList(
                TRACK_LIST, mParcelableTrackList);

        if( mVibrantColor != -1 && mDarkVibrantColor != -1) {
            Bundle colors = new Bundle();
            colors.putInt(PlayerDialogFragment.VIBRANT_COLOR, mVibrantColor);
            colors.putInt(PlayerDialogFragment.DARK_VIBRANT_COLOR, mDarkVibrantColor);
            bundle.putBundle(PlayerDialogFragment.COLORS, colors);
        }

        bundle.putInt(
                TRACK_INDEX,
                trackIndex);

        mPlayerDialogFragmentListener.replacePlayerDialogFragment(bundle);
//        getActivity().finish();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
        if (mMediaPlayer != null) mMediaPlayer.release();
    }

    public void startScrubUpdate(){
        mUpdateScrubRunnable = new Runnable() {
            @Override
            public void run() {
                if(mMediaPlayer != null){
                    int mCurrentPosition = mMediaPlayer.getCurrentPosition();
                    mScrubBar.setProgress(mCurrentPosition);
                }
                mHandler.postDelayed(this, UPDATE_PERIOD);
            }
        };

        getActivity().runOnUiThread(mUpdateScrubRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();

        if(mUpdateScrubRunnable != null) {
            mHandler.removeCallbacks(mUpdateScrubRunnable);
            super.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if(mUpdateScrubRunnable != null) {
            mHandler.postDelayed(mUpdateScrubRunnable, UPDATE_PERIOD);
            super.onResume();
        }
    }
}
