package org.elasticsearch.plugin.analysis.roman2arabic;

import org.elasticsearch.index.analysis.TokenFilterFactory;
import org.elasticsearch.indices.analysis.AnalysisModule;
import org.elasticsearch.plugins.AnalysisPlugin;
import org.elasticsearch.plugins.Plugin;

import java.util.HashMap;
import java.util.Map;

public class Roman2ArabicAnalyzerPlugin extends Plugin implements AnalysisPlugin {

    @Override
    public Map<String, AnalysisModule.AnalysisProvider<TokenFilterFactory>> getTokenFilters() {
        Map<String, AnalysisModule.AnalysisProvider<TokenFilterFactory>> tokenFilters =
                new HashMap<>();

        tokenFilters.put(Roman2ArabicTokenFilterFactory.NAME, Roman2ArabicTokenFilterFactory::new);
        return tokenFilters;
    }
}
