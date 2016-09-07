package org.aksw.jena_sparql_api.mapper.context;

import org.aksw.jena_sparql_api.mapper.impl.engine.EntityGraphMap;
import org.aksw.jena_sparql_api.mapper.model.RdfType;
import org.aksw.jena_sparql_api.mapper.model.RdfTypeFactory;
import org.aksw.jena_sparql_api.util.frontier.Frontier;
import org.aksw.jena_sparql_api.util.frontier.FrontierStatus;
import org.apache.jena.graph.Node;

public class RdfPersistenceContextImpl
    implements RdfPersistenceContext
{
    protected RdfTypeFactory typeFactory;
    
    protected EntityContext<Object> entityContext = EntityContextImpl.createIdentityContext(Object.class);
    protected EntityContext<TypedNode> typedNodeContext = new EntityContextImpl<TypedNode>();

    protected EntityGraphMap entityGraphMap = new EntityGraphMap();

    protected Frontier<TypedNode> frontier;


    public RdfPersistenceContextImpl(Frontier<TypedNode> frontier, RdfTypeFactory typeFactory) {
        super();
        this.typeFactory = typeFactory;
        this.frontier = frontier;
    }

    public Frontier<TypedNode> getFrontier() {
        return frontier;
    }
    
    

//	public void setFrontier(Frontier<TypedNode> frontier) {
//		this.frontier = frontier;
//	}

    @Override
    public RdfTypeFactory getTypeFactory() {
        return typeFactory;
    }

    /**
     * TODO Exposing the entity graph map directly does not seem to be good encapsulation
     *
     * @return
     */
    @Override
    public EntityGraphMap getEntityGraphMap() {
        return entityGraphMap;
    }

    @Override
    public Object getEntity(TypedNode typedNode) {
        Object result = typedNodeContext.getAttribute(typedNode, "entity", null);
        return result;
    }

    @Override
    public Object entityFor(TypedNode typedNode) {
        RdfType rdfType = typedNode.getRdfType();
        Node node = typedNode.getNode();

        // TODO If we request an entity for a typed node for which an entity
        // already exists for a base class, what then?
        // i.e: given Person implements Thing, .entityFor(Thing, node1) followed by .entityFor(Person, node1)
        // I suppose on merging all entities have to be updated.
        // or entityFor needs to be replaced by entityPoolFor().
        // and all entities in a pool are proxied such that setting any property in the pool
        // updates all other entities in that pool - hm, but then entityFor would always have to return a proxy

        // Check if there is already a java object for the given class with the given id
        Object result = typedNodeContext.getAttribute(typedNode, "entity", null);
        if(result == null) {            
            //result = rdfType.createJavaObject(node);
            typedNodeContext.setAttribute(typedNode, "entity", result);
        }

        entityContext.setAttribute(result, "rootNode", node);

//        TypedNode pr = new TypedNode(rdfType, node);
        frontier.add(typedNode);

        return result;
    }
    

    /**
     * TODO It could happen that multiple typedNodes map to the same entity
     * Probably additional context would be needed to resolve the root node for
     * an entity
     */
    @Override
    public Node getRawRootNode(Object entity) {
        Node result = entityContext.getAttribute(entity, "rootNode", null);
        return result;
    }


    public void checkManaged(Object bean) {
        if(!isManaged(bean)) {
            throw new RuntimeException("Bean was expected to be managed: " + bean);
        }
    }

    public boolean isManaged(Object bean) {
        FrontierStatus status = frontier.getStatus(bean);
        boolean result = !FrontierStatus.UNKNOWN.equals(status);
        return result;
    }


    /**
     * Convenience accessors
     *
     * @param bean
     * @return
     */

    public boolean isPopulated(Object populationRequest) {
        boolean result = FrontierStatus.DONE.equals(frontier.getStatus(populationRequest));

        return result;
    }

    public static Node getOrCreateRootNode(RdfPersistenceContext persistenceContext, RdfTypeFactory typeFactory, Object entity) {
        if(true) {
            System.out.println("getOrCreateRootNodeX");
            //throw new RuntimeException("Do not use this method at least for now");
        }
        Node result = persistenceContext.getRawRootNode(entity);
        if(result == null) {
            
            Class<?> clazz = entity.getClass();
            RdfType rdfType = typeFactory.forJavaType(clazz);
            result = rdfType.getRootNode(entity);
            persistenceContext.getFrontier().add(new TypedNode(rdfType, result));
        }
        return result;
    }

    @Override
    public Node getRootNode(Object entity) {
        Node result = getRawRootNode(entity); //getOrCreateRootNode(this, typeFactory, entity);

        return result;
    }

    @Override
    public void put(Node node, Object entity) {
        RdfType rdfType = typeFactory.forJavaType(entity.getClass());
        TypedNode typedNode = new TypedNode(rdfType, node);        

        typedNodeContext.setAttribute(typedNode, "entity", entity);
        entityContext.setAttribute(entity, "rootNode", node);        
    }

    @Override
    public void requestResolution(Object entity, String propertyName,
            Node subject, RdfType rdfType) {
        // TODO Auto-generated method stub
        
    }

}
