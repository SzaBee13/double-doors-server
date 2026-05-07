package me.szabee.doubledoors.proxy;

import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import java.util.UUID;

class ProxySqlClientTest {

    @Test
    void testSQLiteHeartbeat() throws SQLException {
        String dbPath = "target/test-heartbeat-" + UUID.randomUUID() + ".db";
        String jdbcUrl = "jdbc:sqlite:" + dbPath;
        ProxySqlClient client = new ProxySqlClient(jdbcUrl, "", "");
        
        try {
            client.initializeSchema();
            
            long now = System.currentTimeMillis();
            client.upsertHeartbeat("proxy1", "velocity", now);
            client.upsertHeartbeat("proxy1", "velocity", now + 1000);
            
            // If no exception, it's a good sign. 
            // We could add more validation here if we wanted to query it back.
        } finally {
            client.close();
            // Cleanup database file handled by being in target/
        }
    }
}
