package org.tensorflow.lite.examples.transfer.api;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class LoggingService extends Service {

    IBinder mBinder = new LocalBinder();
    String startTime;
    String endTime;
    int batStart;
    int batEnd;
    String startFrwdPassTime;
    String stopFrwdPassTime;
    String startBkwdPassTime;
    String stopBkwdPassTime;

    String epochStartTime;
    String epochStopTime;

    float batchLoss;

    String startOptTime;
    String stopOptTime;
    DBHandler dbHandler;
    public LoggingService() {
    }

    public class LocalBinder extends Binder {
        public LoggingService getLoggingServiceInstance() {
            return LoggingService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(
                () -> {
                    while (true) {
                        Log.e("Service", "Service is running...");
                    }
                }
        ).start();
        return super.onStartCommand(intent, flags, startId);
    }

    public void startForwardPass(String startTime){
        this.startFrwdPassTime = startTime;
    }

    public void startBackwardPass(String startTime){
        this.stopFrwdPassTime = startTime;
        this.startBkwdPassTime = startTime;
    }

    public void startOptimizerStep(String startTime, float loss){
        this.stopBkwdPassTime = startTime;
        this.startOptTime = startTime;
        this.batchLoss = loss;
    }

    public void stopOptimizerStep(String stopTime){
        this.stopOptTime = stopTime;
        dbHandler = new DBHandler(this);
        dbHandler.addNewBatch(startFrwdPassTime, stopOptTime, 0, 0, 0, 0,
                100, startFrwdPassTime, startBkwdPassTime, startOptTime, batchLoss);
    }

    public void startBatch(String startTime){
        String t = startTime;
    }

    public void saveBatch(float cpu, float mem, float maxCpu, float maxMem,
            float battery, String startFrwdPassTime, String startBkwdPassTime, String startOptTime, String stopOptTime, float batchLoss){
        dbHandler = new DBHandler(this);
        dbHandler.addNewBatch(startFrwdPassTime, stopOptTime, cpu, mem, maxCpu, maxMem,
                battery, startFrwdPassTime, startBkwdPassTime, startOptTime, batchLoss);
    }

    public void startEpoch(String startTime){
        this.epochStartTime = startTime;
    }

    public void stopEpoch(String stopTime){
        this.epochStopTime = stopTime;
        dbHandler = new DBHandler(this);
        dbHandler.addNewEpoch(epochStartTime, epochStopTime);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void startRound(String startTime, int batStart){
        this.startTime = startTime;
        this.batStart = batStart;
    }

    public void endRound(String endTime, int batEnd){
        this.endTime = endTime;
        this.batEnd = batEnd;
    }

    public void roundEvaluated(float acc){
        dbHandler = new DBHandler(this);
        dbHandler.addNewRound(startTime, endTime, batStart, batEnd, acc);
    }
}
