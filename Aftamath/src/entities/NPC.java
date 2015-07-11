package entities;

import static handlers.Vars.PPM;
import handlers.Vars;
import scenes.Script;
import scenes.Script.ScriptType;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class NPC extends Mob {

	public boolean returnToPrevLoc, prevDirection, triggered;

	protected Vector2 goalPosition, prevLocation;
	protected float idleWait, idleTime, doTime, doDelay, time;
	protected boolean reached, locked, attacked;
	protected Entity focus;
	protected Script discoverScript;
	protected AttackType attackType;
	protected SightResponse responseType;
	protected AIState state, defaultState;
	
	public static final Array<String> NPCTypes = new Array<>();
	static{
		FileHandle[] handles = Gdx.files.internal("res/images/enitities/mobs").list();
		
		for(FileHandle f:handles){
			if(f.extension().equals("png") && !f.nameWithoutExtension().contains("base"))
				NPCTypes.add(f.nameWithoutExtension());
		}
	}

	//determines when the NPC fights back
	public static enum AttackType{
		ON_SIGHT, ON_ATTACKED, ON_DEFEND, /*ON_EVENT,*/ RANDOM, NEVER
	}

	//determines what the NPC does when the character is spotted
	public static enum SightResponse{
		FOLLOW, ATTACK, TALK, EVADE, IGNORE
	}

	public static enum AIState{
		STATIONARY, IDLEWALK, FOLLOWING, FACEPLAYER, FACEOBJECT, ATTACKING, FIGHTING, 
		EVADING, EVADING_ALL, DANCING, BLOCKPATH, TIMEDFIGHT
	}

	protected static final int IDLE_LIMIT = 500;

	public NPC(){
		super(null, "", 0, 0, Vars.BIT_LAYER3);
		gender = "n/a";
		
		state = AIState.STATIONARY;
		defaultState = AIState.STATIONARY;
		attackType = AttackType.NEVER;
		responseType = SightResponse.IGNORE;

	}

	public NPC(String name, String ID, int sceneID, float x, float y, short layer){
		super(name, ID, x, y, layer);
		health = maxHealth = DEFAULT_MAX_HEALTH;
		determineGender();
		
		state = AIState.STATIONARY;
		defaultState = AIState.STATIONARY;
		attackType = AttackType.NEVER;
		responseType = SightResponse.IGNORE;

		this.sceneID = sceneID;
		goalPosition = new Vector2((float) (((Math.random() * 21)+x)/PPM), y);
		idleWait = (float)(Math.random() *(IDLE_LIMIT)+100);
		time = 0;
	}

	public void setState(String state){
		try{
			AIState s = AIState.valueOf(state.toUpperCase());
			setState(s);
		} catch(Exception e ){}
	}

	public void setState(AIState state){ 
		if (state == AIState.FOLLOWING) follow(character);
		else this.state = state;
	}

	public void setState(AIState state, Entity focus){ 
		if (state == AIState.FOLLOWING) follow(focus);
		else this.state = state;

		this.focus = focus;
	}

	public void setDefaultState(String state){
		try{
			AIState s = AIState.valueOf(state.toUpperCase());
			setDefaultState(s);
		} catch(Exception e){}
	}
	
	public void facePlayer(){
		setState(AIState.FACEPLAYER);
	}
	
	public void faceObject(Entity e){
		focus = e;
		setState(AIState.FACEOBJECT);
	}
	
	public void setDefaultState(AIState state){
		if (state==this.state) return;
		if(state==AIState.BLOCKPATH && this.state!=AIState.BLOCKPATH){
			changeLayer(Vars.BIT_PLAYER_LAYER);
				mdat.mass = 10;
			if(body!=null){
				body.setMassData(mdat);
			}
		} else if(this.state==AIState.BLOCKPATH && state!=AIState.BLOCKPATH){
			mdat.mass = 1;
			changeLayer(origLayer);
			body.setMassData(mdat);
		}
		
		if(state!=AIState.ATTACKING && state!=AIState.TIMEDFIGHT)
			this.state = defaultState = state;
		else defaultState = AIState.STATIONARY;
	}

	public void resetState(){ state = defaultState; }
	public AIState getState(){ return state; }
	public AttackType getAttackType(){ return attackType; }
	public SightResponse getResponseType(){ return responseType; }
	public void setResponseType(String type){ 
		try {
			responseType = SightResponse.valueOf(type.toUpperCase());
		} catch(Exception e){
			responseType = SightResponse.IGNORE;
		}
	}

	public void setAttackType(String type){ 
		try {
			attackType = AttackType.valueOf(type.toUpperCase());
		} catch(Exception e){
			attackType = AttackType.NEVER;
		}
	}

	public void setDiscoverScript(String ID){ 
		if(ID==null)return;
		discoverScript = new Script(ID, ScriptType.DISCOVER, main, this);
	}

	//possibly crunch this down
	public void act(){
		float dx, dy;
//		if(sceneID==1)
//			System.out.println(ID+":"+state);
		
		if(discovered.contains(main.character, true)&&!triggered){
			if(discoverScript!=null){
				triggered = true;
				main.triggerScript(discoverScript);
			} else {
				switch(responseType){
				case ATTACK:
					fight(main.character);
					break;
				case EVADE:
					evade(main.character);
					break;
				case FOLLOW:
					follow(main.character);
					break;
				case TALK:
					//only talk if talking is necessary?
					break;
				default:
					break;
				}
			}
		} else
			doTime+=Vars.DT;
			switch (state){
			//walk to random locations within a radius, then wait a random amount of time
			case IDLEWALK: 
				if(reached) idleTime++;
				if(idleTime >= idleWait && reached) reached = false;
				else if (!reached){
					if (!canMove()) {
						goalPosition = new Vector2((float) (((Math.random() * 6)+x)/PPM), y);
						idleWait = (float)(Math.random() *(IDLE_LIMIT) + 100);
						idleTime = 0;
						reached = true;
					}
					else {
						dx = (goalPosition.x - body.getPosition().x) * PPM ;
						if(dx < 1 && dx > -1){
							goalPosition = new Vector2((float) (((Math.random() * 6)+x)/PPM), y);
							idleWait = (float)(Math.random() *(IDLE_LIMIT) + 100);
							idleTime = 0;
							reached = true;
						} else {
							if(dx < 1) left();
							if(dx > -1) right();
						}
					}
				}
				break;
			case FACEPLAYER:
				facePlayer();
				break;
			case FACEOBJECT:
				faceObject(focus);
				break;
				//stay close behind the target
			case FOLLOWING:
				facePlayer();
				dx = character.getPosition().x - body.getPosition().x;

				float m = MAX_DISTANCE * character.getFollowerIndex(this);

				if(dx > m/PPM) right();
				else if (dx < -1 * m/PPM) left();
				break;
				//attack focus once, then return to previous action
			case ATTACKING: 
				if(!attacked){
					if(attackFocus!=null){
						dx = attackFocus.getPosition().x - getPosition().x;
						dy = attackFocus.getPosition().x - getPosition().x;
						float d = (float) Math.sqrt(dx*dx + dy+dy);

						if(Math.abs(d)>attackRange){
							if(dx > 1) right();
							if(dx < -1) left();
						} else{
							attack();
							attacked = true;
						}
					} else {
						attack();
						attacked = true;
						state = defaultState;
					}
				}

				break;
			case FIGHTING:
				fightAI();
				break;

				// evade target if target is spotted
				// if target not spotted, search vicinity
			case EVADING_ALL:
			case EVADING:
				if(!reached){
					dx = (goalPosition.x - body.getPosition().x) * PPM ;
					if(dx < 1 && dx > -1){
						reached = true;
					} else {
						if(dx < 1) left();
						if(dx > -1) right();
					}
				} else {
					boolean found = false; dx =0;

					if(state == AIState.EVADING_ALL){
						if(discovered.size>0){
							dx = (discovered.get(0).getPosition().x - getPosition().x) * PPM;
							found = true;
						}
					} else if(state == AIState.EVADING)
						if(discovered.contains(focus, true)){
							dx = (focus.getPosition().x - getPosition().x) * PPM;
						}

					if(found)
						if(Math.abs(dx) <= visionRange){
							float gx = -1*(dx/Math.abs(dx))*(visionRange*3)/PPM + getPosition().x;
							goalPosition = new Vector2(gx, y);
							reached = false;
						}
						else {
							//idle
						}
				}
				break;
				// stand around and look in random directions
			case STATIONARY:
				if(doTime >= doDelay){
					doTime = 0;
					doDelay = (float)(Math.random()*6) + 1;
					
					changeDirection();
				}
				break;
			case BLOCKPATH:
				facePlayer();
				break;
			case DANCING:
				if(getAnimationAction()!=Action.DANCE)
					setAnimation(Action.DANCE, true);
				break;
			case TIMEDFIGHT:
				time-=Vars.DT;
				if(time>=0)
					fightAI();
				else
					state = defaultState;
				break;
			default:
				break;
			}
	}

	// attack focus if in range
	// if not in range, get close to object
	// otherwise evade hostile mobs or projectiles
	// if no hostile mobs or projectiles in sight, search vicinity
	private void fightAI(){
		float dx, dy;
		if(!attacked){
			if(attackFocus!=null && doTime>=doDelay){
				dx = attackFocus.getPosition().x - getPosition().x;
				dy = attackFocus.getPosition().x - getPosition().x;
				float d = (float) Math.sqrt(dx*dx + dy+dy);

				if(Math.abs(d)>attackRange){
					if(dx-1>0) right();
					if(dx+1<0) left();
				} else {
					attack();
					attacked = true;
					doDelay = (float) (Math.random()*3);
					doTime = 0;
				}
			}
		} else {
			if(reached) idleWait++;
			if(idleTime >= idleWait && reached) {
				reached = false;
				attacked = false;
			}
			if(!reached){
				if (!canMove()) {
					goalPosition = new Vector2((float) (((Math.random() * 6)+x)/PPM), y);
					idleWait = (float)(Math.random() *(attackRange) + 100);
					idleTime = 0;
					reached = true;
				}
				else {
					dx = (goalPosition.x - body.getPosition().x) * PPM ;
					if(dx < 1 && dx > -1){
						goalPosition = new Vector2((float) (((Math.random() * 6)+x)/PPM), y);
						idleWait = (float)(Math.random() *(attackRange) + 100);
						idleTime = 0;
						reached = true;
					} else {
						if(dx < 1) left();
						if(dx > -1) right();
					}
				}
			}
		}
	}

	public void evade(){
		state = AIState.EVADING_ALL;
	}

	public void evade(Entity focus){
		state = AIState.EVADING;
		this.focus = focus;
	}

	public void follow(Entity focus){
		state = AIState.FOLLOWING;
		focus.addFollower(this);
	}

	public void stay(){
		state = AIState.FACEPLAYER;
		character.removeFollower(this);
	}

	public void fight(Entity d){
		attackFocus = d;
		state = AIState.FIGHTING;
		doTime= (float) (Math.random()*3);
		attacked = false;
		reached = false;
	}
	
	public void timedFight(Entity d, float time){
		attackFocus = d;
		state = AIState.TIMEDFIGHT;
		doTime= (float) (Math.random()*3);
		attacked = false;
		reached = false;
		this.time = time;
	}

	public void lock() { locked = true; }
	public void unlock() { locked = false; }

	private void determineGender(){
		if (Vars.MALES.contains(ID, false))
			gender = MALE;
		else gender = FEMALE;
	}

	public NPC copy(){
		NPC n = new NPC(name, ID, sceneID, 0, 0, layer);

		n.resetHealth(health, maxHealth);
		n.setDefaultState(defaultState);

		if(script!=null)
			n.setAttackScript(script.ID);
		if(discoverScript!=null)
			n.setAttackScript(discoverScript.ID);
		if(attackScript!=null)
			n.setAttackScript(attackScript.ID);

		return n;
	}
}
