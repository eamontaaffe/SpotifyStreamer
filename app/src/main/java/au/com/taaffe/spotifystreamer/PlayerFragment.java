package au.com.taaffe.spotifystreamer;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v4.app.Fragment;
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

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;


/**
 * A placeholder fragment containing a simple view.
 */
public class PlayerFragment extends Fragment {

    private final String LOG_TAG = PlayerFragment.class.getSimpleName();

    public static final String TRACK_LIST = "track_list";
    public static final String TRACK_INDEX = "track_index";

    public static final int UPDATE_PERIOD = 1000;

    @Bind(R.id.artist_textview) TextView artistTextView;
    @Bind(R.id.album_textview) TextView albumTextView;
    @Bind(R.id.album_imageview) ImageView albumImageView;
    @Bind(R.id.track_textview) TextView trackTextView;
    @Bind(R.id.play_pause_button) ImageButton playPauseButton;
    @Bind(R.id.current_time_textview) TextView currentTimeTextView;
    @Bind(R.id.total_time_textview) TextView totalTimeTextView;
    @Bind(R.id.scrub_bar) SeekBar scrubBar;

    public static final String COLORS = "colors";
    public static final String VIBRANT_COLOR = "vibrant_color";
    public static final String DARK_VIBRANT_COLOR = "dark_vibrant_color";

    MediaPlayer mMediaPlayer;
    ArrayList<ParcelableTrack> parcelableTrackList;
    int trackIndex;

    int vibrantColor = -1;
    int darkVibrantColor = -1;

    public PlayerFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.v(LOG_TAG, "onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_player, container, false);

        ButterKnife.bind(this, rootView);

        Intent intent = getActivity().getIntent();

        if (intent != null && intent.hasExtra(COLORS)) {
            Bundle colors = intent.getBundleExtra(COLORS);
            vibrantColor = colors.getInt(VIBRANT_COLOR);
            darkVibrantColor = colors.getInt(DARK_VIBRANT_COLOR);

            android.support.v7.app.ActionBar mActionBar =
                    ((ActionBarActivity)getActivity())
                            .getSupportActionBar();
            Window window = getActivity().getWindow();

            ColorDrawable vibrantColorDrawable = new ColorDrawable(
                    vibrantColor);

            if (mActionBar != null && window != null) {
                mActionBar.setBackgroundDrawable(vibrantColorDrawable);
                window.setStatusBarColor(darkVibrantColor);
            }
        }


        if (intent != null && intent.hasExtra(TRACK_LIST)
                && intent.hasExtra(TRACK_INDEX)) {

            parcelableTrackList = intent.getParcelableArrayListExtra(TRACK_LIST);
            trackIndex = intent.getIntExtra(TRACK_INDEX, 5);

            ParcelableTrack track = parcelableTrackList.get(trackIndex);
            artistTextView.setText(track.artist);
            albumTextView.setText(track.album);
            trackTextView.setText(track.track_name);

            Log.v(LOG_TAG, "Index: " + Integer.toString(trackIndex));
            Log.v(LOG_TAG,"Current Track: " + track.track_name);

            for(int i=0; i< parcelableTrackList.size();i++){
                Log.v(LOG_TAG,parcelableTrackList.get(i).track_name);
            }

            Picasso.with(getActivity())
                    .load(track.track_image_url)
                    .fit()
                    .centerCrop()
                    .into(albumImageView);

            String previewUrl = track.track_preview_url;

            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            //TODO initialise duration and current position textviews

            try {
                mMediaPlayer.setDataSource(previewUrl);
            } catch ( IllegalArgumentException e) {
                Log.e(LOG_TAG,e.getMessage());
            } catch (IOException e) {
                Log.e(LOG_TAG,e.getMessage());
            }

            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Log.v(LOG_TAG, Integer.toString(mp.getDuration()));
                    updateTime();

                    // Set the upperbound of the scrub bar now that you know what it is
                    scrubBar.setMax(mp.getDuration());

                    scrubBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            if (fromUser) {
                                mMediaPlayer.seekTo(progress);
                            }
                            currentTimeTextView.setText(
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
                }
            });

            // Since the preparation is asynchronous we need to listen for error so that they can
            // be handled rather than crashing the application.
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Log.e(LOG_TAG, String.format("Error(%s%s)", what, extra));
                    return false;
                }
            });

            mMediaPlayer.prepareAsync();
        }

        return rootView;
    }

    private void updateTime() {
        totalTimeTextView.setText(
                formatMillis(mMediaPlayer.getDuration()));
        currentTimeTextView.setText(
                formatMillis(mMediaPlayer.getCurrentPosition()));
    }

    private String formatMillis(int time) {
        // TODO use a formatting string instead of hardcode
        return String.format("%2d:%02d",
                TimeUnit.MINUTES.convert(time,TimeUnit.MILLISECONDS),
                TimeUnit.SECONDS.convert(time,TimeUnit.MILLISECONDS));
    }

    private void startPlayer() {
        Log.v(LOG_TAG,"startPlayer()");
        mMediaPlayer.start();
        playPauseButton.setImageDrawable(
                getResources().getDrawable(android.R.drawable.ic_media_pause));
    }

    private void pausePlayer() {
        Log.v(LOG_TAG,"pausePlayer()");
        mMediaPlayer.pause();
        playPauseButton.setImageDrawable(
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
        if (trackIndex == parcelableTrackList.size()-1) {
            Toast.makeText(getActivity(),
                    "This is the last track",Toast.LENGTH_SHORT)
                    .show();
        }   else {
            playTrack(trackIndex + 1);
        }
    }

    @OnClick(R.id.previous_button)
    public void onPreviousButton(View view) {
        if (trackIndex == 0) {
            Toast.makeText(getActivity(),
                    "This is the first track",Toast.LENGTH_SHORT)
                    .show();
        }   else {
            playTrack(trackIndex -
                    1);
        }
    }

    public void playTrack(int trackIndex) {
        if (parcelableTrackList == null) {
            return;
        }

        Intent playTrackIntent = new Intent(getActivity(), PlayerActivity.class);

        playTrackIntent.putParcelableArrayListExtra(
                TRACK_LIST, parcelableTrackList);

        if( vibrantColor != -1 && darkVibrantColor != -1) {
            Bundle colors = new Bundle();
            colors.putInt(PlayerFragment.VIBRANT_COLOR, vibrantColor);
            colors.putInt(PlayerFragment.DARK_VIBRANT_COLOR, darkVibrantColor);
            playTrackIntent.putExtra(PlayerFragment.COLORS,colors);
        }

        // TODO implement better edge case handling previous button
        playTrackIntent.putExtra(
                TRACK_INDEX,
                trackIndex);

        startActivity(playTrackIntent);
        getActivity().finish();
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
}
