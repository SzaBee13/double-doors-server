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
        TranslationManager tm = new TranslationManager(null, null);
        
        // Inject activeTranslations using reflection for pure logic testing
        Field activeTranslationsField = TranslationManager.class.getDeclaredField("activeTranslations");
        activeTranslationsField.setAccessible(true);
        Map<String, String> activeTranslations = (Map<String, String>) activeTranslationsField.get(tm);
        
        activeTranslations.put("test.simple", "Hello World");
        activeTranslations.put("test.args", "Hello %s!");
        
        assertEquals("Hello World", tm.tr("test.simple"));
        assertEquals("Hello Szabi!", tm.tr("test.args", "Szabi"));
        assertEquals("missing.key", tm.tr("missing.key"));
    }
}
