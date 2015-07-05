package au.com.taaffe.spotifystreamer;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

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

    private TrackAdapter trackAdapter;
    private String name;

    ArrayList <ParcelableTrack> parcelableTrackList = new ArrayList<ParcelableTrack>();

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
        if (trackAdapter == null) {
            List<Track> tracks = new ArrayList<Track>();

            trackAdapter = new TrackAdapter(
                    getActivity(),
                    R.layout.list_item_track,
                    tracks
            );



            Intent intent = getActivity().getIntent();

            // If the intent has artist info then populate the list
            if (intent != null && intent.hasExtra(ARTIST_INFO)) {
                Bundle infoBundle = intent.getBundleExtra(ARTIST_INFO);
                String id = infoBundle.getString(ARTIST_ID);
                name = infoBundle.getString(ARTIST_NAME);

                FetchTrackData fetchTrackData = new FetchTrackData();
                fetchTrackData.execute(id);
            }

        }
        trackList.setAdapter(trackAdapter);

        trackList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Track track = (Track) parent.getItemAtPosition(position);
                if (track != null) {
                    Intent playTrackIntent = new Intent(getActivity(), PlayerActivity.class);

                    // Rather than just passing a track id and having to do another API request,
                    // just pass all the relevant info to increase speed and save data.
                    playTrackIntent.putParcelableArrayListExtra(
                            PlayerFragment.TRACK_LIST, parcelableTrackList);

                    for(int i=0; i< parcelableTrackList.size();i++){
                        Log.v(LOG_TAG,parcelableTrackList.get(i).track_name);
                    }

                    playTrackIntent.putExtra(PlayerFragment.TRACK_INDEX, position);

                    startActivity(playTrackIntent);
                }
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (name != null) {
            ((ActionBarActivity)getActivity()).getSupportActionBar().setSubtitle(name);
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
                trackAdapter.clear();
                for (Track track : results.tracks) {
                    trackAdapter.add(track);
                }

                for (int i = 0; i < results.tracks.size(); i++) {
                    parcelableTrackList.add(new ParcelableTrack(
                            (Track) results.tracks.get(i),
                            name));
                    Log.v(LOG_TAG,((Track) results.tracks.get(i)).name);
                }

                for(int i=0; i< parcelableTrackList.size();i++){
                    Log.v(LOG_TAG,parcelableTrackList.get(i).track_name);
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