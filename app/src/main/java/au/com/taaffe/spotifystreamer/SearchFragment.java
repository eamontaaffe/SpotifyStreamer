package au.com.taaffe.spotifystreamer;

import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;


/**
 * A placeholder fragment containing a simple view.
 */
public class SearchFragment extends Fragment {
    ArrayAdapter<String> artistAdapter;

    private final String LOG_TAG = SearchFragment.class.getSimpleName();
    FetchArtistTask fetchArtistTask = new FetchArtistTask();

    public SearchFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        SearchView artistSearch = (SearchView) rootView.findViewById(R.id.artist_search_view);
        artistSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.v(LOG_TAG, "OnQuerySubmit :" + query);
                updateArtistList(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                return false;
            }
        });

        // Create some dummy data for vthe ListView.  Here's a sample weekly forecast
        String[] data = {
                "Coldplay",
                "Coldplay & Lele",
                "Colplay & Rihanna",
                "Various Artists - Coldplay Tribute"
        };

        List<String> artists = new ArrayList<String>(Arrays.asList(data));

        // Now that we have some dummy forecast data, create an ArrayAdapter.
        // The ArrayAdapter will take data from a source (like our dummy forecast)
        // use it to populate the ListView it's attached to.

        artistAdapter = new ArrayAdapter<String>(
                        getActivity(), // The current context (this activity)
                        R.layout.list_item_artist, // The name of the layout ID.
                        R.id.list_item_forecast_textview,
                        artists
        );

        Log.v(LOG_TAG, artistAdapter.toString());

        // Get a reference to the ListView, and attach this artistAdapter to it.
        ListView listView = (ListView) rootView.findViewById(R.id.listview);
        listView.setAdapter(artistAdapter);

        return rootView;
    }

    void updateArtistList(String query) {
        Log.v(LOG_TAG, "updateArtistList: " + query);
        fetchArtistTask.execute(query);
    }

    public class FetchArtistTask extends AsyncTask<String, Void, Void> {
        private final String LOG_TAG = FetchArtistTask.class.getSimpleName();
        ArtistsPager results;

        @Override
        protected Void doInBackground(String... params) {
            if (params.length == 0) {
                Log.v(LOG_TAG,"Empty params list");
                return null;
            }

            String query = params[0];

            Log.v(LOG_TAG, "doInBackground, query: " + params[0]);

            SpotifyApi api = new SpotifyApi();
            SpotifyService spotify = api.getService();
            results = spotify.searchArtists(query);

            Log.v(LOG_TAG, "Search results are size: " + results.artists.items.size());

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            int resultsSize = results.artists.items.size();
            if (resultsSize == 0) {
                Toast toast = Toast.makeText(getActivity(),
                        "There are " + resultsSize + " results",
                        Toast.LENGTH_SHORT
                );
                toast.show();
            }

            // update artistAdapter
            artistAdapter.clear();
            for(Artist result: results.artists.items) {
                artistAdapter.add(result.name);
            }

        }
    }

}
