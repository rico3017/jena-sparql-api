package org.aksw.jena_sparql_api.stmt;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.ext.com.google.common.collect.Streams;
import org.apache.jena.ext.com.google.common.io.CharStreams;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.riot.out.SinkQuadOutput;
import org.apache.jena.riot.out.SinkTripleOutput;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.lang.arq.ParseException;
import org.apache.jena.update.UpdateRequest;

public class SparqlStmtUtils {

	public static Stream<SparqlStmt> parse(InputStream in, Function<String, SparqlStmt> parser)
			throws IOException, ParseException {
		// try(QueryExecution qe = qef.createQueryExecution(q)) {
		// Model result = qe.execConstruct();
		// RDFDataMgr.write(System.out, result, RDFFormat.TURTLE_PRETTY);
		// //ResultSet rs = qe.execSelect();
		// //System.out.println(ResultSetFormatter.asText(rs));
		// }
		// File file = new
		// File("/home/raven/Projects/Eclipse/trento-bike-racks/datasets/test/test.sparql");
		// String str = Files.asCharSource(, StandardCharsets.UTF_8).read();

		String str = CharStreams.toString(new InputStreamReader(in, StandardCharsets.UTF_8));

		// ARQParser parser = new ARQParser(new FileInputStream(file));
		// parser.setQuery(new Query());
		// parser.

		// SparqlStmtParser parser = SparqlStmtParserImpl.create(Syntax.syntaxARQ,
		// PrefixMapping.Extended, true);

		Stream<SparqlStmt> result = Streams.stream(new SparqlStmtIterator(parser, str));
		return result;
	}

	public static SPARQLResultEx execAny(RDFConnection conn, SparqlStmt stmt) {
		SPARQLResultEx result = null;

		if (stmt.isQuery()) {
			SparqlStmtQuery qs = stmt.getAsQueryStmt();
			Query q = qs.getQuery();
			//conn.begin(ReadWrite.READ);
			// SELECT -> STDERR, CONSTRUCT -> STDOUT
			QueryExecution qe = conn.query(q);
	
			if (q.isConstructQuad()) {
				Iterator<Quad> it = qe.execConstructQuads();
				result = SPARQLResultEx.createQuads(it);
				
			} else if (q.isConstructType()) {
				// System.out.println(Algebra.compile(q));
	
				Iterator<Triple> it = qe.execConstructTriples();
				result = SPARQLResultEx.createTriples(it);
			} else if (q.isSelectType()) {
				ResultSet rs = qe.execSelect();
				result = new SPARQLResultEx(rs);
			} else if(q.isJsonType()) {
				Iterator<JsonObject> it = qe.execJsonItems();
				result = new SPARQLResultEx(it);
			} else {
				throw new RuntimeException("Unsupported query type");
			}
		} else if (stmt.isUpdateRequest()) {
			UpdateRequest u = stmt.getAsUpdateStmt().getUpdateRequest();

			conn.update(u);
			result = SPARQLResultEx.createUpdateType();
		}
		
		return result;
	}
	

	public static void output(SPARQLResultEx r) {
		//logger.info("Processing SPARQL Statement: " + stmt);
		if (r.isQuads()) {
			SinkQuadOutput sink = new SinkQuadOutput(System.out, null, null);
			Iterator<Quad> it = r.getQuads();
			while (it.hasNext()) {
				Quad t = it.next();
				sink.send(t);
			}
			sink.flush();
			sink.close();

		} else if (r.isTriples()) {
			// System.out.println(Algebra.compile(q));

			SinkTripleOutput sink = new SinkTripleOutput(System.out, null, null);
			Iterator<Triple> it = r.getTriples();
			while (it.hasNext()) {
				Triple t = it.next();
				sink.send(t);
			}
			sink.flush();
			sink.close();
		} else if (r.isResultSet()) {
			ResultSet rs =r.getResultSet();
			String str = ResultSetFormatter.asText(rs);
			System.err.println(str);
		} else if(r.isJson()) {
			JsonArray tmp = new JsonArray();
			r.getJsonItems().forEachRemaining(tmp::add);
			String json = tmp.toString();
			System.out.println(json);
		} else {
			throw new RuntimeException("Unsupported query type");
		}
	}

	public static void process(RDFConnection conn, SparqlStmt stmt) {
		SPARQLResultEx sr = execAny(conn, stmt);
		output(sr);
	}
	
	
	public static void processOld(RDFConnection conn, SparqlStmt stmt) {
		//logger.info("Processing SPARQL Statement: " + stmt);

		if (stmt.isQuery()) {
			SparqlStmtQuery qs = stmt.getAsQueryStmt();
			Query q = qs.getQuery();
			q.isConstructType();
			conn.begin(ReadWrite.READ);
			// SELECT -> STDERR, CONSTRUCT -> STDOUT
			QueryExecution qe = conn.query(q);

			if (q.isConstructQuad()) {
				// ResultSetFormatter.ntrqe.execConstructTriples();
				//throw new RuntimeException("not supported yet");
				SinkQuadOutput sink = new SinkQuadOutput(System.out, null, null);
				Iterator<Quad> it = qe.execConstructQuads();
				while (it.hasNext()) {
					Quad t = it.next();
					sink.send(t);
				}
				sink.flush();
				sink.close();

			} else if (q.isConstructType()) {
				// System.out.println(Algebra.compile(q));

				SinkTripleOutput sink = new SinkTripleOutput(System.out, null, null);
				Iterator<Triple> it = qe.execConstructTriples();
				while (it.hasNext()) {
					Triple t = it.next();
					sink.send(t);
				}
				sink.flush();
				sink.close();
			} else if (q.isSelectType()) {
				ResultSet rs = qe.execSelect();
				String str = ResultSetFormatter.asText(rs);
				System.err.println(str);
			} else if(q.isJsonType()) {
				String json = qe.execJson().toString();
				System.out.println(json);
			} else {
				throw new RuntimeException("Unsupported query type");
			}

			conn.end();
		} else if (stmt.isUpdateRequest()) {
			UpdateRequest u = stmt.getAsUpdateStmt().getUpdateRequest();

			conn.update(u);
		}
	}
}
