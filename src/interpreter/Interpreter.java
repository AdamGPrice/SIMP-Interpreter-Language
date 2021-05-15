package interpreter;

import parser.ast.ASTCode;
import parser.ast.Simp;
import parser.ast.SimpVisitor;
import values.Value;

import java.io.*;
import java.util.Vector;

public class Interpreter {
	
	private static void usage() {
		System.out.println("Usage: sili [-d1] < <source>");
		System.out.println("          -d1 -- output AST");
	}
	
	public static void main(String args[]) {
		boolean debugAST = false;
		if (args.length == 1) {
			if (args[0].equals("-d1"))
				debugAST = true;
			else {
				usage();
				return;
			}
		}
		
		// Import any base library code the user wants to use
		importLibraries();
		
		Simp language = new Simp(System.in);
		try {
			ASTCode parser = language.code();
			SimpVisitor nodeVisitor;
			if (debugAST)
				nodeVisitor = new ParserDebugger();
			else
				nodeVisitor = new Parser();
			parser.jjtAccept(nodeVisitor, null);
		} catch (Throwable e) {
			System.out.println(e.getMessage());
		}
	}
	
	public static void importLibraries() {
		//importLib("maths");
		//importLib("test");
		
		checkforincludes();
	}
	
	public static void checkforincludes() {
		char include[] = "import".toCharArray();

		try {
			Vector<Byte> savedbytes = new Vector<Byte>(0); 
				
			java.io.InputStream old = System.in;
			boolean finished = false;
			while(!finished) {
				byte nextb = (byte)old.read();
				if (nextb == '#') {
					byte includebytes[] = old.readNBytes(6);
					
					boolean includeWrong = false;
					for (int i = 0; i < includebytes.length; i++) {
						if (include[i] != (char)includebytes[i]) {
							includeWrong = true;
						}
					}
					if (includeWrong) {
						System.out.println("ERROR: Include syntax wrong.");
					}
					
					byte nextb2 = (byte)old.read();
					boolean blankspace = true;
					
					while (blankspace) {
						if (nextb2 == '\'') {
							blankspace = false;							
						} else {
							nextb2 = (byte)old.read();							
						}
					}
					
					nextb2 = (byte)old.read();	
					Vector<Byte> libname = new Vector<Byte>(0);
					libname.add(nextb2);
					
					boolean gotname = false; 
					while (!gotname) {
						nextb2 = (byte)old.read();
						if (nextb2 != '\'') {
							libname.add(nextb2);							
						} else {
							gotname = true;
						}
					}
					
					String name = "";
					for (int i = 0; i < libname.size(); i++) {
						name += (char)(int)libname.get(i);
					}
					//System.out.println(name);

					
					/// Load bytes from maths.sil and append to the savedbytes vector
					
					byte newbytes[] = importLib2(name);
					
					if (newbytes.length == 0 ) {
						System.out.println("Package: '" + name + "' not found.");
					}
					
					
					for (int i = 0; i < newbytes.length; i++) {
						savedbytes.add(newbytes[i]);
					}
										
					//savedbytes.add(nextb);
				} else {
					savedbytes.add(nextb);
				}
				
				if (old.available() == 0) {
					finished = true;
				}
			}	
			
			byte combinebytes[] = new byte[savedbytes.size()];
			
			for (int i = 0; i < combinebytes.length; i++) {
				combinebytes[i] = savedbytes.get(i);
				//System.out.print((char)(int)savedbytes.get(i));
			}
			
			
			
			java.io.InputStream targetStream = new java.io.ByteArrayInputStream(combinebytes);		
			System.setIn(targetStream);	
			
		} catch (Throwable e) {
			System.out.println(e.getMessage());
		}
	}
	
	public static byte[] importLib2(String lib) {
		try {
			java.io.File initialFile = new java.io.File(System.getProperty("user.dir") + "/src/libs/" + lib + ".simp");
			//System.out.println(initialFile.toString());
			java.io.InputStream testInput = new java.io.FileInputStream(initialFile);
			
			byte newbytes[] = testInput.readAllBytes();
			return newbytes;
			
		} catch (Throwable e) {
			System.out.println(e.getMessage());
		}
		
		return new byte[0];
	}
	
	
	public static void importLib(String lib) {
		// Code to insert files into the system stream for pre-built functions
		try {	
			java.io.File initialFile = new java.io.File(System.getProperty("user.dir") + "/src/libs/" + lib + ".sil");
			java.io.InputStream testInput = new java.io.FileInputStream(initialFile);
			
			java.io.InputStream old = System.in;
			
			byte oldbytes[] = old.readAllBytes();
			byte newbytes[] = testInput.readAllBytes();
			
			byte combinebytes[] = new byte[oldbytes.length + newbytes.length];
			
			for (int i = 0; i < oldbytes.length + newbytes.length; i++) {
				if (i < newbytes.length) {
					combinebytes[i] = newbytes[i];
				} else {
					combinebytes[i] = oldbytes[i - newbytes.length];
				}
			}
			
			
			java.io.InputStream targetStream = new java.io.ByteArrayInputStream(combinebytes);		
			System.setIn( targetStream );	
		} catch (Throwable e) {
			System.out.println(e.getMessage());
		}		
	}
}
