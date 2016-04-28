package org.aksw.jena_sparql_api.concept_cache.combinatorics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.aksw.commons.collections.CartesianProduct;
import org.aksw.commons.collections.MapUtils;
import org.aksw.commons.collections.multimaps.IBiSetMultimap;
import org.aksw.jena_sparql_api.concept_cache.core.SparqlCacheUtils;
import org.aksw.jena_sparql_api.concept_cache.domain.PatternSummary;
import org.aksw.jena_sparql_api.utils.Pair;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.Expr;
import org.paukov.combinatorics.Factory;
import org.paukov.combinatorics.Generator;
import org.paukov.combinatorics.ICombinatoricsVector;

import com.codepoetics.protonpack.StreamUtils;

public class CombinatoricsUtils {

    public static Stream<Map<Var, Var>> computeVarMapQuadBased(PatternSummary needle, PatternSummary haystack, Set<Set<Var>> candVarCombos) {
        Stream<Map<Var, Var>> result = computeVarMapQuadBased(needle.getQuadToCnf(), haystack.getQuadToCnf(), candVarCombos);
        return result;
    }

    /**
     * TODO the quad groups are equivalent classes - this seems to be in essence what JgraphT isomorphims tooling does
     * http://jgrapht.org/javadoc/org/jgrapht/alg/isomorphism/VF2GraphIsomorphismInspector.html (and SubGraphIsomorphism) variant
     *
     * Find a mapping of variables from cand to query, such that the pattern of
     * cand becomes a subset of that of query
     *
     * null if no mapping can be established
     *
     * @param query
     * @param cand
     * @return
     */
    public static Stream<Map<Var, Var>> computeVarMapQuadBased(IBiSetMultimap<Quad, Set<Set<Expr>>> queryQuadToCnf, IBiSetMultimap<Quad, Set<Set<Expr>>> candQuadToCnf, Set<Set<Var>> candVarCombos) {

        IBiSetMultimap<Set<Set<Expr>>, Quad> cnfToCandQuad = candQuadToCnf.getInverse();
        IBiSetMultimap<Set<Set<Expr>>, Quad> cnfToQueryQuad = queryQuadToCnf.getInverse();

        //IBiSetMultimap<Quad, Quad> candToQuery = new BiHashMultimap<Quad, Quad>();
//        Map<Set<Set<Expr>>, QuadGroup> cnfToQuadGroup = new HashMap<Set<Set<Expr>>, QuadGroup>();


        // TODO Replace quad group by a pair object
        // Note: quad groups are equivalence classes
        List<QuadGroup> quadGroups = new ArrayList<QuadGroup>();
        for(Entry<Set<Set<Expr>>, Collection<Quad>> entry : cnfToCandQuad.asMap().entrySet()) {

            //Quad candQuad = entry.getKey();
            Set<Set<Expr>> cnf = entry.getKey();

            Collection<Quad> candQuads = entry.getValue();
            Collection<Quad> queryQuads = cnfToQueryQuad.get(cnf);

            if(queryQuads.isEmpty()) {
                return Collections.<Map<Var, Var>>emptySet().stream();
            }

            QuadGroup quadGroup = new QuadGroup(candQuads, queryQuads);
            quadGroups.add(quadGroup);

            // TODO We now have grouped together quad having the same constraint summary
            // Can we derive some additional constraints form the var occurrences?
        }

        // Order the equivalence classes by the number of possible combinations
        // - least number of candidates first
        Collections.sort(quadGroups, new Comparator<QuadGroup>() {
            @Override
            public int compare(QuadGroup a, QuadGroup b) {
                int i = Utils2.getNumMatches(a);
                int j = Utils2.getNumMatches(b);
                int r = i - j;
                return r;
            }
        });

        // TODO Somehow obtain a base mapping (is that even possible?)
        Map<Var, Var> baseMapping = Collections.<Var, Var>emptyMap();


        List<Iterable<Map<Var, Var>>> partialSolutions = quadGroups.stream()
                .map(x -> createSolutions(x, baseMapping))
                .collect(Collectors.toList())
                ;

        // Create the cartesian product over the partial solutions
        CartesianProduct<Map<Var, Var>> cart = new CartesianProduct<Map<Var,Var>>(partialSolutions);

        //cart.stream().forEach(i -> System.out.println("Cart: " + i));

        // Combine the solutions of each equivalence class into an overall solution,
        // thereby filter out incompatible bindings (indicated by null)
        Stream<Map<Var, Var>> result = cart.stream()
            .map(solutionParts -> SparqlCacheUtils.mergeCompatible(solutionParts))
            .filter(Objects::nonNull);

        return result;
    }

    public static Iterable<Map<Var, Var>> createSolutions(Entry<? extends Collection<Quad>, ? extends Collection<Quad>> quadGroup, Map<Var, Var> baseSolution) {
        Iterable<Map<Var, Var>> result = () -> createSolutionStream(quadGroup, baseSolution).iterator();
        return result;
    }

    public static Stream<Map<Var, Var>> createSolutionStream(Entry<? extends Collection<Quad>, ? extends Collection<Quad>> quadGroup, Map<Var, Var> baseSolution) {
        // Reminder: We need to find a mapping from candidate vars to those of the query
        // The cache must have fewer quads than the query to be applicable
        // so we take all combinations and permutations of the *query*'s quads and match them with the cache quads
        Collection<Quad> candQuads = quadGroup.getKey();
        Collection<Quad> queryQuads = quadGroup.getValue();

        ICombinatoricsVector<Quad> queryQuadVector = Factory.createVector(queryQuads);
        Generator<Quad> queryQuadCombis = Factory.createSimpleCombinationGenerator(queryQuadVector, candQuads.size());

        Stream<Map<Var, Var>> result = StreamSupport
            .stream(queryQuadCombis.spliterator(), false)
            .flatMap(tmp -> {
                // Not sure if we need a copy here
                ICombinatoricsVector<Quad> queryQuadCombi = Factory.createVector(tmp);
                Generator<Quad> permutations = Factory.createPermutationGenerator(queryQuadCombi);
                Stream<ICombinatoricsVector<Quad>> perm = StreamSupport.stream(permutations.spliterator(), false);

                Stream<Map<Var, Var>> r = perm
                        .map(tmpQueryQuads -> reduceToVarMap(candQuads, tmpQueryQuads));

                //perm.peek(test -> System.out.println("PEEKABOO: " + test));

                return r;
            })
            .filter(Objects::nonNull);


        Collection<Map<Var, Var>> r = result.collect(Collectors.toList());
        //System.out.println("solutions: " + r);
        result = r.stream();


        return result;
    }

    public static Map<Var, Var> reduceToVarMap(Iterable<Quad> candQuads, Iterable<Quad> queryQuads) {
        Map<Var, Var> result = StreamUtils
            .zip(
                    StreamSupport.stream(candQuads.spliterator(), false),
                    StreamSupport.stream(queryQuads.spliterator(), false),
                    (a, b) -> new Pair<Quad>(a, b))
            .map(pair -> Utils2.createVarMap(pair.getKey(), pair.getValue()))
            .reduce(new HashMap<Var, Var>(), CombinatoricsUtils::mergeIntoIfCompatible);

        return result;
    }

    public static Map<Var, Var> mergeIntoIfCompatible(Map<Var, Var> inout, Map<Var, Var> addition) {
        Map<Var, Var> result = null;
        if(inout != null && addition != null) {
            boolean isCompatible = MapUtils.isPartiallyCompatible(inout, addition);
            if(isCompatible) {
                inout.putAll(addition);
                result = inout;
            }
        }

        return result;
    }

}



//SetMultimap<Quad, Quad> summaryToQuadsCand = quadJoinSummary(new ArrayList<Quad>(candQuads));
//System.out.println("JoinSummaryCand: " + summaryToQuadsCand);
//
//SetMultimap<Quad, Quad> summaryToQuadsQuery = quadJoinSummary(new ArrayList<Quad>(queryQuads));
//System.out.println("JoinSummaryQuery: " + summaryToQuadsQuery);
//
//for(Entry<Quad, Collection<Quad>> candEntry : summaryToQuadsCand.asMap().entrySet()) {
//  queryQuads = summaryToQuadsQuery.get(candEntry.getKey());
//
//  // TODO What if the mapping is empty?
//  QuadGroup group = new QuadGroup(candEntry.getValue(), queryQuads);
//
//  cnfToQuadGroup.put(cnf, group);
//}
