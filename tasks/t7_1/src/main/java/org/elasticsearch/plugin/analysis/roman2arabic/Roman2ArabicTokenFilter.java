package org.elasticsearch.plugin.analysis.roman2arabic;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import java.io.IOException;
import java.util.regex.Pattern;

public final class Roman2ArabicTokenFilter extends TokenFilter {
    private final CharTermAttribute charTermAttr;
    private final TypeAttribute typeAtt;
    private final Pattern validationPattern;

    public Roman2ArabicTokenFilter(TokenStream input, Pattern romanValidationPattern) {
        super(input);
        this.charTermAttr = addAttribute(CharTermAttribute.class);
        this.typeAtt = addAttribute(TypeAttribute.class);
        this.validationPattern = romanValidationPattern;
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (!input.incrementToken()) {
            return false;
        }

        if (Utils.isRoman(charTermAttr, validationPattern)) {
            int arabicRepresentation = Utils.roman2Arabic(charTermAttr);
            charTermAttr.setEmpty();
            charTermAttr.append(String.valueOf(arabicRepresentation));
            typeAtt.setType("<NUM>");
        }
        return true;
    }
}
