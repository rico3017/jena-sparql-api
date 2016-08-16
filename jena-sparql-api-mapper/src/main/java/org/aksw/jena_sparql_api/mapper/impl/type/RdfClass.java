package org.aksw.jena_sparql_api.mapper.impl.type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.aksw.jena_sparql_api.concepts.Concept;
import org.aksw.jena_sparql_api.mapper.context.RdfEmitterContext;
import org.aksw.jena_sparql_api.mapper.context.RdfPersistenceContext;
import org.aksw.jena_sparql_api.mapper.model.RdfPopulator;
import org.aksw.jena_sparql_api.mapper.model.RdfTypeFactory;
import org.aksw.jena_sparql_api.mapper.proxy.MethodInterceptorRdf;
import org.aksw.jena_sparql_api.shape.ResourceShapeBuilder;
import org.apache.jena.atlas.lib.Sink;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.DatasetGraph;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;

/**
 * An RdfClass is one type of implementation that can map Java objects to and from RDF graphs.
 *
 */
public class RdfClass
    extends RdfTypeComplexBase
{
    // TODO: Add type parameters

    /**
     * The affected class (maybe we should use the fully qualified class name instead?)
     */
    //protected Class<?> beanClass;
    EntityOps entityOps;
    

    /**
     * The concept that captures rdf terms that are instances of this class.
     * In the simplest case this is { ?s | ?s a &lt;http://example.org/ontology/SomeType&gt; }
     *
     */
    protected Concept concept;

//    /**
//     * Function that emits triples that should be emitted for a given object
//     *
//     * This should yield all triples needed to satisfy the concept.
//     */
//    protected Function<Object, Graph> enforcedTriples;


    /**
     * A function for obtaining a default IRI from an object
     */
    protected Function<Object, String> defaultIriFn;

    //protected Prologue prologue;

    //protected Multimap<String, RdfPopulator> propertyToMapping;
    // protected Map<String, RdfType> propertyToRdfType;


    /**
     * Populators are used to map between property values and RDF data
     * A single populator may thereby set multiple properties at once.
     * However, the value and the RdfType of the property determine whether
     * additional population is needed.
     *
     * A persistenceContext is used to keep track of beans that were not populated yet.
     *
     * Each property has a corresponding RdfType
     *
     */
    protected List<RdfPopulator> populators = new ArrayList<RdfPopulator>();

    /**
     * PropertyDescriptors map the Java property types to RdfType instances.
     * The population status of a property value depends on the
     * RdfType and value in regard to a persistenceContext.
     *
     */
    protected Map<String, RdfPropertyDescriptor> propertyDescriptors = new HashMap<String, RdfPropertyDescriptor>();



    protected boolean isPopulated;

//    public RdfClass(RdfTypeFactory typeFactory, Class<?> targetClass, Function<Object, String> defaultIriFn, Prologue prologue) {
//        this(typeFactory, targetClass, defaultIriFn, prologue, new LinkedHashMap<String, RdfProperty>());
//    }

    public Collection<RdfPopulator> getPopulators() {
        //Collection<RdfProperty> result = propertyToMapping.values();
        return this.populators;
    }

    public Collection<RdfPropertyDescriptor> getPropertyDescriptors() {
        Collection<RdfPropertyDescriptor> result = propertyDescriptors.values();
        return result;
    }

    public RdfPropertyDescriptor getPropertyDescriptors(String propertyName) {
        RdfPropertyDescriptor result = propertyDescriptors.get(propertyName);
        return result;
    }

//    public List<String> getPropertyNames() {
//    	BeanWrapper beanWrapper = new BeanWrapperImpl(beanClass);
//    	beanWrapper.getPropertyDescriptors()
//    }

    public Concept getConcept() {
        return concept;
    }

    public void setPopulated(boolean isPopulated) {
        this.isPopulated = isPopulated;
    }

    public void checkPopulated() {
        if(isPopulated) {
            throw new IllegalStateException("Class has already been populated");
        }
    }

    public void addPropertyDescriptor(RdfPropertyDescriptor propertyDescriptor) {
        checkPopulated();
        String name = propertyDescriptor.getName();

        propertyDescriptors.put(name, propertyDescriptor);
    }

    public void addPopulator(RdfPopulator populator) {
        checkPopulated();

        populators.add(populator);
//    	Set<String> propertyNames = populator.getPropertyNames();
//    	for(String propertyName : propertyNames) {
//    		propertyToMapping.put(propertyName, populator);
//    	}
    }

    /**
     * Whether this RdfClass instance is fully initialized.
     *
     * @return
     */
    public boolean isPopulated() {
        return isPopulated;
    }

    // Map<String, RdfProperty> propertyToMapping
    // Prologue prologue
    public RdfClass(RdfTypeFactory typeFactory, EntityOps targetClass, Function<Object, String> defaultIriFn) {
        super(typeFactory);
        this.entityOps = targetClass;
        this.defaultIriFn = defaultIriFn;
        //this.prologue = prologue;
        //this.propertyToMapping = propertyToMapping;
    }

    @Override
    public Class<?> getEntityClass() {
        return entityOps.getAssociatedClass();
    }

    @Override
    public void exposeShape(ResourceShapeBuilder builder) {
        for(RdfPopulator populator : populators) {
            populator.exposeShape(builder);
        }
    }

//    public MappedConcept<DatasetGraph> getMappedQuery() {
//        ResourceShapeBuilder builder = new ResourceShapeBuilder(prologue);
//
//
//        for(RdfPopulator populator : populators) {
//        	populator.exposeShape(builder);
//        }
//
//        ResourceShape shape = builder.getResourceShape();
//        MappedConcept<DatasetGraph> result = ResourceShape.createMappedConcept2(shape, null);
//        return result;
//    }

    public static MethodInterceptorRdf getMethodInterceptor(Object o) {
        MethodInterceptorRdf result = null;

        if(o != null && Enhancer.isEnhanced(o.getClass())) {
            Factory factory = (Factory)o;
            Callback callback = factory.getCallback(0);
            result = callback != null && callback instanceof MethodInterceptorRdf
                    ? (MethodInterceptorRdf)callback : null;
        }

        return result;
    }

    /**
     * Returns the subject of a given object or null if not present.
     * First the object is checked for whether it is a proxy referring to a prior subject, which is returned if present.
     * Otherwise, a default iri will be generated.
     *
     * @param o
     * @return
     */
    @Override
    public Node getRootNode(Object o) {
        MethodInterceptorRdf m = getMethodInterceptor(o);
        Node result = m != null ? m.getPresetSubject() : null;

        if(result == null) {
            String str = defaultIriFn != null ? defaultIriFn.apply(o) : null;
            //str = prologue.getPrefixMapping().expandPrefix(str);

            result = str != null ? NodeFactory.createURI(str) : null;
        }

        return result;
    }


    public Object toJava(Node node) {
        Object result;
        if(node == null) {
            result = null;
        } else if(node.isURI()) {
            result = node.getURI();
        } else if(node.isLiteral()) {
            result = node.getLiteralValue();
        } else { //if(node.isBlank()) {
            throw new RuntimeException("not supported (yet)");
        }

        return result;
    }


    /**
     * Set property values of the given target object based a DatasetGraph.
     *
     * @param bean
     * @param datasetGraph
     */
    @Override
    public void populateEntity(RdfPersistenceContext persistenceContext, Object bean, Graph inGraph, Sink<Triple> outSink) {
        //DatasetGraph result = DatasetGraphFactory.createMem();

        //Graph graph = result.getDefaultGraph();
        Node s = persistenceContext.getRootNode(bean);

        /*
         *  Run all of this class' populators
         */
        for(RdfPopulator pd : populators) {
            pd.populateEntity(persistenceContext, bean, inGraph, s, outSink);
        }
    }

    //writeGraph(Object obj, Node g, )

    /**
     * Extract triples for a given object in the specified target graph.
     *
     * @param entity
     * @param g
     * @return
     */
    @Override
    public void emitTriples(RdfPersistenceContext persistenceContext, RdfEmitterContext emitterContext, Graph out, Object entity) {
        //Node s = getRootNode(obj);
        Node s = persistenceContext.getRootNode(entity);

        /*
         * Run the emitters of all of this class' populators
         */
        for(RdfPopulator populator : populators) {
            populator.emitTriples(persistenceContext, emitterContext, out, entity, s);
        }

        /*
         * Based on the property descriptors, the RdfClass
         * notifies the emitter context which property values need additional
         * emitting
         */
//        BeanWrapper beanWrapper = new BeanWrapperImpl(entity);
//        for(RdfPropertyDescriptor pd : propertyDescriptors.values()) {
//            String propertyName = pd.getName();
//
//            Object propertyValue = beanWrapper.getPropertyValue(propertyName);
//            emitterContext.add(propertyValue, entity, propertyName);
//        }
    }



    /**
     * Create a proxied instance of the class based on the given graph
     *
     * @deprecated Use createJavaObject first, and initialize values from a datasetGraph using a separate call to one of the methods of this class
     * TODO which method?
     *
     * @param datasetGraph
     * @return
     */
    @Deprecated
    public Object createProxy(DatasetGraph datasetGraph, Node subject) {

        Object o;
        try {
            o = entityOps.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        MethodInterceptorRdf interceptor = new MethodInterceptorRdf(o, this, subject, datasetGraph);
        //new Class<?>[] { ProxiedRdf.class }
//        Object result = Enhancer.create(targetClass, null, interceptor);
        Class<?> beanClass = entityOps.getAssociatedClass();
        Object result = Enhancer.create(beanClass, null, interceptor);

        return result;
    }

    @Override
    public Object createJavaObject(Node subject) {
        Object result;
        try {
            result = entityOps.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

// TODO The proxy mechanism is not controlled at type level, but at engine level
//        MethodInterceptorRdf interceptor = new MethodInterceptorRdf(o, this, subject);
//        Object result = Enhancer.create(beanClass, null, interceptor);

        return result;
    }
}
