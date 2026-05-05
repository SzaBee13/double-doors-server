package me.szabee.doubledoors.version;

import org.bukkit.Bukkit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class VersionBridgeTest {

    @Test
    void testGetServerApiVersion() {
        VersionBridge bridge = new VersionBridgeImpl();
        
        try (MockedStatic<Bukkit> mockedBukkit = mockStatic(Bukkit.class)) {
            mockedBukkit.when(Bukkit::getBukkitVersion).thenReturn("26.1-R0.1-SNAPSHOT");
            assertEquals("26.1-R0.1-SNAPSHOT", bridge.getServerApiVersion());
            
            mockedBukkit.when(Bukkit::getBukkitVersion).thenReturn("");
            assertEquals("unknown", bridge.getServerApiVersion());

            mockedBukkit.when(Bukkit::getBukkitVersion).thenReturn(null);
            assertEquals("unknown", bridge.getServerApiVersion());
        }
    }
}
