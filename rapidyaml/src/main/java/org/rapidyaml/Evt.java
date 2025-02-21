package org.rapidyaml;

public class Evt {
    // Event types
    public static final int BSTR = 1 <<  0;  // +STR
    public static final int ESTR = 1 <<  1;  // -STR
    public static final int BDOC = 1 <<  2;  // +DOC
    public static final int EDOC = 1 <<  3;  // -DOC
    public static final int BMAP = 1 <<  4;  // +MAP
    public static final int EMAP = 1 <<  5;  // -MAP
    public static final int BSEQ = 1 <<  6;  // +SEQ
    public static final int ESEQ = 1 <<  7;  // -SEQ
    public static final int SCLR = 1 <<  8;  // =VAL
    public static final int ALIA = 1 <<  9;  // =ALI

    // Style flags
    public static final int PLAI = 1 << 16;  // : (plain scalar)
    public static final int SQUO = 1 << 17;  // ' (single-quoted scalar)
    public static final int DQUO = 1 << 18;  // " (double-quoted scalar)
    public static final int LITL = 1 << 19;  // | (block literal scalar)
    public static final int FOLD = 1 << 20;  // > (block folded scalar)

    public static final int FLOW = 1 << 21;  // flow container:
                                             // [] for seqs or {} for maps
    public static final int BLCK = 1 << 22;  // block container

    // Modifiers
    public static final int ANCH = 1 << 24;  // anchor
    public static final int TAG_ = 1 << 25;  // tag

    // Structure flags
    public static final int KEY_ = 1 << 26;  // as key
    public static final int VAL_ = 1 << 27;  // as value
    public static final int EXPL = 1 << 28;  // --- (with BDOC) or
                                             // ... (with EDOC)
                                             // (may be fused with FLOW
                                             // if needed)

    // Utility flags
    public static final int LAST = EXPL;
    public static final int MASK = ((LAST << 1) - 1);
    public static final int HAS_STR = SCLR|ALIA|ANCH|TAG_;

}
