/* Generated By:JJTree: Do not edit this line. ASTSubtractionAssignment.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=false,NODE_PREFIX=AST,NODE_EXTENDS=interpreter.BaseASTNode,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package parser.ast;

public
class ASTSubtractionAssignment extends SimpleNode {
  public ASTSubtractionAssignment(int id) {
    super(id);
  }

  public ASTSubtractionAssignment(Simp p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(SimpVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=8e52604adec7318686362727b1b526f3 (do not edit this line) */