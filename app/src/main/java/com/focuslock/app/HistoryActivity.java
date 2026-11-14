package com.focuslock.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

public class HistoryActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        TextView tvTotalStudyTime = findViewById(R.id.tvTotalStudyTime);
        ListView listViewHistory = findViewById(R.id.listViewHistory);
        Button btnBackFromHistory = findViewById(R.id.btnBackFromHistory);

        int totalMinutes = StorageHelper.getTotalStudyMinutes(this);
        tvTotalStudyTime.setText("Total All-Time Study: " + StorageHelper.formatMinutesReadable(totalMinutes));

        List<String> historyItems = StorageHelper.getStudyHistoryList(this);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                historyItems
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                if (textView != null) {
                    textView.setTextColor(Color.WHITE);
                    textView.setTextSize(16f);
                }
                return view;
            }
        };

        listViewHistory.setAdapter(adapter);

        btnBackFromHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
