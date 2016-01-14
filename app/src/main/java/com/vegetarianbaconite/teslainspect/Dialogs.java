package com.vegetarianbaconite.teslainspect;


import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

/**
 * Created by robotics on 1/4/2016.
 */
public class Dialogs {
    public static AlertDialog osError(MainActivity c) {
        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle("Version Mismatch")
                .setMessage(R.string.osError)
                .setPositiveButton("Fix Now", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        return null;
    }
}
