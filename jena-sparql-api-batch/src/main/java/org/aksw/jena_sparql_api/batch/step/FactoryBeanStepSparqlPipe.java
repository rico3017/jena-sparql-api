package org.aksw.jena_sparql_api.batch.step;

import java.util.List;
import java.util.Map.Entry;

import org.aksw.commons.collections.diff.Diff;
import org.aksw.jena_sparql_api.batch.ListServiceResourceShape;
import org.aksw.jena_sparql_api.batch.processor.ItemProcessorModifierDatasetGraphDiff;
import org.aksw.jena_sparql_api.batch.reader.ItemReaderDatasetGraph;
import org.aksw.jena_sparql_api.batch.writer.ItemWriterSparqlDiff;
import org.aksw.jena_sparql_api.concepts.Concept;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.core.UpdateExecutionFactory;
import org.aksw.jena_sparql_api.lookup.ListService;
import org.aksw.jena_sparql_api.modifier.Modifier;
import org.aksw.jena_sparql_api.modifier.ModifierList;
import org.aksw.jena_sparql_api.shape.ResourceShape;
import org.aksw.jena_sparql_api.stmt.SparqlUpdateParser;
import org.apache.jena.atlas.lib.Sink;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AbstractFactoryBean;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.sparql.core.DatasetGraph;


//public class FactoryBeanStepSparqlPipe
//    extends AbstractFactoryBean<Step>
//{
//    protected StepBuilderFactory stepBuilders;
//
//
//    protected String name;
//    protected int chunkSize;
//    protected Concept concept;
//    protected Query query;
//
//    protected QueryExecutionFactory sourceQef;
//    protected SparqlUpdateParser updateParser;
//    protected List<Modifier<? super DatasetGraph>> modifiers;
//    protected UpdateExecutionFactory targetUef;
//
//    //protected List<?> modifiers;
//
//
//
//    public FactoryBeanStepSparqlPipe() {
//        setSingleton(false);
//    }
//
////	@Autowired
////	public FactoryBeanStepSparqlDiff(StepBuilderFactory stepBuilders) {
////		setSingleton(false);
////		this.stepBuilders = stepBuilders;
////	}
//
//
//    @Autowired
//    public void setStepBuilders(StepBuilderFactory stepBuilders) {
//        this.stepBuilders = stepBuilders;
//    }
//
//
//    @Autowired
//    public void setSparqlUpdateParser(SparqlUpdateParser updateParser) {
//        this.updateParser = updateParser;
//    }
//
////	@Autowired
////	public void setBatchConfig(AbstractBatchConfiguration batchConfig) {
////		try {
////			this.stepBuilders = batchConfig.stepBuilders();
////		} catch (Exception e) {
////			throw new RuntimeException(e);
////		}
////	}
//
//
//    public FactoryBeanStepSparqlPipe setChunk(int chunkSize) {
//        this.chunkSize = chunkSize;
//        return this;
//    }
//
//    public FactoryBeanStepSparqlPipe setShape(ResourceShape shape) {
//        this.shape = shape;
//        return this;
//    }
//
//
////    public FactoryBeanStepSparqlDiff setModifier(Modifier<? super DatasetGraph> modifier) {
////        this.modifier = modifier;
////        return this;
////    }
//
//    public FactoryBeanStepSparqlPipe setModifiers(List<Modifier<? super DatasetGraph>> modifiers) {
//        this.modifiers = modifiers;
//        return this;
//    }
//
////    public FactoryBeanStepSparqlDiff setModifiers(List<?> modifiers) {
////        this.modifiers = modifiers;
////        return this;
////    }
//
////	public FactoryBeanStepSparqlDiff setModifier(Concept concept) {
////		this.concept = concept;
////		return this;
////	}
//
//    public FactoryBeanStepSparqlPipe setName(String name) {
//        this.name = name;
//        return this;
//    }
//
//    public FactoryBeanStepSparqlPipe setSource(QueryExecutionFactory sourceQef) {
//        this.sourceQef = sourceQef;
//        return this;
//    }
//
//    public FactoryBeanStepSparqlPipe setConcept(Concept concept) {
//        this.concept = concept;
//        return this;
//    }
//
//
//    public FactoryBeanStepSparqlPipe setTarget(UpdateExecutionFactory targetUef) {
//        this.targetUef = targetUef;
//        return this;
//    }
//
//
//
//
////	public StepBuilder getStepBuilder() {
////		return stepBuilder;
////	}
//
//    public ResourceShape getShape() {
//        return shape;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public int getChunkSize() {
//        return chunkSize;
//    }
//
//    public QueryExecutionFactory getSource() {
//        return sourceQef;
//    }
//
//    public Concept getConcept() {
//        return concept;
//    }
//
//    public List<Modifier<? super DatasetGraph>> getModifiers() {
//        return modifiers;
//    }
//
//    public UpdateExecutionFactory getTarget() {
//        return targetUef;
//    }
//
//    @Override
//    public Step createInstance() throws Exception {
//        Modifier<DatasetGraph> modifier = ModifierList.<DatasetGraph>create(modifiers);
//
//        ListService<Concept, Node, DatasetGraph> listService = new ListServiceResourceShape(sourceQef, shape, true);
//        //ItemReader<Entry<Node, DatasetGraph>> itemReader = new ItemReaderDatasetGraph(listService, concept);
//        ItemReaderDatasetGraph itemReader = new ItemReaderDatasetGraph(listService, concept);
//        ItemProcessor<Entry<? extends Node, ? extends DatasetGraph>, Entry<Node, Diff<DatasetGraph>>> itemProcessor = new ItemProcessorModifierDatasetGraphDiff(modifier);
//        ItemWriter<Entry<? extends Node, ? extends Diff<? extends DatasetGraph>>> itemWriter = new ItemWriterSparqlDiff(targetUef);
//
//        itemReader.setPageSize(chunkSize);
//
//        //StepBuilderFactory stepBuilders = batchConfig.stepBuilders();
//        StepBuilder stepBuilder = stepBuilders.get(name);
//
//        Step result = stepBuilder
//                .<Entry<Node, DatasetGraph>, Entry<Node, Diff<DatasetGraph>>>chunk(chunkSize)
//                .reader(itemReader)
//                .processor(itemProcessor)
//                .writer(itemWriter)
//                .build();
//
//        return result;
//    }
//
//    @Override
//    public Class<?> getObjectType() {
//        return Step.class;
//    }
//}
