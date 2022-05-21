package com.example.runner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.runner.data.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class Register extends AppCompatActivity {

    private EditText editName;
    private EditText editEmail;
    private EditText editPass;
    private Button registerBtn;
    private FirebaseAuth firebaseAuth;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        editName = findViewById(R.id.edit_text_name_register);
        editEmail = findViewById(R.id.edit_text_email_register);
        editPass = findViewById(R.id.edit_text_pass_register);
        registerBtn = findViewById(R.id.btn_reg_register);
        progressBar = findViewById(R.id.progressBar2);

        progressBar.setVisibility(View.GONE);
        firebaseAuth = FirebaseAuth.getInstance();
        //String uid = firebaseAuth.getCurrentUser().getUid();

        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name=editName.getText().toString().trim();
                String email=editEmail.getText().toString().trim();
                String pass=editPass.getText().toString().trim();

                if(name.isEmpty()){
                    editName.setError("Name is required!");
                    editName.requestFocus();
                    return;
                }

                if(pass.length() < 6){
                    editPass.setError("Password length at least 6 characters!");
                    editPass.requestFocus();
                    return;
                }

                if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                    editEmail.setError("Email is not valid !");
                    editEmail.requestFocus();
                    return;
                }
                progressBar.setVisibility(View.VISIBLE);
                
                firebaseAuth.createUserWithEmailAndPassword(email,pass)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if(task.isSuccessful()) {
                                    // to do - change the ctor later we have 3 now
                                    //User newUser = new User (name,email,pass);

                                    String uid = firebaseAuth.getCurrentUser().getUid();
                                    User newUser = new User (name,email,pass,uid);

                                    FirebaseDatabase.getInstance().getReference("users")
                                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                            .setValue(newUser).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()){
                                                Toast.makeText(Register.this,"Register Complete Successful!",Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(Register.this,MainActivity.class));
                                                progressBar.setVisibility(View.GONE);
                                                finish();
                                            }else
                                                Toast.makeText(Register.this,"Register Failed!",Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }else
                                    Toast.makeText(Register.this,"Error Create User with email!",Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(Register.this,e.getMessage(),Toast.LENGTH_SHORT).show();

                    }
                });

            }
        });


    }


}