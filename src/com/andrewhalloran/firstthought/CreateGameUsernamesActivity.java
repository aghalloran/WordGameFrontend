package com.andrewhalloran.firstthought;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class CreateGameUsernamesActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_creategame_usernames);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_create_game_usernames, menu);
        return true;
    }
    
    public void goBack(View view){
    	setResult(RESULT_CANCELED);
    	finish();
    }
    
    public void ok(View view){
    	
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_LOGIN, MODE_PRIVATE);
        String current_user = settings.getString(MainActivity.PREFS_LOGIN_USERNAME, null);
    	
    	EditText editText = (EditText) findViewById(R.id.game_create_usernames);
    	String usernames = editText.getText().toString();
    	ArrayList<String> users = new ArrayList(Arrays.asList(usernames.split(",")));
    	if(!users.contains(current_user)){
    		users.add(current_user);
    	}
    	
    	// Make REST call to create the user on the server
    	ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
    	if(networkInfo != null && networkInfo.isConnected()){
    		// Make the call, we are connected to the Internets!
    		new CreateGameTask().execute(users);
    	}
    	else {
			Toast.makeText(getApplicationContext(), 
					getApplicationContext().getString(R.string.network_connection_error), 
					Toast.LENGTH_LONG).show();
			return;
    	}
    }
    
    private class CreateGameTask extends AsyncTask<List<String>, String, JSONObject> {
    	
    	@Override
    	protected JSONObject doInBackground(List<String>... params) {
    		// TODO Auto-generated method stub
    		JSONObject jo = null;
        	GameHttpHelper game_helper = new GameHttpHelper();
    		try {
    			Map<String,String> user_map = game_helper.getUserResourceURIs(params[0]);
    			Collection<String> user_uris = user_map.values();
    			jo = game_helper.postNewGame(user_uris);
//    			JSONObject users = new JSONObject(user_map); // Taking this out for now, moved it into postNewGame call
//    			jo.put("players_names", users);
			} catch (GameException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				publishProgress(e.getMessage());
				return null;
			} 
    		
    		return jo;
    	}
    	
    	@Override
    	protected void onProgressUpdate(String... values) {
			Toast.makeText(getApplicationContext(),
					values[0],
					Toast.LENGTH_LONG).show();
    	}
    	
    	@Override
    	protected void onPostExecute(JSONObject result) {
        	
    		if(result == null){
        		// TODO Put message here for error??? Might not be needed after the refactor.
        		return;
        	}
    		
    		JSONObject json_game = result;
    		Intent data = new Intent();
    		data.putExtra(MainActivity.GAME_STRING, json_game.toString());
    		
        	setResult(RESULT_OK,data);
        	finish(); 
    	}
    }    
    
}
