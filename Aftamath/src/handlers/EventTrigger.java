package handlers;

import main.Main;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

import entities.Entity;

public class EventTrigger extends Entity{
	
	public float x, y, width, height;
	public boolean triggered;
	
	@SuppressWarnings("unused")
	private String condition, value;
	private Body body;
	private BodyDef bdef = new BodyDef();
	private FixtureDef fdef = new FixtureDef();
	private World world;
	
	public EventTrigger(World world, Main play, float x, float y, float w, float h, String condition, String value){
		this.x = x;
		this.y = y;
		this.width = w;
		this.height = h;
		this.condition = condition;
		this.value = value;
		this.world = world;
		this.main = play;
		
		create();
	}
	
	public void checkEvent(){
		if (condition.equals("")){
			
		} else if (condition.equals("")){
			
		}
	}

	public void create() {
		PolygonShape shape = new PolygonShape();
		shape.setAsBox((width/2-2)/Vars.PPM, (height/2)/Vars.PPM);

		bdef.position.set(x/Vars.PPM, y/Vars.PPM);
		bdef.type = BodyType.KinematicBody;
		fdef.shape = shape;
		
		fdef.isSensor = true;
		body = world.createBody(bdef);
		body.setUserData(this);
		fdef.filter.maskBits = (short) ( Vars.BIT_GROUND | Vars.BIT_PROJECTILE| Vars.BIT_LAYER1| Vars.BIT_PLAYER_LAYER| Vars.BIT_LAYER3);
		fdef.filter.categoryBits = (short) ( Vars.BIT_GROUND | Vars.BIT_PROJECTILE| Vars.BIT_LAYER1| Vars.BIT_PLAYER_LAYER| Vars.BIT_LAYER3);
		body.createFixture(fdef).setUserData(Vars.trimNumbers("eventTrigger"));
	}

}
