package org.rapidyaml;

import com.sun.jna.Library;

public interface ILibRapidyaml extends Library {
    String ys2edn_stateless(String filename, String src, int src_length);
    // TODO add the more efficient methods
}
