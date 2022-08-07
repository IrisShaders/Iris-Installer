/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package net.hypercubemc.iris_installer;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.event.ItemEvent;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import net.fabricmc.installer.Main;
import net.fabricmc.installer.util.MetaHandler;
import net.fabricmc.installer.util.Reference;
import net.fabricmc.installer.util.Utils;
import org.json.JSONException;

/**
 *
 * @author ims
 */
@SuppressWarnings("serial")
public class NewInstaller extends javax.swing.JFrame {

    private static boolean dark = false;
    private boolean installAsMod;
    private String outdatedPlaceholder = "Warning: We have ended support for <version>.";
    private String BASE_URL = "https://raw.githubusercontent.com/IrisShaders/Iris-Installer-Files/master/";
    private boolean finishedSuccessfulInstall;
    private InstallerMeta.Version selectedVersion;
    private final List<InstallerMeta.Version> GAME_VERSIONS;
    private final InstallerMeta INSTALLER_META;
    private Path customInstallDir;

    /**
     * Creates new form Installer
     */
    public NewInstaller() {
        super("Iris Installer");
        Main.LOADER_META = new MetaHandler(Reference.getMetaServerEndpoint("v2/versions/loader"));

        try {
            Main.LOADER_META.load();
        } catch (IOException e) {
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

        GAME_VERSIONS = INSTALLER_META.getVersions();
        Collections.reverse(GAME_VERSIONS);
        selectedVersion = GAME_VERSIONS.get(0);

        initComponents();

        betaSelection.setText("Use " + INSTALLER_META.getBetaSnippet() + " beta version (not recommended)");

        // Change outdated version text color based on dark mode
        if (!dark) {
            Color newTextColor = new Color(154, 136, 63, 255);

            outdatedText1.setForeground(newTextColor);
            outdatedText2.setForeground(newTextColor);
        }

        if (!INSTALLER_META.hasBeta()) {
            betaSelection.setVisible(false);
        }

        gameVersionList.removeAllItems();

        for (InstallerMeta.Version version : GAME_VERSIONS) {
            gameVersionList.addItem(version.name);
        }

        // Set default dir (.minecraft)
        directoryName.setText(getDefaultInstallDir().toFile().getName());

        // Hide outdated version text
        outdatedText1.setVisible(false);
        outdatedText2.setVisible(false);
    }

    public Path getStorageDirectory() {
        return getAppDataDirectory().resolve(getStorageDirectoryName());
    }

    public Path getInstallDir() {
        return customInstallDir != null ? customInstallDir : getDefaultInstallDir();
    }

    public Path getAppDataDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return new File(System.getenv("APPDATA")).toPath();
        } else if (os.contains("mac")) {
            return new File(System.getProperty("user.home") + "/Library/Application Support").toPath();
        } else if (os.contains("nux")) {
            return new File(System.getProperty("user.home")).toPath();
        } else {
            return new File(System.getProperty("user.dir")).toPath();
        }
    }

    public String getStorageDirectoryName() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            return "iris-installer";
        } else {
            return ".iris-installer";
        }
    }

    private Path getDefaultInstallDir() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("mac")) {
            return getAppDataDirectory().resolve("minecraft");
        } else {
            return getAppDataDirectory().resolve(".minecraft");
        }
    }

    public Path getVanillaGameDir() {
        String os = System.getProperty("os.name").toLowerCase();

        return os.contains("mac") ? getAppDataDirectory().resolve("minecraft") : getAppDataDirectory().resolve(".minecraft");
    }

    public boolean installFromZip(File zip) {
        try {
            int BUFFER_SIZE = 2048; // Buffer Size
            try ( ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zip))) {
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
                        try ( // if the entry is a file, extracts it
                                 BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
                            byte[] bytesIn = new byte[BUFFER_SIZE];
                            int read = 0;
                            while ((read = zipIn.read(bytesIn)) != -1) {
                                bos.write(bytesIn, 0, read);
                            }
                        }
                    } else {
                        // if the entry is a directory, make the directory
                        filePath.mkdir();
                    }
                    zipIn.closeEntry();
                    entry = zipIn.getNextEntry();
                }
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        installType = new javax.swing.ButtonGroup();
        irisInstallerLabel = new javax.swing.JLabel();
        gameVersionLabel = new javax.swing.JLabel();
        outdatedText1 = new javax.swing.JLabel();
        outdatedText2 = new javax.swing.JLabel();
        installationType = new javax.swing.JLabel();
        installationDirectory = new javax.swing.JLabel();
        installationTypesContainer = new javax.swing.JPanel();
        standaloneType = new javax.swing.JRadioButton();
        fabricType = new javax.swing.JRadioButton();
        gameVersionList = new javax.swing.JComboBox<>();
        betaSelection = new javax.swing.JCheckBox();
        directoryName = new javax.swing.JButton();
        progressBar = new javax.swing.JProgressBar();
        installButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setIconImage(new ImageIcon(Objects.requireNonNull(Utils.class.getClassLoader().getResource("iris_profile_icon.png"))).getImage());
        setMaximumSize(new java.awt.Dimension(480, 550));
        setMinimumSize(new java.awt.Dimension(480, 550));
        setPreferredSize(new java.awt.Dimension(480, 550));
        setResizable(false);
        getContentPane().setLayout(new java.awt.GridBagLayout());

        irisInstallerLabel.setFont(new java.awt.Font("Arial", 0, 36)); // NOI18N
        irisInstallerLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        irisInstallerLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/iris_profile_icon.png"))); // NOI18N
        irisInstallerLabel.setText(" Iris & Sodium");
        irisInstallerLabel.setMaximumSize(new java.awt.Dimension(350, 64));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(30, 0, 0, 0);
        getContentPane().add(irisInstallerLabel, gridBagConstraints);

        gameVersionLabel.setFont(new java.awt.Font("Arial", 0, 16)); // NOI18N
        gameVersionLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        gameVersionLabel.setText("Select game version:");
        gameVersionLabel.setToolTipText("");
        gameVersionLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        gameVersionLabel.setMaximumSize(new java.awt.Dimension(300, 24));
        gameVersionLabel.setMinimumSize(new java.awt.Dimension(168, 24));
        gameVersionLabel.setPreferredSize(new java.awt.Dimension(168, 24));
        gameVersionLabel.setRequestFocusEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(20, 0, 0, 0);
        getContentPane().add(gameVersionLabel, gridBagConstraints);

        outdatedText1.setFont(new java.awt.Font("Arial", 0, 16)); // NOI18N
        outdatedText1.setForeground(new java.awt.Color(255, 204, 0));
        outdatedText1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        outdatedText1.setText("Warning: We have ended support for <version>.");
        outdatedText1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        outdatedText1.setMaximumSize(new java.awt.Dimension(400, 21));
        outdatedText1.setMinimumSize(new java.awt.Dimension(310, 21));
        outdatedText1.setPreferredSize(new java.awt.Dimension(310, 21));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        getContentPane().add(outdatedText1, gridBagConstraints);

        outdatedText2.setFont(new java.awt.Font("Arial", 0, 16)); // NOI18N
        outdatedText2.setForeground(new java.awt.Color(255, 204, 0));
        outdatedText2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        outdatedText2.setText("The Iris version you get will most likely be outdated.");
        outdatedText2.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        outdatedText2.setMaximumSize(new java.awt.Dimension(450, 21));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(outdatedText2, gridBagConstraints);

        installationType.setFont(new java.awt.Font("Arial", 0, 16)); // NOI18N
        installationType.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        installationType.setText(" Installation type:");
        installationType.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        installationType.setMaximumSize(new java.awt.Dimension(300, 24));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
        getContentPane().add(installationType, gridBagConstraints);

        installationDirectory.setFont(new java.awt.Font("Arial", 0, 16)); // NOI18N
        installationDirectory.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        installationDirectory.setText("Installation directory:");
        installationDirectory.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        installationDirectory.setMaximumSize(new java.awt.Dimension(300, 24));
        installationDirectory.setMinimumSize(new java.awt.Dimension(165, 24));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
        getContentPane().add(installationDirectory, gridBagConstraints);

        installationTypesContainer.setLayout(new java.awt.BorderLayout(10, 0));

        installType.add(standaloneType);
        standaloneType.setFont(new java.awt.Font("Arial", 0, 16)); // NOI18N
        standaloneType.setSelected(true);
        standaloneType.setText("Iris Install");
        standaloneType.setToolTipText("This installs Iris and Sodium by itself, without any mods.");
        standaloneType.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                standaloneTypeMouseClicked(evt);
            }
        });
        installationTypesContainer.add(standaloneType, java.awt.BorderLayout.LINE_START);

        installType.add(fabricType);
        fabricType.setFont(new java.awt.Font("Arial", 0, 16)); // NOI18N
        fabricType.setText("Fabric Install");
        fabricType.setToolTipText("This installs Iris and Sodium alongside an installation of Fabric.");
        fabricType.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fabricTypeMouseClicked(evt);
            }
        });
        installationTypesContainer.add(fabricType, java.awt.BorderLayout.LINE_END);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        getContentPane().add(installationTypesContainer, gridBagConstraints);

        gameVersionList.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        gameVersionList.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "1.19", "1.18.2", "1.17.1", "1.16.5" }));
        gameVersionList.setMaximumSize(new java.awt.Dimension(168, 35));
        gameVersionList.setMinimumSize(new java.awt.Dimension(168, 35));
        gameVersionList.setPreferredSize(new java.awt.Dimension(168, 35));
        gameVersionList.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                gameVersionListItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        getContentPane().add(gameVersionList, gridBagConstraints);

        betaSelection.setFont(new java.awt.Font("Arial", 0, 16)); // NOI18N
        betaSelection.setText("Use beta version (not recommended)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        getContentPane().add(betaSelection, gridBagConstraints);

        directoryName.setFont(new java.awt.Font("Arial", 0, 16)); // NOI18N
        directoryName.setLabel("Directory Name");
        directoryName.setMaximumSize(new java.awt.Dimension(300, 36));
        directoryName.setMinimumSize(new java.awt.Dimension(300, 36));
        directoryName.setPreferredSize(new java.awt.Dimension(300, 36));
        directoryName.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                directoryNameMouseClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        getContentPane().add(directoryName, gridBagConstraints);

        progressBar.setFont(new java.awt.Font("Arial", 0, 16)); // NOI18N
        progressBar.setAlignmentX(0.0F);
        progressBar.setAlignmentY(0.0F);
        progressBar.setMaximumSize(new java.awt.Dimension(380, 25));
        progressBar.setMinimumSize(new java.awt.Dimension(380, 25));
        progressBar.setPreferredSize(new java.awt.Dimension(380, 25));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.insets = new java.awt.Insets(40, 0, 0, 0);
        getContentPane().add(progressBar, gridBagConstraints);

        installButton.setFont(new java.awt.Font("Arial", 0, 16)); // NOI18N
        installButton.setText("Install");
        installButton.setToolTipText("");
        installButton.setMargin(new java.awt.Insets(15, 60, 15, 60));
        installButton.setMaximumSize(new java.awt.Dimension(300, 45));
        installButton.setMinimumSize(new java.awt.Dimension(170, 45));
        installButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                installButtonMouseClicked(evt);
            }
        });
        installButton.putClientProperty( "JButton.buttonType", "roundRect" );
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 30, 0);
        getContentPane().add(installButton, gridBagConstraints);

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void directoryNameMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_directoryNameMouseClicked
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setFileHidingEnabled(false);

        int option = fileChooser.showOpenDialog(this);

        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            customInstallDir = file.toPath();
            directoryName.setText(file.getName());
        }
    }//GEN-LAST:event_directoryNameMouseClicked

    private void gameVersionListItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_gameVersionListItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            selectedVersion = GAME_VERSIONS.stream().filter(v -> v.name.equals(evt.getItem())).findFirst().orElse(GAME_VERSIONS.get(0));

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
    }//GEN-LAST:event_gameVersionListItemStateChanged

    private void standaloneTypeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_standaloneTypeMouseClicked
        installAsMod = false;
    }//GEN-LAST:event_standaloneTypeMouseClicked

    private void fabricTypeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fabricTypeMouseClicked
        installAsMod = true;
    }//GEN-LAST:event_fabricTypeMouseClicked

    private void installButtonMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_installButtonMouseClicked
        String loaderName = installAsMod ? "fabric-loader" : "iris-fabric-loader";

        try {
            URL loaderVersionUrl = new URL("https://raw.githubusercontent.com/IrisShaders/Iris-Installer-Maven/master/latest-loader");
            String profileName = installAsMod ? "Fabric Loader " : "Iris & Sodium for ";
            VanillaLauncherIntegration.Icon profileIcon = installAsMod ? VanillaLauncherIntegration.Icon.FABRIC : VanillaLauncherIntegration.Icon.IRIS;
            String loaderVersion = installAsMod ? Main.LOADER_META.getLatestVersion(false).getVersion() : Utils.readTextFile(loaderVersionUrl);
            boolean success = VanillaLauncherIntegration.installToLauncher(getVanillaGameDir(), getInstallDir(), profileName + selectedVersion.name, selectedVersion.name, loaderName, loaderVersion, profileIcon);
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

        installButton.setText("Downloading...");
        installButton.setEnabled(false);
        progressBar.setForeground(new Color(76, 135, 200));
        progressBar.setValue(0);

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
                    installButton.setEnabled(true);
                    installButton.setText("Download Failed!");
                    progressBar.setForeground(new Color(204, 0, 0));
                    progressBar.setValue(100);
                    JOptionPane.showMessageDialog(this,
                            msg, "Download Failed!", JOptionPane.ERROR_MESSAGE, null);
                    return;
                }

                installButton.setText("Download Complete!");

                boolean cancelled = false;

                File installDir = getInstallDir().toFile();
                if (!installDir.exists() || !installDir.isDirectory()) {
                    installDir.mkdir();
                }

                File modsFolder = installAsMod ? getInstallDir().resolve("mods").toFile() : getInstallDir().resolve("iris-reserved").resolve(selectedVersion.name).toFile();
                File[] modsFolderContents = modsFolder.listFiles();

                if (modsFolderContents != null) {
                    boolean isEmpty = modsFolderContents.length == 0;

                    if (installAsMod && modsFolder.exists() && modsFolder.isDirectory() && !isEmpty) {
                        int result = JOptionPane.showConfirmDialog(this, "An existing mods folder was found in the selected game directory. Do you want to update/install iris?", "Mods Folder Detected",
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
                                    int result = JOptionPane.showOptionDialog(this, "Optifine was found in your mods folder, but Optifine is incompatible with Iris. Do you want to remove it, or cancel the installation?", "Optifine Detected",
                                            JOptionPane.DEFAULT_OPTION,
                                            JOptionPane.WARNING_MESSAGE, null, new String[]{"Yes", "Cancel"}, "Yes");

                                    shownOptifineDialog = true;
                                    if (result != JOptionPane.YES_OPTION) {
                                        cancelled = true;
                                        break;
                                    }
                                }

                                if (!mod.delete()) {
                                    failedToRemoveOptifine = true;
                                }
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
                                if (!mod.delete()) {
                                    failedToRemoveIrisOrSodium = true;
                                }
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

                if (!modsFolder.exists() || !modsFolder.isDirectory()) {
                    modsFolder.mkdir();
                }

                boolean installSuccess = installFromZip(saveLocation);

                if (installSuccess) {
                    installButton.setText("Completed!");
                    progressBar.setForeground(new Color(39, 195, 75));
                    installButton.setEnabled(true);
                    finishedSuccessfulInstall = true;
                } else {
                    installButton.setText("Failed!");
                    progressBar.setForeground(new Color(204, 0, 0));
                    System.out.println("Failed to install to mods folder!");
                    JOptionPane.showMessageDialog(this, "Failed to install to mods folder, please make sure your game is closed and try again!", "Installation Failed!", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        downloader.execute();
    }//GEN-LAST:event_installButtonMouseClicked

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        dark = DarkModeDetector.isDarkMode();

        System.setProperty("apple.awt.application.appearance", "system");

        if (dark) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }

        System.out.println("Launching installer...");

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new NewInstaller().setVisible(true));
    }

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
    private javax.swing.JPanel installationTypesContainer;
    private javax.swing.JLabel irisInstallerLabel;
    private javax.swing.JLabel outdatedText1;
    private javax.swing.JLabel outdatedText2;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JRadioButton standaloneType;
    // End of variables declaration//GEN-END:variables
}
