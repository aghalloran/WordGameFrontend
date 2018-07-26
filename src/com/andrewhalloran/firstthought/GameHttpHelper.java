package com.andrewhalloran.firstthought;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GameHttpHelper {

	public final static String GAME_SERVER = "http://ec2-23-21-7-11.compute-1.amazonaws.com:8080";
	public final static String API_ROOT = "/wordgame/api/v1";
	public final static String PLAYER_RESOURCE = API_ROOT + "/players/";
	public final static String ROUNDS_RESOURCE = API_ROOT + "/rounds/";
	public final static String STATES_RESOURCE = API_ROOT + "/states/";
	public final static String USERNAME = "username";
	public final static String EMAIL = "email";
	public final static String HTTP_POST = "POST";
//	public final static String HTTP_PATCH = "PATCH"; // PATCH IS NOT SUPPORTED BY HttpURLConnection!!!
	public final static String HTTP_PUT = "PUT";
	public final static String HTTP_GET = "GET";
	
	public String getUserResourceURIFromGame(String username, JSONObject game){
		String user_uri = null;
		JSONObject player_map = game.optJSONObject("players_names");
		user_uri = player_map.optString(username);
		return user_uri;
	}

	public String getUserResourceNameFromGame(String user_uri, JSONObject game){
		String user_name = null;
		JSONObject player_map = game.optJSONObject("players_names");
		Iterator<String> keys = player_map.keys();
		while(keys.hasNext()){
			String name = keys.next();
			String uri = player_map.optString(name);
			if(uri.equals(user_uri)){
				 user_name = name;
				 break;
			}
		}
		
		return user_name;
	}
	
	public String getUserAnswer(String user_uri, JSONObject game){
		JSONObject user_state = getUserState(user_uri, game);
		String answer = user_state.optString("answer");
		return answer;
	}
	
	public JSONObject getUserState(String user_uri, JSONObject game){
		JSONObject user_state = null;
		JSONArray game_states = game.optJSONArray("states");
		user_state = getUserState(user_uri,game_states);
		return user_state;
	}
	
	public JSONObject getUserState(String user_uri, JSONArray game_states) {
		JSONObject user_state = null;
		
		for(int i=0, num_states=game_states.length(); i<num_states; i++){
			JSONObject state = game_states.optJSONObject(i);
			String ri = state.optString("player");
			if(ri.equalsIgnoreCase(user_uri)){
				user_state = state;
				break;
			}
		}
		
		return user_state;
	}
	
	public Map<String, String> getUserResourceURIs(List<String> usernames) throws GameException{
		Iterator<String> i = usernames.iterator();
		HashMap<String, String> map = new HashMap<String, String>();
		while(i.hasNext()){
			String username = i.next().trim();
			String resource_uri = getUserResourceURI(username);
			map.put(username, resource_uri);
		}
			
		return map;
	}
	
	public String getUserResourceURI(String username) throws GameException{
		String url_string = GAME_SERVER + PLAYER_RESOURCE + "?username=" + username;
		byte[] data = null;
		JSONObject jo_response = httpConnect(HTTP_GET, url_string, data);
		String resource_uri = "";
		
		try {
			JSONObject meta = jo_response.getJSONObject("meta");
			int query_count = meta.getInt("total_count");
			
			if(query_count == 0) {
				throw new GameException("We couldn't find a user named " + username + ".");
			}
			else if(query_count > 1) {
				throw new GameException("We found multiple users with the name" + username);
			}
			
			JSONArray jo_objects = jo_response.getJSONArray("objects");
			JSONObject jo_user = jo_objects.getJSONObject(0);
			resource_uri = jo_user.getString("resource_uri");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		} 
		
		return resource_uri;
	}
	
	public String getUserName(String resource_uri) throws GameException {
		String username = null;
		String url_string = GAME_SERVER + resource_uri;
		byte[] data = null;
		JSONObject jo_response = httpConnect(HTTP_GET, url_string, data);
		username = jo_response.optString("username");
		
		return username;
	}
	
	public JSONObject getRoundFromPartialUrl(String partial_url) throws GameException{
		String full_url = GAME_SERVER + partial_url;
		return getRound(full_url);
	}
	
	public JSONObject getRound(String url_string) throws GameException {
		byte[] data = null;
		JSONObject jo_response = httpConnect(HTTP_GET, url_string, data);
		JSONObject users = getPlayersMap(jo_response);
		try {
			jo_response.put("players_names", users);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return jo_response;
	}
	
	private JSONObject getPlayersMap(JSONObject game) throws GameException{
		JSONArray players = game.optJSONArray("players");
		HashMap<String, String> players_map = new HashMap<String, String>();
		
		for(int i=0, size=players.length(); i < size; i++){
			String player = players.optString(i);
			String name = getUserName(player);
			players_map.put(name, player);
		}
		
		JSONObject users = new JSONObject(players_map);
		return users;
	}
	
	public List<JSONObject> getUnansweredRounds(String player_name) throws GameException{
		// Get the unanswered round states, which we will use to get the round objects
		JSONObject states = getUnansweredRoundStates(player_name);
		JSONArray states_array = null;
		
		// Build a list of the round URIs
		ArrayList<String> rounds_urls = new ArrayList();
		states_array = states.optJSONArray("objects");
		for(int i=0, size = states_array.length(); i < size; i++){
			JSONObject jo = states_array.optJSONObject(i);
			rounds_urls.add(GAME_SERVER + jo.optString("round"));
		}
		
		// Get the rounds
		ArrayList<JSONObject> rounds = new ArrayList();
		Iterator<String> it = rounds_urls.iterator();
		while(it.hasNext()){
			String url_string = it.next();
			byte[] data = null;
			JSONObject r = httpConnect(HTTP_GET, url_string, data);
			
			try {
				JSONObject users = getPlayersMap(r);
				r.put("players_names", users);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			rounds.add(r);
		}
		return rounds;
	}
	
	public JSONObject getUnansweredRoundStates(String player_name) throws GameException {
		String url_string = GAME_SERVER + STATES_RESOURCE + "?answer=&player__username=" + player_name;
		byte[] data = null;
		JSONObject jo_response = httpConnect(HTTP_GET, url_string, data);
		
		try {
			JSONObject meta = jo_response.getJSONObject("meta");
			int query_count = meta.getInt("total_count");
			
			if(query_count == 0){
				throw new GameException("Could not find any unanswered rounds.");
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		
		return jo_response;
	}
	
    public JSONObject postNewUser(String username, String emailaddress) throws GameException {
    	String url_string = GAME_SERVER + PLAYER_RESOURCE;
    	byte[] post_data = getPlayerCreatePostData(username, emailaddress);
    	JSONObject user = httpConnect(HTTP_POST, url_string, post_data);
    	
    	return user;
    }	
    
    private byte[] getPlayerCreatePostData(String username, String emailaddress){
    	JSONObject jo = new JSONObject();
    	
    	try {
			jo.put("email", emailaddress);
			jo.put("first_name","");
			jo.put("is_active", true);
			jo.put("is_staff", false);
			jo.put("is_superuser", false);
			jo.put("last_name", "");
			jo.put("password", "");
			jo.put("username", username);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
    	    	
    	byte[] post_data = getPostBytes(jo);
    	
    	return post_data;
    }
    
    public JSONObject postNewGame(Collection<String> players) throws GameException {
    	String url_string = GAME_SERVER + ROUNDS_RESOURCE;
    	byte[] post_data = getGameCreatePostData(players);
    	JSONObject game = httpConnect(HTTP_POST, url_string, post_data);

		JSONObject users = getPlayersMap(game);
		try {
			game.put("players_names", users);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return game;
    }
    
    private byte[] getGameCreatePostData(Collection<String> players) {
		JSONObject jo = new JSONObject();
		JSONArray ja = new JSONArray(players);
		
		try {
			jo.put("players", ja);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}		

    	byte[] post_data = getPostBytes(jo);
		
		return post_data;
	}
    
    /**
     * 
     * @param state_resource_uri This should be in the form "/wordgame/api/v1/states/X/" where X is the number which identifies the state resource.
     * @param answer
     * @return The updated game as JSONObject
     * @throws GameException
     */
//    public JSONObject patchRoundAnswer(JSONObject game, String username_uri, String answer) throws GameException {
//    	JSONArray game_states = game.optJSONArray("states");
//    	JSONObject user_state = null;
//    	for(int i=0, num_states=game_states.length(); i<num_states; i++){
//    		JSONObject state = game_states.optJSONObject(i);
//    		String player = state.optString("player");
//    		if(player.equalsIgnoreCase(username_uri)){
//    			user_state = state;
//    			break;
//    		}
//    	}
//    	    	
//    	String state_resource_uri = user_state.optString("resource_uri");
//    	    	
//    	String url_string = GAME_SERVER + state_resource_uri;
//    	byte[] patch_data = getRoundAnswerPatchData(answer);
//    	JSONObject round_state = httpConnect(HTTP_PATCH,url_string, patch_data);
//    	
//    	String game_url_string = GAME_SERVER + game.optString("resource_uri");
//    	JSONObject new_game = this.getRound(game_url_string);    	
//    	
//    	return new_game;
//    }

    public JSONObject putRoundAnswer(JSONObject game, String username_uri, String answer) throws GameException {
    	JSONArray game_states = game.optJSONArray("states");
    	JSONObject user_state = getUserState(username_uri, game);

    	try {
			user_state.put("answer", answer);
			user_state.remove("point_total");
			user_state.remove("game_score");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	String state_resource_uri = user_state.optString("resource_uri");
    	    	
    	String url_string = GAME_SERVER + state_resource_uri;
    	byte[] patch_data = getPostBytes(user_state);
    	JSONObject round_state = httpConnect(HTTP_PUT,url_string, patch_data);
    	String game_url_string = GAME_SERVER + game.optString("resource_uri");
    	JSONObject new_game = this.getRound(game_url_string);    	
    	
    	return new_game;
    }    
    
//	private byte[] getRoundAnswerPatchData(String answer) {
//		JSONObject jo = new JSONObject();
//		
//		try {
//			jo.put("answer", answer);
//		} catch (JSONException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			throw new RuntimeException(e);
//		}
//		
//		byte[] patch_data = getPostBytes(jo);
//		
//		return patch_data;
//	}

	private byte[] getPostBytes(JSONObject jo) {
		byte[] post_data = null;
		try {
			post_data = jo.toString().getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return post_data;
	}
	
	public JSONObject httpConnect(String method, String url_string, byte[] post_data) throws GameException {    	
    	JSONObject json_object = null;   	
		HttpURLConnection httpcon = null;
		
    	try {
    		URL url = new URL(url_string);
    		
			httpcon = (HttpURLConnection) url.openConnection();
		
			// Check for a redirect due to a WiFi network blocking access until the user clicks through a sign-on page
			if(!url.getHost().equals(httpcon.getURL().getHost())){
				throw new GameException("Unexpected Redirect. Ensure you are connected to the Internet.");
			}
			
			httpcon.setRequestMethod(method);
			httpcon.setRequestProperty("Accept", "application/json");
			httpcon.setReadTimeout(10000);
			httpcon.setConnectTimeout(15000);
			httpcon.setDoInput(true);
			if(post_data != null) {
				httpcon.setDoOutput(true);
				httpcon.setFixedLengthStreamingMode(post_data.length);
				httpcon.setRequestProperty("Content-Type", "application/json");
			}
			
			

			httpcon.connect();

			// Write data
			if(post_data != null){
				BufferedOutputStream os = new BufferedOutputStream(httpcon.getOutputStream());
				os.write(post_data);
				os.close();
			}
			
			// Check to see if the response was good. The variables are not needed but left for debugging purposes
			int response_code = httpcon.getResponseCode();
			String response_message = httpcon.getResponseMessage();
			
			// Read response 
			if(httpcon.getResponseCode() == HttpURLConnection.HTTP_CREATED 
					|| httpcon.getResponseCode() == HttpURLConnection.HTTP_ACCEPTED
					|| httpcon.getResponseCode() == HttpURLConnection.HTTP_OK
					|| httpcon.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT){
				BufferedReader br = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));
				StringBuilder sb = new StringBuilder();
				String line = null;
				while((line = br.readLine()) != null){
					sb.append(line);
				}
				br.close();
				String response_string = sb.toString();
				try {
					JSONObject response_object = new JSONObject(response_string);
					json_object = response_object;
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else {
				// The POST/PATCH/GET failed, do something here to figure it out.
				BufferedReader br = new BufferedReader(new InputStreamReader(httpcon.getErrorStream()));
				StringBuilder sb = new StringBuilder();
				String line = null;
				while((line = br.readLine()) != null){
					sb.append(line);
				}
				br.close();
				String error_string = sb.toString();
				
				// TODO Should this be an alert dialog?
				throw new GameException(error_string);
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new GameException(e);
		} catch (ProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new GameException(e);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new GameException(e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			// TODO Should this be an alert dialog?
			throw new GameException("Oops! We cannot connect to the game server! Hopefully we get that back up soon.");
		} finally {
			if(httpcon != null){
				httpcon.disconnect();
			}
		}

    	return json_object;
    }
}
