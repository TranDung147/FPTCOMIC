package com.example.fptcomicapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fptcomicapp.adapter.CommentAdapter;
import com.example.fptcomicapp.model.Comment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentActivity extends AppCompatActivity {

    public static final String EXTRA_COMIC_ID = "extra_comic_id";
    public static final String EXTRA_COMIC_TITLE = "extra_comic_title";

    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    private EditText edtComment;
    private Button btnSend;
    private ProgressBar progressBar;

    private CommentAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration commentListener;

    private String comicId;
    private String comicTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        comicId = getIntent().getStringExtra(EXTRA_COMIC_ID);
        comicTitle = getIntent().getStringExtra(EXTRA_COMIC_TITLE);

        if (comicId == null) {
            Toast.makeText(this, R.string.error_missing_comic_id, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        setupRecyclerView();
        bindListeners();
        loadComments();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarComments);
        recyclerView = findViewById(R.id.recyclerComments);
        edtComment = findViewById(R.id.edtComment);
        btnSend = findViewById(R.id.btnSendComment);
        progressBar = findViewById(R.id.progressComments);

        toolbar.setTitle(getString(R.string.title_comments));
        toolbar.setSubtitle(comicTitle);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new CommentAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void bindListeners() {
        btnSend.setOnClickListener(v -> {
            String content = edtComment.getText().toString().trim();
            if (TextUtils.isEmpty(content)) {
                edtComment.setError(getString(R.string.error_comment_empty));
                return;
            }
            if (auth.getCurrentUser() == null) {
                Toast.makeText(this, R.string.error_comment_requires_login, Toast.LENGTH_SHORT).show();
                return;
            }
            createComment(content);
        });
    }

    private void loadComments() {
        progressBar.setVisibility(View.VISIBLE);
        commentListener = db.collection("comics")
                .document(comicId)
                .collection("comments")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    progressBar.setVisibility(View.GONE);
                    if (error != null) {
                        Toast.makeText(this, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshot == null) return;
                    List<Comment> comments = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Comment comment = doc.toObject(Comment.class);
                        if (comment != null) {
                            comment.setId(doc.getId());
                            comments.add(comment);
                        }
                    }
                    adapter.submitList(comments);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (commentListener != null) {
            commentListener.remove();
        }
    }

    private void createComment(String content) {
        btnSend.setEnabled(false);
        DocumentReference commentRef = db.collection("comics")
                .document(comicId)
                .collection("comments")
                .document();

        Map<String, Object> data = new HashMap<>();
        data.put("content", content);
        data.put("createdAt", System.currentTimeMillis());
        data.put("userId", auth.getCurrentUser().getUid());
        data.put("userName", auth.getCurrentUser().getDisplayName() != null
                ? auth.getCurrentUser().getDisplayName()
                : auth.getCurrentUser().getEmail());

        commentRef.set(data)
                .addOnSuccessListener(unused -> {
                    edtComment.setText("");
                    btnSend.setEnabled(true);
                    Toast.makeText(this, R.string.message_comment_posted, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    btnSend.setEnabled(true);
                    Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}


