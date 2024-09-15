package org.rapidyaml;

public class Evt {
    // ---------------------
    // structure flags
    public static final int KEY_ = 1 <<  0;  // as key
    public static final int VAL_ = 1 <<  1;  // as value
    public static final int SCLR = 1 <<  2;  // =VAL
    public static final int BSEQ = 1 <<  3;  // +SEQ
    public static final int ESEQ = 1 <<  4;  // -SEQ
    public static final int BMAP = 1 <<  5;  // +MAP
    public static final int EMAP = 1 <<  6;  // -MAP
    public static final int ALIA = 1 <<  7;  // ref
    public static final int ANCH = 1 <<  8;  // anchor
    public static final int TAG_ = 1 <<  9;  // tag
    // ---------------------
    // style flags
    public static final int PLAI = 1 << 10;  // : (plain scalar)
    public static final int SQUO = 1 << 11;  // ' (single-quoted scalar)
    public static final int DQUO = 1 << 12;  // " (double-quoted scalar)
    public static final int LITL = 1 << 13;  // | (block literal scalar)
    public static final int FOLD = 1 << 14;  // > (block folded scalar)
    public static final int FLOW = 1 << 15;  // flow container: [] for seqs or {} for maps
    public static final int BLCK = 1 << 16;  // block container
    // ---------------------
    // document flags
    public static final int BDOC = 1 << 17;  // +DOC
    public static final int EDOC = 1 << 18;  // -DOC
    public static final int EXPL = 1 << 21;  // --- (with BDOC) or ... (with EDOC) (may be fused with FLOW if needed)
    public static final int BSTR = 1 << 19;  // +STR
    public static final int ESTR = 1 << 20;  // -STR
    // ---------------------
    // utility flags
    public static final int LAST = ESTR;
    public static final int MASK = ((LAST << 1) - 1);
    public static final int HAS_STR = SCLR|ALIA|ANCH|TAG_;
}
