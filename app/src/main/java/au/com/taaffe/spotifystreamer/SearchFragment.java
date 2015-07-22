package au.com.taaffe.spotifystreamer;

import android.app.Activity;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsPager;
import retrofit.RetrofitError;

/**
 * A placeholder fragment containing a simple view.
 */
public class SearchFragment extends Fragment {

    private final static String SELECTED_KEY = "selected_key";

    private ArtistAdapter artistAdapter;
    private final String LOG_TAG = SearchFragment.class.getSimpleName();

    private SearchListener mSearchListener;
    private ListView mListView;
    private int mPosition = ListView.INVALID_POSITION;


    public SearchFragment() {
    }

    // this method is only called once for this fragment
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // retain this fragment
        setRetainInstance(true);
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface SearchListener {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        public void onSearchItemSelected(Bundle bundle);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mSearchListener = (SearchListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_search, container, false);

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

        // Get a reference to the ListView, and attach this artistAdapter to it.
        mListView = (ListView) rootView.findViewById(R.id.artist_list_view);

        if (artistAdapter == null) {
            List<Artist> artists = new ArrayList<Artist>();

            artistAdapter = new ArtistAdapter(
                    getActivity(),
                    R.layout.list_item_artist,
                    artists
            );

        }

        mListView.setAdapter(artistAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Artist artist = (Artist) parent.getItemAtPosition(position);
                if (artist != null) {


                    Bundle extras = new Bundle();
                    extras.putString(TopTracksFragment.ARTIST_ID, artist.id);
                    extras.putString(TopTracksFragment.ARTIST_NAME, artist.name);
                    if (artist.images.size() > 0) {
                        extras.putString(TopTracksFragment.ARTIST_IMAGE_URL, artist.images.get(0).url);
                    }

                    // Use the implemented callback to decide what happens
                    mSearchListener.onSearchItemSelected(extras);
                    mPosition = position;
                }
            }
        });

        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            Log.v(LOG_TAG,"savedInstanceState contains SELECTED_KEY");
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mListView.smoothScrollToPosition(mPosition);

    }

    void updateArtistList(String query) {
        FetchArtistTask fetchArtistTask = new FetchArtistTask();
        fetchArtistTask.execute(query);
    }

    void updateArtistAdapter(ArtistsPager results) {
        ListView artistListView = (ListView) getActivity().findViewById(R.id.artist_list_view);
        artistListView.setItemChecked(ListView.INVALID_POSITION, true);
        artistAdapter.clear();
        for(Artist result: results.artists.items) {
            artistAdapter.add(result);
        }
    }

    private class FetchArtistTask extends AsyncTask<String, Void, Void> {
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

            try {
                results = spotify.searchArtists(query);
            } catch (RetrofitError e) {
                Log.e(LOG_TAG,e.getMessage());
                Log.e(LOG_TAG,e.getStackTrace().toString());

            }   finally {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (results != null) {
                int resultsSize = results.artists.items.size();
                if (resultsSize == 0) {
                    Toast toast = Toast.makeText(getActivity(),
                            R.string.no_results,
                            Toast.LENGTH_SHORT
                    );
                    toast.show();
                }

                // update artistAdapter
                updateArtistAdapter(results);
            } else {
                Toast.makeText(getActivity()
                        ,getResources().getText(R.string.null_results),
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }
}
