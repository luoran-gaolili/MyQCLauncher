package com.qingcheng.home;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Properties;

import android.content.Context;
import android.util.Log;

public class PiflowUntil {
	private final static String TAG="PiflowUntil";
	//public final static String PIFLOW_PK_NAMGE="com.inveno.newpiflow";
	//public final static String PIFLOW_PK_NAMGE="com.inveno.kuyue";

	//public final static String PIFLOW_PK_NAMGE="com.inveno.qingcheng";
	//com.inveno.qingcheng
	//com.inveno.kuyue
	
	public final static String PIFLOW_PK_NAMGE="com.ragentek.infostream";
	
	public final static String PIFLOW_LAYOUT="pi_flow_new";
	
	public static void saveConfig(Context context){
		try {
			Properties p =new Properties();
			p.put("launcher_pk_name", context.getPackageName());
			String file = "/sdcard/piflowConfig.dat";
			FileOutputStream s =new FileOutputStream(file,false);
			p.store(s, "");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e(TAG," svae inveno piflow information error:"+e.getMessage());
		}
	}
	
}
