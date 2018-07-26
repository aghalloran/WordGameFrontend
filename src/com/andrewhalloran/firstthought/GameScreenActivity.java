package com.andrewhalloran.firstthought;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class GameScreenActivity extends Activity {

	private HashMap<String, JSONObject> m_game_cache = new HashMap<String,JSONObject>();
	private JSONObject m_game = null;
	private String m_username = null;
	private String m_username_uri = null;
	private JSONObject m_player_names = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_screen);
        
        m_username = getIntent().getStringExtra(MainActivity.GAME_USERNAME);
        String game_string = getIntent().getStringExtra(MainActivity.GAME_STRING);
        try {
			JSONObject game_json = new JSONObject(game_string);
			m_game = game_json;
			String game_uri = m_game.optString("resource_uri");
			m_game_cache.put(game_uri, game_json);
			m_player_names = m_game.getJSONObject("players_names");
			m_username_uri = m_player_names.getString(m_username);
			loadLayout(game_json);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}  
    }
    
    private void loadLayout(JSONObject game_json){
    	try {
    		GameHttpHelper helper = new GameHttpHelper();
    		JSONObject user_state = null;
    		String user_answer = "";
	    	JSONArray game_states = game_json.getJSONArray("states");
	    	
	    	user_state = helper.getUserState(m_username_uri, game_states);
	    	user_answer = user_state.getString("answer");
	    		    	
			LayoutInflater inflater = this.getLayoutInflater();
			LinearLayout ll = (LinearLayout) findViewById(R.id.game_screen_container);
			View game_main = null;
			ll.removeAllViews();
			
			if(user_answer.equals("")){
				game_main = inflater.inflate(R.layout.activity_game_round_ask, null);
			}
			else{		
				game_main = inflater.inflate(R.layout.activity_game_round_answer, null);
			}
			
			ll.addView(game_main);
			TextView round_number = (TextView) game_main.findViewById(R.id.game_screen_round_number);
			round_number.setText(game_json.getString("round_number"));
			TextView word_label = (TextView) game_main.findViewById(R.id.the_word_label);
			word_label.setText(game_json.getString("word"));
			
			if(!user_answer.equals("")){
				
				// Set the users answer so that they can see
				TextView answer_label = (TextView) game_main.findViewById(R.id.the_answer_label);
				answer_label.setText(user_answer);			
			}
			
			// Load the score board
			for(int i=0, size=game_states.length(); i<size; i++){
				JSONObject gs = game_states.getJSONObject(i);
				JSONObject user_score = gs.optJSONObject("game_score");
				
				// Get the data
				String game_score_string = user_score.optString("player_total");
				String score_string = gs.optString("point_total");
				String name_string = helper.getUserResourceNameFromGame(gs.optString("player"), game_json);
				String answer_string = gs.optString("answer");
				
				// Inflate the view and get the display widget views
				View user_status = inflater.inflate(R.layout.activity_game_round_status_bar, null);
				TextView game_score = (TextView) user_status.findViewById(R.id.user_game_score);
				TextView score = (TextView) user_status.findViewById(R.id.user_score);
				TextView name = (TextView) user_status.findViewById(R.id.user_name);
				TextView answer = (TextView) user_status.findViewById(R.id.user_answer);
				
				// Populate the views with information
				game_score.setText(game_score_string);
				if(!score_string.equals("0")) score.setText(score_string);
				name.setText(name_string);
				if(user_answer.equals("")) {
					// If the user has not answered yet then don't show him/her other players' answers
					answer.setText("??????");
				}
				else{
					
					answer.setText(answer_string);
				}
								
				// Add the view to the main layout
				ll.addView(user_status);
			}
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
    }
    
    private JSONObject getLatestRound(){
    	GameHttpHelper helper = new GameHttpHelper();
    	String answer = null;
    	String next_round_uri = null;
    	JSONObject game = m_game;
    	while(true){
    		next_round_uri = game.optString("next_round");
    		JSONObject next_game = m_game_cache.get(next_round_uri);
    		
    		if(next_game == null){
    			return game;
    		}
    		else{
    			game = next_game;
    		}
    	}
    }
    
    public void goBack(View view){
    	Intent data = new Intent();
     	JSONObject game = getLatestRound();
     	data.putExtra(MainActivity.GAME_STRING, game.toString());
    	setResult(RESULT_OK, data);
    	finish();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_game_screen, menu);
        return true;
    }
    
    public void loadNextRound(View view){

		GameHttpHelper helper = new GameHttpHelper();
		JSONObject user_state = null;
		String user_answer = "";
    	JSONArray game_states = m_game.optJSONArray("states");
    	user_state = helper.getUserState(m_username_uri, game_states);
    	user_answer = user_state.optString("answer");    	
    	
    	if(user_answer.equals("")){
			Toast.makeText(getApplicationContext(), 
					getApplicationContext().getString(R.string.provide_an_answer), 
					Toast.LENGTH_LONG).show(); 
			return;
    	}
    	
    	String round_uri = m_game.optString("next_round");
    	JSONObject round = m_game_cache.get(round_uri);
    	round = null;
    	if(round == null){
    		loadRound(round_uri);
	    }
    	else{
    		m_game = round;		
    		loadLayout(round);    		
    	}
    }
    
    public void loadPreviousRound(View view){

		GameHttpHelper helper = new GameHttpHelper();
		JSONObject user_state = null;
		String user_answer = "";
    	JSONArray game_states = m_game.optJSONArray("states");
    	user_state = helper.getUserState(m_username_uri, game_states);
    	user_answer = user_state.optString("answer");    	
    	    	
    	String round_uri = m_game.optString("prev_round");
    	
    	if(round_uri == null || round_uri.equals("") || round_uri.equalsIgnoreCase("null")){
			Toast.makeText(getApplicationContext(), 
					"We can't get a round that doesn't exist!", 
					Toast.LENGTH_SHORT).show();    
    		return;
    	}
    	
    	JSONObject round = m_game_cache.get(round_uri);
    	round = null;
    	if(round == null){
    		loadRound(round_uri);
	    }
    	else{
    		m_game = round;		
    		loadLayout(round);    
    	}
    }    
    
    private void loadRound(String round_uri){
    	ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
    	if(networkInfo != null && networkInfo.isConnected()){
    		// Make the call, we are connected to the Internets!
    		new LoadRoundTask().execute(round_uri);
    	}
    	else {
			Toast.makeText(getApplicationContext(), 
					getApplicationContext().getString(R.string.network_connection_error), 
					Toast.LENGTH_LONG).show();
			return;
    	}    	
    }

    private class LoadRoundTask extends AsyncTask <String, String, JSONObject> {

		@Override
		protected JSONObject doInBackground(String... params) {
			String round_URL = params[0];
			GameHttpHelper gh = new GameHttpHelper();
			JSONObject jo = null;
			
			try {
				jo = gh.getRoundFromPartialUrl(round_URL);
				jo.put("players_names", m_player_names);
			} catch (GameException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				publishProgress(e.getMessage());
				return null;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
    			return;
    		}
    		m_game = result;
			String game_uri = m_game.optString("resource_uri");
			m_game_cache.put(game_uri, m_game);    		
    		loadLayout(result);
    	}
    }    
    
    public void submitAnswer(View view){
    	EditText editText = (EditText) findViewById(R.id.game_screen_answer_field);
    	String answer = editText.getText().toString();
    	
    	ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
    	if(networkInfo != null && networkInfo.isConnected()){
    		// Make the call, we are connected to the Internets!
    		new SubmitAnswerTask().execute(answer);
    	}
    	else {
			Toast.makeText(getApplicationContext(), 
					getApplicationContext().getString(R.string.network_connection_error), 
					Toast.LENGTH_LONG).show();
			return;
    	}
    }
    
    private class SubmitAnswerTask extends AsyncTask <String, String, JSONObject> {

		@Override
		protected JSONObject doInBackground(String... params) {
			String answer = params[0];
			GameHttpHelper gh = new GameHttpHelper();
			JSONObject jo = null;
			
			try {
				jo = gh.putRoundAnswer(m_game, m_username_uri, answer);
				jo.put("players_names", m_player_names);
			} catch (GameException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				publishProgress(e.getMessage());
				return null;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
    			return;
    		}
    		m_game = result;
    		loadLayout(result);
    	}
    }
}
