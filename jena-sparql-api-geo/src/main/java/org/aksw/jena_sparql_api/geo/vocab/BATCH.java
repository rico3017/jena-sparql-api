package org.aksw.jena_sparql_api.geo.vocab;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class BATCH {
    public static final String ns = "http://aksw.org/batch/";

    public static String getURI() {
        return ns;
    }

    public static final Property locationString = ResourceFactory.createProperty(ns + "locationString");
}