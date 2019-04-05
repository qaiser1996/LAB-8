/* Copyright (c) 2015-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package expressivo;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.*;
import expressivo.parser.*;

/**
 * An immutable data type representing a polynomial expression of:
 *   + and *
 *   nonnegative integers and floating-point numbers
 *   variables (case-sensitive nonempty strings of letters)
 * 
 * <p>PS3 instructions: this is a required ADT interface.
 * You MUST NOT change its name or package or the names or type signatures of existing methods.
 * You may, however, add additional methods, or strengthen the specs of existing methods.
 * Declare concrete variants of Expression in their own Java source files.
 */
public interface Expression {
    
    // Datatype definition:
	// 
	// Expression ::= 
	// 		Primitive(p: Primitive) + BinOp(left: Primitive, right: Primitive)
	// Primitive ::= 
	//		Number(n: int) + Number(n: float) + Variable(v: String)
    
    /**
     * Parse an expression.
     * @param input expression to parse, as defined in the PS3 handout.
     * @return expression AST for the input
     * @throws IllegalArgumentException if the expression is invalid
     */
    public static Expression parse(String input) {
        CharStream stream = new ANTLRInputStream(input);
        ExpressionLexer lexer = new ExpressionLexer(stream);
        TokenStream tokens = new CommonTokenStream(lexer);
        ExpressionParser parser = new ExpressionParser(tokens);
        
        parser.reportErrorsAsExceptions();
        lexer.reportErrorsAsExceptions();

        
        ParseTree tree;
        try {
        	tree = parser.root();
        } catch (ParseCancellationException ex) {
        	String reason = ex.getMessage();
        	String msg;
        	if (reason != null)
        		msg = "Syntax error in expression: " + reason;
        	else
        		msg = "Undefined syntax error in expression";

    		throw new IllegalArgumentException(msg);
        }
        
        ParseTreeWalker walker = new ParseTreeWalker();
        ExpressionGenerator listener = new ExpressionGenerator();
        
        walker.walk(listener, tree);
        
        return listener.get();
    }
    
    /**
     * Find whether this Expression will have precedence over another.
     * If not, brackets will be needed to combine the two expressions in
     * a human-readable format (eg a string).
     * 
     * @param other the second Expression 
     * @return true if this Expression can be combined with other without
     *         needing brackets, false otherwise
     */
    public boolean precedes(Expression other);
    
    /**
     * @param variable Variable to differentiate by
     * @return differential of this expression with respect to variable
     */
    public Expression differentiate(Variable variable);
    
    /**
     * @return a version of this Expression in its simplest form, 
     *         replacing e.g. Sum(Numeric, Numeric) with the sum of the values
     */
    public Expression simplify();
    
    /**
     * @return a parsable representation of this expression, such that
     * for all e:Expression, e.equals(Expression.parse(e.toString())).
     */
    @Override 
    public String toString();

    /**
     * @param thatObject any object
     * @return true if and only if this and thatObject are structurally-equal
     * Expressions, as defined in the PS3 handout.
     */
    @Override
    public boolean equals(Object thatObject);
    
    /**
     * @return hash code value consistent with the equals() definition of structural
     * equality, such that for all e1,e2:Expression,
     *     e1.equals(e2) implies e1.hashCode() == e2.hashCode()
     */
    @Override
    public int hashCode();   
}
