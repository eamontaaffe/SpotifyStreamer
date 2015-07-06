package au.com.taaffe.spotifystreamer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.Tracks;
import retrofit.RetrofitError;


/**
 * A placeholder fragment containing a simple view.
 */
public class TopTracksFragment extends Fragment {
    public static final String LOG_TAG = TopTracksFragment.class.getSimpleName();

    public static final String ARTIST_INFO = "artist_info";
    public static final String ARTIST_ID = "artist_id";
    public static final String ARTIST_NAME = "artist_name";
    public static final String ARTIST_IMAGE_URL = "artist_image_url";

    private TrackAdapter mTrackAdapter;
    private String mName;
    int vibrantColor = -1;
    int darkVibrantColor = -1;

    ArrayList <ParcelableTrack> parcelableTracks = new ArrayList<ParcelableTrack>();

    public TopTracksFragment() {
    }

    // this method is only called once for this fragment
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // retain this fragment
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_top_tracks, container, false);

        final ListView trackList = (ListView) rootView.findViewById(R.id.track_list);

        // If the trackAdapter is null than it is not being restored from a configuration change
        // so we need to initialise the top tracks list and adapter
        if (mTrackAdapter == null) {
            List<Track> tracks = new ArrayList<Track>();

            mTrackAdapter = new TrackAdapter(
                    getActivity(),
                    R.layout.list_item_track,
                    tracks
            );



            Intent intent = getActivity().getIntent();

            // If the intent has artist info then populate the list
            if (intent != null && intent.hasExtra(ARTIST_INFO)) {
                Bundle infoBundle = intent.getBundleExtra(ARTIST_INFO);
                String id = infoBundle.getString(ARTIST_ID);
                mName = infoBundle.getString(ARTIST_NAME);
                String artistImageUrl = infoBundle.getString(ARTIST_IMAGE_URL);

                final ImageView artistImageView =
                        (ImageView) rootView.findViewById(R.id.artist_imageview);

                Picasso.with(getActivity())
                        .load(artistImageUrl)
                        .fit()
                        .centerCrop()
                        .into(artistImageView, new Callback() {
                            @Override
                            public void onSuccess() {
                                Bitmap bitmap =
                                        ((BitmapDrawable)artistImageView.getDrawable()).getBitmap();
                                Log.v(LOG_TAG, "Picasso Callback OnSuccess");
                                Palette.generateAsync(bitmap, new Palette.PaletteAsyncListener() {
                                    private String LOG_TAG =
                                            Palette.PaletteAsyncListener.class.getSimpleName();

                                    @Override
                                    public void onGenerated(Palette palette) {
                                        android.support.v7.app.ActionBar mActionBar =
                                                ((ActionBarActivity)getActivity())
                                                        .getSupportActionBar();
                                        Window window = getActivity().getWindow();

                                        palette.getVibrantColor(R.color.primary);

                                        vibrantColor = palette.getVibrantColor(R.color.primary);
                                        ColorDrawable vibrantColorDrawable = new ColorDrawable(
                                                vibrantColor);
                                        darkVibrantColor =
                                                palette.getDarkVibrantColor(R.color.primary_dark);

                                        if (mActionBar != null && window != null) {
                                            mActionBar.setBackgroundDrawable(vibrantColorDrawable);
                                            window.setStatusBarColor(darkVibrantColor);
                                        }
                                    }
                                });
                            }

                            @Override
                            public void onError() {
                                Log.d(LOG_TAG, "Picasso Callback OnFailure");
                            }
                        });



                FetchTrackData fetchTrackData = new FetchTrackData();
                fetchTrackData.execute(id);
            }

        }
        trackList.setAdapter(mTrackAdapter);

        trackList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Track track = (Track) parent.getItemAtPosition(position);
                if (track != null) {
                    Intent playTrackIntent = new Intent(getActivity(), PlayerActivity.class);

                    // Rather than just passing a track id and having to do another API request,
                    // just pass all the relevant info to increase speed and save data.
                    playTrackIntent.putParcelableArrayListExtra(
                            PlayerFragment.TRACK_LIST, parcelableTracks);

                    playTrackIntent.putExtra(PlayerFragment.TRACK_INDEX, position);

                    if( vibrantColor != -1 && darkVibrantColor != -1) {
                        Bundle colors = new Bundle();
                        colors.putInt(PlayerFragment.VIBRANT_COLOR, vibrantColor);
                        colors.putInt(PlayerFragment.DARK_VIBRANT_COLOR, darkVibrantColor);
                        playTrackIntent.putExtra(PlayerFragment.COLORS,colors);
                    }

                    startActivity(playTrackIntent);
                }
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mName != null) {
            ((ActionBarActivity)getActivity()).getSupportActionBar().setSubtitle(mName);
        }
    }

    private class FetchTrackData extends AsyncTask<String, Void, Void> {
        private final String LOG_TAG = FetchTrackData.class.getSimpleName();
        Tracks results;

        @Override
        protected Void doInBackground(String... params) {
            if (params.length == 0) {
                return null;
            }

            String id = params[0];
            Map<String, Object> options = new HashMap<>();

            // TODO add settings option for country
            // Hard code in country setting for now
            options.put("country", "AU");

            SpotifyApi api = new SpotifyApi();
            SpotifyService spotify = api.getService();

            try {
                results = spotify.getArtistTopTrack(id,options);
            } catch (RetrofitError e) {
                Log.e(LOG_TAG, e.getMessage());
                Log.e(LOG_TAG,e.getStackTrace().toString());
            }   finally {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (results != null) {
                mTrackAdapter.clear();
                for (Track track : results.tracks) {
                    mTrackAdapter.add(track);
                }

                for (int i = 0; i < results.tracks.size(); i++) {
                    parcelableTracks.add(new ParcelableTrack(
                            (Track) results.tracks.get(i),
                            mName));
                }
            } else {
                Toast.makeText(getActivity()
                        ,getResources().getText(R.string.null_results),
                        Toast.LENGTH_SHORT
                ).show();
            }

        }
    }
}
