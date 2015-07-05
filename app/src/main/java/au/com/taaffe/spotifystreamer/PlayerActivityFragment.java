package au.com.taaffe.spotifystreamer;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;


/**
 * A placeholder fragment containing a simple view.
 */
public class PlayerActivityFragment extends Fragment {

    private final String LOG_TAG = PlayerActivityFragment.class.getSimpleName();

    public static final String TRACK_ID = "track_id";
    public static final String TRACK_NAME = "track_name";
    public static final String ARTIST = "artist";
    public static final String ALBUM = "album";
    public static final String TRACK_IMAGE_URL = "track_image_url";
    public static final String TRACK_INFO = "track_info";
    public static final String PREVIEW_URL ="preview_url";

    @Bind(R.id.artist_textview) TextView artistTextView;
    @Bind(R.id.album_textview) TextView albumTextView;
    @Bind(R.id.album_imageview) ImageView albumImageView;
    @Bind(R.id.track_textview) TextView trackTextView;
    @Bind(R.id.play_pause_button) ImageButton playPauseButton;
    @Bind(R.id.current_time_textview) TextView currentTimeTextView;
    @Bind(R.id.total_time_textview) TextView totalTimeTextView;

    MediaPlayer mMediaPlayer;

    public PlayerActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.v(LOG_TAG, "onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_player, container, false);

        ButterKnife.bind(this, rootView);

        Intent intent = getActivity().getIntent();

        if (intent != null && intent.hasExtra(TRACK_INFO)) {
            Bundle info = intent.getBundleExtra(TRACK_INFO);
            artistTextView.setText(info.getString(ARTIST));
            albumTextView.setText(info.getString(ALBUM));
            trackTextView.setText(info.getString(TRACK_NAME));

            Picasso.with(getActivity())
                    .load(info.getString(TRACK_IMAGE_URL))
                    .fit()
                    .centerCrop()
                    .into(albumImageView);


            String previewUrl = info.getString(PREVIEW_URL);

            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            //TODO initialise duration and current position textviews
//            currentTimeTextView.setText(mMediaPlayer.getDuration());
//            totalTimeTextView.setText(mMediaPlayer.getDuration());


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
                    start_player();
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

    private void start_player() {
        Log.v(LOG_TAG,"start_player()");
        mMediaPlayer.start();
        playPauseButton.setImageDrawable(
                getResources().getDrawable(android.R.drawable.ic_media_pause));
    }

    private void pause_player() {
        Log.v(LOG_TAG,"pause_player()");
        mMediaPlayer.pause();
        playPauseButton.setImageDrawable(
                getResources().getDrawable(android.R.drawable.ic_media_play));
    }

    @OnClick(R.id.play_pause_button)
    public void play_pause(View view) {
        if(mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            pause_player();
        } else if (mMediaPlayer != null) {
            start_player();
        }
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
