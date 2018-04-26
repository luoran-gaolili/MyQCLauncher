// RemoteDownloadListener.aidl
package com.greenorange.appmarket.service.forlauncher;

// Declare any non-default types here with import statements

interface RemoteDownloadListener {
	void onDownloading(String packageParam2, int progress);
	void onDownloadSuccess(String packageParam3);
	void onDownloadFailed(String packageParam4);
	void onDownloadStatusChange(String packageParam5, int status);
}