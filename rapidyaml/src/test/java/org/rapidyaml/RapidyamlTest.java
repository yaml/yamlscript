package org.rapidyaml;

import org.rapidyaml.*;
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
    public RapidyamlTest(String testName)
    {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite(RapidyamlTest.class);
    }

    private void testEdn_(String ys, String expected)
    {
        Rapidyaml rapidyaml = new Rapidyaml();
        try {
            String actual = rapidyaml.parseYS(ys);
            try {
                assertEquals(expected.length(), actual.length());
                assertEquals(expected, actual);
            }
            catch (Exception e) {
                System.err.println("expected:");
                System.err.println(expected);
                System.err.println("actual");
                System.err.println(actual);
                throw e;
            }
        }
        catch (YamlParseErrorException e) {
            fail("parse error:\n" + e.getMessage());
        }
    }

    public void testPlainMap()
    {
        testEdn_(
            "a: 1"
            ,
            "(\n" +
            "{:+ \"+MAP\"}\n" +
            "{:+ \"=VAL\", := \"a\"}\n" +
            "{:+ \"=VAL\", := \"1\"}\n" +
            "{:+ \"-MAP\"}\n" +
            "{:+ \"-DOC\"}\n" +
            ")\n"
            );
    }

    public void testUtf8()
    {
        testEdn_(
            "𝄞: ✅"
            ,
            "(\n" +
            "{:+ \"+MAP\"}\n" +
            "{:+ \"=VAL\", := \"𝄞\"}\n" +
            "{:+ \"=VAL\", := \"✅\"}\n" +
            "{:+ \"-MAP\"}\n" +
            "{:+ \"-DOC\"}\n" +
            ")\n"
            );
    }

    public void testLargeCase()
    {
        testEdn_(
            "foo: !\n" +
            "- {x: y}\n" +
            "- [x, y]\n" +
            "- foo\n" +
            "- 'foo'\n" +
            "- \"foo\"\n" +
            "- |\n" +
            "  foo\n" +
            "- >\n" +
            "  foo\n" +
            "- [1, 2, true, false, null]\n" +
            "- &anchor-1 !tag-1 foobar\n" +
            "---\n" +
            "another: doc\n"
            ,
            "(\n" +
            "{:+ \"+MAP\"}\n" +
            "{:+ \"=VAL\", := \"foo\"}\n" +
            "{:+ \"+SEQ\", :! \"\"}\n" +
            "{:+ \"+MAP\", :flow true}\n" +
            "{:+ \"=VAL\", := \"x\"}\n" +
            "{:+ \"=VAL\", := \"y\"}\n" +
            "{:+ \"-MAP\"}\n" +
            "{:+ \"+SEQ\", :flow true}\n" +
            "{:+ \"=VAL\", := \"x\"}\n" +
            "{:+ \"=VAL\", := \"y\"}\n" +
            "{:+ \"-SEQ\"}\n" +
            "{:+ \"=VAL\", := \"foo\"}\n" +
            "{:+ \"=VAL\", :' \"foo\"}\n" +
            "{:+ \"=VAL\", :$ \"foo\"}\n" +
            "{:+ \"=VAL\", :| \"foo\\n\"}\n" +
            "{:+ \"=VAL\", :> \"foo\\n\"}\n" +
            "{:+ \"+SEQ\", :flow true}\n" +
            "{:+ \"=VAL\", := \"1\"}\n" +
            "{:+ \"=VAL\", := \"2\"}\n" +
            "{:+ \"=VAL\", := \"true\"}\n" +
            "{:+ \"=VAL\", := \"false\"}\n" +
            "{:+ \"=VAL\", := \"null\"}\n" +
            "{:+ \"-SEQ\"}\n" +
            "{:+ \"=VAL\", :& \"anchor-1\", :! \"tag-1\", := \"foobar\"}\n" +
            "{:+ \"-SEQ\"}\n" +
            "{:+ \"-MAP\"}\n" +
            "{:+ \"-DOC\"}\n" +
            "{:+ \"+DOC\"}\n" +
            "{:+ \"+MAP\"}\n" +
            "{:+ \"=VAL\", := \"another\"}\n" +
            "{:+ \"=VAL\", := \"doc\"}\n" +
            "{:+ \"-MAP\"}\n" +
            "{:+ \"-DOC\"}\n" +
            ")\n"
            );
    }

    public void testFailure()
    {
        Rapidyaml rapidyaml = new Rapidyaml();
        String ys = ": : : :";
        boolean gotit = false;
        try {
            rapidyaml.parseYS(ys);
        }
        catch(YamlParseErrorException e) {
            gotit = true;
            assertEquals(2, e.offset);
            assertEquals(1, e.line);
            assertEquals(3, e.column);
            assertTrue(e.getMessage() != null);
            assertFalse(e.getMessage().isEmpty());
        }
        catch(RuntimeException e) {
            fail("wrong exception type");
        }
        catch(Exception e) {
            fail("wrong exception type");
        }
        assertTrue(gotit);
    }
}
