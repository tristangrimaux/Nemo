package com.tristangrimaux.www.nemo;

import android.app.Activity;
import android.content.Intent;
//import android.content.SharedPreferences;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.nfc.NfcAdapter;

import android.util.Log;
import android.widget.ImageButton;

import java.util.Locale;
import java.util.prefs.Preferences;
//import android.widget.ImageButton;


public class MainActivity extends AppCompatActivity implements SimpleNFC.NFCCallback {

    // Weak reference to prevent retain loop. mBallotCallback is responsible for exiting
    // foreground mode before it becomes invalid (e.g. during onPause() or onStop()).

    public static int READER_FLAGS =
            NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B | NfcAdapter.FLAG_READER_NFC_BARCODE | NfcAdapter.FLAG_READER_NFC_F| NfcAdapter.FLAG_READER_NFC_V | NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK;
    public SimpleNFC mSimpleNFC;
    private ImageButton btnGreen;
    private ImageButton btnRed;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSimpleNFC = new SimpleNFC(this);
        enableReaderMode();
        //SharedPreferences sp = getSharedPreferences("pref_general", MODE_PRIVATE);

        btnGreen = (ImageButton) findViewById(R.id.greenButton);

        btnGreen.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 if (mSimpleNFC != null) {
                     mSimpleNFC.writeTagGreen();
                 }
             }
        });

        btnRed = (ImageButton) findViewById(R.id.redButton);
        btnRed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Grabamos al candidato Rojo!", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                if (mSimpleNFC != null) {
                    mSimpleNFC.writeTagRed();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void TextToSnack(final String textthis) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Snackbar.make(findViewById(R.id.contentmain), textthis, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

    }
    @Override
    public void onNFCWritten (final String Candidate) {
        TextToSnack("Grabamos al candidato " + Candidate + "!");
    }
    @Override
    public void onNFCReceived (final String tagId, final SimpleNFC thisSimpleNFC){
        Log.i("nfc me ", tagId);
        final byte[] data = thisSimpleNFC.readTag();
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (thisSimpleNFC.isDataGreen(data)) {
                    startShake(btnGreen);
                }
                if (thisSimpleNFC.isDataRed(data)) {
                    startShake(btnRed);
                }

                btnGreen.setEnabled(true);
                btnRed.setEnabled(true);
            }
        });

    }

    public void startShake(ImageButton btn) {
        Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
        btn.startAnimation(shake);
    }

    @Override
    public void onNFCFailed(final String error){
        TextToSnack("Problema al grabar" + error + "!");
        Log.i("Nemo.onNFCFailed ", error);
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnGreen.setEnabled(false);
                btnRed.setEnabled(false);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        disableReaderMode();
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnGreen.setEnabled(true);
                btnRed.setEnabled(true);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        enableReaderMode();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        mSimpleNFC.setRedBlock(sp.getString("prefRedBlock", "00000000"));
        mSimpleNFC.setGreenBlock(sp.getString("prefGreenBlock", "00000000"));

    }

    private void enableReaderMode() {
        Activity activity = this;
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(activity);
        if (nfc != null) {
            nfc.enableReaderMode(activity, mSimpleNFC, READER_FLAGS, null);
        } else {
            Log.i("Nemo.enableReaderMode", "NFC Adapter is null");
        }

    }

    private void disableReaderMode() {
        Activity activity = this;
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(activity);
        if (nfc != null) {
            nfc.disableReaderMode(activity);
        }
    }

}

