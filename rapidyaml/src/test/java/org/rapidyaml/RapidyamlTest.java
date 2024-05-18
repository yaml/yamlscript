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

    private void testEdn_(String ys, String expected)
    {
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
            "{:+ \"=VAL\", :| \"foo\n\"}\n" +
            "{:+ \"=VAL\", :> \"foo\n\"}\n" +
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
}
