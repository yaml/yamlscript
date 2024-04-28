package org.rapidyaml;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class RapidyamlTest extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public RapidyamlTest(String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( RapidyamlTest.class );
    }

    public void testLoad()
    {
        Rapidyaml rapidyaml = new Rapidyaml();
        String evts = rapidyaml.parseYS("a: 1");
        assertEquals("+STR\n+DOC\n+MAP\n=VAL :a\n=VAL :1\n-MAP\n-DOC\n-STR\n",
                     evts);
    }

    public void testUtf8()
    {
        Rapidyaml rapidyaml = new Rapidyaml();
        String evts = rapidyaml.parseYS("𝄞: ✅");
        assertEquals("+STR\n+DOC\n+MAP\n=VAL :𝄞\n=VAL :✅\n-MAP\n-DOC\n-STR\n",
                     evts);
    }
}
