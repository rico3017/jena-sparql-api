package org;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.aksw.commons.collections.multimaps.IBiSetMultimap;
import org.aksw.isomorphism.IsoMapUtils;
import org.aksw.isomorphism.Problem;
import org.aksw.isomorphism.ProblemContainer;
import org.aksw.isomorphism.ProblemContainerImpl;
import org.aksw.isomorphism.StateProblemContainer;
import org.aksw.jena_sparql_api.concept_cache.collection.ContainmentMap;
import org.aksw.jena_sparql_api.concept_cache.collection.ContainmentMapImpl;
import org.aksw.jena_sparql_api.concept_cache.combinatorics.ProblemVarMappingExpr;
import org.aksw.jena_sparql_api.concept_cache.combinatorics.ProblemVarMappingQuad;
import org.aksw.jena_sparql_api.concept_cache.core.SparqlCacheUtils;
import org.aksw.jena_sparql_api.concept_cache.domain.ProjectedQuadFilterPattern;
import org.aksw.jena_sparql_api.concept_cache.domain.QuadFilterPatternCanonical;
import org.aksw.jena_sparql_api.stmt.SparqlElementParser;
import org.aksw.jena_sparql_api.stmt.SparqlElementParserImpl;
import org.aksw.jena_sparql_api.utils.Generator;
import org.aksw.jena_sparql_api.utils.VarGeneratorImpl2;
import org.aksw.state_space_search.core.State;
import org.aksw.state_space_search.core.StateSearchUtils;
import org.apache.jena.query.Syntax;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.syntax.Element;
import org.apache.jena.sparql.util.ExprUtils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;



interface ExprQuadPattern {

}



public class TestStateSpaceSearch {
    public static void main(String[] args) throws FileNotFoundException {
        {
//            QueryExecutionFactory qef = FluentQueryExecutionFactory
//                    .http("http://linkedgeodata.org/test/vsparql")
//                    .config()
//                        .withParser(SparqlQueryParserImpl.create())
//                        .withDatasetDescription(DatasetDescriptionUtils.createDefaultGraph("http://linkedgeodata.org/ne/"))
//                        .withQueryTransform(F_QueryTransformDatesetDescription.fn)
//                    .end()
//                    .create();
//            Model model = qef.createQueryExecution("CONSTRUCT WHERE { ?s ?p ?o }").execConstruct();
//
//            //System.out.println(ResultSetFormatter.asText(qef.createQueryExecution("SELECT (count(*) As ?c) FROM <http://linkedgeodata.org/ne/> WHERE { ?s ?p ?o }").execSelect()));
//            model.write(new FileOutputStream(new File("/tmp/ne.nt")), "NTRIPLES");
            //model.write(System.out);
        }

        // We could create a graph over quads and expressions that variables

        SparqlElementParser elementParser = SparqlElementParserImpl.create(Syntax.syntaxSPARQL_10, null);
        Element queryElement = elementParser.apply("?x <my://type> <my://Airport> ; <my://label> ?n . FILTER(langMatches(lang(?n), 'en')) . FILTER(<mp://fn>(?x, ?n))");

        Element cacheElement = elementParser.apply("?s <my://type> <my://Airport> ; ?p ?l . FILTER(?p = <my://label> || ?p = <my://name>)");


        ProjectedQuadFilterPattern cachePqfp = SparqlCacheUtils.transform(cacheElement);
        System.out.println("ProjectedQuadFilterPattern[cache]: " + cachePqfp);

        ProjectedQuadFilterPattern queryPqfp = SparqlCacheUtils.transform(queryElement);
        System.out.println("ProjectedQuadFilterPattern[query]: " + queryPqfp);

        Generator<Var> generator = VarGeneratorImpl2.create();
        QuadFilterPatternCanonical cacheQfpc = SparqlCacheUtils.canonicalize2(cachePqfp.getQuadFilterPattern(), generator);
        System.out.println("QuadFilterPatternCanonical[cache]: " + cacheQfpc);


        QuadFilterPatternCanonical queryQfpc = SparqlCacheUtils.canonicalize2(queryPqfp.getQuadFilterPattern(), generator);
        System.out.println("QuadFilterPatternCanonical[query]: " + queryQfpc);



        // Index the clauses of the cache
        ContainmentMap<Expr, Multimap<Expr, Expr>> cacheIndex = indexDnf(cacheQfpc.getFilterDnf());
        ContainmentMap<Expr, Multimap<Expr, Expr>> queryIndex = indexDnf(queryQfpc.getFilterDnf());


        Collection<Problem<Map<Var, Var>>> problems = new ArrayList<>();
        for(Entry<Set<Expr>, Collection<Multimap<Expr, Expr>>> entry : queryIndex.entrySet()) {
            Set<Expr> querySig = entry.getKey();
            Collection<Multimap<Expr, Expr>> queryMaps = entry.getValue();

            System.out.println("CAND LOOKUP with " + querySig);
            Collection<Entry<Set<Expr>, Multimap<Expr, Expr>>> cands = cacheIndex.getAllEntriesThatAreSubsetOf(querySig);

            for(Entry<Set<Expr>, Multimap<Expr, Expr>> e : cands) {
                Multimap<Expr, Expr> cacheMap = e.getValue();
                System.out.println("  CACHE MAP: " + cacheMap);
                for(Multimap<Expr, Expr> queryMap : queryMaps) {
                    Map<Expr, Entry<Set<Expr>, Set<Expr>>> group = ProblemVarMappingQuad.groupByKey(cacheMap.asMap(), queryMap.asMap());

                    Collection<Problem<Map<Var, Var>>> localProblems = group.values().stream()
                        .map(x -> {
                            Set<Expr> cacheExprs = x.getKey();
                            Set<Expr> queryExprs = x.getValue();
                            Problem<Map<Var, Var>> p = new ProblemVarMappingExpr(cacheExprs, queryExprs, null);

                            //System.out.println("cacheExprs: " + cacheExprs);
                            //System.out.println("queryExprs: " + queryExprs);

                            //Stream<Map<Var, Var>> r = p.generateSolutions();

                            return p;
                        })
                        .collect(Collectors.toList());

                    problems.addAll(localProblems);
                    //problems.stream().forEach(p -> System.out.println("COMPLEX: " + p.getEstimatedCost()));


                    //problemStream.forEach(y -> System.out.println("GOT SOLUTION: " + y));



                    //System.out.println("    QUERY MAP: " + queryMap);
                }
            }

            //cands.forEach(x -> System.out.println("CAND: " + x.getValue()));
        }

        ProblemContainer<Map<Var, Var>> container = ProblemContainerImpl.create(problems);
        State<Map<Var, Var>> state = new StateProblemContainer<Map<Var, Var>>(null, container, (a, b) -> IsoMapUtils.mergeInPlaceIfCompatible(a, b));
        Stream<Map<Var, Var>> xxx = StateSearchUtils.depthFirstSearch(state, 10000);

        xxx.forEach(x -> System.out.println("SOLUTION: " + x));

        System.out.println("PROBLEMS: " + problems.size());




//        System.out.println(cacheSigToExprs);
//
//        ContainmentMap<Expr, Set<Expr>> querySigToExprs = new ContainmentMapImpl<>();
//        for(Set<Expr> clause : queryQfpc.getFilterDnf()) {
//            Set<Expr> clauseSig = ClauseUtils.signaturize(clause);
//            querySigToExprs.put(clauseSig, clause);
//        }
//
//        for(Set<Expr> queryClause : querySigToExprs.keySet()) {
//            System.out.println("CAND LOOKUP with " + queryClause);
//            Collection<Entry<Set<Expr>, Set<Expr>>> cands = cacheSigToExprs.getAllEntriesThatAreSubsetOf(queryClause);
//            cands.forEach(x -> System.out.println("CAND: " + x));
//        }


        //ClauseUtils.signaturize(clause)

        IBiSetMultimap<Quad, Set<Set<Expr>>> index = SparqlCacheUtils.createMapQuadsToFilters(cacheQfpc);
        System.out.println("Index: " + index);

        // Features are objects that describe view
        // A query needs to cover all features of view
        // so it must hold that |featuresOf(query)| >= |featuresOf(cache)|
        Set<Object> features = new HashSet<Object>();
        index.asMap().values().stream().flatMap(cnfs -> cnfs.stream())
            .flatMap(cnf -> cnf.stream())
            .filter(clause -> clause.size() == 1)
            .flatMap(clause -> clause.stream())
            .forEach(feature -> features.add(feature));


        ContainmentMap<Object, Object> featuresToCache = new ContainmentMapImpl<>();
        featuresToCache.put(features, cacheQfpc);


        // The problem graph
        //Graph<Problem<Map<Var, Var>>, DefaultEdge> problemGraph = new SimpleGraph<>(DefaultEdge.class);





        // Probably cache entries should be indexed using DNFs and the table system,
        // whereas lookups could be made using CNFs


        Collection<Entry<Set<Object>, Object>> candidates = featuresToCache.getAllEntriesThatAreSubsetOf(
                new HashSet<>(Arrays.asList(
                        ExprUtils.parse("?o = <my://Airport>"),
                        ExprUtils.parse("?p = <my://type>")
              )));

        System.out.println("Candidates: " + candidates);


        problems.forEach(p -> System.out.println("SOLUTIONS for " + p + " " + p.generateSolutions().collect(Collectors.toList())));


        //ProblemContainerImpl<Map<Var, Var>> container = ProblemContainerImpl.create(problems);
        //container.

//        ContainmentMap<Set<Expr>, Quad> clauseToQuads = new ContainmentMapImpl<>();
//        for(Entry<Quad, Set<Set<Expr>>> entry : index.entries()) {
//            clauseToQuads.put(entry.getValue(), entry.getKey());
//        }
//
        // given a query, we need to conver at least all constraints of the cache
        //clauseToQuads.getAllEntriesThatAreSubsetOf(prototye)


//        SparqlViewCacheImpl x;
//        x.lookup(queryQfpc)

        //QuadFilterPatternCanonical


//        Expr a = ExprUtils.parse("(?z = ?x + 1)");
//        Expr b = ExprUtils.parse("?a = ?b || (?c = ?a + 1) && (?k = ?i + 1)");
        //Expr b = ExprUtils.parse("?x = ?y || (?z = ?x + 1)");
//
//        Set<Set<Expr>> ac = CnfUtils.toSetCnf(b);
//        Set<Set<Expr>> bc = CnfUtils.toSetCnf(a);

//        Problem<Map<Var, Var>> p = new ProblemVarMappingExpr(ac, bc, Collections.emptyMap());
//
//        System.out.println("p");
//        System.out.println(p.getEstimatedCost());
//        ProblemVarMappingExpr.createVarMap(a, b).forEach(x -> System.out.println(x));

//        Collection<Quad> as = Arrays.asList(new Quad(Vars.g, Vars.s, Vars.p, Vars.o));
//        Collection<Quad> bs = Arrays.asList(new Quad(Vars.l, Vars.x, Vars.y, Vars.z));
//
//
//        //Collection<Quad> cq =
//        System.out.println("q");
//        Problem<Map<Var, Var>> q = new ProblemVarMappingQuad(as, bs, Collections.emptyMap());
//        System.out.println(q.getEstimatedCost());
//
//        q.generateSolutions().forEach(x -> System.out.println(x));


        //Maps.com

//        System.out.println("pc");
//        ProblemContainerImpl<Map<Var, Var>> pc = ProblemContainerImpl.create(p, q);
//        StateProblemContainer<Map<Var, Var>> state = new StateProblemContainer<>(Collections.emptyMap(), pc, SparqlCacheUtils::mergeCompatible);
        //SearchUtils.depthFirstSearch(state, isFinal, vertexToResult, vertexToEdges, edgeCostComparator, edgeToTargetVertex, depth, maxDepth)
//        StateSearchUtils.depthFirstSearch(state, 10).forEach(x -> System.out.println(x));


        // Next level: Matching Ops


        // Problem: We can now find whether there exist variable mappings between two expressions or sets of quads
        // But the next step is to determine which exact parts of the query can be substituted
        // The thing is: We need to compute the variable mapping, but once we have obtained it,
        // we could use the state configuration that led to the solution to efficiently determine
        // the appropriate substitutions



        //p.generateSolutions().forEach(x -> System.out.println(x));
    }


    public static ContainmentMap<Expr, Multimap<Expr, Expr>> indexDnf(Set<Set<Expr>> dnf) {
        ContainmentMap<Expr, Multimap<Expr, Expr>> result = new ContainmentMapImpl<>();
        for(Set<Expr> clause : dnf) {
            Multimap<Expr, Expr> exprSigToExpr = HashMultimap.create();
            Set<Expr> clauseSig = new HashSet<>();
            for(Expr expr : clause) {
                Expr exprSig = org.aksw.jena_sparql_api.utils.ExprUtils.signaturize(expr);
                exprSigToExpr.put(exprSig, expr);
                clauseSig.add(exprSig);
            }

            //Set<Expr> clauseSig = ClauseUtils.signaturize(clause);
            result.put(clauseSig, exprSigToExpr);
        }

        return result;
    }
}
