package at.neiti.scribblesmart;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Data class containing infos for one record of a note.
 * Implements Parcelable to be used with Intents
 * 
 * @author markus
 * 
 */
public class RecordInfo implements Parcelable {

    private String path;
    private String total;

    public RecordInfo(String path, String total) {
        this.path = path;
        this.total = total;
    }

    public RecordInfo(Parcel in) {
        readFromParcel(in);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(path);
        dest.writeString(total);
    }

    private void readFromParcel(Parcel in) {
        path = in.readString();
        total = in.readString();
    }

    public static final Parcelable.Creator<RecordInfo> CREATOR = new Parcelable.Creator<RecordInfo>() {
        public RecordInfo createFromParcel(Parcel in) {
            return new RecordInfo(in);
        }

        public RecordInfo[] newArray(int size) {
            return new RecordInfo[size];
        }
    };

}
