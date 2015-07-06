package au.com.taaffe.spotifystreamer;

import android.os.Parcel;
import android.os.Parcelable;

import kaaes.spotify.webapi.android.models.Track;

/**
 * Created by Eamon on 7/5/2015.
 * This is a simplified version of the Track class that only contains the neccessary information
 * required for the player view. Since it is difficult to make Spotify api track class parcelable
 * due to complex data structures, this class will not extend Track.
 */
public class ParcelableTrack implements Parcelable {
    public String track_name;
    public String artist;
    public String album;
    public String track_image_url;
    public String track_preview_url;


    public ParcelableTrack(Track track, String artist) {
        this.track_name = track.name;
        this.artist = artist;
        this.album = track.album.name;
        this.track_image_url = track.album.images.get(0).url;
        this.track_preview_url = track.preview_url;
    }

    // Constructor used for recreating object from parcel
    public ParcelableTrack(Parcel parcel) {
        String[] data = new String[5];
        parcel.readStringArray(data);

        this.track_name = data[0];
        this.artist = data[1];
        this.album = data[2];
        this.track_image_url = data[3];
        this.track_preview_url = data[4];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[] {
                this.track_name,
                this.artist,
                this.album,
                this.track_image_url,
                this.track_preview_url
        });

    }

    public static final Parcelable.Creator<ParcelableTrack> CREATOR =
            new Parcelable.Creator<ParcelableTrack>() {
        @Override
        public ParcelableTrack createFromParcel(Parcel source) {
            return new ParcelableTrack(source);
        }

        @Override
        public ParcelableTrack[] newArray(int size) {
            return new ParcelableTrack[size];
        }
    };


}
