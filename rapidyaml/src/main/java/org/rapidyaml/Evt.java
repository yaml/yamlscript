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
    public static final int PLAI = 1 << 10;  // : (plain scalar)
    public static final int SQUO = 1 << 11;  // ' (single-quoted scalar)
    public static final int DQUO = 1 << 12;  // " (double-quoted scalar)
    public static final int LITL = 1 << 13;  // | (block literal scalar)
    public static final int FOLD = 1 << 14;  // > (block folded scalar)

    public static final int FLOW = 1 << 15;  // flow container:
                                             // [] for seqs or {} for maps
    public static final int BLCK = 1 << 16;  // block container

    // Modifiers
    public static final int ANCH = 1 << 17;  // anchor
    public static final int TAG_ = 1 << 18;  // tag

    // Structure flags
    public static final int KEY_ = 1 << 19;  // as key
    public static final int VAL_ = 1 << 20;  // as value
    public static final int EXPL = 1 << 21;  // --- (with BDOC) or
                                             // ... (with EDOC)
                                             // (may be fused with FLOW
                                             // if needed)

    // Buffer flags
    ///< IMPORTANT. Marks events whose string was placed in the
    ///< arena. Fhis happens when the filtered string is larger than the
    ///< original string in the YAML code (eg from tags that resolve to
    ///< a larger string, or from "\L" or "\P" in double quotes, which
    ///< expand from two to three bytes). Because of this size
    ///< expansion, the filtered string cannot be placed in the original
    ///< source and needs to be placed in the arena.
    public static final int AREN = 1 << 22;
    ///< special flag to enable look back in the event array. it
    ///< signifies that the previous event has a string, meaning that
    ///< the jump back to that event is 3 positions. without this flag it
    ///< would be impossible to jump to the previous event
    public static final int PSTR = (1 << 23);
    ///< special flag to mark a scalar as unfiltered (when the parser
    ///< is set not to filter)
    public static final int UNFILT = (1 << 24);

    // Utility flags/masks
    public static final int LAST = UNFILT;
    public static final int MASK = ((LAST << 1) - 1);
    public static final int HAS_STR = SCLR|ALIA|ANCH|TAG_;

}
