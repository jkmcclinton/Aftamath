package handlers;

import main.Main;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

import entities.Entity;

public class EventTrigger extends Entity{
	
	public float x, y, width, height;
	public boolean triggered, retriggerable;
	
	private Body body;
	private BodyDef bdef = new BodyDef();
	private FixtureDef fdef = new FixtureDef();
	private World world;
	private String script;
	private String condition;
	
	//possibly implement multiple scripts for one event, activate them according to spawnSet perhaps?
	
	public EventTrigger(Main main, float x, float y, float w, float h){
		this.x = x;
		this.y = y;
		this.width = w;
		this.height = h;
		setGameState(main);
		
		ID = "eventTrigger ("+x+", "+y+")";
//		conditions = new HashMap<>();
//		scripts = new HashMap<>();
		
		create();
	}
	
	public void checkEvent(){
//		if (conditionsMet(currentEvent))
//			main.triggerScript(scripts.get(currentEvent));
		if(conditionsMet() && !triggered){
			if(!retriggerable)
				triggered = true;
			main.triggerScript(script);
			main.character.killVelocity();
		}
	}
		
//	public boolean conditionsMet(int event){
//		return main.evaluator.evaluate(conditions.get(event));
//	}
	
	public boolean conditionsMet(){
		return main.evaluator.evaluate(condition);
	}

//	public void addCondition(String event, String condition){
//		if(scripts.containsKey(event))
//			conditions.put(event, condition);
//		else{
//			System.out.println("Event trigger does not have the script \""+event+"\" in its memory.");
//	}
	
//	public void addEvent(int eventIndex, String script){
//		if(!scripts.containsKey(eventName))
//			scripts.put(eventName, script);
//	}
	
	public void setRetriggerable(boolean retrig){ retriggerable = retrig; }
	public void setCondition(String condition){ this.condition = condition; }
	public void setScript(String script){ this.script = script; }
	
	public void create() {
		PolygonShape shape = new PolygonShape();
		shape.setAsBox((width/2-2)/Vars.PPM, (height/2)/Vars.PPM);

		bdef.position.set(x/Vars.PPM, y/Vars.PPM);
		bdef.type = BodyType.KinematicBody;
		fdef.shape = shape;
		
		fdef.isSensor = true;
		if(world==null) world = main.getWorld();
		body = world.createBody(bdef);
		body.setUserData(this);
		fdef.filter.maskBits = (short) ( Vars.BIT_GROUND | Vars.BIT_PROJECTILE| Vars.BIT_LAYER1| Vars.BIT_PLAYER_LAYER| Vars.BIT_LAYER3);
		fdef.filter.categoryBits = (short) ( Vars.BIT_GROUND | Vars.BIT_PROJECTILE| Vars.BIT_LAYER1| Vars.BIT_PLAYER_LAYER| Vars.BIT_LAYER3);
		body.createFixture(fdef).setUserData(Vars.trimNumbers("eventTrigger"));
	}
}
