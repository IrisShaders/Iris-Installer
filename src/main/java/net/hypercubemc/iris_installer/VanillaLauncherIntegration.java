package net.hypercubemc.iris_installer;

import net.fabricmc.installer.client.ProfileInstaller;
import net.fabricmc.installer.util.Reference;
import net.fabricmc.installer.util.Utils;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class VanillaLauncherIntegration {
    public static void installToLauncher(Path vanillaGameDir, Path instanceDir, String profileName, String gameVersion, String loaderName, String loaderVersion, Icon icon) throws IOException {
        String versionId = String.format("%s-%s-%s", loaderName, loaderVersion, gameVersion);

        ProfileInstaller.LauncherType launcherType = System.getProperty("os.name").contains("Windows") ? getLauncherType(vanillaGameDir) : /* Return standalone if we aren't on Windows.*/ ProfileInstaller.LauncherType.WIN32;
        installVersion(vanillaGameDir, gameVersion, loaderName, loaderVersion, launcherType);
        installProfile(vanillaGameDir, instanceDir, profileName, versionId, icon, launcherType);
    }

    public static void installVersion(Path mcDir, String gameVersion, String loaderName, String loaderVersion, ProfileInstaller.LauncherType launcherType) throws IOException {
        System.out.println("Installing " + gameVersion + " with fabric " + loaderVersion + " to launcher " + launcherType);
        String versionId = String.format("%s-%s-%s", loaderName, loaderVersion, gameVersion);
        Path versionsDir = mcDir.resolve("versions");
        Path profileDir = versionsDir.resolve(versionId);
        Path profileJson = profileDir.resolve(versionId + ".json");
        if (!Files.exists(profileDir)) {
            Files.createDirectories(profileDir);
        }

        Path dummyJar = profileDir.resolve(versionId + ".jar");
        Files.deleteIfExists(dummyJar);
        Files.createFile(dummyJar);
        URL profileUrl = new URL(Reference.getMetaServerEndpoint(String.format("v2/versions/loader/%s/%s/profile/json", gameVersion, loaderVersion)));
        Utils.downloadFile(profileUrl, profileJson);
    }

    private static void installProfile(Path mcDir, Path instanceDir, String profileName, String versionId, Icon icon, ProfileInstaller.LauncherType launcherType) throws IOException {
        Path launcherProfiles = mcDir.resolve(launcherType.profileJsonName);
        if (!Files.exists(launcherProfiles)) {
            System.out.println("Could not find launcher_profiles");
            return;
        }

        System.out.println("Creating profile");

        JSONObject jsonObject = new JSONObject(Utils.readString(launcherProfiles));

        JSONObject profiles = jsonObject.getJSONObject("profiles");

        String foundProfileName = profileName;

        for (Iterator<String> it = profiles.keys(); it.hasNext();) {
            String key = it.next();

            JSONObject foundProfile = profiles.getJSONObject(key);
            if (foundProfile.has("lastVersionId") && foundProfile.getString("lastVersionId").equals(versionId) && foundProfile.has("gameDir") && foundProfile.getString("gameDir").equals(instanceDir.toString())) {
                foundProfileName = key;
            }
        }

        // If the profile already exists, use it instead of making a new one so that user's settings are kept (e.g icon)
        JSONObject profile = profiles.has(foundProfileName) ? profiles.getJSONObject(foundProfileName) : createProfile(profileName, instanceDir, versionId, icon);
        profile.put("name", profileName);
        profile.put("lastUsed", Utils.ISO_8601.format(new Date())); // Update timestamp to bring to top of profile list
        profile.put("lastVersionId", versionId);

        profiles.put(foundProfileName, profile);
        jsonObject.put("profiles", profiles);

        Utils.writeToFile(launcherProfiles, jsonObject.toString());
    }

    private static JSONObject createProfile(String name, Path instanceDir, String versionId, Icon icon) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        jsonObject.put("type", "custom");
        jsonObject.put("created", Utils.ISO_8601.format(new Date()));
        jsonObject.put("gameDir", instanceDir.toString());
        jsonObject.put("lastUsed", Utils.ISO_8601.format(new Date()));
        jsonObject.put("lastVersionId", versionId);
        jsonObject.put("icon", getProfileIcon(icon));
        return jsonObject;
    }

    private static String getProfileIcon(Icon icon) {
        if (icon == Icon.FABRIC) {
            return Utils.getProfileIcon();
        }

        try {
            InputStream is = Utils.class.getClassLoader().getResourceAsStream("iris_profile_icon.png");

            String var4;
            try {
                byte[] ret = new byte[4096];
                int offset = 0;

                int len;
                while((len = is.read(ret, offset, ret.length - offset)) != -1) {
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


    private static ProfileInstaller.LauncherType showLauncherTypeSelection() {
        Object[] options = new Object[]{Utils.BUNDLE.getString("prompt.launcher.type.xbox"), Utils.BUNDLE.getString("prompt.launcher.type.win32")};
        int result = JOptionPane.showOptionDialog((Component)null, Utils.BUNDLE.getString("prompt.launcher.type.body"), Utils.BUNDLE.getString("installer.title"), 1, 3, (javax.swing.Icon)null, options, options[0]);
        if (result == -1) {
            return null;
        } else {
            return result == 0 ? ProfileInstaller.LauncherType.MICROSOFT_STORE : ProfileInstaller.LauncherType.WIN32;
        }
    }

    public static ProfileInstaller.LauncherType getLauncherType(Path vanillaGameDir) {
        ProfileInstaller.LauncherType launcherType;
        List<ProfileInstaller.LauncherType> types = getInstalledLauncherTypes(vanillaGameDir);
        if (types.size() == 0) {
            // Default to WIN32, since nothing will happen anyway
            launcherType = ProfileInstaller.LauncherType.WIN32;
        } else if (types.size() == 1) {
            System.out.println("Found only one launcher (" + types.get(0) + "), will proceed with that!");
            launcherType = types.get(0);
        } else {
            launcherType = showLauncherTypeSelection();
            if (launcherType == null) {
                System.out.println(Utils.BUNDLE.getString("prompt.ready.install"));
                launcherType = ProfileInstaller.LauncherType.WIN32;
            }
        }
        return launcherType;
    }

    public static List<ProfileInstaller.LauncherType> getInstalledLauncherTypes(Path mcDir) {
        return Arrays.stream(ProfileInstaller.LauncherType.values()).filter((launcherType) -> Files.exists(mcDir.resolve(launcherType.profileJsonName), new LinkOption[0])).collect(Collectors.toList());
    }

    public enum Icon {
        IRIS,
        FABRIC
    }
}
