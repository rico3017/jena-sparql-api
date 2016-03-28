package org.aksw.jena_sparql_api.concept_cache.core;

import java.util.Collection;
import java.util.Map;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.utils.ElementUtils;
import org.aksw.jena_sparql_api.utils.NodeTransformRenameMap;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.OpVars;
import org.apache.jena.sparql.algebra.op.OpService;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.iterator.QueryIteratorResultSet;
import org.apache.jena.sparql.engine.main.OpExecutor;
import org.apache.jena.sparql.graph.NodeTransform;
import org.apache.jena.sparql.graph.NodeTransformLib;


//class CacheConfig {
//    protected ConceptMap conceptMap;
//
//
//}

public class OpExecutorViewCache
    extends OpExecutor
{
    protected Map<Node, QueryExecutionFactory> serviceToQef;


    protected OpExecutorViewCache(ExecutionContext execCxt, Map<Node, QueryExecutionFactory> serviceToQef) {
        super(execCxt);
        this.serviceToQef = serviceToQef;
    }

    @Override
    protected QueryIterator execute(OpService opService, QueryIterator input) {

        Node serviceNode = opService.getService();
        String serviceUri = serviceNode.getURI();

        QueryIterator result;
        if(serviceUri.startsWith("cache://")) {
            //SparqlCacheUtils.
            QueryExecutionFactory qef = serviceToQef.get(serviceNode);
            if(qef == null) {
                throw new RuntimeException("Could not find a query execution factory for " + serviceUri);
            }

            //ElementService rootElt = opService.getServiceElement();
            // By convention, the subElement must be a sub query
            //ElementSubQuery subQueryElt = (ElementSubQuery)rootElt.getElement();

            //TransformCopy
            //ElementUtils.fixVarNames(element)
            Op op = opService.getSubOp();

            Collection<Var> vars = OpVars.mentionedVars(op);
            Map<Node, Var> nodeMap = ElementUtils.createMapFixVarNames(vars);
            NodeTransform nodeTransform = new NodeTransformRenameMap(nodeMap);

            op = NodeTransformLib.transform(nodeTransform, op);

            Query query = OpAsQuery.asQuery(op);

            //Rename.renameNode(op, oldName, newName)
            // TODO Why is this hack / fix of variable names starting with a '/' necessary? Can we get rid of it?
            query.setQueryPattern(ElementUtils.fixVarNames(query.getQueryPattern()));


            //Query query = subQueryElt.getQuery();

            System.out.println("Executing: " + query);

            QueryExecution qe = qef.createQueryExecution(query);
            ResultSet rs = qe.execSelect();


//            ResultSetViewCache.cacheResultSet(physicalRs, indexVars, indexResultSetSizeThreshold, conceptMap, pqfp);
//
//            QueryExecution qe = qef.createQueryExecution(query);
//            ResultSet rs = qe.execSelect();
//            QueryIterator result = new QueryIteratorResultSet(rs);
//
//            //QueryExecutionFactory
//
//            System.out.println("here");

            result = new QueryIteratorResultSet(rs);

        } else {
            result = super.exec(opService, input);
        }

        return result;
    }


}