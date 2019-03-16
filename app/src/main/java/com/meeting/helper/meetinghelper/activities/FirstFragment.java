package com.meeting.helper.meetinghelper.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.meeting.helper.meetinghelper.R;
import com.meeting.helper.meetinghelper.adapter.FolderListAdapter;
import com.meeting.helper.meetinghelper.ftp.FtpClient;
import com.meeting.helper.meetinghelper.ftp.FtpTaskStatus;
import com.meeting.helper.meetinghelper.ftp.FtpWorker;
import com.meeting.helper.meetinghelper.ftp.OnTaskStatusChangedListener;
import com.meeting.helper.meetinghelper.ftp.task.FtpTask;
import com.meeting.helper.meetinghelper.ftp.task.ListFilesTask;
import com.meeting.helper.meetinghelper.model.FileInfo;
import com.meeting.helper.meetinghelper.utils.FileUtils;

import java.util.ArrayList;

public class FirstFragment extends Fragment {

    private static final String TAG = "FirstFragment";

    private static final String BASE_PATH = "/storage/emulated/0/meetinghelper/records";

    private ListView folderList;
    private FolderListAdapter adapter;
    private ArrayList<String> list = new ArrayList<>();
    private boolean isLocalMode;
    private ArrayList<FileInfo> remoteFiles;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        isLocalMode = ((MainActivity) getActivity()).isLocalMode();
        View view = inflater.inflate(R.layout.fragment_layout, container, false);
        init(view);
        return view;
    }

    private void init(View view) {
        Log.d(TAG, "FirstFragment init");
        folderList = view.findViewById(R.id.lv_main_list);
        adapter = new FolderListAdapter(getActivity(), R.layout.item_folder, list);
        folderList.setAdapter(adapter);
        folderList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, list.get(position));
                ((MainActivity) getActivity()).getSecondFragment().setParentFolder(list.get(position));
                FragmentManager fm = getFragmentManager();
                FragmentTransaction tx = fm.beginTransaction();
                tx.hide(FirstFragment.this);
                tx.show(((MainActivity) getActivity()).getSecondFragment());
                tx.addToBackStack(null);
                tx.commit();
            }
        });
        refreshListView();
    }

    public void refreshListView() {
        Log.d(TAG, "refresh firstFragment");
        if (isLocalMode()) {
            ArrayList<String> temp = FileUtils.getFolder(BASE_PATH, new String[]{"其他"});
            list.clear();
            list.addAll(temp);
            adapter.notifyDataSetChanged();
        } else {
            list.clear();
            adapter.notifyDataSetChanged();
            ListFilesTask task = new ListFilesTask(FtpClient.REMOTE_BASE_PATH);
            task.setOnTaskStatusChangedListener(new OnTaskStatusChangedListener() {
                @Override
                public void onStatusChanged(FtpTask ftpTask, FtpTaskStatus status, Object object) {
                    if (object != null) {
                        remoteFiles = (ArrayList<FileInfo>) object;
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!isLocalMode()) {
                                    list.clear();
                                    list.addAll(getFolderListByFileInfoList(remoteFiles));
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        });
                    }
                }
            });
            FtpWorker.getInstance().addTask(task);
        }
    }

    private boolean isLocalMode() {
        isLocalMode = ((MainActivity) getActivity()).isLocalMode();
        return isLocalMode;
    }

    private ArrayList<String> getFolderListByFileInfoList(ArrayList<FileInfo> infos) {
        ArrayList<String> temp = new ArrayList<>();
        for (FileInfo info : infos) {
            if (!info.isFileMode()) {
                Log.d(TAG, info.getFileName());
                if (info.getFileName().matches("^[0-9]{4}年[0-9]{1,2}月$") || info.getFileName().equals("其他")) {
                    temp.add(info.getFileName());
                }
            }
        }
        return temp;
    }


}
