package io.rftp.redditclient;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * Copyright (c) 2016-present, RFTP Technologies Ltd.
 * All rights reserved.
 * <p>
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

class FileUtils {

    static Uri convertImageUrlToUri(Context context, String urlString) {
      String fileName = cutNameFromUrl(urlString);
      InputStream input = null;
      OutputStream output = null;
      try {
        URL url = new URL(urlString);
        input = url.openStream();

        File file = getFile(fileName);
        if(file.exists()) {
          return convertFileToUri(context, file);
        } else {
          output = new FileOutputStream(file);
          writeDataToTheFile(input, output);
          return convertFileToUri(context, file);
        }

      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        try {
          if (input != null) input.close();
          if (output != null) output.close();
        } catch(IOException e) {
          e.printStackTrace();
        }
      }
      return null;
    }

    private static Uri convertFileToUri(Context context, File file) {
      return FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file);
    }

    private static  void writeDataToTheFile(InputStream input, OutputStream output) throws IOException {
      byte[] buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = input.read(buffer)) != -1) {
        output.write(buffer, 0, bytesRead);
      }
    }

    @NonNull
    private static File getFile(String fileName) {
      File storagePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
      return new File(storagePath, fileName);
    }

    private static String cutNameFromUrl(String urlString) {
      String[] parts = urlString.split("/");
      return  parts[parts.length - 1];
    }
}
