package com.example.fptcomicapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fptcomicapp.adapter.ChapterAdapter;
import com.example.fptcomicapp.model.Chapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ChapterListActivity extends AppCompatActivity implements ChapterAdapter.OnChapterClickListener {

    public static final String EXTRA_COMIC_ID = "extra_comic_id";
    public static final String EXTRA_COMIC_TITLE = "extra_comic_title";

    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyState;

    private ChapterAdapter adapter;
    private FirebaseFirestore db;
    private String comicId;
    private String comicTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter_list);

        comicId = getIntent().getStringExtra(EXTRA_COMIC_ID);
        comicTitle = getIntent().getStringExtra(EXTRA_COMIC_TITLE);

        if (comicId == null) {
            Toast.makeText(this, R.string.error_missing_comic_id, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        initViews();
        setupRecyclerView();
        loadChapters();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarChapterList);
        recyclerView = findViewById(R.id.recyclerChapters);
        progressBar = findViewById(R.id.progressChapters);
        emptyState = findViewById(R.id.tvChapterEmpty);

        toolbar.setTitle(comicTitle != null ? comicTitle : getString(R.string.title_chapter_list));
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new ChapterAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }

    private void loadChapters() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("comics")
                .document(comicId)
                .collection("chapters")
                .orderBy("index", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    progressBar.setVisibility(View.GONE);
                    List<Chapter> chapters = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Chapter chapter = doc.toObject(Chapter.class);
                        if (chapter != null) {
                            chapter.setId(doc.getId());
                            chapters.add(chapter);
                        }
                    }
                    adapter.submitList(chapters);
                    emptyState.setVisibility(chapters.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onChapterClick(Chapter chapter) {
        Intent intent = new Intent(this, ComicReaderActivity.class);
        intent.putExtra(ComicReaderActivity.EXTRA_COMIC_ID, comicId);
        intent.putExtra(ComicReaderActivity.EXTRA_COMIC_TITLE, comicTitle);
        intent.putExtra(ComicReaderActivity.EXTRA_CHAPTER_ID, chapter.getId());
        intent.putExtra(ComicReaderActivity.EXTRA_CHAPTER_TITLE, chapter.getTitle());
        startActivity(intent);
    }

    @Override
    public void onChapterLongClick(View view, Chapter chapter) {
        // no-op for regular users
    }
}


