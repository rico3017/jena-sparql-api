package org.aksw.jena_sparql_api.utils;

import java.io.PrintStream;
import java.util.Iterator;

import org.aksw.commons.collections.diff.Diff;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.DatasetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.core.DatasetGraphFactory;
import com.hp.hpl.jena.sparql.core.Quad;

public class DatasetGraphUtils {
	public static void addAll(DatasetGraph target, DatasetGraph source) {
			Iterator<Quad> it = source.find();
			addAll(target, it);
	}

    public static void addAll(DatasetGraph datasetGraph, Iterable<? extends Quad> items) {
    	addAll(datasetGraph, items.iterator());
    }

    public static void addAll(DatasetGraph datasetGraph, Iterator<? extends Quad> it) {
		while(it.hasNext()) {
			Quad q = it.next();
			datasetGraph.add(q);
		}
    }

    public static DatasetGraph clone(DatasetGraph datasetGraph) {
		Iterator<Quad> it = datasetGraph.find();
		DatasetGraph clone = DatasetGraphFactory.createMem();
		addAll(clone, it);

		return clone;
    }

    public static Diff<DatasetGraph> wrapDiffDatasetGraph(Diff<? extends Iterable<? extends Quad>> diff) {
    	DatasetGraph added = DatasetGraphFactory.createMem();
    	DatasetGraph removed = DatasetGraphFactory.createMem();

    	DatasetGraphUtils.addAll(added, diff.getAdded());
    	DatasetGraphUtils.addAll(removed, diff.getRemoved());


    	Diff<DatasetGraph> result = new Diff<DatasetGraph>(added, removed, null);
    	return result;
    }

	public static void write(PrintStream out, DatasetGraph dg) {
	    Dataset ds = DatasetFactory.create(dg);


	    Model dm = ds.getDefaultModel();
	    if(!dm.isEmpty()) {
	    	out.println("Begin of Default model -----------------------");
	    	dm.write(out, "TURTLE");
	    	out.println("End of Default model -----------------------");
	    }
	    Iterator<String> it = ds.listNames();
	    while(it.hasNext()) {
	    	String name = it.next();
	    	Model model = ds.getNamedModel(name);
	    	System.out.println("Begin of " + name + " -----------------------");
	        model.write(out, "TURTLE");
	        System.out.println("End of " + name + " -----------------------");
	    }

	}

}