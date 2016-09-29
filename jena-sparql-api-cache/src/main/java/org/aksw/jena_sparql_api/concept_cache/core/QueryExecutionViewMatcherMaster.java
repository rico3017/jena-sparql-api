package org.aksw.jena_sparql_api.concept_cache.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.aksw.jena_sparql_api.core.QueryExecutionBaseSelect;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.core.ResultSetCloseable;
import org.aksw.jena_sparql_api.util.collection.RangedSupplier;
import org.aksw.jena_sparql_api.util.collection.RangedSupplierLazyLoadingListCache;
import org.aksw.jena_sparql_api.util.collection.RangedSupplierSubRange;
import org.aksw.jena_sparql_api.utils.BindingUtils;
import org.aksw.jena_sparql_api.utils.QueryUtils;
import org.aksw.jena_sparql_api.utils.ResultSetUtils;
import org.aksw.jena_sparql_api.utils.VarUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.ARQ;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpVars;
import org.apache.jena.sparql.algebra.op.OpNull;
import org.apache.jena.sparql.algebra.op.OpService;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.Plan;
import org.apache.jena.sparql.engine.QueryEngineFactory;
import org.apache.jena.sparql.engine.QueryEngineRegistry;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingRoot;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.util.iterator.ClosableIterator;

import com.google.common.collect.Range;

public class QueryExecutionViewMatcherMaster
	extends QueryExecutionBaseSelect
{
	//protected ExecutorService executorService;
	//protected OpViewMatcher viewMatcher;
	protected OpRewriteViewMatcherStateful opRewriter;
	//protected Map<Node, RangedSupplier<Long, Binding>> opToRangedSupplier;
	protected Map<Node, StorageEntry> storageMap;



    protected long indexResultSetSizeThreshold;

    //protected Map<Node, ? super ViewCacheIndexer> serviceMap;

    public QueryExecutionViewMatcherMaster(
    		Query query,
    		//Function<Query, RangedSupplier<Long, Binding>> rangedSupplierFactory,
    		QueryExecutionFactory subFactory,
    		OpRewriteViewMatcherStateful opRewriter
    		//Map<Node, StorageEntry> storageMap
    		//OpViewMatcher viewMatcher,
    		//Rewrite opRewriter
    		//ExecutorService executorService
    		//Map<Node, RangedSupplier<Long, Binding>> opToRangedSupplier
    		//long indexResultSetSizeThreshold,
    		//Map<Node, ? super ViewCacheIndexer> serviceMap
    ) {
    	super(query, subFactory);

    	this.opRewriter = opRewriter;
    	this.storageMap = OpExecutorFactoryViewMatcher.get().getStorageMap();
    	//this.storageMap = storageMap;
    	//this.executorService = executorService;
    	//this.opToRangedSupplier = opToRangedSupplier;
    	//this.serviceMap = serviceMap;
    }


    public ResultSetCloseable createResultSet(RangedSupplier<Long, Binding> rangedSupplier, Range<Long> range, Map<Var, Var> varMap) {
    	ClosableIterator<Binding> it = rangedSupplier.apply(range);
    	Iterable<Binding> tmp = () -> it;
    	Stream<Binding> stream = StreamSupport.stream(tmp.spliterator(), false);
    	if(varMap != null) {
    		stream = stream.map(b -> BindingUtils.rename(b, varMap));
    	}
    	stream = stream.onClose(() -> it.close());

    	List<String> varNames = query.getResultVars();
    	//ResultSetCloseable result = ResultSetUtils.create(varNames, it);
    	ResultSet rs = ResultSetUtils.create(varNames, stream.iterator());
    	ResultSetCloseable result = new ResultSetCloseable(rs);

    	return result;
    }

    @Override
    protected ResultSetCloseable executeCoreSelect(Query rawQuery) {

    	List<Var> projectVars = rawQuery.getProjectVars();
    	List<String> projectVarNames = VarUtils.getVarNames(projectVars);

    	// Store a present slice, but remove it from the query
    	Range<Long> range = QueryUtils.toRange(query);
    	Query q = query.cloneQuery();
    	q.setLimit(Query.NOLIMIT);
    	q.setOffset(Query.NOLIMIT);

    	Op queryOp = Algebra.toQuadForm(Algebra.compile(q));

    	// The thing here is, that in general we need to
    	// - Initialize the execution context / jena-wise global data
    	// - Perform the rewrite (may affect execution context state)
    	// - Clean up the execution context / jena-wise global data
    	Op rewrittenOp = opRewriter.rewrite(queryOp);



    	Node serviceNode = NodeFactory.createURI("view://test.org");
    	rewrittenOp = new OpService(serviceNode, OpNull.create(), false);

    	ExecutorService executorService = Executors.newCachedThreadPool();
    	RangedSupplier<Long, Binding> backend = new RangedSupplierQuery(parentFactory, rawQuery);
    	//RangedSupplierLazyLoadingListCache<Binding>
    	RangedSupplier<Long, Binding> storage = new RangedSupplierLazyLoadingListCache<>(executorService, backend, Range.atMost(10000l), null);

    	storage = RangedSupplierSubRange.create(storage, range);


    	Set<Var> visibleVars = OpVars.visibleVars(rewrittenOp);
    	VarInfo varInfo = new VarInfo(visibleVars, Collections.emptySet());

    	StorageEntry se = new StorageEntry(storage, varInfo);
    	storageMap.put(serviceNode, se);



    	System.out.println("Rewritten op being passed to execution:\n" + rewrittenOp);
    	System.out.println("Rewritten op being passed to execution:\n" + rawQuery);

    	// Note: We use Jena to execute the op.
    	// The op itself may use SERVICE<> as the root node, which will cause jena to pass execution to the appropriate handler

    	// TODO Pass the op to an op executor
    	//QueryEngineMainQuad

    	DatasetGraph dg = DatasetGraphFactory.create();
    	Context context = ARQ.getContext();
    	QueryEngineFactory qef = QueryEngineRegistry.get().find(rewrittenOp, dg, context);
    	Plan plan = qef.create(rewrittenOp, dg, BindingRoot.create(), context);
    	QueryIterator queryIter = plan.iterator();

    	//QueryIterator queryIter = x.eval(rewrittenOp, dg, BindingRoot.create(), context);
    	ResultSet tmpRs = ResultSetFactory.create(queryIter, projectVarNames);

    	// TODO Not sure if we should really return a result set, or a QueryIter instead
    	ResultSetCloseable result = new ResultSetCloseable(tmpRs, () -> queryIter.close());

    	return result;

    	//ResultSetUtils.create(varNames, bindingIt)

    	//QueryEngineMain
    	//QC.execute(rewrittenOp, BindingRoot.create(), ARQ.getContext());

    	//org.apache.jena.query.QueryExecutionFactory.create(queryStr, syntax, model, initialBinding)
//
//
//
//    	LookupResult<Node> lr = viewMatcher.lookupSingle(opCache);
//        RangedSupplier<Long, Binding> rangedSupplier;
//        Map<Var, Var> varMap;
//        if(lr == null) {
//            Node id = viewMatcher.add(opCache);
//        	// Obtain the supplier from a factory (the factory may e.g. manage the sharing of a thread pool)
//
//            rangedSupplier = new RangedSupplierQuery(parentFactory, query);
//        	rangedSupplier = new RangedSupplierLazyLoadingListCache<>(executorService, rangedSupplier, Range.atMost(10000l), null);
//
//        	//rangedSupplier = new RangedSupplierQuery(parentFactory, q);
//        	opToRangedSupplier.put(id, rangedSupplier);
//        	varMap = null;
//        }
//        else {
//
//            varMap = Iterables.getFirst(lr.getOpVarMap().getVarMaps(), null);
//
//            assert varMap != null : "VarMap was not expected to be null at this point";
//
//        	Node entryId = lr.getEntry().id;
//        	rangedSupplier = opToRangedSupplier.get(entryId);
//        }
//
//        ResultSetCloseable result = createResultSet(rangedSupplier, range, varMap);
//        return result;
    }


	@Override
	protected QueryExecution executeCoreSelectX(Query query) {
		// TODO Fix bad design - this method is not needed
		return null;
	}

}

