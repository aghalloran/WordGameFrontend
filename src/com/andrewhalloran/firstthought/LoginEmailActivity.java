package com.andrewhalloran.firstthought;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

public class LoginEmailActivity extends Activity {

	public static final int LOGIN_EMAIL = 1;	
	
	public final static String EMAIL_ADDRESS = "com.andrewhalloran.firstthought.EMAIL_ADDRESS";	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_email);
    }

    public void goBack(View view){
    	setResult(RESULT_CANCELED);
    	finish();
    }
    
    public void ok(View view){
    	Intent intent = new Intent(this, LoginEmailUsernameActivity.class);
    	EditText editText = (EditText) findViewById(R.id.email_address);
    	String email = editText.getText().toString();
    	intent.putExtra(EMAIL_ADDRESS, email);
    	startActivityForResult(intent, LOGIN_EMAIL);    	
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
    	if (requestCode == LOGIN_EMAIL) {
			if(resultCode == RESULT_OK){
				setResult(RESULT_OK);
				finish();
			}
		}
    }   
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_login_email, menu);
        return true;
    }
}
