package com.vegetarianbaconite.teslainspect;


import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import java.util.ArrayList;

public class Dialogs {
    private ArrayList<String> errors;
    private ArrayList<Integer> errorCodes;
    private Context c;
    private AutoFixer a;

    public Dialogs(Context context) {
        errors = new ArrayList<>();
        errorCodes = new ArrayList<>();
        this.c = context;
        a = new AutoFixer(context);
    }

    public void addError(int errorResID) {
        errors.add(c.getString(errorResID));
        errorCodes.add(errorResID);
    }

    public AlertDialog build() {
        String errorString = "";
        Boolean hasErrors = !(errors.size() == 0);

        if (hasErrors) {
            for (String s : errors) {
                errorString += s;
            }
            errorString += c.getString(R.string.fixOffer);
        } else {
            errorString = c.getString(R.string.goodToGo);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle(hasErrors ? "Errors Found" : "All Good!")
                .setMessage(errorString)
                .setPositiveButton(hasErrors ? "Fix Now" : "Okay",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                a.fix(errorCodes);
                            }
                        })
                .setNegativeButton("No Thanks", null);

        return builder.create();
    }
}
