package com.example.fptcomicapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import com.google.android.material.tabs.TabLayout;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
        // Chỉ định bucket name rõ ràng từ google-services.json
        storage = FirebaseStorage.getInstance("gs://fptcomic.firebasestorage.app");

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
        
        // Tab và layout switching
        TabLayout tabImageSource = dialogView.findViewById(R.id.tabImageSource);
        View layoutFileSelection = dialogView.findViewById(R.id.layoutFileSelection);
        View layoutUrlInput = dialogView.findViewById(R.id.layoutUrlInput);
        View layoutWebScrape = dialogView.findViewById(R.id.layoutWebScrape);
        
        // File selection views
        TextView tvSelectedImages = dialogView.findViewById(R.id.tvSelectedImages);
        Button btnPickImages = dialogView.findViewById(R.id.btnPickImages);
        Button btnClearImages = dialogView.findViewById(R.id.btnClearImages);
        
        // URL input views
        TextInputLayout layoutImageUrls = dialogView.findViewById(R.id.layoutImageUrls);
        TextInputEditText edtImageUrls = dialogView.findViewById(R.id.edtImageUrls);
        TextView tvUrlImagesStatus = dialogView.findViewById(R.id.tvUrlImagesStatus);
        
        // Web scraping views
        TextInputLayout layoutChapterUrl = dialogView.findViewById(R.id.layoutChapterUrl);
        TextInputEditText edtChapterUrl = dialogView.findViewById(R.id.edtChapterUrl);
        TextView tvScrapeStatus = dialogView.findViewById(R.id.tvScrapeStatus);
        Button btnScrapeImages = dialogView.findViewById(R.id.btnScrapeImages);

        List<Uri> selectedImages = new ArrayList<>();
        List<String> imageUrls = new ArrayList<>();
        List<String> scrapedImageUrls = new ArrayList<>();
        
        if (chapter != null) {
            edtTitle.setText(chapter.getTitle());
            edtIndex.setText(String.valueOf(chapter.getIndex()));
            if (chapter.getPageImageUrls() != null) {
                tvSelectedImages.setText(getString(R.string.label_selected_images_existing, chapter.getPageImageUrls().size()));
            }
        }

        // Tab switching logic
        // Ẩn tab "Chọn ảnh" vì không hỗ trợ upload từ thiết bị
        if (tabImageSource.getTabCount() > 0) {
            tabImageSource.removeTabAt(0); // Xóa tab "Chọn ảnh"
        }
        
        tabImageSource.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                // Tab 0 giờ là "Nhập link", Tab 1 là "Lấy từ web"
                layoutFileSelection.setVisibility(View.GONE); // Luôn ẩn
                layoutUrlInput.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
                layoutWebScrape.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // File selection handlers
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

        // URL input handler - update status when text changes
        edtImageUrls.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString().trim();
                if (TextUtils.isEmpty(text)) {
                    imageUrls.clear();
                    tvUrlImagesStatus.setText(R.string.label_url_images_none);
                } else {
                    String[] urls = text.split("\n");
                    imageUrls.clear();
                    for (String url : urls) {
                        String trimmed = url.trim();
                        if (!TextUtils.isEmpty(trimmed)) {
                            imageUrls.add(trimmed);
                        }
                    }
                    tvUrlImagesStatus.setText(getString(R.string.label_url_images_count, imageUrls.size()));
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // Web scraping handler
        btnScrapeImages.setOnClickListener(v -> {
            String chapterUrl = edtChapterUrl.getText() != null ? edtChapterUrl.getText().toString().trim() : "";
            if (TextUtils.isEmpty(chapterUrl)) {
                layoutChapterUrl.setError(getString(R.string.error_field_required));
                return;
            }
            if (!isValidUrl(chapterUrl)) {
                layoutChapterUrl.setError(getString(R.string.error_invalid_url, chapterUrl));
                return;
            }
            
            layoutChapterUrl.setError(null);
            tvScrapeStatus.setText(R.string.label_scraping);
            btnScrapeImages.setEnabled(false);
            
            scrapeImagesFromUrl(chapterUrl, (success, urls) -> {
                btnScrapeImages.setEnabled(true);
                if (success && urls != null && !urls.isEmpty()) {
                    scrapedImageUrls.clear();
                    scrapedImageUrls.addAll(urls);
                    tvScrapeStatus.setText(getString(R.string.label_scraped_images, urls.size()));
                } else {
                    tvScrapeStatus.setText(R.string.error_no_images_found);
                    Toast.makeText(this, R.string.error_scrape_failed, Toast.LENGTH_SHORT).show();
                }
            });
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

                // Check which tab is active
                // Tab 0 = "Nhập link", Tab 1 = "Lấy từ web" (đã xóa tab "Chọn ảnh")
                int tabPosition = tabImageSource.getSelectedTabPosition();
                
                if (chapter == null) {
                    if (tabPosition == 0 && imageUrls.isEmpty()) {
                        Toast.makeText(this, R.string.error_select_images, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (tabPosition == 1 && scrapedImageUrls.isEmpty()) {
                        Toast.makeText(this, R.string.error_select_images, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                dialog.dismiss();
                imageSelectionCallback = null;
                
                // Chỉ hỗ trợ lưu URL, không upload lên Storage
                List<String> finalImageUrls;
                if (tabPosition == 0) {
                    // Tab "Nhập link" - lưu trực tiếp URL
                    finalImageUrls = imageUrls;
                } else {
                    // Tab "Lấy từ web" - lưu trực tiếp URL đã scrape
                    finalImageUrls = scrapedImageUrls;
                }
                
                if (chapter == null) {
                    createChapterWithUrls(title, indexValue, finalImageUrls);
                } else {
                    updateChapterWithUrls(chapter, title, indexValue, finalImageUrls);
                }
            });
        });

        dialog.show();
    }

    // Tạo chapter mới với URLs (không upload lên Storage)
    private void createChapterWithUrls(String title, long index, List<String> imageUrls) {
        progressBar.setVisibility(View.VISIBLE);
        DocumentReference chapterRef = db.collection("comics")
                .document(comicId)
                .collection("chapters")
                .document();

        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("index", index);
        data.put("createdAt", System.currentTimeMillis());
        data.put("pageImageUrls", imageUrls);
        // Không cần pageStoragePaths nữa vì không dùng Storage

        chapterRef.set(data)
                .addOnSuccessListener(unused -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.message_chapter_created, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Cập nhật chapter với URLs (không upload lên Storage)
    private void updateChapterWithUrls(Chapter chapter, String title, long index, List<String> newImageUrls) {
        progressBar.setVisibility(View.VISIBLE);
        DocumentReference chapterRef = db.collection("comics")
                .document(comicId)
                .collection("chapters")
                .document(chapter.getId());

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("index", index);
        
        // Chỉ cập nhật URLs nếu có URLs mới
        if (newImageUrls != null && !newImageUrls.isEmpty()) {
            updates.put("pageImageUrls", newImageUrls);
        }

        chapterRef.update(updates)
                .addOnSuccessListener(unused -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.message_chapter_updated, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void launchImagePicker() {
        // Sử dụng ACTION_GET_CONTENT để hỗ trợ nhiều nguồn hơn (Gallery, Files, Downloads, etc.)
        // Điều này cho phép chọn ảnh từ bất kỳ đâu trên thiết bị, bao gồm ảnh đã chuyển từ PC
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        // Tạo chooser với nhiều tùy chọn
        Intent chooser = Intent.createChooser(intent, getString(R.string.title_select_images));
        
        // Thêm tùy chọn mở Documents (file manager) để dễ truy cập ảnh từ PC
        Intent documentsIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        documentsIntent.addCategory(Intent.CATEGORY_OPENABLE);
        documentsIntent.setType("image/*");
        documentsIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        documentsIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{documentsIntent});
        
        imagePickerLauncher.launch(chooser);
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

    // Method này đã bị xóa vì không còn cần download ảnh
    // Giờ chỉ lưu URL trực tiếp vào Firestore

    private boolean isValidUrl(String url) {
        if (TextUtils.isEmpty(url)) return false;
        try {
            new URL(url);
            return url.startsWith("http://") || url.startsWith("https://");
        } catch (Exception e) {
            return false;
        }
    }

    private interface ScrapeCallback {
        void onComplete(boolean success, List<String> imageUrls);
    }

    private void scrapeImagesFromUrl(String chapterUrl, ScrapeCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        
        executor.execute(() -> {
            try {
                // Kết nối đến trang web với User-Agent để tránh bị chặn
                Document doc = Jsoup.connect(chapterUrl)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .timeout(15000)
                        .followRedirects(true)
                        .get();

                List<String> imageUrls = new ArrayList<>();
                
                // Thử nhiều selector phổ biến để tìm ảnh chapter
                // 1. Tìm tất cả img trong container chapter
                Elements chapterImages = doc.select("div.chapter-content img, " +
                        "div.reading-content img, " +
                        "div.chapter-body img, " +
                        "div.chapter-img img, " +
                        "div.page-chapter img, " +
                        "div.manga-reader img, " +
                        "div.viewer img, " +
                        "img.chapter-img, " +
                        "img.reading-img, " +
                        "img.page-img");
                
                // 2. Nếu không tìm thấy, thử tìm tất cả img có src chứa từ khóa
                if (chapterImages.isEmpty()) {
                    chapterImages = doc.select("img[src*='chapter'], " +
                            "img[src*='page'], " +
                            "img[src*='manga'], " +
                            "img[src*='comic']");
                }
                
                // 3. Nếu vẫn không có, lấy tất cả img (fallback)
                if (chapterImages.isEmpty()) {
                    chapterImages = doc.select("img");
                }

                // Lấy URL ảnh và convert relative URL thành absolute URL
                for (Element img : chapterImages) {
                    String src = img.attr("src");
                    if (src.isEmpty()) {
                        src = img.attr("data-src"); // Một số site dùng data-src
                    }
                    if (src.isEmpty()) {
                        src = img.attr("data-lazy-src"); // Lazy loading
                    }
                    
                    if (!src.isEmpty()) {
                        // Convert relative URL to absolute URL
                        String absoluteUrl = img.absUrl("src");
                        if (absoluteUrl.isEmpty()) {
                            absoluteUrl = img.absUrl("data-src");
                        }
                        if (absoluteUrl.isEmpty()) {
                            absoluteUrl = img.absUrl("data-lazy-src");
                        }
                        
                        // Nếu vẫn là relative, tự build absolute URL
                        if (absoluteUrl.isEmpty() && !src.startsWith("http")) {
                            try {
                                URL baseUrl = new URL(chapterUrl);
                                if (src.startsWith("//")) {
                                    absoluteUrl = baseUrl.getProtocol() + ":" + src;
                                } else if (src.startsWith("/")) {
                                    absoluteUrl = baseUrl.getProtocol() + "://" + baseUrl.getHost() + src;
                                } else {
                                    absoluteUrl = new URL(baseUrl, src).toString();
                                }
                            } catch (Exception e) {
                                continue; // Skip invalid URLs
                            }
                        } else if (!absoluteUrl.isEmpty()) {
                            // Use absolute URL
                        } else {
                            continue; // Skip if still empty
                        }
                        
                        // Chỉ lấy ảnh hợp lệ (jpg, jpeg, png, gif, webp)
                        String lowerUrl = absoluteUrl.toLowerCase();
                        if (lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg") || 
                            lowerUrl.contains(".png") || lowerUrl.contains(".gif") || 
                            lowerUrl.contains(".webp") || lowerUrl.contains("image")) {
                            imageUrls.add(absoluteUrl);
                        }
                    }
                }

                // Remove duplicates
                List<String> uniqueUrls = new ArrayList<>();
                for (String url : imageUrls) {
                    if (!uniqueUrls.contains(url)) {
                        uniqueUrls.add(url);
                    }
                }

                final List<String> finalUrls = uniqueUrls;
                mainHandler.post(() -> callback.onComplete(!finalUrls.isEmpty(), finalUrls));

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> callback.onComplete(false, null));
            }
        });
    }

    private void uploadImages(String chapterId, List<Uri> imageUris, UploadCallback callback) {
        List<Task<Uri>> uploadTasks = new ArrayList<>();
        List<String> storagePaths = new ArrayList<>();

        for (int i = 0; i < imageUris.size(); i++) {
            Uri uri = imageUris.get(i);
            String storagePath = "comics/" + comicId + "/chapters/" + chapterId + "/page_" + (i + 1) + "_" + System.currentTimeMillis() + ".jpg";
            StorageReference ref = storage.getReference().child(storagePath);
            
            // Xử lý cả file:// và content:// URIs
            Task<Uri> uploadTask = null;
            if (uri.getScheme() != null && uri.getScheme().equals("file")) {
                // File URI - đọc file và upload dưới dạng byte array
                try {
                    File file = new File(uri.getPath());
                    if (!file.exists() || !file.canRead()) {
                        android.util.Log.w("UploadImage", "File not found or not readable: " + uri.getPath());
                        continue; // Skip nếu file không tồn tại hoặc không đọc được
                    }
                    
                    // Đọc file thành byte array
                    InputStream fileInputStream = new java.io.FileInputStream(file);
                    byte[] fileBytes = new byte[(int) file.length()];
                    fileInputStream.read(fileBytes);
                    fileInputStream.close();
                    
                    uploadTask = ref.putBytes(fileBytes).continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        return ref.getDownloadUrl();
                    });
                } catch (Exception e) {
                    android.util.Log.w("UploadImage", "Error reading file, trying InputStream: " + e.getMessage());
                    // Fallback: thử upload trực tiếp bằng InputStream
                    try {
                        File file = new File(uri.getPath());
                        if (file.exists() && file.canRead()) {
                            InputStream inputStream = new java.io.FileInputStream(file);
                            uploadTask = ref.putStream(inputStream).continueWithTask(task -> {
                                if (!task.isSuccessful()) {
                                    throw task.getException();
                                }
                                return ref.getDownloadUrl();
                            });
                        } else {
                            continue; // Skip file này
                        }
                    } catch (Exception ex) {
                        android.util.Log.e("UploadImage", "Cannot upload file: " + ex.getMessage());
                        continue; // Skip file này nếu không đọc được
                    }
                }
            } else {
                // Content URI hoặc HTTP URI - upload trực tiếp
                uploadTask = ref.putFile(uri).continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                });
            }
            
            // Chỉ thêm vào list nếu uploadTask được tạo thành công
            if (uploadTask != null) {
                uploadTasks.add(uploadTask);
                storagePaths.add(storagePath);
            }
        }

        // Đợi tất cả task hoàn thành (thành công hoặc thất bại)
        Tasks.whenAllComplete(uploadTasks)
                .addOnSuccessListener(completed -> {
                    List<String> downloadUrls = new ArrayList<>();
                    List<String> successfulPaths = new ArrayList<>();
                    int failedCount = 0;
                    
                    for (int i = 0; i < uploadTasks.size(); i++) {
                        Task<Uri> task = uploadTasks.get(i);
                        if (task.isSuccessful() && task.getResult() != null) {
                            downloadUrls.add(task.getResult().toString());
                            successfulPaths.add(storagePaths.get(i));
                        } else {
                            failedCount++;
                            // Log lỗi để debug
                            if (task.getException() != null) {
                                android.util.Log.e("UploadImage", "Failed to upload image " + (i + 1) + ": " + task.getException().getMessage());
                            }
                        }
                    }
                    
                    // Cho phép upload một phần - chỉ cần có ít nhất 1 ảnh thành công
                    if (downloadUrls.isEmpty()) {
                        callback.onFailure(new IllegalStateException("Không thể upload bất kỳ ảnh nào. Vui lòng kiểm tra kết nối mạng và thử lại."));
                    } else {
                        // Cảnh báo nếu có ảnh thất bại
                        if (failedCount > 0) {
                            Toast.makeText(this, 
                                "Đã upload " + downloadUrls.size() + "/" + uploadTasks.size() + " ảnh. " + 
                                failedCount + " ảnh không thể upload.", 
                                Toast.LENGTH_LONG).show();
                        }
                        callback.onSuccess(successfulPaths, downloadUrls);
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("UploadImage", "Upload failed: " + e.getMessage());
                    callback.onFailure(e);
                });
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


