package com.qingcheng.home.custom.binder;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qingcheng.home.Launcher;
import com.qingcheng.home.LauncherAppState;
import com.qingcheng.home.R;
import com.qingcheng.home.custom.DataBindAdapter;
import com.qingcheng.home.custom.DataBinder;
import com.qingcheng.home.custom.ItemData;
import com.qingcheng.home.custom.QuicksearhData;
import com.qingcheng.home.custom.ViewItemType;
import com.ragentek.infostream.DataProviderInfo;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class ReaderBinder extends DataBinder<ReaderBinder.ViewHolder> implements View.OnClickListener {

    private static final String TAG = "ReaderBinder";
    private static final String READER_URL = "http://s.iyd.cn/mobile/book/index/qingchengh5/067900029";
    private static final int BOOK_COUNT_DATABASE = 9;
    private int mMaxId = 0;
    private ItemData mData = new QuicksearhData();
    private ViewHolder mHolder;


    private ArrayList<Reader> mBooks = null;
    private BooksObserver mBooksObserver;

    public ReaderBinder(DataBindAdapter dataBindAdapter) {
        super(dataBindAdapter);
    }

    @Override
    public ViewHolder newViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.widgets_reader, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void bindViewHolder(ViewHolder holder, int position) {
        mHolder = holder;

        holder.mMoreTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent("com.ragentek.infostream_fullnew");
                    intent.setClassName("com.ragentek.infostream3", "com.ragentek.infostream.FullNews");
                    intent.putExtra("url", READER_URL);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("read", true);
                    v.getContext().startActivity(intent);

                    ((Launcher) v.getContext()).overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

                } catch (Exception e) {
                    Log.e(TAG, "onClick: cannot find explicit activity " + e.getMessage());
                }
            }
        });

        holder.mBook1.setOnClickListener(this);
        holder.mBook2.setOnClickListener(this);
        holder.mBook3.setOnClickListener(this);
        setupViews(holder);
    }

    private void setupViews(ViewHolder holder) {
        Log.d(TAG, "setup views");
//        retrieveBooks();
        if (mBooks == null || mBooks.size() < 3) {
            refreshBooks();
        }

        if (mBooksObserver == null) {
            mBooksObserver = new BooksObserver(null);
            ContentResolver contentResolver = LauncherAppState.getInstance().getContext().getContentResolver();
            Uri uri = Uri.parse("content://com.ragentek.infostream.provider/books");
            contentResolver.registerContentObserver(uri, true, mBooksObserver);
        }
    }

    private void refreshBooks() {
        Log.d(TAG, "refresh books");
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                addBooks();
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (mBooks != null && mBooks.size() > 0) {
                    updateViews();
                }
            }
        }.execute();
    }

    private boolean addBooks() {//get books from database and clear old data
        Log.d(TAG, "add books");
        ContentResolver contentResolver = LauncherAppState.getInstance().getContext().getContentResolver();
        Uri uri = Uri.parse("content://com.ragentek.infostream.provider/books");
        Cursor cursor = null;
        boolean result = false;
        try {
            cursor = contentResolver.query(uri, null, null, null, null);
            if (cursor != null) {
                if (mBooks == null) {
                    mBooks = new ArrayList<Reader>();
                } else {
                    mMaxId = 0;
                    mBooks.clear();
                }

                while (cursor.moveToNext()) {
                    Reader bean = new Reader();
                    bean.setAuthor(cursor.getString(cursor.getColumnIndex(DataProviderInfo.AUTHOR_BOOKS)));
                    bean.setBook_pic_url(cursor.getString(cursor.getColumnIndex(DataProviderInfo.BOOK_PIC_URL_BOOKS)));
                    bean.setBook_score(cursor.getString(cursor.getColumnIndex(DataProviderInfo.BOOK_SCORE_BOOKS)));
                    bean.setBook_url(cursor.getString(cursor.getColumnIndex(DataProviderInfo.BOOK_URL_BOOKS)));
                    bean.setCategory_name(cursor.getString(cursor.getColumnIndex(DataProviderInfo.CAREGORY_NAME_BOOKS)));
                    bean.setCopyright_name(cursor.getString(cursor.getColumnIndex(DataProviderInfo.COPYRIGHT_NAME_BOOKS)));
                    bean.setCreate_date(cursor.getString(cursor.getColumnIndex(DataProviderInfo.CREATE_DATE_BOOKS)));
                    bean.setIncipit(cursor.getString(cursor.getColumnIndex(DataProviderInfo.INCIPIT_BOOKS)));
                    bean.setLastChapter(cursor.getString(cursor.getColumnIndex(DataProviderInfo.LAST_CHAPTER_BOOKS)));
                    bean.setLastChapterId(cursor.getString(cursor.getColumnIndex(DataProviderInfo.LAST_CHAPTER_ID_BOOKS)));
                    bean.setStatus(cursor.getString(cursor.getColumnIndex(DataProviderInfo.STATUS_BOOKS)));
                    bean.setResource_id(cursor.getString(cursor.getColumnIndex(DataProviderInfo.RESOURCE_ID_BOOKS)));
                    bean.setResource_name(cursor.getString(cursor.getColumnIndex(DataProviderInfo.RESOURCE_NAME_BOOKS)));
                    bean.setUdate(cursor.getString(cursor.getColumnIndex(DataProviderInfo.UPDATE_BOOKS)));
                    bean.setSummary(cursor.getString(cursor.getColumnIndex(DataProviderInfo.SUMMARY_BOOKS)));
                    bean.setWords(cursor.getInt(cursor.getColumnIndex(DataProviderInfo.WORDS_BOOKS)));
                    int cur = cursor.getInt(cursor.getColumnIndex(DataProviderInfo.ID_BOOKS));
                    if (cur > mMaxId) {
                        mMaxId = cur;
                    }
                    mBooks.add(bean);
                }

                if (mBooks.size() < 3) {
                    mMaxId = 0;
                    sendBroadcastRefreshBook();
                }

                Log.d(TAG, "cursor count = " + cursor.getCount() + " max id = " + mMaxId);
                if (mBooks.size() > BOOK_COUNT_DATABASE) {//clear old data
                    int tempLastId = mMaxId - BOOK_COUNT_DATABASE;
                    contentResolver.delete(uri, DataProviderInfo.ID_BOOKS + " <= ?", new String[]{String.valueOf(tempLastId)});
                    Log.d(TAG, "book database clear old data tempLastId=" + tempLastId);
                }
            } else {
                sendBroadcastRefreshBook();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            return result;
        }
    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 2) {
                if (mBooks != null && mBooks.size() > 0) {
                    updateViews();
                }
            }
            super.handleMessage(msg);
        }
    };

    private void updateViews() {
        if (mHolder == null) {
            return;
        }

        if (mBooks == null || mBooks.size() < 3) {
            return;
        }

        mHolder.mBookName1.setText(mBooks.get(0).getResource_name());
        mHolder.mBookName2.setText(mBooks.get(1).getResource_name());
        mHolder.mBookName3.setText(mBooks.get(2).getResource_name());

        Picasso.with(mHolder.mMoreTextView.getContext())
                .load(mBooks.get(0).getBook_pic_url())
                .placeholder(R.drawable.ic_book_placeholder_error)
                .error(R.drawable.ic_book_placeholder_error)
                .into(mHolder.mBookImg1);
        Picasso.with(mHolder.mMoreTextView.getContext())
                .load(mBooks.get(1).getBook_pic_url())
                .placeholder(R.drawable.ic_book_placeholder_error)
                .error(R.drawable.ic_book_placeholder_error)
                .into(mHolder.mBookImg2);
        Picasso.with(mHolder.mMoreTextView.getContext())
                .load(mBooks.get(2).getBook_pic_url())
                .placeholder(R.drawable.ic_book_placeholder_error)
                .error(R.drawable.ic_book_placeholder_error)
                .into(mHolder.mBookImg3);

        mHolder.mBook1.setTag(mBooks.get(0));
        mHolder.mBook2.setTag(mBooks.get(1));
        mHolder.mBook3.setTag(mBooks.get(2));
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    @Override
    public int getItemViewType() {
        return ViewItemType.VIEW_ITEM_READER;
    }

    @Override
    public void destroy() {
        if (mBooksObserver != null) {
            ContentResolver contentResolver = LauncherAppState.getInstance().getContext().getContentResolver();
            contentResolver.unregisterContentObserver(mBooksObserver);
            mBooksObserver = null;
        }

    }

    @Override
    public void addData(ItemData data) {
        if (mHolder == null) {
            return;
        }
        if (mBooks == null || mBooks.size() < 3) {
            refreshBooks();
        }
    }

    @Override
    public void updateData(ItemData data) {
    }

    @Override
    public void removeData(ItemData data) {
    }

    public void clear() {
        mData.clear();
        notifyBinderDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        Object tag = v.getTag();
        if (tag != null && tag instanceof Reader && v instanceof LinearLayout) {
            Reader reader = (Reader) tag;
            try {
                Intent intent = new Intent("com.ragentek.infostream_fullnew");
                intent.setClassName("com.ragentek.infostream3", "com.ragentek.infostream.FullNews");
                String url = reader.book_url;
                Log.d(TAG, "onClick: url = " + url);
                intent.putExtra("url", reader.resource_id);
                intent.putExtra("read", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                v.getContext().startActivity(intent);

                ((Launcher) v.getContext()).overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);


            } catch (Exception e) {
                Log.e(TAG, "onClick: cannot find explicit activity " + e.getMessage());
                try {
                    Intent intent = new Intent("com.ragentek.infostream_fullnew");
                    intent.setClassName("com.ragentek.infostream3", "com.ragentek.infostream.FullNews");
                    intent.putExtra("url", READER_URL);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("read", true);
                    v.getContext().startActivity(intent);
                    ((Launcher) v.getContext()).overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } catch (Exception e2) {
                    Log.e(TAG, "onClick: cannot find explicit activity " + e2.getMessage());
                }
            }
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mMoreTextView;
        private ImageView mBookImg1, mBookImg2, mBookImg3;
        private TextView mBookName1, mBookName2, mBookName3;
        private LinearLayout mBook1, mBook2, mBook3;
        private View mRootView;

        public ViewHolder(View view) {
            super(view);
            mRootView = view;
            mBookImg1 = (ImageView) view.findViewById(R.id.book_img_1);
            mBookImg2 = (ImageView) view.findViewById(R.id.book_img_2);
            mBookImg3 = (ImageView) view.findViewById(R.id.book_img_3);

            mBookName1 = (TextView) view.findViewById(R.id.book_name_1);
            mBookName2 = (TextView) view.findViewById(R.id.book_name_2);
            mBookName3 = (TextView) view.findViewById(R.id.book_name_3);

            mBook1 = (LinearLayout) view.findViewById(R.id.book1);
            mBook2 = (LinearLayout) view.findViewById(R.id.book2);
            mBook3 = (LinearLayout) view.findViewById(R.id.book3);

            mMoreTextView = (TextView) view.findViewById(R.id.widget_books_more);

            Context context = view.getContext();
            if (context.getSharedPreferences(LauncherAppState.NAME_CUSTOM_SHARE, Context.MODE_PRIVATE).getBoolean(LauncherAppState.KEY_IS_PROJECTION_WALLPAPER, true)) {
                ((CardView)view.findViewById(R.id.custom_card)).setCardBackgroundColor(context.getColor(R.color.card_bg_project));
                view.findViewById(R.id.custom_ll_title).setBackgroundColor(context.getColor(R.color.widget_title_bg_project));
                ((TextView)view.findViewById(R.id.widget_books_title)).setTextColor(context.getColor(R.color.widget_title_textcolor_project));
                mMoreTextView.setTextColor(context.getColor(R.color.widget_title_textcolor_project));
                mBookName1.setTextColor(context.getColor(R.color.toutiao_list_item_project));
                mBookName2.setTextColor(context.getColor(R.color.toutiao_list_item_project));
                mBookName3.setTextColor(context.getColor(R.color.toutiao_list_item_project));
            } else {
                ((CardView)view.findViewById(R.id.custom_card)).setCardBackgroundColor(context.getColor(R.color.card_bg));
                view.findViewById(R.id.custom_ll_title).setBackgroundColor(context.getColor(R.color.widget_title_bg));
                ((TextView)view.findViewById(R.id.widget_books_title)).setTextColor(context.getColor(R.color.widget_title_textcolor));
                mMoreTextView.setTextColor(context.getColor(R.color.widget_title_textcolor));
                mBookName1.setTextColor(context.getColor(R.color.toutiao_list_item));
                mBookName2.setTextColor(context.getColor(R.color.toutiao_list_item));
                mBookName3.setTextColor(context.getColor(R.color.toutiao_list_item));
            }

        }
    }

    private class BooksObserver extends ContentObserver {

        public BooksObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            if (canRefresh()) {
                refreshBooks();
                Log.d(TAG, "database change, can refresh");
            } else {
                Log.d(TAG, "database change, not refresh");
            }
        }
    }

    private boolean canRefresh() {
        ContentResolver contentResolver = LauncherAppState.getInstance().getContext().getContentResolver();
        Uri uri = Uri.parse("content://com.ragentek.infostream.provider/books");
        Cursor cursor = null;
        boolean result = false;
        try {
            cursor = contentResolver.query(uri, null, DataProviderInfo.ID_BOOKS + " >= ? ", new String[]{String.valueOf(mMaxId)}, null);
            if (cursor != null && cursor.getCount() >= 3) {
                result = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            return result;
        }
    }

    private void sendBroadcastRefreshBook() {
        Intent intent = new Intent();
        intent.setAction("com.ragentek.infostream.refresh.book");
        LauncherAppState.getInstance().getContext().sendBroadcast(intent);
        Log.d(TAG, "send broadcast , request for book from web");
    }

    private class Reader {

        /**
         * summary : 一段离奇的穿越，一卷惊世的秘典，带着一个懵懂的少年来到了神秘的异界大陆，一段血仇背后居然隐藏着惊天的阴谋，所有人都被“天”算计了，而他却算计了“天”。
         * category_name : 东方玄幻
         * author : 半块铜板
         * words : 3993013
         * lastChapter : 第1450章 我为天君（大结局）
         * copyright_name : 3G原创
         * incipit : 懵懂的少年来到了神秘异界，所有人都被天道算计了，他却算计天道！
         * book_pic_url : http://farm3.static.mitang.com/M01/A7/7E/p4YBAFdGvtKAI8NKAAAafwS3AJ0287/135x180_75418_cover.jpg?mobileNetDownload=false
         * lastChapterId : 5752572
         * resource_id : 75418
         * book_score : 5
         * resource_name : 武道天途
         * udate : 2015-10-19 10:11:38
         * create_date : 2013-04-09 12:05:26
         * status : 全本
         */

        private String summary;
        private String category_name;
        private String author;
        private int words;
        private String lastChapter;
        private String copyright_name;
        private String incipit;
        private String book_pic_url;
        private String lastChapterId;
        private String resource_id;
        private String book_score;
        private String resource_name;
        private String udate;
        private String create_date;
        private String status;
        public String book_url;

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public String getCategory_name() {
            return category_name;
        }

        public void setCategory_name(String category_name) {
            this.category_name = category_name;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public int getWords() {
            return words;
        }

        public void setWords(int words) {
            this.words = words;
        }

        public String getLastChapter() {
            return lastChapter;
        }

        public void setLastChapter(String lastChapter) {
            this.lastChapter = lastChapter;
        }

        public String getCopyright_name() {
            return copyright_name;
        }

        public void setCopyright_name(String copyright_name) {
            this.copyright_name = copyright_name;
        }

        public String getIncipit() {
            return incipit;
        }

        public void setIncipit(String incipit) {
            this.incipit = incipit;
        }

        public String getBook_pic_url() {
            return book_pic_url;
        }

        public void setBook_pic_url(String book_pic_url) {
            this.book_pic_url = book_pic_url;
        }

        public String getLastChapterId() {
            return lastChapterId;
        }

        public void setLastChapterId(String lastChapterId) {
            this.lastChapterId = lastChapterId;
        }

        public String getResource_id() {
            return resource_id;
        }

        public void setResource_id(String resource_id) {
            this.resource_id = resource_id;
        }

        public String getBook_score() {
            return book_score;
        }

        public void setBook_score(String book_score) {
            this.book_score = book_score;
        }

        public String getResource_name() {
            return resource_name;
        }

        public void setResource_name(String resource_name) {
            this.resource_name = resource_name;
        }

        public String getUdate() {
            return udate;
        }

        public void setUdate(String udate) {
            this.udate = udate;
        }

        public String getCreate_date() {
            return create_date;
        }

        public void setCreate_date(String create_date) {
            this.create_date = create_date;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getBook_url() {
            return book_url;
        }

        public void setBook_url(String book_url) {
            this.book_url = book_url;
        }
    }

}
