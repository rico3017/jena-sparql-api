package org.aksw.jena_sparql_api.hop;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.mapper.MappedQuery;

import com.hp.hpl.jena.sparql.core.DatasetGraph;

public class HopQuery
    extends HopBase
{
    protected MappedQuery<DatasetGraph> mappedQuery;

    public HopQuery(MappedQuery<DatasetGraph> mappedQuery, QueryExecutionFactory qef) {
        super(qef);
        this.mappedQuery = mappedQuery;
    }

    public MappedQuery<DatasetGraph> getMappedQuery() {
        return mappedQuery;
    }
}
