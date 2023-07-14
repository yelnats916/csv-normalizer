package com.normalizer;

import java.util.Map;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

import java.nio.charset.StandardCharsets;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

import org.apache.commons.lang3.StringUtils;

public class Normalizer {
    public enum Headers {
        Timestamp,
        Address,
        ZIP,
        Fullname,
        FooDuration,
        BarDuration,
        TotalDuration,
        Notes
    }
      
    public static void main(String[] args) {
    
        // TODO: validate input params
        // validateArgs(args);
        String inputFile = args[0];
        String outputFile = args[1];
        
        try {
            FileReader in = new FileReader(inputFile);
            CSVFormat format = CSVFormat.Builder.create().setHeader(Headers.class).setSkipHeaderRecord(true).build();
            CSVParser csvParser = format.parse(in);
            
            FileWriter fileWriter = new FileWriter(outputFile, true);
            CSVPrinter csvPrinter = new CSVPrinter(fileWriter, CSVFormat.Builder.create().setHeader(Headers.class).build());
            
            CSVPrinter errorCsvPrinter = null;
            boolean errorFileCreated = false;
            
            for (CSVRecord record : csvParser) {
               try {
                  Map<String, String> map = record.toMap();
                  
                  String timestamp = formatTimestamp(record.get(Headers.Timestamp));
                  map.put(Headers.Timestamp.toString(), timestamp);

                  String address = decodeText(record.get(Headers.Address));
                  map.put(Headers.Address.toString(), address);
                  
                  String zip = formatZip(record.get(Headers.ZIP));
                  map.put(Headers.ZIP.toString(), zip);
                  
                  String fullName = decodeText(record.get(Headers.Fullname)).toUpperCase();
                  map.put(Headers.Fullname.toString(), fullName);
                                   
                  int fooDuration = formatDuration(record.get(Headers.FooDuration));
                  int barDuration = formatDuration(record.get(Headers.BarDuration));
                  int totalDuration = fooDuration + barDuration;
                  
                  map.put(Headers.FooDuration.toString(), String.valueOf(fooDuration));
                  map.put(Headers.BarDuration.toString(), String.valueOf(barDuration));
                  map.put(Headers.TotalDuration.toString(), String.valueOf(totalDuration));
                  
                  String notes = decodeText(record.get(Headers.Notes));
                  map.put(Headers.Notes.toString(), notes);
                  
                  csvPrinter.printRecord(map.values());
               }  
               catch (InvalidDataException ex) {
                  if (!errorFileCreated) {
                     // TODO: optional 3rd input param for errorfile name, else default 
                     FileWriter errorFileWriter = new FileWriter("errorFile", true);
                     errorCsvPrinter = new CSVPrinter(errorFileWriter, CSVFormat.DEFAULT);
                     errorFileCreated = true;
                  }
                  // TODO: log error message instead of printing out
                  System.out.println("Invalid data for input line " + record.getRecordNumber());
                  System.out.println(ex.getMessage());
                  errorCsvPrinter.printRecord(record);
                  continue;
               }
            }
            
            if (errorFileCreated) {
               errorCsvPrinter.close();
            }
            csvPrinter.close();

        }
        catch (FileNotFoundException ex) {
            System.out.println(ex);
        }
        catch (IOException ex) {
            System.out.println(ex);
        }
    
        return;
    }
    
    public static String decodeText(String input) throws IOException {
          CharsetDecoder charsetDecoder = StandardCharsets.UTF_8.newDecoder();
          charsetDecoder.onMalformedInput(CodingErrorAction.REPLACE);
      return new BufferedReader(
         new InputStreamReader(
         new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), charsetDecoder)).readLine();
    }
    
    public static String formatTimestamp(String timestamp) throws InvalidDataException {
        // Note: update to validate timestamp field and throw invalidDataException
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yy h:mm:ss a").withZone(ZoneId.of("America/Los_Angeles"));
        try {       
           ZonedDateTime zonedDateTime = ZonedDateTime.parse(timestamp, formatter);
           // convert to EST time
           ZonedDateTime estZonedDateTime = zonedDateTime.withZoneSameInstant(ZoneId.of("US/Eastern"));
           
           // convert to RFC 3339 format
           DateTimeFormatter isoDateTimeFormatter = new DateTimeFormatterBuilder()
              .append(DateTimeFormatter.ISO_OFFSET_DATE_TIME).toFormatter();
           return isoDateTimeFormatter.format(estZonedDateTime);
        } catch (DateTimeParseException ex) {
           throw new InvalidDataException("Invalid timestamp format");
        }
    }
    
    public static int formatDuration(String duration) throws InvalidDataException {
        // duration format is HH:MM:SS.MS
        // substring removes millisecond portion of string
        try { 
           String[] durationSplit = duration.substring(0, duration.indexOf(".")).split(":");
           int totalSeconds = 
               (Integer.parseInt(durationSplit[0]) * 60 * 60) +
               (Integer.parseInt(durationSplit[1]) * 60) +
               (Integer.parseInt(durationSplit[2]));
           return totalSeconds;
        } catch (Exception ex) {
           // update to use validate method instead of catch-all
           throw new InvalidDataException("Invalid duration format");
        }
    }
    
    public static String formatZip(String zip) throws InvalidDataException {
        validateZip(zip);
        return StringUtils.leftPad(zip, 5, '0');
    }
    
    public static void validateZip(String zip) throws InvalidDataException {
        if (zip.isEmpty()) {
            throw new InvalidDataException("Zip code cannot be empty");
        }
        if (zip.length() > 5) {
            throw new InvalidDataException("Zip code cannot be greater than 5 digits");
        }
        if (!zip.matches("\\d+")) {
            throw new InvalidDataException("Zip code must only contain digits");
        }
    }

}

class InvalidDataException extends Exception {
    private String errorMsg;

    InvalidDataException(String errorMsg) {
        this.errorMsg = errorMsg;
    }
    
    public String getMessage() { 
        return errorMsg;
    }
}

