package com.example.workdiary;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.*;

public class WorkDoneActivity extends AppCompatActivity {
    private List<WorkdoneRow> rowList = new ArrayList<>();
    private WorkdoneAdapter adapter;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work_done);

        RecyclerView rv = findViewById(R.id.rv_workdone);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WorkdoneAdapter(rowList, position -> {
            rowList.remove(position);
            adapter.notifyItemRemoved(position);
        });
        rv.setAdapter(adapter);

        Button btnAddRow = findViewById(R.id.btn_add_row);
        Button btnEdit = findViewById(R.id.btn_edit);
        Button btnSave = findViewById(R.id.btn_save);

        btnAddRow.setOnClickListener(v -> {
            if (isEditMode) {
                String currentDayDate = new SimpleDateFormat("EEE dd-MM-yyyy", Locale.getDefault()).format(new Date());
                WorkdoneRow newRow = new WorkdoneRow();
                newRow.dayDate = currentDayDate.toUpperCase(); // e.g., "MON 24-06-2025"
                rowList.add(newRow);
                adapter.notifyItemInserted(rowList.size() - 1);
            }
        });

        btnAddRow.setVisibility(View.GONE);

        btnEdit.setOnClickListener(v -> {
            isEditMode = true;
            btnAddRow.setVisibility(View.VISIBLE);
            adapter.setEditMode(true);
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "Edit mode enabled", Toast.LENGTH_SHORT).show();
        });

        btnSave.setOnClickListener(v -> {
            isEditMode = false;
            btnAddRow.setVisibility(View.GONE);
            adapter.setEditMode(false);
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
        });

        // Prepopulate a row with current day and date
        String currentDayDate = new SimpleDateFormat("EEE dd-MM-yyyy", Locale.getDefault()).format(new Date());
        WorkdoneRow firstRow = new WorkdoneRow();
        firstRow.dayDate = currentDayDate.toUpperCase();
        rowList.add(firstRow);
        adapter.notifyItemInserted(0);
    }
}
