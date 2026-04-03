package com.cylorun.utils;

import com.cylorun.Tracker;
import com.cylorun.gui.components.ProgressBar;
import com.github.tuupertunut.powershelllibjava.PowerShellExecutionException;
import org.apache.logging.log4j.Level;
import org.kohsuke.github.GHAsset;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHRepository;

import javax.swing.*;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class UpdateUtil {

    public static GHAsset getJarFromRelease(GHRelease release) throws IOException {
        GHAsset asset = null;
        for (GHAsset listAsset : release.listAssets()) {
            if (listAsset.getBrowserDownloadUrl().endsWith(".jar")) {
                asset = listAsset;
                break;
            }
        }

        return asset;
    }

    public static GHAsset getLatestJar() throws IOException {
        GHRelease release = GithubUtil.getGithub().getRepository("cylorun/live-tracker").listReleases().toList().get(0);
        return getJarFromRelease(release);
    }

    public static boolean shouldUpdate(String currentVersion) throws IOException {
        if (currentVersion.equals("DEV")) {
            Tracker.log(Level.INFO, "No updates in a DEV environment");
            return false;
        }

        GHRepository repository = GithubUtil.getGithub().getRepository("cylorun/live-tracker");
        List<GHRelease> releases = repository.listReleases().toList();

        if (releases.isEmpty()) {
            Tracker.log(Level.INFO, "No releases found for the repository");
            return false;
        }

        GHRelease release = releases.get(0);
        String latestVersion = release.getTagName();

        if (latestVersion == null || latestVersion.isEmpty()) {
            Tracker.log(Level.WARN, "Latest release tag name is null or empty");
            return false;
        }

        GHAsset asset = getJarFromRelease(release);
        if (asset == null || asset.getBrowserDownloadUrl() == null) {
            Tracker.log(Level.WARN, "Latest release does not have a .jar asset");
            return false;
        }

        return !latestVersion.equals(currentVersion);
    }


    public static void checkForUpdates(String currVersion) {
        try {
            if (shouldUpdate(currVersion)) {
                int choice = JOptionPane.showConfirmDialog(null, "A new update was found, want to update?", "New update", JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    GHAsset latestJar = getLatestJar();
                    UpdateUtil.update(latestJar);
                }
            } else {
                Tracker.log(Level.INFO, "No new updates found.");
            }
        } catch (IOException | PowerShellExecutionException e) {
            Tracker.log(Level.ERROR, "Something went wrong while trying to check for an update\n" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static void update(GHAsset asset) throws PowerShellExecutionException, IOException {
        Path downloadPath = Tracker.getSourcePath().resolveSibling(asset.getName());
        Path javaExe = Paths.get(System.getProperty("java.home")).resolve("bin").resolve("javaw.exe");
        String powerCommand = String.format("start-process '%s' '-jar \"%s\" -deleteOldJar \"%s\"'", javaExe, downloadPath, Tracker.getSourcePath());

        downloadAsset(asset, downloadPath);

        PowerShellUtil.execute(powerCommand);
        System.exit(0);
    }


    private static void downloadAsset(GHAsset asset, Path dowloadPath) throws IOException {
        String downloadUrl = asset.getBrowserDownloadUrl();
        ProgressBar progressBar = new ProgressBar((int) Math.min(asset.getSize(), Integer.MAX_VALUE), 0, "Downloading "+asset.getName());
        try (BufferedInputStream in = new BufferedInputStream(new URL(downloadUrl).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(dowloadPath.toFile())) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
                progressBar.setValue(progressBar.getValue() + bytesRead);
            }
        } catch (IOException e) {
            Tracker.log(Level.ERROR, "Failed to download asset: " + e.getMessage());
        }
    }

}
