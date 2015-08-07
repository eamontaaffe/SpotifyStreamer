package au.com.taaffe.spotifystreamer;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import au.com.taaffe.spotifystreamer.service.PlayerService;


public class MainActivity extends ActionBarActivity implements SearchFragment.SearchListener,
        TopTracksFragment.TopTracksListener,PlayerDialogFragment.PlayerDialogFragmentListener {

    public static final String EXTRA_LAUNCH_PLAYER = "action_launch_player";

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final String TOPTRACKSFRAGMENT_TAG = "TTTAG";
    private static final String PLAYERDIALOGFRAGMENT_TAG = "PDFTAG";

    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(LOG_TAG,"onCreate");
        Intent intent = getIntent();
        if (intent != null) {
            Log.v(LOG_TAG, "intent extras: " + intent.getExtras());
            Log.v(LOG_TAG,"action: " + intent.getAction());
        }
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
    protected void onResume() {
        super.onResume();

        // If the PlayerService is running you whant the player to pop up
        if (isPlayerServiceRunning() && mTwoPane)
            openPlayer(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
     public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.action_settings:
                Intent settingsIntent = new Intent(this,SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // This method is the implementation of the search fragment selection callback, it chooses what
    // happens when an artist is selected, knowing if it is a tablet or a phone
    @Override
    public void onSearchItemSelected(Bundle bundle) {
        Log.v(LOG_TAG, "onSearchItemSelected");
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

        bundle.putBoolean(PlayerService.EXTRA_TWO_PANE, mTwoPane);

        if (mTwoPane) {
            // The device is using a large layout, so show the fragment as a dialog
            openPlayer(bundle);
        } else {
            // The device is smaller, so show the fragment fullscreen in the PlayerActivity
            FragmentManager fragmentManager = getSupportFragmentManager();
            Intent openPlayerIntent = new Intent(this, PlayerActivity.class);
            openPlayerIntent.putExtras(bundle);
            startActivity(openPlayerIntent);
        }
    }

    private void openPlayer(Bundle bundle) {
        Log.v(LOG_TAG,"openPlayer");
        FragmentManager fragmentManager = getSupportFragmentManager();

        DialogFragment old = (DialogFragment) fragmentManager.findFragmentByTag(PLAYERDIALOGFRAGMENT_TAG);

        if(old ==null) {
            PlayerDialogFragment newPlayerDialogFragment = new PlayerDialogFragment();
            newPlayerDialogFragment.setArguments(bundle);
            newPlayerDialogFragment.show(fragmentManager, PLAYERDIALOGFRAGMENT_TAG);
        }
    }

    @Override
    public void onPlayerServiceComplete() {
        Fragment prev = getSupportFragmentManager().findFragmentByTag(PLAYERDIALOGFRAGMENT_TAG);
        if (prev != null) {
            DialogFragment df = (DialogFragment) prev;
            df.dismiss();
        }
    }

    private boolean isPlayerServiceRunning() {
        ActivityManager manager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (PlayerService.class.getName().equals(service.service.getClassName())) {
                Log.v(LOG_TAG,"PlayerService is not running");
                return true;
            }
        }
        Log.v(LOG_TAG,"PlayerService is not running");
        return false;
    }
}
