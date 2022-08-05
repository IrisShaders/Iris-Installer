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
    private boolean hasBeta;
    private final List<InstallerMeta.Version> versions = new ArrayList<>();

    public InstallerMeta(String url) {
        this.metaUrl = url;
    }

    public void load() throws IOException, JSONException {
        JSONObject json = readJsonFromUrl(this.metaUrl);
        hasBeta = json.getBoolean("hasBeta");
        json.getJSONArray("versions").toList().forEach(element -> versions.add(new Version((HashMap) element)));
    }

    public boolean hasBeta() {
        return hasBeta;
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
        return "{\n" +
                "            \"hasBeta\": true,\n" +
                "    \"versions\": [\n" +
                "        {\n" +
                "            \"name\": \"1.16.5\",\n" +
                "            \"outdated\": false\n" +
                "        },\n" +
                "        {\n" +
                "            \"name\": \"1.17.1\",\n" +
                "            \"outdated\": false\n" +
                "        },\n" +
                "        {\n" +
                "            \"name\": \"1.18.2\",\n" +
                "            \"outdated\": false\n" +
                "        },\n" +
                "        {\n" +
                "            \"name\": \"1.19\",\n" +
                "            \"outdated\": true\n" +
                "        },\n" +
                "        {\n" +
                "            \"name\": \"1.19.1\",\n" +
                "            \"outdated\": false\n" +
                "        }\n" +
                "    ]\n" +
                "}";
    }

    public static JSONObject readJsonFromUrl(String url) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new URL(url).openStream(), StandardCharsets.UTF_8));
        return new JSONObject(readAll(bufferedReader));
    }

    public static class Version {
        boolean outdated;
        String name;

        public Version(HashMap<String, Object> jsonObject) {
            System.out.println(jsonObject.toString());
            this.name = (String) jsonObject.get("name");
            this.outdated = (boolean) jsonObject.get("outdated");
        }
    }
}
