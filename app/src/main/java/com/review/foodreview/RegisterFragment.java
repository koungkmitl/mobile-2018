package com.review.foodreview;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterFragment extends Fragment {
    private static final String TAG = "REGISTER";
    private EditText _email, _password, _username;
    private Button _submitBtn, _loginBtn;
    private ProgressBar _loading;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        auth = FirebaseAuth.getInstance();
        return inflater.inflate(R.layout.register, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "RegisterFragment: onActivityCreated");
        MainActivity.onFragmentChanged(TAG);
        firestore = FirebaseFirestore.getInstance();
        registerFragmentElements();
        initSubmitBtn();
        initLoginBtn();
    }

    private void registerFragmentElements() {
        Log.d(TAG, "RegisterFragment: registerFragmentElements");
        _email = getView().findViewById(R.id.login_input_email);
        _password = getView().findViewById(R.id.login_input_password);
        _username = getView().findViewById(R.id.register_input_username);
        _submitBtn = getView().findViewById(R.id.register_btn);
        _loginBtn = getView().findViewById(R.id.register_btn_login);
        _loading = getView().findViewById(R.id.register_loading);
    }

    private void initSubmitBtn() {
        _submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: clicked");
                final String email = _email.getText().toString();
                final String password = _password.getText().toString();
                final String username = _username.getText().toString();
                if (email.isEmpty() || password.isEmpty() || username.isEmpty() || password.length() < 6) {
                    Toast.makeText(
                            getActivity(),
                            "Fill in all the fields. Make sure the password is longer than 6 characters",
                            Toast.LENGTH_SHORT)
                            .show();
                } else {
                    hideButtons(true);
                    auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        Log.d(TAG, "createUserWithEmail: success");
                                        final FirebaseUser currentUser = auth.getCurrentUser();

                                        Log.d(TAG, "sending verification email");
                                        currentUser.sendEmailVerification();

                                        final UserProfileChangeRequest changeRequest = new UserProfileChangeRequest.Builder()
                                                .setDisplayName(username)
                                                .build();

                                        Log.d(TAG, "submitting profile change request");
                                        currentUser.updateProfile(changeRequest)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        List< DocumentReference> bookmarkList = new ArrayList<>();
                                                        if (task.isSuccessful()) {
                                                            Log.d(TAG, "submitted profile change request");
                                                            Map<String, Object> u = new HashMap<>();
                                                            u.put("displayName", username);
                                                            u.put("email", email);

                                                            Log.d(TAG, "saving user " + currentUser.getUid() + " to database");
                                                            firestore.collection("user")
                                                                    .document(currentUser.getUid())
                                                                    .set(u)
                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                            if (task.isSuccessful()) {
                                                                                Log.d(TAG, "saved user to database");
                                                                                auth.signOut();
                                                                                displaySuccessDialog();
                                                                            } else {
                                                                                Log.d(TAG, "save to db failed: " + task.getException().getMessage());
                                                                                displayFailureDialog(task.getException().getLocalizedMessage());
                                                                            }
                                                                            hideButtons(false);
                                                                        }
                                                                    });
                                                        } else {
                                                            Log.d(TAG, "update profile failed: " + task.getException().getMessage());
                                                            displayFailureDialog(task.getException().getLocalizedMessage());
                                                            hideButtons(false);
                                                        }
                                                    }
                                                });
                                    } else {
                                        Log.w(TAG, "createUserWithEmail: failure", task.getException());
                                        displayFailureDialog(task.getException().getLocalizedMessage());
                                        hideButtons(false);
                                    }
                                }
                            });
                }
            }
        });
    }

    private void initLoginBtn() {
        _loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.main_view, new LoginFragment())
                        .commit();
            }
        });
    }

    private void displaySuccessDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), android.R.style.Theme_Material_Light_Dialog_Alert);
        builder.setTitle("Check your email")
                .setMessage("Successfully registered. Please click the link we sent to your email to verify your account.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getActivity()
                                .getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.main_view, new DiscoverFragment())
                                .commit();
                    }
                })
                .show();
    }

    private void displayFailureDialog(String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), android.R.style.Theme_Material_Light_Dialog_Alert);
        builder.setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {}
                })
                .show();
    }

    private void hideButtons(boolean hide) {
        if (hide == true) {
            _loading.setVisibility(View.VISIBLE);
            _submitBtn.setVisibility(View.INVISIBLE);
            _loginBtn.setVisibility(View.INVISIBLE);
        } else {
            _loading.setVisibility(View.GONE);
            _submitBtn.setVisibility(View.VISIBLE);
            _loginBtn.setVisibility(View.VISIBLE);
        }
    }
}
