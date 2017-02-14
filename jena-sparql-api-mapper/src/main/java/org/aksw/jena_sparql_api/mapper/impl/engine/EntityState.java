package org.aksw.jena_sparql_api.mapper.impl.engine;

import org.aksw.jena_sparql_api.mapper.impl.type.EntityFragment;
import org.aksw.jena_sparql_api.mapper.impl.type.ResourceFragment;
import org.apache.jena.rdf.model.RDFNode;

public class EntityState {
	protected Object entity;
	protected RDFNode shapeResource;
	protected ResourceFragment resourceFragment;
	protected EntityFragment entityFragment;
	protected RDFNode currentResource;
		
	public EntityState(Object entity, RDFNode shapeResource, ResourceFragment resourceFragment,
			EntityFragment entityFragment) {
		super();
		this.entity = entity;
		this.shapeResource = shapeResource;
		this.resourceFragment = resourceFragment;
		this.entityFragment = entityFragment;
		this.currentResource = null;
	}

	public Object getEntity() {
		return entity;
	}
	
	public RDFNode getShapeResource() {
		return shapeResource;
	}
	
	public ResourceFragment getResourceFragment() {
		return resourceFragment;
	}
	
	public EntityFragment getEntityFragment() {
		return entityFragment;
	}

	public RDFNode getCurrentResource() {
		return currentResource;
	}

	public void setCurrentResource(RDFNode currentResource) {
		this.currentResource = currentResource;
	}
	
	
	
}
