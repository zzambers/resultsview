/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package resultsview.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author zzambers
 */
public class VersionUtil {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]+");

    public static int versionCompare(String s1, String s2) {
        if (s1.equals(s2)) {
            return 0;
        }
        int curIndex1 = 0;
        int curIndex2 = 0;
        Matcher m1 = NUMBER_PATTERN.matcher(s1);
        Matcher m2 = NUMBER_PATTERN.matcher(s2);
        for (;;) {
            boolean found1 = m1.find();
            boolean found2 = m2.find();
            /* index of next digit (from current index) */
            int digitIndex1 = found1 ? m1.start() : s1.length();
            int digitIndex2 = found2 ? m2.start() : s2.length();
            int digitDistance1 = digitIndex1 - curIndex1;
            int digitDistance2 = digitIndex2 - curIndex2;
            if (digitDistance1 != digitDistance2) {
                /* distance to next digit is different for s1 and s2
                   => normal compare is used */
                return s1.substring(curIndex1).compareTo(s2.substring(curIndex2));
            }
            /* compare substrings before next digit */
            int cmp = s1.substring(curIndex1, curIndex1 + digitDistance1)
                    .compareTo(s2.substring(curIndex2, curIndex2 + digitDistance2));
            if (cmp != 0) {
                return cmp;
            }
            // substrings are equal
            if (!found1) {
                if (!found2) {
                    /* reached end of both strings => whole strings are equal */
                    return 0;
                }
                /* reached end of s1 => s1 is substring of s2 */
                return -1;
            }
            if (!found2) {
                /* reached end of s2 => s2 is substring of s1 */
                return 1;
            }
            int end1 = m1.end();
            int end2 = m2.end();
            int numLength1 = end1 - digitIndex1;
            int numLength2 = end2 - digitIndex2;
            int biggerNumLength = Math.max(numLength1, numLength2);
            int missingDigits1 = biggerNumLength - numLength1;
            int missingDigits2 = biggerNumLength - numLength2;
            for (int i = 0; i < biggerNumLength; ++i) {
                /* treat missing digits from left as zeros */
                char digit1 = i >= missingDigits1 ? s1.charAt(digitIndex1 + i - missingDigits1) : '0';
                char digit2 = i >= missingDigits2 ? s2.charAt(digitIndex2 + i - missingDigits2) : '0';
                if (digit2 != digit1) {
                    return digit1 > digit2 ? 1 : -1;
                }
            }
            curIndex1 = end1;
            curIndex2 = end2;
        }
    }

}
