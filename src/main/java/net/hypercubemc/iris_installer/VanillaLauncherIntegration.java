package net.hypercubemc.iris_installer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.quiltmc.installer.LaunchJson;
import org.quiltmc.installer.LauncherProfiles;
import org.quiltmc.installer.QuiltMeta;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

public class VanillaLauncherIntegration {
    private static final DateFormat ISO_8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    public static boolean installToLauncher(Component parent, Path vanillaGameDir, Path instanceDir, String profileName, String gameVersion, String loaderName, Icon icon) throws IOException {
        String loaderVersion = "";

        URL loaderVersionUrl = new URL("https://raw.githubusercontent.com/IrisShaders/Iris-Installer-Maven/master/latest-loader-quilt");
        URLConnection conn = loaderVersionUrl.openConnection();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            loaderVersion = reader.lines().collect(Collectors.joining("\n"));
        }

        if ("latest".equals(loaderVersion.trim())) {
            Set<QuiltMeta.Endpoint<?>> endpoints = new HashSet();
            endpoints.add(QuiltMeta.LOADER_VERSIONS_ENDPOINT);
            endpoints.add(QuiltMeta.INTERMEDIARY_VERSIONS_ENDPOINT);
            AtomicReference<String> loaderVersionReference = new AtomicReference<>();
            CompletableFuture<QuiltMeta> metaFuture = QuiltMeta.create("https://meta.quiltmc.org", "https://meta.fabricmc.net", endpoints);
            try {
                loaderVersionReference.set(metaFuture.get().getEndpoint(QuiltMeta.LOADER_VERSIONS_ENDPOINT).get(0));
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

            loaderVersion = loaderVersionReference.get();
        }

        String versionId = String.format("%s-%s-%s", loaderName, loaderVersion, gameVersion);

        LauncherType launcherType = System.getProperty("os.name").contains("Windows") ? getLauncherType(vanillaGameDir) : /* Return standalone if we aren't on Windows.*/ LauncherType.WIN32;
        if (launcherType == null) {
            // The installation has been canceled via closing the window, most likely.
            return false;
        }
        installVersion(vanillaGameDir, gameVersion, loaderName, loaderVersion, icon);
        installProfile(parent, vanillaGameDir, instanceDir, profileName, versionId, icon, launcherType);
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
                    array.put("-Diris.installer=true");
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

    private static void installProfile(Component parent, Path mcDir, Path instanceDir, String profileName, String versionId, Icon icon, LauncherType launcherType) throws IOException {
        final Path launcherProfiles = mcDir.resolve(launcherType.profileJsonName);
        if (!Files.exists(launcherProfiles)) {
            System.out.println("Could not find launcher_profiles");
            return;
        }

        System.out.println("Creating profile");

        JSONObject jsonObject = null;

        try {
            jsonObject = new JSONObject(new String(Files.readAllBytes(launcherProfiles), StandardCharsets.UTF_8));
        } catch (JSONException e) {
            JOptionPane.showMessageDialog(parent, "Failed to add profile, you might not have logged into the launcher.");
            return;
        }

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
        try (Writer writer = Files.newBufferedWriter(launcherProfiles)) {
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
            Class klass = icon == Icon.QUILT ? LauncherProfiles.class : NewInstaller.class;
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

    private static LauncherType showLauncherTypeSelection() {
        String[] options = new String[]{"Xbox/MS Store", "Standalone"};
        int result = JOptionPane.showOptionDialog(null, "Iris has detected 2 different installations of the Minecraft Launcher, which launcher do you wish to install Iris to?\\n\\n- Select Microsoft Store if you are playing Minecraft through the Xbox App or the Windows Store.\\n- Select Standalone if you downloaded the Minecraft launcher directly from the Minecraft.net website.\\n\\nIf you are unsure try the Microsoft Store option first, you can always re-run the installer.\n"
                , "Iris Installer", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (result == JOptionPane.CLOSED_OPTION) {
            return null;
        } else {
            return result == JOptionPane.YES_OPTION ? LauncherType.MICROSOFT_STORE : LauncherType.WIN32;
        }
    }

    public static LauncherType getLauncherType(Path vanillaGameDir) {
        LauncherType launcherType;
        List<LauncherType> types = getInstalledLauncherTypes(vanillaGameDir);
        if (types.size() == 0) {
            // Default to WIN32, since nothing will happen anyway
            System.out.println("No launchers found, profile installation will not take place!");
            launcherType = LauncherType.WIN32;
        } else if (types.size() == 1) {
            System.out.println("Found only one launcher (" + types.get(0) + "), will proceed with that!");
            launcherType = types.get(0);
        } else {
            System.out.println("Multiple launchers found, showing selection screen!");
            launcherType = showLauncherTypeSelection();
            if (launcherType == null) {
                System.out.println("couldn't find a launcher, doing win32!");
                launcherType = LauncherType.WIN32;
            }
        }

        return launcherType;
    }

    public static List<LauncherType> getInstalledLauncherTypes(Path mcDir) {
        return Arrays.stream(LauncherType.values()).filter((launcherType) -> Files.exists(mcDir.resolve(launcherType.profileJsonName))).collect(Collectors.toList());
    }

    public enum Icon {
        IRIS,
        QUILT
    }
}
