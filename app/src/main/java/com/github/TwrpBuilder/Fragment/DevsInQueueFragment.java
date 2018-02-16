package com.github.TwrpBuilder.Fragment;

import android.app.DownloadManager;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.database.FirebaseListOptions;
import com.github.TwrpBuilder.model.Message;
import com.github.TwrpBuilder.model.Rejected;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import com.github.TwrpBuilder.R;
import com.github.TwrpBuilder.util.Config;
import com.github.TwrpBuilder.util.FirebaseProgressBar;
import com.github.TwrpBuilder.model.User;

/**
 * Created by androidlover5842 on 19/1/18.
 */

public class DevsInQueueFragment extends Fragment {
    private FirebaseListAdapter<User> adapter;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private ListView mListView;
    private Query query;
    private DatabaseReference mUploader;
    private DatabaseReference mReject;
    private FirebaseDatabase mFirebaseInstance;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    public DevsInQueueFragment() {}

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_devs_inqueue, container, false);
        storage = FirebaseStorage.getInstance();
        auth=FirebaseAuth.getInstance();
        storageRef=storage.getReference();
        mListView = view.findViewById(R.id.Lv_devs);
        progressBar= view.findViewById(R.id.pb_builds);
        mFirebaseInstance = FirebaseDatabase.getInstance();
        mUploader = mFirebaseInstance.getReference("RunningBuild");
        query = FirebaseDatabase.getInstance()
                .getReference("InQueue");
        query.keepSynced(true);

        FirebaseListOptions<User> options = new FirebaseListOptions.Builder<User>()
                .setLayout(R.layout.list_developer_stuff)
                .setQuery(query,User.class)
                .build();

        adapter = new FirebaseListAdapter<User>(options) {
            @Override
            protected void populateView(View v, final User model, int position) {
                TextView tvEmail = v.findViewById(R.id.list_user_email);
                TextView tvDevice = v.findViewById(R.id.list_user_device);
                TextView tvBoard = v.findViewById(R.id.list_user_board);
                TextView tvDate= v.findViewById(R.id.list_user_date);
                TextView tvBrand = v.findViewById(R.id.list_user_brand);
                Button btFiles=v.findViewById(R.id.BtFile);
                Button btReject=v.findViewById(R.id.bt_reject);
                final Button btStartBuild=v.findViewById(R.id.bt_start_build);
                tvDate.setText("Date : "+model.getDate());
                tvEmail.setText("Email : "+model.getEmail());
                tvDevice.setText("Model : " + model.getModel());
                tvBoard.setText("Board : "+model.getBoard());
                tvBrand.setText("Brand : " +model.getBrand());

                btFiles.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view1) {
                        storageRef.child("queue/" + model.getBrand() + "/" + model.getBoard() + "/" + model.getModel() + "/"+ Config.TwrpBackFName ).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                DownloadManager downloadManager = (DownloadManager) getContext().getSystemService(getContext().DOWNLOAD_SERVICE);

                                DownloadManager.Request request = new DownloadManager.Request(uri);
                                String fileName=model.getModel()+"-"+model.getBoard()+"-"+model.getEmail()+".tar.gz";
                                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

                                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                Long reference = downloadManager.enqueue(request);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                Toast.makeText(getContext(),"Failed",Toast.LENGTH_SHORT).show();
                            }
                        });


                        Toast.makeText(getContext(),model.getModel(),Toast.LENGTH_SHORT).show();
                    }
                });

                btReject.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        LayoutInflater li = LayoutInflater.from(getContext());
                        View promptsView = li.inflate(R.layout.dialog_edittext, null);

                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                                getContext());

                        // set prompts.xml to alertdialog builder
                        alertDialogBuilder.setView(promptsView);

                        final EditText userInput = (EditText) promptsView
                                .findViewById(R.id.editTextDialogUserInput);

                        // set dialog message
                        alertDialogBuilder
                                .setCancelable(false)
                                .setPositiveButton("Reject",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,int id) {
                                                // get user input and set it to result
                                                // edit text
                                                Rejected rejected=new Rejected(model.getBrand(),model.getBoard(),model.getModel(),model.getEmail(),model.getDate(),userInput.getText().toString(),auth.getCurrentUser().getEmail());
                                                mReject=mFirebaseInstance.getReference();
                                                mReject.child("Rejected").push().setValue(rejected);
                                                Message message=new Message("TwrpBuilder","Build rejected for "+model.getModel());
                                                mReject.child("messages").push().setValue(message);
                                                mFirebaseInstance.getReference("InQueue").orderByChild("model").equalTo(model.getModel()).addListenerForSingleValueEvent(
                                                        new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                                for (DataSnapshot child: dataSnapshot.getChildren()) {
                                                                    child.getRef().removeValue();
                                                                }
                                                            }


                                                            @Override
                                                            public void onCancelled(DatabaseError databaseError) {
                                                                Log.w("TodoApp", "getUser:onCancelled", databaseError.toException());
                                                            }
                                                        });
                                            }
                                        })
                                .setNegativeButton("Cancel",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,int id) {
                                                dialog.cancel();
                                            }
                                        });

                        // create alert dialog
                        AlertDialog alertDialog = alertDialogBuilder.create();

                        // show it
                        alertDialog.show();


                    }
                });

                btStartBuild.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mFirebaseInstance.getReference("InQueue").orderByChild("model").equalTo(model.getModel()).addListenerForSingleValueEvent(
                                new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        for (DataSnapshot child: dataSnapshot.getChildren()) {
                                            child.getRef().removeValue();
                                        }
                                    }


                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        Log.w("TodoApp", "getUser:onCancelled", databaseError.toException());
                                    }
                                });

                        User user = new User(model.getBrand(),model.getBoard(),model.getModel(),model.getCodeName(),model.getEmail(),model.getUid(),model.getFmcToken(),model.getDate());
                        mUploader.push().setValue(user);
                    }
                });


            }
        };

        ProgressBar progressBar= view.findViewById(R.id.pb_builds);
        TextView textView= view.findViewById(R.id.tv_no_build);
        new FirebaseProgressBar().start(progressBar,textView,adapter,"InQueue");

        mListView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    public void onStop() {
        super.onStop();
        adapter.stopListening();
    }
}
