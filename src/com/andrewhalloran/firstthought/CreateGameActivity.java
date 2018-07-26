package com.andrewhalloran.firstthought;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

public class CreateGameActivity extends Activity {

	public static final int USERNAME_GAME = 0;	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_creategame);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_create_game, menu);
        return true;
    }
    
    public void createGameWithFacebook(View view){
    	// Add Facebook code here
		Toast.makeText(getApplicationContext(), 
				"Facebook not available", 
				Toast.LENGTH_SHORT).show();    	
    }
    
    public void createGameWithUsernames(View view){
    	Intent intent = new Intent(this, CreateGameUsernamesActivity.class);
    	startActivityForResult(intent, USERNAME_GAME);
    }
    
    public void createGameWithRandomOpponents(View view){

		Toast.makeText(getApplicationContext(), 
				"Random opponents not available", 
				Toast.LENGTH_SHORT).show();
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
    	if (requestCode == USERNAME_GAME) {
			if(resultCode == RESULT_OK){
				setResult(RESULT_OK,data);
				finish();
			}
		}
    }      
}
