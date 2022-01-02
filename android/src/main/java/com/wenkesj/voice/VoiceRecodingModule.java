package com.wenkesj.voice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognitionService;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import androidx.annotation.NonNull;
import android.util.Log;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.modules.core.PermissionAwareActivity;
import com.facebook.react.modules.core.PermissionListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import javax.annotation.Nullable;

public class VoiceRecodingModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    final ReactApplicationContext reactContext;
    private SpeechRecognizer speech = null;
    private boolean isRecognizing = false;
    private String locale = null;
    private String AudiofileName = "";
    private MediaRecorder myAudioRecorder = null;
    private File myAudioFile = null;
    private static final String LOG_TAG = "VoiceRecodingModule";
    private Intent intent;
    private static final int REQUEST_CODE_SPEECH = 2002;
    private String fileName = null;


    public VoiceRecodingModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.reactContext.addActivityEventListener(this);
    }


    private String getLocale(String locale) {
        if (locale != null && !locale.equals("")) {
            return locale;
        }

        return Locale.getDefault().toString();
    }

    @SuppressLint("WrongConstant")
    private void startListening(ReadableMap opts) {
        if (speech != null) {
            speech.destroy();
            speech = null;
        }
        String str = "en_us";
        int i = 2;
        String str2 = "speak now";
        Boolean bool = true;
        Boolean bool2 = true;
        Boolean bool3 = true;

        speech = SpeechRecognizer.createSpeechRecognizer(this.reactContext);
        speech.setRecognitionListener(new VoiceRecodingModule.SpeechRecognitionListener());

        Log.d(LOG_TAG, "startListening() language: " + str + ", matches: " + i + ", prompt: " + str2 + ", showPartial: " + bool + ", showPopup: " + bool2 + ", withAudio: " + bool3);
        Intent intent2 = new Intent("android.speech.action.RECOGNIZE_SPEECH");
        this.intent = intent2;
        intent2.putExtra("android.speech.extra.LANGUAGE_MODEL", "en-US");
        this.intent.putExtra("android.speech.extra.LANGUAGE", str);
        this.intent.putExtra("android.speech.extra.MAX_RESULTS", i);
        this.intent.putExtra("calling_package", "com.inventrix.speechtest1");
        this.intent.putExtra("android.speech.extra.PARTIAL_RESULTS", bool);
        this.intent.putExtra("android.speech.extra.DICTATION_MODE", bool);
        this.intent.putExtra("android.speech.extra.GET_AUDIO_FORMAT", "audio/AMR");
        this.intent.putExtra("android.speech.extra.GET_AUDIO", true);
        this.intent.putExtra("android.speech.extras.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 90000);
        this.intent.putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, true);
        this.intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

        if (str2 != null) {
            this.intent.putExtra("android.speech.extra.PROMPT", str2);
        }
        if (bool2.booleanValue()) {
            getReactApplicationContext().startActivityForResult(this.intent, REQUEST_CODE_SPEECH,null);
            return;
        }
        if (bool3.booleanValue()) {
            Date date = new Date();
            long epochTime = date.getTime();
            StringBuilder paramString2 = new StringBuilder();
            paramString2.append("No-Prompt");
            paramString2.append(epochTime);
            paramString2.append(".mp3");

            ContextWrapper cw = new ContextWrapper(this.reactContext);
            File directory = cw.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            File file = new File(directory, paramString2.toString());

            this.myAudioFile = file;

            this.AudiofileName = file.getAbsolutePath();
            Log.d(LOG_TAG, this.AudiofileName);
            try {
                MediaRecorder mediaRecorder = new MediaRecorder();
                this.myAudioRecorder = mediaRecorder;
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                this.myAudioRecorder.setOutputFormat(1);
                this.myAudioRecorder.setAudioEncoder(3);
                this.myAudioRecorder.setOutputFile(file.getAbsolutePath());
                this.myAudioRecorder.prepare();
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.myAudioRecorder.start();
            getReactApplicationContext().startActivityForResult(this.intent, REQUEST_CODE_SPEECH,null);
        }
    }


    private void startSpeechWithPermissions(final String locale, final ReadableMap opts, final Callback callback) {
        this.locale = locale;

        Handler mainHandler = new Handler(this.reactContext.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    startListening(opts);
                    isRecognizing = true;
                    callback.invoke(false);
                } catch (Exception e) {
                    callback.invoke(e.getMessage());
                }
            }
        });
    }

    @Override
    public String getName() {
        return "VoiceRecoding";
    }

    @ReactMethod
    public void startSpeech(final String locale, final ReadableMap opts, final Callback callback) {
        if (!isPermissionGranted() && opts.getBoolean("REQUEST_PERMISSIONS_AUTO")) {
            String[] PERMISSIONS = {Manifest.permission.RECORD_AUDIO};
            if (this.getCurrentActivity() != null) {
                ((PermissionAwareActivity) this.getCurrentActivity()).requestPermissions(PERMISSIONS, 1, new PermissionListener() {
                    public boolean onRequestPermissionsResult(final int requestCode,
                                                              @NonNull final String[] permissions,
                                                              @NonNull final int[] grantResults) {
                        boolean permissionsGranted = true;
                        for (int i = 0; i < permissions.length; i++) {
                            final boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                            permissionsGranted = permissionsGranted && granted;
                        }
                        startSpeechWithPermissions(locale, opts, callback);
                        return permissionsGranted;
                    }
                });
            }
            return;
        }
        startSpeechWithPermissions(locale, opts, callback);
    }

    @ReactMethod
    public void stopSpeech(final Callback callback) {
        Handler mainHandler = new Handler(this.reactContext.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (speech != null) {
                        speech.stopListening();
                    }
                    isRecognizing = false;
                    callback.invoke(false);
                } catch(Exception e) {
                    callback.invoke(e.getMessage());
                }
            }
        });
    }

    @ReactMethod
    public void cancelSpeech(final Callback callback) {
        Handler mainHandler = new Handler(this.reactContext.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (speech != null) {
                        speech.cancel();
                    }
                    isRecognizing = false;
                    callback.invoke(false);
                } catch(Exception e) {
                    callback.invoke(e.getMessage());
                }
            }
        });
    }

    @ReactMethod
    public void destroySpeech(final Callback callback) {
        Handler mainHandler = new Handler(this.reactContext.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (speech != null) {
                        speech.destroy();
                    }
                    speech = null;
                    isRecognizing = false;
                    callback.invoke(false);
                } catch(Exception e) {
                    callback.invoke(e.getMessage());
                }
            }
        });
    }

    @ReactMethod
    public void isSpeechAvailable(final Callback callback) {
        final VoiceRecodingModule self = this;
        Handler mainHandler = new Handler(this.reactContext.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Boolean isSpeechAvailable = SpeechRecognizer.isRecognitionAvailable(self.reactContext);
                    callback.invoke(isSpeechAvailable, false);
                } catch(Exception e) {
                    callback.invoke(false, e.getMessage());
                }
            }
        });
    }

    @ReactMethod
    public void getSpeechRecognitionServices(Promise promise) {
        final List<ResolveInfo> services = this.reactContext.getPackageManager()
                .queryIntentServices(new Intent(RecognitionService.SERVICE_INTERFACE), 0);
        WritableArray serviceNames = Arguments.createArray();
        for (ResolveInfo service : services) {
            serviceNames.pushString(service.serviceInfo.packageName);
        }

        promise.resolve(serviceNames);
    }

    private boolean isPermissionGranted() {
        String permission = Manifest.permission.RECORD_AUDIO;
        int res = getReactApplicationContext().checkCallingOrSelfPermission(permission);
        return res == PackageManager.PERMISSION_GRANTED;
    }

    @ReactMethod
    public void isRecognizing(Callback callback) {
        callback.invoke(isRecognizing);
    }

    private void sendEvent(String eventName, @Nullable WritableMap params) {
        this.reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    //  @Override
    public void onResults(Bundle results) {
        WritableArray arr = Arguments.createArray();
        WritableArray confidenceArr = Arguments.createArray();

        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        float [] scores =  results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES);


        for (String result : matches) {
            arr.pushString(result);
        }
        if (scores != null){
            for (float result : scores) {
                confidenceArr.pushDouble(result);
            }
        }

        WritableMap event = Arguments.createMap();
        event.putArray("value", arr);
        event.putArray("confidence", confidenceArr);
        if(this.myAudioFile != null) {
            this.myAudioRecorder.stop();
            try {
                this.myAudioRecorder.release();
                Log.d("ASR", "myAudioRecorder releaseed");
            } catch(Exception e) {
                Log.d("ASR", "myAudioRecorder releaseed failed");
            }
            event.putString("audioFilePath", this.myAudioFile.getAbsolutePath());
            Log.d("ASR", "file is not empty and size : "+getFileSizeKiloBytes(this.myAudioFile)+" location :"+this.myAudioFile.getAbsolutePath()+" path :"+this.myAudioFile.getPath());
        } else {
            Log.d("ASR", "file is empty");
            event.putNull("audioFilePath");
        }

        sendEvent("onSpeechResults", event);
        Log.d("ASR", "onResults()");
    }

    private static String getFileSizeKiloBytes(File file) {
        return (double) file.length() / 1024 + "  kb";
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent2) {
        Log.d(LOG_TAG, "onActivityResult() requestCode: " + requestCode + ", resultCode: " + resultCode);

        try {
            ArrayList<String> stringArrayListExtra = intent2.getStringArrayListExtra("android.speech.extra.RESULTS");
            float [] scores =  intent2.getFloatArrayExtra("android.speech.extra.CONFIDENCE_SCORES");
            JSONArray jSONArray = new JSONArray((Collection) stringArrayListExtra);
            Uri data = intent2.getData();
            PrintStream printStream = System.out;
            printStream.println("nikhil audio data" + data);
            new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date());
            stringArrayListExtra.get(0);
            printStream.println("Result " + stringArrayListExtra.get(0));
            try {
                InputStream openInputStream = this.reactContext.getContentResolver().openInputStream(data);
                if (openInputStream != null) {
                    try {
                        StringBuilder paramString2 = new StringBuilder();
                        Date date = new Date();
                        long epochTime = date.getTime();
                        paramString2.append("Prompt-yes-"+stringArrayListExtra.get(0).replaceAll(" ","-")+"-");
                        paramString2.append(epochTime);

                        paramString2.append(".mp3");
                        ContextWrapper cw = new ContextWrapper(this.reactContext);

                        File directory = cw.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
                        File file = new File(directory, paramString2.toString());

                        File file2 = new File(file.getAbsolutePath());
                        if (!file2.exists()  || !file2.isDirectory()) {
                            file2.getParentFile().mkdirs();
                        }

                        this.fileName = file.getAbsolutePath();
                        Log.d("filename", file.getAbsolutePath());
                        try {
                            FileOutputStream fileOutputStream = new FileOutputStream(file);
                            byte[] bArr = new byte[4096];
                            while (true) {
                                int read = openInputStream.read(bArr);
                                if (read == -1) {
                                    break;
                                }
                                fileOutputStream.write(bArr, 0, read);
                            }
                            fileOutputStream.flush();
                            fileOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } finally {
                        try {
                            openInputStream.close();
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                    }
                } else {
                    Log.d("error on save file", "noo");
                }
            } catch (FileNotFoundException e3) {
                e3.printStackTrace();
            }
            WritableArray arr = Arguments.createArray();
            WritableArray confidenceArr = Arguments.createArray();


            for (String result : stringArrayListExtra) {
                arr.pushString(result);
            }
            if (scores != null){
                for (float result : scores) {
                    confidenceArr.pushDouble(result);
                }
            }

            WritableMap event = Arguments.createMap();
            event.putArray("value", arr);
            event.putArray("confidence", confidenceArr);
            event.putString("audioFilePath", this.fileName);

            sendEvent("onSpeechResults", event);

            MediaRecorder mediaRecorder = this.myAudioRecorder;
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                this.myAudioRecorder.release();
            }
        } catch (Exception e4) {
            e4.printStackTrace();
        }
    }

    @Override
    public void onNewIntent(Intent intent) {

    }


    private class SpeechRecognitionListener implements RecognitionListener {
        private String getErrorText(int i) {
            switch (i) {
                case 1:
                    return "Network timeout";
                case 2:
                    return "Network error";
                case 3:
                    return "Audio recording error";
                case 4:
                    return "error from server";
                case 5:
                    return "Client side error";
                case 6:
                    return "No speech input";
                case 7:
                    return "No match";
                case 8:
                    return "RecognitionService busy";
                case 9:
                    return "Insufficient permissions";
                default:
                    return "Didn't understand, please try again.";
            }
        }

        public void onBeginningOfSpeech() {
        }

        public void onBufferReceived(byte[] bArr) {
        }

        public void onEndOfSpeech() {
        }

        public void onEvent(int i, Bundle bundle) {
        }

        public void onRmsChanged(float f) {
        }

        private SpeechRecognitionListener() {
        }

        public void onError(int i) {
            String errorText = getErrorText(i);
            Log.d(VoiceRecodingModule.LOG_TAG, "Error: " + errorText);
//            MainActivity.this.callbackContext.error(errorText);
        }

        public void onPartialResults(Bundle bundle) {
            ArrayList<String> stringArrayList = bundle.getStringArrayList("results_recognition");
            Log.d(VoiceRecodingModule.LOG_TAG, "SpeechRecognitionListener partialResults: " + stringArrayList);
        }

        public void onReadyForSpeech(Bundle bundle) {
            Log.d(VoiceRecodingModule.LOG_TAG, "onReadyForSpeech");
        }

        private void shutDowMedia() {

        }

        public void onResults(Bundle bundle) {
            ArrayList<String> stringArrayList = bundle.getStringArrayList("results_recognition");
            Log.d(VoiceRecodingModule.LOG_TAG, "SpeechRecognitionListener results: " + stringArrayList);
        }
    }
}
