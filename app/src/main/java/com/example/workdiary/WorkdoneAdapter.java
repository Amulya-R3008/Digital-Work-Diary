package com.example.workdiary;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class WorkdoneAdapter extends RecyclerView.Adapter<WorkdoneAdapter.WorkdoneViewHolder> {
    private final List<WorkdoneRow> rowList;
    private boolean isEditMode = false;
    private final OnRowDeleteListener deleteListener;

    public interface OnRowDeleteListener {
        void onRowDelete(int position);
    }

    public WorkdoneAdapter(List<WorkdoneRow> rowList, OnRowDeleteListener deleteListener) {
        this.rowList = rowList;
        this.deleteListener = deleteListener;
    }

    public void setEditMode(boolean isEditMode) {
        this.isEditMode = isEditMode;
    }

    @NonNull
    @Override
    public WorkdoneViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_workdone_row, parent, false);
        return new WorkdoneViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkdoneViewHolder holder, int position) {
        WorkdoneRow row = rowList.get(position);

        holder.etDayDate.setText(row.dayDate);
        holder.etTime.setText(row.time);
        holder.etClass.setText(row.className);
        holder.etCourse.setText(row.course);
        holder.etPortion.setText(row.portion);
        holder.etNo.setText(row.no);
        holder.etRemarks.setText(row.remarks);

        holder.clearWatchers();

        holder.etDayDate.addTextChangedListener(holder.getWatcher(s -> row.dayDate = s));
        holder.etTime.addTextChangedListener(holder.getWatcher(s -> row.time = s));
        holder.etClass.addTextChangedListener(holder.getWatcher(s -> row.className = s));
        holder.etCourse.addTextChangedListener(holder.getWatcher(s -> row.course = s));
        holder.etPortion.addTextChangedListener(holder.getWatcher(s -> row.portion = s));
        holder.etNo.addTextChangedListener(holder.getWatcher(s -> row.no = s));
        holder.etRemarks.addTextChangedListener(holder.getWatcher(s -> row.remarks = s));

        holder.etDayDate.setEnabled(isEditMode);
        holder.etTime.setEnabled(isEditMode);
        holder.etClass.setEnabled(isEditMode);
        holder.etCourse.setEnabled(isEditMode);
        holder.etPortion.setEnabled(isEditMode);
        holder.etNo.setEnabled(isEditMode);
        holder.etRemarks.setEnabled(isEditMode);

        // Show/hide delete (cross) button
        holder.btnDelete.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (deleteListener != null && pos != RecyclerView.NO_POSITION) {
                deleteListener.onRowDelete(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return rowList.size();
    }

    static class WorkdoneViewHolder extends RecyclerView.ViewHolder {
        EditText etDayDate, etTime, etClass, etCourse, etPortion, etNo, etRemarks;
        ImageButton btnDelete;
        private final TextWatcher[] watchers = new TextWatcher[7];

        public WorkdoneViewHolder(@NonNull View itemView) {
            super(itemView);
            etDayDate = itemView.findViewById(R.id.et_day_date);
            etTime = itemView.findViewById(R.id.et_time);
            etClass = itemView.findViewById(R.id.et_class);
            etCourse = itemView.findViewById(R.id.et_course);
            etPortion = itemView.findViewById(R.id.et_portion);
            etNo = itemView.findViewById(R.id.et_no);
            etRemarks = itemView.findViewById(R.id.et_remarks);
            btnDelete = itemView.findViewById(R.id.btn_delete_row);
        }

        void clearWatchers() {
            EditText[] fields = {etDayDate, etTime, etClass, etCourse, etPortion, etNo, etRemarks};
            for (int i = 0; i < fields.length; i++) {
                if (watchers[i] != null) fields[i].removeTextChangedListener(watchers[i]);
                watchers[i] = null;
            }
        }

        TextWatcher getWatcher(java.util.function.Consumer<String> consumer) {
            TextWatcher watcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) { consumer.accept(s.toString()); }
            };
            for (int i = 0; i < watchers.length; i++) {
                if (watchers[i] == null) {
                    watchers[i] = watcher;
                    break;
                }
            }
            return watcher;
        }
    }
}
