package com.example.hear;

import java.io.IOException;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class NoiseRecorder {

//private final String TAG = SoundOfTheCityConstants.TAG;
public static double REFERENCE = 0.00002;
public static double MAX_DB_GALAXY_NOTE = 5.0237; // max dB (SLP) 108dB

public double getNoiseLevel() {
    
    int bufferSize = AudioRecord.getMinBufferSize(44100,AudioFormat.CHANNEL_IN_DEFAULT,AudioFormat.ENCODING_PCM_16BIT);
    //making the buffer bigger....
    bufferSize=bufferSize*4;
    AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

    short data [] = new short[bufferSize];
    double average = 0.0;
    recorder.startRecording();
    //recording data;
    recorder.read(data, 0, bufferSize);

    recorder.stop();
    
    for (short s : data) {
        if(s>0) {
            average += Math.abs(s);
        } else {
            bufferSize--;
        }
    }
    //x=max;
    double x = average/bufferSize;
    
    recorder.release();
    
    double db = 0;
    if (x == 0){
    	IOException e = new IOException();
    	throw new RuntimeException("No Valid Sound Level Exception", e);
    }
    // calculating the pascal pressure based on the idea that the max amplitude (between 0 and 32767) is 
    // relative to the pressure
    double estimate_max_pressure = 32767.0/MAX_DB_GALAXY_NOTE;
    double pressure = x/estimate_max_pressure; //the value 51805.5336 can be derived from asuming that x=32767=0.6325 Pa and x=1 = 0.00002 Pa (the reference value)
    
    db = (20 * Math.log10(pressure/REFERENCE));
   
    if(db > 0) {
        return db;
    }
    IOException e = new IOException();
	throw new RuntimeException("No Valid Sound Level Exception", e);
}
}
