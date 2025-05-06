package com.example.workdiary;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;

public class TopicPlannerFragment extends Fragment {

    private String subjectName;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_topic_planner, container, false);

        // Get the subject name passed from the MainActivity
        subjectName = getArguments().getString("subjectName");

        // Set the subject name dynamically in the layout
        TextView subjectTextView = view.findViewById(R.id.subjectNameTextView);
        subjectTextView.setText(subjectName);

        // Optionally, fetch topics and other details for this subject from Back4App
        fetchTopicPlannerData(view);

        return view;
    }

    // Fetch topic planner data (topics, weeks, etc.) for the subject
    private void fetchTopicPlannerData(View view) {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("TopicPlanner");
        query.whereEqualTo("subjectName", subjectName); // Get data for the specific subject

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> topicList, ParseException e) {
                if (e == null) {
                    // Populate the topic planner (e.g., weeks, topics, Bloom's Taxonomy)
                    displayTopicPlannerData(view, topicList);
                } else {
                    Toast.makeText(getActivity(), "Error fetching topics", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Display the topic planner data dynamically
    private void displayTopicPlannerData(View view, List<ParseObject> topicList) {
        // You can now iterate over topicList and fill your views with data
        // For example, using a TableLayout or other layouts to display topics

        for (ParseObject topic : topicList) {
            String week = topic.getString("week");
            String days = topic.getString("days");
            String unit = topic.getString("unit");
            String subTopic = topic.getString("subTopic");
            String bloomLevel = topic.getString("bloomLevel");
            String courseOutcomes = topic.getString("courseOutcomes");

            // You can now populate a dynamic TableRow or any other layout for each topic
            // For example, you can add TextViews dynamically in a TableLayout for each topic
        }
    }
}

