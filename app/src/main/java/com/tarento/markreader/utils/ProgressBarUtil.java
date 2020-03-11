/*
 *  Copyright (C) 2013 Zaark
 *  All rights reserved.
 *
 *  All code contained herein is and remains the property of Zaark.
 *  It may only be used only in accordance with the terms of
 *          the license agreement.
 *
 *  Contact: contact@zaark.com
 */

package com.tarento.markreader.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.view.WindowManager.BadTokenException;

import com.tarento.markreader.R;

/**
 * This class is used to show the progress dialog.
 *
 * @author Muthu.Krishnan
 */

public class ProgressBarUtil {
    private static final String TAG = ProgressBarUtil.class.getSimpleName();
    private static ProgressDialog progressDialog;

    /**
     * To show the progress dialog with the title as application name and message as "Loading" text.
     *
     * @param context
     */
    public static void showProgressDialog(Context context) {
        showProgressDialog(context, false);

    }

    public static void showProgressDialog(Context context, boolean cancelable) {
        if (context == null) {
            return;
        }
//        if (ZVLog.LOG) {
//            ZVLog.d(TAG, "showProgressDialog");
//        }
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(context);
            try {
                progressDialog.show();
            } catch (BadTokenException e) {

            }
            progressDialog.setCancelable(cancelable);
            progressDialog.setContentView(R.layout.progress_dialog_layout);
        }

    }

    /**
     * To dismiss the progress dialog
     */
    public static void dismissProgressDialog() {
//        if (ZVLog.LOG) {
//            ZVLog.d(TAG, "dismissProgressDialog");
//        }
        // try catch block for handling java.lang.IllegalArgumentException
        try {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        } catch (Exception e) {

        }
        progressDialog = null;
    }

}
