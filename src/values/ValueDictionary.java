package values;

import java.util.Hashtable;

public class ValueDictionary extends ValueAbstract {
	
	private Hashtable<String, Value> internalValue;
	
	public ValueDictionary(Hashtable<String, Value> values) {
		internalValue = values;
	}
	
	public String getName() {
		return "dictionary";
	}
	
	public Hashtable<String, Value> getDictionary() {
		return internalValue;
	}
	
	public int length() {
		return internalValue.size();
	}
	
	public Value get(String key) {
		return internalValue.get(key);
	}
	
	public Value put(String key, Value val) {
		return internalValue.put(key, val);
	}
	
	public void pop(String key) {
		internalValue.remove(key);
	}
	
	public void replace(String key, Value val) {
		internalValue.replace(key, val);
	}
	
	public int compare(Value v) {
		return 0;
	}
}
