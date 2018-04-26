package com.qingcheng.home.custom.binder;

import com.qingcheng.home.Launcher;
import com.qingcheng.home.LauncherAppState;
import com.qingcheng.home.R;
import com.qingcheng.home.custom.DataBindAdapter;
import com.qingcheng.home.custom.DataBinder;
import com.qingcheng.home.custom.ItemData;
import com.qingcheng.home.custom.ViewItemType;
import com.ragentek.infostream.DataProviderInfo;
import com.squareup.picasso.Picasso;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Locale;

public class NewsBinder extends DataBinder<NewsBinder.ViewHolder> {

    public static final int TYPE_2G = 1;
    public static final int TYPE_3G = 2;
    public static final int TYPE_4G = 3;
    public static final int TYPE_WIFI = 4;
    public static final String TOKEN_URL = "http://open.snssdk.com/auth/access/device/";
    protected static final String EVENT_REPORT_URL = "http://open.snssdk.com/log/app_log_for_partner/v1/";
    protected static final String AD_REPORT_URL = "http://open.snssdk.com/log/app_log_for_partner/v2/";

    public static String APISTRING = "qingchengsp_api";            // "qingchengzm_api";	 // "qingchengsuoping";
    public static String KEYSTRING = "d4aa6f6cdcaff892756c4ace7952a0c2";    //"71d0721cab8aa6f1d25e46c92179d8bc";	//"63f1777241cbee8d968414638554e76b";

    private static final String TAG = "NewsBinder";
    private static final String ALL_CHANNEL = "news_hot";
    public int touchOffset;
    private Animation mAnimation;
    private ToutiaoNewsAdapter mAdapter;
    private ArrayList<ToutiaoNewsEntity> mNesDataList = null;
    private ViewHolder mHolder;
    private String token = null;
    private NewsObserver mNewsObserver;
    private int mMaxIdInLauncher = 0;
    private long mLastClickTime = 0;
    private SharedPreferences sharedPreferences = null;
    private long token_expires = 0;
    private TelephonyManager tm = null;
    private ConnectivityManager cm = null;
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                mHolder.mRefreshLayout.setRefreshing(false);
                mHolder.mRefresh.setVisibility(View.GONE);
                mHolder.mImageRefresh.setVisibility(View.VISIBLE);
                if (mAnimation == null) {
                    mAnimation = AnimationUtils.loadAnimation(LauncherAppState.getInstance().getContext(),
                            R.anim.info_rotate_refresh);
                }
                mHolder.mImageRefresh.startAnimation(mAnimation);
            } else if (msg.what == 2) {
                mHolder.mImageRefresh.clearAnimation();
                mHolder.mRefresh.setVisibility(View.VISIBLE);
                mHolder.mImageRefresh.setVisibility(View.GONE);
                mHolder.mRefreshLayout.setRefreshing(false);
                if (mNesDataList != null && mNesDataList.size() >= 3) {
                    mAdapter.clearItem();
                    LauncherAppState.mLastNewsUpdateTime = System.currentTimeMillis();
                    if (mNesDataList.size() > 3) {
                        for (ToutiaoNewsEntity entity : mNesDataList.subList(0, 3)) {
                            mAdapter.addItem(entity);
                        }
                    } else {
                        for (ToutiaoNewsEntity entity : mNesDataList) {
                            mAdapter.addItem(entity);
                        }
                    }
                    mAdapter.notifyDataSetChanged();
//                    mHolder.mListView.setSelection(0);
                }
            }
            super.handleMessage(msg);
        }
    };

    public NewsBinder(DataBindAdapter dataBindAdapter) {
        super(dataBindAdapter);
    }

    @Override
    public ViewHolder newViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.widgets_news, parent, false);
        touchOffset = parent.getContext().getResources().getDimensionPixelOffset(R.dimen.touch_offset);
        mAnimation = AnimationUtils.loadAnimation(parent.getContext(), R.anim.info_rotate_refresh);
        return new ViewHolder(view);
    }

    @Override
    public void addData(ItemData data) {
        Log.d(TAG, "add data");
        if (mHolder == null) {
            return;
        }

        if (mNesDataList == null || mNesDataList.size() < 3) {
            refreshNews();
        }
    }

    @Override
    public void updateData(ItemData data) {
        Log.d(TAG, "update data");
    }

    @Override
    public void removeData(ItemData data) {

    }

    @Override
    public void bindViewHolder(ViewHolder holder, int position) {
        mHolder = holder;
//        Log.e(TAG, "bindViewHolder: position " + position);
//        Log.e(TAG, "bindViewHolder: mRefreshLayout height  " + mHolder.mRefreshLayout.getMeasuredHeight());
//        Log.e(TAG, "bindViewHolder: mListView height  " + mHolder.mListView.getMeasuredHeight());
        setupNews(holder);
//        mHolder.itemView.requestLayout();
    }

    private void setupNews(final ViewHolder holder) {
        sharedPreferences = holder.mListView.getContext().getSharedPreferences("news", Context.MODE_PRIVATE);
        mAdapter = new ToutiaoNewsAdapter(holder.mListView.getContext(), ALL_CHANNEL, 0);
        cm = (ConnectivityManager) holder.mListView.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        tm = (TelephonyManager) holder.mListView.getContext().getSystemService(Context.TELEPHONY_SERVICE);

        mAdapter.setActivity((Activity) holder.mListView.getContext());

        holder.mListView.setAdapter(mAdapter);

        expandViewTouchDelegate(holder.mRefreshLayout, touchOffset, touchOffset, touchOffset, touchOffset);
        holder.mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
//                retrieveNews();
                long now = System.currentTimeMillis();
                if (now - mLastClickTime > 1000) {
                    mLastClickTime = now;
                    refreshNews();
                }
            }
        });
        holder.mMoreView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.setClassName("com.ragentek.infostream3", "com.ragentek.infostream.NewsMoreActivity");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("token", getToken());
                    v.getContext().startActivity(intent);
                    ((Launcher) v.getContext()).overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } catch (Exception e) {
                    Log.e(TAG, "can not find com.ragentek.infostream.NewsMoreActivity " + e.getMessage());
                }
            }
        });
        holder.mRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                retrieveNews();
                long now = System.currentTimeMillis();
                if (now - mLastClickTime > 1000) {
                    mLastClickTime = now;
                    refreshNews();
                }
            }
        });

        if (mNewsObserver == null) {
            ContentResolver contentResolver = LauncherAppState.getInstance().getContext().getContentResolver();
            Uri uri = Uri.parse("content://com.ragentek.infostream.provider/newsTodayItems");
            mNewsObserver = new NewsObserver(null);
            contentResolver.registerContentObserver(uri, true, mNewsObserver);
        }

        if (mNesDataList == null || mNesDataList.size() < 3) {
            refreshNews();
        }
    }

    public String getToken() {
        if (token == null) {
            token = sharedPreferences.getString("access_token", null);
            token_expires = sharedPreferences.getLong("token_expires", 0);
        }
        if (token_expires < System.currentTimeMillis() / 1000) {
            retrieveNewToken();
        }
        return token;
    }

    protected void upateToken(String token, int expires) {
        this.token = token;
        this.token_expires = System.currentTimeMillis() / 1000 + expires;
        sharedPreferences.edit().putString("access_token", this.token).putLong("token_expires", this.token_expires).commit();
    }

    public boolean retrieveNewToken() {
        if (LauncherAppState.getNewsParams() == null) {
            return false;
        }
        String result = sendHttpRequest(LauncherAppState.getNewsUrlSb().toString(), LauncherAppState.getNewsParams().toString().getBytes());
        if (result == null) {
            return false;
        }

        try {
            JSONObject root = new JSONObject(result);
            if (root.getInt("ret") == 0) {
                JSONObject data = root.getJSONObject("data");
                upateToken(data.getString("access_token"), data.getInt("expires_in"));
            }
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return false;
    }

    public String sendHttpRequest(String baseUrl, byte[] data) {
        HttpURLConnection c = null;
        try {
            URL url = new URL(baseUrl);
            c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("POST");
            c.setConnectTimeout(1000);
            c.setDoInput(true);
            c.setDoOutput(true);
            c.setUseCaches(false);
            c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            c.setRequestProperty("Content-Length", String.valueOf(data.length));
            OutputStream outputStream = c.getOutputStream();
            outputStream.write(data);

            int status = c.getResponseCode();
            switch (status) {
                case 200:
                case 201:
                    BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    StringBuffer sb = new StringBuffer();
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();
                    return sb.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private void sendBroadcastRefresh() {
        Intent intent = new Intent();
        intent.setAction("com.ragentek.infostream.refresh");
        LauncherAppState.getInstance().getContext().sendBroadcast(intent);
    }

    public void expandViewTouchDelegate(final View view, final int top,
                                        final int bottom, final int left, final int right) {
        ((View) view.getParent()).post(new Runnable() {
            @Override
            public void run() {
                Rect bounds = new Rect();
                view.setEnabled(true);
                view.getHitRect(bounds);

                bounds.top -= top;
                bounds.bottom += bottom;
                bounds.left -= left;
                bounds.right += right;

                TouchDelegate touchDelegate = new TouchDelegate(bounds, view);

                if (View.class.isInstance(view.getParent())) {
                    ((View) view.getParent()).setTouchDelegate(touchDelegate);
                }
            }
        });
    }

    private void refreshNews() {//first get news from memory or database, if both fail:send broadcast
        mHandler.sendEmptyMessage(1);
        final AsyncTask<Void, Void, Boolean> asyncRefreshNews = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                if (mNesDataList != null && mNesDataList.size() >= 6) {
                    Log.d(TAG, "refresh news from memory size=" + mNesDataList.size());
                    mNesDataList.subList(0, 3).clear();
                    Log.d(TAG, "after clear size=" + mNesDataList.size());
                    return false;
                } else {
                    Log.d(TAG, "refresh news from database");
                    return addNews();//get news from database
                }
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                if (aBoolean) {
                    mHandler.sendEmptyMessageDelayed(2, 3000);//wait for database change
                } else {
                    mHandler.sendEmptyMessage(2);
                }
            }
        };
        asyncRefreshNews.execute();
    }

    private boolean addNews() {//get news from database and send broadcast ,clear database
        Cursor cursor = null;
        boolean isWaitedDatabaseChanged = false;
        try {
            ContentResolver contentResolver = LauncherAppState.getInstance().getContext().getContentResolver();
            Uri uri = Uri.parse("content://com.ragentek.infostream.provider/newsTodayItems");
            cursor = contentResolver.query(uri, null, DataProviderInfo.CHANNEL_TODAY_NEWS + " = ?", new String[]{ALL_CHANNEL}, DataProviderInfo.ID_TODAY_NEWS + " desc");
            if (cursor != null) {
                if (mNesDataList == null) {
                    mNesDataList = new ArrayList<>();
                } else {
                    if (mNesDataList.size() > 0) {
                        mMaxIdInLauncher = 0;
                        sendBroadcastRefresh();
                        mNesDataList.clear();
                        isWaitedDatabaseChanged = true;
                        return isWaitedDatabaseChanged;
                    }
                }
                Log.d(TAG, "cursor " + cursor.getCount());
                int tempId = 0;
                while (cursor.moveToNext()) {
                    ToutiaoNewsEntity bean = new ToutiaoNewsEntity();
                    bean.setUrl(cursor.getString(cursor.getColumnIndex(DataProviderInfo.URL_TODAY_NEWS)));
                    bean.setName(cursor.getString(cursor.getColumnIndex(DataProviderInfo.TITLE_TODAY_NEWS)));
                    bean.setSource(cursor.getString(cursor.getColumnIndex(DataProviderInfo.SOURCE_TODAY_NEWS)));
                    bean.setAdId(cursor.getString(cursor.getColumnIndex(DataProviderInfo.AD_TODAY_NEWS)));
                    bean.setBigPicInfo(cursor.getString(cursor.getColumnIndex(DataProviderInfo.BIGPIC_TODAY_NEWS)));
                    bean.setDate(cursor.getString(cursor.getColumnIndex(DataProviderInfo.DATE_TODAY_NEWS)));
                    bean.setPic1Url(cursor.getString(cursor.getColumnIndex(DataProviderInfo.PIC1_TODAY_NEWS)));
                    bean.setPic2Url(cursor.getString(cursor.getColumnIndex(DataProviderInfo.PIC2_TODAY_NEWS)));
                    bean.setPic3Url(cursor.getString(cursor.getColumnIndex(DataProviderInfo.PIC3_TODAY_NEWS)));
                    bean.setPicUrl(cursor.getString(cursor.getColumnIndex(DataProviderInfo.PIC_TODAY_NEWS)));
                    bean.setChannel(cursor.getString(cursor.getColumnIndex(DataProviderInfo.CHANNEL_TODAY_NEWS)));
                    bean.setType(cursor.getString(cursor.getColumnIndex(DataProviderInfo.TYPE_TODAY_NEWS)));
                    if (!TextUtils.isEmpty(bean.getPic1Url()) && !TextUtils.isEmpty(bean.getPic2Url()) && !TextUtils.isEmpty(bean.getPic3Url())) {//only add three pic
                        mNesDataList.add(bean);
                    }
                    tempId = cursor.getInt(cursor.getColumnIndex(DataProviderInfo.ID_TODAY_NEWS));
                    if (tempId > mMaxIdInLauncher) {
                        mMaxIdInLauncher = tempId;
                    }
                }

                if (mNesDataList.size() < 3) {
                    mMaxIdInLauncher = 0;
                    sendBroadcastRefresh();
                    mNesDataList.clear();
                    isWaitedDatabaseChanged = true;
                    return isWaitedDatabaseChanged;
                }

                if (mNesDataList.size() > 10) {//clear database
                    int id = mMaxIdInLauncher - 10;
                    contentResolver.delete(uri, DataProviderInfo.CHANNEL_TODAY_NEWS + " = ? and " + DataProviderInfo.ID_TODAY_NEWS + " < ?", new String[]{ALL_CHANNEL, String.valueOf(id)});
                    Log.d(TAG, "delete old news data in database id < " + id);
                }

            } else {
                isWaitedDatabaseChanged = true;
                sendBroadcastRefresh();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            return isWaitedDatabaseChanged;
        }
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    @Override
    public int getItemViewType() {
        return ViewItemType.VIEW_ITEM_NEWS;
    }

    @Override
    public void destroy() {
        if (mNewsObserver != null) {
            ContentResolver contentResolver = LauncherAppState.getInstance().getContext().getContentResolver();
            contentResolver.unregisterContentObserver(mNewsObserver);
            mNewsObserver = null;
        }
    }

    private boolean canRefresh() {
        Cursor cursor = null;
        boolean result = false;
        try {
            ContentResolver contentResolver = LauncherAppState.getInstance().getContext().getContentResolver();
            Uri uri = Uri.parse("content://com.ragentek.infostream.provider/newsTodayItems");
            cursor = contentResolver.query(uri, null, DataProviderInfo.CHANNEL_TODAY_NEWS + " = ? and " + DataProviderInfo.ID_TODAY_NEWS + " > ?",
                    new String[]{ALL_CHANNEL, String.valueOf(mMaxIdInLauncher)}, DataProviderInfo.ID_TODAY_NEWS + " desc");
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

    protected int getNetworkType() {
        int type = TYPE_WIFI;
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info != null && info.getType() == ConnectivityManager.TYPE_MOBILE) {
            switch (info.getSubtype()) {
                case TelephonyManager.NETWORK_TYPE_LTE:
                    type = TYPE_4G;
                    break;

                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                    type = TYPE_3G;
                    break;
                default:
                    type = TYPE_2G;
                    break;
            }
        }
        return type;
    }

    @SuppressWarnings("deprecation")
    protected String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        String ip = Formatter.formatIpAddress(inetAddress.hashCode());
                        return ip;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public boolean reportEvent(ToutiaoNewsEntity entity) {
        StringBuffer urlBuffer = new StringBuffer(EVENT_REPORT_URL);
        StringBuffer sb = new StringBuffer();
        StringBuffer eventBuffer = new StringBuffer();
        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            eventBuffer.append("[{\"category\":\"open\",\"tag\":\"go_detail\",\"label\":\"click_headline\",\"datetime\":\"").append(timeFormat.format(new Date())).append("\",\"value\":\"").append(entity.docId).append("\"}]");
            urlBuffer.append("?").append(LauncherAppState.getSecureKey());
            urlBuffer.append("&access_token=").append(getToken());
            sb.append("events=").append(URLEncoder.encode(eventBuffer.toString(), "utf-8"));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        String result = sendHttpRequest(urlBuffer.toString(), sb.toString().getBytes());
        Log.d(TAG, "events:" + eventBuffer.toString());
        Log.d(TAG, "result:" + result);
        if (result == null) {
            return false;
        }

        try {
            JSONObject root = new JSONObject(result);
            if (root.getInt("ret") == 0) {
                return true;
            }
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean reportAd(ToutiaoNewsEntity entity, String type) {
        StringBuffer urlBuffer = new StringBuffer(AD_REPORT_URL);
        StringBuffer sb = new StringBuffer();
        StringBuffer eventBuffer = new StringBuffer();
        try {
            Long current = System.currentTimeMillis();
            eventBuffer.append("[{\"category\":\"open\",\"tag\":\"embeded_ad\",\"is_ad_event\":1,\"label\":\"").append(type).append("\"")
                    .append(",\"value\":").append(entity.adId).append(",\"log_extra\":").append(entity.adExtra)
                    .append(",\"nt\":").append(getNetworkType()).append(",\"client_ip\":\"").append(getLocalIpAddress()).append("\"")
                    .append(",\"client_at\":").append(Long.toString(current / 1000))
                    .append(",\"show_time\":").append(current - entity.showAt)
                    .append(",\"dx\":").append(entity.x).append(",\"dy\":").append(entity.y).append(",\"ux\":").append(entity.x).append(",\"uy\":").append(entity.y)
                    .append("}]");
            urlBuffer.append("?").append(LauncherAppState.getSecureKey());
            urlBuffer.append("&access_token=").append(getToken());
            sb.append("ua=").append(URLEncoder.encode(System.getProperty("http.agent"), "utf-8"));
            sb.append("&pdid=").append(tm.getDeviceId());
            sb.append("&device_type=").append("android_phone");
            sb.append("&events=").append(URLEncoder.encode(eventBuffer.toString(), "utf-8"));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        String result = sendHttpRequest(urlBuffer.toString(), sb.toString().getBytes());
        Log.d(TAG, "events:" + eventBuffer.toString());
        Log.d(TAG, "result:" + result);
        if (result == null) {
            return false;
        }

        try {
            JSONObject root = new JSONObject(result);
            if (root.getInt("ret") == 0) {
                return true;
            }
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return false;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        //        private final View mRootView;
        SwipeRefreshLayout mRefreshLayout;
        ListView mListView;

        TextView mMoreView;
        TextView mRefresh;
        ImageView mImageRefresh;

        public ViewHolder(View view) {
            super(view);
//            mRootView = view;
            mRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.refresh_layout);
            mListView = (ListView) view.findViewById(R.id.refresh_istView);
            mMoreView = (TextView) view.findViewById(R.id.home_pi_new_more);
            mRefresh = (TextView) view.findViewById(R.id.home_pi_new_refresh);
            mImageRefresh = (ImageView) view.findViewById(R.id.iv_refresh);
            Context context = view.getContext();
            boolean isProject = context.getSharedPreferences(LauncherAppState.NAME_CUSTOM_SHARE, Context.MODE_PRIVATE).getBoolean(LauncherAppState.KEY_IS_PROJECTION_WALLPAPER, true);
            if (isProject) {
                ((CardView) view.findViewById(R.id.custom_card)).setCardBackgroundColor(context.getColor(R.color.card_bg_project));
                view.findViewById(R.id.custom_ll_title).setBackgroundColor(context.getColor(R.color.widget_title_bg_project));
                ((TextView) view.findViewById(R.id.home_pi_new_title)).setTextColor(context.getColor(R.color.widget_title_textcolor_project));
                mRefresh.setTextColor(context.getColor(R.color.widget_title_textcolor_project));
                mListView.setDivider(new ColorDrawable(context.getColor(R.color.custom_divider_project)));
                mListView.setDividerHeight(1);
            } else {
                ((CardView) view.findViewById(R.id.custom_card)).setCardBackgroundColor(context.getColor(R.color.card_bg));
                view.findViewById(R.id.custom_ll_title).setBackgroundColor(context.getColor(R.color.widget_title_bg));
                ((TextView) view.findViewById(R.id.home_pi_new_title)).setTextColor(context.getColor(R.color.widget_title_textcolor));
                mRefresh.setTextColor(context.getColor(R.color.widget_title_textcolor));
                mListView.setDivider(new ColorDrawable(context.getColor(R.color.custom_divider)));
                mListView.setDividerHeight(1);
            }
            Log.d(TAG, "view holder create  is project=" + isProject);
        }
    }

    private class NewsObserver extends ContentObserver {

        public NewsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            if (canRefresh()) {
                refreshNews();
                Log.d(TAG, "database change, refresh");
            } else {
                Log.d(TAG, "database change,delete success");
            }
        }
    }

    private class ToutiaoNewsEntity {
        public String source;
        public long ts;
        public String adId;
        public String adExtra;
        public long showAt;
        public int x;
        public int y;
        public String pic1Url;
        public String pic2Url;
        public String pic3Url;
        public String picBigUrl;
        public String picUrl;
        public String name;
        public String url;
        public String date;
        public String docId;
        private String channel;
        private String type;//only for ad, if nonull (app, game, web)

        public ToutiaoNewsEntity() {
            super();
        }

        public ToutiaoNewsEntity(String picUrl, String name, String url, String date, String docId, String source, long ts) {
            this.pic1Url = picUrl;
            this.name = name;
            this.url = url;
            this.date = date;
            this.docId = docId;

            this.source = source;
            this.ts = ts;
            this.adId = null;
            this.adExtra = null;
            this.showAt = 0;
            this.x = 0;
            this.y = 0;
            this.pic1Url = null;
            this.pic2Url = null;
            this.pic3Url = null;
            this.picBigUrl = null;
        }

        public void setBigPicInfo(String picBigUrl) {
            this.picBigUrl = picBigUrl;
        }

        public void set3PicInfo(String pic1Url, String pic2Url, String pic3Url) {
            this.pic1Url = pic1Url;
            this.pic2Url = pic2Url;
            this.pic3Url = pic3Url;
        }

        public void set1PicInfo(String picUrl) {
            this.picUrl = picUrl;
        }

        public void setAdInfo(String adId, String adExtra) {
            this.adId = adId;
            this.adExtra = adExtra;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public long getTs() {
            return ts;
        }

        public void setTs(long ts) {
            this.ts = ts;
        }

        public String getAdId() {
            return adId;
        }

        public void setAdId(String adId) {
            this.adId = adId;
        }

        public String getAdExtra() {
            return adExtra;
        }

        public void setAdExtra(String adExtra) {
            this.adExtra = adExtra;
        }

        public long getShowAt() {
            return showAt;
        }

        public void setShowAt(long showAt) {
            this.showAt = showAt;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public String getPic1Url() {
            return pic1Url;
        }

        public void setPic1Url(String pic1Url) {
            this.pic1Url = pic1Url;
        }

        public String getPic2Url() {
            return pic2Url;
        }

        public void setPic2Url(String pic2Url) {
            this.pic2Url = pic2Url;
        }

        public String getPic3Url() {
            return pic3Url;
        }

        public void setPic3Url(String pic3Url) {
            this.pic3Url = pic3Url;
        }

        public String getPicBigUrl() {
            return picBigUrl;
        }

        public void setPicBigUrl(String picBigUrl) {
            this.picBigUrl = picBigUrl;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getPicUrl() {
            return picUrl;
        }

        public void setPicUrl(String picUrl) {
            this.picUrl = picUrl;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getDocId() {
            return docId;
        }

        public void setDocId(String docId) {
            this.docId = docId;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof ToutiaoNewsEntity) {
                ToutiaoNewsEntity temp = (ToutiaoNewsEntity) o;
                if (this.name.equals(temp.name)) {
                    return true;
                }
            }
            return false;
        }

    }

    private class ToutiaoNewsAdapter extends BaseAdapter {
        protected Context context = null;
        protected String channel = null;
        protected LinkedList<ToutiaoNewsEntity> newsList = new LinkedList<>();

        private int bigpicHeight = 0;
        private int normallHeight = 0;
        private Activity activity;
        private int mType;//0;common adapter   1:more adapter

        public ToutiaoNewsAdapter(Context context, String channel, int type) {
            this.context = context;
            this.channel = channel;
            mType = type;
            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            windowManager.getDefaultDisplay().getMetrics(metrics);
            bigpicHeight = metrics.widthPixels * 9 / 16;
            normallHeight = 170;
        }

        public LinkedList<ToutiaoNewsEntity> getData() {
            return newsList;
        }

        /**
         * @param view
         * @param url
         * @param type 0 mean big_pic , 1 mean pic, 2 mean pic 1 2 3
         */
        private void setImageView(ImageView view, String url, int type) {
            if (url != null && !url.isEmpty()) {
                if (context.getApplicationContext() == null) {
                    // for Picasso issue
                    context = new WrappedPackageContext(context);
                }
                switch (type) {
                    case 0://big_pic
                        Picasso.with(context)
                                .load(url)
                                .placeholder(R.drawable.ic_big_news_placeholder_error)
                                .error(R.drawable.ic_big_news_placeholder_error)
                                .into(view);
                        break;
                    case 1:// pic see mHolder
                        Picasso.with(context)
                                .load(url)
                                .placeholder(R.drawable.ic_new_normal_placeholder_error)
                                .error(R.drawable.ic_new_normal_placeholder_error)
                                .into(view);
                        break;
                    case 2:// pic1 pic2 pic3 see #mHolder
                        Picasso.with(context)
                                .load(url)
                                .placeholder(R.drawable.ic_news_placeholder_error)
                                .error(R.drawable.ic_news_placeholder_error)
                                .into(view);
                        break;
                }

                view.setVisibility(View.VISIBLE);
            } else {
                view.setVisibility(View.GONE);
            }
        }

        private void showImageView(ImageView view, String id) {
            if (id != null && !id.isEmpty()) {
                view.setVisibility(View.VISIBLE);
            } else {
                view.setVisibility(View.GONE);
            }
        }

        private String formatDate(long ts) {
            long current = System.currentTimeMillis();
            long diff_sec = (current - ts) / 1000;
            long diff_min = diff_sec / 60;
            long diff_hour = diff_min / 60;
            if (diff_sec < 60) {
                return activity.getResources().getString(R.string.just_now);
            } else if (diff_min < 60) {
                return diff_min + activity.getResources().getString(R.string.minutes_ago);
            } else if (diff_hour < 24) {
                return diff_hour + activity.getResources().getString(R.string.hour_ago);
            } else {
                // TODO: 2017/3/3
                return activity.getResources().getString(R.string.just_now);
            }
        }

        @Override
        public int getCount() {
            return newsList.size();
        }

        public void addItem(ToutiaoNewsEntity entity) {
            newsList.addFirst(entity);
        }

        public void addLastItem(ToutiaoNewsEntity entity) {
            newsList.addLast(entity);
        }

        public void clearItem() {
            newsList.clear();
        }

        @Override
        public Object getItem(int position) {
            return newsList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public String getChannel() {
            return channel;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ToutiaoViewHolder holder;
            final ToutiaoNewsEntity entity = (ToutiaoNewsEntity) newsList.get(position);
            if (convertView == null) {
                if (mType == 0) {
                    convertView = LayoutInflater.from(context).inflate(R.layout.toutiao_list_item, parent, false);
                } else if (mType == 1) {
                    convertView = LayoutInflater.from(context).inflate(R.layout.toutiao_more_item, parent, false);
                }

                holder = new ToutiaoViewHolder();
                holder.entity = entity;
                holder.pic = (ImageView) convertView.findViewById(R.id.pic);
                holder.pic.getLayoutParams().height = normallHeight;

                holder.title = (TextView) convertView.findViewById(R.id.title);
                holder.bigpic = (ImageView) convertView.findViewById(R.id.bigpic);
                holder.bigpic.getLayoutParams().height = bigpicHeight;
                holder.pic1 = (ImageView) convertView.findViewById(R.id.pic1);
                holder.pic1.getLayoutParams().height = normallHeight;
                holder.pic2 = (ImageView) convertView.findViewById(R.id.pic2);
                holder.pic2.getLayoutParams().height = normallHeight;
                holder.pic3 = (ImageView) convertView.findViewById(R.id.pic3);
                holder.pic3.getLayoutParams().height = normallHeight;
                holder.ad = (ImageView) convertView.findViewById(R.id.ad);
                holder.source = (TextView) convertView.findViewById(R.id.source);
                holder.date = (TextView) convertView.findViewById(R.id.date);
                convertView.setTag(holder);
                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            final ToutiaoNewsEntity entity = ((ToutiaoViewHolder) v.getTag()).entity;
                            Intent intent = new Intent("com.ragentek.infostream_fullnew");
                            intent.setClassName("com.ragentek.infostream3", "com.ragentek.infostream.FullNews");
                            intent.putExtra("url", entity.url);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                            if (activity != null) {
                                activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                            }
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    if (entity.adId == null) {
                                        reportEvent(entity);
                                    } else {
                                        reportAd(entity, "click");
                                    }
                                }
                            }).start();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } else {
                holder = (ToutiaoViewHolder) convertView.getTag();
                holder.entity = entity;
            }

            // TODO: 2017/3/7 0 mean big_pic , 1 mean pic, 2 mean pic 1 2 3
            setImageView(holder.pic, entity.picUrl, 1);
            holder.title.setText(entity.name);
            setImageView(holder.bigpic, entity.picBigUrl, 0);
            setImageView(holder.pic1, entity.pic1Url, 2);
            setImageView(holder.pic2, entity.pic2Url, 2);
            setImageView(holder.pic3, entity.pic3Url, 2);
            showImageView(holder.ad, entity.adId);
            holder.source.setText(entity.source);
            holder.date.setText(formatDate(entity.ts));

            if (entity.adId != null && entity.showAt == 0) {
                entity.showAt = System.currentTimeMillis();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        reportAd(entity, "show");
                    }
                }).start();
            }
            if (context.getSharedPreferences(LauncherAppState.NAME_CUSTOM_SHARE, Context.MODE_PRIVATE).getBoolean(LauncherAppState.KEY_IS_PROJECTION_WALLPAPER, true)) {
                holder.title.setTextColor(context.getColor(R.color.toutiao_list_item_project));
                holder.source.setTextColor(context.getColor(R.color.toutiao_list_item_ad_project));
                holder.date.setTextColor(context.getColor(R.color.toutiao_list_item_ad_project));
            } else {
                holder.title.setTextColor(context.getColor(R.color.toutiao_list_item));
                holder.source.setTextColor(context.getColor(R.color.toutiao_list_item_ad));
                holder.date.setTextColor(context.getColor(R.color.toutiao_list_item_ad));
            }
            return convertView;
        }

        public void setActivity(Activity activity) {
            this.activity = activity;
        }

        class WrappedPackageContext extends ContextWrapper {
            WrappedPackageContext(Context packageContext) {
                super(packageContext);
            }

            @Override
            public Context getApplicationContext() {
                return this;
            }
        }

        class ToutiaoViewHolder {
            ToutiaoNewsEntity entity;
            ImageView pic;
            TextView title;
            ImageView bigpic;
            ImageView pic1;
            ImageView pic2;
            ImageView pic3;
            ImageView ad;
            TextView source;
            TextView date;
        }
    }

}
