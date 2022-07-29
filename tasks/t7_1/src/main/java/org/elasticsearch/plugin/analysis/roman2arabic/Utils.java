package org.elasticsearch.plugin.analysis.roman2arabic;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Utils {
    private static final String[] ROMAN = {"I", "IV", "V", "IX", "X", "XL", "L", "XC", "C", "CD", "D", "CM", "M"};
    private static final int[] ARABIC   = {1,    4,    5,   9,    10,  40,  50,   90,  100,  400, 500, 900, 1000};

    private Utils() {}

    static int roman2Arabic(CharSequence roman) {
        var ret = 0;
        var i = ARABIC.length - 1;
        var pos = 0;
        while(i >= 0 && pos < roman.length())
        {
            if(CharSequence.compare(
                    roman.subSequence(pos, Math.min(pos + ROMAN[i].length(), roman.length())),
                    ROMAN[i]
            ) == 0) {
                ret += ARABIC[i];
                pos += ROMAN[i].length(); //MMMXLVII
            }
            else {
                i--;
            }
        }
        return ret;
    }

    static boolean isRoman(CharSequence string, Pattern p) {
        final Matcher m = p.matcher(string);
        return m.matches();
    }

    static boolean isRoman(CharSequence string) {
        final Pattern p = Pattern.compile(DEFAULT_ROMAN_VALIDATION_PATTERN);
        final Matcher m = p.matcher(string);
        return m.matches();
    }

    public static final String DEFAULT_ROMAN_VALIDATION_PATTERN = "^M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})$";
}
