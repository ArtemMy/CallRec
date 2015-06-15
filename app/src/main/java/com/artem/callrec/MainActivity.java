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

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceFragment;
import android.support.v4.widget.DrawerLayout;

//import android.support.v7.app.ActionBar;
//import android.support.v7.app.ActionBarActivity;

//import android.app.ActionBar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;

import fr.lium.spkDiarization.lib.DiarizationException;
import fr.lium.spkDiarization.programs.MClust;
import fr.lium.spkDiarization.programs.MSeg;

import android.app.ActionBar;
import android.support.v7.app.ActionBarActivity;

public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks{

    private static final int REQUEST_CODE = 0;
    ProgressDialog mProgressDialog;
    ProgressDialog dProgressDialog;
    public static final int MFCC_DIALOG = 0;
    public static final int DRZ_DIALOG = 1;

    // File Locations
    private static final String GENERAL_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/OneMoreCallRecorder/";
    private String AUDIO_FILE = GENERAL_PATH + "recordoutput.pcm";
    private String FINAL_FILE = GENERAL_PATH + "final.txt";
    private static final String TEMP_PATH = GENERAL_PATH + "tmp/";
    private static final String CONFIG_FILE = GENERAL_PATH + "config.xml";
    private static final String MFCC_FILE = TEMP_PATH + "test.mfc";
    private static final String UEM_FILE = TEMP_PATH + "test.uem.seg";
    private static final String SEG_FILE = TEMP_PATH + "test.s.seg";
    private static final String SEGL_FILE = TEMP_PATH + "test.l.seg";

    private DevicePolicyManager mDPM;
    private ComponentName mAdminName;
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case MFCC_DIALOG:
                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setMessage("Computing MFCCs...");
                mProgressDialog.setCancelable(true);
                mProgressDialog.show();
                return mProgressDialog;

            case DRZ_DIALOG:
                dProgressDialog = new ProgressDialog(this);
                dProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                dProgressDialog.setMessage("Performing Diarization...");
                dProgressDialog.setCancelable(true);
                dProgressDialog.show();
                return dProgressDialog;

            default:
                return null;
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        Fragment fragment = null;
        FragmentManager fragmentManager = getFragmentManager();
        switch(position) {
            default:
                break;
            case 0:
                fragment = new FragmentDialog();
                break;
            case 2:
                fragment = new HelpFragment();
                break;
            case 1:
                fragment = new PrefsFragment();
                break;
            case 3:
                fragment = new AboutFragment();
                break;
//            case 1:
//                fragment = new RecordFragment();
//                break;
        }
        if(fragment != null)
            fragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
        else
            fragmentManager.beginTransaction()
                    .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                    .commit();
    }

    public void onSectionAttached(int number) {
        String[] stringArray = getResources().getStringArray(R.array.section_titles);
        if (number >= 1) {
            mTitle = stringArray[number - 1];
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate your Menu
        getMenuInflater().inflate(R.menu.main, menu);

        // Get the action view used in your toggleservice item
        final MenuItem toggleservice = menu.findItem(R.id.toggleservice);
        final Switch actionView = (Switch) toggleservice.getActionView();
        final MenuItem updater = menu.findItem(R.id.action_update);
        final Button updActionView = (Button) toggleservice.getActionView();

        actionView.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    turnOnCallRecorder();
                    Toast.makeText(getApplicationContext(), "Background recording is on", Toast.LENGTH_SHORT).show();
                }
                else {
                    turnOffCallRecorder();
                    Toast.makeText(getApplicationContext(), "Background recording is off", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_update:
                (new File(MFCC_FILE)).delete();
                (new File(UEM_FILE)).delete();
                (new File(SEG_FILE)).delete();
                Update();
                return true;
            case R.id.action_settings:
                Fragment fragment = new PrefsFragment();
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.container, fragment)
                        .commit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class makeMFCC extends AsyncTask<Void, Integer, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            showDialog(MFCC_DIALOG);
        }

        @Override
        protected Void doInBackground(Void... params) {
            MfccMaker Mfcc = new MfccMaker(CONFIG_FILE, AUDIO_FILE, MFCC_FILE, UEM_FILE);
            Mfcc.produceFeatures();
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            dismissDialog(MFCC_DIALOG);
            mProgressDialog.dismiss();
            new diarize().execute();
        }

        @Override
        protected void onProgressUpdate(Integer... value) {
            dProgressDialog.setProgress(value[0]);
        }
    }

    private class diarize extends AsyncTask<Void, Integer, Void> {
        int DONE_LINEARSEG = 50;
        int DONE_LINEARCLUST = 100;
        String[] linearSegParams = {"--trace", "--help", "--kind=FULL", "--sMethod=GLR", "--fInputMask=" + MFCC_FILE, "--fInputDesc=sphinx,1:3:2:0:0:0,13,0:0:0", "--sInputMask=" + UEM_FILE, "--sOutputMask="+ SEG_FILE, "test"};
        String[] linearClustParams = {"--trace", "--help", "--fInputMask=" + MFCC_FILE, "--fInputDesc=sphinx,1:1:0:0:0:0,13,0:0:0", "--sInputMask=" + SEG_FILE, "--sOutputMask=" + SEGL_FILE, "--cMethod=l", "--cThr=2", "test"};

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showDialog(DRZ_DIALOG);
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                MSeg.main(linearSegParams);
            } catch (DiarizationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            setProgress(DONE_LINEARSEG);


            try {
                MClust.main(linearClustParams);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            setProgress(DONE_LINEARCLUST);

            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {

            dismissDialog(DRZ_DIALOG);
            dProgressDialog.dismiss();
            new Recognizer(){
                @Override
                public void whenRecognized() {
                    Update();
                }
            }.Recognize(AUDIO_FILE, FINAL_FILE, SEGL_FILE, getApplicationContext());
//            Update();
        }

        @Override
        protected void onProgressUpdate(Integer... value) {
            dProgressDialog.setProgress(value[0]);
        }


    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

    public static class AboutFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater,
                                 ViewGroup container, Bundle savedInstanceState) {
            //Inflate the layout for this fragment
            return inflater.inflate(
                    R.layout.about, container, false);
        }
    }

    public static class HelpFragment extends Fragment {
        Button StartDemoButton;

        @Override
        public View onCreateView(LayoutInflater inflater,
                                 ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_help, container, false);
/*            StartDemoButton = (Button) view.findViewById(R.id.demo_start);
            StartDemoButton.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View buttonView) {
/*                    ShowcaseView sv;
                    ShowcaseView.ConfigOptions co;
                    co = new ShowcaseView.ConfigOptions();
                    co.hideOnClickOutside = false;
                    co.block = false;

                    ActionViewTarget target = new ActionViewTarget(getActivity(), ActionViewTarget.Type.HOME);
                    new ShowcaseView.Builder(getActivity())
                            .setTarget(target)
                            .setContentTitle("ShowcaseView")
                            .setContentText("This is highlighting the Home button")
                            .hideOnTouchOutside()
                            .build();
                }
            }); */
            //Inflate the layout for this fragment
            return view;
        }
    }

    public static class PrefsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            // Load the preferences from an XML resource

        }
    }

    @Override
    public void onBackPressed(){
        if(mNavigationDrawerFragment.isDrawerOpen()){
            mNavigationDrawerFragment.closeDrawer();
        }
        else{
            mNavigationDrawerFragment.setUp(
                    R.id.navigation_drawer,
                    (DrawerLayout) findViewById(R.id.drawer_layout));
        }
    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent e) {
        switch(keycode) {
            case KeyEvent.KEYCODE_MENU:
                if(!mNavigationDrawerFragment.isDrawerOpen()){
                    mNavigationDrawerFragment.setUp(
                            R.id.navigation_drawer,
                            (DrawerLayout) findViewById(R.id.drawer_layout));
                }
                break;
        }
        return super.onKeyDown(keycode, e);
    }//    @Override
//    public void onFragmentInteraction(String id)
//    {
//        Toast.makeText(getApplicationContext(), id, Toast.LENGTH_SHORT).show();
//    }
    public void turnOnCallRecorder()
    {
        try {
            mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            mAdminName = new ComponentName(this, DeviceAdminDemo.class);

            if (!mDPM.isAdminActive(mAdminName)) {
                Log.e("service", "if");
                Intent intent1 = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent1.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mAdminName);
                intent1.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Click on Activate button to secure your application.");
                startActivityForResult(intent1, REQUEST_CODE);
            }
            else {
                Intent intent = new Intent(MainActivity.this, TService.class);
                startService(intent);
                Log.e("service", "else");
            }

        } catch (Exception e) {
            Log.e("service", Log.getStackTraceString(e));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (REQUEST_CODE == requestCode) {
            Intent intent = new Intent(MainActivity.this, TService.class);
            Log.d("service", "startService (mainActivity)");
            startService(intent);
        }
    }

    public void turnOffCallRecorder()
    {
        try {
            Log.d("service", "stopService (mainActivity)");
            Intent intent = new Intent(MainActivity.this, TService.class);
            stopService(intent);
        } catch (Exception e) {
            Log.e("service", Log.getStackTraceString(e));
        }
    }

    void Update() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        File f = new File(GENERAL_PATH);

        File[] fileList = f.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".pcm");
            }
        });

        for (int i = 0; i < fileList.length; i++)
            if (!(new File(GENERAL_PATH
                    + fileList[i].getName().substring(0, fileList[i].getName().length() - 3)
                    + "txt")
                    .exists())) {

                AUDIO_FILE = fileList[i].getAbsolutePath();
                FINAL_FILE = GENERAL_PATH
                        + fileList[i].getName().substring(0, fileList[i].getName().length() - 3)
                        + "txt";
                new makeMFCC().execute();
                return;
            }
    }
}