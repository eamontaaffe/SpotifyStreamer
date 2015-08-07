package au.com.taaffe.spotifystreamer;

import android.content.Intent;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class TopTracksActivity extends ActionBarActivity implements TopTracksFragment.TopTracksListener {
    public static String LOG_TAG = TopTracksActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_tracks);

        if(savedInstanceState == null) {

            Bundle arguments = getIntent().getBundleExtra(TopTracksFragment.ARTIST_INFO);

            TopTracksFragment fragment = new TopTracksFragment();
            fragment.setArguments(arguments);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.top_tracks_container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_top_tracks, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this,SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // This method is the implementation of the top track fragment selection callback
    // in case the two plane logic is altered it should offer either the dialog or fragment
    // cases
    @Override
    public void onTopTrackItemSelected(Bundle bundle) {
        Log.v(LOG_TAG, "onTopTrackItemSelected");

        // The device is smaller, so show the fragment fullscreen in the PlayerActivity
        Intent openPlayerIntent = new Intent(this, PlayerActivity.class);
        openPlayerIntent.putExtras(bundle);
        startActivity(openPlayerIntent);
    }
}
