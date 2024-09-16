package com.cylorun.io.sheets;

import com.cylorun.Tracker;
import com.cylorun.io.TrackerOptions;
import com.cylorun.utils.ResourceUtil;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

public class GoogleSheetsClient {
    private static boolean hasSetup = false;

    public static void setup() {
        TrackerOptions options = TrackerOptions.getInstance();
        if (options.gen_labels && !GoogleSheetsClient.hasSetup && isValidSheet(options.sheet_id, options.sheet_name)) {
            GoogleSheetsClient.generateLabels();
            GoogleSheetsClient.hasSetup = true;
        }
    }

    public static boolean isValidSheet(String id, String name) {
        try {
            GoogleSheetsService.getSheetsService().spreadsheets().values()
                    .get(id.trim(), name + "!A1:B")
                    .execute();
        } catch (NullPointerException | IOException | GeneralSecurityException a) {
            Tracker.log(Level.ERROR, "Invalid sheet_id or sheet_name");
            return false;
        }
        return true;
    }

    public static void generateLabels() {
        List<Object> headers = ResourceUtil.getHeaderLabels();

        try {
            GoogleSheetsClient.insert(headers, 2, true);
            Tracker.log(Level.INFO, "Generated header labels");
        } catch (GeneralSecurityException | IOException e) {
            Tracker.log(Level.ERROR, "Failed to generate google sheets headers: " + e.getMessage());
        }
    }

    public static void appendRowTop(Map<String, Object> rowData) throws IOException, GeneralSecurityException {
        List<Object> headers =  GoogleSheetsClient.getSheetHeaders();

        List<Object> rowList =  GoogleSheetsClient.convertMapToList(rowData, headers);
        GoogleSheetsClient.insert(rowList, 4, false);
    }

    private static List<Object> getSheetHeaders() throws IOException, GeneralSecurityException {
        Sheets sheetsService = GoogleSheetsService.getSheetsService();
        String sheetId = TrackerOptions.getInstance().sheet_id.trim();
        String sheetName = TrackerOptions.getInstance().sheet_name;
        String range = sheetName + "!A1:CZ1";

        ValueRange response = sheetsService.spreadsheets().values()
                .get(sheetId, range)
                .execute();
        return response.getValues().get(0);
    }

    private static List<Object> convertMapToList(Map<String, Object> map, List<Object> headers) {
        List<Object> rowList = new ArrayList<>(Arrays.asList(new Object[headers.size()]));
        for (int i = 0; i < headers.size(); i++) {
            String header = (String) headers.get(i);
            rowList.set(i, map.getOrDefault(header, ""));
        }
        return rowList;
    }

    public static void insert(List<Object> rowData, int row, boolean overwrite) throws GeneralSecurityException, IOException {
        Sheets sheetsService = GoogleSheetsService.getSheetsService();
        String sheetName = TrackerOptions.getInstance().sheet_name;
        String sheetId = TrackerOptions.getInstance().sheet_id.strip();

        Spreadsheet spreadsheet = sheetsService.spreadsheets().get(sheetId).execute();
        Integer sheetIdInt = null;
        for (Sheet sheet : spreadsheet.getSheets()) {
            if (sheet.getProperties().getTitle().equals(sheetName)) {
                sheetIdInt = sheet.getProperties().getSheetId();
                break;
            }
        }

        if (sheetIdInt == null) {
            throw new RuntimeException("Sheet with name '" + sheetName + "' not found.");
        }

        String range = String.format("%s!A%s:CZ", sheetName, row);

        if (overwrite) {
            ValueRange newRow = new ValueRange().setValues(Collections.singletonList(rowData));
            UpdateValuesResponse res = sheetsService.spreadsheets().values()
                    .update(sheetId, range, newRow)
                    .setValueInputOption("RAW")
                    .execute();
            return;
        }

        Request insertRowRequest = new Request()
                .setInsertDimension(new InsertDimensionRequest()
                        .setRange(new DimensionRange()
                                .setSheetId(sheetIdInt)
                                .setDimension("ROWS")
                                .setStartIndex(row - 1)
                                .setEndIndex(row))
                        .setInheritFromBefore(false));

        Request copyPasteRequest = new Request()
                .setCopyPaste(new CopyPasteRequest()
                        .setSource(new GridRange()
                                .setSheetId(sheetIdInt)
                                .setStartRowIndex(row - 1)
                                .setEndRowIndex(row)
                                .setStartColumnIndex(0)
                                .setEndColumnIndex(103)) //  CZ column
                        .setDestination(new GridRange()
                                .setSheetId(sheetIdInt)
                                .setStartRowIndex(row)
                                .setEndRowIndex(row + 1)
                                .setStartColumnIndex(0)
                                .setEndColumnIndex(103)) //  CZ column
                        .setPasteType("PASTE_FORMAT"));

        BatchUpdateSpreadsheetRequest batchRequest = new BatchUpdateSpreadsheetRequest()
                .setRequests(Arrays.asList(insertRowRequest, copyPasteRequest));
        sheetsService.spreadsheets().batchUpdate(sheetId, batchRequest).execute();

        ValueRange newRow = new ValueRange().setValues(Collections.singletonList(rowData));
        UpdateValuesResponse result = sheetsService.spreadsheets().values()
                .update(sheetId, range, newRow)
                .setValueInputOption("RAW")
                .execute();
    }

    public static Integer getSheetIdByName(Sheets sheetsService, String spreadsheetId, String sheetName) throws IOException {
        Spreadsheet spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).execute();
        for (Sheet sheet : spreadsheet.getSheets()) {
            if (sheet.getProperties().getTitle().equals(sheetName)) {
                return sheet.getProperties().getSheetId();
            }
        }
        return null;
    }

    public static void deleteRow(int rowIndex) throws IOException, GeneralSecurityException {
        TrackerOptions options = TrackerOptions.getInstance();
        Sheets service = GoogleSheetsService.getSheetsService();
        Integer sheetId = getSheetIdByName(service, options.sheet_id, options.sheet_name);

        if (sheetId == null) {
            Tracker.log(Level.WARN, "Sheet not found! " + options.sheet_name);
            return;
        }

        DeleteDimensionRequest deleteRequest = new DeleteDimensionRequest()
                .setRange(new DimensionRange()
                        .setSheetId(sheetId)
                        .setDimension("ROWS")
                        .setStartIndex(rowIndex)
                        .setEndIndex(rowIndex + 1));

        Request request = new Request().setDeleteDimension(deleteRequest);

        BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest()
                .setRequests(Collections.singletonList(request));

        service.spreadsheets()
                .batchUpdate(options.sheet_id, batchUpdateRequest)
                .execute();

    }
}
