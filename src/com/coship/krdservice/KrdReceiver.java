package com.coship.krdservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class KrdReceiver extends BroadcastReceiver{
    private static String TAG = "KrdReceiver";
	
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive");
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.i(TAG, "Intent is ACTION_BOOT_COMPLETED");
            Intent krdServiceIn = new Intent(context, com.coship.krdservice.KrdService.class);
            context.startService(krdServiceIn);
        }
    }
}
