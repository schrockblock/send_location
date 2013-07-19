package com.rndapp.sendloc;

import java.util.ArrayList;
import java.util.Calendar;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
//import android.widget.EditText;
import android.widget.Toast;

public class SendLocation extends MapActivity implements LocationListener, OnClickListener{
	//EditText et;
	AutoCompleteTextView textView;
	Button b;
	Button share;
	long t0;
	private static final int SECONDS_TO_FIX=10;
	MapView map;
	MapController control;
	MyLocationOverlay myLoc;
	LocationManager mgr;
	Location currentBest;
	boolean pressed = false;
	boolean shared=false;
	boolean delivered=false;
	public static final String SMS_ADDRESS_PARAM="SMS_ADDRESS_PARAM";
	public static final String SMS_SENT_ACTION="com.tilab.msn.SMS_SENT";
	public static final String SMS_DELIVERY_MSG_PARAM="SMS_DELIVERY_MSG_PARAM";
	private boolean paused=false;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alt);
        
        map = (MapView)findViewById(R.id.mapview);
		control = map.getController();
        map.setBuiltInZoomControls(true);
        control.setZoom(3);
		
        myLoc = new MyLocationOverlay(this,map);
        myLoc.enableMyLocation();
        myLoc.runOnFirstFix(new Runnable(){
        	public void run(){
        		control.setZoom(16);
        		control.animateTo(myLoc.getMyLocation());
        	}
        });
        map.getOverlays().add(myLoc);
        
        Calendar cal = Calendar.getInstance();
		t0=cal.getTimeInMillis();
		mgr = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);
		mgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		mgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
		
		//et = (EditText)findViewById(R.id.editText1);
		textView = (AutoCompleteTextView) findViewById(R.id.auto_com);
		
		Cursor c = managedQuery(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
				new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
				ContactsContract.CommonDataKinds.Phone.NUMBER,ContactsContract.CommonDataKinds.Phone._ID},
				null,null,null);
		//boolean q = c.moveToFirst();
		ContactsAutoCompleteCursorAdapter adapter = new ContactsAutoCompleteCursorAdapter(this, c);
		textView.setAdapter(adapter);
		b = (Button)findViewById(R.id.button1);
		b.setTag("");
		b.setOnClickListener(this);
		share = (Button)findViewById(R.id.share);
		share.setOnClickListener(this);
		
		
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	
    	myLoc.enableMyLocation();
    	
    	Calendar cal = Calendar.getInstance();
		t0=cal.getTimeInMillis();
		mgr = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);
		mgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		mgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
		currentBest=null;
    }
    
    @Override
    public void onPause(){
    	super.onPause();
    	paused=true;
    	if (!pressed || shared){
    		mgr.removeUpdates(this);
    	}
    	myLoc.disableMyLocation();
    }
    
    private void sendLoc(Location loc){
    	double myla = loc.getLatitude();
    	double mylo = loc.getLongitude();
    	
    	String to = textView.getEditableText().toString();//et.getText().toString();
    	to=to.substring(to.indexOf("<")+1, to.indexOf(">"));
    	
    	String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";
		
		SmsManager smsMgr = SmsManager.getDefault();
		
		String smsText = "I am here:\nmaps.google.com/maps?q="+myla+",+"+mylo+"&z=17\n\nSent with ImHere: market.android.com/details?id=com.rndapp.sendloc";
		ArrayList<String> messages = smsMgr.divideMessage(smsText);
		Intent sentIntent = new Intent(SMS_SENT_ACTION);
        sentIntent.putExtra(SMS_ADDRESS_PARAM, to);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, sentIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0,
            new Intent(SENT), 0);
        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
            new Intent(DELIVERED), 0);
        //---when the SMS has been sent---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS sent", 
                                Toast.LENGTH_SHORT).show();
                        delivered=true;
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(getBaseContext(), "Generic failure", 
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(getBaseContext(), "No service", 
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(getBaseContext(), "Null PDU", 
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(getBaseContext(), "Radio off", 
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(SENT));
        //---when the SMS has been delivered---
        registerReceiver(new BroadcastReceiver(){
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS delivered", 
                                Toast.LENGTH_SHORT).show();
                        delivered=true;
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(getBaseContext(), "SMS not delivered", 
                                Toast.LENGTH_SHORT).show();
                        break;                        
                }
            }
        }, new IntentFilter(DELIVERED));  
        ArrayList<PendingIntent> listOfIntents = new ArrayList<PendingIntent>();
        listOfIntents.add(pi);
        listOfIntents.add(sentPI);
        listOfIntents.add(deliveredPI);
		smsMgr.sendMultipartTextMessage(to, null, messages, listOfIntents, null);
		textView.setText("");
		pressed=false;
		b.setEnabled(true);
		b.setText("Send My Location!");
		if (paused){
			mgr.removeUpdates(this);
		}
		
    }
    
	public void onClick(View v) {
		if (v.getId()==b.getId()){
			if (textView.getEditableText().toString().equals("")){
				Toast.makeText(getBaseContext(), "Please enter a contact.", 
                        Toast.LENGTH_SHORT).show();
			}else{
				((Button)v).setText("Sending...");
				((Button)v).setEnabled(false);
				pressed = true;
			}
		}else{
			shared=true;
			double myla = currentBest.getLatitude();
	    	double mylo = currentBest.getLongitude();
	    	
	    	String smsText = "I am here:\nmaps.google.com/maps?q="+myla+",+"+mylo+"&z=17\n\nSent with ImHere: market.android.com/details?id=com.rndapp.sendloc";
	    	
			Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
			shareIntent.setType("text/plain");
			shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, smsText);

			startActivity(Intent.createChooser(shareIntent, "Please choose how to share your location"));
		}
	}

	public void onLocationChanged(Location loc) {
		Calendar cal = Calendar.getInstance();
		if (pressed && (loc.getAccuracy()<15||cal.getTimeInMillis()>t0+SECONDS_TO_FIX*1000)){
			if (currentBest!=null){
				sendLoc(currentBest);
			}
		}
		
		if (isBetterLocation(loc)){
			currentBest=loc;
			TextView acc = (TextView) findViewById(R.id.accur);
			float feet = loc.getAccuracy()*(float)3.28;
			acc.setText(""+feet+" feet");
			
			//alt stuff
			TextView lat = (TextView) findViewById(R.id.lat);
			TextView lon = (TextView) findViewById(R.id.lon);
			lat.setText(""+loc.getLatitude());
			lon.setText(""+loc.getLongitude());
			int late6=(int)loc.getLatitude()*10^6;
			int lone6=(int)loc.getLongitude()*10^6;
		}
	}
	
	protected boolean isBetterLocation(Location location) {
	    if (currentBest == null) {
	        // A new location is always better than no location
	        return true;
	    }

	    // Check whether the new location fix is newer or older
	    long timeDelta = location.getTime() - currentBest.getTime();
	    boolean isNewer = timeDelta > 0;

	    // Check whether the new location fix is more or less accurate
	    int accuracyDelta = (int) (location.getAccuracy() - currentBest.getAccuracy());
	    boolean isLessAccurate = accuracyDelta > 0;
	    boolean isMoreAccurate = accuracyDelta < 0;
	    boolean isSignificantlyLessAccurate = accuracyDelta > 200;

	    // Check if the old and new location are from the same provider
	    boolean isFromSameProvider = isSameProvider(location.getProvider(),
	            currentBest.getProvider());

	    // Determine location quality using a combination of timeliness and accuracy
	    if (isMoreAccurate) {
	        return true;
	    } else if (isNewer && !isLessAccurate) {
	        return true;
	    } 
	    return false;
	}
	
	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
	    if (provider1 == null) {
	      return provider2 == null;
	    }
	    return provider1.equals(provider2);
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub
		
	}
}