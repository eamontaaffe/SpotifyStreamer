package au.com.taaffe.spotifystreamer;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
    ArtistAdapter artistAdapter;

    private final String LOG_TAG = SearchFragment.class.getSimpleName();

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
                updateArtistList(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                return false;
            }
        });

        List<Artist> artists = new ArrayList<Artist>();

        artistAdapter = new ArtistAdapter(
                getActivity(),
                R.layout.list_item_artist,
                artists
        );

        // Get a reference to the ListView, and attach this artistAdapter to it.
        ListView listView = (ListView) rootView.findViewById(R.id.listview);
        listView.setAdapter(artistAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Artist artist = (Artist) parent.getItemAtPosition(position);
                if (artist != null) {
                    // Add the artist id to the intent so the tracks view knows what tracks to show.
                    Intent openTracksIntent = new Intent(getActivity(), TopTracksActivity.class)
                            .putExtra(Intent.EXTRA_TEXT, artist.id);

                    startActivity(openTracksIntent);
                }
            }
        });

        return rootView;
    }

    void updateArtistList(String query) {
        FetchArtistTask fetchArtistTask = new FetchArtistTask();
        fetchArtistTask.execute(query);
    }

    public class FetchArtistTask extends AsyncTask<String, Void, Void> {
        private final String LOG_TAG = FetchArtistTask.class.getSimpleName();
        ArtistsPager results;

        @Override
        protected Void doInBackground(String... params) {
            if (params.length == 0) {
                return null;
            }

            String query = params[0];

            SpotifyApi api = new SpotifyApi();
            SpotifyService spotify = api.getService();
            results = spotify.searchArtists(query);

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
                artistAdapter.add(result);
            }

        }
    }

}
