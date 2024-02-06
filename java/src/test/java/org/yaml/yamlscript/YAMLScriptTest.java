package org.yaml.yamlscript;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Unit test for simple App.
 */
public class YAMLScriptTest extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public YAMLScriptTest(String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( YAMLScriptTest.class );
    }

    public void testLoadJSON()
    {
        JSONObject result = YAMLScript.loadJSON("a: 1");
        int value = result.getJSONObject("data").getInt("a");

        assertEquals(1, value);
    }

    public void testLoad()
    {
        JSONObject data = (JSONObject)YAMLScript.load("a: 1");

        assertEquals(1, data.getInt("a"));
    }

    public void testLoadArray()
    {
        JSONArray data = YAMLScript.loadArray("[1, 2, 3]");

        assertEquals(1, data.getInt(0));
    }

    public void testLoadInt()
    {
        int data = YAMLScript.loadInt("1");

        assertEquals(1, data);
    }

    public void testLoadBoolean()
    {
        boolean data = YAMLScript.loadBoolean("true");

        assertTrue(data);
    }

    public void testLoadFloat()
    {
        float data = YAMLScript.loadFloat("1.4");

        assertEquals(1.4F, data);
    }

    public void testLoadDouble()
    {
        double data = YAMLScript.loadDouble("1.4");

        assertEquals(1.4D, data);
    }

    public void testLoadLong()
    {
        long data = YAMLScript.loadLong("1");

        assertEquals(1L, data);
    }

    public void testLoadBigInteger()
    {
        BigInteger data = YAMLScript.loadBigInteger("1");

        assertEquals(new BigInteger("1"), data);
    }

    public void testLoadBigDecimal()
    {
        BigDecimal data = YAMLScript.loadBigDecimal("1.4");

        assertEquals(new BigDecimal("1.4"), data);
    }
}
