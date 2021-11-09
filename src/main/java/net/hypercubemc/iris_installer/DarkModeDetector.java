package net.hypercubemc.iris_installer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

// Based off HanSolo's Detector, with added support for GNOME, and removed support for macOS accent colors for Java 8 compatibility. Original: https://gist.github.com/HanSolo/7cf10b86efff8ca2845bf5ec2dd0fe1d
public class DarkModeDetector {
    public enum OperatingSystem {WINDOWS, MACOS, LINUX, SOLARIS, NONE}

    private static final String REGQUERY_UTIL = "reg query ";
    private static final String REGDWORD_TOKEN = "REG_DWORD";
    private static final String DARK_THEME_CMD = REGQUERY_UTIL + "\"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize\"" + " /v AppsUseLightTheme";

    public static boolean isDarkMode() {
        switch (getOperatingSystem()) {
            case WINDOWS:
                return isWindowsDarkMode();
            case MACOS:
                return isMacOsDarkMode();
            case LINUX:
                return isGnome() && isGnomeDarkMode();
            case SOLARIS: // Solaris is screwed so we'll just return false.
            default:
                return false;
        }
    }

    public static boolean isMacOsDarkMode() {
        boolean isDarkMode = false;
        String line = query("defaults read -g AppleInterfaceStyle");
        if (line.equals("Dark")) {
            isDarkMode = true;
        }
        return isDarkMode;
    }

    public static boolean isWindowsDarkMode() {
        try {
            String result = query(DARK_THEME_CMD);
            int p = result.indexOf(REGDWORD_TOKEN);

            if (p == -1) {
                return false;
            }

            // 1 == Light Mode, 0 == Dark Mode
            String temp = result.substring(p + REGDWORD_TOKEN.length()).trim();
            return ((Integer.parseInt(temp.substring("0x".length()), 16))) == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isGnomeDarkMode() {
        // This is based off jSystemThemeDetector's code.
        final Pattern darkThemeNamePattern = Pattern.compile(".*dark.*", Pattern.CASE_INSENSITIVE);
        return darkThemeNamePattern.matcher(query("gsettings get org.gnome.desktop.interface gtk-theme")).matches();
    }

    public static OperatingSystem getOperatingSystem() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return OperatingSystem.WINDOWS;
        } else if (os.contains("mac")) {
            return OperatingSystem.MACOS;
        } else if (os.contains("nix") || os.contains("nux")) {
            return OperatingSystem.LINUX;
        } else if (os.contains("sunos")) {
            return OperatingSystem.SOLARIS;
        } else {
            return OperatingSystem.NONE;
        }
    }

    // The following GNOME + query methods are based off jSystemThemeDetector's code.
    public static boolean isGnome() {
        return getOperatingSystem() == OperatingSystem.LINUX && (
                queryResultContains("echo $XDG_CURRENT_DESKTOP", "gnome") ||
                        queryResultContains("echo $XDG_DATA_DIRS | grep -Eo 'gnome'", "gnome") ||
                        queryResultContains("ps -e | grep -E -i \"gnome\"", "gnome")
        );
    }

    private static boolean queryResultContains(String cmd, String subResult) {
        return query(cmd).toLowerCase().contains(subResult);
    }

    private static String query(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            StringBuilder stringBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String actualReadLine;
                while ((actualReadLine = reader.readLine()) != null) {
                    if (stringBuilder.length() != 0)
                        stringBuilder.append('\n');
                    stringBuilder.append(actualReadLine);
                }
            }
            return stringBuilder.toString();
        } catch (IOException e) {
            System.out.println("Exception caught while querying the OS:");
            e.printStackTrace();
            return "";
        }
    }
}