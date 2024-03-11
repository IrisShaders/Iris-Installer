package net.hypercubemc.iris_installer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InstallerMeta {
    private final String metaUrl;
    private String betaSnippet;
    private final List<InstallerMeta.Version> versions = new ArrayList<>();

    public InstallerMeta(String url) {
        this.metaUrl = url;
    }

    public void load() throws IOException, JSONException {
        System.out.println(metaUrl);

        JSONObject json = readJsonFromUrl(this.metaUrl);
        betaSnippet = "Distant Horizons";
        json.getJSONArray("versions").toList().forEach(element -> versions.add(new Version((HashMap<String, Object>) element)));
    }

    public String getBetaSnippet() {
        return betaSnippet;
    }

    public List<InstallerMeta.Version> getVersions() {
        return this.versions;
    }

    public static String readAll(Reader reader) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        int codePoint;
        while ((codePoint = reader.read()) != -1) {
            stringBuilder.append((char) codePoint);
        }
        return stringBuilder.toString();
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new URL(url).openStream(), StandardCharsets.UTF_8));
        return new JSONObject(readAll(bufferedReader));
    }

    public static class Version {
        boolean outdated;
        boolean hasBeta;
        boolean snapshot;
        String name;

        public Version(HashMap<String, Object> jsonObject) {
            this.name = (String) jsonObject.get("name");
            this.snapshot = (boolean) jsonObject.get("snapshot");
            this.outdated = (boolean) jsonObject.get("outdated");
            this.hasBeta = (boolean) jsonObject.get("hasBeta");
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
