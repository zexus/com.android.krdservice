package com.coship.krdservice;

import android.view.WindowManager;
import android.view.View;
import android.view.KeyEvent;
import android.widget.LinearLayout; 
import android.widget.EditText;
import android.widget.Toast;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.HandlerThread;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RecoverySystem;
import android.os.SystemProperties;
import android.os.PowerManager;
import android.os.IBinder;
//import android.os.storage.StorageVolume;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.Instrumentation;
import android.content.BroadcastReceiver; 
import android.content.Intent; 
import android.content.IntentFilter; 
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.util.Log;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.provider.Settings.System;
import java.io.StringWriter;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;
import java.io.FileOutputStream;

import javax.xml.parsers.DocumentBuilder;  
import javax.xml.parsers.DocumentBuilderFactory;  
import javax.xml.transform.OutputKeys;  
import javax.xml.transform.Result;  
import javax.xml.transform.Source;  
import javax.xml.transform.Transformer;  
import javax.xml.transform.TransformerFactory;  
import javax.xml.transform.dom.DOMSource;  
import javax.xml.transform.stream.StreamResult;  
  
import org.w3c.dom.Document;  
import org.w3c.dom.Element;  
import org.w3c.dom.Node;  
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileReader;
import java.io.PrintWriter;
import java.lang.String;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Basic, yet fully functional and spec compliant, HTTP/1.1 file server.
 * <p>
 * Please note the purpose of this application is demonstrate the usage of HttpCore APIs.
 * It is NOT intended to demonstrate the most efficient way of building an HTTP file server.
 *
 *
 */


public class KrdService extends Service {
    private static final String TAG = "krdService";
	//private static final String PNAME = "krdservice";
	//private static final String PINDEX = "groupcount";
	private static final String FILENAME = "/krd.xml";
	private final int TWO_EVENT_TIME_INTERVAL = 4 * 1000;//2000ms
	
	class CMDSqueue {
        public int       mCMDSqueueSize;
        public int      mCMDSqueueKey;
        public int      mFags;
        public int[]     mCMDSqueueArray;
    };
	class KeyAry {
    	  public long mTime;
    	  public int mKeyCode;
    };
    
    private static Context mContext = null;

	private CMDSqueue mStartRecordCommand;
    private CMDSqueue mStopRecordCommand;
    private CMDSqueue mStartPlayCommand;
    private CMDSqueue mStopPlayCommand;
    private boolean mStartRecord = false;
	private boolean mNPauseRecord = true;
    private boolean mStartPlay = false;
	private boolean mPausePlay = false;
	private boolean mFirstPlay = true;
    private long mPrevKeyDownTime = -1;
    private long mCurKeyDownTime = -1;
	private int mWhichGroup;
	private File mXmlFile;
	private int mKey;

	private SharedPreferences mPreferences;
	private String[] mProvinces;
    private ArrayList<KeyAry> mKeyAry;
	private EditText mEditText;


    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate"); 
		mContext = this;
         
        mStartRecordCommand = new CMDSqueue();
        mStartRecordCommand.mCMDSqueueSize = 5;
        mStartRecordCommand.mCMDSqueueKey = 1;
        mStartRecordCommand.mCMDSqueueArray = new int[5];
        mStartRecordCommand.mCMDSqueueArray[0] = KeyEvent.KEYCODE_DPAD_UP;//8  //22;//right
        mStartRecordCommand.mCMDSqueueArray[1] = KeyEvent.KEYCODE_DPAD_DOWN;//6 //20;//down
        mStartRecordCommand.mCMDSqueueArray[2] = KeyEvent.KEYCODE_DPAD_UP;//2 //21;//left
        mStartRecordCommand.mCMDSqueueArray[3] = KeyEvent.KEYCODE_DPAD_DOWN;//4 //19;//up
        mStartRecordCommand.mCMDSqueueArray[4] = KeyEvent.KEYCODE_DPAD_UP;//8 //19;//up
        mStartRecordCommand.mFags = 0;
        mStopRecordCommand = new CMDSqueue();
        mStopRecordCommand.mCMDSqueueSize = 5;
        mStopRecordCommand.mCMDSqueueKey = 2;
        mStopRecordCommand.mCMDSqueueArray = new int[5];
        mStopRecordCommand.mCMDSqueueArray[0] = KeyEvent.KEYCODE_DPAD_UP;//8  //22;//right
        mStopRecordCommand.mCMDSqueueArray[1] = KeyEvent.KEYCODE_DPAD_DOWN;//6 //20;//down
        mStopRecordCommand.mCMDSqueueArray[2] = KeyEvent.KEYCODE_DPAD_UP;//2 //21;//left
        mStopRecordCommand.mCMDSqueueArray[3] = KeyEvent.KEYCODE_DPAD_DOWN;//4 //19;//up
        mStopRecordCommand.mCMDSqueueArray[4] = KeyEvent.KEYCODE_DPAD_DOWN;//2 //21;//left
        mStopRecordCommand.mFags = 0;
        mStartPlayCommand = new CMDSqueue();
        mStartPlayCommand.mCMDSqueueSize = 5;
        mStartPlayCommand.mCMDSqueueKey = 3;
        mStartPlayCommand.mCMDSqueueArray = new int[5];
        mStartPlayCommand.mCMDSqueueArray[0] = KeyEvent.KEYCODE_DPAD_UP;//8  //22;//right
        mStartPlayCommand.mCMDSqueueArray[1] = KeyEvent.KEYCODE_DPAD_DOWN;//6 //20;//down
        mStartPlayCommand.mCMDSqueueArray[2] = KeyEvent.KEYCODE_DPAD_UP;//2 //21;//left
        mStartPlayCommand.mCMDSqueueArray[3] = KeyEvent.KEYCODE_DPAD_DOWN;//4 //19;//up
        mStartPlayCommand.mCMDSqueueArray[4] = KeyEvent.KEYCODE_DPAD_LEFT;//6 //20;//down
        mStartPlayCommand.mFags = 0;
        mStopPlayCommand = new CMDSqueue();
        mStopPlayCommand.mCMDSqueueSize = 5;
        mStopPlayCommand.mCMDSqueueKey = 4;
        mStopPlayCommand.mCMDSqueueArray = new int[5];
        mStopPlayCommand.mCMDSqueueArray[0] = KeyEvent.KEYCODE_DPAD_UP;//8  //22;//right
        mStopPlayCommand.mCMDSqueueArray[1] = KeyEvent.KEYCODE_DPAD_DOWN;//6 //20;//down
        mStopPlayCommand.mCMDSqueueArray[2] = KeyEvent.KEYCODE_DPAD_UP;//2 //21;//left
        mStopPlayCommand.mCMDSqueueArray[3] = KeyEvent.KEYCODE_DPAD_DOWN;//4 //19;//up
        mStopPlayCommand.mCMDSqueueArray[4] = KeyEvent.KEYCODE_DPAD_RIGHT;//4 //19;//up
        mStopPlayCommand.mFags = 0;
        mKeyAry = new ArrayList<KeyAry>();
        
        Log.i(TAG, "register krd_boardcast");	
        IntentFilter krdActionFilter = new IntentFilter();  
        krdActionFilter.addAction("com.coship.stp.KEYEVENT");
        krdActionFilter.addDataScheme("file");
        krdActionFilter.addDataScheme("keyevent");
        mContext.getApplicationContext().registerReceiver(mKrdReceiver, krdActionFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    @Override
	public int onStartCommand(Intent intent, int flags, int startId){
    	super.onStart(intent, startId);
    	getXmlFile();
    	Log.i(TAG, "onStart = " + startId);
    	return START_REDELIVER_INTENT;
    }

	public String serialize() throws Exception {  
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();  
        DocumentBuilder builder = factory.newDocumentBuilder();  
        Document doc = builder.newDocument();
        Element rootElement = doc.createElement("keybase");
        rootElement.setAttribute("id", "0");
        doc.appendChild(rootElement);   
        TransformerFactory transFactory = TransformerFactory.newInstance();
		Transformer transformer = transFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		StringWriter writer = new StringWriter();
		Source source = new DOMSource(doc);
		Result result = new StreamResult(writer);
		transformer.transform(source, result);
		return writer.toString();  
    }  

	private void getXmlFile() {
		mXmlFile = new File(mContext.getFilesDir()+FILENAME);
		if(mXmlFile.exists())
			return;      
        try{
			mXmlFile.createNewFile();
            FileOutputStream fileos = new FileOutputStream(mXmlFile);
			byte [] bytes = serialize().getBytes();
			fileos.write(bytes);
			fileos.close();
        }catch(Exception e){  
     		e.printStackTrace();  
    	}
	}

	
	/*private void setXmlFile(){
		
		try{

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(mXmlFile);
		Element rootElement = doc.getDocumentElement(); 
		Log.d(TAG,"check "+rootElement.getAttribute("id"));
		Log.d(TAG,"check "+rootElement.getAttribute("keycode"));
		Log.d(TAG,"check "+rootElement.getAttribute("time"));
		Element keyElement = doc.createElement("keygroup"); 
		rootElement.setAttribute("id", "4");
        keyElement.setAttribute("id", "abc");
		rootElement.appendChild(keyElement);

		StringWriter writer = new StringWriter();

		Source source = new DOMSource(doc);
		Result result = new StreamResult(writer);
		TransformerFactory transFactory = TransformerFactory.newInstance();
		Transformer transformer = transFactory.newTransformer();
		transformer.transform(source, result);


			
            FileOutputStream fileos = new FileOutputStream(mXmlFile);
			byte [] bytes = writer.toString().getBytes();
			fileos.write(bytes);
			fileos.close();
        }
		catch(Exception e){  
     		e.printStackTrace();  
    	}
		
		
		
	}*/
	
    
    private void clearFags() {
       mStartRecordCommand.mFags = 0;
       mStopRecordCommand.mFags = 0;
       mStartPlayCommand.mFags = 0;
       mStopPlayCommand.mFags = 0;
    }
    
    private int checkCmd(KeyEvent event) { 
        if (mPrevKeyDownTime != -1) {
            mPrevKeyDownTime = mCurKeyDownTime;
            mCurKeyDownTime = event.getDownTime();
        } else {
            mCurKeyDownTime = event.getDownTime();
            mPrevKeyDownTime = mCurKeyDownTime;
        }
        if (mCurKeyDownTime - mPrevKeyDownTime > TWO_EVENT_TIME_INTERVAL) {
            clearFags();
        }
		
        mKey = event.getKeyCode();
        //Log.d(TAG,"check "+mKey);
        //Log.d(TAG,"check from"+mStartRecordCommand.mCMDSqueueArray[mStartRecordCommand.mFags]);
        if(mStartRecord ) {
        	if(mKey == mStopRecordCommand.mCMDSqueueArray[mStopRecordCommand.mFags]){    		
            	if((++mStopRecordCommand.mFags)==mStopRecordCommand.mCMDSqueueSize) {
            		clearFags();
            		return mStopRecordCommand.mCMDSqueueKey;
            	}
          	} else {
        	 	mStopRecordCommand.mFags = 0; 
          	}
          	return 0;
        }
        if(mStartPlay){
          	if(mKey == mStopPlayCommand.mCMDSqueueArray[mStopPlayCommand.mFags]){
            	if((++mStopPlayCommand.mFags)==mStopPlayCommand.mCMDSqueueSize) {
            		clearFags();
            		return mStopPlayCommand.mCMDSqueueKey;
            	}
         	} else {
        	 	mStopPlayCommand.mFags = 0; 
         	}
         	return 0;
        } 
        if(mKey == mStartRecordCommand.mCMDSqueueArray[mStartRecordCommand.mFags]){
            if((++mStartRecordCommand.mFags)==mStartRecordCommand.mCMDSqueueSize) {
            	clearFags();
            	return mStartRecordCommand.mCMDSqueueKey;
            }
        } else {
        	mStartRecordCommand.mFags = 0; 
        }
        if(mKey == mStartPlayCommand.mCMDSqueueArray[mStartPlayCommand.mFags]){
           	if((++mStartPlayCommand.mFags)==mStartPlayCommand.mCMDSqueueSize) {
            	clearFags();
            	return mStartPlayCommand.mCMDSqueueKey;
            }
        } else {
        	 mStartPlayCommand.mFags = 0; 
        }
       	return 0;    
    }
    
    private void recordKey(KeyEvent event) { 
    	KeyAry keytemp = new KeyAry();   
        keytemp.mTime = event.getDownTime();
        keytemp.mKeyCode = event.getKeyCode();
        mKeyAry.add(keytemp);
		Log.d(TAG, " keycount "+mKeyAry.size());
    }
    
    private void saveKey(String kname) { 
		int keycount = mKeyAry.size()-mStopRecordCommand.mCMDSqueueSize;
		Log.d(TAG, " keycount "+keycount);
		try{

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(mXmlFile);
			Element rootElement = doc.getDocumentElement(); 
            
			Element groupElement = doc.createElement("keygroup"); 
			groupElement.setAttribute("name", kname);
            for(int i = 0; i < keycount; i++) {
				Element keyElement = doc.createElement("key");
				keyElement.setAttribute("keycode", mKeyAry.get(i).mKeyCode+"");
				keyElement.setAttribute("time", mKeyAry.get(i).mTime+"");
				groupElement.appendChild(keyElement);
			}
			rootElement.appendChild(groupElement);

			StringWriter writer = new StringWriter();
			Source source = new DOMSource(doc);
			Result result = new StreamResult(writer);
			TransformerFactory transFactory = TransformerFactory.newInstance();
			Transformer transformer = transFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(source, result);
            FileOutputStream fileos = new FileOutputStream(mXmlFile);
			byte [] bytes = writer.toString().getBytes();
			fileos.write(bytes);
			fileos.close();
        }catch(Exception e){  
     		e.printStackTrace();  
    	}
		/*int precount  = mPreferences.getInt(PINDEX,0);
		int prekeycount = mKeyAry.size()-mStopRecordCommand.mCMDSqueueSize;
		String groupname = "groupname" + precount+"dp";
		String keycount = "keycount" + precount+"dp";
		String keycode  = "keycode" + precount+"dp";
		String time = "time" + precount+"dp";
		SharedPreferences.Editor editor = mPreferences.edit();
		editor.putInt(PINDEX,precount+1);
		editor.putInt(keycount,prekeycount);
		editor.putString(groupname,kname);
		Log.d(TAG,"keynumber: "+mKeyAry.size());
		Log.d(TAG,"keynumber: "+prekeycount);
		for(int i = 0;i < prekeycount;i++) {
            editor.putInt(keycode+i,mKeyAry.get(i).mKeyCode);
			editor.putLong(time+i,mKeyAry.get(i).mTime);
		}
		editor.commit();*/
		
    }
    
    class PlayThread extends Thread {
       @Override
       public void run(){
       		try{
		  		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		  		DocumentBuilder builder = factory.newDocumentBuilder();
		  		Document doc = builder.parse(mXmlFile);
		  		Element rootElement = doc.getDocumentElement(); 
          		NodeList tempitems = rootElement.getElementsByTagName("keygroup");
		  		NodeList items = ((Element)(tempitems.item(mWhichGroup))).getElementsByTagName("key");
          		long pretime,nowtime;
				int i = 0;
		  		pretime = Long.parseLong(((Element)(items.item(0))).getAttribute("time"));
                Log.d(TAG, " ignore record"+items.getLength());
				Instrumentation inst = new Instrumentation();
				/*if(mFirstPlay) {
					mFirstPlay = false;
					inst.sendCharacterSync(22);
					inst.sendCharacterSync(20);
					inst.sendCharacterSync(22);
					inst.sendCharacterSync(19);
					inst.sendCharacterSync(21);
				}*/
				while(mStartPlay) {
		  			if(mPausePlay) continue;
       	    			inst.sendCharacterSync(Integer.parseInt(((Element)(items.item(i))).getAttribute("keycode")));
       	    			Log.d(TAG, "---->KEY"+Integer.parseInt(((Element)(items.item(i))).getAttribute("keycode")));
						nowtime = Long.parseLong(((Element)(items.item(i))).getAttribute("time"));
						i++;
					try {
						if(nowtime>pretime)
			   				sleep(nowtime-pretime);
					} catch (InterruptedException e) {
			   			e.printStackTrace();
					}
					pretime = nowtime;
					if(i == items.getLength()) {
						i = 0;
						pretime = Long.parseLong(((Element)(items.item(0))).getAttribute("time"))-500;
					}
       	 		}
       	 		mStartPlay = false;
		  	}catch(Exception e){  
     			e.printStackTrace();  
    		}		
		  /*int i = 0;
		  int keynum;
		  String keycount = "keycount" + mWhichGroup +"dp";
		  String keycode  = "keycode" + mWhichGroup+"dp";
		  String time = "time" + mWhichGroup+"dp";
		  String kdown = "down" + mWhichGroup+"dp";
		  pretime = mPreferences.getLong(time+i,0);
		  keynum = mPreferences.getInt(keycount,0);
		  Instrumentation inst = new Instrumentation();//sendCharacterSync(int keyCode)  sendKeyDownUpSync(int key) 
       	  while(mStartPlay) {
		  	if(mPausePlay) continue;
       	    inst.sendCharacterSync(mPreferences.getInt(keycode+i,0));
			nowtime = mPreferences.getLong(time+i,0);
			i++;
			try {
			   sleep(nowtime-pretime);
			} catch (InterruptedException e) {
			   e.printStackTrace();
			}
			pretime = nowtime;
			if(i == keynum) {
				i = 0;
				pretime = mPreferences.getLong(time+0,0)-500;
			}
       	 }
       	 mStartPlay = false;*/
      }
    }
	private void checkStartRecord(){
        AlertDialog checkRecordDialog = new AlertDialog.Builder(mContext)
            .setTitle(mContext.getString(R.string.record))
            .setMessage(mContext.getString(R.string.recordHint))
            .setPositiveButton(mContext.getString(R.string.positive), new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    mStartRecord = true;
					mNPauseRecord = true;
					mKeyAry.clear();
                }
            })
            .setNegativeButton(mContext.getString(R.string.negative), new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, " ignore record");
            }
            })
        	.create();
		checkRecordDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		checkRecordDialog.show();
	}

	private void checkJoinKey(){
		try{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(mXmlFile);
			Element rootElement = doc.getDocumentElement(); 
            NodeList items = rootElement.getElementsByTagName("keygroup");
			if(items.getLength() == 0) {
	   	    	AlertDialog noexistDialog = new AlertDialog.Builder(mContext)
            		.setTitle(mContext.getString(R.string.join))
            		.setMessage(mContext.getString(R.string.noexist))
            		.setPositiveButton(mContext.getString(R.string.positive), new OnClickListener() {
                		public void onClick(DialogInterface dialog, int which) {
                			Log.d(TAG, "no exist record"); 
                		}
            		})
           			.create();
		    	noexistDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		    	noexistDialog.show();
		    	return;
	   		}
			mProvinces = new String[items.getLength()];
	        for(int i=0;i<items.getLength();i++) {
				mProvinces[i] = ((Element)(items.item(i))).getAttribute("name");	
	        }
	   	  		
        }catch(Exception e){  
     		e.printStackTrace();  
    	}


		
	   /*int precount  = mPreferences.getInt(PINDEX,0);
	   if(precount == 0) {
	   	    AlertDialog noexistDialog = new AlertDialog.Builder(mContext)
            .setTitle(mContext.getString(R.string.join))
            .setMessage(mContext.getString(R.string.noexist))
            .setPositiveButton(mContext.getString(R.string.positive), new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                	Log.d(TAG, "no exist record"); 
                }
            })
           .create();
		    noexistDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		    noexistDialog.show();
		    return;
	   }

	   mProvinces = new String[precount];
	   for(int i=0;i<precount;i++)
	   	  mProvinces[i] = mPreferences.getString("groupname" + i + "dp",null);*/
       AlertDialog checkJoinDialog = new AlertDialog.Builder(mContext)
            .setTitle(mContext.getString(R.string.join))
            .setItems(mProvinces, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                	  mStartPlay = true;
					  mFirstPlay = true;
					  mWhichGroup = which;
                	  new PlayThread().start();
                }
            })
        .create();
		checkJoinDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		checkJoinDialog.show();
	}

	private void checkStopRecord(){  
	   mEditText = new EditText(mContext);
       AlertDialog checkStopRecordDialog = new AlertDialog.Builder(mContext)
            .setTitle(mContext.getString(R.string.input))
            .setView(mEditText)
            .setPositiveButton(mContext.getString(R.string.positive), new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                	   mStartRecord = false;
                	   saveKey(mEditText.getText().toString());
					   mNPauseRecord = true;
                }
            })
        .setNegativeButton(mContext.getString(R.string.negative), new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, " ignore save");
				mNPauseRecord = true;
            }
        })
        .setCancelable(false)
        .create();
		checkStopRecordDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		checkStopRecordDialog.show();
	}

	private void checkStopJoinKey(){
       	AlertDialog checkStopJoinDialog = new AlertDialog.Builder(mContext)
            .setTitle(mContext.getString(R.string.joinstop))
            .setMessage(mContext.getString(R.string.joinstopHint))
            .setPositiveButton(mContext.getString(R.string.positive), new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
					
					   mPausePlay = false;
                	  mStartPlay = false;
                }
            })
        .setNegativeButton(mContext.getString(R.string.negative), new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
				
				mPausePlay = false;
                Log.d(TAG, " ignore join");
            }
        })
        .setCancelable(false)
        .create();
		checkStopJoinDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		checkStopJoinDialog.show();
	}


    private final BroadcastReceiver mKrdReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            String action = intent.getAction();      
            if(action.equals("com.coship.stp.KEYEVENT")) {
                KeyEvent event = (KeyEvent)intent.getParcelableExtra("keyevent");
                if(mStartRecord && mNPauseRecord)
                    recordKey(event);
                switch (checkCmd(event)) {
                	case 1:
                	   checkStartRecord();
                	   break;
                	case 2:
					   mNPauseRecord = false;
					   checkStopRecord();
                	   break;
                    case 3:
				  	   checkJoinKey();
                	   break;
                    case 4:
					   mPausePlay = true;
                	   checkStopJoinKey();
                	   break;
                	
                }
                //Log.d(TAG, "receive Broadcast KEYEVENT "+event);
            }
	    }
    };
}
    
