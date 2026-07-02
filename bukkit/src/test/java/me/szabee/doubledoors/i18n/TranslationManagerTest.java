package me.szabee.doubledoors.i18n;

import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TranslationManagerTest {

  @Test
  @SuppressWarnings("unchecked")
  void testTr() throws Exception {
    TranslationManager tm = new TranslationManager();
    Field field = TranslationManager.class.getDeclaredField("translations");
    field.setAccessible(true);
    Map<String, Map<String, String>> translations = (Map<String, Map<String, String>>) field.get(tm);

    Map<String, String> en = new HashMap<>();
    en.put("test.simple", "Hello World");
    en.put("test.args", "Hello %s!");
    translations.put("en_US", en);

    assertEquals("Hello World", tm.tr("test.simple"));
    assertEquals("Hello Szabi!", tm.tr("test.args", "Szabi"));
    assertEquals("missing.key", tm.tr("missing.key"));
  }
}
