package me.szabee.doubledoors.proxy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.faststats.velocity.VelocityMetrics;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.junit.jupiter.api.Test;

final class ProxyShadeConfigurationTest {

  @Test
  void testSlf4jIsNotRelocated() throws Exception {
    String pom = Files.readString(Path.of("pom.xml"));

    assertFalse(pom.contains("<pattern>org.slf4j</pattern>"),
      "Velocity injects org.slf4j.Logger; relocating SLF4J breaks plugin creation.");
    assertFalse(pom.contains("me.szabee.doubledoors.lib.slf4j"),
      "The proxy jar must keep Velocity-provided SLF4J types unshaded.");
  }

  @Test
  void testVelocityConstructorUsesVelocityProvidedLogger() {
    boolean hasVelocityConstructor = false;
    for (Constructor<?> constructor : DoubleDoorsProxy.class.getConstructors()) {
      Class<?>[] parameterTypes = constructor.getParameterTypes();
      if (parameterTypes.length == 4
        && parameterTypes[0] == ProxyServer.class
        && parameterTypes[1] == Logger.class
        && parameterTypes[2] == Path.class
        && parameterTypes[3] == VelocityMetrics.Factory.class) {
        hasVelocityConstructor = true;
      }
    }

    assertTrue(hasVelocityConstructor, "Velocity must be able to inject org.slf4j.Logger into the proxy plugin.");
  }
}
