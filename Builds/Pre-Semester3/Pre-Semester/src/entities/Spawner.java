package entities;

import handlers.Vars;
import main.Main;

public class Spawner extends Entity {
	
	private float spawnTime, nighterDelay, spawnDelay, specialDelay;
	private int nighterType;
	private String specialType;
	private SpawnType spawnType;
//	private Pathing pathing;
	
	//these determine what properties the NPC is given
	private String dScript, spawnState, aType, dType;
	
	public static final float CIV_DELAY = 30;
	public static final float NIGHTER_DELAY = 40;
	
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
		
		nighterDelay = NIGHTER_DELAY;
		nighterType = 1;
	}
	
	/**
	 * spawns an object of the current type within current time interval.
	 */
	public void update(float dt){
		spawnTime-=dt;
		if(spawnTime>=0){
			if(main.dayTime>=Main.NIGHT_TIME && spawnType==SpawnType.CIVILIAN)
				spawnType = SpawnType.NIGHTER;
			else if(main.dayTime<Main.NIGHT_TIME && spawnType==SpawnType.NIGHTER)
				spawnType = SpawnType.CIVILIAN;
			
			Mob m = null;
			short lyr = Vars.BIT_LAYER1;
			if(Math.random()>.5) lyr = Vars.BIT_LAYER3;
			
			switch(spawnType){
			case CIVILIAN:
				int type = (int) (Math.random()*9) + 1;
				
				m = new Mob("", "civilian" + type, -1, x, y, lyr);
				m.setDefaultState(spawnState);
				m.setDialogueScript(script.ID);
				m.setAttackScript(attackScript.ID);
				m.setDiscoverScript(dScript);
				m.setResponseType(dType);
				m.setAttackType(aType);
				
				spawnDelay = CIV_DELAY;
				break;
			case NIGHTER:
				m = new Mob("", "nighter"+nighterType, -1, x, y, lyr);
				m.setDefaultState("followplayer");
				m.setDialogueScript("nighter"+nighterType);
//				n.setAttackScript("");
				m.setDiscoverScript("nighterSight"+nighterType);
				m.setResponseType("attack");
				m.setAttackType("on_sight");
				
				spawnDelay = nighterDelay; 
				break;
			case SPECIAL:
				m = new Mob("", specialType, -1, x, y, lyr);
				m.setDefaultState(spawnState);
				m.setDialogueScript(script.ID);
				m.setAttackScript(attackScript.ID);
				m.setDiscoverScript(dScript);
				m.setResponseType(dType);
				m.setAttackType(aType);
				
				spawnDelay = specialDelay;
				break;
			}
			
			spawnTime = spawnDelay;
			if(m!=null){
				m.setGameState(main);
				main.addObject(m);
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
