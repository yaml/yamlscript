package org.ryml.ryml;

import com.sun.jna.Library;

public interface ILibRyml extends Library {
    String parse_yamlscript_to_events(String src);
    void free_yamlscript_events(String buf);
}
