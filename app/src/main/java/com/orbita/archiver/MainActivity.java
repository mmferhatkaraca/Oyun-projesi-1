package com.orbita.archiver;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipInputStream;

public class MainActivity extends AppCompatActivity {

    private TextView txtLog;
    private static final int PICK_FILE_TO_ZIP = 1;
    private static final int PICK_FILE_TO_UNZIP = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtLog = findViewById(R.id.txtLog);
        MaterialCardView cardCompress = findViewById(R.id.cardCompress);
        MaterialCardView cardExtract = findViewById(R.id.cardExtract);

        cardCompress.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, PICK_FILE_TO_ZIP);
        });

        cardExtract.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, PICK_FILE_TO_UNZIP);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) return;

            if (requestCode == PICK_FILE_TO_ZIP) {
                zipSelectedFile(uri);
            } else if (requestCode == PICK_FILE_TO_UNZIP) {
                extractArchive(uri);
            }
        }
    }

    private void zipSelectedFile(Uri uri) {
        new Thread(() -> {
            try {
                String fileName = getFileName(uri);
                File outDir = getExternalFilesDir(null);
                File zipFile = new File(outDir, fileName + ".zip");

                log("> Sıkıştırma başlatıldı: " + fileName);

                InputStream is = getContentResolver().openInputStream(uri);
                FileOutputStream fos = new FileOutputStream(zipFile);
                ZipOutputStream zos = new ZipOutputStream(fos);

                ZipEntry entry = new ZipEntry(fileName);
                zos.putNextEntry(entry);

                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }

                zos.closeEntry();
                zos.close();
                is.close();
                fos.close();

                log("> BAŞARILI: ZIP Oluşturuldu!\n> Kayıt Yeri: " + zipFile.getAbsolutePath());
            } catch (Exception e) {
                log("> HATA: " + e.getMessage());
            }
        }).start();
    }

    private void extractArchive(Uri uri) {
        new Thread(() -> {
            try {
                String originalName = getFileName(uri);
                log("> Arşiv analiz ediliyor: " + originalName);
                
                File outDir = new File(getExternalFilesDir(null), "Extracted_" + System.currentTimeMillis());
                outDir.mkdirs();
                
                log("> ZIP motoru devrede...");
                try (InputStream is = getContentResolver().openInputStream(uri);
                     ZipInputStream zis = new ZipInputStream(is)) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        if (entry.isDirectory()) continue;
                        File outFile = new File(outDir, entry.getName());
                        outFile.getParentFile().mkdirs();
                        try (FileOutputStream fos = new FileOutputStream(outFile)) {
                            byte[] buffer = new byte[8192];
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                        log("  -> " + entry.getName());
                    }
                }
                log("> ZIP Arşivi başarıyla çıkarıldı!\n> Klasör: " + outDir.getAbsolutePath());
                
            } catch(Exception e) {
                log("> HATA: Lütfen geçerli bir ZIP arşivi seçin. " + e.getMessage());
            }
        }).start();
    }

    private String getFileName(Uri uri) {
        String result = "archive_" + System.currentTimeMillis();
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if(index != -1) {
                result = cursor.getString(index);
            }
            cursor.close();
        }
        return result;
    }

    private void log(String message) {
        runOnUiThread(() -> {
            txtLog.append("\n" + message);
        });
    }
}
