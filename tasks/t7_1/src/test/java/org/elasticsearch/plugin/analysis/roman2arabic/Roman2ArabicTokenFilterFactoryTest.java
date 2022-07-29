package org.elasticsearch.plugin.analysis.roman2arabic;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class Roman2ArabicTokenFilterFactoryTest {
    private Analyzer analyzer;
    private final String testSequence = "XII    MMC     LXXX   XXX  MMMCMXCIX";
    private final String[] answers =    {"12",  "2100", "80",  "30", "3999"};

    @BeforeEach
    void setUp() throws IOException {
        analyzer = CustomAnalyzer
                .builder()
                .withTokenizer("standard")
                .addTokenFilter(Roman2ArabicTokenFilterFactory.Lucene.class)
                .build();
    }

    @Test
    void test_custom_analyzer_with_roman2arabic_token_filter() throws IOException {
        final TokenStream ts = analyzer.tokenStream("field1", testSequence);
        final CharTermAttribute attr = ts.addAttribute(CharTermAttribute.class);
        ts.reset();
        var i = 0;
        while(ts.incrementToken()) {
            Assertions.assertEquals(answers[i], attr.toString());
            i++;
        }
    }

    @AfterEach
    void cleanUp() {
        analyzer.close();
    }
}
