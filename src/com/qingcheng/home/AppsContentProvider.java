package com.qingcheng.home;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

public class AppsContentProvider extends ContentProvider {
    private final static String AUTHORITH = "com.qingcheng.home.recommend_apps";

    private final static int VERSION  = 1;

    private final static String DATABASE_NAME = "apps_db";

    private final static String TABLE_NAME = "apps";

    //packagename text, classname text, clicktime text, duration text
    public final static String TABLE_COLUMN_ID = "_id";

    public final static String TABLE_COLUMN_NAME = "packagename";

    public final static String TABLE_COLUMN_CLASS = "classname";

    public final static String TABLE_COLUMN_TIME = "clicktime";

    public final static String TABLE_COLUMN_DURATION = "duration";

    private AppsDatabaseHelper mHelper = null;

    public AppsContentProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        LauncherProvider.SqlArguments args = new LauncherProvider.SqlArguments(uri, selection, selectionArgs);

        SQLiteDatabase db = mHelper.getWritableDatabase();
        if(getContext() != null && getContext().getContentResolver() != null){
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return db.delete(args.table, args.where, args.args);
    }

    @Override
    public String getType(@NonNull Uri uri) {
        LauncherProvider.SqlArguments args = new LauncherProvider.SqlArguments(uri, null, null);
        if (TextUtils.isEmpty(args.where)) {
            return "vnd.android.cursor.dir/" + args.table;
        } else {
            return "vnd.android.cursor.item/" + args.table;
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        SQLiteDatabase database = mHelper.getWritableDatabase();
        long id = database.insert(TABLE_NAME, null, values);
        if(getContext() != null && getContext().getContentResolver() != null){
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public boolean onCreate() {
        mHelper  = new AppsDatabaseHelper(getContext(), DATABASE_NAME, null, VERSION);
        return false;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteDatabase database = mHelper.getWritableDatabase();
        SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
        sqlBuilder.setTables(TABLE_NAME);

        Cursor c = sqlBuilder.query(
                database,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);

        //---register to watch a content URI for changes---
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        LauncherProvider.SqlArguments args = new LauncherProvider.SqlArguments(uri, selection, selectionArgs);
        SQLiteDatabase db = mHelper.getWritableDatabase();
        return db.update(args.table, values, args.where, args.args);
    }

}
