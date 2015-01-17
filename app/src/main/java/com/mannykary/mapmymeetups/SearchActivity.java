package com.mannykary.mapmymeetups;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;


public class SearchActivity extends ListActivity {

    private String[] categories;
    private HashMap<String, String> categoriesMap = new HashMap<String, String>();
    private EditText searchEditText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        
        try {
            categories = getCategories();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        setListAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                categories));
        searchEditText = (EditText) findViewById(R.id.editText_search);
        searchEditText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView tv, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    Log.i("SearchActivity", "Enter was pressed!");
                    Intent i = new Intent();
                    Log.i("SearchActivity", tv.getText().toString());
                    i.putExtra("searchQuery", tv.getText().toString());
                    //i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    setResult(MainActivity.RESULT_OK_SEARCH, i);
                    //startActivity(i);
                    finish();
                    
                    return true;
                }
                return false;
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onListItemClick(ListView parent, View v, int position, long id) {
        String selectedCategory = categories[position];
        //selection.setText(categories[position]);
        Intent i = new Intent();
        Log.i("SearchActivity", selectedCategory);
        i.putExtra("selectedCategory", categoriesMap.get(selectedCategory));
        //i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        setResult(MainActivity.RESULT_OK_CATEGORY, i);
        //startActivity(i);
        finish();
    }

    public String[] getCategories() throws JSONException {

        String q = "https://api.meetup.com/2/categories"
                + "?key=" + APIKeys.MEETUP
                + "&sign=true"
                + "&format=json";

        Log.i("SearchActivity", "getCategories query: " + q);

        String jq = null;

        try {
            jq = new JSONReaderTask().execute(q).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        Log.i("MapMyMeetups", "categories: " + jq);

        JSONArray results = new JSONObject(jq).getJSONArray("results");

        int numCategories = results.length();
        String[] categories = new String[numCategories];

        for (int i = 0; i < numCategories; i++){
            String categoryId = results.getJSONObject(i).getString("id");
            String categoryName = results.getJSONObject(i).getString("name");
            categories[i] = categoryName;
            categoriesMap.put(categoryName, categoryId);
        }

        return categories;
    }
}
