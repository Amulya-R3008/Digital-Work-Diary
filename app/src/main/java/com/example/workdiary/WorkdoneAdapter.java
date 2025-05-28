package com.example.workdiary;

import android.app.DatePickerDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.*;

public class WorkdoneAdapter extends RecyclerView.Adapter<WorkdoneAdapter.ViewHolder> {
    private final List<WorkdoneRow> rows;
    private final String[] remarksOptions = {
            "Seminar", "Talk from Expert", "9/13", "2/10", "Seminar Hall"
    };

    public interface OnRowDeleteListener {
        void onRowDelete(int position);
    }

    private final OnRowDeleteListener deleteListener;

    public WorkdoneAdapter(List<WorkdoneRow> rows, OnRowDeleteListener listener) {
        this.rows = rows;
        this.deleteListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_workdone_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        WorkdoneRow row = rows.get(pos);

        h.tvDayDate.setText(row.dayDate.isEmpty() ? "Select" : row.dayDate);
        h.etTime.setText(row.time);
        h.etClass.setText(row.classSection);
        h.etCourse.setText(row.course);
        h.etPortion.setText(row.portion);
        h.etStudents.setText(row.students);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(h.spinnerRemarks.getContext(),
                android.R.layout.simple_spinner_dropdown_item, remarksOptions);
        h.spinnerRemarks.setAdapter(adapter);
        int selIdx = Arrays.asList(remarksOptions).indexOf(row.remarks);
        h.spinnerRemarks.setSelection(selIdx >= 0 ? selIdx : 0);

        h.tvDayDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            DatePickerDialog dp = new DatePickerDialog(v.getContext(),
                    (view, year, month, day) -> {
                        String date = String.format("%s\n%02d/%02d/%02d",
                                getDayOfWeek(year, month, day), day, month + 1, year % 100);
                        h.tvDayDate.setText(date);
                        row.dayDate = date;
                    },
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            dp.show();
        });

        h.spinnerRemarks.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int p, long id) {
                row.remarks = remarksOptions[p];
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        h.etTime.addTextChangedListener(new SimpleTextWatcher(s -> row.time = s));
        h.etClass.addTextChangedListener(new SimpleTextWatcher(s -> row.classSection = s));
        h.etCourse.addTextChangedListener(new SimpleTextWatcher(s -> row.course = s));
        h.etPortion.addTextChangedListener(new SimpleTextWatcher(s -> row.portion = s));
        h.etStudents.addTextChangedListener(new SimpleTextWatcher(s -> row.students = s));

        h.btnDelete.setOnClickListener(v -> deleteListener.onRowDelete(pos));
    }

    @Override
    public int getItemCount() { return rows.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayDate;
        EditText etTime, etClass, etCourse, etPortion, etStudents;
        Spinner spinnerRemarks;
        ImageButton btnDelete;
        ViewHolder(View v) {
            super(v);
            tvDayDate = v.findViewById(R.id.tv_day_date);
            etTime = v.findViewById(R.id.et_time);
            etClass = v.findViewById(R.id.et_class);
            etCourse = v.findViewById(R.id.et_course);
            etPortion = v.findViewById(R.id.et_portion);
            etStudents = v.findViewById(R.id.et_students);
            spinnerRemarks = v.findViewById(R.id.spinner_remarks);
            btnDelete = v.findViewById(R.id.btn_delete);
        }
    }

    private String getDayOfWeek(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day);
        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        return days[cal.get(Calendar.DAY_OF_WEEK) - 1];
    }

    public static class SimpleTextWatcher implements TextWatcher {
        private final java.util.function.Consumer<String> consumer;
        public SimpleTextWatcher(java.util.function.Consumer<String> consumer) { this.consumer = consumer; }
        @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
        @Override public void onTextChanged(CharSequence s, int st, int b, int c) { consumer.accept(s.toString()); }
        @Override public void afterTextChanged(Editable s) {}
    }
}
