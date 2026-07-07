package me.szabee.doubledoors.velocity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.velocitypowered.api.proxy.ProxyServer;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.junit.jupiter.api.Test;

final class VelocityShadeConfigurationTest {

  @Test
  void testSlf4jIsNotRelocated() throws Exception {
    String buildScript = Files.readString(Path.of("build.gradle"));

    assertFalse(buildScript.contains("relocate \"org.slf4j\""),
      "Velocity injects org.slf4j.Logger; relocating SLF4J breaks plugin creation.");
    assertFalse(buildScript.contains("me.szabee.doubledoors.lib.slf4j"),
      "The proxy jar must keep Velocity-provided SLF4J types unshaded.");
  }

  @Test
  void testVelocityConstructorUsesVelocityProvidedLogger() {
    boolean hasVelocityConstructor = false;
    for (Constructor<?> constructor : DoubleDoorsVelocity.class.getConstructors()) {
      Class<?>[] parameterTypes = constructor.getParameterTypes();
      if (parameterTypes.length == 3
        && parameterTypes[0] == ProxyServer.class
        && parameterTypes[1] == Logger.class
        && parameterTypes[2] == Path.class) {
        hasVelocityConstructor = true;
      }
    }

    assertTrue(hasVelocityConstructor, "Velocity must be able to inject org.slf4j.Logger into the proxy plugin.");
  }
}
