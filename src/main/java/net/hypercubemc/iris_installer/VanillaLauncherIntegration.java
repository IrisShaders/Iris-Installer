package net.hypercubemc.iris_installer;

import org.json.JSONArray;
import org.json.JSONObject;
import org.quiltmc.installer.LaunchJson;
import org.quiltmc.installer.LauncherProfiles;
import org.quiltmc.installer.QuiltMeta;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

public class VanillaLauncherIntegration {
    private static final DateFormat ISO_8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    public static boolean installToLauncher(Path vanillaGameDir, Path instanceDir, String profileName, String gameVersion, String loaderName, Icon icon) throws IOException {
        Set<QuiltMeta.Endpoint<?>> endpoints = new HashSet();
        endpoints.add(QuiltMeta.LOADER_VERSIONS_ENDPOINT);
        endpoints.add(QuiltMeta.INTERMEDIARY_VERSIONS_ENDPOINT);
        AtomicReference<String> loaderVersion = new AtomicReference<>();
        CompletableFuture<QuiltMeta> metaFuture = QuiltMeta.create("https://meta.quiltmc.org", "https://meta.fabricmc.net", endpoints);
        try {
            loaderVersion.set(metaFuture.get().getEndpoint(QuiltMeta.LOADER_VERSIONS_ENDPOINT).get(0));
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        String versionId = String.format("%s-%s-%s", loaderName, loaderVersion, gameVersion);

        installVersion(vanillaGameDir, gameVersion, loaderName, loaderVersion.get(), icon);
        installProfile(vanillaGameDir, instanceDir, profileName, versionId, icon);
        return true;
    }

    public static void installVersion(Path mcDir, String gameVersion, String loaderName, String loaderVersion, Icon icon) throws IOException {
        System.out.println("Installing " + gameVersion + " with quilt " + loaderVersion + " to launcher");
        String versionId = String.format("%s-%s-%s", loaderName, loaderVersion, gameVersion);
        Path versionsDir = mcDir.resolve("versions");
        Path profileDir = versionsDir.resolve(versionId);
        Path profileJsonPath = profileDir.resolve(versionId + ".json");
        if (!Files.exists(profileDir)) {
            Files.createDirectories(profileDir);
        }

        Path dummyJar = profileDir.resolve(versionId + ".jar");
        Files.deleteIfExists(dummyJar);
        Files.deleteIfExists(profileJsonPath);
        Files.createFile(dummyJar);
        CompletableFuture<String> json = LaunchJson.get(gameVersion, loaderVersion, "/v3/versions/loader/%s/%s/profile/json");
            try {
                String json2 = json.get();
                JSONObject object = new JSONObject(json2);
                object.put("id", versionId);
                if (icon == Icon.IRIS) {
                    JSONObject object2 = object.has("arguments") ? object.getJSONObject("arguments") : new JSONObject();
                    JSONArray array = object2.has("jvm") ? object2.getJSONArray("jvm") : new JSONArray();
                    array.put("-Dloader.modsDir=iris-reserved/" + gameVersion);
                    object2.put("jvm", array);
                    object.put("arguments", object2);
                }
                try (Writer writer = new OutputStreamWriter(Files.newOutputStream(profileJsonPath, StandardOpenOption.CREATE_NEW))) {
                    object.write(writer);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
    }

    private static void installProfile(Path mcDir, Path instanceDir, String profileName, String versionId, Icon icon) throws IOException {
        final Path launcherProfilesPath = mcDir.resolve("launcher_profiles.json");

        if (!Files.exists(launcherProfilesPath)) {
            System.out.println("Could not find launcher_profiles");
            return;
        }

        System.out.println("Creating profile");

        JSONObject jsonObject = new JSONObject(new String(Files.readAllBytes(launcherProfilesPath)));

        JSONObject profiles = jsonObject.getJSONObject("profiles");

        // Modify the profile
        if (profiles.has(profileName)) {
            JSONObject rawProfile = profiles.getJSONObject(profileName);

            rawProfile.put("lastVersionId", versionId);

            profiles.put(profileName, rawProfile);
        } else {
            // Create a new profile
            JSONObject rawProfile = new JSONObject();

            rawProfile.put("name", profileName);
            rawProfile.put("type", "custom");
            rawProfile.put("created", ISO_8601.format(new Date()));
            rawProfile.put("lastUsed", ISO_8601.format(new Date()));
            rawProfile.put("icon", getProfileIcon(icon));
            rawProfile.put("lastVersionId", versionId);

            profiles.put(profileName, rawProfile);
        }

        jsonObject.put("profiles", profiles);

        // Write out the new profiles
        try (Writer writer = Files.newBufferedWriter(launcherProfilesPath)) {
            jsonObject.write(writer);
        }
    }

    private static JSONObject createProfile(String name, Path instanceDir, String versionId, Icon icon) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        jsonObject.put("type", "custom");
        jsonObject.put("created", ISO_8601.format(new Date()));
        jsonObject.put("gameDir", instanceDir.toString());
        jsonObject.put("lastUsed", ISO_8601.format(new Date()));
        jsonObject.put("lastVersionId", versionId);
        jsonObject.put("icon", getProfileIcon(icon));
        return jsonObject;
    }

    private static String getProfileIcon(Icon icon) {
        try {
            Class klass = icon == Icon.QUILT ? LauncherProfiles.class : Installer.class;
            InputStream is = klass.getClassLoader().getResourceAsStream(icon == Icon.QUILT ? "icon.png" : "iris_profile_icon.png");

            String var4;
            try {
                byte[] ret = new byte[4096];
                int offset = 0;

                int len;
                while ((len = is.read(ret, offset, ret.length - offset)) != -1) {
                    offset += len;
                    if (offset == ret.length) {
                        ret = Arrays.copyOf(ret, ret.length * 2);
                    }
                }

                var4 = "data:image/png;base64," + Base64.getEncoder().encodeToString(Arrays.copyOf(ret, offset));
            } catch (Throwable var6) {
                if (is != null) {
                    try {
                        is.close();
                    } catch (Throwable var5) {
                        var6.addSuppressed(var5);
                    }
                }

                throw var6;
            }

            if (is != null) {
                is.close();
            }

            return var4;
        } catch (IOException var7) {
            var7.printStackTrace();
            return "TNT";
        }
    }

    public enum Icon {
        IRIS,
        QUILT
    }
}
