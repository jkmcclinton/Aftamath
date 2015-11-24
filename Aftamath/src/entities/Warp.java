package entities;

import handlers.Vars;
import scenes.Scene;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.PolygonShape;

public class Warp extends Entity {

	public String next = null;
	public Scene owner;
	public String locTitle;
	public int warpID;
	public boolean instant;
	public TransType transitionType;
	
	private String condition;
	private Vector2 warpLoc, offset;
	private int nextWarp;
	private Warp link;
	
	public enum TransType {
		HORIZONTAL_BARS, PINHOLE, ZOOM, BLACKOUT, FADE, FADE_WHITE,
		FADE_RISE, FADE_SINK
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
		this.owner = owner;
		this.offset = new Vector2(0, 0);
		this.warpLoc = new Vector2(x, y-rh);
		this.locTitle = owner.title;
		loadSprite();
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
			return owner.levelFromID(next, this, nextWarp);
		return null;
	}
	
	public void setOwner(Scene owner){ this.owner =owner; }
	public int getLinkID(){ return nextWarp; }
	public Warp getLink() { return link; }
	public void setLink(Warp link){this.link = link; }
	public Vector2 getWarpLoc(){ return warpLoc.cpy(); }
	public void setOffset(float x, float y){ 
		offset = new Vector2(x, y); 
		warpLoc = new Vector2(this.x+offset.x, this.y+offset.y-rh);
	}
	
	public Vector2 getOffset(){ return offset; }
	public void setInstant(boolean instant){ this.instant = instant; }

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
	
	public String toString(){
		return locTitle+warpID;
	}
}
