package org.aksw.jena_sparql_api.utils;

import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.sparql.core.Var;

public class NodeUtils {

    public static Node createTypedLiteral(TypeMapper typeMapper, Object o) {
        Class<?> clazz = o.getClass();
        RDFDatatype dtype = typeMapper.getTypeByClass(clazz);
        String lex = dtype.unparse(o);
        Node result = NodeFactory.createLiteral(lex, dtype);
        return result;
    }

    public static Set<Node> getBnodesMentioned(Iterable<Node> nodes) {
        Set<Node> result = new HashSet<Node>();
        for (Node node : nodes) {
            if (node.isBlank()) {
                result.add(node);
            }
        }

        return result;
    }

    public static Set<Var> getVarsMentioned(Iterable<Node> nodes)
    {
        Set<Var> result = new HashSet<Var>();
        for (Node node : nodes) {
            if (node.isVariable()) {
                result.add((Var)node);
            }
        }

        return result;
    }

    public static String toNTriplesString(Node node) {
        String result;
        if(node.isURI()) {
            result = "<" + node.getURI() + ">";
        }
        else if(node.isLiteral()) {
            String lex = node.getLiteralLexicalForm();
            String lang = node.getLiteralLanguage();
            String dt = node.getLiteralDatatypeURI();

            String tmp = lex;
            // \\   \"   \n    \t   \r
            tmp = tmp.replace("\\", "\\\\");
            tmp = tmp.replace("\"", "\\\"");
            tmp = tmp.replace("\n", "\\n");
            tmp = tmp.replace("\t", "\\t");
            tmp = tmp.replace("\r", "\\r");

            String encoded = tmp;
            // If fields contain new lines, escape them with triple quotes
//			String quote = encoded.contains("\n")
//					? "\"\"\""
//					: "\"";
            String quote = "\"";

            result =  quote + encoded + quote;

            if(dt != null && !dt.isEmpty()) {
                result = result + "^^<" + dt+ ">";
            } else {
                if(!lang.isEmpty()) {
                    result = result + "@" + lang;
                }
            }
        }
        else if(node.isBlank()) {
            result = node.getBlankNodeLabel();
        } else {
            throw new RuntimeException("Cannot serialize [" + node + "] as N-Triples");
        }

        return result;
    }
}