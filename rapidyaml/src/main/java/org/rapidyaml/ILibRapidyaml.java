package org.rapidyaml;

import com.sun.jna.Library;

public interface ILibRapidyaml extends Library {
    String ys2evts_create(String src);
    void ys2evts_destroy(String evts);
}
