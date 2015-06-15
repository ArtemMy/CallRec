package com.artem.callrec;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.artem.callrec.RowItem.RowItem;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

public class FragmentDialog extends ListFragment implements OnItemClickListener {

    String[] menutitles;
    TypedArray menuIcons;
    File[] fileList;
    private static final String GENERAL_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/OneMoreCallRecorder/";
    CustomAdapter adapter;
    private List<RowItem> rowItems;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.list_fragment, null, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);


//        menutitles = getResources().getStringArray(R.array.titles);
        File f = new File(GENERAL_PATH);
        fileList = f.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".txt") && name.length() >= 24;
            }
        });
        String[] titles = new String[fileList.length];
        for(int i = 0; i < fileList.length; i++)  {
            titles[i] = fileList[i].getName().
                    substring(20, fileList[i].getName().length() - 4);
        }
        menutitles = titles;
        menuIcons = getResources().obtainTypedArray(R.array.icons);

        rowItems = new ArrayList<>();

        for (int i = 0; i < menutitles.length; i++) {
            RowItem items = new RowItem(menutitles[i], menuIcons.getResourceId(
                    i, -1));

            rowItems.add(items);
        }

        adapter = new CustomAdapter(getActivity(), rowItems);
        setListAdapter(adapter);
        getListView().setOnItemClickListener(this);

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
            Bundle bundle = new Bundle();
            bundle.putString("filename", fileList[position].getAbsolutePath());
            Fragment fragment = new PartDialogFragment();
            fragment.setArguments(bundle);
            FragmentManager fragmentManager = getFragmentManager();

            fragmentManager.beginTransaction()
                        .replace(R.id.container, fragment)
                        .commit();
    }
}
