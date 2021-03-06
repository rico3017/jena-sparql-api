package org.aksw.jena_sparql_api.core.utils;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;

import org.aksw.jena_sparql_api.concepts.Concept;
import org.aksw.jena_sparql_api.concepts.ConceptUtils;
import org.aksw.jena_sparql_api.utils.IteratorResultSetBinding;
import org.aksw.jena_sparql_api.utils.VarUtils;
import org.apache.jena.ext.com.google.common.base.Objects;
import org.apache.jena.ext.com.google.common.collect.Maps;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.algebra.table.TableData;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingHashMap;

import com.google.common.collect.Range;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.Single;
import io.reactivex.functions.Predicate;
import io.reactivex.processors.PublishProcessor;

public class ReactiveSparqlUtils {
	
	public static <K, X> Flowable<Entry<K, List<X>>> groupByOrdered(
			Flowable<X> in, Function<X, K> getGroupKey) {

	      Object[] current = {null};
	      Object[] prior = {null};
	      PublishProcessor<K> boundaryIndicator = PublishProcessor.create();

	      return in
	    	 .doOnComplete(boundaryIndicator::onComplete)
	    	 .doOnNext(item -> {
		    		K groupKey = getGroupKey.apply(item);
		    		boolean isEqual = Objects.equal(current, groupKey);

		    		prior[0] = current[0];
		    		if(prior[0] == null) {
		    			prior[0] = groupKey;
		    		}

		    		current[0] = groupKey;
		
		    		
		    		if(!isEqual) {
		    			boundaryIndicator.onNext(groupKey);
		    		}
		      })
		      .buffer(boundaryIndicator)
		      .map(buffer -> {
		    	  K tmp = (K)prior[0];
		    	  K groupKey = tmp;
		    	  
		    	  return Maps.immutableEntry(groupKey, buffer);
		      });
	      
	}
	
	/**
	 * 
	 * 
	 * @param vars
	 * @return
	 */
	public static Function<List<Binding>, Table> createTableBuffer(List<Var> vars) {
		Function<List<Binding>, Table> result = rows -> new TableData(vars, rows);

		return result;
	}

	public static <T> void processExecSelect(FlowableEmitter<T> emitter, QueryExecution qe, Function<? super ResultSet, ? extends T> next) {
		try {
			emitter.setCancellable(qe::abort);
			ResultSet rs = qe.execSelect();
			while(!emitter.isCancelled() && rs.hasNext()) {
				T binding = next.apply(rs);
				emitter.onNext(binding);
			}
			emitter.onComplete();
		} catch (Exception e) {
			emitter.onError(e);
		}
	}

	
	public static void processExecConstructTriples(FlowableEmitter<Triple> emitter, QueryExecution qe) {
		try {
			emitter.setCancellable(qe::abort);
			Iterator<Triple> it = qe.execConstructTriples();
			while(!emitter.isCancelled() && it.hasNext()) {
				Triple item = it.next();
				emitter.onNext(item);
			}
			emitter.onComplete();
		} catch (Exception e) {
			emitter.onError(e);
		}
	}

//
//	public static void processExecConstructTriples(SingleEmitter<ResultSet> emitter, QueryExecution qe) {
//		try {
//			emitter.setCancellable(qe::abort);
//			Iterator<Triple> = qe.execConstructTriples();
//			emitter.onSuccess(rs);
//			//emitter.onComplete();
//		} catch (Exception e) {
//			emitter.onError(e);
//		}
//	}

	public static <T> Flowable<T> execSelect(Supplier<QueryExecution> qes, Function<? super ResultSet, ? extends T> next) {
		Flowable<T> result = Flowable.create(emitter -> {
			QueryExecution qe = qes.get();
			processExecSelect(emitter, qe, next);
			//new Thread(() -> process(emitter, qe)).start();
		}, BackpressureStrategy.BUFFER);

		return result;
	}

	public static Flowable<Binding> execSelect(Supplier<QueryExecution> qes) {
		return execSelect(qes, ResultSet::nextBinding);
	}

	public static Flowable<QuerySolution> execSelectQs(Supplier<QueryExecution> qes) {
		return execSelect(qes, ResultSet::next);
	}

	public static Flowable<Triple> execConstructTriples(Supplier<QueryExecution> qes) {
		Flowable<Triple> result = Flowable.create(emitter -> {
			QueryExecution qe = qes.get();
			processExecConstructTriples(emitter, qe);
			//new Thread(() -> process(emitter, qe)).start();
		}, BackpressureStrategy.BUFFER);

		return result;
	}
	
	public static Entry<List<Var>, Flowable<Binding>> mapToFlowable(ResultSet rs) {
		Iterator<Binding> it = new IteratorResultSetBinding(rs);
		Iterable<Binding> i = () -> it;
		
		List<Var> vars = VarUtils.toList(rs.getResultVars());

		Flowable<Binding> flowable = Flowable.fromIterable(i);
		Entry<List<Var>, Flowable<Binding>> result = new SimpleEntry<>(vars, flowable);
		return result;		
	}

	public static Flowable<Binding> mapToBinding(ResultSet rs) {
		Entry<List<Var>, Flowable<Binding>> e = mapToFlowable(rs);
		Flowable<Binding> result = e.getValue();
		return result;
	}

//	public static Flowable<Binding> mapToBinding(ResultSet rs) {
//		Iterator<Binding> it = new IteratorResultSetBinding(rs);
//		Iterable<Binding> i = () -> it;
//		return Flowable.fromIterable(i);
//	}

	/**
	 * Create a grouping function
	 *
	 * Usage:
	 * flowable
	 * 	.groupBy(createGrouper(Arrays.asList(... yourVars ...)))
	 * 
	 * @param vars
	 * @param retainNulls
	 * @return
	 */
	public static Function<Binding, Binding> createGrouper(Collection<Var> vars, boolean retainNulls) {
		return b -> {
			BindingHashMap groupKey = new BindingHashMap();
			for(Var k : vars) {
				Node v = b.get(k);
				if(v != null || retainNulls) {
					groupKey.add(k, v);
				}
			}
			return groupKey;
		};
	}
	
	public static Function<Binding, Node> createGrouper(Var var) {
		return b -> {
			Node groupKey = b.get(var);
			return groupKey;
		};
	}

//	public static Flowable<Table> groupBy(Flowable<Binding> )
	
	// /**
	// * Mapping that includes
	// *
	// */
	// public static Flowable<Entry<List<Var>, Binding>>(List<Var> vars, ) {
	//
	// }

	public static void main(String[] args) {
//		List<Entry<Integer, List<Entry<Integer, Integer>>>> list = groupByOrdered(Flowable.range(0, 10).map(i -> Maps.immutableEntry((int)(i / 3), i)),
//		e -> e.getKey())
//		.toList().blockingGet();


		Integer currentValue[] = {null};
		boolean isCancelled[] = {false};
		
		Flowable<Entry<Integer, List<Integer>>> list = Flowable
				.range(0, 10)
				.doOnNext(i -> currentValue[0] = i)
				.doOnCancel(() -> isCancelled[0] = true)
				.map(i -> Maps.immutableEntry((int)(i / 3), i))
				.lift(new OperatorOrderedGroupBy<Entry<Integer, Integer>, Integer, List<Integer>>(Entry::getKey, ArrayList::new, (acc, e) -> acc.add(e.getValue())));

		Predicate<Entry<Integer, List<Integer>>> p = e -> e.getKey().equals(1); 
		list.takeUntil(p).subscribe(x -> System.out.println("Item: " + x));
		
		System.out.println("Value = " + currentValue[0] + ", isCancelled = " + isCancelled[0]);
		
//		Disposable d = list.defe
//		
//		Iterator<Entry<Integer, List<Integer>>> it = list.iterator();
//		for(int i = 0; i < 2 && it.hasNext(); ++i) {
//			Entry<Integer, List<Integer>> item = it.next();
//			System.out.println("Item: " + item);
//		}
//		
//		
//		System.out.println("List: " + list);
		
		
		PublishProcessor<String> queue = PublishProcessor.create();
		queue.buffer(3).subscribe(x -> System.out.println("Buffer: " + x));

		for(int i = 0; i < 10; ++i) {
			String item = "item" + i;
			System.out.println("Adding " + item);
			queue.onNext(item);
		}
		queue.onComplete();
		

		if(true) {
			return;
		}
		
		for(int j = 0; j < 10; ++j) {
			int i[] = { 0 };
			System.out.println("HERE");
			execSelect(() -> org.apache.jena.query.QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql",
					"SELECT * { ?s a <http://dbpedia.org/ontology/Person> }"))
							.takeUntil(b -> i[0] == 10).subscribe(x -> {
								i[0]++;
								System.out.println("x: " + x);
	
							});
		}
		// NOTE This way, the main thread will terminate before the queries are processed
	}
	
	public static Single<Number> fetchNumber(org.aksw.jena_sparql_api.core.QueryExecutionFactory qef, Query query, Var var) {
    	return ReactiveSparqlUtils.execSelect(() -> qef.createQueryExecution(query))
            	.map(b -> b.get(var))
            	.map(countNode -> ((Number)countNode.getLiteralValue()))
            	.single(null);
	}
	
	
    public static Single<Range<Long>> fetchCountConcept(org.aksw.jena_sparql_api.core.QueryExecutionFactory qef, Concept concept, Long itemLimit, Long rowLimit) {

        Var outputVar = ConceptUtils.freshVar(concept);

        Long xitemLimit = itemLimit == null ? null : itemLimit + 1;
        Long xrowLimit = rowLimit == null ? null : rowLimit + 1;

        Query countQuery = ConceptUtils.createQueryCount(concept, outputVar, xitemLimit, xrowLimit);

        return ReactiveSparqlUtils.fetchNumber(qef, countQuery, outputVar)
        		.map(count -> ReactiveSparqlUtils.toRange(count.longValue(), xitemLimit, xrowLimit));
    }
//	return ReactiveSparqlUtils.execSelect(() -> qef.createQueryExecution(countQuery))
//        	.map(b -> b.get(outputVar))
//        	.map(countNode -> ((Number)countNode.getLiteralValue()).longValue())
//        	.map(count -> {
//        		boolean mayHaveMoreItems = rowLimit != null
//        				? true
//        				: itemLimit != null && count > itemLimit;
//
//                Range<Long> r = mayHaveMoreItems ? Range.atLeast(itemLimit) : Range.singleton(count);        		
//                return r;
//        	})
//        	.single(null);

    public static Single<Range<Long>> fetchCountQuery(org.aksw.jena_sparql_api.core.QueryExecutionFactory qef, Query query, Long itemLimit, Long rowLimit) {

        Var outputVar = Var.alloc("_count_"); //ConceptUtils.freshVar(concept);

        Long xitemLimit = itemLimit == null ? null : itemLimit + 1;
        Long xrowLimit = rowLimit == null ? null : rowLimit + 1;

        Query countQuery = QueryGenerationUtils.createQueryCount(query, outputVar, xitemLimit, xrowLimit);

        return ReactiveSparqlUtils.fetchNumber(qef, countQuery, outputVar)
        		.map(count -> ReactiveSparqlUtils.toRange(count.longValue(), xitemLimit, xrowLimit));
    }

    
    public static Range<Long> toRange(Long count, Long itemLimit, Long rowLimit) {
		boolean mayHaveMoreItems = rowLimit != null
				? true
				: itemLimit != null && count > itemLimit;
	
	    Range<Long> r = mayHaveMoreItems ? Range.atLeast(itemLimit) : Range.singleton(count);        		
	    return r;
    }

}
