package me.cylorun.io.sheets;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import me.cylorun.io.TrackerOptions;
import me.cylorun.utils.ExceptionUtil;
import me.cylorun.utils.ResourceUtil;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static me.cylorun.io.sheets.GoogleSheetsService.getSheetsService;

public class GoogleSheetsClient {

    public static void setup(){
        TrackerOptions options = TrackerOptions.getInstance();
        if (options.gen_labels) {
            generateLabels();
        }
    }


    public static void generateLabels() {
        List<Object> headers = ResourceUtil.getHeaderLabels();

        try {
            insert(headers, 1, true);
        } catch (GeneralSecurityException | IOException e) {
            ExceptionUtil.showError(e);
            throw new RuntimeException(e);
        }
    }

    public static void appendRowTop(List<Object> rowData) throws IOException, GeneralSecurityException {
        insert(rowData, 3, false);
    }

    public static void insert(List<Object> rowData, int row, boolean overwrite) throws GeneralSecurityException, IOException {
        Sheets sheetsService = getSheetsService();
        String sheetName = TrackerOptions.getInstance().sheet_name;
        String sheetId = TrackerOptions.getInstance().sheet_id;
        String range = String.format("A%s:CN", row);

        if (overwrite) {
            ValueRange newRow = new ValueRange().setValues(Arrays.asList(rowData));
            UpdateValuesResponse res = sheetsService.spreadsheets().values()
                    .update(sheetId, range, newRow)
                    .setValueInputOption("RAW")
                    .execute();
            return;
        }

        ValueRange response = sheetsService.spreadsheets().values()
                .get(sheetId, sheetName + "!" + range)
                .execute();
        List<List<Object>> values = response.getValues();

        List<List<Object>> newValues = new ArrayList<>();
        newValues.add(rowData);
        if (values != null) {
            newValues.addAll(values);
        }

        ValueRange body = new ValueRange().setValues(newValues);
        UpdateValuesResponse result = sheetsService.spreadsheets().values()
                .update(sheetId, sheetName + "!" + range.split(":")[0], body)
                .setValueInputOption("RAW")
                .execute();
    }
}
