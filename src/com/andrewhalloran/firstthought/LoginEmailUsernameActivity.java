package com.andrewhalloran.firstthought;

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

public class LoginEmailUsernameActivity extends Activity {

	private String email_address = "";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_email_username);
        
        // Get the email from the last Intent
        Intent intent = getIntent();
        this.email_address = intent.getStringExtra(LoginEmailActivity.EMAIL_ADDRESS);
    }

    public void goBack(View view){
    	setResult(RESULT_CANCELED);
    	finish();
    }
      
    public void ok(View view){
    	
    	EditText editText = (EditText) findViewById(R.id.username);
    	String username = editText.getText().toString();
    	
    	// Make REST call to create the user on the server
    	ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
    	if(networkInfo != null && networkInfo.isConnected()){
    		// Make the call, we are connected to the Internets!
    		new PostUserTask().execute(username, this.email_address);
    	}
    	else {
			Toast.makeText(getApplicationContext(), 
					getApplicationContext().getString(R.string.network_connection_error), 
					Toast.LENGTH_LONG).show();
			return;
    	}
    }
    
    private class PostUserTask extends AsyncTask<String, String, JSONObject> {
    	
    	@Override
    	protected JSONObject doInBackground(String... params) {
    		// TODO Auto-generated method stub
    		JSONObject json_user = null;
        	GameHttpHelper game_helper = new GameHttpHelper();
    		try {
				json_user = game_helper.postNewUser((String)params[0],(String)params[1]);
			} catch (GameException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				publishProgress(e.getMessage());
				return null;
			}
    		
    		return json_user;
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
    		
    		JSONObject json_user = result;
        	
        	// Save the username and the email in the SharedPreferences
    		SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_LOGIN, MODE_PRIVATE);
    		SharedPreferences.Editor editor = settings.edit();
    		
    		try {
    			editor.putString(MainActivity.PREFS_LOGIN_USERNAME, json_user.getString(GameHttpHelper.USERNAME));
    			editor.putString(MainActivity.PREFS_LOGIN_EMAIL, json_user.getString(GameHttpHelper.EMAIL));
    		} catch (JSONException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    		editor.commit();    	
    		
        	setResult(RESULT_OK);
        	finish(); 
    	}
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_login_email_username, menu);
        return true;
    }
}
