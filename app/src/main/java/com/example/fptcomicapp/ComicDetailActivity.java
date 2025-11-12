package com.example.fptcomicapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.fptcomicapp.model.Chapter;
import com.example.fptcomicapp.model.Comic;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class ComicDetailActivity extends AppCompatActivity {

    public static final String EXTRA_COMIC_ID = "extra_comic_id";

    private MaterialToolbar toolbar;
    private ImageView ivCover;
    private TextView tvTitle;
    private TextView tvAuthor;
    private TextView tvDescription;
    private TextView tvStats;
    private Button btnRead;
    private Button btnChapters;
    private Button btnComments;
    private Button btnManage;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private DocumentReference comicRef;

    private String comicId;
    private Comic currentComic;
    private Chapter latestChapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comic_detail);

        comicId = getIntent().getStringExtra(EXTRA_COMIC_ID);
        if (comicId == null) {
            Toast.makeText(this, R.string.error_missing_comic_id, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        comicRef = db.collection("comics").document(comicId);

        initViews();
        setupListeners();
        loadComic();
        loadLatestChapter();
    }

    private void initViews() {
        ivCover = findViewById(R.id.ivDetailCover);
        tvTitle = findViewById(R.id.tvDetailTitle);
        tvAuthor = findViewById(R.id.tvDetailAuthor);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvStats = findViewById(R.id.tvDetailStats);
        btnRead = findViewById(R.id.btnRead);
        btnChapters = findViewById(R.id.btnChapterList);
        btnComments = findViewById(R.id.btnComments);
        btnManage = findViewById(R.id.btnManageChapters);
        progressBar = findViewById(R.id.progressComicDetail);

        btnManage.setVisibility(isAdminUser() ? View.VISIBLE : View.GONE);

        toolbar = findViewById(R.id.toolbarComicDetail);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void setupListeners() {
        btnRead.setOnClickListener(v -> openReader(latestChapter));
        btnChapters.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChapterListActivity.class);
            intent.putExtra(ChapterListActivity.EXTRA_COMIC_ID, comicId);
            intent.putExtra(ChapterListActivity.EXTRA_COMIC_TITLE, currentComic != null ? currentComic.getTitle() : "");
            startActivity(intent);
        });
        btnComments.setOnClickListener(v -> {
            Intent intent = new Intent(this, CommentActivity.class);
            intent.putExtra(CommentActivity.EXTRA_COMIC_ID, comicId);
            intent.putExtra(CommentActivity.EXTRA_COMIC_TITLE, currentComic != null ? currentComic.getTitle() : "");
            startActivity(intent);
        });
        btnManage.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminChapterActivity.class);
            intent.putExtra(AdminChapterActivity.EXTRA_COMIC_ID, comicId);
            intent.putExtra(AdminChapterActivity.EXTRA_COMIC_TITLE, currentComic != null ? currentComic.getTitle() : "");
            startActivity(intent);
        });
    }

    private void loadComic() {
        progressBar.setVisibility(View.VISIBLE);
        comicRef.get()
                .addOnSuccessListener(snapshot -> {
                    progressBar.setVisibility(View.GONE);
                    if (snapshot.exists()) {
                        currentComic = snapshot.toObject(Comic.class);
                        if (currentComic != null) {
                            currentComic.setId(snapshot.getId());
                            bindComic(currentComic);
                        }
                    } else {
                        Toast.makeText(this, R.string.error_comic_not_found, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadLatestChapter() {
        comicRef.collection("chapters")
                .orderBy("index", Query.Direction.ASCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        DocumentSnapshot doc = snapshot.getDocuments().get(0);
                        latestChapter = doc.toObject(Chapter.class);
                        if (latestChapter != null) {
                            latestChapter.setId(doc.getId());
                        }
                    }
                });
    }

    private void bindComic(Comic comic) {
        if (toolbar != null) {
            toolbar.setTitle(comic.getTitle());
        }
        tvTitle.setText(comic.getTitle());
        tvAuthor.setText(getString(R.string.label_author, comic.getAuthor() != null ? comic.getAuthor() : getString(R.string.label_unknown)));
        tvDescription.setText(comic.getDescription());
        tvStats.setText(getString(R.string.label_stats, comic.getLikes(), comic.getViews(), comic.getChaptersCount()));

        Glide.with(this)
                .load(comic.getCoverUrl())
                .placeholder(R.drawable.home)
                .error(R.drawable.home)
                .into(ivCover);
    }

    private void openReader(@Nullable Chapter chapter) {
        if (chapter == null) {
            Toast.makeText(this, R.string.error_no_chapter_available, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ComicReaderActivity.class);
        intent.putExtra(ComicReaderActivity.EXTRA_COMIC_ID, comicId);
        intent.putExtra(ComicReaderActivity.EXTRA_COMIC_TITLE, currentComic != null ? currentComic.getTitle() : "");
        intent.putExtra(ComicReaderActivity.EXTRA_CHAPTER_ID, chapter.getId());
        intent.putExtra(ComicReaderActivity.EXTRA_CHAPTER_TITLE, chapter.getTitle());
        startActivity(intent);
    }

    private boolean isAdminUser() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return false;
        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        if (email == null) return false;
        return email.endsWith("@admin.com") || email.equalsIgnoreCase("admin@fptcomic.com");
    }
}


