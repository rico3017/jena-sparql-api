package org.aksw.jena_sparql_api_sparql_path2;

import org.apache.jena.sparql.path.Path;

public class PathRewriterVisitor
    implements PathRewriter
{
    protected PathVisitorRewrite visitor;

    public PathRewriterVisitor(PathVisitorRewrite visitor) {
        super();
        this.visitor = visitor;
    }

    @Override
    public Path apply(Path path) {
        path.visit(visitor);
        Path result = visitor.getResult();
        return result;
    }

}