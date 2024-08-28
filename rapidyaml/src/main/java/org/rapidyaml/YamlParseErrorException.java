package org.rapidyaml;

// https://www.baeldung.com/java-new-custom-exception
public class YamlParseErrorException extends Exception
{
    public final int offset;
    public final int line;
    public final int column;
    public YamlParseErrorException(int offset_, int line_, int column_, String msg)
    {
        super(msg);
        offset = offset_;
        line = line_;
        column = column_;
    }
}
