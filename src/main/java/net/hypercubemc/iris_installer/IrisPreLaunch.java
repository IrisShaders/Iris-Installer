package net.hypercubemc.iris_installer;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public class IrisPreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        boolean dark = DarkModeDetector.isDarkMode();

        System.setProperty("apple.awt.application.appearance", "system");

        if (dark) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }

        if (GraphicsEnvironment.isHeadless() || !hasAwtSupport()) {
            TinyFileDialogs.tinyfd_messageBox("Cannot launch game", "The Iris Installer is not a mod, please remove it from your mods folder and download Iris from Modrinth. (https://modrinth.com/mod/iris)", "yesno", "error", false);
        } else {
            if(JOptionPane.showConfirmDialog(null, "The Iris Installer is not a mod, please remove it from your mods folder and download Iris from Modrinth. Would you like to launch Modrinth?", "Cannot launch game", JOptionPane.YES_NO_OPTION) == 0) {
                try {
                    openURL(new URI("https://modrinth.com/mod/iris"));
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        System.exit(0);
    }

    // shamelessly stolen from fabric
    public static boolean hasMacOs() {
        return System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("mac");
    }

    public static boolean openURL(URI uri) {
        if (!Desktop.isDesktopSupported()) {
            return false;
        }

        Desktop d = Desktop.getDesktop();
        if (!d.isSupported(Desktop.Action.BROWSE)) {
            return false;
        }

        try {
            d.browse(uri);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean hasAwtSupport() {
        if (hasMacOs()) {
            // check for JAVA_STARTED_ON_FIRST_THREAD_<pid> which is set if -XstartOnFirstThread is used
            // -XstartOnFirstThread is incompatible with AWT (force enables embedded mode)
            for (String key : System.getenv().keySet()) {
                if (key.startsWith("JAVA_STARTED_ON_FIRST_THREAD_")) return false;
            }
        }

        return true;
    }
}
