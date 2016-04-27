package org.aksw.jena_sparql_api.concept_cache.main;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.aksw.jena_sparql_api.concept_cache.core.CacheResult;
import org.aksw.jena_sparql_api.concept_cache.core.SparqlCacheUtils;
import org.aksw.jena_sparql_api.concept_cache.dirty.SparqlViewCache;
import org.aksw.jena_sparql_api.concept_cache.dirty.SparqlViewCacheImpl;
import org.aksw.jena_sparql_api.concept_cache.domain.QuadFilterPatternCanonical;
import org.aksw.jena_sparql_api.resources.sparqlqc.SparqlQcReader;
import org.aksw.jena_sparql_api.stmt.SparqlQueryParser;
import org.aksw.jena_sparql_api.stmt.SparqlQueryParserImpl;
import org.aksw.jena_sparql_api.utils.Generator;
import org.aksw.jena_sparql_api.utils.VarGeneratorBlacklist;
import org.aksw.jena_sparql_api.utils.VarGeneratorImpl2;
import org.apache.jena.graph.Node;
import org.apache.jena.query.Query;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.algebra.table.TableData;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.syntax.PatternVars;
import org.apache.jena.sparql.syntax.syntaxtransform.QueryTransformOps;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

public class TestSparqlViewCacheVariableRenaming {


    /**
     * Take a set of queries comprised of quads and filters,
     *
     * randomly shuffle their variables
     * @throws IOException
     *
     */
    @Test
    public void testSparqlViewCacheRenaming() throws IOException {
        //Reader reader = new InputStreamRnew FileInputStream(file) //new InputStreamReader(in);
        //List<String> lines = Files.readAllLines(Paths.get(""), encoding);
        Resource r = new ClassPathResource("bgp-queries.json");

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonReader jsonReader = new JsonReader(new InputStreamReader(r.getInputStream(), StandardCharsets.UTF_8));
        jsonReader.setLenient(true);

        Type listType = new TypeToken<ArrayList<String>>() {}.getType();
        List<String> queryStrs = gson.fromJson(jsonReader, listType);

        SparqlQueryParser sparqlParser = SparqlQueryParserImpl.create(Syntax.syntaxARQ);
        List<Query> query = queryStrs.stream().map(sparqlParser).collect(Collectors.toList());


        for(String queryStr : queryStrs) {
            System.out.println(queryStr);
            Query testQuery = sparqlParser.apply(queryStr);
            testVariableRenaming(testQuery);
        }

        Model model = SparqlQcReader.readResources("sparqlqc/1.4/benchmark/noprojection/*");

        model.write(System.out);

    }

    public static void testVariableRenaming(Query baseQuery) throws IOException {

        Collection<Var> vars = PatternVars.vars(baseQuery.getQueryPattern());

        QuadFilterPatternCanonical qfpc = SparqlCacheUtils.transform2(baseQuery);
        List<Var> baseVars = baseQuery.getProjectVars();

        Generator<Var> gen = new VarGeneratorBlacklist(VarGeneratorImpl2.create("v"), vars);

        Map<Var, Node> varMap = vars.stream()
                .collect(Collectors.toMap(
                        v -> v,
                        v -> (Node)gen.next()));

        Query renamedQuery = QueryTransformOps.transform(baseQuery, varMap);
        //List<Var> renamedVars = renamedQuery.getProjectVars();
        QuadFilterPatternCanonical renamedQfpc = SparqlCacheUtils.transform2(renamedQuery);


        SparqlViewCache sparqlViewCache = new SparqlViewCacheImpl();



        sparqlViewCache.index(qfpc, new TableData(baseVars, Collections.emptyList()));
        CacheResult cr = sparqlViewCache.lookup(renamedQfpc);

        System.out.println("Cache lookup - got " + cr.getTables().size() + " candidate tables");


        //SparqlViewCache.

//        SparqlV
//        public CacheResult lookup(QuadFilterPatternCanonical queryQfpc) { //PatternSummary queryPs) {

        //SparqlViewCache x;
        //x.lookup(queryQfpc);
        //x.index(qfp, rsp);

        //query.
    }

}