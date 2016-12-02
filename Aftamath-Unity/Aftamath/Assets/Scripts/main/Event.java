package main;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.Json.Serializable;

public class Event implements Comparable<Event>, Serializable{
	public String description;
	public int occurrence;
	public Event(){}
	public Event(String desc, int occ){
		this.description = desc;
		this.occurrence = occ;
	}
	public int compareTo(Event e) {
		if(e.occurrence<occurrence) return -1;
		if(e.occurrence>occurrence) return 1;
		return 0;
	}
	public void read(Json json, JsonValue val) {
		this.occurrence = val.getInt("occurrence");
		this.description = val.getString("desc");
	}
	public void write(Json json) {
		json.writeValue("occurrence", this.occurrence);
		json.writeValue("desc", this.description);
	}
}
