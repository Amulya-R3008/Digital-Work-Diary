package com.example.workdiary;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        notifyDataSetChanged();
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
        holder.etCourse.setText(row.course);
        holder.etPortion.setText(row.portion);
        holder.etClass.setText(row.className);
        holder.etNo.setText(row.no);
        holder.etRemarks.setText(row.remarks);

        // Only focusable in edit mode
        setEditTextFocusable(holder.etDayDate, isEditMode);
        setEditTextFocusable(holder.etTime, isEditMode);
        setEditTextFocusable(holder.etCourse, isEditMode);
        setEditTextFocusable(holder.etPortion, isEditMode);
        setEditTextFocusable(holder.etClass, isEditMode);
        setEditTextFocusable(holder.etNo, isEditMode);
        setEditTextFocusable(holder.etRemarks, isEditMode);

        // TextWatchers to update model on user edit
        holder.etClass.addTextChangedListener(new SimpleTextWatcher(s -> row.className = s));
        holder.etNo.addTextChangedListener(new SimpleTextWatcher(s -> row.no = s));
        holder.etDayDate.addTextChangedListener(new SimpleTextWatcher(s -> row.dayDate = s));
        holder.etTime.addTextChangedListener(new SimpleTextWatcher(s -> row.time = s));
        holder.etCourse.addTextChangedListener(new SimpleTextWatcher(s -> row.course = s));
        holder.etPortion.addTextChangedListener(new SimpleTextWatcher(s -> row.portion = s));
        holder.etRemarks.addTextChangedListener(new SimpleTextWatcher(s -> row.remarks = s));

        holder.btnDelete.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (deleteListener != null && pos != RecyclerView.NO_POSITION) {
                deleteListener.onRowDelete(pos);
            }
        });
    }

    private void setEditTextFocusable(EditText editText, boolean focusable) {
        editText.setFocusable(focusable);
        editText.setFocusableInTouchMode(focusable);
        editText.setCursorVisible(focusable);
        editText.setLongClickable(focusable);
    }

    @Override
    public int getItemCount() {
        return rowList.size();
    }

    static class WorkdoneViewHolder extends RecyclerView.ViewHolder {
        EditText etDayDate, etTime, etCourse, etPortion, etClass, etNo, etRemarks;
        ImageButton btnDelete;

        public WorkdoneViewHolder(@NonNull View itemView) {
            super(itemView);
            etDayDate = itemView.findViewById(R.id.et_day_date);
            etTime = itemView.findViewById(R.id.et_time);
            etCourse = itemView.findViewById(R.id.et_course);
            etPortion = itemView.findViewById(R.id.et_portion);
            etClass = itemView.findViewById(R.id.et_class);
            etNo = itemView.findViewById(R.id.et_no);
            etRemarks = itemView.findViewById(R.id.et_remarks);
            btnDelete = itemView.findViewById(R.id.btn_delete_row);
        }
    }

    // Helper class for cleaner TextWatcher
    private static class SimpleTextWatcher implements TextWatcher {
        private final java.util.function.Consumer<String> onChanged;
        SimpleTextWatcher(java.util.function.Consumer<String> onChanged) { this.onChanged = onChanged; }
        @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
        @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
        @Override public void afterTextChanged(Editable s) { onChanged.accept(s.toString()); }
    }
}
