package main;

import java.util.HashMap;
import java.util.LinkedHashMap;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

//class that contains minor handling for all events and flags
public class History implements Serializable {

	public HashMap<String, Boolean> flagList;
	public float playTime;
	
	private LinkedHashMap<String, Event> eventList;
	private HashMap<String, Object> variableList;
	
	public History(){
		eventList = new LinkedHashMap<>();
		flagList = new HashMap<>();
		variableList = new HashMap<>();
		
		//static script variables
		variableList.put("true","true");
		variableList.put("false","false");
		variableList.put("male", "male");
		variableList.put("female", "female");
		variableList.put("time", 0);
		variableList.put("day", 0);
		variableList.put("noon", 1);
		variableList.put("night", 2);
		variableList.put("trainLoc", "CommercialDistrictNW");
		variableList.put("trainDest", "nowhere");
	}
	
	public Boolean getFlag(String flag){ 
		for(String p : flagList.keySet())
			if (p.equals(flag))
				return flagList.get(p);
		return null;
	}

	//creates the flag if no flag found
	public void setFlag(String flag, boolean val){
		for(String p : flagList.keySet())
			if(p.equals(flag)){
				flagList.put(p, val);
				return;
			}
		addFlag(flag, val);
	}
	
	public void addFlag(String flag, boolean val){ 
		for(String p : flagList.keySet())
			if(p.equals(flag))
				return;
			flagList.put(flag, val);
	}
	
	public boolean setEvent(String event, String description){ 
		if (findEvent(event)) {
			return false;
		}
		eventList.put(event, new Event(description, eventList.size() + 1));
		return true;
	}
	
	public boolean findEvent(String event){ 
		return eventList.containsKey(event);
	}
	
	public String getDescription(String event){
		if (findEvent(event)) {
			return eventList.get(event).description;
		}
		return null;
	}
	
	public boolean declareVariable(String variableName, Object value){
		if (value instanceof Boolean){ 
			addFlag(variableName, (Boolean)value);
			return true;
		}
		
		for(String p : variableList.keySet())
			if (p.equals(variableName))
				return false;
		if (!(value instanceof String) && !(value instanceof Integer) && !(value instanceof Float))
			return false;
		
		variableList.put(variableName, value);
		return true;
	}
	
	public Object getVariable(String variableName){
		for(String p : variableList.keySet()){
			if (p.equals(variableName))
				return variableList.get(p);
		}
		return null;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        
	}

	public void setVariable(String var, Object val){
		for(String p : variableList.keySet())
			if(p.equals(var)){
				String type = variableList.get(p).getClass().getSimpleName();
				if(!var.getClass().getSimpleName().equals(type)){
					try{
						if(type.toLowerCase().equals("float")){
							variableList.put(p, (float) val);
						}
						if(type.toLowerCase().equals("integer")){
							if(val instanceof Float)
								variableList.put(p, (int)((float) val));
							else
								variableList.put(p, (int)val);
						}if(type.toLowerCase().equals("string"))
							variableList.put(p, (String) val);
					} catch (Exception e){e.printStackTrace(); }
				} else 
					variableList.put(p, val);
			}
	}
	
	public HashMap<String, Object> getVarlist(){ return variableList; }
	public LinkedHashMap<String, Event> getEventList(){ return eventList; }

	@Override
	public void read(Json json, JsonValue val) {
		for (JsonValue child = val.getChild("eventList"); child != null; child = child.next()) {
			this.eventList.put(child.name(), json.fromJson(Event.class, child.toString()));
		}
		
		for (JsonValue child = val.getChild("flagList"); child != null; child = child.next()) {
			this.flagList.put(child.name(), child.getBoolean("value"));
		}
		
		for (JsonValue child = val.getChild("variableList"); child != null; child = child.next()) {
			Object obj = json.fromJson(Object.class, child.toString());
			this.variableList.put(child.name(), obj);
		}
		
		playTime = val.getInt("playTime");
	}

	@Override
	public void write(Json json) {
		json.writeValue("eventList", this.eventList);
		json.writeValue("flagList", this.flagList);
		json.writeValue("variableList", this.variableList);
		json.writeValue("playTime", this.playTime);
	}
}