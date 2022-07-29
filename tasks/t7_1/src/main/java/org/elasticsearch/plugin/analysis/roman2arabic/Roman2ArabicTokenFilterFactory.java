package org.elasticsearch.plugin.analysis.roman2arabic;


import org.apache.lucene.analysis.TokenStream;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory;

import java.util.Map;
import java.util.regex.Pattern;


public class Roman2ArabicTokenFilterFactory extends AbstractTokenFilterFactory {
    public static final String NAME = "roman2arabic";
    static final String VALIDATION_PATTERN_PARAM_NAME = "roman_validation_pattern";

    private final Pattern romanValidationPattern;

    public Roman2ArabicTokenFilterFactory(IndexSettings indexSettings, Environment environment, String s, Settings settings) {
        super(indexSettings, s, settings);
        final String validationRegex = settings.get(VALIDATION_PATTERN_PARAM_NAME, Utils.DEFAULT_ROMAN_VALIDATION_PATTERN);
        romanValidationPattern = Pattern.compile(validationRegex);
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new Roman2ArabicTokenFilter(tokenStream, romanValidationPattern);
    }

    // Useful for testing with Lucene' CustomAnalyzer
    public static class Lucene extends org.apache.lucene.analysis.TokenFilterFactory {
        private final Pattern romanValidationPattern;

        public Lucene(Map<String, String> params) {
            super(params);
            final String validationRegex = params.getOrDefault(VALIDATION_PATTERN_PARAM_NAME, Utils.DEFAULT_ROMAN_VALIDATION_PATTERN);
            romanValidationPattern = Pattern.compile(validationRegex);
        }

        @Override
        public TokenStream create(TokenStream tokenStream) {
            return new Roman2ArabicTokenFilter(tokenStream, romanValidationPattern);
        }
    }
}
