package au.com.taaffe.spotifystreamer;

import android.content.Intent;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends ActionBarActivity implements SearchFragment.SearchListener,
        TopTracksFragment.TopTracksListener {

    private final String LOG_TAG = SearchFragment.class.getSimpleName();

    private static final String TOPTRACKSFRAGMENT_TAG = "TTTAG";
    private static final String PLAYERDIALOGFRAGMENT_TAG = "PDFTAG";

    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (findViewById(R.id.top_tracks_container) != null) {
            // The top tracks container view will be present only in the large-screen layouts
            // (res/layout-sw600dp). If this view is present, then the activity should be
            // in two-pane mode.
            mTwoPane = true;
            // In two-pane mode, show the top tracks view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.top_tracks_container, new TopTracksFragment(),
                                TOPTRACKSFRAGMENT_TAG)
                        .commit();
            }
        } else {
            mTwoPane = false;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
     public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // This method is the implementation of the search fragment selection callback, it chooses what
    // happens when an artist is selected, knowing if it is a tablet or a phone
    @Override
    public void onSearchItemSelected(Bundle bundle) {
        Log.v(LOG_TAG,"onSearchItemSelected");
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            TopTracksFragment fragment = new TopTracksFragment();
            fragment.setArguments(bundle);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.top_tracks_container, fragment, TOPTRACKSFRAGMENT_TAG)
                    .commit();
        } else {
            // Add the artist id to the intent so the tracks view knows what tracks to show.
            Intent openTracksIntent = new Intent(this, TopTracksActivity.class);

            openTracksIntent.putExtra(TopTracksFragment.ARTIST_INFO, bundle);

            startActivity(openTracksIntent);
        }

    }

    // This method is the implementation of the top track fragment selection callback
    // in case the two plane logic is altered it should offer either the dialog or fragment
    // cases
    @Override
    public void onTopTrackItemSelected(Bundle bundle) {
        Log.v(LOG_TAG, "onTopTrackItemSelected");
        FragmentManager fragmentManager = getSupportFragmentManager();

        if (mTwoPane) {
            // The device is using a large layout, so show the fragment as a dialog
            PlayerDialogFragment newPlayerDialogFragment = new PlayerDialogFragment();
            newPlayerDialogFragment.setArguments(bundle);
            newPlayerDialogFragment.show(fragmentManager, PLAYERDIALOGFRAGMENT_TAG);
        } else {
            // The device is smaller, so show the fragment fullscreen in the PlayerActivity
            Intent openPlayerIntent = new Intent(this, PlayerActivity.class);
            openPlayerIntent.putExtras(bundle);
            startActivity(openPlayerIntent);
        }
    }
}
