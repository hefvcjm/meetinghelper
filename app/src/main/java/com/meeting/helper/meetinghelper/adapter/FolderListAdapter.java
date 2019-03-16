package com.meeting.helper.meetinghelper.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.meeting.helper.meetinghelper.R;

import java.util.List;

public class FolderListAdapter extends ArrayAdapter<String> {

    private static final String TAG = "FolderListAdapter";

    private Context context;
    private int resource;
    private List<String> list;
    private ViewHolder viewHolder;

    public FolderListAdapter(Context context, int resource, List<String> objects) {
        super(context, resource, objects);
        this.context = context;
        this.list = objects;
        this.resource = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(resource, null);
            viewHolder = new ViewHolder();
            viewHolder.tvFolderName = view.findViewById(R.id.tv_folder_name);
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }
        viewHolder.tvFolderName.setText(list.get(position));
        return view;
    }

    class ViewHolder {
        TextView tvFolderName;
    }
}
