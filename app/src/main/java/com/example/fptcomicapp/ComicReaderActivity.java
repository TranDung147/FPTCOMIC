package com.example.fptcomicapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.fptcomicapp.adapter.PageImageAdapter;
import com.example.fptcomicapp.model.Chapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;

public class ComicReaderActivity extends AppCompatActivity {

    public static final String EXTRA_COMIC_ID = "extra_comic_id";
    public static final String EXTRA_COMIC_TITLE = "extra_comic_title";
    public static final String EXTRA_CHAPTER_ID = "extra_chapter_id";
    public static final String EXTRA_CHAPTER_TITLE = "extra_chapter_title";

    private MaterialToolbar toolbar;
    private ViewPager2 viewPager;
    private ProgressBar progressBar;
    private TextView tvPageIndicator;

    private PageImageAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private String comicId;
    private String chapterId;
    private String chapterTitle;
    private String comicTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comic_reader);

        comicId = getIntent().getStringExtra(EXTRA_COMIC_ID);
        chapterId = getIntent().getStringExtra(EXTRA_CHAPTER_ID);
        comicTitle = getIntent().getStringExtra(EXTRA_COMIC_TITLE);
        chapterTitle = getIntent().getStringExtra(EXTRA_CHAPTER_TITLE);

        if (comicId == null || chapterId == null) {
            Toast.makeText(this, R.string.error_missing_chapter_id, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        initViews();
        setupViewPager();
        loadChapter();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarReader);
        viewPager = findViewById(R.id.viewPagerPages);
        progressBar = findViewById(R.id.progressReader);
        tvPageIndicator = findViewById(R.id.tvPageIndicator);

        String title = chapterTitle != null ? chapterTitle : getString(R.string.title_reader);
        toolbar.setTitle(title);
        toolbar.setSubtitle(comicTitle);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupViewPager() {
        adapter = new PageImageAdapter();
        viewPager.setAdapter(adapter);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updatePageIndicator(position);
            }
        });
    }

    private void loadChapter() {
        progressBar.setVisibility(View.VISIBLE);
        DocumentReference chapterRef = db.collection("comics")
                .document(comicId)
                .collection("chapters")
                .document(chapterId);
        chapterRef.get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, R.string.error_chapter_not_found, Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    Chapter chapter = snapshot.toObject(Chapter.class);
                    if (chapter == null) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, R.string.error_chapter_not_found, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<String> urls = chapter.getPageImageUrls();
                    if (urls != null && !urls.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        adapter.submitList(urls);
                        updatePageIndicator(0);
                    } else if (chapter.getPageStoragePaths() != null && !chapter.getPageStoragePaths().isEmpty()) {
                        fetchDownloadUrls(chapter.getPageStoragePaths());
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, R.string.error_chapter_no_pages, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchDownloadUrls(@NonNull List<String> storagePaths) {
        List<Task<String>> urlTasks = new ArrayList<>();
        for (String path : storagePaths) {
            StorageReference ref = storage.getReference().child(path);
            Task<String> task = ref.getDownloadUrl().continueWith(downloadTask -> downloadTask.getResult().toString());
            urlTasks.add(task);
        }
        Tasks.whenAllComplete(urlTasks)
                .addOnSuccessListener(tasks -> {
                    List<String> urls = new ArrayList<>();
                    for (Task<?> task : tasks) {
                        if (task.isSuccessful() && task.getResult() instanceof String) {
                            urls.add((String) task.getResult());
                        }
                    }
                    progressBar.setVisibility(View.GONE);
                    if (urls.isEmpty()) {
                        Toast.makeText(this, R.string.error_chapter_no_pages, Toast.LENGTH_SHORT).show();
                    } else {
                        adapter.submitList(urls);
                        updatePageIndicator(0);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updatePageIndicator(int position) {
        int total = adapter.getItemCount();
        if (total == 0) {
            tvPageIndicator.setVisibility(View.GONE);
        } else {
            tvPageIndicator.setVisibility(View.VISIBLE);
            tvPageIndicator.setText(getString(R.string.label_page_indicator, position + 1, total));
        }
    }
}


