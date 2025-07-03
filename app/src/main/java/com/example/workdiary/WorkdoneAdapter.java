package com.example.workdiary;

import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class WorkdoneAdapter extends RecyclerView.Adapter<WorkdoneAdapter.ViewHolder> {
    private final List<WorkdoneRow> rowList;
    private boolean isEditMode = false;

    public WorkdoneAdapter(List<WorkdoneRow> rowList) {
        this.rowList = rowList;
    }

    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WorkdoneAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_workdone_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkdoneAdapter.ViewHolder holder, int position) {
        WorkdoneRow row = rowList.get(position);
        holder.etDayDate.setText(row.dayDate);
        holder.etTime.setText(row.time);
        holder.etCourse.setText(row.course);
        holder.etPortion.setText(row.portion);
        holder.etClass.setText(row.classField);
        holder.etNo.setText(row.no);
        holder.etRemarks.setText(row.remarks);

        setEditTextEnabled(holder.etDayDate, isEditMode);
        setEditTextEnabled(holder.etTime, isEditMode);
        setEditTextEnabled(holder.etCourse, isEditMode);
        setEditTextEnabled(holder.etPortion, isEditMode);
        setEditTextEnabled(holder.etClass, isEditMode);
        setEditTextEnabled(holder.etNo, isEditMode);
        setEditTextEnabled(holder.etRemarks, isEditMode);

        // Save edits back to model
        holder.etDayDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) row.dayDate = holder.etDayDate.getText().toString();
        });
        holder.etTime.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) row.time = holder.etTime.getText().toString();
        });
        holder.etCourse.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) row.course = holder.etCourse.getText().toString();
        });
        holder.etPortion.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) row.portion = holder.etPortion.getText().toString();
        });
        holder.etClass.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) row.classField = holder.etClass.getText().toString();
        });
        holder.etNo.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) row.no = holder.etNo.getText().toString();
        });
        holder.etRemarks.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) row.remarks = holder.etRemarks.getText().toString();
        });

        holder.btnDelete.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                rowList.remove(pos);
                notifyItemRemoved(pos);
                notifyItemRangeChanged(pos, rowList.size() - pos);
            }
        });
    }

    private void setEditTextEnabled(EditText et, boolean enabled) {
        et.setFocusable(enabled);
        et.setFocusableInTouchMode(enabled);
        et.setInputType(enabled ? InputType.TYPE_CLASS_TEXT : InputType.TYPE_NULL);
        et.setCursorVisible(enabled);
    }

    @Override
    public int getItemCount() {
        return rowList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        EditText etDayDate, etTime, etCourse, etPortion, etClass, etNo, etRemarks;
        ImageButton btnDelete;

        ViewHolder(View itemView) {
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
}
