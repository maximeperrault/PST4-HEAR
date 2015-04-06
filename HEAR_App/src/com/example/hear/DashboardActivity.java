package com.example.hear;

import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Time;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.hear.R;
import com.example.hear.library.UserFunctions;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;
 
public class DashboardActivity extends Activity implements OnClickListener {
    UserFunctions userFunctions;
    Button btnLogout;

    /* constants */
    private static final int POLL_INTERVAL = 500;
    static final double REFERENCE = 0.00002;

    /* References to view elements */
    TextView statusTextView, amplitudeTextView, latitudeTextView, longitudeTextView, dateTextView;
	Button startRecording, stopRecording;
	ProgressBar bar;
	
	String currentDateTimeString;

    /* sound data source */
	MediaRecorder recorder;
	RecordAmplitude recordAmplitude;
	boolean isRecording = false;
	NoiseRecorder noise;
	double db;
	String str_db;
	
	/* Location source */
	GPSService gps;
    double latitude;
    double longitude;
    String lati;
    String longi;
    
    /* handler */
    Handler handler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         
        /**Dashboard Screen for the application **/ 

        // Check login status in database
        userFunctions = new UserFunctions();
        if(userFunctions.isUserLoggedIn(getApplicationContext())){
       
        	// user already logged in show databoard
            setContentView(R.layout.activity_dashboard);
            btnLogout = (Button) findViewById(R.id.btnLogout);
            
            // declaration for view
            statusTextView = (TextView) this.findViewById(R.id.StatusTextView);
    		statusTextView.setText("Ready");
    		
    		amplitudeTextView = (TextView) this.findViewById(R.id.AmplitudeTextView);
    		amplitudeTextView.setText("0");
    		
    		bar = (ProgressBar) findViewById(R.id.progressBar1);
    		
    		// declaration for the GPS
            gps = new GPSService(DashboardActivity.this);
    
            if(gps.canGetLocation()){
                latitude = gps.getLatitude();
                longitude = gps.getLongitude();
            }else{
                gps.showSettingsAlert();
            }
            
    		latitudeTextView = (TextView) this.findViewById(R.id.textlat);
    		latitudeTextView.setText(Double.toString(latitude));
    		
    		longitudeTextView = (TextView) this.findViewById(R.id.textlong);
    		longitudeTextView.setText(Double.toString(longitude));
    		
    		// declaration for the date
    		dateTextView = (TextView) this.findViewById(R.id.date);
    		String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
            dateTextView.setText(currentDateTimeString);
    		
    		stopRecording = (Button) this.findViewById(R.id.StopRecording);
    		startRecording = (Button) this.findViewById(R.id.StartRecording);
    		
    		startRecording.setOnClickListener(this);
    		stopRecording.setOnClickListener(this);
    		
    		stopRecording.setEnabled(false);
            
            btnLogout.setOnClickListener(new View.OnClickListener() {
                 
                public void onClick(View arg0) {
                    // TODO Auto-generated method stub
                    userFunctions.logoutUser(getApplicationContext());
                    Intent login = new Intent(getApplicationContext(), LoginActivity.class);
                    login.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(login);
                    // Closing dashboard screen
                    finish();
                }
            });
             
        }else{
            // user is not logged in show login screen
            Intent login = new Intent(getApplicationContext(), LoginActivity.class);
            login.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(login);
            // Closing dashboard screen
            finish();
        }        
    }

    @Override
	public void onClick(View v) {
		if (v == stopRecording){
			isRecording = false;
			recordAmplitude.cancel(true);
			
			/*recorder.stop();
			recorder.release();*/
			
			stopRecording.setEnabled(false);
			startRecording.setEnabled(true);
			
			statusTextView.setText("Stop sending data");
		
		} else if (v == startRecording) {
			/*recorder = new MediaRecorder();
			
			recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile("/dev/null"); 
            
            try{
            	recorder.prepare();
            } catch (IllegalStateException e) {
            	throw new RuntimeException("IllegalStateException on MediaRecorder.prepare", e);
            } catch (IOException e) {
            	throw new RuntimeException("IOException on MediaRecorder.prepare", e);
            }
            recorder.start();*/
            
            isRecording = true;
            recordAmplitude = new RecordAmplitude();
            recordAmplitude.execute(); 
            
            statusTextView.setText("Start sending data");
            
            stopRecording.setEnabled(true);
            startRecording.setEnabled(false);
		} 	
	}
	
	@Override
	public void onStart() {
		super.onStart();
		handler.postDelayed(pollTask, POLL_INTERVAL);
	}
	
	@Override
	public void onPause() {
		super.onPause();
	}
	
	private Runnable pollTask = new Runnable() {
    	@Override
	    public void run() {
    		
    		/* Get db */
    		noise = new NoiseRecorder();
    		db = noise.getNoiseLevel();
    		str_db = myRound(db, "0.00");
    		amplitudeTextView.setText(str_db + " db");
    		bar.setProgress((int)db);
	    	
	    	/* update GPS */
    		gps = new GPSService(DashboardActivity.this);
            latitude = gps.getLatitude();
            longitude = gps.getLongitude();

    		latitudeTextView.setText(Double.toString(latitude));
    		longitudeTextView.setText(Double.toString(longitude));
    		
    		/* update date */
    		currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
    		dateTextView.setText(currentDateTimeString);
    		
    		handler.postDelayed(pollTask, POLL_INTERVAL);            
	    }
	};
	
	private static String myRound(double value, String format) {
        if(format == null  ||  format.length() <= 0) { 
        	return String.valueOf(value); 
        }
        return new DecimalFormat(format).format(value);
    }
	
	private class RecordAmplitude extends AsyncTask<Void, Integer, Void>{

		@Override
		protected Void doInBackground(Void... params) {
			while(isRecording) {
				
				try {
					Thread.sleep(POLL_INTERVAL);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				String date = DateFormat.getDateInstance().format(new Date());
				String time = DateFormat.getTimeInstance().format(new Date());

	            Intent intent = getIntent();
	            String user_id = intent.getStringExtra("user_id");
	            
	            try{   
	            	MongoClientURI uri  = new MongoClientURI("mongodb://projectsysteam19:Bigdata19@ds033069.mongolab.com:33069/db_hear"); 
	                MongoClient mongoClient = new MongoClient(uri);
	        		DB database = mongoClient.getDB("db_hear");
	        		System.out.println("MongoDB Connection Successful!");
	        		DBCollection collec = database.getCollection("data");
	                BasicDBObject doc = new BasicDBObject()
	                .append("sound", db)
	                .append("latitude", latitude)
	        		.append("longitude", longitude)
	        		.append("date", date)
	        		.append("time", time)
	                .append("user_id", user_id);
	                collec.insert(doc);
	                System.out.println("Document is inserted successfully");
	        	    }catch(Exception e){
	        	     System.err.println( e.getClass().getName() + ": " + e.getMessage() );
	        	 }
	          
	            
	            
			}
			return null;
		}
		
		protected void onProgressUpdate(Integer... progress) {
			
		}
	}

}


