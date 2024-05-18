package org.rapidyaml;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

public interface ILibRapidyaml extends Library
{
    Pointer ys2edn_init();
    void ys2edn_destroy(Pointer ryml2edn);
    //String ys2edn_alloc(Pointer ryml2edn, String filename, String ys, int ys_length);
    String ys2edn_alloc(Pointer ryml2edn, String filename, byte[] ys, int ys_length);
    int ys2edn(Pointer ryml2edn, String filename,
               byte[] ys, int ys_length,
               byte[] edn, int edn_length);
    int ys2edn_retry_get(Pointer ryml2edn, byte[] edn, int edn_size);
}
