package org.rapidyaml;

import org.rapidyaml.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
            String actual = rapidyaml.parseYsToEdn(ys);
            try {
                assertEquals(expected, actual);
                assertEquals(expected.length(), actual.length());
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

    // the result is an array of integers, but we use this to simplify
    // running the tests
    private class ExpectedEvent
    {
        int flags;
        int str_start;
        int str_len;
        String str;
        ExpectedEvent(int flags)
        {
            this.flags = flags;
            this.str_start = 0;
            this.str_len = 0;
            this.str = "";
        }
        ExpectedEvent(int flags, int str_start, int str_len, String str)
        {
            this.flags = flags;
            this.str_start = str_start;
            this.str_len = str_len;
            this.str = str;
        }
        int required_size()
        {
            return ((flags & Evt.HAS_STR) != 0) ? 3 : 1;
        }
    };
    private static int required_size_(ExpectedEvent[] evts)
    {
        int sz = 0;
        for(int i = 0; i < evts.length; ++i) {
            sz += evts[i].required_size();
        }
        return sz;
    }

    private void testEvt_(String ys, ExpectedEvent[] expected)
    {
        boolean dbglog = true;
        Rapidyaml rapidyaml = new Rapidyaml();
        try {
            int[] actual = new int[2 * required_size_(expected)];
            byte[] src = ys.getBytes(StandardCharsets.UTF_8);
            int numEvts = rapidyaml.parseYsToEvt(src, actual);
            assertTrue(numEvts < actual.length);
            try {
                int ia = 0;
                int ie = 0;
                int status = 1;
                while(true) {
                    if((ia < numEvts) != (ie < expected.length)) {
                        status = 0;
                        break;
                    }
                    if(ia >= numEvts)
                        break;
                    if(ie >= expected.length)
                        break;
                    int cmp = 1;
                    if(dbglog)
                        System.out.printf("status=%d evt=%d pos=%d expflags=%d actualflags=%d", status, ie, ia, expected[ie].flags, actual[ia]);
                    cmp &= (expected[ie].flags == actual[ia]) ? 1 : 0;
                    if(((actual[ia] & Evt.HAS_STR) != 0) && ((expected[ie].flags & Evt.HAS_STR)) != 0) {
                        cmp &= (ia + 2 < numEvts) ? 1 : 0;
                        if(cmp != 0) {
                            cmp &= (expected[ie].str_start == actual[ia + 1]) ? 1 : 0;
                            cmp &= (expected[ie].str_len == actual[ia + 2]) ? 1 : 0;
                            if(dbglog)
                                System.out.printf("  exp=(%d,%d) actual=(%d,%d)", expected[ie].str_start, expected[ie].str_len, actual[ia + 1], actual[ia + 2]);
                            if(cmp != 0) {
                                cmp &= (actual[ia + 1] >= 0) ? 1 : 0;
                                cmp &= (actual[ia + 2] >= 0) ? 1 : 0;
                                cmp &= (actual[ia + 1] + actual[ia + 2] <= src.length) ? 1 : 0;
                                if(cmp != 0) {
                                    String actualStr = new String(src, actual[ia + 1], actual[ia + 2], StandardCharsets.UTF_8);
                                    cmp &= actualStr.equals(expected[ie].str) ? 1 : 0;
                                    if(dbglog)
                                        System.out.printf("  exp=~~~%s~~~ actual=~~~%s~~~", expected[ie].str, actualStr);
                                }
                                else {
                                    if(dbglog)
                                        System.out.printf("  BAD RANGE len=%d yslen=%d", src.length, ys.length());
                                }
                            }
                        }
                    }
                    if(dbglog)
                        System.out.printf("  --> %s\n", cmp != 0 ? "ok!" : "FAIL");
                    status &= cmp;
                    ia += ((actual[ia] & Evt.HAS_STR) != 0) ? 3 : 1;
                    ++ie;
                }
                if(required_size_(expected) != numEvts)
                    status = 0;
                assertEquals(1, status);
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

    ExpectedEvent mkev(int flags)
    {
        return new ExpectedEvent(flags);
    }

    ExpectedEvent mkev(int flags, int offs, int len, String ref)
    {
        return new ExpectedEvent(flags, offs, len, ref);
    }

    public void testPlainMap()
    {
        String ys = "a: 1";
        testEdn_(ys,
            "(\n" +
            "{:+ \"+MAP\"}\n" +
            "{:+ \"=VAL\", := \"a\"}\n" +
            "{:+ \"=VAL\", := \"1\"}\n" +
            "{:+ \"-MAP\"}\n" +
            "{:+ \"-DOC\"}\n" +
            ")\n"
            );
        ExpectedEvent[] expected = {
            mkev(Evt.BSTR),
            mkev(Evt.BDOC),
            mkev(Evt.VAL_|Evt.BMAP|Evt.BLCK),
            mkev(Evt.KEY_|Evt.SCLR|Evt.PLAI, 0, 1, "a"),
            mkev(Evt.VAL_|Evt.SCLR|Evt.PLAI, 3, 1, "1"),
            mkev(Evt.EMAP),
            mkev(Evt.EDOC),
            mkev(Evt.ESTR),
        };
        testEvt_(ys, expected);
    }

    public void testUtf8()
    {
        String ys = "𝄞: ✅";
        testEdn_(ys,
            "(\n" +
            "{:+ \"+MAP\"}\n" +
            "{:+ \"=VAL\", := \"𝄞\"}\n" +
            "{:+ \"=VAL\", := \"✅\"}\n" +
            "{:+ \"-MAP\"}\n" +
            "{:+ \"-DOC\"}\n" +
            ")\n"
            );
        ExpectedEvent[] expected = {
            mkev(Evt.BSTR),
            mkev(Evt.BDOC),
            mkev(Evt.VAL_|Evt.BMAP|Evt.BLCK),
            mkev(Evt.KEY_|Evt.SCLR|Evt.PLAI, 0, 4, "𝄞"),
            mkev(Evt.VAL_|Evt.SCLR|Evt.PLAI, 6, 3, "✅"),
            mkev(Evt.EMAP),
            mkev(Evt.EDOC),
            mkev(Evt.ESTR),
        };
        testEvt_(ys, expected);
    }

    public void testTaggedInt()
    {
        String ys = "- !!int 42";
        testEdn_(ys,
            "(\n" +
            "{:+ \"+SEQ\"}\n" +
            "{:+ \"=VAL\", :! \"tag:yaml.org,2002:int\", := \"42\"}\n" +
            "{:+ \"-SEQ\"}\n" +
            "{:+ \"-DOC\"}\n" +
            ")\n"
            );
        ExpectedEvent[] expected = {
            mkev(Evt.BSTR),
            mkev(Evt.BDOC),
            mkev(Evt.VAL_|Evt.BSEQ|Evt.BLCK),
            mkev(Evt.VAL_|Evt.TAG_, 2, 5, "!!int"),
            mkev(Evt.VAL_|Evt.SCLR|Evt.PLAI, 8, 2, "42"),
            mkev(Evt.ESEQ),
            mkev(Evt.EDOC),
            mkev(Evt.ESTR),
        };
        testEvt_(ys, expected);
    }

    public void testTaggedSeq()
    {
        String ys = "- !!seq []";
        testEdn_(ys,
            "(\n" +
            "{:+ \"+SEQ\"}\n" +
            "{:+ \"+SEQ\", :! \"tag:yaml.org,2002:seq\", :flow true}\n" +
            "{:+ \"-SEQ\"}\n" +
            "{:+ \"-SEQ\"}\n" +
            "{:+ \"-DOC\"}\n" +
            ")\n"
            );
        ExpectedEvent[] expected = {
            mkev(Evt.BSTR),
            mkev(Evt.BDOC),
            mkev(Evt.VAL_|Evt.BSEQ|Evt.BLCK),
            mkev(Evt.VAL_|Evt.TAG_, 2, 5, "!!seq"),
            mkev(Evt.VAL_|Evt.BSEQ|Evt.FLOW),
            mkev(Evt.ESEQ),
            mkev(Evt.ESEQ),
            mkev(Evt.EDOC),
            mkev(Evt.ESTR),
        };
        testEvt_(ys, expected);
    }

    public void testLargeCase()
    {
        String ys = "--- !yamlscript/v0\n" +
            "foo: !\n" +
            "- {x: y}\n" +
            "- [x, y]\n" +
            "- foo\n" +
            "- 'foo'\n" +
            "- \"foo\"\n" +
            "- |\n" +
            "      foo\n" +
            "      literal\n" +
            "- >\n" +
            "      foo\n" +
            "      folded\n" +
            "- [1, 2, true, false, null]\n" +
            "- &anchor-1 !tag-1 foobar\n" +
            "---\n" +
            "another: doc\n";
        testEdn_(ys,
            "(\n" +
            "{:+ \"+MAP\", :! \"yamlscript/v0\"}\n" +
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
            "{:+ \"=VAL\", :| \"foo\\nliteral\\n\"}\n" +
            "{:+ \"=VAL\", :> \"foo folded\\n\"}\n" +
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
        ExpectedEvent[] expected = {
            mkev(Evt.BSTR),
            mkev(Evt.BDOC|Evt.EXPL),
            mkev(Evt.VAL_|Evt.TAG_, 5, 13, "yamlscript/v0"),
            mkev(Evt.VAL_|Evt.BMAP|Evt.BLCK),
            mkev(Evt.KEY_|Evt.SCLR|Evt.PLAI, 19, 3, "foo"),
            mkev(Evt.VAL_|Evt.TAG_, 25, 0, ""),
            mkev(Evt.VAL_|Evt.BSEQ|Evt.BLCK),
            mkev(Evt.VAL_|Evt.BMAP|Evt.FLOW),
            mkev(Evt.KEY_|Evt.SCLR|Evt.PLAI, 29, 1, "x"),
            mkev(Evt.VAL_|Evt.SCLR|Evt.PLAI, 32, 1, "y"),
            mkev(Evt.EMAP),
            mkev(Evt.VAL_|Evt.BSEQ|Evt.FLOW),
            mkev(Evt.VAL_|Evt.SCLR|Evt.PLAI, 38, 1, "x"),
            mkev(Evt.VAL_|Evt.SCLR|Evt.PLAI, 41, 1, "y"),
            mkev(Evt.ESEQ),
            mkev(Evt.VAL_|Evt.SCLR|Evt.PLAI, 46, 3, "foo"),
            mkev(Evt.VAL_|Evt.SCLR|Evt.SQUO, 53, 3, "foo"),
            mkev(Evt.VAL_|Evt.SCLR|Evt.DQUO, 61, 3, "foo"),
            mkev(Evt.VAL_|Evt.SCLR|Evt.LITL, 70, 12, "foo\nliteral\n"),
            mkev(Evt.VAL_|Evt.SCLR|Evt.FOLD, 98, 11, "foo folded\n"),
            mkev(Evt.VAL_|Evt.BSEQ|Evt.FLOW),
            mkev(Evt.VAL_|Evt.SCLR|Evt.PLAI, 124, 1, "1"),
            mkev(Evt.VAL_|Evt.SCLR|Evt.PLAI, 127, 1, "2"),
            mkev(Evt.VAL_|Evt.SCLR|Evt.PLAI, 130, 4, "true"),
            mkev(Evt.VAL_|Evt.SCLR|Evt.PLAI, 136, 5, "false"),
            mkev(Evt.VAL_|Evt.SCLR|Evt.PLAI, 143, 4, "null"),
            mkev(Evt.ESEQ),
            mkev(Evt.VAL_|Evt.TAG_, 162, 5, "tag-1"),
            mkev(Evt.VAL_|Evt.ANCH, 152, 8, "anchor-1"),
            mkev(Evt.VAL_|Evt.SCLR|Evt.PLAI, 168, 6, "foobar"),
            mkev(Evt.ESEQ),
            mkev(Evt.EMAP),
            mkev(Evt.EDOC),
            mkev(Evt.BDOC|Evt.EXPL),
            mkev(Evt.VAL_|Evt.BMAP|Evt.BLCK),
            mkev(Evt.KEY_|Evt.SCLR|Evt.PLAI, 179, 7, "another"),
            mkev(Evt.VAL_|Evt.SCLR|Evt.PLAI, 188, 3, "doc"),
            mkev(Evt.EMAP),
            mkev(Evt.EDOC),
            mkev(Evt.ESTR),
        };
        testEvt_(ys, expected);
    }

    public void testFilterCase()
    {
        String ys = "" +
            "plain: well\n" +
            "  a\n" +
            "  b\n" +
            "  c\n" +
            "squo: 'single''quote'\n" +
            "dquo: \"x\\t\\ny\"\n" +
            "lit: |\n" +
            "     X\n" +
            "     Y\n" +
            "     Z\n" +
            "fold: >\n" +
            "     U\n" +
            "     V\n" +
            "     W\n";
        testEdn_(ys,
                 "(\n" +
                 "{:+ \"+MAP\"}\n" +
                 "{:+ \"=VAL\", := \"plain\"}\n" +
                 "{:+ \"=VAL\", := \"well a b c\"}\n" +
                 "{:+ \"=VAL\", := \"squo\"}\n" +
                 "{:+ \"=VAL\", :' \"single'quote\"}\n" +
                 "{:+ \"=VAL\", := \"dquo\"}\n" +
                 "{:+ \"=VAL\", :$ \"x\\t\\ny\"}\n" +
                 "{:+ \"=VAL\", := \"lit\"}\n" +
                 "{:+ \"=VAL\", :| \"X\\nY\\nZ\\n\"}\n" +
                 "{:+ \"=VAL\", := \"fold\"}\n" +
                 "{:+ \"=VAL\", :> \"U V W\\n\"}\n" +
                 "{:+ \"-MAP\"}\n" +
                 "{:+ \"-DOC\"}\n" +
                 ")\n");
        ExpectedEvent[] expected = {
           mkev(Evt.BSTR),
           mkev(Evt.BDOC),
           mkev(Evt.VAL_|Evt.BMAP|Evt.BLCK),
           mkev(Evt.KEY_|Evt.SCLR|Evt.PLAI, 0, 5, "plain"),
           mkev(Evt.VAL_|Evt.SCLR|Evt.PLAI, 7, 10, "well a b c"),
           mkev(Evt.KEY_|Evt.SCLR|Evt.PLAI, 24, 4, "squo"),
           mkev(Evt.VAL_|Evt.SCLR|Evt.SQUO, 31, 12, "single'quote"),
           mkev(Evt.KEY_|Evt.SCLR|Evt.PLAI, 46, 4, "dquo"),
           mkev(Evt.VAL_|Evt.SCLR|Evt.DQUO, 53, 4, "x\t\ny"),
           mkev(Evt.KEY_|Evt.SCLR|Evt.PLAI, 61, 3, "lit"),
           mkev(Evt.VAL_|Evt.SCLR|Evt.LITL, 68, 6, "X\nY\nZ\n"),
           mkev(Evt.KEY_|Evt.SCLR|Evt.PLAI, 89, 4, "fold"),
           mkev(Evt.VAL_|Evt.SCLR|Evt.FOLD, 97, 6, "U V W\n"),
           mkev(Evt.EMAP),
           mkev(Evt.EDOC),
           mkev(Evt.ESTR),
        };
        testEvt_(ys, expected);
    }

    public void testFailure()
    {
        Rapidyaml rapidyaml = new Rapidyaml();
        String ys = ": : : :";
        boolean gotit = false;
        try {
            rapidyaml.parseYsToEdn(ys);
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
