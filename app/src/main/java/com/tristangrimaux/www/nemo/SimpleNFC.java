package com.tristangrimaux.www.nemo;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;

/**
 * Created by tristan on 11/7/16.
 */
public class SimpleNFC implements NfcAdapter.ReaderCallback {
    public NfcV nfcvTag = null;
    public static final byte cmdWriteSingleBlock = 0x21;
    public static final byte cmdReadMultipleBlocks = 0x23;
    public static final byte simpleNFCFirstBlock    = 0;
    public static final byte simpleNFCBlocks    = 28;
    public static final byte simpleNFCBlockSize = 4;

    public static final byte[] GreenCode = new byte[] {
            (byte) 0x3f,
            (byte) 0x3f,
            (byte) 0x3f,
            (byte) 0x3f

    };

    public static final byte[] RedCode = new byte[] {
            (byte) 0x00,
            (byte) 0x00,
            (byte) 0x00,
            (byte) 0x00

    };


    private WeakReference<NFCCallback> mNFCCallback;

    public void setRedBlock(String redBlock) {

        String tmpStr = "00000000".substring(redBlock.length())+redBlock;
        byte [] tmp = hexStringToByteArray(tmpStr);
        for (int i = 0; i< this.RedCode.length-1; i++){
            if (i <= tmp.length ) {
                this.RedCode[i] = tmp[i];
            } else {
                this.RedCode[i] = 0;
            }
            Log.i("xxx", bytesToHex(this.RedCode));

        }
    }

    public void setGreenBlock(String greenBlock) {
        String tmpStr = "00000000".substring(greenBlock.length())+greenBlock;
        byte [] tmp = hexStringToByteArray(tmpStr);
        for (int i = 0; i< this.GreenCode.length-1; i++){
            if (i <= tmp.length ) {
                this.GreenCode[i] = tmp[i];
            } else {
                this.GreenCode[i] = 0;
            }
        }
        Log.i("xxx", bytesToHex(this.GreenCode));

    }

    interface NFCCallback {
        void onNFCReceived(String tagId, SimpleNFC thisSimpleNFC);
        void onNFCFailed(String error);
        void onNFCWritten(String candidate);
    }


    public SimpleNFC(NFCCallback NFCCallback) {
        mNFCCallback = new WeakReference<NFCCallback>(NFCCallback);
    }

    public void onTagDiscovered(Tag tag) {
        //NfcV nfcvTag = NfcV.get(tag);

        nfcvTag = NfcV.get(tag);
        if (nfcvTag != null) {
            try {
                // Connect to the remote NFC device
                // set up read command buffer
                nfcvTag.connect();
                mNFCCallback.get().onNFCReceived(tag.getId().toString(), this);
            } catch (IOException e) {
                //Log.e(TAG, "Error communicating with card: " + e.toString());
                mNFCCallback.get().onNFCFailed(e.toString());
            }
        }

    }

    public byte[] readTag() {
        byte[] dataBlock = null;
        if (nfcvTag != null) {
            int offset = 0;
            byte[] cmd;
            cmd = new byte[]{
                    (byte) 0x0,                  // flags: addressed (= UID field present)
                    (byte) cmdReadMultipleBlocks,                  // command: READ MULTIPLE BLOCKS
                    (byte) (offset & 0xff),      // first block number
                    (byte) ((simpleNFCBlocks - 1)& 0xff) // number of blocks (-1 as 0x00 means one block)
            };
            try {
                Log.i("inside", "estamos por leer");
                if (!nfcvTag.isConnected()) {
                    nfcvTag.connect();
                }
                dataBlock = nfcvTag.transceive(cmd);
                dataBlock = Arrays.copyOfRange(dataBlock, 1, 1+(simpleNFCBlockSize * simpleNFCBlocks));


                Log.i("aasas", bytesToHex(dataBlock));
                Log.i("aasas", String.valueOf(dataBlock.length));
                Log.i("inside", "ya leimos!");

            } catch (IOException e) {
                if (e.getMessage().equals("Tag was lost.")) {
                    Log.i("inside", "tag was lost carajo");
                } else {
                    Log.i("inside", e.getMessage());
                    mNFCCallback.get().onNFCFailed(e.toString());
                }
            }
        }
        return dataBlock;
    }

    private boolean compareSample(byte[] data, byte[] portion) {
        boolean rst = true;
        byte[] part = new byte[]{0,0,0,0};
        if (data == null) {
            rst = false;
        } else {
            for (int i = simpleNFCFirstBlock; i < simpleNFCBlocks; i ++) {
                System.arraycopy(data, simpleNFCBlockSize * i, part, 0, simpleNFCBlockSize);
                rst = rst & Arrays.equals(part, portion);
            }
        }
        return rst;
    }

    public boolean isDataRed(byte[] data) {
        return compareSample(data, RedCode);
    }

    public boolean isDataGreen(byte[] data) {
        return compareSample(data, GreenCode);
    }

    public int writeTag(byte[] dataBlock) {

        int blocksWritten = 0;  // offset of first block to read
        if (nfcvTag != null) {
            byte[] TagId = nfcvTag.getTag().getId();
            byte[] cmd;

            try {
                Log.i("inside", "estamos por escribir");
                if (! nfcvTag.isConnected()) {
                    nfcvTag.connect();
                }

                cmd = new byte[]{
                        (byte) 0x00, // Flags NOT addressed
                        cmdWriteSingleBlock, // Command: Write 1 blocks
                        simpleNFCFirstBlock,
                        (byte) 0x00,
                        (byte) 0x00,
                        (byte) 0x00,
                        (byte) 0x00
                };
                for (byte BlockNumber = simpleNFCFirstBlock; BlockNumber < simpleNFCBlocks ; BlockNumber ++) {
                    cmd[2] = BlockNumber; // copy BlockNumber
                    System.arraycopy(dataBlock, 0, cmd, 3, simpleNFCBlockSize); // copy data

                    //NfcV nfcvTag = NfcV.get(tag);
                    try {
                        byte[] xdata = nfcvTag.transceive(cmd);
                        //que hago con xdata? verifico algo??? calculo que si, no?
                        blocksWritten += 1;
                        Log.i("inside", "algo escribimos");
                    } catch (IOException e) {
                        if (e.getMessage().equals("Tag was lost.")) {
                            Log.i("inside", "tag was lost carajo");
                        } else {
                            mNFCCallback.get().onNFCFailed(e.toString());
                        }
                    }

                }

            } catch (IOException e) {
                mNFCCallback.get().onNFCFailed(e.toString());
            }
        }
        return blocksWritten;
    }


    public void writeTagRed() {
        int tagsWritten = writeTag(RedCode);
        if (tagsWritten == simpleNFCBlocks ) {
          mNFCCallback.get().onNFCWritten("Rojo");
        } else if (tagsWritten > 0 ) {
            mNFCCallback.get().onNFCWritten("Rojo");
        }
    }

    public void writeTagGreen() {
        int tagsWritten = writeTag(GreenCode);
        if (tagsWritten == simpleNFCBlocks ) {
            mNFCCallback.get().onNFCWritten("Verde");
        } else if (tagsWritten > 0 ) {
            mNFCCallback.get().onNFCWritten("Verde");
        }

    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len/2];

        for(int i = 0; i < len; i+=2){
            data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }

        return data;
    }
}
