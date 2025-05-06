package com.example.workdiary;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.util.List;

public class TopicPlannerAdapter extends FragmentStateAdapter {

    private List<String> subjectsList;

    public TopicPlannerAdapter(@NonNull FragmentActivity fragmentActivity, List<String> subjects) {
        super(fragmentActivity);
        this.subjectsList = subjects;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Create a new TopicPlannerFragment for each subject
        String subjectName = subjectsList.get(position);
        Bundle bundle = new Bundle();
        bundle.putString("subjectName", subjectName);

        TopicPlannerFragment fragment = new TopicPlannerFragment();
        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public int getItemCount() {
        return subjectsList.size(); // Number of subjects
    }
}
