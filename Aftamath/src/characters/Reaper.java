package characters;

import static handlers.Vars.PPM;
import states.Play;

import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;

import handlers.Vars;

public class Reaper extends SuperNPC {

	protected static int[] actionLengths = {0, 6, 6, 6, 6, 6, 6, 6, 4};

	public Reaper(float x, float y, short layer) {
		super("Grim Reaper", "reaper", 100, 10, x, y, 74, 64, layer);
	}

	public void create() {
		//hitbox
		PolygonShape shape = new PolygonShape();
		shape.setAsBox((width-22)/PPM, (height)/PPM);

		bdef.position.set(x/PPM, y/PPM);
		bdef.type = BodyType.DynamicBody;
		fdef.shape = shape;

		body = world.createBody(bdef);
		body.setUserData(this);
		fdef.filter.maskBits = (short) (layer | Vars.BIT_GROUND | Vars.BIT_PROJECTILE);
		fdef.filter.categoryBits = layer;
		body.createFixture(fdef).setUserData(Vars.trimNumbers(getID()));

		createFootSensor();
		createInteractSensor();
		
		warp();
	}

	public void warp() {
		setAnimation(SPECIAL1, Vars.ACTION_ANIMATION_RATE);
	}

}
