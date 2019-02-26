package com.meeting.helper.meetinghelper.adapter;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.meeting.helper.meetinghelper.R;
import com.meeting.helper.meetinghelper.model.FileInfo;
import com.meeting.helper.meetinghelper.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;

public class RecordListAdapter extends ArrayAdapter<FileInfo> {

    private static final String TAG = "RecordListAdapter";

    public static final int LISTVIEW_CHANGED = 0;

    private Context context;
    private int resource;
    private List<FileInfo> list;
    private boolean isMultiSelected = false;
    private ArrayList<FileInfo> deleteFileList = new ArrayList<>();
    private ViewHolder viewHolder;
    private Handler handler;


    public RecordListAdapter(Context context, int resource, List<FileInfo> list, Handler handler) {
        super(context, resource, list);
        this.context = context;
        this.resource = resource;
        this.list = list;
        this.handler = handler;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final FileInfo fileInfo = getItem(position);
        View view;
        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(resource, null);
            viewHolder = new ViewHolder();
            viewHolder.tv_name = view.findViewById(R.id.file_name);
            viewHolder.tv_size = view.findViewById(R.id.file_size);
            viewHolder.tv_time = view.findViewById(R.id.file_time);
            viewHolder.cb_select = view.findViewById(R.id.item_select);
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }
        viewHolder.cb_select.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    addDeleteList(fileInfo);
                    fileInfo.setSelected(true);
                } else {
                    fileInfo.setSelected(false);
                    deleteFileList.remove(fileInfo);
                }
                sendMsg();
                Log.d(TAG, deleteFileList.size() + "");
            }
        });
        viewHolder.tv_name.setText(fileInfo.getFileName());
        viewHolder.tv_size.setText(FileUtils.getFileSize(fileInfo.getFileSize()));
        viewHolder.tv_time.setText(FileUtils.getFormatTime(fileInfo.getFileTime()));
        if (isMultiSelected) {
            viewHolder.cb_select.setVisibility(View.VISIBLE);
        } else {
            viewHolder.cb_select.setVisibility(View.INVISIBLE);
        }
        viewHolder.cb_select.setChecked(fileInfo.isSelected());
        return view;
    }

    public static class ViewHolder {
        public TextView tv_name;
        public TextView tv_size;
        public TextView tv_time;
        public CheckBox cb_select;
    }

    public void setMultiSelected(boolean b) {
        isMultiSelected = b;
    }

    public boolean getMultiSelected() {
        return isMultiSelected;
    }

    public ArrayList<FileInfo> getDeleteList() {
        return deleteFileList;
    }

    public ArrayList<String> getDeleteFileNameList() {
        ArrayList<String> names = new ArrayList<>();
        for (FileInfo info : deleteFileList) {
            names.add(info.getFilePath());
        }
        return names;
    }

    public void setDeleteList(ArrayList<FileInfo> deleteList) {
        deleteFileList = deleteList;
    }

    public List<FileInfo> getList() {
        return list;
    }

    public void setList(List<FileInfo> list) {
        this.list = list;
    }

    public void addDeleteList(FileInfo fileInfo) {
        if (deleteFileList.contains(fileInfo)) {
            return;
        }
        deleteFileList.add(fileInfo);
    }

    public void clearDeleteList() {
        for (FileInfo info : list) {
            info.setSelected(false);
        }
        deleteFileList.clear();
    }

    public void cancelSelected() {
        setMultiSelected(false);
        ((Activity) context).findViewById(R.id.title).setVisibility(View.VISIBLE);
        ((Activity) context).findViewById(R.id.top_bar).setVisibility(View.GONE);
        ((Activity) context).findViewById(R.id.bottom_bar).setVisibility(View.GONE);
        ((Activity) context).findViewById(R.id.new_record).setVisibility(View.VISIBLE);
        clearDeleteList();
        notifyDataSetChanged();
    }

    public void selectAll() {
        for (FileInfo item : list) {
            item.setSelected(true);
            addDeleteList(item);
            notifyDataSetChanged();
        }
    }

    public void cancelSelectAll() {
        clearDeleteList();
        for (FileInfo item : list) {
            item.setSelected(false);
            notifyDataSetChanged();
        }
    }

    public void reverseSelected() {
        for (FileInfo item : list) {
            if (item.isSelected()) {
                deleteFileList.remove(item);
            } else {
                addDeleteList(item);
            }
            item.setSelected(!item.isSelected());
            notifyDataSetChanged();
        }
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        sendMsg();
    }

    private void sendMsg() {
        Message msg = new Message();
        msg.what = LISTVIEW_CHANGED;
        msg.obj = deleteFileList.size();
        handler.sendMessage(msg);
    }

    public boolean onLongClick(int position) {
        FileInfo fileInfo = list.get(position);
        if (isMultiSelected) {
            return true;
        }
        isMultiSelected = true;
        deleteFileList.clear();
        fileInfo.setSelected(true);
        addDeleteList(fileInfo);
        notifyDataSetChanged();
        ((Activity) context).findViewById(R.id.title).setVisibility(View.GONE);
        ((Activity) context).findViewById(R.id.top_bar).setVisibility(View.VISIBLE);
        ((Activity) context).findViewById(R.id.bottom_bar).setVisibility(View.VISIBLE);
        ((Activity) context).findViewById(R.id.new_record).setVisibility(View.GONE);
        return true;
    }
}
