package net.hypercubemc.iris_installer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import javax.swing.SwingWorker;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/**
 *
 * @author ims
 */
public class Downloader extends SwingWorker<Void, Void> {

    private final String url;
    private final File file;

    public Downloader(String url, File file) {
        this.url = url;
        this.file = file;
    }

    @Override
    protected Void doInBackground() throws Exception {
        URL url = new URL(this.url);
        HttpsURLConnection connection = (HttpsURLConnection) url
                .openConnection();
        long filesize = connection.getContentLengthLong();

        if (filesize == -1) {
            throw new Exception("Content length must not be -1 (unknown)!");
        }

        long totalDataRead = 0;

        try ( java.io.BufferedInputStream in = new java.io.BufferedInputStream(connection.getInputStream())) {
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            try ( java.io.BufferedOutputStream bout = new BufferedOutputStream(fos, 1024)) {
                byte[] data = new byte[1024];
                int i;

                while ((i = in.read(data, 0, 1024)) >= 0) {
                    totalDataRead = totalDataRead + i;
                    bout.write(data, 0, i);

                    int percent = (int) ((totalDataRead * 100) / filesize);

                    setProgress(percent);
                }
            }
        }
        return null;
    }
}
