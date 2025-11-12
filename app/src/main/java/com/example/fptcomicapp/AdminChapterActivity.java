package com.example.fptcomicapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fptcomicapp.adapter.AdminChapterAdapter;
import com.example.fptcomicapp.model.Chapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminChapterActivity extends AppCompatActivity implements AdminChapterAdapter.AdminChapterListener {

    public static final String EXTRA_COMIC_ID = "extra_comic_id";
    public static final String EXTRA_COMIC_TITLE = "extra_comic_title";

    private interface ImageSelectionCallback {
        void onImagesSelected(List<Uri> imageUris);
    }

    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private View emptyState;
    private FloatingActionButton fabAdd;

    private AdminChapterAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private ListenerRegistration chaptersListener;

    private String comicId;
    private String comicTitle;

    private ImageSelectionCallback imageSelectionCallback;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null || imageSelectionCallback == null) {
                    return;
                }
                Intent data = result.getData();
                List<Uri> uris = new ArrayList<>();
                if (data.getClipData() != null) {
                    for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                        Uri uri = data.getClipData().getItemAt(i).getUri();
                        takePersistablePermission(uri);
                        uris.add(uri);
                    }
                } else if (data.getData() != null) {
                    Uri uri = data.getData();
                    takePersistablePermission(uri);
                    uris.add(uri);
                }
                imageSelectionCallback.onImagesSelected(uris);
                imageSelectionCallback = null;
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_chapter);

        comicId = getIntent().getStringExtra(EXTRA_COMIC_ID);
        comicTitle = getIntent().getStringExtra(EXTRA_COMIC_TITLE);

        if (comicId == null) {
            Toast.makeText(this, R.string.error_missing_comic_id, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        initViews();
        setupRecyclerView();
        observeChapters();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chaptersListener != null) {
            chaptersListener.remove();
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarAdminChapter);
        recyclerView = findViewById(R.id.recyclerAdminChapters);
        progressBar = findViewById(R.id.progressAdminChapters);
        emptyState = findViewById(R.id.tvAdminChapterEmpty);
        fabAdd = findViewById(R.id.fabAddChapter);

        toolbar.setTitle(getString(R.string.title_admin_chapter, comicTitle != null ? comicTitle : ""));
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        fabAdd.setOnClickListener(v -> showChapterDialog(null));
    }

    private void setupRecyclerView() {
        adapter = new AdminChapterAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void observeChapters() {
        progressBar.setVisibility(View.VISIBLE);
        chaptersListener = db.collection("comics")
                .document(comicId)
                .collection("chapters")
                .orderBy("index", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    progressBar.setVisibility(View.GONE);
                    if (error != null) {
                        Toast.makeText(this, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshot == null) return;
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
                });
    }

    private void showChapterDialog(@Nullable Chapter chapter) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_chapter_editor, null);
        TextInputLayout layoutTitle = dialogView.findViewById(R.id.layoutChapterTitle);
        TextInputLayout layoutIndex = dialogView.findViewById(R.id.layoutChapterIndex);
        TextInputEditText edtTitle = dialogView.findViewById(R.id.edtChapterTitle);
        TextInputEditText edtIndex = dialogView.findViewById(R.id.edtChapterIndex);
        TextView tvSelectedImages = dialogView.findViewById(R.id.tvSelectedImages);
        Button btnPickImages = dialogView.findViewById(R.id.btnPickImages);
        Button btnClearImages = dialogView.findViewById(R.id.btnClearImages);

        List<Uri> selectedImages = new ArrayList<>();
        if (chapter != null) {
            edtTitle.setText(chapter.getTitle());
            edtIndex.setText(String.valueOf(chapter.getIndex()));
            if (chapter.getPageImageUrls() != null) {
                tvSelectedImages.setText(getString(R.string.label_selected_images_existing, chapter.getPageImageUrls().size()));
            }
        }

        btnClearImages.setOnClickListener(v -> {
            selectedImages.clear();
            tvSelectedImages.setText(R.string.label_selected_images_none);
        });

        btnPickImages.setOnClickListener(v -> {
            imageSelectionCallback = uris -> {
                selectedImages.clear();
                selectedImages.addAll(uris);
                tvSelectedImages.setText(getString(R.string.label_selected_images, selectedImages.size()));
            };
            launchImagePicker();
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(chapter == null ? R.string.title_add_chapter_short : R.string.title_edit_chapter_short)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, (d, which) -> imageSelectionCallback = null)
                .setPositiveButton(chapter == null ? R.string.action_create : R.string.action_update, null)
                .create();

        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                layoutTitle.setError(null);
                layoutIndex.setError(null);
                String title = edtTitle.getText() != null ? edtTitle.getText().toString().trim() : "";
                String indexText = edtIndex.getText() != null ? edtIndex.getText().toString().trim() : "";

                if (TextUtils.isEmpty(title)) {
                    layoutTitle.setError(getString(R.string.error_field_required));
                    return;
                }

                long indexValue;
                try {
                    indexValue = Long.parseLong(indexText);
                } catch (NumberFormatException ex) {
                    layoutIndex.setError(getString(R.string.error_invalid_number));
                    return;
                }

                if (chapter == null && selectedImages.isEmpty()) {
                    Toast.makeText(this, R.string.error_select_images, Toast.LENGTH_SHORT).show();
                    return;
                }

                dialog.dismiss();
                imageSelectionCallback = null;
                if (chapter == null) {
                    createChapter(title, indexValue, selectedImages);
                } else {
                    updateChapter(chapter, title, indexValue, selectedImages);
                }
            });
        });

        dialog.show();
    }

    private void createChapter(String title, long index, List<Uri> imageUris) {
        progressBar.setVisibility(View.VISIBLE);
        DocumentReference chapterRef = db.collection("comics")
                .document(comicId)
                .collection("chapters")
                .document();

        uploadImages(chapterRef.getId(), imageUris, new UploadCallback() {
            @Override
            public void onSuccess(List<String> storagePaths, List<String> downloadUrls) {
                Map<String, Object> data = new HashMap<>();
                data.put("title", title);
                data.put("index", index);
                data.put("createdAt", System.currentTimeMillis());
                data.put("pageStoragePaths", storagePaths);
                data.put("pageImageUrls", downloadUrls);
                chapterRef.set(data)
                        .addOnSuccessListener(unused -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(AdminChapterActivity.this, R.string.message_chapter_created, Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(AdminChapterActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        });
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminChapterActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateChapter(Chapter chapter, String title, long index, List<Uri> newImageUris) {
        progressBar.setVisibility(View.VISIBLE);
        DocumentReference chapterRef = db.collection("comics")
                .document(comicId)
                .collection("chapters")
                .document(chapter.getId());

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("index", index);

        if (newImageUris.isEmpty()) {
            chapterRef.update(updates)
                    .addOnSuccessListener(unused -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, R.string.message_chapter_updated, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    });
            return;
        }

        uploadImages(chapter.getId(), newImageUris, new UploadCallback() {
            @Override
            public void onSuccess(List<String> storagePaths, List<String> downloadUrls) {
                updates.put("pageStoragePaths", storagePaths);
                updates.put("pageImageUrls", downloadUrls);
                chapterRef.update(updates)
                        .addOnSuccessListener(unused -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(AdminChapterActivity.this, R.string.message_chapter_updated, Toast.LENGTH_SHORT).show();
                            deleteStorageFilesQuietly(chapter.getPageStoragePaths());
                        })
                        .addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(AdminChapterActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        });
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminChapterActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void launchImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        imagePickerLauncher.launch(Intent.createChooser(intent, getString(R.string.title_select_images)));
    }

    private void takePersistablePermission(Uri uri) {
        if (uri == null) return;
        final int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION;
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {
            // ignore if not persisted
        }
    }

    private void uploadImages(String chapterId, List<Uri> imageUris, UploadCallback callback) {
        List<Task<?>> tasks = new ArrayList<>();
        List<Task<Uri>> downloadTasks = new ArrayList<>();
        List<String> storagePaths = new ArrayList<>();

        for (int i = 0; i < imageUris.size(); i++) {
            Uri uri = imageUris.get(i);
            String storagePath = "comics/" + comicId + "/chapters/" + chapterId + "/page_" + (i + 1) + "_" + System.currentTimeMillis() + ".jpg";
            StorageReference ref = storage.getReference().child(storagePath);
            Task<Uri> task = ref.putFile(uri)
                    .continueWithTask(uploadTask -> {
                        if (!uploadTask.isSuccessful()) {
                            throw uploadTask.getException();
                        }
                        return ref.getDownloadUrl();
                    });
            tasks.add(task);
            downloadTasks.add(task);
            storagePaths.add(storagePath);
        }

        Tasks.whenAllComplete(tasks)
                .addOnSuccessListener(completed -> {
                    List<String> downloadUrls = new ArrayList<>();
                    for (Task<Uri> task : downloadTasks) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            downloadUrls.add(task.getResult().toString());
                        }
                    }
                    if (downloadUrls.size() != storagePaths.size()) {
                        callback.onFailure(new IllegalStateException("Không thể tải toàn bộ ảnh"));
                    } else {
                        callback.onSuccess(storagePaths, downloadUrls);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    private void deleteStorageFilesQuietly(@Nullable List<String> storagePaths) {
        if (storagePaths == null || storagePaths.isEmpty()) return;
        for (String path : storagePaths) {
            storage.getReference().child(path).delete();
        }
    }

    private void deleteStorageFilesSync(@Nullable List<String> storagePaths) {
        if (storagePaths == null || storagePaths.isEmpty()) return;
        List<Task<Void>> deletes = new ArrayList<>();
        for (String path : storagePaths) {
            deletes.add(storage.getReference().child(path).delete());
        }
        Tasks.whenAllComplete(deletes).addOnCompleteListener(task -> {
            // ignore result
        });
    }

    @Override
    public void onEditChapter(Chapter chapter) {
        showChapterDialog(chapter);
    }

    @Override
    public void onDeleteChapter(Chapter chapter) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_delete_chapter)
                .setMessage(R.string.message_delete_chapter_confirm)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> performDeleteChapter(chapter))
                .show();
    }

    private void performDeleteChapter(Chapter chapter) {
        progressBar.setVisibility(View.VISIBLE);
        DocumentReference ref = db.collection("comics")
                .document(comicId)
                .collection("chapters")
                .document(chapter.getId());
        ref.delete()
                .addOnSuccessListener(unused -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.message_chapter_deleted, Toast.LENGTH_SHORT).show();
                    deleteStorageFilesSync(chapter.getPageStoragePaths());
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onOpenChapter(Chapter chapter) {
        Intent intent = new Intent(this, ComicReaderActivity.class);
        intent.putExtra(ComicReaderActivity.EXTRA_COMIC_ID, comicId);
        intent.putExtra(ComicReaderActivity.EXTRA_COMIC_TITLE, comicTitle);
        intent.putExtra(ComicReaderActivity.EXTRA_CHAPTER_ID, chapter.getId());
        intent.putExtra(ComicReaderActivity.EXTRA_CHAPTER_TITLE, chapter.getTitle());
        startActivity(intent);
    }

    private interface UploadCallback {
        void onSuccess(List<String> storagePaths, List<String> downloadUrls);

        void onFailure(Exception e);
    }
}


