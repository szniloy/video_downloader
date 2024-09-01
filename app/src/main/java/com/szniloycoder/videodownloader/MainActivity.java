package com.szniloycoder.videodownloader;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.szniloycoder.videodownloader.databinding.ActivityMainBinding;
import com.yausername.youtubedl_android.DownloadProgressCallback;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    ProgressDialog dialog;


    private final CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final DownloadProgressCallback callback = (v, l, s) -> {
    };


    // Use the new ActivityResultLauncher for permission requests
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permission -> {
                boolean allGranted = true;

                for (Boolean isGranted : permission.values()) {
                    if (!isGranted) {
                        allGranted = false;
                        break;
                    }
                }

                if (allGranted) {
                    // All permissions are granted, start download
                    startDownload();
                } else {
                    // Not all permissions are granted
                    Toast.makeText(MainActivity.this, "Permissions denied. Please enable all required permissions.", Toast.LENGTH_SHORT).show();
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //hide statusBar:
        Window w = getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);



        dialog = new ProgressDialog(MainActivity.this);
        dialog.setMessage("Downloading...");

        // Set the dialog to be non-cancelable and prevent dismiss on touch outside
        dialog.setCancelable(false); // Back button won't dismiss the dialog
        dialog.setCanceledOnTouchOutside(false); // Touching outside won't dismiss the dialog


        // Create the downloads directory
        File dir = new File(Environment.getExternalStoragePublicDirectory
                (Environment.DIRECTORY_DOWNLOADS), "Video Downloader");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Set click listener for the download button
        binding.download.setOnClickListener(view -> {
            if (requireNonNull(binding.urlET.getText()).toString().isEmpty()) {
                binding.urlLayout.setError("Enter valid url");
            } else {
                // Check permissions before starting download
                checkPermissionsAndDownload(binding.urlET.getText().toString(), dir);
            }
        });
    }

    // Function to check permissions and start download
    @SuppressLint("ObsoleteSdkInt")
    private void checkPermissionsAndDownload(String url, File dir) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String[] permissions = new String[]{
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.READ_MEDIA_AUDIO,
                    android.Manifest.permission.READ_MEDIA_VIDEO,
            };

            List<String> permissionsToRequest = new ArrayList<>();
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }

            if (permissionsToRequest.isEmpty()) {
                // All permissions are already granted
                startDownload();
            } else {
                String[] permissionsArray = permissionsToRequest.toArray(new String[0]);
                boolean shouldShowRationale = false;

                for (String permission : permissionsArray) {
                    if (shouldShowRequestPermissionRationale(permission)) {
                        shouldShowRationale = true;
                        break;
                    }
                }

                if (shouldShowRationale) {
                    new AlertDialog.Builder(this)
                            .setMessage("Please allow all permissions")
                            .setCancelable(false)
                            .setPositiveButton("YES", (dialogInterface, i) -> requestPermissionLauncher.launch(permissionsArray))
                            .setNegativeButton("NO", (dialogInterface, i) -> dialogInterface.dismiss())
                            .show();
                } else {
                    requestPermissionLauncher.launch(permissionsArray);
                }
            }

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = new String[]{
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
            };

            List<String> permissionsToRequest = new ArrayList<>();
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }

            if (permissionsToRequest.isEmpty()) {
                // All permissions are already granted
                startDownload();
            } else {
                String[] permissionsArray = permissionsToRequest.toArray(new String[0]);
                boolean shouldShowRationale = false;

                for (String permission : permissionsArray) {
                    if (shouldShowRequestPermissionRationale(permission)) {
                        shouldShowRationale = true;
                        break;
                    }
                }

                if (shouldShowRationale) {
                    new AlertDialog.Builder(this)
                            .setMessage("Please allow all permissions")
                            .setCancelable(false)
                            .setPositiveButton("YES", (dialogInterface, i) -> requestPermissionLauncher.launch(permissionsArray))
                            .setNegativeButton("NO", (dialogInterface, i) -> dialogInterface.dismiss())
                            .show();
                } else {
                    requestPermissionLauncher.launch(permissionsArray);
                }
            }
        }
    }



    // Function to start video download
    private void startDownload() {
        String url = requireNonNull(binding.urlET.getText()).toString();
        File dir = new File(Environment.getExternalStoragePublicDirectory
                (Environment.DIRECTORY_DOWNLOADS), "Video Downloader");

        if (!dir.exists()) {
            dir.mkdirs();
        }

        downloadVideo(url, dir);
    }


    // Function to handle video download
    private void downloadVideo(String url, File dir) {
        dialog.show();
        YoutubeDLRequest request = new YoutubeDLRequest(url);
        request.addOption("--no-mtime");
        request.addOption("-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best");
        request.addOption("-o", dir.getAbsolutePath() + "/%(title)s.%(ext)s");

        Disposable disposable = Observable.fromCallable(() -> YoutubeDL.getInstance().execute(request, String.valueOf(callback)))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(youtubeDLResponse -> {
                    // On successful download
                    dialog.dismiss();
                    Toast.makeText(MainActivity.this, "Downloaded Successfully!", Toast.LENGTH_SHORT).show();
                    binding.urlET.setText(""); // Clear the URL input field
                }, e -> {
                    // On download failure
                    dialog.dismiss();
                    Toast.makeText(MainActivity.this, "Download Failed " + e.getCause(), Toast.LENGTH_SHORT).show();
                    binding.urlET.setText(""); // Clear the URL input field
                });
        compositeDisposable.add(disposable);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.dispose();
    }
}