package com.example.workdiary;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.*;

import java.util.*;

public class WorkDoneActivity extends AppCompatActivity {
    private List<WorkdoneRow> rowList = new ArrayList<>();
    private WorkdoneAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work_done);

        RecyclerView rv = findViewById(R.id.rv_workdone);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WorkdoneAdapter(rowList, pos -> {
            rowList.remove(pos);
            adapter.notifyItemRemoved(pos);
        });
        rv.setAdapter(adapter);

        findViewById(R.id.btn_add_row).setOnClickListener(v -> {
            rowList.add(new WorkdoneRow());
            adapter.notifyItemInserted(rowList.size() - 1);
        });

        // Optionally, prepopulate a row
        rowList.add(new WorkdoneRow());
        adapter.notifyItemInserted(0);

        // Handle Edit/Save as needed
        findViewById(R.id.btn_save).setOnClickListener(v -> {
            // Save logic here
            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.btn_edit).setOnClickListener(v -> {
            // Edit logic here
            Toast.makeText(this, "Edit mode!", Toast.LENGTH_SHORT).show();
        });
    }
}
