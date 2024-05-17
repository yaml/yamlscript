package org.rapidyaml;

import com.sun.jna.Library;

public interface ILibRapidyaml extends Library {
    String ys2edn_create(String src);
    void ys2edn_destroy(String evts);
}
