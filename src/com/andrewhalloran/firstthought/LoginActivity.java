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

public class LoginActivity extends Activity {

	public static final int LOGIN_SETUP = 0;	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_login, menu);
        return true;
    }
    
    public void loginWithFacebook(View view){
    	// Add Facebook login code here
		Toast.makeText(getApplicationContext(), 
				"Facebook authentication not available", 
				Toast.LENGTH_LONG).show();
    }
    
    public void loginWithEmail(View view){
    	// Launch an intent for email configuration
    	Intent intent = new Intent(this, LoginEmailActivity.class);
    	startActivityForResult(intent, LOGIN_SETUP);
    }
    
    public void loginWithAnonymous(View view){
    	// Launch an intent for anonymous login

    	JSONObject json_user = null;
    	GameHttpHelper game_helper = new GameHttpHelper();
    	
    	// Make REST call to create the user on the server
    	ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
    	if(networkInfo != null && networkInfo.isConnected()){
    		// Make the call, we are connected to the Internets!
    		new PostUserTask().execute("", "");
    	}
    	else {
			Toast.makeText(getApplicationContext(), 
					getApplicationContext().getString(R.string.network_connection_error), 
					Toast.LENGTH_LONG).show();
			return;
    	}    	
    }
    
    private class PostUserTask extends AsyncTask {
    	
    	@Override
    	protected JSONObject doInBackground(Object... params) {
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
    	protected void onProgressUpdate(Object... values) {
			Toast.makeText(getApplicationContext(),
					(String)values[0],
					Toast.LENGTH_LONG).show();
    	}
    	
    	@Override
    	protected void onPostExecute(Object result) {
        	
    		if(result == null){
        		// TODO Put message here for error??? Might not be needed after the refactor.
        		return;
        	}
    		
    		JSONObject json_user = (JSONObject) result;
        	
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
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
    	if (requestCode == LOGIN_SETUP) {
			if(resultCode == RESULT_OK){
				setResult(RESULT_OK);
				finish();
			}
		}
    }    
}
