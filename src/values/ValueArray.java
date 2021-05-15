package values;

import java.util.Vector;
import java.util.Comparator;
import java.util.Collections;

public class ValueArray extends ValueAbstract {
	
	private Vector<Value> internalValue;
	
	public ValueArray(Vector<Value> values) {
		internalValue = values;
	}
	
	public String getName() {
		return "array";
	}
	
	public Vector<Value> getAllItems() {
		return internalValue;
	}

	public Value getItem(int index) {
		return internalValue.get(index);
	}
	
	public void setItem(int index, Value val) {
		internalValue.set(index, val);
	}
	
	public void pushItem(Value val) {
		internalValue.add(val);
	}
	
	public Value popItem() {
		return internalValue.remove(internalValue.size() - 1);
	}
	
	public int length() {
		return internalValue.size();
	}
	
	public void sort() {
		System.out.println("here: sort function");
		Comparator comparator = Collections.reverseOrder();
		
		Collections.sort(internalValue, comparator);
		
		//internalValue.sort(vector, comparator);
	}
	
	public Value first() {
		return internalValue.firstElement();
	}
	
	public Value last() {
		return internalValue.lastElement();
	}
	
	public void insert(int index, Value val) {
		internalValue.insertElementAt(val, index);
	}
	
	public Value remove(int index) {
		return internalValue.remove(index);
	}
	
	public int compare(Value v) {
		return 0;
	}
}
