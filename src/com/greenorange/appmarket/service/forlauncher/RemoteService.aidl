// RemoteService.aidl
package com.greenorange.appmarket.service.forlauncher;

// Declare any non-default types here with import statements
import com.greenorange.appmarket.service.forlauncher.RemoteDownloadListener;

interface RemoteService {
    int checkAppStatus(in String packageName);
    void dlButtonOnclick(in String packageName, in String downlaodUrl, RemoteDownloadListener remoteDownloadListener);
}