package entities;

import java.util.HashMap;

import entities.MobAI.ResetType;
import handlers.Vars;
import main.Game;
import main.Main;

public class Spawner extends Entity {
	
	public boolean enabled = true;
	
	private float spawnTime, nighterDelay, spawnDelay, specialDelay;
	private int nighterType, spawnMax = DEFAULT_MAX;
	private String specialType;
	private SpawnType spawnType;
	private HashMap<Entity, Float> spawnedObjs;
	private Path path;
	
	//these determine what properties the NPC is given
	private String dScript, spawnState, aType, dType;
	
	public static final float CIV_DELAY = 30;
	public static final float NIGHTER_DELAY = 40;
	public static final float CIV_LIFETIME = 60;
	public static final float NIGHTER_LIFETIME = 150f;
	public static final int DEFAULT_MAX = 15;
	
	public static enum SpawnType{
		CIVILIAN, SPECIAL, NIGHTER
	}

	public Spawner(float x, float y, String specialType, String spawnType, String spawnState, String aScript, String dScript, 
			String aType, String dType) {
		this(x, y, spawnType, spawnState, aScript, dScript, aType, dType);
		
		if(!Mob.NPCTypes.contains(specialType, false))
			this.spawnType = SpawnType.CIVILIAN;
		else{
			this.specialType = specialType;
			specialDelay = CIV_DELAY;
		}
		
		spawnedObjs = new HashMap<>();	
	}
	
	public Spawner(float x, float y, String spawnType, String spawnState, String aScript, String dScript, 
			String aType, String dType) {
		super(x, y, "spawner");
		setSpawnType(spawnType);
		setAttackScript(aScript);
		this.spawnState = spawnState;
		this.dScript = dScript;
		this.dType = dType;
		this.aType = aType;

		spawnedObjs = new HashMap<>();	
		nighterDelay = NIGHTER_DELAY;
		nighterType = 1;
	}
	
	public void setMax(int max){ this.spawnMax = max; }
	public void setPath(Path path){ this.path = path; }
	
	/**
	 * spawns an object of the current type within current time interval.
	 */
	public void update(float dt){
		updateSpawned(dt);
		if(enabled) attemptSpawn(dt);
		else spawnTime = spawnDelay;
	}
	
	private void attemptSpawn(float dt){
		spawnTime-=dt;
		if(spawnTime>=0){
			if(main.dayTime>=Main.NIGHT_TIME && spawnType==SpawnType.CIVILIAN)
				spawnType = SpawnType.NIGHTER;
			else if(main.dayTime<Main.NIGHT_TIME && spawnType==SpawnType.NIGHTER)
				spawnType = SpawnType.CIVILIAN;
			
			Mob e = null;
			short lyr = Vars.BIT_LAYER1;
			if(Math.random()>.5) lyr = Vars.BIT_LAYER3;
			
			switch(spawnType){
			case CIVILIAN:
				int type = (int) (Math.random()*9) + 1;
				e = new Mob("", "civilian" + type, -1, x, y, lyr);
				e.setState(spawnState, null, -1, ResetType.NEVER.toString());
				e.setDialogueScript(script.ID);
				e.setAttackScript(attackScript.ID);
				e.setDiscoverScript(dScript);
				e.setResponseType(dType);
				e.setAttackType(aType);
				if(path!=null) e.moveToPath(path);
				spawnDelay = CIV_DELAY;
				break;
			case NIGHTER:
				e = new Mob("", "nighter"+nighterType, -1, x, y, lyr);
				e.setState("followplayer", null, -1, ResetType.NEVER.toString());
				e.setDialogueScript("nighter"+nighterType);
//				n.setAttackScript("");
				e.setDiscoverScript("nighterSight"+nighterType);
				e.setResponseType("attack");
				e.setAttackType("on_sight");
				spawnDelay = nighterDelay; 
				break;
			case SPECIAL:
				e = new Mob("", specialType, -1, x, y, lyr);
				e.setState(spawnState, null, -1, ResetType.NEVER.toString());
				e.setDialogueScript(script.ID);
				e.setAttackScript(attackScript.ID);
				e.setDiscoverScript(dScript);
				e.setResponseType(dType);
				e.setAttackType(aType);
				spawnDelay = specialDelay;
				break;
			}
			
			spawnTime = spawnDelay;
			if(e!=null)
				if(spawnedObjs.size()<spawnMax && Game.res.getTexture(e.ID)!=null){
					e.setGameState(main);
					main.addObject(e);
					spawnedObjs.put(e, 0f);
				}
		}
	}
	
	//step lifetimes of spawned entities
	private void updateSpawned(float dt){
		for(Entity e : spawnedObjs.keySet()){
			float life = spawnedObjs.get(e);
			boolean alive = true;
			if(life>=CIV_LIFETIME && spawnType==SpawnType.CIVILIAN)
				alive = false;
			if(life>=NIGHTER_LIFETIME && spawnType==SpawnType.NIGHTER)
				alive = false;
			if(alive)
				spawnedObjs.put(e, life+dt);
			else {
				switch(spawnType){
				case CIVILIAN:
					float dx = main.character.getPixelPosition().x - e.getPixelPosition().x;
					if(Math.abs(dx) >= 10 * Vars.TILE_SIZE){
						main.removeBody(e.body);
						spawnedObjs.remove(e);
					}	
					break;
				case NIGHTER:
				case SPECIAL:
					main.removeBody(e.body);
					spawnedObjs.remove(e);
					break;
				}
			}
		}
	}
	
	public void setNighterType(int i){ 
		if(i>6) i = 6;
		nighterType = i; 
	}
	
	public void setNighterDelay(float t){
		if(spawnType!=SpawnType.CIVILIAN && spawnType!=SpawnType.NIGHTER)
			return;
		nighterDelay = t;
	}
	
	public void setSpecialDelay(float t){ specialDelay = t; } 
	public void setAttackType(String s){ aType = s; }
	public void setResponseType(String s){ dType = s; }
	public void setSpawnState(String s){ spawnState = s; }
	public void setSpawnType(String s){
		try{
			spawnType = SpawnType.valueOf(s);
		} catch (Exception e){
			spawnType = SpawnType.CIVILIAN;
		}
	}
}
