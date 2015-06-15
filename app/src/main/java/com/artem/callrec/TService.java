/**
 This file is part of CallRec.

 CallRec is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 CallRec is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with CallRec.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.artem.callrec;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by 805268 on 27.04.2015.
 */
public class TService extends Service {
    private AudioRecord audioRecorder;
    private String date;
    private boolean recordstarted = false;
    private Thread recordingThread = null;

    public static final String T_SERVICE = "com.artem.callrec.TService";

    private static final String ACTION_IN = "android.intent.action.PHONE_STATE";
    private static final String ACTION_OUT = "android.intent.action.NEW_OUTGOING_CALL";
    private static final String DIR_PATH = Environment.getExternalStorageDirectory() + "/OneMoreCallRecorder";
    private CallBr br_call;


    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d("service", "destroy");

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // final String terminate =(String)
        // intent.getExtras().get("terminate");//
        // intent.getStringExtra("terminate");
        // Log.d("TAG", "service started");
        //
        // TelephonyManager telephony = (TelephonyManager)
        // getSystemService(Context.TELEPHONY_SERVICE); // TelephonyManager
        // // object
        // CustomPhoneStateListener customPhoneListener = new
        // CustomPhoneStateListener();
        // telephony.listen(customPhoneListener,
        // PhoneStateListener.LISTEN_CALL_STATE);
        // context = getApplicationContext();
        Log.d("service", "start");

        final IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_OUT);
        filter.addAction(ACTION_IN);
        this.br_call = new CallBr();
        this.registerReceiver(this.br_call, filter);

        // if(terminate != null) {
        // stopSelf();
        // }
        return START_NOT_STICKY;
    }

    public class CallBr extends PhoneCallReceiver {
        private static final int RECORDER_SAMPLERATE = 16000;
//        private static final int RECORDER_SAMPLERATE = 8000;
        private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
        private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
        int BufferElements2Rec = 512; // want to play 2048 (2K) since 2 bytes we use only 1024
        int BytesPerElement = 2; // 2 bytes in 16bit format
        File file  = null;

        public CallBr() {
            File dir = new File(DIR_PATH);
            Log.d("service", Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())?"mounted" :"not mounted");
            Log.d("service", dir.exists()?"exist" :"not exist");
            dir.mkdir();
        }

        public void onCallStarted(boolean isIncoming, String number, Date start) {
            if (isIncoming)
                Log.d("service", "IncomingCallStarted");
            else
                Log.d("service", "OutgoingCallStarted");

            int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);

            if (bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.d("service", "Buffer size bad value");
            }

            try {
                audioRecorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_DOWNLINK,
                        RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                        RECORDER_AUDIO_ENCODING, bufferSize);
                if (audioRecorder.getState() != AudioRecord.STATE_INITIALIZED)
                    throw(new Exception("Unsuccessful init of VOICE_CALL"));
            }
            catch(Exception e)
            {
                Log.e("service", Log.getStackTraceString(e));
                audioRecorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                        RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                        RECORDER_AUDIO_ENCODING, bufferSize);
            }

            audioRecorder.startRecording();
            recordstarted = true;
            Log.d("service", "startRecording");

            date = new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss").format(new Date());
            file = new File(DIR_PATH, date + "_" + number + ".pcm");

            try{
                file.createNewFile();
            }
            catch(IOException e)
            {
                Log.e("service", Log.getStackTraceString(e));
            }

            recordingThread = new Thread(new Runnable() {
                public void run() {
                    writeAudioData();
                }
            }, "AudioRecorder Thread");
            recordingThread.start();

            Log.d("service", "startWriting");
        }

        public void onCallEnded(boolean isIncoming, String number, Date start, Date end) {
            if (isIncoming)
                Log.d("service", "IncomingCallEnded");
            else
                Log.d("service", "OutgoingCallEnded");
            if (recordstarted) {
                Log.d("service", "RecStopping");
                try {
                    recordstarted = false;
                    recordingThread.join();
                }
                catch(InterruptedException e)
                {
                    Log.e("service", Log.getStackTraceString(e));
                }
                audioRecorder.stop();
                Log.d("service", "RecStopped");
                try {
                    FileInputStream is = null;
                    is = new FileInputStream(file);
                    FileOutputStream os = null;
                    File ofile = new File(DIR_PATH, date + "_" + number + ".wav");
                    ofile.createNewFile();
                    os = new FileOutputStream(ofile);
                    ToWav encoder = new ToWav();
                    encoder.setFormat(ToWav.FORMAT_PCM);
                    encoder.setNumChannels((short) audioRecorder.getChannelCount());
                    encoder.setSampleRate(audioRecorder.getSampleRate());
                    encoder.setBitsPerSample((short) 16);
                    encoder.setNumBytes((int) file.length());
                    encoder.write(os);

                    FileChannel inChannel = is.getChannel();
                    FileChannel outChannel = os.getChannel();
                    outChannel.position(outChannel.size());
                    inChannel.transferTo(0, inChannel.size(), outChannel);

                    os.close();
                    is.close();
                } catch (IOException e) {
                    Log.e("service", Log.getStackTraceString(e));
                }

                audioRecorder.release();
                Log.d("service", "RecReleased");
                audioRecorder = null;
                recordingThread = null;
                stopSelf();
            }
        }

        private byte[] short2byte(short[] sData) {
            int shortArrsize = sData.length;
            byte[] bytes = new byte[shortArrsize * 2];
            for (int i = 0; i < shortArrsize; i++) {
                bytes[i * 2] = (byte) (sData[i] & 0x00FF);
                bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
                sData[i] = 0;
            }
            return bytes;

        }

        private void writeAudioData() {
            // Write the output audio in byte

            short sData[] = new short[BufferElements2Rec];

            FileOutputStream os = null;
            try {
                os = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                Log.e("service", Log.getStackTraceString(e));
            }

            while (recordstarted) {
                // gets the voice output from microphone to byte format

                audioRecorder.read(sData, 0, BufferElements2Rec);
                System.out.println("Short writing to file" + sData.toString());
                try {
                    // // writes the data to file from buffer
                    // // stores the voice buffer
                    byte bData[] = short2byte(sData);
                    os.write(bData, 0, BufferElements2Rec * BytesPerElement);
                } catch (IOException e) {
                    Log.e("service", Log.getStackTraceString(e));
                }
            }
            try {
                os.close();
            } catch (IOException e) {
                Log.e("service", Log.getStackTraceString(e));
            }
        }
    }
}