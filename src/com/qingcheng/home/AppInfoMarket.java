package com.qingcheng.home;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by zhuzi on 17-2-28.
 */

public class AppInfoMarket extends AppInfo implements Parcelable {
    public String packageName;
    public String versionCode;
    public String downloadUrl;
    public String logoUrl;
    public int downloadProgress;
    public int downloadStatus;

    AppInfoMarket() {
        itemType = LauncherSettings.BaseLauncherColumns.ITEM_TYPE_RECOMMEND_APP;
    }

    @Override
    public String toString() {
        return "title = " + title + ", packageName = " + packageName + ", downloadUrl = " + downloadUrl + ", logoUrl = " + logoUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.packageName);
        dest.writeString(this.versionCode);
        dest.writeString(this.downloadUrl);
        dest.writeString(this.logoUrl);
        dest.writeInt(this.downloadProgress);
        dest.writeInt(this.downloadStatus);
    }

    protected AppInfoMarket(Parcel in) {
        this.packageName = in.readString();
        this.versionCode = in.readString();
        this.downloadUrl = in.readString();
        this.logoUrl = in.readString();
        this.downloadProgress = in.readInt();
        this.downloadStatus = in.readInt();
    }

    public static final Parcelable.Creator<AppInfoMarket> CREATOR = new Parcelable.Creator<AppInfoMarket>() {
        @Override
        public AppInfoMarket createFromParcel(Parcel source) {
            return new AppInfoMarket(source);
        }

        @Override
        public AppInfoMarket[] newArray(int size) {
            return new AppInfoMarket[size];
        }
    };
}
