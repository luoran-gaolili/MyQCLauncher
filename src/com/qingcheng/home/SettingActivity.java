package com.qingcheng.home;

import android.app.ActionBar;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.preference.TwoStatePreference;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;

import com.qingcheng.home.R;
import com.qingcheng.home.config.QCConfig;
import com.qingcheng.home.database.QCPreference;

public class SettingActivity extends PreferenceActivity {
    private static final String TAG = "SettingActivity";
    private static final CharSequence KEY_TOGGLE_AUTOREORDER_ICONS = "autoreorder_icons_Checkbox";
    private static final CharSequence KEY_TOGGLE_CYCLESLIDE = "cycleSliding_Checkbox";
    private static final CharSequence KEY_TOGGLE_QC_READ = "orangeReading_Checkbox";
    private static final CharSequence KEY_TOGGLE_PROJECTOR = "key_projector";
    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    boolean flag = false;
    private SwitchPreference autoreorder_icons_Checkbox;
    private SwitchPreference cycleSliding_Checkbox;
    private SwitchPreference orangeReading_Checkbox;
    private SwitchPreference mSwitchProjector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);
        ActionBar actionBar = SettingActivity.this.getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        preferences = getSharedPreferences(QCPreference.PREFERENCE_NAME, Context.MODE_PRIVATE);
        editor = preferences.edit();

        initializeAllPreferences();
    }

    //sunfeng modify JLLEL-464 @20150821 start:
    private void initializeAllPreferences() {
        // TODO Auto-generated method stub
        autoreorder_icons_Checkbox = (SwitchPreference) findPreference(KEY_TOGGLE_AUTOREORDER_ICONS);
        cycleSliding_Checkbox = (SwitchPreference) findPreference(KEY_TOGGLE_CYCLESLIDE);
        orangeReading_Checkbox = (SwitchPreference) findPreference(KEY_TOGGLE_QC_READ);
        mSwitchProjector = (SwitchPreference) findPreference(KEY_TOGGLE_PROJECTOR);

        autoreorder_icons_Checkbox.setEnabled(true);
        cycleSliding_Checkbox.setEnabled(true);
        orangeReading_Checkbox.setEnabled(true);
        mSwitchProjector.setEnabled(true);

        flag = preferences.getBoolean(QCPreference.KEY_AUTOREORDER_ICONS, false);
        orangeReading_Checkbox.setChecked(flag);
        flag = preferences.getBoolean(QCPreference.KEY_CYCLE_SLIDE, false);
        cycleSliding_Checkbox.setChecked(flag);
        flag = preferences.getBoolean(QCPreference.KEY_NEWS_PAGE, false);
        orangeReading_Checkbox.setChecked(flag);
        flag = preferences.getBoolean(QCPreference.KEY_PROJECTOR, true);
        mSwitchProjector.setChecked(flag);

        if (!QCConfig.supportAutoReorder) {
            getPreferenceScreen().removePreference(autoreorder_icons_Checkbox);
        }
        if (!QCConfig.supportCycleSliding) {
            getPreferenceScreen().removePreference(cycleSliding_Checkbox);
        }
//		if (!(QCConfig.supportNewsPage &&
//				Utilities.isAppInstalled(getApplicationContext(), PiflowUntil.PIFLOW_PK_NAMGE))) {
        getPreferenceScreen().removePreference(orangeReading_Checkbox);
//		}
    }

    @Override
    @Deprecated
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (autoreorder_icons_Checkbox == preference) {
            autoreorder_icons_Checkbox = (SwitchPreference) preference;
            flag = autoreorder_icons_Checkbox.isChecked();
            editor.putBoolean(QCPreference.KEY_AUTOREORDER_ICONS, flag);
            editor.commit();
        }else if(mSwitchProjector == preference) {
            mSwitchProjector = (SwitchPreference) preference;
            flag = mSwitchProjector.isChecked();
            editor.putBoolean(QCPreference.KEY_PROJECTOR, flag);
            editor.commit();
        } else if (cycleSliding_Checkbox == preference) {
            cycleSliding_Checkbox = (SwitchPreference) preference;
            flag = cycleSliding_Checkbox.isChecked();
            editor.putBoolean(QCPreference.KEY_CYCLE_SLIDE, flag);
            editor.commit();
        } else if (orangeReading_Checkbox == preference) {
            orangeReading_Checkbox = (SwitchPreference) preference;
            flag = orangeReading_Checkbox.isChecked();
            editor.putBoolean(QCPreference.KEY_NEWS_PAGE, flag);
            editor.commit();
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void finish() {
        super.finish();
        Log.i(TAG, "finish: ");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.i(TAG, "onBackPressed: ");
        setResult(RESULT_OK);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}
