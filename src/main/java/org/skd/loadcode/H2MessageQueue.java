package org.skd.loadcode;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class H2MessageQueue {
    private static final String DB_URL = "jdbc:h2:file:./message_db";
    private static final String USER = "sa";
    private static final String PASSWORD = "sa";

    public H2MessageQueue() {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("H2 Driver not found!", e);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASSWORD);
    }

    /**
     * Creates a table for the given topic if it doesn't exist.
     */
    private void ensureTopicTableExists(String topic) {
        String sql = "CREATE TABLE IF NOT EXISTS " + topic + " (" +
                "id IDENTITY PRIMARY KEY, " +
                "data VARCHAR(255) NOT NULL, " +
                "status VARCHAR(20) DEFAULT 'PENDING')";

        String indexSql = "CREATE INDEX IF NOT EXISTS idx_status ON " + topic + " (status)";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            stmt.executeUpdate(indexSql); // Add index on `status`
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /**
     * Inserts a new message into the given topic.
     */
    public void insert(String topic, String data) {
        ensureTopicTableExists(topic);
        String sql = "INSERT INTO " + topic + " (data, status) VALUES (?, 'PENDING')";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, data);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads a batch of messages from the given topic.
     * Ensures each thread gets unique messages by updating their status to 'READ'.
     */
    public  synchronized List<String> read(String topic, int batchSize) {
        ensureTopicTableExists(topic);
        List<String> messages = new ArrayList<>();

        String sqlSelect = "SELECT id, data FROM " + topic + " WHERE status = 'PENDING' " +
                "ORDER BY id LIMIT ? FOR UPDATE SKIP LOCKED";
        String sqlUpdate = "UPDATE " + topic + " SET status = 'READ' WHERE id = ?";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);  // Start transaction

            try (PreparedStatement stmtSelect = conn.prepareStatement(sqlSelect);
                 PreparedStatement stmtUpdate = conn.prepareStatement(sqlUpdate)) {

                stmtSelect.setInt(1, batchSize);
                ResultSet rs = stmtSelect.executeQuery();

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String data = rs.getString("data");
                    messages.add(data);

                    // Mark as READ to prevent re-reading
                    stmtUpdate.setInt(1, id);
                    stmtUpdate.executeUpdate();
                }

                conn.commit();  // Commit transaction
            } catch (SQLException e) {
                conn.rollback();  // Rollback in case of failure
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return messages;
    }

    /**
     * Deletes a specific message from the topic.
     */
    public synchronized void delete(String topic, String data) {
        ensureTopicTableExists(topic);
        String sql = "DELETE FROM " + topic + " WHERE data = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, data);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Resets a message by making it available for processing again.
     */
    public void reset(String topic, String data) {
        ensureTopicTableExists(topic);
        String sql = "UPDATE " + topic + " SET status = 'PENDING' WHERE data = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, data);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public synchronized void bulkInsert(String topic, List<String> messages) {
        ensureTopicTableExists(topic);
        String sql = "INSERT INTO " + topic + " (data, status) VALUES (?, 'PENDING')";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);  // Start transaction

            for (String data : messages) {
                stmt.setString(1, data);
                stmt.addBatch();
            }
            stmt.executeBatch();  // Execute all inserts in a batch
            conn.commit();  // Commit transaction
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public int getAvailableRecords(String topic) {
        ensureTopicTableExists(topic);
        String sql = "SELECT COUNT(*) FROM " + topic + " WHERE status = 'PENDING'";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);  // Returns the count of records with 'PENDING' status
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;  // Return 0 if no records are found
    }

    public static void main(String[] args) {

        AtomicInteger y = new AtomicInteger(1);
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        for(int i=0;i<100;i++)
        {

            H2MessageQueue h2MessageQueue = new H2MessageQueue();
            int finalI = i;
            executorService.submit(()->{
                final int x = finalI;
                while (true)
                {
                    List<String> datared = h2MessageQueue.read("forupdate",1);
                    if(datared.size()>0)
                    {
                        //update forupdate set status = 'PENDING'
                        //System.out.println(y.getAndIncrement()+"=>"+x+" -- "+datared);
                        System.out.println(datared);
                    }
                }


            });
        }


    }


}
