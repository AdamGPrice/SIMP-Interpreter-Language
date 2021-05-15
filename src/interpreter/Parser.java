package interpreter;

import java.util.Vector;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;

import parser.ast.*;
import values.*;

public class Parser implements SiliVisitor {
	
	// Scope display handler
	private Display scope = new Display();
	
	// Get the ith child of a given node.
	private static SimpleNode getChild(SimpleNode node, int childIndex) {
		return (SimpleNode)node.jjtGetChild(childIndex);
	}
	
	// Get the token value of the ith child of a given node.
	private static String getTokenOfChild(SimpleNode node, int childIndex) {
		return getChild(node, childIndex).tokenValue;
	}
	
	// Execute a given child of the given node
	private Object doChild(SimpleNode node, int childIndex, Object data) {
		return node.jjtGetChild(childIndex).jjtAccept(this, data);
	}
	
	// Execute a given child of a given node, and return its value as a Value.
	// This is used by the expression evaluation nodes.
	Value doChild(SimpleNode node, int childIndex) {
		return (Value)doChild(node, childIndex, null);
	}
	
	// Execute all children of the given node
	Object doChildren(SimpleNode node, Object data) {
		return node.childrenAccept(this, data);
	}
	
	// Called if one of the following methods is missing...
	public Object visit(SimpleNode node, Object data) {
		System.out.println(node + ": acceptor not implemented in subclass?");
		return data;
	}
	
	// Execute a Sili program
	public Object visit(ASTCode node, Object data) {
		return doChildren(node, data);	
	}
	
	// Execute a statement
	public Object visit(ASTStatement node, Object data) {
		return doChildren(node, data);	
	}

	// Execute a block
	public Object visit(ASTBlock node, Object data) {
		return doChildren(node, data);	
	}

	// Function definition
	public Object visit(ASTFnDef node, Object data) {
		// Already defined?
		if (node.optimised != null)
			return data;
		// Child 0 - identifier (fn name)
		String fnname = getTokenOfChild(node, 0);
		if (scope.findFunctionInCurrentLevel(fnname) != null)
			throw new ExceptionSemantic("Function " + fnname + " already exists.");
		FunctionDefinition currentFunctionDefinition = new FunctionDefinition(fnname, scope.getLevel() + 1);
		// Child 1 - function definition parameter list
		doChild(node, 1, currentFunctionDefinition);
		// Add to available functions
		scope.addFunction(currentFunctionDefinition);
		// Child 2 - function body
		currentFunctionDefinition.setFunctionBody(getChild(node, 2));
		// Child 3 - optional return expression
		if (node.fnHasReturn)
			currentFunctionDefinition.setFunctionReturnExpression(getChild(node, 3));
		// Preserve this definition for future reference, and so we don't define
		// it every time this node is processed.
		node.optimised = currentFunctionDefinition;
		return data;
	}
	
	// Function definition parameter list
	public Object visit(ASTParmlist node, Object data) {
		FunctionDefinition currentDefinition = (FunctionDefinition)data;
		for (int i=0; i<node.jjtGetNumChildren(); i++)
			currentDefinition.defineParameter(getTokenOfChild(node, i));
		return data;
	}
	
	// Function body
	public Object visit(ASTFnBody node, Object data) {
		return doChildren(node, data);
	}
	
	// Function return expression
	public Object visit(ASTReturnExpression node, Object data) {
		return doChildren(node, data);
	}
	
	// Function call
	public Object visit(ASTCall node, Object data) {
		FunctionDefinition fndef;
		if (node.optimised == null) { 
			// Child 0 - identifier (fn name)
			String fnname = getTokenOfChild(node, 0);
			fndef = scope.findFunction(fnname);
			if (fndef == null)
				throw new ExceptionSemantic("Function " + fnname + " is undefined.");
			// Save it for next time
			node.optimised = fndef;
		} else
			fndef = (FunctionDefinition)node.optimised;
		FunctionInvocation newInvocation = new FunctionInvocation(fndef);
		// Child 1 - arglist
		doChild(node, 1, newInvocation);
		// Execute
		scope.execute(newInvocation, this);
		return data;
	}
	
	// Function invocation in an expression
	public Object visit(ASTFnInvoke node, Object data) {
		FunctionDefinition fndef;
		if (node.optimised == null) { 
			// Child 0 - identifier (fn name)
			String fnname = getTokenOfChild(node, 0);
			fndef = scope.findFunction(fnname);
			if (fndef == null)
				throw new ExceptionSemantic("Function " + fnname + " is undefined.");
			if (!fndef.hasReturn())
				throw new ExceptionSemantic("Function " + fnname + " is being invoked in an expression but does not have a return value.");
			// Save it for next time
			node.optimised = fndef;
		} else
			fndef = (FunctionDefinition)node.optimised;
		FunctionInvocation newInvocation = new FunctionInvocation(fndef);
		// Child 1 - arglist
		doChild(node, 1, newInvocation);
		// Execute
		return scope.execute(newInvocation, this);
	}
	
	// Function invocation argument list.
	public Object visit(ASTArgList node, Object data) {
		FunctionInvocation newInvocation = (FunctionInvocation)data;
		for (int i=0; i<node.jjtGetNumChildren(); i++)
			newInvocation.setArgument(doChild(node, i));
		newInvocation.checkArgumentCount();
		return data;
	}
	
	// Execute an IF 
	public Object visit(ASTIfStatement node, Object data) {
		// evaluate boolean expression
		Value hopefullyValueBoolean = doChild(node, 0);
		if (!(hopefullyValueBoolean instanceof ValueBoolean))
			throw new ExceptionSemantic("The test expression of an if statement must be boolean.");
		if (((ValueBoolean)hopefullyValueBoolean).booleanValue())
			doChild(node, 1);							// if(true), therefore do 'if' statement
		else if (node.ifHasElse)						// does it have an else statement?
			doChild(node, 2);							// if(false), therefore do 'else' statement
		return data;
	}
	
	// Execute a FOR loop
	public Object visit(ASTForLoop node, Object data) {
		// loop initialisation
		doChild(node, 0);
		while (true) {
			// evaluate loop test
			Value hopefullyValueBoolean = doChild(node, 1);
			if (!(hopefullyValueBoolean instanceof ValueBoolean))
				throw new ExceptionSemantic("The test expression of a for loop must be boolean.");
			if (!((ValueBoolean)hopefullyValueBoolean).booleanValue())
				break;
			// do loop statement
			doChild(node, 3);
			// assign loop increment
			doChild(node, 2);
		}
		return data;
	}
	
	// Execute a FOR loop
	public Object visit(ASTForToLoop node, Object data) {	
		Display.Reference reference;
		if (node.optimised == null) {
			String indexname = getTokenOfChild(node, 0);
			reference = scope.findReference(indexname);
			if (reference == null)
				reference = scope.defineVariable(indexname);
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		
		int end = 0;
		int offset = 0;
	
		if (node.loopHasStart) {
			reference.setValue(doChild(node, 1));			
			end = (int)doChild(node, 2).longValue();
		} else {
			reference.setValue(new ValueInteger(0));
			end = (int)doChild(node, 1).longValue();
			offset += - 1;
		}
		
		
		ValueInteger index = (ValueInteger)reference.getValue();
		
		//System.out.println(index.longValue());
		//System.out.println(end);
		
		int stepval = 1;
		if (node.loopHasStep) {
			Value step = doChild(node, 3);
			stepval = (int)step.longValue();
		} else {
			offset += - 1;
		}
		
		
		if ((int)index.longValue() <= end) {
			for (int i = (int)index.longValue(); i < end; i = i + stepval) {
				reference.setValue(new ValueInteger(i));
				doChild(node, 4 + offset);
			}			
		} else {
			for (int i = (int)index.longValue(); i > end; i = i - stepval) {
				reference.setValue(new ValueInteger(i));
				doChild(node, 4 + offset);
			}	
		}
		
		return data;
	}
	
	// Execute a WHILE loop
	public Object visit(ASTWhileLoop node, Object data) {
		while (true) {
			//evaluate the loop
			Value hopefullyValueBoolean = doChild(node, 0);
			if (!((ValueBoolean)hopefullyValueBoolean).booleanValue())
				break;
			doChild(node, 1);
		}
		return data;
	}
	
	// Process an identifier
	// This doesn't do anything, but needs to be here because we need an ASTIdentifier node.
	public Object visit(ASTIdentifier node, Object data) {
		return data;
	}
	
	// Execute the PRINT statement
	public Object visit(ASTPrint node, Object data) {
		System.out.print(doChild(node, 0));
		return data;
	}
	
	// Execute the PRINTLN statement
	public Object visit(ASTPrintLn node, Object data) {
		System.out.println(doChild(node, 0));
		return data;
	}
	
	// Execute the Quit statement (Exit the program)
	public Object visit(ASTQuit node, Object data) {
		System.exit(0);
		return data;
	}
	
	// Dereference a variable or parameter, and return its value.
	public Object visit(ASTDereference node, Object data) {
		Display.Reference reference;
		if (node.optimised == null) {
			String name = node.tokenValue;
			reference = scope.findReference(name);
			if (reference == null)
				throw new ExceptionSemantic("Variable or parameter " + name + " is undefined.");
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		return reference.getValue();
	}
	
	// Execute an assignment statement.
	public Object visit(ASTAssignment node, Object data) {
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				reference = scope.defineVariable(name);
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		
		reference.setValue(doChild(node, 1));
		return data;
	}
	
	// Execute an ASTAdditionAssignment statement.
	public Object visit(ASTAdditionAssignment node, Object data) {
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				reference = scope.defineVariable(name);
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		
		reference.setValue(reference.getValue().add(doChild(node, 1)));
		return data;
	}
	
	// Execute an ASTAdditionAssignment statement.
	public Object visit(ASTSubtractionAssignment node, Object data) {
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				reference = scope.defineVariable(name);
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		
		reference.setValue(reference.getValue().subtract(doChild(node, 1)));
		return data;
	}
	
	
	// Array init list
	public Object visit(ASTArrayList node, Object data) {
		Vector<Value> values = new Vector<Value>(0);	
		for (int i = 0; i < node.jjtGetNumChildren(); i++) {
			values.add(doChild(node, i));
		}
		
		ValueArray arr = new ValueArray(values);
		node.optimised = arr;
		return node.optimised;
	}
	
	////////////////////////// ARRAY FUNCTIONS

	// Get array index value
	public Object visit(ASTArrayGetIndex node, Object data) {
		Value index = doChild(node, 1);
		
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				throw new ExceptionSemantic("Variable or parameter " + name + " is undefined.");
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		
		ValueArray arr = (ValueArray)reference.getValue();
		Value item = arr.getItem((int)index.longValue());
		
		return item;
	}
	
	// Set array index value
	public Object visit(ASTArraySetIndex node, Object data) {
		Value index = doChild(node, 1);
		Value val = doChild(node, 2);
		
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				throw new ExceptionSemantic("Variable or parameter " + name + " is undefined.");
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
				
		ValueArray arr = (ValueArray)reference.getValue();	
		arr.setItem((int)index.longValue(), val);

		return data;
	}
	
	// PUSH
	public Object visit(ASTArrayPush node, Object data) {
		Value val = doChild(node, 1);
		
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				throw new ExceptionSemantic("Variable or parameter " + name + " is undefined.");
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
				
		ValueArray arr = (ValueArray)reference.getValue();	
		arr.pushItem(val);

		return data;
	}
	
	// POP
	public Object visit(ASTArrayPop node, Object data) {	
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				throw new ExceptionSemantic("Variable or parameter " + name + " is undefined.");
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
				
		ValueArray arr = (ValueArray)reference.getValue();	
		Value item = arr.popItem();
		
		return item;
	}
	
	// Length
	public Object visit(ASTArrayLength node, Object data) {	
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				throw new ExceptionSemantic("Variable or parameter " + name + " is undefined.");
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
				
		ValueArray arr = (ValueArray)reference.getValue();	
		ValueInteger len = new ValueInteger(arr.length());
		
		return len;
	}
	
	// Print
	public Object visit(ASTArrayPrint node, Object data) {	
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				throw new ExceptionSemantic("Variable or parameter " + name + " is undefined.");
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		
		Value object = reference.getValue();
		
		if (object.getName() == "array") {
			ValueArray arr = (ValueArray)object;	
			
			System.out.print("[ ");
			for (int i = 0; i < arr.length(); i++) {
				System.out.print(arr.getItem(i).toString());
				
				if (i != arr.length() - 1)
					System.out.print(", ");
			}
			System.out.println(" ]");
		} else if ((object.getName() == "dictionary")) {
			ValueDictionary dict = (ValueDictionary)object;			
			Hashtable hash = dict.getDictionary();		
			Set<String> keys = hash.keySet();
	        
			System.out.print("{ ");
			int count = 1;
			
			for(String key: keys) {
				System.out.print("\"" + key + "\"");
				System.out.print(":");
				
				Value v1 = (Value)hash.get(key);
				
				if (v1.getName() == "string") {
					System.out.print("\"" + hash.get(key) + "\"");
				} else {
					System.out.print(hash.get(key));					
				}
				
				
				if (count < keys.size()) {
					System.out.print(", ");					
				}
				count++;
			}
			System.out.println(" }");
		}
				
		return data;
	}
	
	// Print
	public Object visit(ASTArraySort node, Object data) {	
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				throw new ExceptionSemantic("Variable or parameter " + name + " is undefined.");
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
				
		ValueArray arr = (ValueArray)reference.getValue();	
		
		arr.sort();
		
		return data;
	}
	
	// FIST
	public Object visit(ASTArrayFirst node, Object data) {	
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				throw new ExceptionSemantic("Variable or parameter " + name + " is undefined.");
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
				
		ValueArray arr = (ValueArray)reference.getValue();	
		Value item = arr.first();
		
		return item;
	}
	
	// LAST
	public Object visit(ASTArrayLast node, Object data) {	
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				throw new ExceptionSemantic("Variable or parameter " + name + " is undefined.");
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
				
		ValueArray arr = (ValueArray)reference.getValue();	
		Value item = arr.last();
		
		return item;
	}
	
	// ARRAY REMOVE AT
	public Object visit(ASTArrayRemove node, Object data) {
		Value index = doChild(node, 1);
		
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				throw new ExceptionSemantic("Variable or parameter " + name + " is undefined.");
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		
		ValueArray arr = (ValueArray)reference.getValue();
		Value item = arr.remove((int)index.longValue());
		
		return item;
	}
	
	// ARRAY INSERT AT
	public Object visit(ASTArrayInsert node, Object data) {
		Value index = doChild(node, 1);
		Value val = doChild(node, 2);
		
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				throw new ExceptionSemantic("Variable or parameter " + name + " is undefined.");
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
				
		ValueArray arr = (ValueArray)reference.getValue();	
		arr.insert((int)index.longValue(), val);

		return data;
	}
	
	//////////////////////////////////// ARRAY FUNCTIONS END
	
	
	///////////////////////////////////// DICTIONARY FUNCTIONS START
	
	
	// Dictionary init list
	public Object visit(ASTDictionaryList node, Object data) {
		Hashtable<String, Value> hash = new Hashtable<String, Value>();
		 
		for(int i = 0; i < node.jjtGetNumChildren(); i += 2) {
			ValueString key = (ValueString)doChild(node, i);
			Value val = (Value)doChild(node, i + 1);
			
			//System.out.println(key.toString());
			//System.out.println(val.toString());
			
			hash.put(key.toString(), val);
		}
		
		//hash.
		//Value v = hash.get("apple");
		//System.out.println(v.toString());
		
		ValueDictionary dict = new ValueDictionary(hash);
	
		//System.out.println(dict.get(new ValueString("apple")));
		
		node.optimised = dict;
		return node.optimised;
	}
	
	
	// Get array index value
	public Object visit(ASTDictGet node, Object data) {
		ValueString key = (ValueString)doChild(node, 1);
		
		//System.out.println(key.toString());
		
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				throw new ExceptionSemantic("Variable or parameter " + name + " is undefined.");
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		
		ValueDictionary dict = (ValueDictionary)reference.getValue();
		
		
		Value item = dict.get(key.toString());
		return item;
	}
	
	// Get array index value
	public Object visit(ASTDictPut node, Object data) {
		ValueString key = (ValueString)doChild(node, 1);
		Value val = (Value)doChild(node, 2);
		//System.out.println(key.toString());
		
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				throw new ExceptionSemantic("Variable or parameter " + name + " is undefined.");
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		
		ValueDictionary dict = (ValueDictionary)reference.getValue();
		
		
		Value item = dict.put(key.toString(), val);
		return item;
	}
	
	// Get array index value
	public Object visit(ASTDictPop node, Object data) {
		ValueString key = (ValueString)doChild(node, 1);
		
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				throw new ExceptionSemantic("Variable or parameter " + name + " is undefined.");
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		
		ValueDictionary dict = (ValueDictionary)reference.getValue();
		
		
		dict.pop(key.toString());
		return data;
	}

	// Get array index value
	public Object visit(ASTDictReplace node, Object data) {
		Value key = (Value)doChild(node, 1);
		//System.out.println("here");
		Value val = (Value)doChild(node, 2);
		
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				throw new ExceptionSemantic("Variable or parameter " + name + " is undefined.");
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		
		ValueDictionary dict = (ValueDictionary)reference.getValue();
		
		dict.replace(key.toString(), val);
		return data;
	}
	
	
	/////////////////////////////////////// DICTIONARY FUNCTIONS END
	
	
	// OR
	public Object visit(ASTOr node, Object data) {
		return doChild(node, 0).or(doChild(node, 1));
	}

	// AND
	public Object visit(ASTAnd node, Object data) {
		return doChild(node, 0).and(doChild(node, 1));
	}

	// ==
	public Object visit(ASTCompEqual node, Object data) {
		return doChild(node, 0).eq(doChild(node, 1));
	}

	// !=
	public Object visit(ASTCompNequal node, Object data) {
		return doChild(node, 0).neq(doChild(node, 1));
	}

	// >=
	public Object visit(ASTCompGTE node, Object data) {
		return doChild(node, 0).gte(doChild(node, 1));
	}

	// <=
	public Object visit(ASTCompLTE node, Object data) {
		return doChild(node, 0).lte(doChild(node, 1));
	}

	// >
	public Object visit(ASTCompGT node, Object data) {
		return doChild(node, 0).gt(doChild(node, 1));
	}

	// <
	public Object visit(ASTCompLT node, Object data) {
		return doChild(node, 0).lt(doChild(node, 1));
	}

	// +
	public Object visit(ASTAdd node, Object data) {
		return doChild(node, 0).add(doChild(node, 1));
	}

	// -
	public Object visit(ASTSubtract node, Object data) {
		return doChild(node, 0).subtract(doChild(node, 1));
	}

	// *
	public Object visit(ASTTimes node, Object data) {
		return doChild(node, 0).mult(doChild(node, 1));
	}

	// /
	public Object visit(ASTDivide node, Object data) {
		return doChild(node, 0).div(doChild(node, 1));
	}
	
	// %
	public Object visit(ASTRemainder node, Object data) {
		return doChild(node, 0).mod(doChild(node, 1));
	}
	
	// ^
	public Object visit(ASTPow node, Object data) {
		return doChild(node, 0).pow(doChild(node, 1));
	}

	// **
	public Object visit(ASTSqrt node, Object data) {
		return doChild(node, 0).sqrt();
	}
	
	// ?
	public Object visit(ASTRandom node, Object data) {
		double val = Math.random();
		int start = (int)doChild(node, 0).longValue();
		int end = (int)doChild(node, 1).longValue();		
		
		val = val * (end - start) + start;
		return new ValueRational(val);
	}

	// ~ floor
	public Object visit(ASTFloor node, Object data) {
		return new ValueInteger((int)doChild(node, 0).doubleValue());
	}
	
	// $ Cos
	public Object visit(ASTCos node, Object data) {
		double val = doChild(node, 0).doubleValue();
		val = Math.cos(val);
	
		return new ValueRational(val);
	}
	
	// $ Cos
	public Object visit(ASTSin node, Object data) {
		double val = doChild(node, 0).doubleValue();
		val = Math.sin(val);
	
		return new ValueRational(val);
	}
	
	// NOT
	public Object visit(ASTUnaryNot node, Object data) {
		return doChild(node, 0).not();
	}

	// + (unary)
	public Object visit(ASTUnaryPlus node, Object data) {
		return doChild(node, 0).unary_plus();
	}

	// - (unary)
	public Object visit(ASTUnaryMinus node, Object data) {
		return doChild(node, 0).unary_minus();
	}

	// Return string literal
	public Object visit(ASTCharacter node, Object data) {
		if (node.optimised == null)
			node.optimised = ValueString.stripDelimited(node.tokenValue);
		return node.optimised;
	}

	// Return integer literal
	public Object visit(ASTInteger node, Object data) {
		if (node.optimised == null)
			node.optimised = new ValueInteger(Long.parseLong(node.tokenValue));
		return node.optimised;
	}

	// Return floating point literal
	public Object visit(ASTRational node, Object data) {
		if (node.optimised == null)
			node.optimised = new ValueRational(Double.parseDouble(node.tokenValue));
		return node.optimised;
	}

	// Return true literal
	public Object visit(ASTTrue node, Object data) {
		if (node.optimised == null)
			node.optimised = new ValueBoolean(true);
		return node.optimised;
	}

	// Return false literal
	public Object visit(ASTFalse node, Object data) {
		if (node.optimised == null)
			node.optimised = new ValueBoolean(false);
		return node.optimised;
	}

}
