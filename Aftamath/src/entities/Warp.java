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
	
	public boolean conditionsMet(){
		//check if conditions in scene have been met
		//for example, has a certain event been triggered
		
		return true;
	}

	public Scene getNext(){
		if (next!=null)
			return owner.levelFromID(next, this, nextWarp);
		return null;
	}
	
	public Vector2 getPosition(){ return new Vector2(x/Vars.PPM, y/Vars.PPM); }

	@Override
	public void create() {
		PolygonShape shape = new PolygonShape();
		shape.setAsBox((rw-2)/Vars.PPM, (rh)/Vars.PPM);

		bdef.position.set(x/Vars.PPM, y/Vars.PPM);
		bdef.type = BodyType.KinematicBody;
		fdef.shape = shape;
		
		fdef.isSensor = true;
		body = world.createBody(bdef);
		body.setUserData(this);
		fdef.filter.maskBits = (short) ( Vars.BIT_GROUND | Vars.BIT_PROJECTILE| Vars.BIT_LAYER1| Vars.BIT_LAYER2| Vars.BIT_LAYER3);
		fdef.filter.categoryBits = (short) ( Vars.BIT_GROUND | Vars.BIT_PROJECTILE| Vars.BIT_LAYER1| Vars.BIT_LAYER2| Vars.BIT_LAYER3);
		body.createFixture(fdef).setUserData(Vars.trimNumbers(getID()));
	}

	public Vector2 getLink() { return warpLocation; }
	public void setLink(Vector2 link){ warpLocation = new Vector2(link.x*Vars.PPM, link.y*Vars.PPM); }
}
