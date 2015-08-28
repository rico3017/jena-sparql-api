package org.aksw.jena_sparql_api.concept_cache;

import org.aksw.jena_sparql_api.concept_cache.domain.QuadFilterPatternCanonical;

import com.hp.hpl.jena.sparql.algebra.Table;

class CacheHit {
    private QuadFilterPatternCanonical replacementPattern;
    private QuadFilterPatternCanonical diffPattern;
    private Table table;

    public CacheHit(QuadFilterPatternCanonical replacementPattern, QuadFilterPatternCanonical diffPattern, Table table) {
        super();
        this.replacementPattern = replacementPattern;
        this.diffPattern = diffPattern;
        this.table = table;
    }

    public QuadFilterPatternCanonical getReplacementPattern() {
        return this.replacementPattern;
    }

    public QuadFilterPatternCanonical getDiffPattern() {
        return diffPattern;
    }

    public Table getTable() {
        return table;
    }
}