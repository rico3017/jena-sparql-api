package org.aksw.jena_sparql_api.sparql.ext.json;

import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.vocabulary.XSD;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

public class RDFDatatypeJson
    extends BaseDatatype
{
	public static final String IRI = XSD.getURI() + "json"; 
	public static final RDFDatatypeJson INSTANCE = new RDFDatatypeJson();
	
    private Gson gson;

    public RDFDatatypeJson() {
    	this(IRI);
    }

    public RDFDatatypeJson(String uri) {
        this(uri, new GsonBuilder().setLenient().create());
    }

    public RDFDatatypeJson(String uri, Gson gson) {
        super(uri);
        this.gson = gson;
    }

    @Override
    public Class<?> getJavaClass() {
        return JsonElement.class;
    }

    /**
     * Convert a value of this datatype out
     * to lexical form.
     */
    @Override
    public String unparse(Object value) {
        String result = gson.toJson(value);
        return result;
    }

    /**
     * Parse a lexical form of this datatype to a value
     * @throws DatatypeFormatException if the lexical form is not legal
     */
    @Override
    public JsonElement parse(String lexicalForm) throws DatatypeFormatException {
    	//Object result = gson.fromJson(lexicalForm, Object.class);
    	JsonElement result;
    	try {
    		result = gson.fromJson(lexicalForm, JsonElement.class);
    	} catch(Exception e) {
    		// TODO This is not the best place for an expr eval exception; it should go to E_StrDatatype
    		throw new ExprEvalException(e);
    	}
        return result;
    }

}