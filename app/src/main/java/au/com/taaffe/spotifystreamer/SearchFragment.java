package au.com.taaffe.spotifystreamer;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * A placeholder fragment containing a simple view.
 */
public class SearchFragment extends Fragment {
    ArrayAdapter<String> adapter;

    private final String LOG_TAG = SearchFragment.class.getSimpleName();

    public SearchFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        Log.v(LOG_TAG, "OnCreate");
        // Create some dummy data for the ListView.  Here's a sample weekly forecast
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

        adapter = new ArrayAdapter<String>(
                        getActivity(), // The current context (this activity)
                        R.layout.list_item_artist, // The name of the layout ID.
                        R.id.list_item_forecast_textview,
                        artists
        );

        Log.v(LOG_TAG,adapter.toString());

        // Get a reference to the ListView, and attach this adapter to it.
        ListView listView = (ListView) rootView.findViewById(R.id.listview);
        listView.setAdapter(adapter);



        return inflater.inflate(R.layout.fragment_main, container, false);
    }
}
