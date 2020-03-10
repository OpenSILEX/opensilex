//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRA 2019
// Contact: vincent.migot@inra.fr, anne.tireau@inra.fr, pascal.neveu@inra.fr
//******************************************************************************
package org.opensilex.config;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.opensilex.OpenSilex;
import org.opensilex.unit.test.AbstractUnitTest;


/**
 *
 * @author vincent
 */
public class ConfigTest extends AbstractUnitTest {

    interface TestConfig {

        int int_();

        @ConfigDescription(
                value = "default int",
                defaultInt = 8888
        )
        int int_default();

        Integer int_Class();

        @ConfigDescription(
                value = "default Integer",
                defaultInt = 8888
        )
        int int_Class_default();

        boolean boolean_();

        @ConfigDescription(
                value = "default boolean",
                defaultBoolean = true
        )
        boolean boolean_default();

        Boolean boolean_Class();

        @ConfigDescription(
                value = "default Boolean",
                defaultBoolean = true
        )
        Boolean boolean_Class_default();

        char char_();

        @ConfigDescription(
                value = "default char",
                defaultChar = 'Z'
        )
        char char_default();

        Character char_Class();

        @ConfigDescription(
                value = "default Character",
                defaultChar = 'Z'
        )
        Character char_Class_default();

        float float_();

        @ConfigDescription(
                value = "default float",
                defaultFloat = 6.66f
        )
        float float_default();

        Float float_Class();

        @ConfigDescription(
                value = "default Float",
                defaultFloat = 6.66f
        )
        Float float_Class_default();

        double double_();

        @ConfigDescription(
                value = "default double",
                defaultDouble = 9.99d
        )
        double double_default();

        Double double_Class();

        @ConfigDescription(
                value = "default Double",
                defaultDouble = 9.99d
        )
        Double double_Class_default();

        short short_();

        @ConfigDescription(
                value = "default short",
                defaultShort = (short) 9
        )
        short short_default();

        Short short_Class();

        @ConfigDescription(
                value = "default Short",
                defaultShort = (short) 9
        )
        Short short_Class_default();

        long long_();

        @ConfigDescription(
                value = "default long",
                defaultLong = 9999L
        )
        long long_default();

        Long long_Class();

        @ConfigDescription(
                value = "default Long",
                defaultLong = 9999L
        )
        Long long_Class_default();

        byte byte_();

        @ConfigDescription(
                value = "default byte",
                defaultByte = (byte) 25
        )
        byte byte_default();

        Byte byte_Class();

        @ConfigDescription(
                value = "default Byte",
                defaultByte = (byte) 25
        )
        Byte byte_Class_default();

        String string_();

        @ConfigDescription(
                value = "default String",
                defaultString = "default"
        )
        String string_default();

        String string_Class();

        @ConfigDescription(
                value = "default String",
                defaultString = "default"
        )
        String string_Class_default();

        List<String> listStr();

        @ConfigDescription(
                value = "default String List",
                defaultList = {"a", "b", "c"}
        )
        List<String> listStr_default();

        List<Integer> listInt();

        @ConfigDescription(
                value = "default Integer List",
                defaultList = {"1", "2", "3"}
        )
        List<Integer> listInt_default();

        List<TestConfig> listInterface();

        @ConfigDescription(
                value = "default Interface List",
                defaultList = {"{float_: 2.4, long_: 666}", "{char_: c, int_default: 25}"}
        )
        List<TestConfig> listInterface_default();

        Map<String, String> mapStr();

        @ConfigDescription(
                value = "default String Map",
                defaultMap = {"az: er", "ty: ui", "qs: df"}
        )
        Map<String, String> mapStr_default();

        Map<String, Integer> mapInt();

        @ConfigDescription(
                value = "default Integer Map",
                defaultMap = {"az: 2", "ty: 3", "qs: 4"}
        )
        Map<String, Integer> mapInt_default();

        TestConfig inception();

        OpenSilex invalidClassMemberInConfig();

        @ConfigDescription(
                value = "Invalid default List",
                defaultList = {"\""}
        )
        List<Integer> invalidListDefault();

        Class<?> clazz();

        @ConfigDescription(
                value = "default Class<?>",
                defaultClass = Object.class
        )
        Class<?> clazz_default();

    }

    private enum Primitives {
        INT("int", Integer.class, 0, 1, 2, 8888),
        BOOL("boolean", Boolean.class, false, true, false, true),
        CHAR("char", Character.class, Character.MIN_VALUE, 'a', 'b', 'Z'),
        FLOAT("float", Float.class, 0f, 1.3f, 2.4f, 6.66f),
        DOUBLE("double", Double.class, 0d, 1.23d, 3.33d, 9.99d),
        SHORT("short", Short.class, (short) 0, (short) 5, (short) 6, (short) 9),
        LONG("long", Long.class, 0L, 99L, 1000L, 9999L),
        BYTE("byte", Byte.class, (byte) 0, (byte) 3, (byte) 12, (byte) 25),
        STRING("string", String.class, "", "azerty", "uiop", "default");

        final String primitiveKey;
        final String primitiveKeyDefault;
        final String classKey;
        final String classKeyDefault;
        final Object defaultValue;
        final Object firstValue;
        final Object secondValue;
        final Object defaultAnnotationValue;
        final Class<?> clazz;

        Primitives(
                String key,
                Class<?> clazz,
                Object defaultValue,
                Object firstValue,
                Object secondValue,
                Object defaultAnnotationValue
        ) {
            this.primitiveKey = key + "_";
            this.classKey = primitiveKey + "Class";
            this.primitiveKeyDefault = primitiveKey + "default";
            this.classKeyDefault = classKey + "_default";
            this.defaultValue = defaultValue;
            this.firstValue = firstValue;
            this.secondValue = secondValue;
            this.defaultAnnotationValue = defaultAnnotationValue;
            this.clazz = clazz;
        }
    }

    private void testPrimitive(Primitives p, ConfigManager provider, int depth) {
        try {

            TestConfig cfg = getDeepConfig(provider.loadConfig("test", TestConfig.class), depth);

            String prefix = "test:\n  ";
            for (int i = 1; i <= depth; i++) {
                prefix += "inception:\n  " + StringUtils.repeat("  ", i);
            }

            Method primitiveMethod = TestConfig.class.getMethod(p.primitiveKey, new Class<?>[]{});
            Method classMethod = TestConfig.class.getMethod(p.classKey, new Class<?>[]{});
            Method primitiveDefaultMethod = TestConfig.class.getMethod(p.primitiveKeyDefault, new Class<?>[]{});
            Method classDefaultMethod = TestConfig.class.getMethod(p.classKeyDefault, new Class<?>[]{});

            assertEquals("Check primitive default", p.defaultValue, primitiveMethod.invoke(cfg));
            assertEquals("Check primitive Class default", p.defaultValue, classMethod.invoke(cfg));

            assertEquals("Check primitive annotation default", p.defaultAnnotationValue, primitiveDefaultMethod.invoke(cfg));
            assertEquals("Check primitive Class annotation default", p.defaultAnnotationValue, classDefaultMethod.invoke(cfg));

            assertTrue("Check primitive type", primitiveMethod.invoke(cfg).getClass().isAssignableFrom(p.clazz));
            assertTrue("Check primitive Class type", classMethod.invoke(cfg).getClass().isAssignableFrom(p.clazz));

            provider.addLines(prefix + p.primitiveKeyDefault + ":");
            provider.addLines(prefix + p.classKeyDefault + ":");

            cfg = getDeepConfig(provider.loadConfig("test", TestConfig.class), depth);

            assertEquals("Check primitive annotation default with empty node", p.defaultAnnotationValue, primitiveDefaultMethod.invoke(cfg));
            assertEquals("Check primitive Class annotation default with empty node", p.defaultAnnotationValue, classDefaultMethod.invoke(cfg));

            provider.addLines(prefix + p.primitiveKeyDefault + ": ~");
            provider.addLines(prefix + p.classKeyDefault + ": ~");

            cfg = getDeepConfig(provider.loadConfig("test", TestConfig.class), depth);

            assertEquals("Check primitive annotation default with explicit null node", p.defaultAnnotationValue, primitiveDefaultMethod.invoke(cfg));
            assertEquals("Check primitive Class annotation default with explicit null node", p.defaultAnnotationValue, classDefaultMethod.invoke(cfg));

            provider.addLines(prefix + p.primitiveKey + ": " + p.firstValue);
            provider.addLines(prefix + p.classKey + ": " + p.firstValue);
            provider.addLines(prefix + p.primitiveKeyDefault + ": " + p.firstValue);
            provider.addLines(prefix + p.classKeyDefault + ": " + p.firstValue);

            cfg = getDeepConfig(provider.loadConfig("test", TestConfig.class), depth);

            assertEquals("Check define primitive value", p.firstValue, primitiveMethod.invoke(cfg));
            assertEquals("Check define primitive Class value", p.firstValue, classMethod.invoke(cfg));

            assertEquals("Check primitive annotation default override", p.firstValue, primitiveDefaultMethod.invoke(cfg));
            assertEquals("Check primitive Class annotation default override", p.firstValue, classDefaultMethod.invoke(cfg));

            assertTrue("Check primitive type", primitiveMethod.invoke(cfg).getClass().isAssignableFrom(p.clazz));
            assertTrue("Check primitive Class type", classMethod.invoke(cfg).getClass().isAssignableFrom(p.clazz));

            provider.addLines(prefix + p.primitiveKey + ": " + p.secondValue);
            provider.addLines(prefix + p.classKey + ": " + p.secondValue);
            provider.addLines(prefix + p.primitiveKeyDefault + ": " + p.secondValue);
            provider.addLines(prefix + p.classKeyDefault + ": " + p.secondValue);

            assertEquals("Check previously loaded primitive value", p.firstValue, primitiveMethod.invoke(cfg));
            assertEquals("Check previously loaded primitive Class value", p.firstValue, classMethod.invoke(cfg));

            assertEquals("Check previously loaded primitive annotation default", p.firstValue, primitiveDefaultMethod.invoke(cfg));
            assertEquals("Check previously loaded primitive Class annotation default", p.firstValue, classDefaultMethod.invoke(cfg));

            assertTrue("Check primitive type", primitiveMethod.invoke(cfg).getClass().isAssignableFrom(p.clazz));
            assertTrue("Check primitive Class type", classMethod.invoke(cfg).getClass().isAssignableFrom(p.clazz));

            cfg = getDeepConfig(provider.loadConfig("test", TestConfig.class), depth);

            assertEquals("Check reloaded primitive value", p.secondValue, primitiveMethod.invoke(cfg));
            assertEquals("Check reloaded primitive Class value", p.secondValue, classMethod.invoke(cfg));

            assertEquals("Check reloaded primitive annotation default", p.secondValue, primitiveDefaultMethod.invoke(cfg));
            assertEquals("Check reloaded primitive Class annotation default", p.secondValue, classDefaultMethod.invoke(cfg));

            assertTrue("Check primitive type", primitiveMethod.invoke(cfg).getClass().isAssignableFrom(p.clazz));
            assertTrue("Check primitive Class type", classMethod.invoke(cfg).getClass().isAssignableFrom(p.clazz));
        } catch (IOException ex) {
            fail("No exception should be thrown while parsing YAML string: " + ex.getMessage());
        } catch (IllegalAccessException
                | IllegalArgumentException
                | NoSuchMethodException
                | SecurityException
                | InvocationTargetException ex) {
            fail("Unexpected reflection error: " + ex.getMessage());
        }
    }

    private TestConfig getDeepConfig(TestConfig base, int depth) {
        if (depth > 0) {
            return getDeepConfig(base.inception(), depth - 1);
        } else {
            return base;
        }
    }

    private void testMap(String testMessage, Map<String, ? extends Object> expected, Map<String, ? extends Object> actual) {
        assertEquals(testMessage + " size", expected.size(), actual.size());

        Iterator<?> iterator = expected.entrySet().iterator();
        while (iterator.hasNext()) {
            @SuppressWarnings("unchecked")
            Map.Entry<String, ? extends Object> entry = (Entry<String, ? extends Object>) iterator.next();

            assertTrue(testMessage + " key should exists " + entry.getKey(), actual.containsKey(entry.getKey()));
            assertTrue(testMessage + " value should be equal for key " + entry.getValue(), actual.get(entry.getKey()).equals(entry.getValue()));
        }
    }

    @Test
    public void testInt() {
        testPrimitive(Primitives.INT, new ConfigManager(), 0);
    }

    @Test
    public void testBool() {
        testPrimitive(Primitives.BOOL, new ConfigManager(), 0);
    }

    @Test
    public void testChar() {
        testPrimitive(Primitives.CHAR, new ConfigManager(), 0);
    }

    @Test
    public void testFloat() {
        testPrimitive(Primitives.FLOAT, new ConfigManager(), 0);
    }

    @Test
    public void testDouble() {
        testPrimitive(Primitives.DOUBLE, new ConfigManager(), 0);
    }

    @Test
    public void testShort() {
        testPrimitive(Primitives.SHORT, new ConfigManager(), 0);
    }

    @Test
    public void testLong() {
        testPrimitive(Primitives.LONG, new ConfigManager(), 0);
    }

    @Test
    public void testByte() {
        testPrimitive(Primitives.BYTE, new ConfigManager(), 0);
    }

    @Test
    public void testString() {
        testPrimitive(Primitives.STRING, new ConfigManager(), 0);
    }

    @Test
    public void testList() {
        try {
            ConfigManager provider = new ConfigManager();

            TestConfig cfg = provider.loadConfig("test", TestConfig.class);

            List<Integer> listInt = cfg.listInt();
            List<String> listStr = cfg.listStr();
            List<Integer> listIntDefault = cfg.listInt_default();
            List<String> listStrDefault = cfg.listStr_default();

            assertEquals("Check empty int list size", 0, listInt.size());
            assertEquals("Check empty string list size", 0, listStr.size());

            assertArrayEquals("Check default string list", new String[]{"a", "b", "c"}, listStrDefault.toArray());
            assertArrayEquals("Check default int list", new Integer[]{1, 2, 3}, listIntDefault.toArray());

            TestConfig subCfg = cfg.listInterface_default().get(0);
            assertEquals("Check default sub interface 1 float value", 2.4f, subCfg.float_(), 0);
            assertEquals("Check default sub interface 1 long value", 666L, subCfg.long_());

            subCfg = cfg.listInterface_default().get(1);
            assertEquals("Check default sub interface 2 char value", 'c', subCfg.char_());
            assertEquals("Check default sub interface 2 int default override value", 25, subCfg.int_default());

            provider.addLines(
                    "test:",
                    "  listStr: ",
                    "    - az",
                    "    - er",
                    "    - ty",
                    "  listInt: ",
                    "    - 2",
                    "    - 4",
                    "    - 6",
                    "  listStr_default: ",
                    "    - abc",
                    "  listInt_default: []",
                    "  listInterface:",
                    "    - int_: 1",
                    "      string_: s1",
                    "    - int_: 2",
                    "      string_: s2",
                    "  listInterface_default:",
                    "    - float_: 3.2",
                    "      long_: 999",
                    "    - char_: z",
                    "      int_default: 6"
            );

            cfg = provider.loadConfig("test", TestConfig.class);

            listInt = cfg.listInt();
            listStr = cfg.listStr();
            listIntDefault = cfg.listInt_default();
            listStrDefault = cfg.listStr_default();

            subCfg = cfg.listInterface().get(0);
            assertEquals("Check loaded sub interface 1 int value", subCfg.int_(), 1);
            assertEquals("Check loaded sub interface 1 string value", subCfg.string_(), "s1");

            subCfg = cfg.listInterface().get(1);
            assertEquals("Check loaded sub interface 2 int value", subCfg.int_(), 2);
            assertEquals("Check loaded sub interface 2 string value", subCfg.string_(), "s2");

            subCfg = cfg.listInterface_default().get(0);
            assertEquals("Check overloaded default sub interface 1 float value", 3.2f, subCfg.float_(), 0);
            assertEquals("Check overloaded default sub interface 1 long value", 999L, subCfg.long_());

            subCfg = cfg.listInterface_default().get(1);
            assertEquals("Check overloaded default sub interface 2 char value", 'z', subCfg.char_());
            assertEquals("Check overloaded default sub interface 2 int default override value", 6, subCfg.int_default());

            assertArrayEquals("Check loaded string list", new String[]{"az", "er", "ty"}, listStr.toArray());
            assertArrayEquals("Check loaded int list", new Integer[]{2, 4, 6}, listInt.toArray());

            assertArrayEquals("Check default override string list", new String[]{"abc"}, listStrDefault.toArray());
            assertArrayEquals("Check default override int list", new Integer[]{}, listIntDefault.toArray());

        } catch (IOException ex) {
            fail("No exception should be thrown while parsing YAML string: " + ex.getMessage());
        }
    }

    @Test
    public void testMap() {
        try {
            ConfigManager provider = new ConfigManager();

            TestConfig cfg = provider.loadConfig("test", TestConfig.class);

            Map<String, Integer> mapInt = cfg.mapInt();
            Map<String, String> mapStr = cfg.mapStr();
            Map<String, Integer> mapIntDefault = cfg.mapInt_default();
            Map<String, String> mapStrDefault = cfg.mapStr_default();

            assertEquals("Check empty int map size", 0, mapInt.size());
            assertEquals("Check empty string map size", 0, mapStr.size());

            Map<String, String> expectedStrMap = new HashMap<>();
            expectedStrMap.put("az", "er");
            expectedStrMap.put("ty", "ui");
            expectedStrMap.put("qs", "df");
            Map<String, Integer> expectedIntMap = new HashMap<>();
            expectedIntMap.put("az", 2);
            expectedIntMap.put("ty", 3);
            expectedIntMap.put("qs", 4);

            testMap("Check default string map", expectedStrMap, mapStrDefault);
            testMap("Check default int map", expectedIntMap, mapIntDefault);

            provider.addLines(
                    "test:",
                    "  mapStr: ",
                    "    za: re",
                    "    yt: iu",
                    "  mapInt: ",
                    "    za: 0",
                    "    yt: 1",
                    "  mapStr_default: ",
                    "    sq: fd",
                    "  mapInt_default: {}"
            );

            cfg = provider.loadConfig("test", TestConfig.class);

            mapInt = cfg.mapInt();
            mapStr = cfg.mapStr();
            mapIntDefault = cfg.mapInt_default();
            mapStrDefault = cfg.mapStr_default();

            expectedStrMap = new HashMap<>();
            expectedStrMap.put("za", "re");
            expectedStrMap.put("yt", "iu");
            expectedIntMap = new HashMap<>();
            expectedIntMap.put("za", 0);
            expectedIntMap.put("yt", 1);
            testMap("Check loaded string map", expectedStrMap, mapStr);
            testMap("Check loaded int map", expectedIntMap, mapInt);

            expectedStrMap = new HashMap<>();
            expectedStrMap.put("sq", "fd");
            expectedIntMap = new HashMap<>();
            testMap("Check default override string map", expectedStrMap, mapStrDefault);
            testMap("Check default override int map", expectedIntMap, mapIntDefault);

        } catch (IOException ex) {
            fail("No exception should be thrown while parsing YAML string: " + ex.getMessage());
        }
    }

    @Test
    public void testInterface() throws IOException {
        ConfigManager provider = new ConfigManager();
        for (int i = 1; i < 5; i++) {
            testPrimitive(Primitives.INT, provider, i);
            testPrimitive(Primitives.BOOL, provider, i);
            testPrimitive(Primitives.CHAR, provider, i);
            testPrimitive(Primitives.FLOAT, provider, i);
            testPrimitive(Primitives.DOUBLE, provider, i);
            testPrimitive(Primitives.SHORT, provider, i);
            testPrimitive(Primitives.LONG, provider, i);
            testPrimitive(Primitives.BYTE, provider, i);
            testPrimitive(Primitives.STRING, provider, i);
        }
    }

    @Test(expected = InvalidConfigException.class)
    public void testInvalidClassMemberInConfig() throws Throwable {
        ConfigManager provider = new ConfigManager();

        TestConfig cfg = provider.loadConfig("test", TestConfig.class);

        try {
            cfg.invalidClassMemberInConfig();
        } catch (UndeclaredThrowableException ex) {
            throw ex.getCause();
        }
    }

    @Test(expected = InvalidConfigException.class)
    public void testInvalidListDefault() throws Throwable {
        ConfigManager provider = new ConfigManager();

        TestConfig cfg = provider.loadConfig("test", TestConfig.class);

        try {
            cfg.invalidListDefault();
        } catch (UndeclaredThrowableException ex) {
            throw ex.getCause();
        }
    }

    @Test
    public void testClass() throws Throwable {
        ConfigManager provider = new ConfigManager();

        TestConfig cfg = provider.loadConfig("test", TestConfig.class);

        Class<?> clazz = cfg.clazz();

        assertNull("Class with no default must be null", clazz);

        Class<?> classDefault = cfg.clazz_default();

        assertTrue("Default class value must be Object", classDefault.equals(Object.class));
    }
}
