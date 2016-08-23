package org.aksw.jena_sparql_api.beans.model;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public interface EntityOps {
    // Entity ops may but need to be derived from a java class
    Class<?> getAssociatedClass();
    
    <A> A findAnnotation(Class<A> annotationClass);
    
    Object newInstance();
    Collection<? extends PropertyOps> getProperties();
    
    PropertyOps getProperty(String name);
    
    default Set<String> getPropertyNames() {
        Set<String> result = getProperties().stream().map(p -> p.getName()).collect(Collectors.toSet());
        return result;
    }
    
    public static void copy(EntityOps sourceOps, EntityOps targetOps, Object fromEntity, Object toEntity) {
        for(PropertyOps toOps : targetOps.getProperties()) {
            String name = toOps.getName();
            PropertyOps fromOps = sourceOps.getProperty(name);
            
            Object value = fromOps.getValue(fromEntity);
            toOps.setValue(toEntity, value);
        }
    }
}
