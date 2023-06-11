//  MIT License
//
//  Copyright (c) 2023 MohitLabs
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files (the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions:
//
//  The above copyright notice and this permission notice shall be included in all
//  copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
//  SOFTWARE.

package com.mohitlabs.yakshini;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final int MICROPHONE_PERMISSION_CODE = 1048;
    SpeechRecognizer speechRecognizer;
    MediaPlayer mediaPlayer;
    private boolean isListening = true;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton listenBtn = findViewById(R.id.listenBtn);

        if (this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
            getMicrophonePermission();
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        listenBtn.setOnClickListener(view -> {
            if (isListening) {
                listenBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_baseline_mic_24));
                speechRecognizer.startListening(speechRecognizerIntent);
                isListening = false;
            } else {
                listenBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_baseline_mic_off_24));
                speechRecognizer.stopListening();
                isListening = true;
            }
            callGPTAPI("Hello!");
        });

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float v) {
            }

            @Override
            public void onBufferReceived(byte[] bytes) {
            }

            @Override
            public void onEndOfSpeech() {
            }

            @Override
            public void onError(int i) {
            }

            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                Toast.makeText(getApplicationContext(), data.get(0), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onPartialResults(Bundle bundle) {
            }

            @Override
            public void onEvent(int i, Bundle bundle) {
            }
        });
//        playBtn.setOnClickListener(view -> {
//            try {
//                mediaPlayer = new MediaPlayer();
//                mediaPlayer.setDataSource(getRecordingFilePath());
//                mediaPlayer.prepare();
//                mediaPlayer.start();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            Toast.makeText(this, "I'm Saying...", Toast.LENGTH_LONG).show();
//        });
    }

    private void getMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MICROPHONE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MICROPHONE_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
        }
    }

    public void callGPTAPI(String question) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "text-davinci-003");
            jsonBody.put("prompt", question);
            jsonBody.put("max_tokens", 4000);
            jsonBody.put("temperature", 0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/completions")
                .header("Authorization", "Bearer " + APIKEYS.GPTKEY)
                .post(body).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Toast.makeText(getApplicationContext(), "Failed to get response " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        String result = jsonArray.getJSONObject(0).getString("text");
                        Log.d("GPTAPI", result.trim());
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    Log.d("GPTAPI", "Failed to get response due to " + response.body().toString());
                }
            }
        });
    }
}