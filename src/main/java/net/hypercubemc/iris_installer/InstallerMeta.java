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
import java.util.List;

public class InstallerMeta {
    private final String metaUrl;
    private final List<String> gameVersions = new ArrayList<>();
    private final List<InstallerMeta.Edition> editions = new ArrayList<>();

    public InstallerMeta(String url) {
        this.metaUrl = url;
    }

    public void load() throws IOException, JSONException {
        JSONObject json = readJsonFromUrl(this.metaUrl);
        json.getJSONArray("game_versions").toList().forEach(element -> gameVersions.add(element.toString()));
        json.getJSONArray("editions").forEach(object -> editions.add(new Edition((JSONObject) object)));
    }

    public List<String> getGameVersions() {
        return this.gameVersions;
    }

    public List<InstallerMeta.Edition> getEditions() {
        return this.editions;
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

    public static class Edition {
        String name;
        String displayName;
        List<String> compatibleVersions = new ArrayList<>();

        public Edition(JSONObject jsonObject) {
            this.name = jsonObject.getString("name");
            this.displayName = jsonObject.getString("display_name");

            for (int i = 0; i < jsonObject.getJSONArray("compatible_versions").toList().size(); i++){
                compatibleVersions.add(jsonObject.getJSONArray("compatible_versions").toList().get(i).toString());
            }
        }
    }
}
