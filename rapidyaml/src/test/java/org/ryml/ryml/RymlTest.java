package org.ryml.ryml;

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
public class RymlTest extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public RymlTest(String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( RymlTest.class );
    }

    public void testLoad()
    {
        Ryml ryml = new Ryml();
        String evts = ryml.parseYS("a: 1");
        assertEquals(evts, "+STR\n+DOC\n+MAP\n=VAL :a\n=VAL :1\n-MAP\n-DOC\n-STR\n");
    }
}
