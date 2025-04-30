package com.vszines.adatbazisapp;

import android.os.AsyncTask;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.journeyapps.barcodescanner.CaptureActivity;
import com.journeyapps.barcodescanner.IntentIntegrator;
import com.journeyapps.barcodescanner.IntentResult;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {
    private ArrayList<String[]> csvDatabase = new ArrayList<>();
    private static final int CAMERA_PERMISSION_CODE = 100;
    private TextView resultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultText = findViewById(R.id.result_text);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            startScan();
        }

        // Mindig töltsük le a CSV-t
        new DownloadCSVTask().execute("https://drive.google.com/uc?export=download&id=1UxBRjpLLpZcW9JUFHNIMhbjJFvyn3mjZ");
    }

    private void startScan() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Olvasd be a vonalkódot");
        integrator.setOrientationLocked(true);
        integrator.setCaptureActivity(CaptureActivity.class);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {

                if (csvDatabase.isEmpty()) {
                    Toast.makeText(this, "Az adatbázis még nem töltődött be", Toast.LENGTH_SHORT).show();
                    return;
                }

                String scannedCode = result.getContents();
                boolean found = false;

                for (String[] row : csvDatabase) {
                    for (int i = 0; i < 8 && i < row.length; i++) {
                        if (row[i].equalsIgnoreCase(scannedCode)) {
                            String info = "Név: " + getSafe(row, 7) + "\n" +
                                    "Készlet: " + getSafe(row, 8) + "\n" +
                                    "Ár: " + getSafe(row, 9) + "\n" +
                                    "Beszerzés: " + getSafe(row, 10) + "\n" +
                                    "Utolsó eladás: " + getSafe(row, 11) + "\n" +
                                    "Bolti polc: " + getSafe(row, 12) + "\n" +
                                    "Raktári polc: " + getSafe(row, 13);
                            resultText.setText(info);
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                }

                if (!found) {
                    resultText.setText("Nincs találat a beolvasott kódra: " + scannedCode);
                }

            } else {
                Toast.makeText(this, "Nincs adat", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private String getSafe(String[] row, int index) {
        return index < row.length ? row[index] : "";
    }

    private class DownloadCSVTask extends AsyncTask<String, Void, ArrayList<String[]>> {
        @Override
        protected ArrayList<String[]> doInBackground(String... urls) {
            ArrayList<String[]> csvData = new ArrayList<>();
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                InputStream inputStream = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                boolean firstLine = true;
                while ((line = reader.readLine()) != null) {
                    if (firstLine) {
                        firstLine = false; // Fejléc átugrása
                        continue;
                    }
                    csvData.add(line.split(","));
                }

                reader.close();
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return csvData;
        }

        @Override
        protected void onPostExecute(ArrayList<String[]> result) {
            csvDatabase = result;
            Toast.makeText(MainActivity.this, "Adatbázis betöltve: " + result.size() + " sor", Toast.LENGTH_SHORT).show();
        }
    }
}
