/* GENERATED SOURCE. DO NOT MODIFY. */
/*
 *******************************************************************************
 * Copyright (C) 2004-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package android.icu.text;

/**
 * A post-processor for Chinese text.
 */
final class RBNFChinesePostProcessor implements RBNFPostProcessor {
    //private NFRuleSet lastRuleSet;
    private boolean longForm;
    private int format;

    private static final String[] rulesetNames = {
        "%traditional", "%simplified", "%accounting", "%time"
    };

    /**
     * Initialization routine for this instance, called once
     * immediately after first construction and never again.  
     */
    public void init(RuleBasedNumberFormat formatter, String rules) {
    }

    /**
     * Work routine.  Post process the output, which was generated by the
     * ruleset with the given name.
     */
    public void process(StringBuffer buf, NFRuleSet ruleSet) {
        // markers depend on what rule set we are using

        // Commented by johnvu on the if statement since lastRuleSet is never initialized
        //if (ruleSet != lastRuleSet) {
            String name = ruleSet.getName();
            for (int i = 0; i < rulesetNames.length; ++i) {
                if (rulesetNames[i].equals(name)) {
                    format = i;
                    longForm = i == 1 || i == 3;
                    break;
                }
            }
        //}

        if (longForm) {
            for (int i = buf.indexOf("*"); i != -1; i = buf.indexOf("*", i)) {
                buf.delete(i, i+1);
            }
            return;
        }

        final String DIAN = "\u9ede"; // decimal point

        final String[][] markers = {
            { "\u842c", "\u5104", "\u5146", "\u3007" }, // marker chars, last char is the 'zero'
            { "\u4e07", "\u4ebf", "\u5146", "\u3007" },
            { "\u842c", "\u5104", "\u5146", "\u96f6" }
            // need markers for time?
        };

        // remove unwanted lings
        // a '0' (ling) with * might be removed
        // mark off 10,000 'chunks', markers are Z, Y, W (zhao, yii, and wan)
        // already, we avoid two lings in the same chunk -- ling without * wins
        // now, just need  to avoid optional lings in adjacent chunks
        // process right to left

        // decision matrix:
        // state, situation
        //     state         none       opt.          req.
        //     -----         ----       ----          ----
        // none to right     none       opt.          req.  
        // opt. to right     none   clear, none  clear right, req.
        // req. to right     none   clear, none       req.

        // mark chunks with '|' for convenience
        {
            String[] m = markers[format];
            for (int i = 0; i < m.length-1; ++i) {
                int n = buf.indexOf(m[i]);
                if (n != -1) {
                    buf.insert(n+m[i].length(), '|');
                }
            }
        }

        int x = buf.indexOf(DIAN);
        if (x == -1) {
            x = buf.length();
        }
        int s = 0; // 0 = none to right, 1 = opt. to right, 2 = req. to right
        int n = -1; // previous optional ling
        String ling = markers[format][3];
        while (x >= 0) {
            int m = buf.lastIndexOf("|", x);
            int nn = buf.lastIndexOf(ling, x);
            int ns = 0;
            if (nn > m) {
                ns = (nn > 0 && buf.charAt(nn-1) != '*') ? 2 : 1;
            }
            x = m - 1;

            // actually much simpler, but leave this verbose for now so it's easier to follow
            switch (s*3+ns) {
            case 0: /* none, none */
                s = ns; // redundant
                n = -1;
                break;
            case 1: /* none, opt. */
                s = ns;
                n = nn; // remember optional ling to right
                break;
            case 2: /* none, req. */
                s = ns;
                n = -1;
                break;
            case 3: /* opt., none */
                s = ns;
                n = -1;
                break;
            case 4: /* opt., opt. */
                buf.delete(nn-1, nn+ling.length()); // delete current optional ling
                s = 0;
                n = -1;
                break;
            case 5: /* opt., req. */
                buf.delete(n-1, n+ling.length()); // delete previous optional ling
                s = ns;
                n = -1;
                break;
            case 6: /* req., none */
                s = ns;
                n = -1;
                break;
            case 7: /* req., opt. */
                buf.delete(nn-1, nn+ling.length()); // delete current optional ling
                s = 0;
                n = -1;
                break;
            case 8: /* req., req. */
                s = ns;
                n = -1;
                break;
            default:
                throw new IllegalStateException();
            }
        }

        for (int i = buf.length(); --i >= 0;) {
            char c = buf.charAt(i);
            if (c == '*' || c == '|') {
                buf.delete(i, i+1);
            }
        }
    }
}
