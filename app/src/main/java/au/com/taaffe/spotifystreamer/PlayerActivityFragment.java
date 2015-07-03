package au.com.taaffe.spotifystreamer;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import butterknife.Bind;
import butterknife.ButterKnife;


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

    @Bind(R.id.artist_textview) TextView artistTextView;
    @Bind(R.id.album_textview) TextView albumTextView;
    @Bind(R.id.album_imageview) ImageView albumImageView;
    @Bind(R.id.track_textview) TextView trackTextView;

    public PlayerActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.v(LOG_TAG,"onCreateView" );
        View rootView = inflater.inflate(R.layout.fragment_player, container, false);

        ButterKnife.bind(this, rootView);

        Intent intent = getActivity().getIntent();

        if (intent != null && intent.hasExtra(PlayerActivityFragment.TRACK_INFO)) {
            Bundle info = intent.getBundleExtra(PlayerActivityFragment.TRACK_INFO);
            artistTextView.setText(info.getString(PlayerActivityFragment.ARTIST));
            albumTextView.setText(info.getString(PlayerActivityFragment.ALBUM));
            trackTextView.setText(info.getString(PlayerActivityFragment.TRACK_NAME));

            Picasso.with(getActivity())
                    .load(info.getString(PlayerActivityFragment.TRACK_IMAGE_URL))
                    .fit()
                    .centerCrop()
                    .into(albumImageView);
        }

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }
}
