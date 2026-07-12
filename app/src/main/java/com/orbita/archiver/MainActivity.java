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
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import com.github.junrar.Junrar;

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
                
                // Güvenli kopyalama (Android SAF kısıtlamalarını aşmak için Cache'e alırız)
                File tempFile = new File(getCacheDir(), "temp_" + System.currentTimeMillis() + "_" + originalName);
                try (InputStream is = getContentResolver().openInputStream(uri);
                     OutputStream os = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                    }
                }
                
                // Çıktı klasörü oluştur
                String folderName = originalName.contains(".") ? originalName.substring(0, originalName.lastIndexOf('.')) : originalName;
                File outDir = new File(getExternalFilesDir(null), "Extracted_" + folderName);
                outDir.mkdirs();
                
                log("> İşlem Başlatıldı. Hedef format tespit ediliyor...");
                String lowerName = originalName.toLowerCase();
                
                // RAR DESTEĞİ
                if (lowerName.endsWith(".rar")) {
                    log("> RAR motoru (Junrar) devrede...");
                    Junrar.extract(tempFile, outDir);
                    log("> RAR Arşivi başarıyla çıkarıldı!");
                } 
                // 7Z DESTEĞİ
                else if (lowerName.endsWith(".7z")) {
                    log("> 7Z motoru (Apache Commons) devrede...");
                    try (SevenZFile sevenZFile = new SevenZFile(tempFile)) {
                        SevenZArchiveEntry entry;
                        while ((entry = sevenZFile.getNextEntry()) != null) {
                            if (entry.isDirectory()) continue;
                            File outFile = new File(outDir, entry.getName());
                            outFile.getParentFile().mkdirs();
                            try (FileOutputStream out = new FileOutputStream(outFile)) {
                                byte[] content = new byte[8192];
                                int len;
                                while ((len = sevenZFile.read(content)) > 0) {
                                    out.write(content, 0, len);
                                }
                            }
                            log("  -> " + entry.getName());
                        }
                    }
                    log("> 7Z Arşivi başarıyla çıkarıldı!");
                } 
                // ZIP DESTEĞİ (Fallback)
                else {
                    log("> ZIP motoru devrede...");
                    try (InputStream is = new FileInputStream(tempFile);
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
                    log("> ZIP Arşivi başarıyla çıkarıldı!");
                }
                
                // Temizlik
                tempFile.delete();
                log("> İŞLEM TAMAMLANDI!\n> Klasör: " + outDir.getAbsolutePath());
                
            } catch(Exception e) {
                log("> HATA: Arşiv bozuk veya format desteklenmiyor. " + e.getMessage());
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
