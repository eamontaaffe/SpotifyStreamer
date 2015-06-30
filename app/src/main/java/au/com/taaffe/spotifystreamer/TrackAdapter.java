package au.com.taaffe.spotifystreamer;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Track;

/**
 * Created by Eamon on 6/30/2015.
 */
public class TrackAdapter extends ArrayAdapter<Track> {


    Context context;
    int layoutResourceId;
    List<Track> data = null;

    public TrackAdapter(Context context, int layoutResourceId, List<Track> data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        Track track = getItem(position);

        LayoutInflater mInflater = (LayoutInflater) context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.list_item_track, null);
            holder = new ViewHolder();

            holder.trackView = (TextView) convertView.findViewById(R.id.list_item_track);
            holder.albumView = (TextView) convertView.findViewById(R.id.list_item_album);
            holder.imageView = (ImageView) convertView.findViewById(R.id.list_item_icon);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.trackView.setText(track.name);
        holder.albumView.setText(track.album.name);

        // TODO Get the image from the spotify and render it
        if (!track.album.images.isEmpty()) {
            String url = track.album.images.get(0).url;
            Picasso.with(context)
                    .load(url)
                    .fit()
                    .centerCrop()
                    .into(holder.imageView);
        }

        return convertView;
    }

    private class ViewHolder {
        ImageView imageView;
        TextView trackView;
        TextView albumView;
    }


}
