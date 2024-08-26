package org.rapidyaml;

// https://www.baeldung.com/java-new-custom-exception
public class YamlParseErrorException extends Exception
{
    public YamlParseErrorException(String errorMessage) {
        super(errorMessage);
    }
}
