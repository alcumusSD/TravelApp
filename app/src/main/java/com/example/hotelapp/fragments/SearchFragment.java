package com.example.hotelapp.fragments;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hotelapp.R;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
//import com.squareup.picasso.Picasso;

public class SearchFragment extends Fragment {

    private EditText editText;
    private TextView textView;
    private ImageView imageView;  // Add an ImageView for displaying hotel images

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        editText = view.findViewById(R.id.editTextText);
        textView = view.findViewById(R.id.textView);
        imageView = view.findViewById(R.id.imageView);  // Initialize the ImageView
        Button searchButton = view.findViewById(R.id.searchButton);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonCallGeminiAPI();
            }
        });

        return view;
    }

    private void buttonCallGeminiAPI() {
        String userInput = editText.getText().toString();
        if (userInput.isEmpty()) {
            textView.setText("Please enter a location.");
            return; // Exit if no input
        }

        GenerativeModel gm = new GenerativeModel(
                "gemini-pro",
                "AIzaSyAtxFuRcSwmRsVzAQONOZB4eH9FQ-NU9nc"
        );

        GenerativeModelFutures model = GenerativeModelFutures.from(gm);
        Content content = new Content.Builder()
                .addText("Find the best hotels in " + userInput + ". Provide a detailed summary of the top five hotels, including features, amenities, guest ratings, and images.")
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    // Assuming result.getText() contains hotel info and image URLs
                    String resultText = result.getText();
                    textView.setText(resultText);

                    // Assuming you get a valid image URL from the response
                    // Replace "imageUrl" with the actual image URL from the response
                    String imageUrl = "https://example.com/sample-hotel.jpg"; // Replace with actual URL
                    //Picasso.get().load(imageUrl).into(imageView);  // Use Picasso to load the image into ImageView
                }

                @Override
                public void onFailure(@NonNull Throwable t) {
                    textView.setText("Error: " + t.getMessage());
                }
            }, requireActivity().getMainExecutor());
        }
    }
}
