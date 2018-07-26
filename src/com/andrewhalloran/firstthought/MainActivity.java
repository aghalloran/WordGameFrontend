package com.andrewhalloran.firstthought;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	public static final int LOGIN_REQUEST = 0;
	public static final int CREATE_GAME_REQUEST = 1;
	public static final int PLAY_GAME_ROUND = 2;
	public final static String PREFS_LOGIN = "Login";
	public final static String PREFS_LOGIN_USERNAME = "Username";
	public final static String PREFS_LOGIN_EMAIL = "Email";
	public final static String GAME_STRING = "game_string";
	public final static String GAME_USERNAME = "game_username";
	private HashMap<String, JSONObject> m_games = new HashMap<String, JSONObject>();
	private String m_username = null;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Check to see if user is logged in
        SharedPreferences settings = getSharedPreferences(PREFS_LOGIN, MODE_PRIVATE);
        m_username = settings.getString(PREFS_LOGIN_USERNAME, null);
        
		Toast.makeText(getApplicationContext(), 
				"Logged in as "+ m_username, 
				Toast.LENGTH_SHORT).show();	 
        
        // Launch LoginActivity if the user is not logged-in (null)
        if(m_username == null){
        	Intent intent = new Intent(this, LoginActivity.class);
        	startActivityForResult(intent, LOGIN_REQUEST);
        }
        
        // Setup view for users games
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected()) {
			// Make the call, we are connected to the Internets!
			new GetGamesTask().execute(m_username);
		} else {
			Toast.makeText(
					getApplicationContext(),
					getApplicationContext().getString(
							R.string.network_connection_error),
					Toast.LENGTH_LONG).show();
			return;
		}
    }
    
    public class GetGamesTask extends AsyncTask<String, String, HashMap<String,JSONObject>> {

		@Override
		protected HashMap<String,JSONObject> doInBackground(String... params) {
			HashMap<String,JSONObject> all_games = new HashMap<String, JSONObject>();
			String username = params[0];
			
			try{
				GameHttpHelper gh = new GameHttpHelper();
				List<JSONObject> games = gh.getUnansweredRounds(username);
				Iterator<JSONObject> it = games.iterator();
				while(it.hasNext()){
					JSONObject game = it.next();
					String key = game.optString("game_uid");
					
					if(all_games.containsKey(key)){
						int r1 = game.optInt("round_number");
						JSONObject jo = all_games.get(key);
						int r2 = jo.optInt("round_number");
						if(r1 < r2){
							all_games.put(key, game);
						}
					}
					else{
						all_games.put(key, game);
					}
				}
			} catch (GameException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				publishProgress(e.getMessage());
				return null;
			}
			
			return all_games;
		}

		@Override
		protected void onProgressUpdate(String... values) {
			Toast.makeText(getApplicationContext(), values[0],
					Toast.LENGTH_LONG).show();
		}

		@Override
		protected void onPostExecute(HashMap<String,JSONObject> result) {
			
			if(result == null) return;
			
			m_games = result;
	        Iterator<JSONObject> game_it = m_games.values().iterator();
	        while(game_it.hasNext()){
	        	JSONObject g = game_it.next();
	        	addGameToScreen(g);
	        }
		}
	}
    
    public void launchMenu(View view){
//    	m_username = null;
		Toast.makeText(getApplicationContext(), 
				"Open menu! Username="+m_username, 
				Toast.LENGTH_SHORT).show();
		
//		SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_LOGIN, MODE_PRIVATE);
//		SharedPreferences.Editor editor = settings.edit();	
//		editor.putString(MainActivity.PREFS_LOGIN_USERNAME, "");
//		editor.putString(MainActivity.PREFS_LOGIN_EMAIL, "");
    }
    
    public void createGame(View view){		  
		
		// Add code here to launch activity
		Intent intent = new Intent(this, CreateGameActivity.class);
		startActivityForResult(intent, CREATE_GAME_REQUEST);
    }    
    
    private void addGameToScreen(JSONObject game){
		try {
			JSONObject player_names = game.getJSONObject("players_names");
			Iterator<String> players_it = player_names.keys();
			
			String players = "";
			while(players_it.hasNext()){
				String player = players_it.next();
				players += player +", ";
			}
			players = players.substring(0,players.length()-2);
			if(players.length() > 30){
				players = players.substring(0, 30);
				players += "...";
			}
			
			LayoutInflater inflater = this.getLayoutInflater();
			LinearLayout ll = (LinearLayout) findViewById(R.id.games_container);
			View toRemove = ll.findViewWithTag(game.optString("game_uid"));
			if(toRemove != null){
				ll.removeView(toRemove);
			}
			View v = inflater.inflate(R.layout.game_button, null);
			v.setTag(R.id.game_identifier, game.getString("game_uid"));
			ll.addView(v);
			TextView tv1 = (TextView) v.findViewById(R.id.game_button_text1);
			tv1.setText("Word: "+game.getString("word"));
			TextView tv2 = (TextView) v.findViewById(R.id.game_button_text2);
			tv2.setText("Players: "+players);
			TextView round_num = (TextView) v.findViewById(R.id.game_button_round);
			round_num.setText(game.getString("round_number"));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
    }
    
    public void loadGame(View view){
		String game_id = (String) view.getTag(R.id.game_identifier);
		JSONObject game = m_games.get(game_id);
		
		Intent intent = new Intent(this, GameScreenActivity.class);
		intent.putExtra(GAME_STRING, game.toString());
		intent.putExtra(GAME_USERNAME, m_username);
		startActivityForResult(intent,this.PLAY_GAME_ROUND);
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
    	if (requestCode == LOGIN_REQUEST) {
			if(resultCode == RESULT_OK){

			}
		}
    	else if(requestCode == CREATE_GAME_REQUEST){
    		if(resultCode == RESULT_OK){
    			String game_str = data.getExtras().getString(GAME_STRING);
    			JSONObject game_json = null;
    			try {
					game_json = new JSONObject(game_str);
					m_games.put(game_json.getString("game_uid"), game_json);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    			addGameToScreen(game_json);
    		}
    	}
    	else if (requestCode == this.PLAY_GAME_ROUND) {
    		if(resultCode == RESULT_OK){
    			// Refresh game screen
    			String game_str = data.getStringExtra(MainActivity.GAME_STRING);
    			JSONObject game_json = null;
    			try {
					game_json = new JSONObject(game_str);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		
    			GameHttpHelper helper = new GameHttpHelper();
    			String user_uri = helper.getUserResourceURIFromGame(m_username, game_json);
    			String answer = helper.getUserAnswer(user_uri, game_json);
    			
    			if(answer == null || answer.equals("")){
    				m_games.put(game_json.optString("game_uid"), game_json);
    				reloadGames();
    			}
    			else{
    				String round_uri = game_json.optString("next_round");
    				ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
					NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
					if (networkInfo != null && networkInfo.isConnected()) {
						// Make the call, we are connected to the Internets!
						new GetGameTask().execute(round_uri);
					} else {
						Toast.makeText(
								getApplicationContext(),
								getApplicationContext().getString(
										R.string.network_connection_error),
								Toast.LENGTH_LONG).show();
						return;
					}
    			}
    		}			
		}
    }
    
    private void reloadGames(){
		LayoutInflater inflater = this.getLayoutInflater();
		LinearLayout ll = (LinearLayout) findViewById(R.id.games_container);
		ll.removeAllViews();
        Iterator<JSONObject> game_it = m_games.values().iterator();
        while(game_it.hasNext()){
        	JSONObject g = game_it.next();
        	addGameToScreen(g);
        }
    }
    
    private class GetGameTask extends AsyncTask<String, String, JSONObject> {

		@Override
		protected JSONObject doInBackground(String... params) {
			JSONObject rval = null;
			String round_url = params[0];
			GameHttpHelper helper = new GameHttpHelper();
			
			try {
				rval = helper.getRoundFromPartialUrl(round_url);
			} catch (GameException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				publishProgress(e.getMessage());
				return null;
			}
			
			return rval;
		}

		@Override
		protected void onProgressUpdate(String... values) {
			Toast.makeText(getApplicationContext(), values[0],
					Toast.LENGTH_LONG).show();
		}

		@Override
		protected void onPostExecute(JSONObject result) {
			if(result == null){
				return;
			}
			m_games.put(result.optString("game_uid"), result);
			reloadGames();
		}
	}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
}
