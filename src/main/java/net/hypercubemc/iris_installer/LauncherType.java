package net.hypercubemc.iris_installer;

public enum LauncherType {
    WIN32("launcher_profiles.json"),
    MICROSOFT_STORE("launcher_profiles_microsoft_store.json");

    public final String profileJsonName;

    LauncherType(String profileJsonName) {
        this.profileJsonName = profileJsonName;
    }
}