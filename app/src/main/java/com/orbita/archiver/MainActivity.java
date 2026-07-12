package com.orbita.archiver;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class MainActivity extends Activity {

    private TextView txtLog;
    private static final int PICK_FILE_TO_ZIP = 1;
    private static final int PICK_FILE_TO_UNZIP = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtLog = findViewById(R.id.txtLog);
        Button btnZip = findViewById(R.id.btnZip);
        Button btnUnzip = findViewById(R.id.btnUnzip);

        btnZip.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, PICK_FILE_TO_ZIP);
        });

        btnUnzip.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/zip");
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
                unzipSelectedFile(uri);
            }
        }
    }

    private void zipSelectedFile(Uri uri) {
        new Thread(() -> {
            try {
                String fileName = getFileName(uri);
                File outDir = getExternalFilesDir(null);
                File zipFile = new File(outDir, fileName + ".zip");

                log("Sıkıştırma başlatıldı: " + fileName);

                InputStream is = getContentResolver().openInputStream(uri);
                FileOutputStream fos = new FileOutputStream(zipFile);
                ZipOutputStream zos = new ZipOutputStream(fos);

                ZipEntry entry = new ZipEntry(fileName);
                zos.putNextEntry(entry);

                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }

                zos.closeEntry();
                zos.close();
                is.close();
                fos.close();

                log("BAŞARILI: Dosya sıkıştırıldı!\nYol: " + zipFile.getAbsolutePath());
            } catch (Exception e) {
                log("HATA: " + e.getMessage());
            }
        }).start();
    }

    private void unzipSelectedFile(Uri uri) {
        new Thread(() -> {
            try {
                String zipName = getFileName(uri);
                File outDir = new File(getExternalFilesDir(null), "Extracted_" + System.currentTimeMillis());
                outDir.mkdirs();

                log("Çıkarma başlatıldı: " + zipName);

                InputStream is = getContentResolver().openInputStream(uri);
                ZipInputStream zis = new ZipInputStream(is);
                ZipEntry entry;

                while ((entry = zis.getNextEntry()) != null) {
                    File outFile = new File(outDir, entry.getName());
                    FileOutputStream fos = new FileOutputStream(outFile);

                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                    zis.closeEntry();
                    log("Çıkarıldı: " + entry.getName());
                }
                zis.close();
                is.close();

                log("BAŞARILI: Tüm dosyalar klasöre çıkarıldı!\nKlasör: " + outDir.getAbsolutePath());
            } catch (Exception e) {
                log("HATA: " + e.getMessage());
            }
        }).start();
    }

    private String getFileName(Uri uri) {
        String result = "dosya_" + System.currentTimeMillis();
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
            txtLog.append("\n- " + message);
        });
    }
}
