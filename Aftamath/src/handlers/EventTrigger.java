package handlers;

import java.util.HashMap;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

import entities.Entity;
import main.Main;

public class EventTrigger extends Entity{
	
	public float x, y, width, height;
//	public boolean triggered, retriggerable, halt = true;
	
	private Body body;
	private BodyDef bdef = new BodyDef();
	private FixtureDef fdef = new FixtureDef();
	private World world;
	private HashMap<String, Pair<String, Boolean>> scripts; //scriptID, <condition, triggered>
	private HashMap<String, Boolean> retriggerables, haltables;
//	private String condition;
	
	//possibly implement multiple scripts for one event, activate them according to spawnSet perhaps?
	
	public EventTrigger(Main main, float x, float y, float w, float h){
		this.x = x;
		this.y = y + h/2f;
		this.width = w;
		this.height = h;
		setGameState(main);
		
		ID = "eventTrigger ("+x+", "+y+")";
		scripts = new HashMap<>();
		retriggerables = new HashMap<>();
		haltables = new HashMap<>();
		
		create();
	}
	
	public void checkEvent(){
//		for(String script : haltables.keySet())
//			System.out.println(script+": "+haltables.get(script));
		
		for(String script : scripts.keySet())
			if(!triggered(script)){
//				System.out.println("CondMet: \""+script+"\"\t"+
//			main.evaluator.evaluate(scripts.get(script).getKey())+"\ttrig: "+scripts.get(script).getValue());
				if(conditionsMet(script)){
					main.triggerScript(script, this);
					if(haltable(script)) main.character.killVelocity();
					if(!retrig(script)) scripts.get(script).setValue(true);
					break;
				}
			}
	}
	
	private boolean triggered(String key){ return scripts.get(key).getValue(); }
	private boolean retrig(String key){ return retriggerables.get(key); }
	private boolean haltable(String key){ return haltables.get(key); }
	
	public boolean conditionsMet(String key){
		Pair<String, Boolean> pair;
		if((pair = scripts.get(key))!=null){
			if(!pair.getKey().isEmpty())
				return main.evaluator.evaluate(pair.getKey());
			else 
				return true;
		} else
			return true;
	}
	
	public void addEvent(String script, String condition){
		scripts.put(script, new Pair<String, Boolean>(condition, false));
		haltables.put(script, true);
		retriggerables.put(script, false);
	}
	
	public void setRetriggerable(String key, boolean retrig){ retriggerables.put(key, retrig); }
	public void setRetriggerable(boolean retrig){
		for(String s: retriggerables.keySet())
			retriggerables.put(s, retrig);
	}
	
	public void setCondition(String key, String condition){ scripts.get(script).setKey(condition); }
	public boolean getHalt(String key){ return haltables.get(key); }
	public void setHalt(String key, boolean retrig){ haltables.put(key, retrig); }
	public void setHalt(boolean retrig){
		for(String s: haltables.keySet())
			haltables.put(s, retrig);
	}
	
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
		fdef.filter.maskBits = (short) ( Vars.BIT_GROUND | Vars.BIT_BATTLE| Vars.BIT_LAYER1| Vars.BIT_PLAYER_LAYER| Vars.BIT_LAYER3);
		fdef.filter.categoryBits = (short) ( Vars.BIT_GROUND | Vars.BIT_BATTLE| Vars.BIT_LAYER1| Vars.BIT_PLAYER_LAYER| Vars.BIT_LAYER3);
		body.createFixture(fdef).setUserData(Vars.trimNumbers("eventTrigger"));
	}
}
