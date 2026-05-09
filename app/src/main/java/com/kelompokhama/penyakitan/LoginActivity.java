package com.example.penyakitan;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;


public class LoginActivity extends AppCompatActivity {

    FirebaseAuth auth = FirebaseAuth.getInstance();

    EditText etUsername, etPassword;
    Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> {

            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            auth.signInWithEmailAndPassword(username, password)
                    .addOnCompleteListener(task -> {

                        if(task.isSuccessful()){

                            Intent i = new Intent(LoginActivity.this, DashboardActivity.class);
                            startActivity(i);
                            finish();

                        }else{

                            Toast.makeText(this,"Login gagal: " + task.getException().getMessage(),Toast.LENGTH_SHORT).show();

                        }

                    });
            }
        );

    }

    //PLACEHOLDER LOGIN
    private boolean checkLoginFirebase(String user,String pass){

        //TODO: Firebase Auth / Firestore

        if(user.equals("a") && pass.equals("1")){
            return true;
        }

        return false;
    }

}