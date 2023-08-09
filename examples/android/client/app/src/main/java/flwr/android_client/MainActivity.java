package flwr.android_client;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.Pair;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import  flwr.android_client.FlowerServiceGrpc.FlowerServiceStub;
import com.google.protobuf.ByteString;

import org.tensorflow.lite.examples.transfer.api.LoggingService;

import io.grpc.stub.StreamObserver;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {
    private ManagedChannel channel;
    public FlowerClient fc;
    private static final String TAG = "Flower";

    int DEVICE_ID = 1;

    boolean isConnected;
    boolean isLoaded;

    boolean mBounded;
    private static LoggingService mLoggingService;
    private ServiceConnection mConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

         mConnection = new ServiceConnection() {
             @Override
             public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                 Toast.makeText(MainActivity.this, "Service is connected", Toast.LENGTH_SHORT).show();
                 mBounded = true;
                 LoggingService.LocalBinder mLocalBinder = (LoggingService.LocalBinder)iBinder;
                 mLoggingService = mLocalBinder.getLoggingServiceInstance();
             }

             @Override
            public void onServiceDisconnected(ComponentName name) {
                Toast.makeText(MainActivity.this, "Service is disconnected", Toast.LENGTH_SHORT).show();
                mBounded = false;
                mLoggingService = null;
            }
        };

        fc = new FlowerClient(this);

    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent mIntent = new Intent(this, LoggingService.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);

        isLoaded = false;
        loadData();
        while (!isLoaded){
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        isConnected = false;
        connect();
        while (!isConnected){
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        runGrpc();
    };

    @Override
    protected void onStop() {
        super.onStop();
        if(mBounded) {
            unbindService(mConnection);
            mBounded = false;
        }
    };

    public void loadData(){

        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            try {
                fc.loadData(DEVICE_ID);
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                pw.flush();
            }
            isLoaded = true;
        });
    }

    public void connect() {
        String host = "10.0.2.2";
        String portStr = "8080";
        if (TextUtils.isEmpty(host) || TextUtils.isEmpty(portStr) || !Patterns.IP_ADDRESS.matcher(host).matches()) {
            Toast.makeText(this, "Please enter the correct IP and port of the FL server", Toast.LENGTH_LONG).show();
        }
        else {
            int port = TextUtils.isEmpty(portStr) ? 0 : Integer.parseInt(portStr);
            channel = ManagedChannelBuilder.forAddress(host, port).maxInboundMessageSize(10 * 1024 * 1024).usePlaintext().build();
            isConnected = true;
        }
    }

    public void runGrpc(){
        MainActivity activity = this;
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            try {
                (new FlowerServiceRunnable()).run(FlowerServiceGrpc.newStub(channel), activity);
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                pw.flush();
            }
        });
    }


    private static class FlowerServiceRunnable{
        protected Throwable failed;
        private StreamObserver<ClientMessage> requestObserver;

        public void run(FlowerServiceStub asyncStub, MainActivity activity) {
             join(asyncStub, activity);
        }

        private void join(FlowerServiceStub asyncStub, MainActivity activity)
                throws RuntimeException {

            final CountDownLatch finishLatch = new CountDownLatch(1);
            requestObserver = asyncStub.join(
                            new StreamObserver<ServerMessage>() {
                                @Override
                                public void onNext(ServerMessage msg) {
                                    handleMessage(msg, activity);
                                }

                                @Override
                                public void onError(Throwable t) {
                                    t.printStackTrace();
                                    failed = t;
                                    finishLatch.countDown();
                                    Log.e(TAG, t.getMessage());
                                }

                                @Override
                                public void onCompleted() {
                                    finishLatch.countDown();
                                    Log.e(TAG, "Done");
                                }
                            });
        }

        private void handleMessage(ServerMessage message, MainActivity activity) {

            try {
                ByteBuffer[] weights;
                ClientMessage c = null;

                if (message.hasGetParametersIns()) {
                    Log.e(TAG, "Handling GetParameters");

                    weights = activity.fc.getWeights();
                    c = weightsAsProto(weights);
                } else if (message.hasFitIns()) {

                    mLoggingService.startRound((String)(new SimpleDateFormat("yyyy/MM/dd_HH:mm:ss").format(Calendar.getInstance().getTime())), 100);

                    Log.e(TAG, "Handling FitIns");

                    List<ByteString> layers = message.getFitIns().getParameters().getTensorsList();

                    Scalar epoch_config = message.getFitIns().getConfigMap().getOrDefault("local_epochs", Scalar.newBuilder().setSint64(1).build());

                    assert epoch_config != null;
                    int local_epochs = (int) epoch_config.getSint64();

                    // Our model has 10 layers
                    ByteBuffer[] newWeights = new ByteBuffer[10] ;
                    for (int i = 0; i < 10; i++) {
                        newWeights[i] = ByteBuffer.wrap(layers.get(i).toByteArray());
                    }

                    Pair<ByteBuffer[], Integer> outputs = activity.fc.fit(newWeights, local_epochs, mLoggingService);
                    c = fitResAsProto(outputs.first, outputs.second);

                    String endTime = (String)(new SimpleDateFormat("yyyy/MM/dd_HH:mm:ss").format(Calendar.getInstance().getTime()));

                    mLoggingService.endRound(endTime, 100);

                } else if (message.hasEvaluateIns()) {

                    Log.e(TAG, "Handling EvaluateIns");

                    List<ByteString> layers = message.getEvaluateIns().getParameters().getTensorsList();

                    // Our model has 10 layers
                    ByteBuffer[] newWeights = new ByteBuffer[10] ;
                    for (int i = 0; i < 10; i++) {
                        newWeights[i] = ByteBuffer.wrap(layers.get(i).toByteArray());
                    }
                    Pair<Pair<Float, Float>, Integer> inference = activity.fc.evaluate(newWeights);

                    float loss = inference.first.first;
                    float accuracy = inference.first.second;
                    mLoggingService.roundEvaluated(accuracy);
                    Log.d("DELO", "accuracy: " + accuracy);
                    int test_size = inference.second;
                    c = evaluateResAsProto(loss, test_size);
                }
                requestObserver.onNext(c);
            }
            catch (Exception e){
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private static ClientMessage weightsAsProto(ByteBuffer[] weights){
        List<ByteString> layers = new ArrayList<>();
        for (ByteBuffer weight : weights) {
            layers.add(ByteString.copyFrom(weight));
        }
        Parameters p = Parameters.newBuilder().addAllTensors(layers).setTensorType("ND").build();
        ClientMessage.GetParametersRes res = ClientMessage.GetParametersRes.newBuilder().setParameters(p).build();
        return ClientMessage.newBuilder().setGetParametersRes(res).build();
    }

    private static ClientMessage fitResAsProto(ByteBuffer[] weights, int training_size){
        List<ByteString> layers = new ArrayList<>();
        for (ByteBuffer weight : weights) {
            layers.add(ByteString.copyFrom(weight));
        }
        Parameters p = Parameters.newBuilder().addAllTensors(layers).setTensorType("ND").build();
        ClientMessage.FitRes res = ClientMessage.FitRes.newBuilder().setParameters(p).setNumExamples(training_size).build();
        return ClientMessage.newBuilder().setFitRes(res).build();
    }

    private static ClientMessage evaluateResAsProto(float accuracy, int testing_size){
        ClientMessage.EvaluateRes res = ClientMessage.EvaluateRes.newBuilder().setLoss(accuracy).setNumExamples(testing_size).build();
        return ClientMessage.newBuilder().setEvaluateRes(res).build();
    }
}
