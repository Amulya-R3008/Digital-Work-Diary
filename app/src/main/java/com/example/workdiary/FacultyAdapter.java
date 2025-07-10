package com.example.workdiary;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FacultyAdapter extends RecyclerView.Adapter<FacultyAdapter.FacultyViewHolder> {
    private List<Faculty> facultyList;
    private OnStatusClickListener listener;

    public interface OnStatusClickListener {
        void onStatusClick(Faculty faculty);
    }

    public FacultyAdapter(List<Faculty> facultyList, OnStatusClickListener listener) {
        this.facultyList = facultyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FacultyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_faculty, parent, false);
        return new FacultyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FacultyViewHolder holder, int position) {
        Faculty faculty = facultyList.get(position);
        holder.tvFacultyName.setText(faculty.getName());

        if ("Submitted".equalsIgnoreCase(faculty.getStatus())) {
            holder.btnSubmitted.setVisibility(View.VISIBLE);
            holder.btnPending.setVisibility(View.GONE);
        } else {
            holder.btnSubmitted.setVisibility(View.GONE);
            holder.btnPending.setVisibility(View.VISIBLE);
        }

        holder.btnSubmitted.setOnClickListener(v -> listener.onStatusClick(faculty));
        holder.btnPending.setOnClickListener(v -> listener.onStatusClick(faculty));
    }

    @Override
    public int getItemCount() {
        return facultyList.size();
    }

    static class FacultyViewHolder extends RecyclerView.ViewHolder {
        TextView tvFacultyName;
        Button btnSubmitted, btnPending;
        FacultyViewHolder(View itemView) {
            super(itemView);
            tvFacultyName = itemView.findViewById(R.id.tvFacultyName);
            btnSubmitted = itemView.findViewById(R.id.btnSubmitted);
            btnPending = itemView.findViewById(R.id.btnPending);
        }
    }
}
