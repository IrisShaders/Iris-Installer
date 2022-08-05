/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

package net.hypercubemc.iris_installer;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import net.fabricmc.installer.Main;
import net.fabricmc.installer.util.MetaHandler;
import net.fabricmc.installer.util.Reference;
import net.fabricmc.installer.util.Utils;
import org.json.JSONException;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 * @author ims
 */
public class NewInstaller extends JFrame {
    private boolean finishedSuccessfulInstall;
    private boolean installAsMod;
    private InstallerMeta.Version selectedVersion;
    String outdatedPlaceholder = " Warning: We have ended support for <version>.";
    private final List<InstallerMeta.Version> GAME_VERSIONS;
    String BASE_URL = "https://raw.githubusercontent.com/IrisShaders/Iris-Installer-Files/master/";
    private final InstallerMeta INSTALLER_META;
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox betaSelection;
    private javax.swing.JButton directoryName;
    private javax.swing.JRadioButton fabricType;
    private javax.swing.JLabel gameVersionLabel;
    private javax.swing.JComboBox<String> gameVersionList;
    private javax.swing.JButton installButton;
    private javax.swing.ButtonGroup installType;
    private javax.swing.JLabel installationDirectory;
    private javax.swing.JLabel installationType;
    private javax.swing.JLabel irisInstallerLabel;
    private javax.swing.JLabel outdatedText1;
    private javax.swing.JLabel outdatedText2;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JRadioButton standaloneType;
    // End of variables declaration//GEN-END:variables

    private Path customInstallDir;
    private static boolean dark;

    /** Creates new form NewJFrame */
    public NewInstaller() {
        super("Iris Installer");
        Main.LOADER_META = new MetaHandler(Reference.getMetaServerEndpoint("v2/versions/loader"));
        try {
            Main.LOADER_META.load();
        } catch (Exception e) {
            System.out.println("Failed to fetch fabric version info from the server!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "The installer was unable to fetch fabric version info from the server, please check your internet connection and try again later.", "Please check your internet connection!", JOptionPane.ERROR_MESSAGE);
            throw new RuntimeException(e);
        }

        INSTALLER_META = new InstallerMeta(BASE_URL + "meta-new.json");
        try {
            INSTALLER_META.load();
        } catch (IOException e) {
            System.out.println("Failed to fetch installer metadata from the server!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "The installer was unable to fetch metadata from the server, please check your internet connection and try again later.", "Please check your internet connection!", JOptionPane.ERROR_MESSAGE);
            throw new RuntimeException(e);
        } catch (JSONException e) {
            System.out.println("Failed to fetch installer metadata from the server!");
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Installer metadata parsing failed, please contact the Iris support team via Discord! \nError: " + e, "Metadata Parsing Failed!", JOptionPane.ERROR_MESSAGE);
            throw new RuntimeException(e);
        }

        this.setLocationRelativeTo(null); // Centers the window
        this.setIconImage(new ImageIcon(Objects.requireNonNull(Utils.class.getClassLoader().getResource("iris_profile_icon.png"))).getImage());

        GAME_VERSIONS = INSTALLER_META.getVersions();
        Collections.reverse(GAME_VERSIONS);
        selectedVersion = GAME_VERSIONS.get(0);

        initComponents();

        if (!INSTALLER_META.hasBeta()) {
            betaSelection.setVisible(false);
        }
        gameVersionList.removeAllItems();
        for (InstallerMeta.Version version : GAME_VERSIONS) {
            gameVersionList.addItem(version.name);
        }
        directoryName.setText(getDefaultInstallDir().toFile().getName());
        directoryName.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setFileHidingEnabled(false);
            int option = fileChooser.showOpenDialog(this);
            if (option == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                customInstallDir = file.toPath();
                directoryName.setText(file.getName());
            }
        });

        outdatedText1.setVisible(false);
        outdatedText2.setVisible(false);
        gameVersionList.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                selectedVersion = GAME_VERSIONS.stream().filter(v -> v.name.equals(e.getItem())).findFirst().orElse(GAME_VERSIONS.get(0));

                if (selectedVersion.outdated) {
                    outdatedText1.setText(outdatedPlaceholder.replace("<version>", selectedVersion.name));
                    betaSelection.setVisible(false);
                    outdatedText1.setVisible(true);
                    outdatedText2.setVisible(true);
                } else {
                    if (INSTALLER_META.hasBeta()) {
                        betaSelection.setVisible(true);
                    }
                    outdatedText1.setVisible(false);
                    outdatedText2.setVisible(false);
                }
            }
        });

        standaloneType.setSelected(true);
        standaloneType.addActionListener(e -> {
            installAsMod = false;
        });

        fabricType.addActionListener(e -> {
            installAsMod = true;
        });

        installButton.addActionListener(action -> {
            String loaderName = installAsMod ? "fabric-loader" : "iris-fabric-loader";

            try {
                URL loaderVersionUrl = new URL("https://raw.githubusercontent.com/IrisShaders/Iris-Installer-Maven/master/latest-loader");
                String loaderVersion = installAsMod ? Main.LOADER_META.getLatestVersion(false).getVersion() : Utils.readTextFile(loaderVersionUrl);
                boolean success = VanillaLauncherIntegration.installToLauncher(getVanillaGameDir(), getInstallDir(), installAsMod ? "Fabric Loader " + selectedVersion : "Iris for " + selectedVersion.name, selectedVersion.name, loaderName, loaderVersion, installAsMod ? VanillaLauncherIntegration.Icon.FABRIC: VanillaLauncherIntegration.Icon.IRIS);
                if (!success) {
                    System.out.println("Failed to install to launcher, canceling!");
                    return;
                }
            } catch (IOException e) {
                System.out.println("Failed to install version and profile to vanilla launcher!");
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to install to vanilla launcher, please contact the Iris support team via Discord! \nError: " + e, "Failed to install to launcher", JOptionPane.ERROR_MESSAGE);
                return;
            }

            File storageDir = getStorageDirectory().toFile();
            if (!storageDir.exists() || !storageDir.isDirectory()) {
                storageDir.mkdir();
            }

            installButton.setText("Downloading");
            progressBar.setValue(0);
            installButton.setEnabled(false);

            String zipName = (betaSelection.isSelected() ? "Iris-Sodium-Beta" : "Iris-Sodium") + "-" + selectedVersion.name + ".zip";

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
                        JOptionPane.showMessageDialog(this,
                                msg, "Download Failed!", JOptionPane.ERROR_MESSAGE, null);
                        installButton.setEnabled(true);
                        installButton.setText("Failed!");
                        return;
                    }

                    installButton.setText("Complete!");

                    boolean cancelled = false;

                    File installDir = getInstallDir().toFile();
                    if (!installDir.exists() || !installDir.isDirectory()) installDir.mkdir();

                    File modsFolder = installAsMod ? getInstallDir().resolve("mods").toFile() : getInstallDir().resolve("iris-reserved").resolve(selectedVersion.name).toFile();
                    File[] modsFolderContents = modsFolder.listFiles();

                    if (modsFolderContents != null) {
                        boolean isEmpty = modsFolderContents.length == 0;

                        if (installAsMod && modsFolder.exists() && modsFolder.isDirectory() && !isEmpty) {
                            int result = JOptionPane.showConfirmDialog(this,"An existing mods folder was found in the selected game directory. Do you want to update/install iris?", "Mods Folder Detected",
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
                                        int result = JOptionPane.showOptionDialog(this,"Optifine was found in your mods folder, but Optifine is incompatible with Iris. Do you want to remove it, or cancel the installation?", "Optifine Detected",
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
                                JOptionPane.showMessageDialog(this, "Failed to remove optifine from your mods folder, please make sure your game is closed and try again!", "Failed to remove optifine", JOptionPane.ERROR_MESSAGE);
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
                                JOptionPane.showMessageDialog(this, "Failed to remove iris and sodium from your mods folder to update them, please make sure your game is closed and try again!", "Failed to prepare mods for update", JOptionPane.ERROR_MESSAGE);
                                cancelled = true;
                            }
                        }
                    }

                    if (cancelled) {
                        installButton.setEnabled(true);
                        return;
                    }

                    if (!modsFolder.exists() || !modsFolder.isDirectory()) modsFolder.mkdir();

                    boolean installSuccess = installFromZip(saveLocation);
                    if (installSuccess) {
                        installButton.setText("Success!");
                        installButton.setEnabled(true);
                        finishedSuccessfulInstall = true;
                    } else {
                        installButton.setText("Failed!");
                        System.out.println("Failed to install to mods folder!");
                        JOptionPane.showMessageDialog(this, "Failed to install to mods folder, please make sure your game is closed and try again!", "Installation Failed!", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            downloader.execute();
        });
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

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        installType = new javax.swing.ButtonGroup();
        irisInstallerLabel = new javax.swing.JLabel();
        installButton = new javax.swing.JButton();
        gameVersionList = new javax.swing.JComboBox<>();
        progressBar = new javax.swing.JProgressBar();
        gameVersionLabel = new javax.swing.JLabel();
        outdatedText1 = new javax.swing.JLabel();
        outdatedText2 = new javax.swing.JLabel();
        betaSelection = new javax.swing.JCheckBox();
        standaloneType = new javax.swing.JRadioButton();
        fabricType = new javax.swing.JRadioButton();
        installationType = new javax.swing.JLabel();
        installationDirectory = new javax.swing.JLabel();
        directoryName = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        irisInstallerLabel.setFont(new java.awt.Font("sansserif", 0, 36)); // NOI18N
        irisInstallerLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/iris_profile_icon.png"))); // NOI18N
        irisInstallerLabel.setText(" Iris Installer");

        installButton.setFont(new java.awt.Font("sansserif", 0, 18)); // NOI18N
        installButton.setText("Install");
        installButton.putClientProperty( "JButton.buttonType", "roundRect" );

        gameVersionList.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1.19", "1.18.2", "1.17.1", "1.16.5" }));

        gameVersionLabel.setFont(new java.awt.Font("sansserif", 0, 18)); // NOI18N
        gameVersionLabel.setText("Select game version:");

        outdatedText1.setFont(new java.awt.Font("sansserif", 0, 16)); // NOI18N
        outdatedText1.setForeground(new java.awt.Color(255, 204, 0));
        outdatedText1.setText(" Warning: We have ended support for <version>.");

        outdatedText2.setFont(new java.awt.Font("sansserif", 0, 16)); // NOI18N
        outdatedText2.setForeground(new java.awt.Color(255, 204, 0));
        outdatedText2.setText(" The Iris version you get will most likely be outdated.");

        betaSelection.setFont(new java.awt.Font("sansserif", 0, 16)); // NOI18N
        betaSelection.setText("Use beta version (not recommended)");

        installType.add(standaloneType);
        standaloneType.setFont(new java.awt.Font("sansserif", 0, 16)); // NOI18N
        standaloneType.setText("Standalone");

        installType.add(fabricType);
        fabricType.setFont(new java.awt.Font("sansserif", 0, 16)); // NOI18N
        fabricType.setText("Fabric/Quilt mod");

        installationType.setFont(new java.awt.Font("sansserif", 0, 18)); // NOI18N
        installationType.setText(" Installation type:");

        installationDirectory.setFont(new java.awt.Font("sansserif", 0, 18)); // NOI18N
        installationDirectory.setText(" Installation directory:");

        directoryName.setText("directory name");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(196, Short.MAX_VALUE)
                .addComponent(irisInstallerLabel)
                .addGap(196, 196, 196))
            .addGroup(layout.createSequentialGroup()
                .addGap(19, 19, 19)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(installButton, javax.swing.GroupLayout.PREFERRED_SIZE, 171, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(26, 26, 26))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(installationType)
                            .addComponent(betaSelection)
                            .addComponent(outdatedText2)
                            .addComponent(outdatedText1)
                            .addComponent(gameVersionLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 227, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(gameVersionList, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(standaloneType)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(fabricType))
                            .addComponent(installationDirectory)
                            .addComponent(directoryName, javax.swing.GroupLayout.PREFERRED_SIZE, 302, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(48, 48, 48)
                .addComponent(irisInstallerLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 75, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(gameVersionLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(gameVersionList, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(betaSelection)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(outdatedText1, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(outdatedText2)
                .addGap(18, 18, 18)
                .addComponent(installationType)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(standaloneType)
                    .addComponent(fabricType))
                .addGap(18, 18, 18)
                .addComponent(installationDirectory)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(directoryName, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(91, 91, 91)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(installButton, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(16, 16, 16))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(31, 31, 31))))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        dark = true;
        System.setProperty("apple.awt.application.appearance", "system");
        if (dark) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }

        System.out.println("Launching installer...");

        /* Create and display the form */
        EventQueue.invokeLater(() -> new NewInstaller().setVisible(true));
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
            if (!installAsMod) {
                getInstallDir().resolve("iris-reserved/").toFile().mkdir();
            }
            while (entry != null) {
                String entryName = entry.getName();

                if (!installAsMod && entryName.startsWith("mods/")) {
                    entryName = entryName.replace("mods/", "iris-reserved/" + selectedVersion + "/");
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
            e.printStackTrace();
            return false;
        }
    }
}
