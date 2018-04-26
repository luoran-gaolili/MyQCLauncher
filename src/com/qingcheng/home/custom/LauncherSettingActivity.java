package com.qingcheng.home.custom;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toolbar;

import com.qingcheng.home.R;

import java.util.ArrayList;
import java.util.List;

import static com.qingcheng.home.custom.ViewItemType.BINDER_SP_NAME;
import static com.qingcheng.home.custom.ViewItemType.SP_NAMES;
import static com.qingcheng.home.custom.ViewItemType.SWITCH_COUNT;

public class LauncherSettingActivity extends Activity implements AdapterView.OnItemClickListener{
    private LayoutInflater mInflater;
    private SharedPreferences sharedPreferences;
    // layout inflater object used to inflate views
    protected ListView mListView;
    private BinderSwitchAdapter mApdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.binder_list);
        mInflater=LayoutInflater.from(this);

        mListView=(ListView)findViewById(android.R.id.list);
        mListView.setSaveEnabled(true);
        mListView.setItemsCanFocus(true);
        mListView.setTextFilterEnabled(true);
        mListView.setOnItemClickListener(this);
        mApdater = new BinderSwitchAdapter(this);
        mListView.setAdapter(mApdater);



        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView mTitle = (TextView) toolbar.findViewById(R.id.toolbar_title);
        mTitle.setText(R.string.launcher_binder_setting);
        setActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        sharedPreferences=getSharedPreferences(BINDER_SP_NAME, Context.MODE_MULTI_PROCESS);

        /*getFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new PrefsFragement())
                .commit();*/


    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ListView l = (ListView) parent;
        BinderSwitchAdapter adapter = (BinderSwitchAdapter) l.getAdapter();
        boolean status=adapter.getItem(position);

        Switch statusBox = (Switch) view.findViewById(R.id.status);
        statusBox.setChecked(!status);
        SharedPreferences.Editor editor=sharedPreferences.edit();
        editor.putBoolean(SP_NAMES[position],!status);
        editor.commit();
    }
/*

    public class PrefsFragement extends PreferenceFragment implements AdapterView.OnItemClickListener {
        private static final String TAG = "PrefsFragement";
        private Context mContext;
        // layout inflater object used to inflate views
        protected ListView mListView;
        private View mRootView;
        private BinderSwitchAdapter mApdater;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            // TODO Auto-generated method stub
            super.onCreate(savedInstanceState);
            mContext = getActivity().getApplicationContext();
            //addPreferencesFromResource(R.xml.launcher_prefs);

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            mInflater=inflater;
            mRootView=inflater.inflate(R.layout.binder_list,container,false);
            mListView=(ListView)mRootView.findViewById(android.R.id.list);
            mListView.setSaveEnabled(true);
            mListView.setItemsCanFocus(true);
            mListView.setTextFilterEnabled(true);
            mListView.setOnItemClickListener(this);
            mApdater = new BinderSwitchAdapter(mContext);
            mListView.setAdapter(mApdater);
            return mRootView;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            ListView l = (ListView) parent;
            BinderSwitchAdapter adapter = (BinderSwitchAdapter) l.getAdapter();
            boolean status=adapter.getItem(position);

            Switch statusBox = (Switch) view.findViewById(R.id.status);
            statusBox.setChecked(!status);
            SharedPreferences.Editor editor=sharedPreferences.edit();
            editor.putBoolean(SP_NAMES[position],!status);
            editor.commit();
        }
    }
*/

    public class BinderSwitchAdapter extends BaseAdapter {
        private static final String TAG="BinderSwitchAdapter";
        private Context mContext;
        //private List<Boolean> mCheckRecordList=new ArrayList<>();
        private List<Integer> mIconName=new ArrayList<>();
        private List<Integer> mBinderName=new ArrayList<>();

        public BinderSwitchAdapter(Context context) {
            mContext=context;
            mIconName.add(0,R.drawable.ic_pi_apps);
            mIconName.add(1,R.drawable.ic_pi_intelcards);
            mIconName.add(2,R.drawable.ic_pi_news);
            mIconName.add(3,R.drawable.ic_pi_reader);

            mBinderName.add(0,R.string.apps_title);
            mBinderName.add(1,R.string.widget_intelcards_title);
            mBinderName.add(2,R.string.home_pi_news_title);
            mBinderName.add(3,R.string.widget_reader_title);
        }


        @Override
        public int getCount() {

            return SWITCH_COUNT;
        }

        @Override
        public Boolean getItem(int position) {
            return sharedPreferences.getBoolean(SP_NAMES[position],true);
        }


        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = mInflater.inflate(R.layout.binder_switch_item, parent, false);
            ImageView binderIcon = (ImageView) v.findViewById(R.id.binder_icon);
            TextView binderName = (TextView) v.findViewById(R.id.binder_name);
            Switch mSwitch = (Switch ) v.findViewById(R.id.status);

            binderIcon.setImageResource(mIconName.get(position));
            binderName.setText(mContext.getString(mBinderName.get(position)));

            boolean value=sharedPreferences.getBoolean(SP_NAMES[position],true);
            mSwitch.setChecked(value);
            mSwitch.setEnabled(true);
            return v;
        }
    }
}
