package com.cylorun.io.sheets;

import com.cylorun.Tracker;
import com.cylorun.io.TrackerOptions;
import com.cylorun.utils.ExceptionUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Collections;

public class GoogleSheetsService {
    public static final String CREDENTIALS_FILE = TrackerOptions.getTrackerDir().resolve("credentials.json").toString();


    public static Sheets getSheetsService() throws IOException, GeneralSecurityException {
        if (!new File(CREDENTIALS_FILE).exists()) {
            Tracker.log(Level.ERROR, "MISSING credentials.json file");
            ExceptionUtil.showDialogAndExit("Missing credentials.json file, cannot upload to sheets");
            return null;
        }

        GoogleCredential credential = GoogleCredential
                .fromStream(new FileInputStream(CREDENTIALS_FILE))
                .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));

        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                credential
        ).setApplicationName("LiveTrackerService").build();
    }
}
