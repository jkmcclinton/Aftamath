package entities;

import handlers.Vars;
import scenes.Scene;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.PolygonShape;

public class Warp extends Entity {

	public String next = null;
	public Scene owner;
	public int warpID;
	public boolean instant;
	
	private String condition;
	private Vector2 warpLocation;
	private int nextWarp;

	public Warp(Scene owner, String next, int warpID, int nextWarp, float x, float y, float w, float h){
		this.ID = "warp";
		this.warpID= warpID;
		this.nextWarp = nextWarp;
		this.x = x;
		this.y = y;

		this.next = next;
		this.width = (int) w; 
		this.height = (int) h;
		this.rw = width/2;
		this.rh = height/2;
		this.owner = owner;
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

	public Scene getNext(){
		if (next!=null)
			return owner.levelFromID(next, this, nextWarp);
		return null;
	}

	public Vector2 getLink() { return warpLocation; }
	public void setLink(Vector2 link){ warpLocation = link.cpy(); }
	public Vector2 getPosition(){ return new Vector2(x/Vars.PPM, y/Vars.PPM); }
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
}
