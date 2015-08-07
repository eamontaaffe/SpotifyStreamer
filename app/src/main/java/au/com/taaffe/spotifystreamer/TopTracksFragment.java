package au.com.taaffe.spotifystreamer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.wasabeef.picasso.transformations.BlurTransformation;
import jp.wasabeef.picasso.transformations.gpu.KuwaharaFilterTransformation;
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

    private static final String ACTIVITY_COLORS = "activity_colors";
    private static final String VIBRANT_COLOR = "vibrant_color";
    private static final String DARK_VIBRANT_COLOR = "dark_vibrant_color";
    private static final String ARTIST_BITMAP = "artist_bitmap";

    private TrackAdapter mTrackAdapter;
    private String mName;
    private int mVibrantColor = -1;
    private int mDarkVibrantColor = -1;
    private String mArtistImageUrl;
    private TopTracksListener mTopTracksListener;
    private Bitmap mArtistBitmap;
    private FrameLayout mArtistImageFrameLayout;
    private boolean mAttached = false;

    ArrayList <ParcelableTrack> parcelableTracks = new ArrayList<ParcelableTrack>();

    public TopTracksFragment() {
    }

    public interface TopTracksListener {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        public void onTopTrackItemSelected(Bundle bundle);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mAttached = true;

        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mTopTracksListener = (TopTracksListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mAttached = false;
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
        mArtistImageFrameLayout = (FrameLayout) rootView.findViewById(R.id.artist_image_frame_layout);

        if(savedInstanceState != null && savedInstanceState.containsKey(ACTIVITY_COLORS)) {
            Bundle colors = savedInstanceState.getBundle(ACTIVITY_COLORS);
            setActivityColors(colors.getInt(VIBRANT_COLOR),colors.getInt(DARK_VIBRANT_COLOR));
        }
        if(savedInstanceState != null && savedInstanceState.containsKey(ARTIST_BITMAP)) {
            final ImageView artistImageView =
                    (ImageView) rootView.findViewById(R.id.artist_imageview);

            mArtistBitmap = savedInstanceState.getParcelable(ARTIST_BITMAP);

            artistImageView.setImageBitmap(mArtistBitmap);
        } else if (savedInstanceState != null) {
            mArtistImageFrameLayout.setVisibility(View.GONE);
        }

        // If the trackAdapter is null than it is not being restored from a configuration change
        // so we need to initialise the top tracks list and adapter
        if (mTrackAdapter == null) {
            List<Track> tracks = new ArrayList<Track>();

            mTrackAdapter = new TrackAdapter(
                    getActivity(),
                    R.layout.list_item_track,
                    tracks
            );


            Log.v(LOG_TAG,"onCreateView");
            Bundle arguments = getArguments();


            // If the intent has artist info then populate the list
            if (arguments != null) {
                String id = arguments.getString(ARTIST_ID);
                mName = arguments.getString(ARTIST_NAME);


                if (arguments.containsKey(ARTIST_IMAGE_URL)) {
                    final ImageView artistImageView =
                            (ImageView) rootView.findViewById(R.id.artist_imageview);
                    mArtistImageUrl = arguments.getString(ARTIST_IMAGE_URL);

                    loadImageIntoView(mArtistImageUrl,artistImageView);
                } else {
                    mArtistImageFrameLayout.setVisibility(View.GONE);
                }



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
                    Bundle bundle = new Bundle();
                    bundle.putParcelableArrayList(PlayerDialogFragment.TRACK_LIST, parcelableTracks);
                    bundle.putInt(PlayerDialogFragment.TRACK_INDEX, position);


                    if (mVibrantColor != -1 && mDarkVibrantColor != -1) {
                        Bundle colors = new Bundle();
                        colors.putInt(PlayerDialogFragment.VIBRANT_COLOR, mVibrantColor);
                        colors.putInt(PlayerDialogFragment.DARK_VIBRANT_COLOR, mDarkVibrantColor);
                        bundle.putParcelable(PlayerDialogFragment.COLORS, colors);
                    }

                    mTopTracksListener.onTopTrackItemSelected(bundle);
                }
            }
        });

        return rootView;
    }

    public void loadImageIntoView(String imageUrl, final ImageView view) {

        Picasso.with(getActivity())
                .load(imageUrl)
                .into(view, new Callback() {
                    @Override
                    public void onSuccess() {
                        mArtistBitmap =
                                ((BitmapDrawable) view.getDrawable()).getBitmap();
                        Log.v(LOG_TAG, "Picasso Callback OnSuccess");
                        Palette.generateAsync(mArtistBitmap, new Palette.PaletteAsyncListener() {
                            private String LOG_TAG =
                                    Palette.PaletteAsyncListener.class.getSimpleName();

                            @Override
                            public void onGenerated(Palette palette) {
                                setActivityColors(palette.getVibrantColor(R.color.primary),
                                        palette.getDarkVibrantColor(R.color.primary_dark));
                            }
                        });
                    }

                    @Override
                    public void onError() {
                        Log.d(LOG_TAG, "Picasso Callback OnFailure");
                    }
                });
    }

    public void setActivityColors(int vibrantColor, int darkVibrantColor) {
        mVibrantColor = vibrantColor;
        mDarkVibrantColor = darkVibrantColor;

        ActionBarActivity activity = (ActionBarActivity) getActivity();

        if (activity != null) {
            android.support.v7.app.ActionBar mActionBar = activity.getSupportActionBar();
            Window window = getActivity().getWindow();

            ColorDrawable vibrantColorDrawable = new ColorDrawable(
                    mVibrantColor);

            if (mActionBar != null && window != null) {
                mActionBar.setBackgroundDrawable(vibrantColorDrawable);

                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    window.setStatusBarColor(mDarkVibrantColor);
                }
            }
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mName != null) {
            ((ActionBarActivity)getActivity()).getSupportActionBar().setSubtitle(mName);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        if (mVibrantColor != -1 && mDarkVibrantColor != -1) {
            Bundle colors = new Bundle();
            colors.putInt(VIBRANT_COLOR, mVibrantColor);
            colors.putInt(DARK_VIBRANT_COLOR, mDarkVibrantColor);
            outState.putBundle(ACTIVITY_COLORS, colors);
        }

        if(mArtistBitmap != null) {
            outState.putParcelable(ARTIST_BITMAP, mArtistBitmap);
        }

        super.onSaveInstanceState(outState);
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

            SharedPreferences preferences =
                    PreferenceManager.getDefaultSharedPreferences(getActivity());

            String regionKey = getString(R.string.pref_spotify_region_key);
            String regionDefaultValue = getString(R.string.pref_spotify_region_value_default);
            String region = preferences.getString(regionKey,regionDefaultValue);

            options.put("country", region);

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
                if(mAttached) {
                    Toast.makeText(getActivity()
                            , getResources().getText(R.string.null_results),
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
        }
    }
}
