package org.aksw.jena_sparql_api.batch;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.aksw.commons.util.StreamUtils;
import org.aksw.jena_sparql_api.concepts.Concept;
import org.aksw.jena_sparql_api.core.FluentQueryExecutionFactory;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.jena_sparql_api.shape.ResourceShape;
import org.aksw.jena_sparql_api.shape.ResourceShapeParserJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;





interface Vobuild {
    void add(ResourceShape shape);
}

class VobuildBase
    implements Vobuild
{
    @Override
    public void add(ResourceShape shape) {
        // TODO Auto-generated method stub
        
    }
    
}

class VobuildGeoSparql {
    public void add(ResourceShape shape) {
        ResourceShapeBuilder b = new ResourceShapeBuilder(shape);
        b.outgoing("geom:geometry").outgoing("ogc:asWKT");
    }
}

class VobuildWgs84 {
    public void add(ResourceShape shape) {
        ResourceShapeBuilder b = new ResourceShapeBuilder(shape);
        b.outgoing("geo:lat");
        b.outgoing("geo:long");
    }
}


public class MainBatchWorkflow {

    public void foo() {
        Map<String, Vobuild> nameToVocab = new HashMap<String, Vobuild>();
        //nameToVocab.put("geo", new VobuildWgs84());
        
        
        
    }
    
    private static final Logger logger = LoggerFactory.getLogger(MainBatchWorkflow.class);

    
    public static void main(String[] args) throws Exception {
        
        PrefixMapping pm = new PrefixMappingImpl();
        pm.setNsPrefix("rdf", RDF.getURI());
        pm.setNsPrefix("rdfs", RDFS.getURI());
        pm.setNsPrefix("geo", "http://www.w3.org/2003/01/geo/wgs84_pos#");
        pm.setNsPrefix("geom", "http://geovocab.org/geometry#");
        pm.setNsPrefix("ogc", "http://www.opengis.net/ont/geosparql#");
        pm.setNsPrefix("fp7", "http://fp7-pp.publicdata.eu/ontology/");
        
        ResourceShapeBuilder b = new ResourceShapeBuilder(pm);
        //b.outgoing("rdfs:label");
        b.outgoing("geo:lat");
        b.outgoing("geo:long");
        b.outgoing("geo:geometry");
        b.outgoing("geom:geometry").outgoing("ogc:asWKT");
                
        
        ResourceShapeParserJson parser = new ResourceShapeParserJson(pm);
        Map<String, Object> json = readJsonResource("workflow.json");
        ResourceShape rs = parser.parse(json.get("shape"));
        System.out.println(rs);
        
        //b.outgoing("rdf:type").outgoing(NodeValue.TRUE).incoming(ExprUtils.parse("?p = rdfs:label && langMatches(lang(?o), 'en')", pm));

        //ElementTriplesBlock
        //com.hp.hpl.jena.sparql.syntax.

        //Concept concept = Concept.parse("?s | ?s a <http://linkedgeodata.org/ontology/Castle>");
        Concept concept = Concept.parse("?s | Filter(?s = <http://linkedgeodata.org/triplify/node289523439> || ?s = <http://linkedgeodata.org/triplify/node290076702>)");
        
        Query query = ResourceShape.createQuery(b.getResourceShape(), concept);
        System.out.println(query);
        
        QueryExecutionFactory qef = FluentQueryExecutionFactory.http("http://linkedgeodata.org/sparql", "http://linkedgeodata.org").create();
        QueryExecution qe = qef.createQueryExecution(query);
        Model model = qe.execConstruct();
        
        model.write(System.out, "TURTLE");
        
        
        
//        List<Concept> concepts = ResourceShape.collectConcepts(b.getResourceShape());
//        for(Concept concept : concepts) {
//            System.out.println(concept);
//        }
    }

    
    public static <T> T readJsonResource(String r) throws IOException {
        String str = readResource(r);
        T result = readJson(str);
        return result;
    }
    
    public static String readResource(String r) throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource resource = resolver.getResource(r);        
        InputStream in = resource.getInputStream();
        String result = StreamUtils.toString(in);
        return result;
    }
    
    public static <T> T readJson(String str) throws IOException {
        Gson gson = new Gson();

        //String str = readResource(r);
        Reader reader = new StringReader(str); //new InputStreamReader(in);
        
        JsonReader jsonReader = new JsonReader(reader);
        jsonReader.setLenient(true);
        Object tmp = gson.fromJson(jsonReader, Object.class);
        
        @SuppressWarnings("unchecked")
        T result = (T)tmp;
        return result;
    }
    
    /**
     * @param args
     * @throws JobParametersInvalidException
     * @throws JobInstanceAlreadyCompleteException
     * @throws JobRestartException
     * @throws JobExecutionAlreadyRunningException
     */
    public static void main2(String[] args) throws Exception
    {
        
                
        Map<String, String> classAliasMap = new HashMap<String, String>();
        classAliasMap.put("QueryExecutionFactoryHttp", QueryExecutionFactoryHttp.class.getCanonicalName());
                
        String str = readResource("workflow.json");
        Map<String, Object> data = readJson(str);
        System.out.println(data);
        
        
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        
        JsonContextProcessor.processContext(context, ((Map)data.get("job")).get("context"), classAliasMap);
        
        context.refresh();
        
        System.exit(0);
        
        
        //Gson gson = (new GsonBuilder()).
        
        //cleanUp();
        System.out.println("Test");
        
        BatchWorkflowManager workflowManager = BatchWorkflowManager.createTestInstance();

        
        JobExecution je = workflowManager.launchWorkflowJob(str);


        if(je.getStatus().equals(BatchStatus.COMPLETED)) {
//            ResultSet rs = ResultSetFactory.fromXML(new FileInputStream(fileName));
//            while(rs.hasNext()) {
//                System.out.println(rs.nextBinding());
//            }
        }

        //JobExecution je = launchSparqlExport("http://linkedgeodata.org/sparql", Arrays.asList("http://linkedgeodata.org"), "Select * { ?s a <http://linkedgeodata.org/ontology/Airport> }", "/tmp/lgd-airport-uris.txt");

        for(;;) {
            Collection<StepExecution> stepExecutions = je.getStepExecutions();

            for(StepExecution stepExecution : stepExecutions) {
                ExecutionContext sec = stepExecution.getExecutionContext();
                //long processedItemCount = sec.getLong("FlatFileItemWriter.current.count");
                System.out.println("CONTEXT");
                System.out.println(sec.entrySet());
                Thread.sleep(5000);
                //System.out.println(processedItemCount);
            }


            //Set<Entry<String, Object>> entrySet = je.getExecutionContext().entrySet();
            //ExecutionContext ec = je.getExecutionContext();
            //ec.
            //System.out.println(entrySet);
        }
        
        
        //ed.shutdown();
    }


}