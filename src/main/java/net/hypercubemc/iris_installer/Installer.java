package net.hypercubemc.iris_installer;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import net.fabricmc.installer.Main;
import net.fabricmc.installer.util.MetaHandler;
import net.fabricmc.installer.util.Reference;
import net.fabricmc.installer.util.Utils;
import net.hypercubemc.iris_installer.layouts.VerticalLayout;
import mjson.Json;
import org.json.JSONException;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Installer {
    private static final int BREAKING_VERSION_NUMBER = 1;
    InstallerMeta INSTALLER_META;
    List<InstallerMeta.Edition> EDITIONS;
    List<String> GAME_VERSIONS;
    String BASE_URL = "https://raw.githubusercontent.com/IrisShaders/Iris-Installer-Files/master/";

    String selectedEditionName;
    String selectedEditionDisplayName;
    String selectedVersion;
    Path customInstallDir;

    JButton button;
    JComboBox<String> editionDropdown;
    JComboBox<String> versionDropdown;
    JButton installDirectoryPicker;
    JCheckBox installAsModCheckbox;
    JProgressBar progressBar;

    boolean finishedSuccessfulInstall = false;
    boolean installAsMod = false;

    public Installer() {

    }

    public static void main(String[] args) {
        System.out.println("Launching installer...");
        new Installer().start();
    }

    public void start() {
        boolean dark = DarkModeDetector.isDarkMode();
        System.setProperty("apple.awt.application.appearance", "system");
        if (dark) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }

        Main.LOADER_META = new MetaHandler(Reference.getMetaServerEndpoint("v2/versions/loader"));
        try {
            Main.LOADER_META.load();
        } catch (Exception e) {
            System.out.println("Failed to fetch fabric version info from the server!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "The installer was unable to fetch fabric version info from the server, please check your internet connection and try again later.", "Please check your internet connection!", JOptionPane.ERROR_MESSAGE);
            return;
        }

        INSTALLER_META = new InstallerMeta(BASE_URL + "meta.json");
        try {
            INSTALLER_META.load();
        } catch (IOException e) {
            System.out.println("Failed to fetch installer metadata from the server!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "The installer was unable to fetch metadata from the server, please check your internet connection and try again later.", "Please check your internet connection!", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (JSONException e) {
            System.out.println("Failed to fetch installer metadata from the server!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Installer metadata parsing failed, please contact Justsnoopy30! \nError: " + e, "Metadata Parsing Failed!", JOptionPane.ERROR_MESSAGE);
            return;
        }

        GAME_VERSIONS = INSTALLER_META.getGameVersions();
        EDITIONS = INSTALLER_META.getEditions();

        JFrame frame = new JFrame("Iris Installer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setSize(350,350);
        frame.setLocationRelativeTo(null); // Centers the window
        frame.setIconImage(new ImageIcon(Objects.requireNonNull(Utils.class.getClassLoader().getResource("iris_profile_icon.png"))).getImage());

        JPanel topPanel = new JPanel(new VerticalLayout());

        JPanel editionPanel = new JPanel();

        JLabel editionDropdownLabel = new JLabel("Select Edition:");
        editionDropdownLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        List<String> editionNames = new ArrayList<>();
        List<String> editionDisplayNames = new ArrayList<>();
        for (InstallerMeta.Edition edition : EDITIONS) {
            editionNames.add(edition.name);
            editionDisplayNames.add(edition.displayName);
        }
        String[] editionNameList = editionNames.toArray(new String[0]);
        selectedEditionName = editionNameList[0];
        String[] editionDisplayNameList = editionDisplayNames.toArray(new String[0]);
        selectedEditionDisplayName = editionDisplayNameList[0];

        editionDropdown = new JComboBox<>(editionDisplayNameList);
        editionDropdown.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                selectedEditionName = editionNameList[editionDropdown.getSelectedIndex()];
                selectedEditionDisplayName = (String) e.getItem();
                if (customInstallDir == null) {
                    installDirectoryPicker.setText(getDefaultInstallDir().toFile().getName());
                }

                readyAll();
            }
        });

        editionPanel.add(editionDropdownLabel);
        editionPanel.add(editionDropdown);

        JPanel versionPanel = new JPanel();

        JLabel versionDropdownLabel = new JLabel("Select Game Version:");
        versionDropdownLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        List<String> gameVersions = GAME_VERSIONS.subList(0, GAME_VERSIONS.size()); // Clone the list
        Collections.reverse(gameVersions); // Reverse the order of the list so that the latest version is on top and older versions downward
        String[] gameVersionList = gameVersions.toArray(new String[0]);
        selectedVersion = gameVersionList[0];

        versionDropdown = new JComboBox<>(gameVersionList);
        versionDropdown.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                selectedVersion = (String) e.getItem();

                readyAll();
            }
        });

        versionPanel.add(versionDropdownLabel);
        versionPanel.add(versionDropdown);

        JPanel installDirectoryPanel = new JPanel();

        JLabel installDirectoryPickerLabel = new JLabel("Select Install Directory:");
        installDirectoryPickerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        installDirectoryPicker = new JButton(getDefaultInstallDir().toFile().getName());
        installDirectoryPicker.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setFileHidingEnabled(false);
            int option = fileChooser.showOpenDialog(frame);
            if (option == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                customInstallDir = file.toPath();
                installDirectoryPicker.setText(file.getName());

                readyAll();
            }
        });

        installDirectoryPanel.add(installDirectoryPickerLabel);
        installDirectoryPanel.add(installDirectoryPicker);

        installAsModCheckbox = new JCheckBox("Install as Fabric Mod", false);
        installAsModCheckbox.setToolTipText("If this box is checked, Iris will be installed to your mods \n folder," +
                        "allowing you to use Iris with other Fabric mods!");
        installAsModCheckbox.setHorizontalTextPosition(SwingConstants.LEFT);
        installAsModCheckbox.setAlignmentX(Component.CENTER_ALIGNMENT);
        installAsModCheckbox.addActionListener(e -> {
            installAsMod = installAsModCheckbox.isSelected();
            readyAll();
        });

        topPanel.add(editionPanel);
        topPanel.add(versionPanel);
        topPanel.add(installDirectoryPanel);
        topPanel.add(installAsModCheckbox);

        JPanel bottomPanel = new JPanel();

        progressBar = new JProgressBar();
        progressBar.setValue(0);
        progressBar.setMaximum(100);
        progressBar.setStringPainted(true);

        button = new JButton("Install");
        button.addActionListener(action -> {
            if (!EDITIONS.stream().filter(edition -> edition.name.equals(selectedEditionName)).findFirst().get().compatibleVersions.contains(selectedVersion)) {
                JOptionPane.showMessageDialog(frame, "The selected edition is not compatible with the chosen game version.", "Incompatible Edition", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String loaderName = installAsMod ? "fabric-loader" : "iris-fabric-loader";

            try {
                URL loaderVersionUrl = new URL("https://raw.githubusercontent.com/IrisShaders/Iris-Installer-Maven/master/latest-loader");
                String loaderVersion = installAsMod ? Main.LOADER_META.getLatestVersion(false).getVersion() : Utils.readTextFile(loaderVersionUrl);
                boolean success = VanillaLauncherIntegration.installToLauncher(getVanillaGameDir(), getInstallDir(), installAsMod ? "Fabric Loader " + selectedVersion : selectedEditionDisplayName, selectedVersion, loaderName, loaderVersion, installAsMod ? VanillaLauncherIntegration.Icon.FABRIC: VanillaLauncherIntegration.Icon.IRIS);
                if (!success) {
                    System.out.println("Failed to install to launcher, canceling!");
                    return;
                }
            } catch (IOException e) {
                System.out.println("Failed to install version and profile to vanilla launcher!");
                e.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Failed to install to vanilla launcher, please contact Justsnoopy30! \nError: " + e, "Failed to install to launcher", JOptionPane.ERROR_MESSAGE);
                return;
            }

            File storageDir = getStorageDirectory().toFile();
            if (!storageDir.exists() || !storageDir.isDirectory()) {
                storageDir.mkdir();
            }

            button.setText("Downloading...");
            progressBar.setValue(0);
            setInteractionEnabled(false);

            String zipName = selectedEditionName + "-" + selectedVersion + ".zip";

            String downloadURL = "https://github.com/IrisShaders/Iris-Installer-Files/releases/latest/download/" + zipName;

            File saveLocation = getStorageDirectory().resolve(zipName).toFile();

            final Downloader downloader = new Downloader(downloadURL, saveLocation);
            downloader.addPropertyChangeListener(event -> {
                if ("progress".equals(event.getPropertyName())) {
                    progressBar.setValue((Integer) event.getNewValue());
                } else if (event.getNewValue() == SwingWorker.StateValue.DONE) {
                    try {
                        downloader.get();
                    } catch (InterruptedException | ExecutionException e) {
                        System.out.println("Failed to download zip!");
                        e.getCause().printStackTrace();

                        String msg = String.format("An error occurred while attempting to download the required files, please check your internet connection and try again! \nError: %s",
                                e.getCause().toString());
                        JOptionPane.showMessageDialog(frame,
                                msg, "Download Failed!", JOptionPane.ERROR_MESSAGE, null);
                        readyAll();

                        return;
                    }

                    button.setText("Download completed!");

                    boolean cancelled = false;

                    File installDir = getInstallDir().toFile();
                    if (!installDir.exists() || !installDir.isDirectory()) installDir.mkdir();

                    File modsFolder = getInstallDir().resolve(installAsMod ? "mods" : "iris-reserved").toFile();
                    File[] modsFolderContents = modsFolder.listFiles();

                    if (modsFolderContents != null) {
                        boolean isEmpty = modsFolderContents.length == 0;

                        if (installAsMod && modsFolder.exists() && modsFolder.isDirectory() && !isEmpty) {
                            int result = JOptionPane.showConfirmDialog(frame,"An existing mods folder was found in the selected game directory. Do you want to update/install iris?", "Mods Folder Detected",
                                    JOptionPane.YES_NO_OPTION,
                                    JOptionPane.QUESTION_MESSAGE);
                            if (result != JOptionPane.YES_OPTION) {
                                cancelled = true;
                            }
                        }

                        if (!cancelled) {
                            boolean shownOptifineDialog = false;
                            boolean failedToRemoveOptifine = false;

                            for (File mod : modsFolderContents) {
                                if (mod.getName().toLowerCase().contains("optifine") || mod.getName().toLowerCase().contains("optifabric")) {
                                    if (!shownOptifineDialog) {
                                        int result = JOptionPane.showOptionDialog(frame,"Optifine was found in your mods folder, but Optifine is incompatible with Iris. Do you want to remove it, or cancel the installation?", "Optifine Detected",
                                                JOptionPane.DEFAULT_OPTION,
                                                JOptionPane.WARNING_MESSAGE, null, new String[]{"Yes", "Cancel"}, "Yes");

                                        shownOptifineDialog = true;
                                        if (result != JOptionPane.YES_OPTION) {
                                            cancelled = true;
                                            break;
                                        }
                                    }

                                    if (!mod.delete()) failedToRemoveOptifine = true;
                                }
                            }

                            if (failedToRemoveOptifine) {
                                System.out.println("Failed to delete optifine from mods folder");
                                JOptionPane.showMessageDialog(frame, "Failed to remove optifine from your mods folder, please make sure your game is closed and try again!", "Failed to remove optifine", JOptionPane.ERROR_MESSAGE);
                                cancelled = true;
                            }
                        }

                        if (!cancelled) {
                            boolean failedToRemoveIrisOrSodium = false;

                            for (File mod : modsFolderContents) {
                                if (mod.getName().toLowerCase().contains("iris") || mod.getName().toLowerCase().contains("sodium-fabric")) {
                                    if (!mod.delete()) failedToRemoveIrisOrSodium = true;
                                }
                            }

                            if (failedToRemoveIrisOrSodium) {
                                System.out.println("Failed to remove Iris or Sodium from mods folder to update them!");
                                JOptionPane.showMessageDialog(frame, "Failed to remove iris and sodium from your mods folder to update them, please make sure your game is closed and try again!", "Failed to prepare mods for update", JOptionPane.ERROR_MESSAGE);
                                cancelled = true;
                            }
                        }
                    }

                    if (cancelled) {
                        readyAll();
                        return;
                    }

                    if (!modsFolder.exists() || !modsFolder.isDirectory()) modsFolder.mkdir();

                    boolean installSuccess = installFromZip(saveLocation);
                    if (installSuccess) {
                        button.setText("Installation succeeded!");
                        finishedSuccessfulInstall = true;
                        editionDropdown.setEnabled(true);
                        versionDropdown.setEnabled(true);
                        installDirectoryPicker.setEnabled(true);
                        installAsModCheckbox.setEnabled(true);
                    } else {
                        button.setText("Installation failed!");
                        System.out.println("Failed to install to mods folder!");
                        JOptionPane.showMessageDialog(frame, "Failed to install to mods folder, please make sure your game is closed and try again!", "Installation Failed!", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            downloader.execute();
        });

        bottomPanel.add(progressBar);
        bottomPanel.add(button);

        frame.getContentPane().add(topPanel, BorderLayout.NORTH);
        frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);
        
        try {
            Json versionInfo = Json.read(new URL("https://raw.githubusercontent.com/IrisShaders/Iris-Installer-Maven/master/installer-versions.json"));
            if (versionInfo.asJsonMap().get("lastBreakingId").asInteger() > BREAKING_VERSION_NUMBER) {
                invalidVersionError(frame, "Outdated Installer", "You are running an outdated Iris Installer version that can no longer install Iris.", false);
                System.exit(0);
            }
        } catch (Exception e) {
            System.err.println("Failed to check for updates!");
            e.printStackTrace();
            invalidVersionError(frame, "Update check failed", "Unable to check for installer updates. The installation may fail, you should try downloading it again.", true);
        }

        frame.setVisible(true);

        System.out.println("Launched!");
    }

    private void invalidVersionError(JFrame frame, String title, String message, boolean allowContinue) {
        int messageType = allowContinue ? JOptionPane.WARNING_MESSAGE : JOptionPane.ERROR_MESSAGE;
        int result = JOptionPane.showConfirmDialog(frame, message
                + "\nDo you want to open the Iris website to get the latest version?", title,
                JOptionPane.OK_CANCEL_OPTION, messageType);
        if (result == -1) { // User pressed escape or X button in the corner
            System.exit(0);
        }
        if (result == 0) {
            System.out.println("Opening browser...");
            try {
                Desktop.getDesktop().browse(new URI("https://irisshaders.net/download"));
            } catch (Exception e2) { // Should never happen, but let's handle it, why not, just in case
                JOptionPane.showMessageDialog(frame, "errrrr... This is ankward... Browser didn't launch. "
                        + "\nGo to https://irisshaders.net/download to get the latest installer", "Failed to open browser", JOptionPane.ERROR_MESSAGE);
                e2.printStackTrace();
            }
            System.exit(0);
        }
        if (!allowContinue) {
            System.exit(0);
        }
    }

    // Works up to 2GB because of long limitation
    class Downloader extends SwingWorker<Void, Void> {
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
            try (java.io.BufferedInputStream in = new java.io.BufferedInputStream(
                    connection.getInputStream())) {
                java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
                try (java.io.BufferedOutputStream bout = new BufferedOutputStream(
                        fos, 1024)) {
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

    public boolean installFromZip(File zip) {
        try {
            int BUFFER_SIZE = 2048; // Buffer Size

            ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zip));

            ZipEntry entry = zipIn.getNextEntry();
            // iterates over entries in the zip file
            while (entry != null) {
                String entryName = entry.getName();

                if (!installAsMod && entryName.startsWith("mods/")) {
                    entryName = entryName.replace("mods/", "iris-reserved/");
                }

                File filePath = getInstallDir().resolve(entryName).toFile();
                if (!entry.isDirectory()) {
                    // if the entry is a file, extracts it
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
                    byte[] bytesIn = new byte[BUFFER_SIZE];
                    int read = 0;
                    while ((read = zipIn.read(bytesIn)) != -1) {
                        bos.write(bytesIn, 0, read);
                    }
                    bos.close();
                } else {
                    // if the entry is a directory, make the directory
                    filePath.mkdir();
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
            zipIn.close();
            return true;
        } catch (IOException e) {
            e.getCause().printStackTrace();
            return false;
        }
    }

    public Path getStorageDirectory() {
        return getAppDataDirectory().resolve(getStorageDirectoryName());
    }

    public Path getInstallDir() {
        return customInstallDir != null ? customInstallDir : getDefaultInstallDir();
    }

    public Path getAppDataDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win"))
            return new File(System.getenv("APPDATA")).toPath();
        else if (os.contains("mac"))
            return new File(System.getProperty("user.home") + "/Library/Application Support").toPath();
        else if (os.contains("nux"))
            return new File(System.getProperty("user.home")).toPath();
        else
            return new File(System.getProperty("user.dir")).toPath();
    }

    public String getStorageDirectoryName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac"))
            return "iris-installer";
        else
            return ".iris-installer";
    }

    public Path getDefaultInstallDir() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("mac"))
            return getAppDataDirectory().resolve("minecraft");
        else
            return getAppDataDirectory().resolve(".minecraft");
    }

    public Path getVanillaGameDir() {
        String os = System.getProperty("os.name").toLowerCase();

        return os.contains("mac") ? getAppDataDirectory().resolve("minecraft") : getAppDataDirectory().resolve(".minecraft");
    }

    public boolean deleteDirectory(File dir) {
        File[] allContents = dir.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return dir.delete();
    }

    public void setInteractionEnabled(boolean enabled) {
        editionDropdown.setEnabled(enabled);
        versionDropdown.setEnabled(enabled);
        installDirectoryPicker.setEnabled(enabled);
        installAsModCheckbox.setEnabled(enabled);
        button.setEnabled(enabled);
    }

    public void readyAll() {
        finishedSuccessfulInstall = false;
        button.setText("Install");
        progressBar.setValue(0);
        setInteractionEnabled(true);
    }
}
