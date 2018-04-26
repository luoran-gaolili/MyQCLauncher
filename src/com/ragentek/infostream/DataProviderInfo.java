package com.ragentek.infostream;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 *
 * Defines constants for accessing the content provider defined in DataProvider. A content provider
 * contract assists in accessing the provider's available content URIs, column names, MIME types,
 * and so forth, without having to know the actual values.
 */
public final class DataProviderInfo implements BaseColumns {

    private DataProviderInfo() { }

    // The URI scheme used for content URIs
    public static final String SCHEME = "content";

    // The provider's authority
    public static final String AUTHORITY = "com.ragentek.infostream.provider";

    /**
     * The DataProvider content URI
     */
    public static final Uri CONTENT_URI = Uri.parse(SCHEME + "://" + AUTHORITY);

    /**
     *  The MIME type for a content URI that would return multiple rows
     *  <P>Type: TEXT</P>
     */
    public static final String MIME_TYPE_ROWS =
            "vnd.android.cursor.dir/vnd.com.ragentek.infostream.provider";

    /**
     * The MIME type for a content URI that would return a single row
     *  <P>Type: TEXT</P>
     *
     */
    public static final String MIME_TYPE_SINGLE_ROW =
            "vnd.android.cursor.item/vnd.com.ragentek.infostream.provider";

    public static final String DATABASE_TODAY_NEWS_TABLE_NAME = "newsTodayItems";

    public static final Uri TODAY_NEWS_TABLE_CONTENTURI =
            Uri.withAppendedPath(CONTENT_URI, DATABASE_TODAY_NEWS_TABLE_NAME);

    public static final String ID_TODAY_NEWS = "_id";
    public static final String URL_TODAY_NEWS = "url";
    public static final String TITLE_TODAY_NEWS = "title";
    public static final String PIC_TODAY_NEWS = "pic";
    public static final String BIGPIC_TODAY_NEWS = "bigpic";
    public static final String PIC1_TODAY_NEWS = "pic1";
    public static final String PIC2_TODAY_NEWS = "pic2";
    public static final String PIC3_TODAY_NEWS = "pic3";
    public static final String AD_TODAY_NEWS = "adId";
    public static final String SOURCE_TODAY_NEWS = "source";
    public static final String DATE_TODAY_NEWS = "date";
    public static final String TYPE_TODAY_NEWS = "type";
    public static final String CHANNEL_TODAY_NEWS = "channel";
    public static final String DEFAULT_SORT_ORDER_TODAY_NEWS = ID_TODAY_NEWS + " DESC";


    public static final String DATABASE_BOOKS_TABLE_NAME = "books";

    public static final Uri BOOKS_TABLE_CONTENTURI =
            Uri.withAppendedPath(CONTENT_URI, DATABASE_BOOKS_TABLE_NAME);

    public static final String ID_BOOKS = "_id";
    public static final String SUMMARY_BOOKS = "summary";
    public static final String CAREGORY_NAME_BOOKS = "category_name";
    public static final String AUTHOR_BOOKS = "author";
    public static final String WORDS_BOOKS = "words";
    public static final String LAST_CHAPTER_BOOKS = "lastChapter";
    public static final String COPYRIGHT_NAME_BOOKS = "copyright_name";
    public static final String INCIPIT_BOOKS = "incipit";
    public static final String BOOK_PIC_URL_BOOKS = "book_pic_url";
    public static final String LAST_CHAPTER_ID_BOOKS = "lastChapterId";
    public static final String RESOURCE_ID_BOOKS = "resource_id";
    public static final String BOOK_SCORE_BOOKS = "book_score";
    public static final String RESOURCE_NAME_BOOKS = "resource_name";
    public static final String UPDATE_BOOKS = "udate";
    public static final String CREATE_DATE_BOOKS = "create_date";
    public static final String STATUS_BOOKS = "status";
    public static final String BOOK_URL_BOOKS = "book_url";
    public static final String DEFAULT_SORT_ORDER_BOOKS = ID_BOOKS + " DESC";

    // The starting version of the database
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "info.db";

}