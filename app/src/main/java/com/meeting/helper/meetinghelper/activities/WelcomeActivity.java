package com.meeting.helper.meetinghelper.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.meeting.helper.audio.AudioActivity;
import com.meeting.helper.meetinghelper.R;

public class WelcomeActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO = 0;

    private ImageView ivViewFile;
    private ImageView ivNewRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        findViewById(R.id.iv_reverse_list).setVisibility(View.GONE);
        ivViewFile = findViewById(R.id.iv_view_file);
        ivNewRecord = findViewById(R.id.iv_start_record);
        ivViewFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
            }
        });
        ivNewRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WelcomeActivity.this, AudioActivity.class);
                startActivityForResult(intent, REQUEST_RECORD_AUDIO);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
