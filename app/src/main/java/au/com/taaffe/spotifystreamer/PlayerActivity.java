package au.com.taaffe.spotifystreamer;

import android.content.Intent;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class PlayerActivity extends ActionBarActivity
        implements PlayerDialogFragment.PlayerDialogFragmentListener{

    private ShareActionProvider mShareActionProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        if(savedInstanceState == null) {

            Bundle arguments = getIntent().getExtras();

            PlayerDialogFragment newPlayerDialogFragment = new PlayerDialogFragment();

            newPlayerDialogFragment.setArguments(arguments);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.player_container, newPlayerDialogFragment)
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_player, menu);

        getMenuInflater().inflate(R.menu.menu_player_dialog_fragment, menu);

        // Retrieve the share menu item
        MenuItem menuItem = menu.findItem(R.id.action_share);

        // Get the provider and hold onto it to set/change the share intent.
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        updateShareIntent(null);

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

    @Override
    public void onPlayerServiceComplete() {
        finish();
    }

    @Override
    public void updateShareIntent(String previewUrl) {
        if(mShareActionProvider != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            shareIntent.setType("text/plain");
            if (previewUrl == null) {
                shareIntent.putExtra(Intent.EXTRA_TEXT, PlayerDialogFragment.SPOTIFY_SHARE_HASHTAG);
            } else {
                shareIntent.putExtra(Intent.EXTRA_TEXT, previewUrl + PlayerDialogFragment.SPOTIFY_SHARE_HASHTAG);
            }
            mShareActionProvider.setShareIntent(shareIntent);
        }
    }
}
