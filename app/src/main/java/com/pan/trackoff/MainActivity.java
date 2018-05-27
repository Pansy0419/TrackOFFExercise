package com.pan.trackoff;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String baseUrl = "https://haveibeenpwned.com/api/v2/breachedaccount/";
    private static final String TAG = "MainActivity";

    private static final String NODE_DATACLASSES = "DataClasses";
    private static final String NODE_TITLE = "Title";
    private static final String NODE_DATE = "BreachDate";
    private static final String NODE_DESCRIPTION = "Description";

    ListAdapter adapter;

    private ListView resultListView;
    private Button button;
    private EditText editText;

    private String text;


    private ProgressDialog progressDialog;
    ArrayList<HashMap<String, String>> resultList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        resultList = new ArrayList<>();

        resultListView = (ListView) findViewById(R.id.result_listview);

        button = (Button) findViewById(R.id.button);
        editText = (EditText) findViewById(R.id.username_text);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String account = editText.getText().toString();
                new ParseJson(account).execute();
                hideKeyboard(MainActivity.this);
            }
        });

        if (savedInstanceState != null)
            editText.setText(savedInstanceState.getString("editText"));
        if (adapter != null)
            resultListView.setAdapter(adapter);
    }

    /**
     * Task for retrieving and parsing json file.
     */
    private class ParseJson extends AsyncTask<Void, Void, Void> {
        private String accountName;

        public ParseJson(String accountName) {
            this.accountName = accountName;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMessage("Loading...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            HttpHandler httpHandler = new HttpHandler();

            // Making a request to url and getting response
            String jsonStr = httpHandler.makeServiceCall(baseUrl + accountName);

            Log.v(TAG, "Response from url: " + baseUrl + accountName + " is " + jsonStr);

            if (jsonStr != null) {
                try {
                    // Getting JSON Array node
                    JSONArray results = new JSONArray(jsonStr);

                    // looping through All results
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject json = results.getJSONObject(i);

                        String title = json.getString(NODE_TITLE);
                        String date = json.getString(NODE_DATE);
                        String description = json.getString(NODE_DESCRIPTION);

                        StringBuffer sb = new StringBuffer();
                        JSONArray dataClasses = json.getJSONArray(NODE_DATACLASSES);
                        for (int j = 0; j < dataClasses.length(); j++) {
                            if(j != 0)
                                sb.append(", ");
                            sb.append(dataClasses.getString(j));
                            Log.d(TAG, "data class index : " + j);
                        }
                        String data = sb.toString();

                        // tmp hash map for single result
                        HashMap<String, String> result = new HashMap<>();

                        result.put(NODE_TITLE, title);
                        result.put(NODE_DATE, date);
                        result.put(NODE_DESCRIPTION, Html.fromHtml(description).toString());
                        result.put(NODE_DATACLASSES, data);

                        resultList.add(result);

                        Log.v(TAG, "Parsed result NO." + i);
                    }
                } catch (final JSONException e) {
                    Log.e(TAG, "Json parsing error: " + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Json parsing error: " + e.getMessage(),
                                    Toast.LENGTH_LONG)
                                    .show();
                        }
                    });
                }
            } else {
                Log.e(TAG, "Couldn't get json from server.");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Couldn't get json from server.",
                                Toast.LENGTH_LONG)
                                .show();
                    }
                });

            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // Dismiss the progress dialog
            if (progressDialog.isShowing())
                progressDialog.dismiss();
            /**
             * Updating parsed JSON data into ListView
             * */
            adapter = new SimpleAdapter(
                    MainActivity.this, resultList,
                    R.layout.item_listview, new String[]{NODE_TITLE, NODE_DATE, NODE_DESCRIPTION, NODE_DATACLASSES},
                    new int[]{R.id.listview_title,R.id.listview_date, R.id.listview_description, R.id.listview_data});
            resultListView.setAdapter(adapter);

            editText.setText("");

            Log.v(TAG, "Loaded data into listview");
        }
    }


    @Override
    protected void onPause() {
        text = editText.getText().toString();
        super.onPause();
    }

    @Override
    protected void onResume() {
        if (text != null)
            editText.setText(text);
        if (resultList != null && resultList.size() > 0) {
            adapter = new SimpleAdapter(
                    MainActivity.this, resultList,
                    R.layout.item_listview, new String[]{NODE_TITLE, NODE_DATE, NODE_DESCRIPTION, NODE_DATACLASSES},
                    new int[]{R.id.listview_title,R.id.listview_date, R.id.listview_description, R.id.listview_data});
            resultListView.setAdapter(adapter);
        }
        super.onResume();
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


}
