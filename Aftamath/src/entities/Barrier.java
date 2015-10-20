package entities;

import static handlers.Vars.PPM;
import handlers.Vars;

import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.utils.Array;

public class Barrier extends Entity {

	public Barrier(float x, float y, int width, int height, String ID) {
		this.ID = "barrier"+ID;
		this.x = x;
		this.y = y;
		isAttackable = false;
		
		setDimensions(width, height);
		loadSprite();
		followers = new Array<>();
	}
	
	public void create(){
		init = true;
		//hitbox
		PolygonShape shape = new PolygonShape();
		shape.setAsBox((rw)/PPM, (rh)/PPM);
		
		bdef.position.set(x/PPM, y/PPM);
		bdef.type = BodyType.StaticBody;
		fdef.shape = shape;
		
		fdef.friction = .25f;
		fdef.filter.categoryBits = Vars.BIT_GROUND;
		fdef.filter.maskBits = Vars.BIT_LAYER1 | Vars.BIT_PLAYER_LAYER | Vars.BIT_LAYER3 | Vars.BIT_PROJECTILE;
		fdef.isSensor = false;
		body = world.createBody(bdef);
		body.createFixture(fdef).setUserData("barrier");
		body.setUserData(this);
	}
}
