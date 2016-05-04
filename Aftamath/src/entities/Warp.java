package entities;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import handlers.TextTrigger;
import handlers.Vars;
import scenes.Scene;

public class Warp extends Entity {

	public String next = null;
//	public Scene owner;
	public boolean outside;
	public String locTitle;
	public String locID;	//filename ID
	public int warpID;
	public boolean instant;
	public TransType transitionType;
	private TextTrigger tt;
	
	private String condition;
	private Vector2 warpLoc, offset;
	private int nextWarp;
	private Warp link;
//	private boolean prevAvail = false;
	
	public enum TransType {
		HORIZONTAL_BARS, PINHOLE, ZOOM, BLACKOUT, FADE, FADE_WHITE,
		FADE_RISE, FADE_SINK
	}

	//only used from serializer
	public Warp() {
		this.ID = "warp";
	}
	
	public Warp(Scene owner, String next, int warpID, int nextWarp, float x, float y, float w, float h){
		this.ID = "warp";
		this.warpID= warpID;
		this.nextWarp = nextWarp;
		this.x = x;
		this.y = y;
		this.transitionType = TransType.FADE;

		this.next = next;
		this.width = (int) w; 
		this.height = (int) h;
		this.rw = width/2;
		this.rh = height/2;
		this.outside = owner.outside;
		this.offset = new Vector2(0, 0);
		this.warpLoc = new Vector2(x, y-rh);
		this.locTitle = owner.title;
		this.locID = owner.ID;
		loadSprite();
	}
	
	public void setPosition(Vector2 location){
		if(location==null) return;
		x=location.x;
		y=location.y;
	}

	//check if conditions in scene have been met
	//for example, has a certain event been triggered
	public boolean conditionsMet(){
		if(condition!=null)
			return main.evaluator.evaluate(condition);
		return true;
	}
	
	public void setCondition(String condition){
		this.condition = condition;
	}

	public Scene getNextScene(){
		if (next!=null)
			return Scene.levelFromID(next, this, nextWarp);
		return null;
	}
	
//	public void setOwner(Scene owner){ this.owner =owner; }
	public int getLinkID(){ return nextWarp; }
	public void setLinkID(int id){ this.nextWarp = id; }
	public Warp getLink() { return link; }
	public void setLink(Warp link){this.link = link;}
	public Vector2 getWarpLoc(){ return warpLoc.cpy(); }
	public void setOffset(float x, float y){ 
		offset = new Vector2(x, y); 
		warpLoc = new Vector2(this.x+offset.x, this.y+offset.y-rh);
	}
	
	public Vector2 getOffset(){ return offset; }
	public void setInstant(boolean instant){ this.instant = instant; }
	public void setTextTrigger(TextTrigger tt){ this.tt = tt; }
	public TextTrigger getTextTrigger(){ return tt; }

	@Override
	public void create() {
		init = true;
		PolygonShape shape = new PolygonShape();
		shape.setAsBox((rw)/Vars.PPM, (rh)/Vars.PPM);

		bdef.position.set(x/Vars.PPM, y/Vars.PPM);
		bdef.type = BodyType.KinematicBody;
		fdef.shape = shape;
		
		fdef.isSensor = true;
		body = world.createBody(bdef);
		body.setUserData(this);
		fdef.filter.maskBits = (short) ( Vars.BIT_GROUND | Vars.BIT_BATTLE| Vars.BIT_LAYER1| Vars.BIT_PLAYER_LAYER| Vars.BIT_LAYER3);
		fdef.filter.categoryBits = (short) ( Vars.BIT_GROUND | Vars.BIT_BATTLE| Vars.BIT_LAYER1| Vars.BIT_PLAYER_LAYER| Vars.BIT_LAYER3);
		body.createFixture(fdef).setUserData(Vars.trimNumbers(ID));
		
		createCenter();
	}
	
	@Override
	public void read(Json json, JsonValue val) {
		super.read(json, val);
		this.next = val.getString("next");
		this.outside = val.getBoolean("outside");
		this.locTitle = val.getString("locTitle");
		this.locID = val.getString("locID");
		this.warpID = val.getInt("warpID");
		this.instant = val.getBoolean("instant");
		this.transitionType = TransType.valueOf(val.getString("transitionType"));
		try {
			this.condition = val.getString("condition");
		} catch (IllegalArgumentException | NullPointerException e) {
			this.condition = null;
		}
		float warpLocX = val.getFloat("warpLocX");
		float warpLocY = val.getFloat("warpLocY");
		this.warpLoc = new Vector2(warpLocX, warpLocY);
		this.width = val.getInt("warpWidth");
		this.height = val.getInt("warpHeight");
		this.rw = width/2;
		this.rh = height/2;
		float offsetX = val.getFloat("offsetX");
		float offsetY = val.getFloat("offsetY");
		this.offset = new Vector2(offsetX, offsetY);
		this.nextWarp = val.getInt("nextWarp");
	}
	
	@Override
	public void write(Json json) {
		super.write(json);
		json.writeValue("next", this.next);
		json.writeValue("outside", this.outside);
		json.writeValue("locTitle", this.locTitle);
		json.writeValue("locID", this.locID);
		json.writeValue("warpID", this.warpID);
		json.writeValue("instant", this.instant);
		json.writeValue("transitionType", this.transitionType);
		//text trigger is not saved since it is set by Scene on creation
		json.writeValue("condition", this.condition);	//possibly null var
		json.writeValue("warpLocX", this.warpLoc.x);
		json.writeValue("warpLocY", this.warpLoc.y);
		json.writeValue("offsetX", this.offset.x);
		json.writeValue("offsetY", this.offset.y);
		json.writeValue("nextWarp", this.nextWarp);
		//link gets initialized after reading using other field info
		
		//NOTE: we save width and height because no texture exists to initialize this data from Entity
		json.writeValue("warpWidth", this.width);
		json.writeValue("warpHeight", this.height);
	}
	
	public String toString(){
		if(link==null)
		return "Warp: "+locTitle+warpID+"\tlink: "+link+"\tnext: "+next;
		return "Warp: "+locTitle+warpID+"\tlink: "+link.locTitle+link.warpID+"\tnext: "+next;
	}
}
