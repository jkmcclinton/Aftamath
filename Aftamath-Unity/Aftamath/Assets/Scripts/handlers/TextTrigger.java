package handlers;

import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.PolygonShape;

import entities.Entity;
import entities.SpeechBubble.PositionType;

//simple class that spawns a speechbubble overhead when collided with
public class TextTrigger extends Entity {

	public String message;
	public PositionType positioning;
	public static final float DEFAULT_WIDTH = 2*Vars.TILE_SIZE;
	public static final float DEFAULT_HEIGHT = 4.5f*Vars.TILE_SIZE;
	
	public TextTrigger(float x, float y, String message, PositionType positioning){
		this.message = message;
		this.positioning = positioning;
		this.width = (int) DEFAULT_WIDTH;
		this.height = (int) DEFAULT_HEIGHT;
		this.rw = width/2;
		this.rh = height/2;
		this.x = x;
		this.y = y;
		this.ID = "texttrigger";
		loadSprite();
	}
	
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
	}
}
