package entities;

import handlers.Vars;
import characters.NPC;

public class Partner extends NPC {
	private String name;
	private String gender;
	private String information;
	
	public Partner(String name, String newGender, int ID, int x, int y) {
		super(name, "girlfriend" + ID, x, y, DEFAULT_WIDTH, DEFAULT_HEIGHT, Vars.BIT_LAYER3);
		this.name = name;
		gender = newGender;
		makeInvulnerable(-1);
		setDefaultState(FACEPLAYER);
		health = MAX_HEALTH = 20;
	}
	
	public Partner(){
		super(null, "", 0, 0, 10, 25, Vars.BIT_LAYER3);
		gender = "n/a";
	}
	
	public void kiss() {
		
	}
	
	public String getGender(){
		return gender;
	}
	
	public String getInfo(){
		return information;
	}
	
	public void setInfo(String info){
		information = info;
	}
	
	public String getName(){
		return name;
	}
	
	
	
}
