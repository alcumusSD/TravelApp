package com.example.hotelapp.fragments;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.hotelapp.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class WeatherFragment extends Fragment {

    StringBuilder jsonString;
    StringBuilder jsonString2;
    ListView listView;

    String name;
    String lat;
    String lon;
    EditText editText;
    String zipCode;
    TextView weatherTextView;
    TextView latTextView;
    TextView lonTextView;
    TextView locTextView;
    ImageView imageView2;

    ArrayList<String> minTempValues = new ArrayList<>();
    ArrayList<String> maxTempValues = new ArrayList<>();
    ArrayList<String> descriptions = new ArrayList<>();
    ArrayList<String> dates = new ArrayList<>();

    ArrayList<WeatherData> wList = new ArrayList<>();

    Button button;
    CustomAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_weather, container, false);

        // Initialize UI components
        editText = view.findViewById(R.id.zipCodeEditText);
        button = view.findViewById(R.id.submitBtn);
        weatherTextView = view.findViewById(R.id.weatherTextView);
        latTextView = view.findViewById(R.id.latTextView);
        lonTextView = view.findViewById(R.id.lonTextView);
        locTextView = view.findViewById(R.id.locTextView);
        listView = view.findViewById(R.id.listView);
        imageView2 = view.findViewById(R.id.imageView2);

        // Set the button listener
        button.setOnClickListener(v -> {
            zipCode = editText.getText().toString();
            if (!zipCode.isEmpty()) {
                // Clear existing data
                wList.clear();
                descriptions.clear();
                minTempValues.clear();
                maxTempValues.clear();
                dates.clear();
                // Fetch new weather data
                new AsyncThread(zipCode).execute();
            } else {
                Toast.makeText(getActivity(), "Please enter a ZIP code", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    // Set adapter for the ListView
    public void setAdapter() {
        adapter = new CustomAdapter(getActivity(), R.layout.adapter_layout, wList);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    // AsyncTask to fetch weather data
    public class AsyncThread extends AsyncTask<String, Void, String> {
        private final String zip;

        public AsyncThread(String zipCode) {
            this.zip = zipCode;
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                // Fetch geolocation based on ZIP code
                URL url = new URL("https://api.openweathermap.org/geo/1.0/zip?zip="+zip+",US&appid=ba86f83a7f6d97d8134a5ba145aab073");
                URLConnection urlConnection = url.openConnection();
                InputStream stream = urlConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
                jsonString = new StringBuilder();
                String inputLine;
                while ((inputLine = bufferedReader.readLine()) != null) {
                    jsonString.append(inputLine);
                }
                bufferedReader.close();
                JSONObject jsonObject = new JSONObject(jsonString.toString());
                name = jsonObject.getString("name");
                lat = jsonObject.getString("lat");
                lon = jsonObject.getString("lon");

                // Fetch 5-day forecast based on latitude and longitude
                URL url2 = new URL("https://api.openweathermap.org/data/2.5/forecast?lat="+lat+"&lon="+lon+"&appid=ba86f83a7f6d97d8134a5ba145aab073&units=imperial");
                URLConnection urlcon2 = url2.openConnection();
                InputStream stream2 = urlcon2.getInputStream();
                BufferedReader bR2 = new BufferedReader(new InputStreamReader(stream2));
                jsonString2 = new StringBuilder();
                while ((inputLine = bR2.readLine()) != null) {
                    jsonString2.append(inputLine);
                }
                bR2.close();
                return jsonString2.toString();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String weatherData) {
            if (weatherData == null) {
                Toast.makeText(getActivity(), "Please enter a valid ZIP code", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                JSONObject jsonObj = new JSONObject(weatherData);
                JSONArray listArray = jsonObj.getJSONArray("list");

                for (int i = 0; i < listArray.length(); i += 8) {
                    JSONObject listItem = listArray.getJSONObject(i);
                    JSONObject main = listItem.getJSONObject("main");
                    JSONArray weather = listItem.getJSONArray("weather");
                    String desc = weather.getJSONObject(0).getString("description");
                    String minTemp = main.getString("temp_min");
                    String maxTemp = main.getString("temp_max");
                    String dtText = listItem.getString("dt_txt");

                    minTempValues.add(minTemp);
                    maxTempValues.add(maxTemp);
                    descriptions.add(desc);
                    dates.add(dtText);
                }

                // Create WeatherData objects
                for (int i = 0; i < 5; i++) {
                    WeatherData day = new WeatherData(dates.get(i), minTempValues.get(i), maxTempValues.get(i), descriptions.get(i));
                    wList.add(day);
                }

                // Update the UI
                setAdapter();

                // Update current weather info
                JSONObject firstListItem = listArray.getJSONObject(0);
                JSONObject main = firstListItem.getJSONObject("main");
                String cTemp = main.getString("temp");
                JSONArray weather1 = firstListItem.getJSONArray("weather");
                String cDesc = weather1.getJSONObject(0).getString("description");

                weatherTextView.setText("CURRENT TEMP: " + cTemp + "°");
                latTextView.setText("LATITUDE: " + lat);
                lonTextView.setText("LONGITUDE: " + lon);
                locTextView.setText("LOCATION: " + name);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    // WeatherData class to store weather info
    public class WeatherData {
        private final String date;
        private final String minTemp;
        private final String maxTemp;
        private final String desc;

        public WeatherData(String date, String minTemp, String maxTemp,  String desc) {
            this.date = date;
            this.minTemp = minTemp;
            this.maxTemp = maxTemp;
            this.desc = desc;
        }

        public String getDate() {
            return date;
        }

        public String getMinTemp() {
            return minTemp;
        }

        public String getMaxTemp() {
            return maxTemp;
        }



        public String getDesc() {
            return desc;
        }
    }

    // Custom Adapter for the ListView
    public class CustomAdapter extends ArrayAdapter<WeatherData> {
        private final List<WeatherData> list;
        private final Context context;
        private final int xmlResource;

        public CustomAdapter(Context context, int resource, ArrayList<WeatherData> objects) {
            super(context, resource, objects);
            xmlResource = resource;
            list = objects;
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(xmlResource, null);

            TextView dateTextView = convertView.findViewById(R.id.dateTextView);
            TextView minTempTextView = convertView.findViewById(R.id.minTempTextView);
            TextView maxTempTextView = convertView.findViewById(R.id.maxTempTextView);
            TextView descTextView = convertView.findViewById(R.id.descTextView);

            WeatherData day = list.get(position);
            dateTextView.setText(day.getDate());
            minTempTextView.setText(day.getMinTemp() + "°");
            maxTempTextView.setText(day.getMaxTemp() + "°");
            descTextView.setText(day.getDesc());

            return convertView;
        }
    }
}
