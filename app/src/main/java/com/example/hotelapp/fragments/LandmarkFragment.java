package com.example.hotelapp.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hotelapp.R;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class LandmarkFragment extends Fragment {

    private static final String TAG = "";  // Tag for debugging
    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView imageView;
    private TextView txtLandmarkResult;
    private Bitmap selectedImageBitmap;
    private FirebaseFunctions functions;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_landmark, container, false);

        Button btnSelectImage = view.findViewById(R.id.btnSelectImage);
        imageView = view.findViewById(R.id.imageView);
        txtLandmarkResult = view.findViewById(R.id.txtLandmarkResult);

        functions = FirebaseFunctions.getInstance();
        Log.d(TAG, "FirebaseFunctions initialized");

        btnSelectImage.setOnClickListener(v -> {
            Log.d(TAG, "Select Image button clicked");
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult triggered with requestCode: " + requestCode + " resultCode: " + resultCode);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            Log.d(TAG, "Image URI: " + imageUri.toString());

            try {
                selectedImageBitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                imageView.setImageBitmap(selectedImageBitmap);
                Log.d(TAG, "Selected image bitmap set");
                detectLandmark(selectedImageBitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Error getting bitmap from URI: " + e.getMessage());
            }
        } else {
            Log.w(TAG, "Image selection failed or canceled");
        }
    }
    //Method for detecting landmarks
    private void detectLandmark(Bitmap bitmap) {
        Log.d(TAG, "detectLandmark called");

        Bitmap scaledBitmap = scaleBitmapDown(bitmap, 640);
        Log.d(TAG, "Bitmap scaled down");

        uploadImage(scaledBitmap);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        String base64encoded = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
        Log.d(TAG, "Bitmap converted to Base64 string for landmark detection");

        // Create a JSON request for landmark detection
        JsonObject request = new JsonObject();
        JsonObject image = new JsonObject();
        image.add("content", new JsonPrimitive(base64encoded));
        request.add("image", image);

        JsonObject feature = new JsonObject();
        feature.add("maxResults", new JsonPrimitive(5));
        feature.add("type", new JsonPrimitive("LANDMARK_DETECTION"));
        JsonArray features = new JsonArray();
        features.add(feature);
        request.add("features", features);

        Log.d(TAG, "JSON request created: " + request.toString());

        annotateImage(request.toString())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase function call successful");
                        // Task completed successfully, handle the response
                        StringBuilder resultText = new StringBuilder();
                        JsonArray annotations = task.getResult().getAsJsonArray();
                        for (JsonElement label : annotations.get(0).getAsJsonObject().get("landmarkAnnotations").getAsJsonArray()) {
                            JsonObject labelObj = label.getAsJsonObject();
                            String landmarkName = labelObj.get("description").getAsString();
                            float score = labelObj.get("score").getAsFloat() * 100;
                            resultText.append(landmarkName).append(" - ").append(score).append("%\n");
                        }
                        txtLandmarkResult.setText(resultText.toString());
                        Log.d(TAG, "Landmark detection result: " + resultText.toString());
                    } else {
                        // Handle failure
                        Exception e = task.getException();
                        Log.d(TAG, "Firebase function call failed", e);
                        if (e instanceof FirebaseFunctionsException) {
                            FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                            Log.d(TAG, "Firebase Functions error: " + ffe.getDetails());
                        }
                        txtLandmarkResult.setText("Landmark detection failed.");
                    }
                });
    }

    private void uploadImage(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        String base64encoded = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
        Log.d(TAG, "Bitmap converted to Base64 string for upload");

        JsonObject uploadRequest = new JsonObject();
        uploadRequest.add("image", new JsonPrimitive(base64encoded));

        // Call the Firebase Function to upload the image
        uploadImageToFirebase(uploadRequest.toString())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Image upload successful");
                        // Handle the successful upload if needed
                    } else {
                        Log.d(TAG, "Image upload failed", task.getException());
                        // Handle the failure
                    }
                });
    }

    private com.google.android.gms.tasks.Task<JsonElement> uploadImageToFirebase(String requestJson) {
        Log.d(TAG, "Calling Firebase function with upload request JSON");
        return functions
                .getHttpsCallable("uploadImage")
                .call(requestJson)
                .continueWith(task -> {
                    Object result = task.getResult().getData();
                    Log.d(TAG, "Firebase function response for upload received");
                    return com.google.gson.JsonParser.parseString(new com.google.gson.Gson().toJson(result));
                });
    }

    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        }
        Log.d(TAG, "Bitmap resized: " + resizedWidth + "x" + resizedHeight);
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    private com.google.android.gms.tasks.Task<JsonElement> annotateImage(String requestJson) {
        Log.d(TAG, "Calling Firebase function with request JSON");
        return functions
                .getHttpsCallable("annotateImage")
                .call(requestJson)
                .continueWith(task -> {
                    Object result = task.getResult().getData();
                    Log.d(TAG, "Firebase function response received");
                    return com.google.gson.JsonParser.parseString(new com.google.gson.Gson().toJson(result));
                });
    }
}
