package com.example.bandwidthassignment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.List;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends FragmentActivity implements OnClickListener, LocationListener {
	final int FILE_SIZE = 1024;				//kB
	final int MAX_LOCATION_SIZE = 100;
	final int MAX_READING_SIZE = 10;
	final int MAX_DATA_SIZE = 6;
	final int MAX_LATLNG_SIZE = 2;
	final int DURATION_OUTLIER = 30;
	
	TextView mTextView1;
    Intent i = new Intent();
    
	String fileName = "bandwidth.txt";
	String dirName = "Bandwidth";
	String statFileName = "stats.txt";
	String statDirName = "Bandwidth";
	File rootPath;
	File dataPath;
	File statDataPath;
	
    FileWriter fw;
    FileReader fr;
    Long date;
    Long timerStart;
    Long timerEnd;
    String dataToWrite;
    int index = -1;
    int loc = 1;
    LocationManager locationManager;
    Location location;
    String currentLocation;
    GoogleMap googleMap;
    Marker[] rawMarkers;
    Marker[] cleanMarkers;
    
    String[] readings; 	//raw readings
    String[] data;		//processed readings
    
    boolean button3Toggle = false;
    boolean button6Toggle = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Button clickMeBtn1 = (Button) findViewById(R.id.button1);	//Download
        clickMeBtn1.setOnClickListener(this);   
		Button clickMeBtn2 = (Button) findViewById(R.id.button2);	//Save
        clickMeBtn2.setOnClickListener(this);
		Button clickMeBtn3 = (Button) findViewById(R.id.button3);	//Map plot
        clickMeBtn3.setOnClickListener(this);   
		Button clickMeBtn4 = (Button) findViewById(R.id.button4);	//GPS status
        clickMeBtn4.setOnClickListener(this);   		
        Button clickMeBtn5 = (Button) findViewById(R.id.button5);	//Process
        clickMeBtn5.setOnClickListener(this);   
        Button clickMeBtn6 = (Button) findViewById(R.id.button6);	//Map plot 2
        clickMeBtn6.setOnClickListener(this);  
        
        clickMeBtn1.setVisibility(Button.INVISIBLE);
        clickMeBtn2.setVisibility(Button.INVISIBLE);
        clickMeBtn6.setVisibility(Button.INVISIBLE);
        String fileLocation = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Bandwidth/";
    	String fileName = "bandWidth.txt";
		File fileCheck = new File (fileLocation, fileName);
		if (fileCheck.exists()){
	        clickMeBtn3.setVisibility(Button.VISIBLE);
		}
		fileCheck = new File (fileLocation, statFileName);
		if (fileCheck.exists()){
	        clickMeBtn6.setVisibility(Button.VISIBLE);
		}               
        init();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void init(){		
        rootPath = new File(Environment.getExternalStorageDirectory(), dirName);
		dataPath = new File(rootPath, fileName);
		statDataPath = new File(rootPath, statFileName);
		readings = new String[MAX_READING_SIZE*MAX_LOCATION_SIZE];
		data = new String[MAX_LOCATION_SIZE];
		
        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        mTextView1 = (TextView)findViewById(R.id.textView1);
        googleMap = ((SupportMapFragment)(getSupportFragmentManager().findFragmentById(R.id.map))).getMap();
        rawMarkers = new Marker[MAX_READING_SIZE*MAX_LOCATION_SIZE];
        cleanMarkers = new Marker[MAX_READING_SIZE*MAX_LOCATION_SIZE];
        mTextView1.setText("\n");

		if (!rootPath.exists()){
			rootPath.mkdir();
		} 
		
		if (!dataPath.exists()){
			try {
				dataPath.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			loadFromText();
			int i = 0;
	        while ((i < readings.length)&&(readings[i]!=null)){
	        	i++;
			}
	        String[] tokenSplit = new String[MAX_DATA_SIZE];
	        tokenSplit = readings[i-1].split("_");
	        loc = Integer.parseInt(tokenSplit[4]);
	        index = Integer.parseInt(tokenSplit[5]);
	        mTextView1.setText("Last recorded\nreading: "+loc+"."+index);
		}
		if (!statDataPath.exists()){
			try {
				statDataPath.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()){
			case R.id.button1: myClick1();		// Download
			break;
			case R.id.button2: myClick2();		// Save
			break;
			case R.id.button3: myClick3();		// Map
			break;
			case R.id.button4: myClick4();		// GPS check
			break;
			case R.id.button5: myClick5();		// Process
			break;
			case R.id.button6: myClick6();		// Clean Map
			break;
		}
	}
		
	@SuppressLint("NewApi")
	private void myClick1() {
		Button visibilityButton1 = (Button)findViewById(R.id.button1);
		Button visibilityButton2 = (Button)findViewById(R.id.button2);
		Button visibilityButton3 = (Button)findViewById(R.id.button3);
		Button visibilityButton4 = (Button)findViewById(R.id.button4);
		Button visibilityButton5 = (Button)findViewById(R.id.button5);
		Button visibilityButton6 = (Button)findViewById(R.id.button6);

		if (!isDownloadManagerAvailable(getBaseContext())){
	        mTextView1.setText("Download Manager not supported.\n");
		} else {
	        visibilityButton1.setVisibility(Button.INVISIBLE);
	        visibilityButton2.setVisibility(Button.INVISIBLE);
	        visibilityButton3.setVisibility(Button.INVISIBLE);
	        visibilityButton4.setVisibility(Button.INVISIBLE);
	        visibilityButton5.setVisibility(Button.INVISIBLE);
	        visibilityButton6.setVisibility(Button.INVISIBLE);

			String url = "http://androidnetworktester.googlecode.com/files/1mb.txt";
			DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
			request.setDescription("1MB");
			request.setTitle("Test File");
			
			// use the android 3.2 to compile your app in order for this if to run
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			    request.allowScanningByMediaScanner();
			    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
			}
			File dlDir = new File(Environment.getExternalStorageDirectory().toString() +"/Download/TestFile.txt" );
			if (dlDir.exists()){
				dlDir.delete();
			}
			
			request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "TestFile.txt");

	        // register receiver
	        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
	        registerReceiver(mReceiver, filter);
			
	        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	        if (location == null){
	        	
	        } else {
	        	StringBuilder sb = new StringBuilder();
	        	sb.append(String.format("%012.8f", location.getLatitude())+",");
	        	sb.append(String.format("%012.8f", location.getLongitude()));
	        	currentLocation = sb.toString();
	        }
	        
			// get download service and enqueue file
			DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
			manager.enqueue(request);
	        i.setAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);

	        // timer start
	        timerStart = System.nanoTime();
	        date = System.currentTimeMillis();
	        mTextView1.setText("Download Started.\n");
	        visibilityButton1.setVisibility(Button.INVISIBLE);
	        
		}
	}
	
	private void myClick2(){
		Button visibilityButton2 = (Button)findViewById(R.id.button2);
		Button visibilityButton3 = (Button)findViewById(R.id.button3);
		Button visibilityButton1 = (Button)findViewById(R.id.button1);

		//save event to file
    	String fileLocation = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Bandwidth/";
    	String fileName = "bandwidth.txt";
        try {

			fw = new FileWriter(fileLocation + fileName, true);
			fw.append(dataToWrite);
			fw.flush();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		mTextView1.setText("Download data saved to file.\n");
        visibilityButton3.setVisibility(Button.VISIBLE);
        visibilityButton1.setVisibility(Button.VISIBLE);
        visibilityButton2.setVisibility(Button.INVISIBLE);
	}
	
	private void myClick3() {
		Button textButton3 = (Button)findViewById(R.id.button3);
		int i = 0;
		
		if (button3Toggle){
			textButton3.setText("Display raw");
			while ((i<rawMarkers.length)&&(rawMarkers[i]!= null)){
				rawMarkers[i].remove();
				i++;
			}
		} else {
			loadFromText();
			mapPlot();
			textButton3.setText("Hide raw");
		}
		button3Toggle = !button3Toggle;
	}

	private void myClick4() {
		Button visibilityButton1 = (Button)findViewById(R.id.button1);
		Button visibilityButton4 = (Button)findViewById(R.id.button4);
 
		boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		if (isGPSEnabled){
			mTextView1.setText("GPS is enabled.\n");
			//GPS ENABLED
	        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
	        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (location==null){
				mTextView1.setText("GPS location not obtained.\n Please try again.");
			} else {
				mTextView1.setText("Location detected.\n");
				visibilityButton1.setVisibility(Button.VISIBLE);
				visibilityButton4.setVisibility(Button.INVISIBLE);
			}
		} else {
			mTextView1.setText("GPS not enabled.\n");
			showGPSEnableDialog();
		}		
	}
	
	private void myClick5() {
		processData();
	}

	private void myClick6() {
		Button textButton6 = (Button)findViewById(R.id.button6);
		int i = 0;
		
		if (button6Toggle){
			textButton6.setText("Display clean");
			while ((i<cleanMarkers.length)&&(cleanMarkers[i]!= null)){
				cleanMarkers[i].remove();
				i++;
			}
		} else {
			loadFromTextStats();
			mapPlotStats();
			textButton6.setText("Hide clean");
		}
		button6Toggle = !button6Toggle;
	}
	
	private void loadFromText() {
    	String fileLocation = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Bandwidth/";
    	String fileName = "bandwidth.txt";
    	int i = 0;
    	
    	try {
			fr = new FileReader(fileLocation + fileName);
			BufferedReader br = new BufferedReader(fr);
			String s;
			try {
				while(((s = br.readLine()) != null)&&(i < MAX_LOCATION_SIZE*MAX_READING_SIZE)) {
					readings[i] = s;
					i++;
				}			
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				fr.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//0	   1       2    3    4   5
	//DATE_LAT-LON_DURA_BAND_LOC_IND	
	private void mapPlot() {
		int i = 0;
		while ((i < readings.length)&&(readings[i]!=null)){
			String[] token = new String[MAX_DATA_SIZE];
			token = readings[i].split("_");
			String[] latLngTok = new String[MAX_LATLNG_SIZE];
			latLngTok = token[1].split(",");
			
			LatLng latLng = new LatLng(Double.parseDouble(latLngTok[0]), Double.parseDouble(latLngTok[1]));
	        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
	        rawMarkers[i] = googleMap.addMarker(new MarkerOptions()
	                .position(latLng)
	                .title("Date: "+token[0])
	                .visible(true)
	                .snippet("DL Time: "+token[2]+"s Bandwidth: "+token[3]+"kb/s")
	                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
	        googleMap.getUiSettings().setCompassEnabled(true);
	        googleMap.getUiSettings().setZoomControlsEnabled(true);
	        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20));
	        i++;
		}
	}
	
	//0	   1       2    3    4   5
	//DATE_LAT-LON_DURA_BAND_LOC_IND
	private void processData(){
		int i = 0;	
		double pLat = 0;
		double pLng = 0;
		float pDTime = 0;
		float pBandwidth = 0;
		float pLoc = 0;
		int outCnt = 0;
		int locCnt = 0;
		Button visibilityButton6 = (Button)findViewById(R.id.button6);
		String fileLocation = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Bandwidth/";
    	String fileName = "stats.txt";
		File fileCheck = new File (fileLocation, fileName);
		if (fileCheck.exists()){
			fileCheck.delete();
			try {
				fileCheck.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
    	mTextView1.setText("Please wait while data is processed.\n");
    	visibilityButton6.setVisibility(Button.INVISIBLE);
		loadFromText();
		while ((i < readings.length)&&(readings[i]!=null)){
			locCnt++;

			String[] token = new String[MAX_DATA_SIZE];
			token = readings[i].split("_");
			String[] latLngTok = new String[MAX_LATLNG_SIZE];
			latLngTok = token[1].split(",");
			
			//add cumulative for current location + outlier check
			if (Float.parseFloat(token[2]) > DURATION_OUTLIER){
				outCnt++;
			} else {
				pDTime += Float.parseFloat(token[2]);
				pBandwidth += Float.parseFloat(token[3]);
			}
			
			//all readings for location added
			if (locCnt == 10){
				pLat = Double.parseDouble(latLngTok[0]);
				pLng = Double.parseDouble(latLngTok[1]);
			
				//get average factoring out outliers
				pDTime /= (MAX_READING_SIZE - outCnt);
				pBandwidth /= (MAX_READING_SIZE - outCnt);
				StringBuilder sb = new StringBuilder();
				sb.append(String.format("%02d",Integer.parseInt(token[4]))+"_"+String.format("%012.8f",pLat)+","+String.format("%012.8f",pLng)+"_"+String.format("%07.3f",pDTime)+"_"+String.format("%08.3f",pBandwidth)+"\n");

				locCnt = 0;
				outCnt = 0;
				pDTime = 0;
				pBandwidth = 0;
				
		        try {
					fw = new FileWriter(fileLocation + fileName, true);
					fw.append(sb.toString());
					fw.flush();
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}			
			}
			i++;
		}
    	mTextView1.setText("Data processed.\n");
    	visibilityButton6.setVisibility(Button.VISIBLE);
	}

	//1
	//LOC_LAT-LNG_DURA_BANDWIDTH
	private void loadFromTextStats() {
		String fileLocation = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Bandwidth/";
    	String fileName = "stats.txt";
    	int i = 0;
    	
    	try {
			fr = new FileReader(fileLocation + fileName);
			BufferedReader br = new BufferedReader(fr);
			String s;
			try {
				while(((s = br.readLine()) != null)&&(i < MAX_LOCATION_SIZE*MAX_READING_SIZE)) {
					data[i] = s;
					i++;
				}			
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				fr.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	//0   1       2    3
	//LOC_LAT-LNG_DURA_BANDWIDTH
	private void mapPlotStats(){
		int i = 0;
		while ((i < data.length)&&(data[i]!=null)){
			String[] token = new String[MAX_DATA_SIZE];
			token = data[i].split("_");
			String[] latLngTok = new String[MAX_LATLNG_SIZE];
			latLngTok = token[1].split(",");
			
			LatLng latLng = new LatLng(Double.parseDouble(latLngTok[0]), Double.parseDouble(latLngTok[1]));
	        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
	        cleanMarkers[i] = googleMap.addMarker(new MarkerOptions()
	                .position(latLng)
	                .title("Location: "+token[0]+" average.")
	                .snippet("DL Time: "+token[2]+"s Bandwidth: "+token[3]+"kb/s")
	                .visible(true)
	                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
	        googleMap.getUiSettings().setCompassEnabled(true);
	        googleMap.getUiSettings().setZoomControlsEnabled(true);
	        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20));
	        i++;
		}
		
	}
	
	private final BroadcastReceiver mReceiver = new BroadcastReceiver(){
		public void onReceive(Context context, Intent intent){
			String action = intent.getAction();
			if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)){
				Button visibilityButton2 = (Button)findViewById(R.id.button2);
				Button visibilityButton3 = (Button)findViewById(R.id.button3);
				Button visibilityButton5 = (Button)findViewById(R.id.button5);
				Button visibilityButton6 = (Button)findViewById(R.id.button6);

				mTextView1.setText("Download Complete.\n");
		        timerEnd = System.nanoTime();
		        
		        // calculate bandwidth
		        bandwidthCalculate();
		        visibilityButton2.setVisibility(Button.VISIBLE);
		        visibilityButton3.setVisibility(Button.VISIBLE);
		        visibilityButton5.setVisibility(Button.VISIBLE);
		        String fileLocation = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Bandwidth/";
		    	String fileName = "stats.txt";
				File fileCheck = new File (fileLocation, fileName);
				if (fileCheck.exists()){       
					visibilityButton6.setVisibility(Button.VISIBLE);
				}
			}
		}
		
	};
	
	//String.format("%03d", num)
	public void bandwidthCalculate(){
		// calculate time
		Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        String dateString = String.format("%02d", c.get(Calendar.DATE)) + "/" + String.format("%02d", c.get(Calendar.MONTH)) + "/" + c.get(Calendar.YEAR) +
                " " + String.format("%02d", c.get(Calendar.HOUR_OF_DAY)) + ":" + String.format("%02d", c.get(Calendar.MINUTE)) + ":" + String.format("%02d", c.get(Calendar.SECOND))
                + ":" + String.format("%03d", c.get(Calendar.MILLISECOND));

		
		String differenceString = String.format("%07.3f", (float)((float)(timerEnd - timerStart)/1000000000));
		
		// calculate bandwidth
		int size = FILE_SIZE*8; //kilobits
		String bandwidthString = String.format("%08.3f", (float)size/Float.parseFloat(differenceString));
		
		
		// format event to string
		StringBuilder sb = new StringBuilder();

		
		//0	   1       2    3    4   5
		//DATE_LAT-LON_DURA_BAND_LOC_IND
		
		index++;
		if (index==10){
			index = 0;
			loc++;
		}
		sb.append(dateString+"_"+currentLocation+"_"+differenceString+"_"+bandwidthString+"_"+String.format("%02d", loc)+"_"+index+"\n");
		mTextView1.setText("Download Complete.\nReading: "+loc+"."+index);


		dataToWrite = sb.toString();
	}
	
	public static boolean isDownloadManagerAvailable(Context context) {
	    try {
	        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
	            return false;
	        }
	        Intent intent = new Intent(Intent.ACTION_MAIN);
	        intent.addCategory(Intent.CATEGORY_LAUNCHER);
	        intent.setClassName("com.android.providers.downloads.ui", "com.android.providers.downloads.ui.DownloadList");
	        List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent,
	                PackageManager.MATCH_DEFAULT_ONLY);
	        return list.size() > 0;
	    } catch (Exception e) {
	        return false;
	    }
	}
	
	private void showGPSEnableDialog(){
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		dialogBuilder.setMessage("GPS is not enabled. Do you want to go settings menu?");
		dialogBuilder.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//go settings
				Intent GPSEnableIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				startActivity(GPSEnableIntent);
			}			  
		});
		dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {  
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//cancel
			}
		});
		AlertDialog alertDialog = dialogBuilder.create();
		alertDialog.show();		  
	}

	@Override
	public void onLocationChanged(Location arg0) {
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,this);	}

	@Override
	public void onProviderDisabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		// TODO Auto-generated method stub
		
	}
	
}
