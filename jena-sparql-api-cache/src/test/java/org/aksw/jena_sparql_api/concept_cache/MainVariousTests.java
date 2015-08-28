package org.aksw.jena_sparql_api.concept_cache;

import org.aksw.jena_sparql_api.concept_cache.op.OpUtils;
import org.junit.Test;

import com.hp.hpl.jena.query.ARQ;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.sparql.algebra.Algebra;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.optimize.Optimize;
import com.hp.hpl.jena.sparql.algebra.optimize.Rewrite;
import com.hp.hpl.jena.sparql.util.Context;

public class MainVariousTests {


    @Test
    public void test1() {
        String str = "Select Distinct ?s ?o { { ?s ?p ?o } Union { ?s ?p ?o } Filter(?p = <a>)}";
        Query query = QueryFactory.create(str, Syntax.syntaxSPARQL_11);
        Op op = Algebra.compile(query);
        op = Algebra.toQuadForm(op);


        Context context = new Context();
        context.put(ARQ.optMergeBGPs, true);
        context.put(ARQ.optMergeExtends, true);
        context.put(ARQ.optExprConstantFolding, true);
        //context.put(ARQ.optFilterPlacement, true);
        context.put(ARQ.optFilterConjunction, true);
        context.put(ARQ.optImplicitLeftJoin, true);

        // It is important to keep optFilterEquality turned off!
        // Otherwise it may push constants back into the quads
        context.put(ARQ.optFilterEquality, false);
        context.put(ARQ.optFilterInequality, false);
        context.put(ARQ.optDistinctToReduced, false);
        context.put(ARQ.optFilterExpandOneOf, false);
        context.put(ARQ.optFilterPlacementBGP, false);

        Rewrite rewriter = Optimize.getFactory().create(context);

        op = rewriter.rewrite(op);// Algebra.optimize(op);


        Object summary = OpUtils.summarize(op);
        System.out.println("Summary: " + summary);
        System.out.println("IsEquivalent: " + OpUtils.isEquivalent(op, op));


        System.out.println("yay" + op);
    }
}