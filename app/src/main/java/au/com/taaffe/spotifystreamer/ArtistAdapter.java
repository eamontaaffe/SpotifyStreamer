package au.com.taaffe.spotifystreamer;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import kaaes.spotify.webapi.android.models.Artist;

/**
 * Created by Eamon on 6/29/2015.
 */
public class ArtistAdapter extends ArrayAdapter<Artist> {

    Context context;
    int layoutResourceId;
    Artist data[] = null;

    public ArtistAdapter(Context context, int layoutResourceId, Artist[] data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        Artist artist = getItem(position);

        LayoutInflater mInflater = (LayoutInflater) context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.list_item_artist, null);
            holder = new ViewHolder();

            holder.textView = (TextView) convertView.findViewById(R.id.list_item_textview);
            holder.imageView = (ImageView) convertView.findViewById(R.id.list_item_icon);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.textView.setText(artist.name);

        // TODO Get the image from the spotify and render it
        //
        //artist.images.get(0);
        Image topImage;

        //
        holder.imageView.setImageResource(topImage);

        return convertView;
    }

    private class ViewHolder {
        ImageView imageView;
        TextView textView;
    }


}
