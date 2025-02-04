package org.skd.loadcode;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class CsvDataset {
    private final List<Map<String, String>> dataRows; // List of rows (each row is a Map of columnName -> value)
    private final AtomicInteger currentIndex = new AtomicInteger(0);
    private final int batchSize;
    private final boolean loop;
    private final boolean shuffle;
    private final String delimiter;

    // Main constructor with configurable delimiter
    public CsvDataset(String filePath, int batchSize, boolean loop, boolean shuffle, String delimiter) throws IOException {
        this.dataRows = loadCsv(filePath, delimiter);
        this.batchSize = batchSize;
        this.loop = loop;
        this.shuffle = shuffle;
        this.delimiter = delimiter;

        if (shuffle) {
            Collections.shuffle(dataRows); // Randomize order if required
        }
    }

    // Constructor with default delimiter (",")
    public CsvDataset(String filePath, int batchSize, boolean loop, boolean shuffle) throws IOException {
        this(filePath, batchSize, loop, shuffle, ",");
    }

    // Constructor with batchSize = 1, shuffle = false, default delimiter
    public CsvDataset(String filePath, boolean loop) throws IOException {
        this(filePath, 1, loop, false, ",");
    }

    private List<Map<String, String>> loadCsv(String filePath, String delimiter) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String[] headers = br.readLine().split(delimiter); // Read header row

            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(delimiter);
                if (values.length != headers.length) continue; // Skip invalid rows

                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    row.put(headers[i].trim(), values[i].trim());
                }
                rows.add(row);
            }
        }
        return rows;
    }

    public synchronized List<Map<String, String>> getNextBatch() {
        if (dataRows.isEmpty()) return Collections.emptyList();

        int startIdx = currentIndex.getAndAdd(batchSize);
        int endIdx = Math.min(startIdx + batchSize, dataRows.size());

        if (startIdx >= dataRows.size()) {
            if (loop) {
                currentIndex.set(0);
                return getNextBatch(); // Restart from beginning
            } else {
                return Collections.emptyList(); // No more data
            }
        }

        return dataRows.subList(startIdx, endIdx);
    }

    public synchronized Map<String, String> getNextRow() {
        List<Map<String, String>> batch = getNextBatch();
        return batch.isEmpty() ? null : batch.get(0);
    }

    // Column filtering support
    public synchronized List<Map<String, String>> getNextBatch(List<String> columns) {
        return getNextBatch().stream()
                .map(row -> row.entrySet().stream()
                        .filter(entry -> columns.contains(entry.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .collect(Collectors.toList());
    }

    public synchronized Map<String, String> getNextRow(List<String> columns) {
        List<Map<String, String>> batch = getNextBatch(columns);
        return batch.isEmpty() ? null : batch.get(0);
    }

    public synchronized void reset() {
        currentIndex.set(0);
        if (shuffle) {
            Collections.shuffle(dataRows);
        }
    }

    // Write back to CSV
    public synchronized void writeCsv(String filePath) throws IOException {
        if (dataRows.isEmpty()) return;

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            // Write header
            bw.write(String.join(delimiter, dataRows.get(0).keySet()));
            bw.newLine();

            // Write data
            for (Map<String, String> row : dataRows) {
                bw.write(String.join(delimiter, row.values()));
                bw.newLine();
            }
        }
    }
}
