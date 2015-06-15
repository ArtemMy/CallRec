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

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SaxAsyncHttpResponseHandler;

import org.apache.http.Header;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 * Created by 805268 on 08.06.2015.
 */
public class Recognizer {

    private static final String KEY = "754c9602-91a0-40cd-8495-238600287949";
    Context context;
    AsyncHttpClient client;
    // File Locations
    private static final String GENERAL_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/OneMoreCallRecorder/";
    private static final String TEMP_PATH = GENERAL_PATH + "tmp/";
    private String AUDIO_FILE = GENERAL_PATH + "recordoutput.pcm";
    private String FINAL_FILE = GENERAL_PATH + "recordoutput.txt";
    private String SEG_FILE = GENERAL_PATH + "test.s.seg";
    private int STACK_SIZE;

    Stack <Segment> segments = new Stack<>();
    DataInputStream dis;
    DataOutputStream dos;

    public void Recognize(String _audiofile, String _finalfile, String _segfile, Context _context)
    {
        context = _context;
        AUDIO_FILE = _audiofile;
        FINAL_FILE  = _finalfile;
        SEG_FILE = _segfile;

        client = new AsyncHttpClient();
        client.setMaxConnections(1);

//               client.addHeader("Content-Type", "audio/x-wav");
        client.addHeader("Content-Type", "audio/x-pcm;bit=16;rate=16000");
        client.addHeader("User-Agent", System.getProperty("http.agent"));

//        client.addHeader("Transfer-Encoding", "chunked");

        client.setTimeout(100 * 1000);
        client.setConnectTimeout(60*1000);
        client.setResponseTimeout(40*1000);

        client.setURLEncodingEnabled(false);

        File file = new File(AUDIO_FILE);
        try {
            dis = new DataInputStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            Log.e("request", Log.getStackTraceString(e));
        }

        Trim();
    }
    private void Trim ()
    {
        List<Segment> values =  new ArrayList<>();;
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(SEG_FILE)));
            String line;
            try {
                String[] _values;
                while ((line = br.readLine()) != null) {
                    _values = line.split("[ ]+");
                    values.add(new Segment(_values[7],
                            Integer.parseInt(_values[2]),
                            Integer.parseInt(_values[3])));
                }

                Collections.sort(values);
                segments.addAll(values);
                STACK_SIZE = segments.size();
                RecognizeFile();

//                byte[] fileData = new byte[values.get(0).length * 320];
//                dis.read(fileData, 0, values.get(0).length * 320);
//                for(item : values)
/*                File ofile = null;
                 byte[] fileData;
               ExecutorService executorService = Executors.newSingleThreadExecutor();
                try {
                    for(Segment item : values)
                    {
                        fileData = new byte[item.length * 320];
                        dis.read(fileData, 0, item.length * 320);
                        ofile = new File(TEMP_PATH, "_t_" + item.speaker + ".pcm");
                        ofile.createNewFile();
                        OutputStream os = new FileOutputStream(ofile);
                        BufferedOutputStream bos = new BufferedOutputStream(os);
                        bos.write(fileData);
                        bos.flush();
                        bos.close();
                        RecognizeFile(item.speaker, executorService);
                        //                    RecognizeFile(ofile.getAbsolutePath(), values.get(1).speaker);
                        //                    ofile.delete();
                    }
                } catch(IOException e)
                {
                    Log.e("request", Log.getStackTraceString(e));
                }
*/
               /*
                    byte[] fileData = new byte[item.length * 320];
                    dis.read(fileData, 0, item.length * 320);
                    RecognizeFile(fileData, item.speaker);
                }*/
            } catch (IOException e) {
                Log.e("request", Log.getStackTraceString(e));
            }
        } catch (FileNotFoundException e) {
            Log.e("request", Log.getStackTraceString(e));
        }
    }

    private void RecognizeFile()
    {
        String uuid = java.util.UUID.randomUUID().toString().replace("-", "");
        String url = "http://asr.yandex.net/asr_xml?uuid=" + uuid + "&key=" + KEY +"&topic=notes&lang=ru-RU";

        try {
            Segment item = segments.pop();
            byte[] fileData = new byte[item.length * 320];
            dis.read(fileData, 0, item.length * 320);
            File ofile = new File(TEMP_PATH, "_tmp_item" + ".pcm");
            ofile.createNewFile();
            OutputStream os = new FileOutputStream(ofile);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            bos.write(fileData);
            bos.flush();
            bos.close();
            os.close();

            RequestParams param = new RequestParams();
            param.put("", new File(TEMP_PATH, "_tmp_item" + ".pcm"));

            client.post(url, param, new SaxAsyncHttpResponseHandler<ResponseHandler>
                    (new ResponseHandler(FINAL_FILE, STACK_SIZE - segments.size())) {
                @Override
                public void onSuccess(int statusCode, Header[] headers, ResponseHandler response) {
                    Log.e("request", "Successfully responded");
                    Toast.makeText(context, "Responded, " + String.valueOf(segments.size()) + " left",
                            Toast.LENGTH_LONG).show();
                    if(segments.empty()) {
                           whenRecognized();
                    } else {
                        RecognizeFile();
                    }
                }

                @Override
                public void onFailure(int statusCode, org.apache.http.Header[] headers, ResponseHandler response) {
                    Log.e("request", "Failure");
                    Log.e("request", String.valueOf(statusCode));
                    Toast.makeText(context, "Fail",
                            Toast.LENGTH_LONG).show();
                }
            });
        } catch(IOException e)
        {
            Log.e("request", Log.getStackTraceString(e));
        }

        Log.e("request", "Successfully sent");
        Toast.makeText(context, "Sent",
                Toast.LENGTH_SHORT).show();
    }

    public void whenRecognized() {
    }

    class Segment implements Comparable<Segment> {
        String speaker;
        int start;
        int length;

        private Segment(String v, int A, int n) {
            this.speaker = v;
            this.start = A;
            this.length = n;
        }

        public int compareTo(Segment other) {
            return this.start > other.start ? 1 : -1;
        }
    }
}
