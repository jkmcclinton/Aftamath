package entities;

import static handlers.Vars.PPM;

import java.util.HashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.SerializationException;

import entities.Projectile.ProjectileType;
import handlers.FadingSpriteBatch;
import handlers.JsonSerializer;
import handlers.Vars;
import main.Game;
import main.Main;
import main.Main.InputState;
import scenes.Script;
import scenes.Script.ScriptType;

public class Mob extends Entity {

	public Array<Entity> contacts;
	public double strength = DEFAULT_STRENGTH;
	public float attackRange = DEFAULT_ATTACK_RANGE;
	public Sound voice;
	public ScriptAI controlledAction;
	public boolean canWarp, canClimb, wasOnGround, running;
	public boolean climbing, falling, snoozing, knockedOut;
	public float experience;

	//used for adjusting the distance between the character
	//and the object currently being interacted with
	public boolean positioning;
	private Entity positioningFocus;
	
	protected int level;
	protected DamageType powerType;
	protected IFFTag iff;
	protected Vector2 respawnPoint;
	protected String groundType, gender, name;
	protected Anim action;
	protected float knockOutTime, idleTime, idleDelay;
	protected float stepTime, actionTime, attackTime, attackDelay=DEFAULT_ATTACK_DELAY;
	protected float maxSpeed = WALK_SPEED, fallTime;
	protected boolean controlledPT2, aiming, ctrlReached;
	protected int ctrlRepeat = -1, timesIdled;
	protected TextureRegion[] face, healthBar;
	protected Warp warp;
	private Path path;
	protected Array<Entity> attackables, discovered;
	protected Entity attackFocus, AIfocus;
	protected float visionRange = DEFAULT_VISION_RANGE;
	protected Entity interactable;
	protected static final int IDLE_LIMIT = 500;
	
	private static Array<Anim> immobileActions = new Array<>(); 

	//determines when the NPC fights back
	public static enum AttackType{
		ON_SIGHT, ENGAGE, HIT_ONCE, /*ON_EVENT,*/ RANDOM, NEVER
	}

	//determines what the NPC does when the character is spotted
	public static enum SightResponse{
		FOLLOW, ATTACK, TALK, EVADE, IGNORE
	}

	//determines what 
	public static enum AIState{
		STATIONARY, IDLEWALK, FOLLOWING, FACEPLAYER, FACEOBJECT, ATTACKING, FIGHTING, 
		EVADING, EVADING_ALL, DANCING, BLOCKPATH, TIMEDFIGHT, PATH, PATH_PAUSE
	}
	
	static{
	 Anim[] tmp = {Anim.DEAD, Anim.DIE_TRANS, Anim.FLINCHING, Anim.GET_DOWN, Anim.SNOOZING, Anim.WAKE_UP, 
			Anim.SIT_DOWN, Anim.SITTING, Anim.AIMING, Anim.TURN_RUN, Anim.LIE_DOWN, 
			Anim.SLEEPING, Anim.STUMBLE, Anim.KNOCKED_OUT, Anim.RECOVER};
		immobileActions.addAll(tmp);
	}
	
	public static enum IFFTag {
		FRIENDLY, NEUTRAL, HOSTILE
	}

	//emotion indicies
	public static final int NORMAL = 0;
	public static final int HAPPY = 1;
	public static final int SAD = 2;
	public static final int MAD = 3;	
	public static final int FLIRTY = 4;

	//characteristic constants
	public static final String MALE = "male";
	public static final String FEMALE = "female";
	protected static final float WALK_SPEED = .75f;
	protected static final float RUN_SPEED = .75f * 2.75f;
	protected static final float MOVE_DELAY = Vars.ANIMATION_RATE * 2;
	protected static final int DEFAULT_WIDTH = 20;
	protected static final int DEFAULT_HEIGHT = 50;
	protected static final float DEFAULT_ATTACK_DELAY = .5f;
	protected static final float DEFAULT_ATTACK_RANGE = 20;
	protected static final float DEFAULT_VISION_RANGE = 10*Vars.TILE_SIZE;
	protected static final double DEFAULT_STRENGTH = 1;
	protected static final double DAMAGE_THRESHOLD = 4;
	protected static final int INTERACTION_SPACE = 17;

	//used solely for controlling animation
	public static enum Anim{
		STANDING,
		WALKING,
		RUNNING,
		JUMPING,
		FALLING_TRANS,
		FALLING,
		LANDING,
		DUCK,
		DUCKING,
		LOOK_UP,
		LOOKING_UP,
		SIT_DOWN,
		SITTING,
		IDLE,
		GET_DOWN,
		SNOOZING,			//for idle timeout
		WAKE_UP,
		LIE_DOWN,
		SLEEPING,
		DANCE,
		TURN,
		TURN_RUN,
		ON_FIRE,
		FLINCHING,
		STUMBLE,
		KNOCKED_OUT,
		RECOVER,
		AIM_TRANS,
		AIMING,
		ATTACKING,
		PUNCHING,
		DIE_TRANS,
		DEAD,
		EMBRACE,
		HUGGING,
		ENGAGE_KISS,
		KISSING,
		FLOATING,
		SWIMMING,
		TURN_SWIM,
		SPECIAL1,
		SPECIAL2,
		SPECIAL3,
		SPECIAL3_TRANS ,
	}
	
	//used for making the mob do stuff when triggered by scripts
	public static enum ScriptAI{
		MOVE, RUN, JUMP, FLY, SLEEP, FLAIL, IDLE, AIM, LOSEAIM, PUNCH, ATTACK, HUG, 
		KISS, SNOOZE, DANCE, SPECIAL1, SPECIAL2, SPECIAL3, STOP
	}
	
	public static HashMap<Anim, Integer> animationIndicies = new HashMap<>();
	static{
		for(int i = 0; i<Anim.values().length; i++){
			animationIndicies.put(Anim.values()[i], i);
//			System.out.println(Action.values()[i]);
		}
	}
	
	//Instantiate NPC mobs
	public Mob(String name, String ID, int sceneID, float x, float y, short layer){
		this(name, ID, sceneID, 0, DamageType.PHYSICAL, x, y, layer);
	}
	
	public Mob(String name, String ID, int sceneID, Vector2 location, short layer) {
		this(name, ID, sceneID, 0, DamageType.PHYSICAL, location.x, location.y, layer);
	}

	public Mob(String name, String ID, int sceneID, int level, DamageType type, float x, float y, short layer){
		super(x, y+getHeight(ID)/2f, ID);
		respawnPoint = new Vector2(x,y);
		this.init();
		
		this.name = name;
		this.layer = layer;
		origLayer = layer;

		determineGender();
		this.powerType = type;

		Texture texture = Game.res.getTexture(ID + "face");
		if (texture != null) face = TextureRegion.split(texture, 64, 64)[0];
		texture = Game.res.getTexture("healthBar");
		if (texture != null) healthBar = TextureRegion.split(texture, 12, 1)[0];
		
		determineGender();
		idleDelay = 3*Vars.ANIMATION_RATE*animation.getDefaultLength();
		
		this.sceneID = sceneID;
		if (!Entity.idToEntity.containsKey(this.sceneID)) {
			Entity.idToEntity.put(this.sceneID, this);
		} else {
			Entity e = Entity.idToEntity.get(sceneID);
			boolean conflict = true;
			String error = e.ID;
			
			if(e instanceof Mob)
				if(this.getName().equals(((Mob) e).getName()) && ID.equals(e.ID)){
					conflict = false;
					Entity.idToEntity.put(sceneID, this);
				} else
					error = "("+((Mob)e).getName() + ", "+e.ID+")";
			
			if(conflict)
				System.out.println("Created mob with ID "+this.sceneID+" ("+ this.name +", "+ID+") when " +
						error + " already exists");
		}
	}

	//constructor called from serializer - use others in other cases
	public Mob(){
		//this(null, "", 0, 0, 0, Vars.BIT_LAYER3);
		super();
		this.init();
		gender = "n/a";	
	}

	private void init() {
		attackables = new Array<>();
		discovered = new Array<>();
		contacts = new Array<>();
		flamable = true;
		isAttackable = true;
		attackTime = attackDelay;
		iff=IFFTag.FRIENDLY;
		health = maxHealth = DEFAULT_MAX_HEALTH;
		action = Anim.STANDING;
		state = AIState.STATIONARY;
		defaultState = AIState.STATIONARY;
		attackType = AttackType.NEVER;
		responseType = SightResponse.IGNORE;
		goalPosition = new Vector2((float) (((Math.random() * 21)+x)/PPM), y);
		inactiveWait = (float)(Math.random() *(IDLE_LIMIT)+100);
		time = 0;
	}
	
	public void update(float dt){
		if(main==null){
			System.out.println("still BROKEN");
			return;
		}
		attackTime+=dt;
		
		if(frozen)
			super.update(dt);
		else{
			
			if(controlled){
				if(actionTime>0)
					doTimedAction();
				else
					doAction();
			} else if (!equals(main.character)){
				if (isOnGround() && !locked && !controlled && state!=null) 
					act();
			}

			// idle player actions
			if (action == Anim.STANDING && main.currentScript==null) {
				animation.removeAction();
				idleTime+=dt;
				if(idleTime>=idleDelay){
					timesIdled +=1;
					idleTime = 0;
					if(timesIdled >=4 && main.character.equals(this) && 
							(main.stateType==InputState.MOVE || main.stateType==InputState.MOVELISTEN)){
						setTransAnimation(Anim.GET_DOWN, Anim.SNOOZING, true);
						timesIdled = 0;
						snoozing = true;
//						controlledAction = ScriptAI.SNOOZE;
					}
					else{
						if(this.equals(main.character))
							if(Math.random()>.25d)
								setAnimation(Anim.IDLE);
							else
								setAnimation(Anim.IDLE);
						else
							setAnimation(Anim.IDLE);
					}
				}
			} else
				idleTime = 0;

			if(knockedOut){
				knockOutTime+=dt;
				if(knockOutTime>=3f)
					setAnimation(Anim.RECOVER);
				else if(action!=Anim.RECOVER && action!=Anim.STUMBLE)
					setAnimation(Anim.KNOCKED_OUT);
			}else if(knockedOut && action==Anim.RECOVER)
				if(animationIndicies.get(Anim.RECOVER)<=actionLengths.length)
					if(animation.getIndex()==actionLengths[animationIndicies.get(Anim.RECOVER)]-1){
						knockedOut = controlled = false;
					}

			//make player sleep
//			if(snoozing && action!=Action.GET_DOWN)
//				setAnimation(Action.SNOOZING, true);

			if (!isOnGround()){
				setAnimation(Anim.FALLING);
				fallTime+=dt;
			}if(isOnGround() && !wasOnGround && (getAnimationAction()==Anim.FALLING||
					getAnimationAction()==Anim.FALLING_TRANS)){
				setAnimation(Anim.LANDING);
				
				if(this.equals(main.character)){
					float a = .4f, b = 2, t = fallTime;
					float maxAmp = (float) (b*t / (t + Math.exp(1 - a*t)));
					main.getCam().shake(maxAmp);
				}
				
				fallTime=0;
			}

			if (!climbing)
				animation.update(dt);
			else
				if(body.getLinearVelocity().y>0)
					animation.update(dt);

			if (invulnerable && invulnerableTime > 0) 
				invulnerableTime-= dt;
			else if (invulnerable && invulnerableTime <= 0 && invulnerableTime > -1) 
				invulnerable = false;
			
			if(burning){
				burnTime+=dt;
				burnDelay+=dt;
				if(burnDelay>= 3){
					damage(maxHealth/(2*DEFAULT_MAX_HEALTH), DamageType.FIRE);
					burnDelay = 0;
				} if(burnTime >= totBurnLength){
					burning = false;
					burnTime = 0;
				}
			}

			//stepping sound
			stepTime += dt;
			if (stepTime >= MOVE_DELAY && isOnGround() && Math.abs(body.getLinearVelocity().x) >= maxSpeed / 2){
				int num = (int) (Math.random()*4+1);
				float pitch = (float) Math.random()*(1.6f-.85f) + .85f;
//				num =4; //for volume testing
				if(groundType!=null)				
					if(groundType.isEmpty())
						main.playSound(getPosition(), "step_concrete"+num, pitch);
					else{
						main.playSound(getPosition(), "step_"+groundType+num, pitch);
					}
				else
					main.playSound(getPosition(), "step_concrete"+num, pitch);
				stepTime = 0;
			}

			if (animation.actionIndex < 4 && getAnimationAction()!= Anim.JUMPING && getAnimationAction()!= Anim.FLINCHING
					&& getAnimationAction()!= Anim.LANDING) {
				action = Anim.STANDING;
			} 

			wasOnGround = isOnGround();

			if (Math.abs(body.getLinearVelocity().x)>=WALK_SPEED && !running)
				running = true;
			else if (Math.abs(body.getLinearVelocity().x)>=WALK_SPEED && running){
				running = false; maxSpeed=WALK_SPEED;
			}
		}
	}
	
	public void render(FadingSpriteBatch sb){
		super.render(sb);;
		
//		if(isAttackable && !main.character.equals(this)){
//			sb.draw(healthBar[health], getPosition().x * Vars.PPM- rw, 
//					getPosition().y * Vars.PPM + rh + 3);
//		}
	}

	public void setPosition(Vector2 location){
		if(location==null) return;
		x=location.x;
		y=location.y+rh;
		setRespawnpoint(location);
	}

	protected void setAnimation(Anim action){
		setAnimation(action, false);
	}

	protected void setAnimation(Anim action, boolean repeat){
		if(action==Anim.RUNNING)
			setAnimation(action, .07f, repeat);
		else setAnimation(action, Vars.ACTION_ANIMATION_RATE, repeat);
	}

	protected void setAnimation(Anim action, float delay){
		setAnimation(action, delay, false);
	}
	
	/**
	 * Flips the order of the images in the spritesheet for the given action. If the image set is not reverseable, the 
	 * animation is not changed. The animation length is determined by the field 'actionLengths'
	 * @param reversed if the operation should be completed. If not, then the animation plays like normal 
	 * @param action the action of the animation to be reversed
	 */
	public void setAnimation(boolean reversed, Anim action) {
		Array<Anim> reverseable = new Array<>(new Anim[]{Anim.LOOK_UP, Anim.DUCK, Anim.SIT_DOWN, Anim.LIE_DOWN,
				Anim.GET_DOWN, Anim.AIM_TRANS, Anim.EMBRACE, Anim.ENGAGE_KISS, Anim.SPECIAL3_TRANS});
		if(reversed){
			if(!reverseable.contains(action, true))
				return;
		} else{
			setAnimation(action);
			return;
		}
		if (isNotAvailable(action)) return;
		this.action = action;
		int aI = animationIndicies.get(action);
		
		try{
			TextureRegion[] sprites = TextureRegion.split(texture, width, height)[aI];
			Array<TextureRegion> tmp = new Array<>(), orig = new Array<>(sprites);
			
			for(int i = actionLengths[aI]-1; i > 0; i--){
				tmp.add(sprites[i]);
				orig.removeValue(sprites[i], true);
			}
			
			tmp.addAll(orig);
			orig = new Array<>(sprites);
			
			sprites = new TextureRegion[tmp.size];
			for(int i = 0;i<tmp.size;i++){
				sprites[i] = tmp.get(i);
			}
			
			animation.setAction(sprites, actionLengths[aI], isFacingLeft(), aI, Vars.ACTION_ANIMATION_RATE, false);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}

	protected void setAnimation(Anim action, float delay, boolean repeat) {
		if (isNotAvailable(action)) return;
		this.action = action;

		int aI = animationIndicies.get(action);
		int i = actionLengths[aI];
		if(ID.equals("reaper")) i = 6;

		try{
			TextureRegion[] sprites = TextureRegion.split(texture, width, height)[aI];
			animation.setAction(sprites, i, isFacingLeft(), aI, delay, repeat);
		} catch(Exception e) { }
	}

	public void setTransAnimation(Anim trans, Anim action) {
		setTransAnimation(trans, action, false);
	}
	
	public void setTransAnimation(Anim trans, Anim action, boolean looping) {
		if (isNotAvailable(trans)) return;
		if (this.action == trans)
			return;

		this.action = trans;
		int aI = animationIndicies.get(trans);
		int tAI = animationIndicies.get(action);
		
		try{
			TextureRegion[] sprites = TextureRegion.split(texture, width, height)[aI];
			TextureRegion[] nextSprites = TextureRegion.split(texture, width, height)[tAI];
			animation.setTransitionAction(nextSprites, actionLengths[aI], isFacingLeft(), 
					aI, sprites, actionLengths[tAI], tAI, looping);
		} catch (Exception e){

		}
	}
	
	public boolean isNotAvailable(Anim action){
		int i = animationIndicies.get(action);
		
		if(i < actionPriorities.length)
			if (actionPriorities[i] < actionPriorities[animation.actionIndex])
				return true;
		return false;
	}
	
	public Anim getAnimationAction(){
		return Anim.values()[animation.actionIndex];
	}
	
	public static Anim getAnimationAction(int index){
		if(index >=0 && index < Anim.values().length)
			return Anim.values()[index];
		else return null;
	}
	
	public boolean returnToPrevLoc, prevDirection, triggered;

	protected Vector2 goalPosition, prevLocation;
	protected float inactiveWait, inactiveTime, doTime, doDelay, time;
	protected boolean reached, locked, attacked;
	protected Entity focus;
	protected Script discoverScript;
	protected AttackType attackType;
	protected SightResponse responseType;
	protected AIState state, defaultState;
	
	public static final Array<String> NPCTypes = new Array<>();
	static{
		FileHandle[] handles = Gdx.files.internal("assets/images/enitities/mobs").list();
		
		for(FileHandle f:handles){
			if(f.extension().equals("png") && !f.nameWithoutExtension().contains("base"))
				NPCTypes.add(f.nameWithoutExtension());
		}
	}

	public void setState(String state){
		try{
			AIState s = AIState.valueOf(state.toUpperCase());
			setState(s);
		} catch(Exception e ){}
	}

	public void setState(AIState state){ 
		System.out.println("setting: "+state);
		if (state == AIState.FOLLOWING) {
			follow(main.character);
		} else {
			this.state = state;
		}
		//replace with
		//setState(state, main.character);
		//?
	}

	public void setState(AIState state, Entity focus){
		if (state == AIState.FOLLOWING) {
			follow(focus);
		} else {
			this.state = state;
		}
		this.focus = focus;
	}

	public void setDefaultState(String state){
		try{
			AIState s = AIState.valueOf(state.toUpperCase());
			setDefaultState(s);
		} catch(Exception e){}
	}
	
	public void watchPlayer(){
		setState(AIState.FACEPLAYER);
	}
	
	public void watchObject(Entity e){
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
	
	public void moveToPath(){
		moveToPath(path);
	}
	
	// for setting mobs to always follow path
	public void moveToPath(String src){
		Path p = main.getPath(src);
		if(p!=null){
			this.path = p;
			goalPosition = path.getCurrent();
			state = AIState.PATH;
		}
	}
	
	public void moveToPath(Path path){
		if(path!=null){
			System.out.println(this + "\t"+path);
			this.path = path;
			doAction(ScriptAI.MOVE);
		}
	}
	
	public void setPath(Path path){
		this.path = path;
	}

	//possibly crunch this down
	public void act(){
		float dx, dy;
//		if(ID.equals("maleplayer1"))
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
//		if(ID.equals("reaper"))
//			System.out.println("State: " +state.name());
		
			switch (state){
			//walk to random locations within a radius, then wait a random amount of time
			case IDLEWALK: 
				if(reached) inactiveTime++;
				if(inactiveTime >= inactiveWait && reached) reached = false;
				else if (!reached){
					if (!canMove()) {
						goalPosition = new Vector2((float) (((Math.random() * 6)+x)/PPM), y);
						inactiveWait = (float)(Math.random() *(IDLE_LIMIT) + 100);
						inactiveTime = 0;
						reached = true;
					}
					else {
						dx = (goalPosition.x - body.getPosition().x) * PPM ;
						if(dx < 1 && dx > -1){
							goalPosition = new Vector2((float) (((Math.random() * 6)+x)/PPM), y);
							inactiveWait = (float)(Math.random() *(IDLE_LIMIT) + 100);
							inactiveTime = 0;
							reached = true;
						} else {
							if(dx <= -1) left();
							if(dx >= 1) right();
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
				dx = main.character.getPosition().x - body.getPosition().x;

				float m = MAX_DISTANCE * (main.character.getFollowerIndex(this)+1);
				
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
							if(dx <= -1) left();
							if(dx >= 1) right();
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
			case PATH:
				if (!isReachable()) {
					path.stepIndex();
					if(path.completed){
						path = null;
						state = AIState.STATIONARY;
					} else {
						goalPosition = path.getCurrent();
					}
				}
				else {
					dx = (goalPosition.x - body.getPosition().x*PPM) ;
					if(dx < 1 && dx > -1){
						path.stepIndex();
						if(path.completed){
							path = null;
							state = AIState.STATIONARY;
						} else {
							goalPosition = path.getCurrent();
							reached = true;
						}
					} else {
						if(dx <= -1) left();
						if(dx >= 1) right();
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
						if(dx <= -1) left();
						if(dx >= 1) right();
					}
				} else {
					boolean found = false; dx =0;

					if(state == AIState.EVADING_ALL){
						if(discovered.size>0){
							dx = (discovered.get(0).getPixelPosition().x - getPixelPosition().x);
							found = true;
						}
					} else if(state == AIState.EVADING)
						if(discovered.contains(focus, true)){
							dx = (focus.getPixelPosition().x - getPixelPosition().x);
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
				if(getAnimationAction()!=Anim.DANCE)
					setAnimation(Anim.DANCE, true);
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
//		System.out.println("FIGHTING");
		float dx, dy;
		if(!attacked){
//			System.out.println("!attacked");
			if(attackFocus!=null && doTime>=doDelay){
				//System.out.println("attackFocus!=null && doTime>=doDelay");
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
			} /*else
				System.out.println(doTime+":"+doDelay);*/
		} else {
			if(reached) inactiveWait++;
			if(inactiveTime >= inactiveWait && reached) {
				reached = false;
				attacked = false;
			}
			if(!reached){
				if (!canMove()) {
					goalPosition = new Vector2((float) (((Math.random() * 6)+x)/PPM), y);
					inactiveWait = (float)(Math.random() *(attackRange) + 100);
					inactiveTime = 0;
					reached = true;
				}
				else {
					dx = (goalPosition.x - body.getPosition().x) * PPM ;
					if(dx < 1 && dx > -1){
						goalPosition = new Vector2((float) (((Math.random() * 6)+x)/PPM), y);
						inactiveWait = (float)(Math.random() *(attackRange) + 100);
						inactiveTime = 0;
						reached = true;
					} else {
						if(dx < 1) left();
						if(dx > -1) right();
					}
				}
			}
		}
	}
	
	public void facePlayer(){
		faceObject(main.character);
	}
	
	public void faceObject(Entity obj){
		float dx = obj.getPosition().x - getPosition().x;
		if(dx > 0 && isFacingLeft()) changeDirection();
		else if(dx < 0 && !isFacingLeft()) changeDirection();
	}

	public void evade(){
		state = AIState.EVADING_ALL;
	}

	public void evade(Entity focus){
		state = AIState.EVADING;
		this.focus = focus;
	}

	public void follow(Entity focus){
		setDefaultState(AIState.FOLLOWING);
		focus.addFollower(this);
	}

	public void stay(){
		state = AIState.FACEPLAYER;
		main.character.removeFollower(this);
	}

	public void fight(Entity d){
		attackFocus = d;
		state = AIState.FIGHTING;
		iff = IFFTag.HOSTILE; 
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

	public double getHealth(){ return health; }
	public String getName(){ return name; }
	public void setName(String name) {this.name = name;}
	
	public void ignite(){
		super.ignite();
		doAction(ScriptAI.FLAIL);
	}

	public void damage (double val, DamageType type){
		if(!dead)
			if(!invulnerable){
				main.playSound(getPosition(), "damage");
				health -= val;

				if(type==DamageType.FIRE){
					double chance = Math.random();
					if(chance>=.7d)
						ignite();

					if(frozen)
						thaw();
				} if (type==DamageType.ICE){
					double chance = Math.random();
					if(chance>=.8d)
						freeze();
				}

				if(health<=0)
					die();
				else if(val<=DAMAGE_THRESHOLD)
					setAnimation(Anim.FLINCHING);
				else {
					setTransAnimation(Anim.STUMBLE, Anim.KNOCKED_OUT);
				}
			}
	}

	public void damage(double val, DamageType type, Mob owner){
		if(!dead)
			if(!invulnerable){
				main.playSound(getPosition(), "damage");
				health -= val;

				if(type==DamageType.FIRE){
					double chance = Math.random();
					if(chance>=.7d)
						ignite();

					if(frozen)
						thaw();
				} if (type==DamageType.ICE){
					double chance = Math.random();
					if(chance>=.8d)
						freeze();
				}

				if(health<=0){
					if(owner.equals(main.character) && this.iff!=IFFTag.FRIENDLY)
						owner.experience+= .15f/owner.level;
					die();
				}
				else
					if(val>DAMAGE_THRESHOLD)
						setTransAnimation(Anim.STUMBLE, Anim.KNOCKED_OUT);

				setAnimation(Anim.FLINCHING);
			}

		if(owner.equals(main.character)){
				if(state!=AIState.ATTACKING){
					Script script = attackScript;
					if(script!=null)
						main.triggerScript(script);
					else
						switch(getAttackType()){
						case ENGAGE:
						fight(owner);
							break;
						case HIT_ONCE:
							attack(owner.getPosition());
							break;
						case RANDOM:
							double chance = Math.random();
							if(chance>.8d)
								fight(this);
							else if(chance>.5d)
								attack(getPosition());
							break;
						default:
							break;
						}
				}
		}
	}

	public void restore(){
		heal(maxHealth);
		makeInvulnerable(30);
	}

	public void makeInvulnerable(float t){
		invulnerableTime = t;
		invulnerable = true;
	}

	public void heal(double healVal){
		heal(healVal, true);
	}
	
	public void heal(double healVal, boolean playSound){
		health += healVal;
		if (health > maxHealth) 
			health = maxHealth;
		if (playSound)
			main.playSound(getPosition(), "heal");
	}

	public void die(){
		main.playSound(getPosition(), "fallhit");
		dead = true;
//		deadTime = 0;

		body.setLinearVelocity(0, 0);
		setTransAnimation(Anim.DIE_TRANS, Anim.DEAD);
	}

	/**
	 * must only be used for copying data
	 * @param health
	 * @param maxHealth
	 */
	public void resetHealth(double health, double maxHealth){
		this.health = health;
		this.maxHealth = maxHealth;
	}

	//create the mob in its last saved position
	public void respawn(){
		if (respawnPoint!=null){
			System.out.println("respawning");
			contacts.truncate(0);
			setPosition(respawnPoint);
			dead = false;
			animation.removeAction();

			if(main.character.equals(this)){
				main.getCam().resetZoom();
				main.getB2dCam().resetZoom();
			}
			
			main.addBodyToRemove(body);
			body.setUserData(this.copy());
			create();
		}
	}

	public void setRespawnpoint(Vector2 location){ respawnPoint = location.cpy(); }

	public void setGoal(float gx) { 
		this.goalPosition = new Vector2((float) gx/PPM + getPosition().x, getPosition().y); 
//		if(path==null)
		doAction(ScriptAI.MOVE);
	}

	public void doTimedAction(String action, float maxTime){
		try {
			ScriptAI s = ScriptAI.valueOf(action.toUpperCase());
			doTimedAction(s, maxTime);
		} catch(Exception e){ }
	}
	
	public void doTimedAction(ScriptAI action, float maxTime){
		doTimedAction(action, null, maxTime);
	}
	
	public void doTimedAction(ScriptAI action, Entity target, float maxTime){
		if(action == ScriptAI.MOVE || action == ScriptAI.RUN ||
				action==ScriptAI.HUG || action == ScriptAI.LOSEAIM)
			return;
		if(maxTime<=0)return;
		
		actionTime = maxTime;
		AIfocus = target;
		controlledAction = action;
		controlled = true;
		
		beginAction();
		doTimedAction();
	}
	
	public void doTimedAction(){
		//do shit
		Anim anim = null;
		switch(controlledAction){
		case AIM:
			if(actionTime<=Vars.DT){
				aiming = false;
				setAnimation(true, Anim.AIM_TRANS);
			}else if(!aiming){
				aiming = true;
				setTransAnimation(Anim.AIM_TRANS, Anim.AIMING, true);
			}
			break;
		case ATTACK:
			anim = Anim.ATTACKING;
			break;
		case DANCE:
			anim = Anim.DANCE;
			break;
		case FLAIL:
			if(!controlledPT2){
				setAnimation(Anim.ON_FIRE, true);
				controlledPT2 = true;
			} else {
				if(ctrlReached){
					goalPosition = new Vector2(getPosition().x + (float) Math.random()*(2f*Vars.TILE_SIZE)+
							Vars.TILE_SIZE, getPosition().y);
					float d = 1;
					if(!isFacingLeft()) d = -1;

					goalPosition = new Vector2(d * goalPosition.x, goalPosition.y);
					ctrlReached = false;
				} else {
					if(isReachable()){
						float dx = (goalPosition.x - getPosition().x)* PPM ;

						if(Math.abs(dx) > 1){
							if (dx > 0) right();
							else left();
						} else 
							ctrlReached = true;
					} else 
						ctrlReached = true;
				}

				if(actionTime<=Vars.DT)
					animation.removeAction();
			}
			break;
		case FLY:
			break;
		case HUG:
			if(actionTime<=Vars.DT)
				setAnimation(true, Anim.EMBRACE);
			else if(!controlledPT2){
				controlledPT2 = true;
				setTransAnimation(Anim.EMBRACE, Anim.HUGGING, true);
			}
			break;
		case IDLE:
			anim = Anim.IDLE;
			break;
		case JUMP:
			jump();
			break;
		case KISS:
			if(actionTime<=Vars.DT)
				setAnimation(true, Anim.ENGAGE_KISS);
			else if(!controlledPT2){
				controlledPT2 = true;
				setTransAnimation(Anim.ENGAGE_KISS, Anim.KISSING, true);
			}
			break;
		case PUNCH:
			anim = Anim.PUNCHING;
			break;
		case SNOOZE:
			if(actionTime<=Vars.DT)
				setAnimation(true, Anim.GET_DOWN);
			else if(!controlledPT2){
				controlledPT2 = true;
				setTransAnimation(Anim.GET_DOWN, Anim.SNOOZING);			
			}
			break;
		case SPECIAL1:
			anim = Anim.SPECIAL1;
			break;
		case SPECIAL2:
			anim = Anim.SPECIAL2;
			break;
		case SPECIAL3:
			if(actionTime<=Vars.DT)
				setAnimation(true, Anim.SPECIAL3);
			else if(!controlledPT2){
				controlledPT2 = true;
				setTransAnimation(Anim.SPECIAL3, Anim.HUGGING, true);
			}
			break;
		default:
			break;
		}
		
		if(anim!=null)
			setAnimation(anim);
		
		if(actionTime<=Vars.DT)
			finishAction();
	}
	
	public void doAction(String action){
		try{
			ScriptAI a = ScriptAI.valueOf(action.toUpperCase());
			doAction(a);
		} catch(Exception e){ }
	}
	
	public void doAction(ScriptAI action, Vector2 goal){
		goalPosition = goal;
		doAction(action);
	}

	public void doAction(ScriptAI action){
		if(action==ScriptAI.STOP){
			finishAction();
		} else {
			if(controlled && controlledAction==null){
				controlled = false;
				return;
			} if(!controlled){
				if((action==ScriptAI.HUG || action == ScriptAI.KISS || action == ScriptAI.FLY)
						&& AIfocus==null)
					return;
				controlled = true;
				controlledAction = action;
			}

			beginAction();
			doAction();
		}
	}
	
	/**
	 * sets the initial animation for the appropiate controlled action
	 */
	public void beginAction(){
		switch(controlledAction){
		case JUMP:
			jump();
			break;
		case AIM:
			aiming = true;
			setTransAnimation(Anim.AIM_TRANS, Anim.AIMING);
			break;
		case LOSEAIM:
			aiming = false;
			setAnimation(true, Anim.AIM_TRANS);
			break;
		case ATTACK:
			setAnimation(Anim.ATTACKING);
			break;
		case PUNCH:
			setAnimation(Anim.PUNCHING);
			break;
		case DANCE:
			if(actionTime>0)
				setAnimation(Anim.DANCE, true);
			else setAnimation(Anim.DANCE);
			break;
		case FLAIL:
			setAnimation(Anim.ON_FIRE, true);
			break;
		case FLY:
			//flying related mechanics
			
			//flying = true;
			body.setType(BodyType.KinematicBody);
			setAnimation(Anim.SWIMMING);
			break;
		case IDLE:
			if(actionTime>0)
				setAnimation(Anim.IDLE, true);
			else setAnimation(Anim.IDLE);
			break;
		case SLEEP:
			setTransAnimation(Anim.LIE_DOWN, Anim.SLEEPING, true);
			if(main.character.equals(this)/* && !sleepSpell*/){
				main.getSpriteBatch().fade();
				
				main.saveGame();
			}
			break;
		case SPECIAL3:
			setTransAnimation(Anim.SPECIAL3_TRANS, Anim.SPECIAL3);
			break;
		default:
			break;
		}
	}
	
	public void doAction(){
		Anim remove=null;
		switch(controlledAction){
		case RUN:
			run();
		case MOVE:
			if(goalPosition==null && path==null)
				return;

			if(isReachable()){
				float dx = (goalPosition.x - getPosition().x)* PPM ;

				if(Math.abs(dx) > 1){
					if (dx > 0) right();
					else left();
				} else {
					// handling when the mob engages conversation
					if (positioning) {
						positioning = false;
						if(positioningFocus!=null)
							faceObject(positioningFocus);
						finishAction();
					} else if(path!=null){
						path.stepIndex();
						if (path.completed)
							path = null;
						else
							goalPosition = path.getCurrent();
					}
				}
			} else {
				//path cannot be completed
				if(path!=null) {
					path = null;
				}
				finishAction();
			}
			break;
		// go to the focus object and hug it
		// if the object is a mob, make it hug back
		// wait for the animation to loop for 2 seconds before making both objects release
		case HUG:
			if(AIfocus==null)
				finishAction();
			else {
				if(getAnimationAction()!=Anim.HUGGING && getAnimationAction()!=Anim.EMBRACE){
					float dx = (AIfocus.getPosition().x - getPosition().x) * PPM;
				
					if(Math.abs(dx) > 1){
						if (dx > 0) right();
						else left();
					} else {
						AIfocus.faceObject(this);
						if(AIfocus instanceof Mob)
							((Mob)AIfocus).setTransAnimation(Anim.EMBRACE, Anim.HUGGING, true);
						setTransAnimation(Anim.EMBRACE, Anim.HUGGING, true);
					}
				} else if (getAnimationAction()==Anim.HUGGING){
					if(animation.getSpeed() * animation.getTimesPlayed() >= 2){
						if(AIfocus instanceof Mob)
							((Mob)AIfocus).setAnimation(true, Anim.EMBRACE);
						setAnimation(true, Anim.EMBRACE);
						controlledPT2 =true;
					}
				} else if (controlledPT2){
					if(getAnimationAction()!=Anim.EMBRACE)
						finishAction();
				}
			}
			break;
		case KISS:
			if(AIfocus==null)
				finishAction();
			else {
				if(getAnimationAction()!=Anim.KISSING && getAnimationAction()!=Anim.ENGAGE_KISS){
					float dx = (AIfocus.getPosition().x - getPosition().x) * PPM;
					
					if(Math.abs(dx) > 1){
						if (dx > 0) right();
						else left();
					} else {
						AIfocus.faceObject(this);
						setTransAnimation(Anim.ENGAGE_KISS, Anim.KISSING, true);
					}
				} else if (getAnimationAction()==Anim.KISSING){
					if(animation.getSpeed() * animation.getTimesPlayed() >= 2){
						setAnimation(true, Anim.ENGAGE_KISS);
						controlledPT2 =true;
					}
				} else if (controlledPT2){
					if(getAnimationAction()!=Anim.ENGAGE_KISS)
						finishAction();
				}
			}
			break;
		case JUMP:
			if(isOnGround())
				finishAction();
			break;
		// run around back and forth flailing for a few seconds
		// repeated a random # of times
		case FLAIL:
			maxSpeed = RUN_SPEED;
			
			if(ctrlRepeat==-1)
				ctrlRepeat = (int) (Math.random()*2) + 2;
			
			if(ctrlReached){
				goalPosition = new Vector2(getPosition().x + (float) Math.random()*(2f*Vars.TILE_SIZE)+
						Vars.TILE_SIZE, getPosition().y);
				float d = 1;
				if(!isFacingLeft()) d = -1;
				
				goalPosition = new Vector2(d * goalPosition.x, goalPosition.y);
				ctrlReached = false;
				ctrlRepeat--;
			} else {
				if(isReachable()){
					if(goalPosition==null)
						goalPosition = new Vector2(getPosition().x + (float) Math.random()*(2f*Vars.TILE_SIZE)+
								Vars.TILE_SIZE, getPosition().y);
					float dx = (goalPosition.x - getPosition().x)* PPM ;

					if(Math.abs(dx) > 1){
						if (dx > 0) right();
						else left();
					} else 
						ctrlReached = true;
				} else 
					ctrlReached = true;
			}
			
			if(ctrlRepeat == 0){
				finishAction();
				animation.removeAction();
			}
			break;
		case FLY:
			break;
		case SLEEP:
			heal(1, false);
			if(main.character.equals(this)){
				if(main.getSpriteBatch().getFadeType()==FadingSpriteBatch.FADE_IN &&
						!controlledPT2){
//					if(!sleepSpell)
					main.playSound("slept");
					main.dayTime = (main.dayTime + Main.NOON_TIME) % Main.DAY_TIME;
				}
			}
			
			if(health == maxHealth && !controlledPT2){
				controlledPT2=true;
				setAnimation(true, Anim.LIE_DOWN);
			}
			
			if(controlledPT2 && getAnimationAction()!=Anim.LIE_DOWN){
				finishAction();
				if(main.character.equals(this)){
					//trigger any events
				}
			}
			break;
		case SNOOZE:
			if(!controlledPT2)
				if(getAnimationAction()!=Anim.SNOOZING && getAnimationAction()!=Anim.GET_DOWN)
					if(animation.getTimesPlayed() * animation.getSpeed() >= 3){
						controlledPT2 = true;
						setAnimation(true, Anim.GET_DOWN);
					}
			break;
		case LOSEAIM:
		case AIM:
			remove = Anim.AIM_TRANS;
			break;
		case IDLE:
			remove = Anim.IDLE;
			break;
		case DANCE:
			remove = Anim.DANCE;
			break;
		case SPECIAL1:
			remove = Anim.SPECIAL1;
			break;
		case SPECIAL2:
			remove = Anim.SPECIAL2;
			break;
		case SPECIAL3:
			remove = Anim.SPECIAL3;
			break;
		case ATTACK:
			remove = Anim.ATTACKING;
			break;
		case PUNCH:
			remove = Anim.PUNCHING;
			break;
		default:
			break;
		}
		
		if(remove!=null)
			if(getAnimationAction()!=remove)
				finishAction();
	}
	
	//conclude any controlled actions for mob
	private void finishAction(){
		goalPosition = null;
		controlled = false;
		controlledPT2 = false;
		controlledAction = null;
		actionTime = 0;
		ctrlRepeat = -1;
	}

	public void lookUp(){
		if(snoozing){
			animation.removeAction();
			setAnimation(true, Anim.GET_DOWN);
			snoozing = false;
			return;
		}
		setTransAnimation(Anim.LOOK_UP, Anim.LOOKING_UP, true);
	}

	public void duck(){
		if(snoozing){
			//set body to half its size
			animation.removeAction();
			setAnimation(true, Anim.GET_DOWN);
			snoozing = false;
			return;
		}
		setTransAnimation(Anim.DUCK, Anim.DUCKING);
	}
	
	public void unDuck(){

		//normalize body shape
		
		setAnimation(true, Anim.DUCK);
	}

	public void jump() {
		inactiveTime = 0;
		timesIdled=0;
		if(snoozing){
			animation.removeAction();
			setAnimation(true, Anim.GET_DOWN);
			snoozing = false;
		}
		
		//can't move if the current action disallows it
		if(immobileActions.contains(getAnimationAction(), true) || frozen)
			return;
		if (isOnGround()) {
			action = Anim.JUMPING;

			main.playSound(getPosition(), "jump1");
			body.applyForceToCenter(0f, 160f, true);
			setAnimation(Anim.JUMPING, Vars.ACTION_ANIMATION_RATE*1.5f);
		}
	}

	// determine if object is in way
	public boolean mustJump(){
		return false;
	}

	public void changeDirection(){
		int d = -1;
		if (isFacingLeft()) {setDirection(false); d = 1; }
		else setDirection(true);
		
		int max = body.getFixtureList().size;
		float w1;
		PolygonShape shape;
			
		if(max >=3)
			if(body.getFixtureList().get(2).getUserData().equals("interact")){
				w1 = w + INTERACTION_SPACE;
				shape = (PolygonShape) body.getFixtureList().get(2).getShape();
				shape.setAsBox((w1/2f)/Vars.PPM, (rh+1)/Vars.PPM, new Vector2(d*INTERACTION_SPACE/(2*Vars.PPM), 0), 0);
			} 
		if(max >=4)
			if(body.getFixtureList().get(3).getUserData().equals("attack")){
				w1 = w + attackRange;
				shape = (PolygonShape) body.getFixtureList().get(3).getShape();
				shape.setAsBox((w1/2f)/Vars.PPM, (rh-1)/Vars.PPM, new Vector2(d*attackRange/(2*Vars.PPM), 0), 0);
			}
		if(max >=5)
			if(body.getFixtureList().get(4).getUserData().equals("vision")){
				int h = main.getScene().height/4;
				w1 = w + visionRange;
				shape = (PolygonShape) body.getFixtureList().get(4).getShape();
				shape.setAsBox((w1/2f+rh)/Vars.PPM, h/Vars.PPM, new Vector2((d*visionRange/2-d*w)/Vars.PPM, (h-height)/(Vars.PPM)), 0);
			}
		
		if(isOnGround())
			if(Math.abs(body.getLinearVelocity().x)> WALK_SPEED + .2f){
				main.playSound(getPosition(), "skid");
				setAnimation(Anim.TURN_RUN, false);
			}else
				setAnimation(Anim.TURN, false);
		animation.flip(isFacingLeft());
	}

	public boolean canMove() {
		inactiveTime = 0;
		timesIdled=0;
		if(snoozing){
			animation.removeAction();
			setAnimation(true, Anim.GET_DOWN);
			snoozing = false;
		}
		
		//can't move if the current action disallows it
		if(immobileActions.contains(getAnimationAction(), true) || frozen)
			return false;
		
		float min = visionRange, d;
		for(Entity e : discovered){
			if(e instanceof Ground || e.layer==layer){
				d = e.getPosition().x - getPosition().x;
				if(d<min) min = d;
			}
		}
		
		if(min<Vars.TILE_SIZE-rh)
			return false;
		return true;
	}
	
	//ensure the mob can reach its destination
	public boolean isReachable(){
		if(!canMove())
			return false;
		//goalPosisition;
		return true;
	}

	//optimize!
	public boolean canRun() {
		float dx;
		for(Entity e : discovered){
			if (e.getBody()==null) continue;
			dx = e.getPosition().x - getPosition().x;

			//ignore mobs on other layers
			if(e instanceof Mob)
				if(e.layer!=layer) 
					continue;
			if(Math.abs(dx)<Vars.TILE_SIZE/Vars.PPM)
				return false;
		}
		return true;
	}

	public void run(){
		if(!canMove() || !canRun()) return;
		if (getAnimationAction() != Anim.JUMPING) action = Anim.RUNNING;
		maxSpeed = RUN_SPEED;
	}

	public void left(){
		if (!canMove()) return;
		//		if (animation.actionID != JUMPING && animation.actionID != RUNNING) action = WALKING;
		if (!isFacingLeft()) changeDirection();
		if(action==Anim.TURN || action==Anim.TURN_RUN || action==Anim.TURN_SWIM) return;
		if(getAnimationAction()==Anim.DUCKING) {
			animation.removeAction();
			setAnimation(true, Anim.DUCK); 
			return;
		}

		float x = 1;
		if(running) x = 2;
		if (body.getLinearVelocity().x > -maxSpeed) body.applyForceToCenter(-5f*x, 0, true);
		if (Math.abs(body.getLinearVelocity().x)> WALK_SPEED+.15f) setAnimation(Anim.RUNNING);
		else setAnimation(Anim.WALKING);

		if (!(this.equals(main.character)) && mustJump()){
			jump();
		}
	}

	public void right(){
		if(!canMove()) return;
//		if (animation.actionID != JUMPING && animation.actionID != RUNNING) action = WALKING;
		if (isFacingLeft()) changeDirection();
		if(action==Anim.TURN || action==Anim.TURN_RUN || action==Anim.TURN_SWIM) return;
		if(getAnimationAction()==Anim.DUCKING) {
			animation.removeAction();
			setAnimation(true, Anim.DUCK); 
			return;
		}

		float x = 1;
		if(running) x = 2;
		if (body.getLinearVelocity().x < maxSpeed) body.applyForceToCenter(5f*x, 0, true);
		if (Math.abs(body.getLinearVelocity().x)> WALK_SPEED+.15f) setAnimation(Anim.RUNNING);
		else setAnimation(Anim.WALKING);

		if (!(this.equals(main.character)) && mustJump()){
			jump();
		}
	}

	public void climb(){ }
	public void descend(){ }

	public Script interact(){
		if (interactable == null) return null;
		
		killVelocity();
		interactable.killVelocity();
		return interactable.getScript();
	}
	
	public boolean useWarp(){
		//if(canWarp)
		if(warp == null) return false;
		main.addBodyToRemove(getBody());
		setPosition(warp.getLink().getWarpLoc());
//		Scene s = warp.getLink().owner;
		//save location in new level
		return true;
	}

	public TextureRegion getFace(int face){
		if (this.face != null) return this.face[face];
		return null;
	}

	public boolean isOnGround(){
		if (contacts.size>0) return true;
		else return false;
	}

	public String getGender(){ return gender; }
	public void setGender(String gender){
		gender = gender.toLowerCase();
		if(gender.equals("male") || gender.equals("female"))
			this.gender = gender;
	}
	
	public float getinactiveTime(){ return inactiveTime; }
	public int getTimesIdled(){ return timesIdled; }

	public void addAttackable(Entity e){
		if(e instanceof Ground || e.equals(this)) return;
		attackables.add(e);
	}

	public void removeAttackable(Entity e){ attackables.removeValue(e, true); }
	public Array<Entity> getAttackables(){ return attackables; }
	
	public void discover(Entity e){
		if(e instanceof Ground || e.equals(this)) return;
		discovered.add(e);
	}
	
	public void loseSightOf(Entity e){
		discovered.removeValue(e, true);
	}	

	public void attack(Vector2 focus){
		if(attackTime < attackDelay) return;
		attackTime = 0;
		
		if(focus!=null){
			if(powerType!=DamageType.PHYSICAL){
				setAnimation(Anim.ATTACKING);
				powerAttack(focus);
			}
		} else
			punch();
	}
	
	public void attack(){
		if(attackTime < attackDelay) return;
		attackTime = 0;
		
		if(attackFocus!=null){
			if(powerType!=DamageType.PHYSICAL){
				setAnimation(Anim.ATTACKING);
				powerAttack(attackFocus.getPosition());
			}
		} else 
			punch();
	}
	
	public void punch(){
		setAnimation(Anim.PUNCHING);
		for(Entity e : attackables){
			if(e instanceof Mob)
				if(((Mob)e).getIFF()!=IFFTag.FRIENDLY)
					((Mob) e).damage(strength, DamageType.PHYSICAL, this);
				else{
					// either damage the mob or don't at all
					((Mob) e).damage(0, DamageType.PHYSICAL, this);
				}else
					e.damage(strength);
			float force = (float) (5*strength);
			float dx = e.getPosition().x-getPosition().x;
			int d = 1;
			if(dx<0) d = -1;
			
			e.getBody().applyForceToCenter(new Vector2(d*force, 100), true);
		}
	}

	public void powerAttack(Vector2 target){
		switch (powerType){
		case FIRE: 
			main.addObject(shoot(target));
			break;
		case ICE: break;
		case ELECTRO: break;
		case ROCK: break;
		default:
		}
	}

	public Projectile shoot(Vector2 target){
		Vector2 spawnLoc = new Vector2((getPosition().x + rw/Vars.PPM)*Vars.PPM, 
				getPixelPosition().y);
		
		Projectile p = null;
		switch (powerType){
		case FIRE:
			p = new Projectile(this, ProjectileType.FIREBALL, spawnLoc.x, spawnLoc.y, target);
			break;
		case ICE:
			p = new Projectile(this, ProjectileType.ICE_SPIKE, spawnLoc.x, spawnLoc.y, target);
			break;
		case BULLET:
//			item = bullet
//			p = new Projectile(this, ProjectileType.ITEM, spawnLoc.x, spawnLoc.y, target);
			p = new Projectile(this, ProjectileType.ICE_SPIKE, spawnLoc.x, spawnLoc.y, target);
			break;
		case ELECTRO:
			break;
		case PHYSICAL:
			break;
		case ROCK:
			break;
		case WIND:
			break;
		}
		
		setAnimation(Anim.ATTACKING);
		p.setGameState(this.main);
		p.create();
		return p;
	}
	
	public Particle special(){
		switch(powerType){
		case BULLET:
			break;
		case ELECTRO:
			break;
		case FIRE:
			break;
		case ICE:
			break;
		case ROCK:
			break;
		case WIND:
			break;
		default:
			break;
		}
		
		setTransAnimation(Anim.AIM_TRANS, Anim.ATTACKING);
		return null;
	}

	public void levelUp(){
		if(level < 20) level++;
	}
	
	public IFFTag getIFF(){return iff;}
	public void setIFF(IFFTag tag){iff=tag;}
	public void setIFF(String tag){
		try{
			IFFTag i = IFFTag.valueOf(tag);
			setIFF(i);
		}catch(Exception e){
			setIFF(IFFTag.FRIENDLY);
		}
	}

	public void resetLevel(){ resetLevel(0); }
	public void resetLevel(int level){ this.level = level; }
	public int getLevel(){ return level; }
	public DamageType getPowerType(){ return powerType; }
	public void setPowerType(DamageType type){
		level = 1;
		this.powerType = type;
	}

	public void setPositioningFocus(Entity e){ positioningFocus = e; }
	public Entity getInteractable(){ return interactable; }
	public void setInteractable( Entity d) { interactable = d; }
	public void setInvulnerable(boolean val){
		if(val){
			invulnerable = true;
			invulnerableTime = -1;
		} else {
			invulnerable = false;
			invulnerableTime = 0;
		}
	}

	public void setGround(String type){ this.groundType = type; }
	public Warp getWarp(){ return warp; }
	public void setWarp(Warp warp){ this.warp = warp; }

	public void spawn(Vector2 location){
		setPosition(location);
		create();
		main.addObject(this);
	}

	private float w = DEFAULT_WIDTH/2f-4;
	public void create(){
		init = true;
		bdef = new BodyDef();
		fdef = new FixtureDef();

		//hitbox
		PolygonShape shape = new PolygonShape();
		shape.setAsBox(w/Vars.PPM, (rh)/Vars.PPM);

		bdef.position.set(x/Vars.PPM, y/Vars.PPM);
		bdef.type = BodyType.DynamicBody;
		fdef.shape = shape;

		body = world.createBody(bdef);
		body.setUserData(this);
		fdef.filter.maskBits = (short) (Vars.BIT_GROUND | Vars.BIT_BATTLE);
		fdef.filter.categoryBits = layer;
		body.createFixture(fdef).setUserData(Vars.trimNumbers(ID));
		body.setFixedRotation(true);
		body.setMassData(mdat);

		createFootSensor();
		createInteractSensor();
		createAttackSensor();
		createVisionSensor();
		createCenter();
	}

	protected void createFootSensor(){
		PolygonShape shape = new PolygonShape();
		shape.setAsBox((rw - 5)/Vars.PPM, 2/Vars.PPM, new Vector2(0, -1 * rh/Vars.PPM), 0);
		fdef.shape = shape;

		fdef.isSensor = true;
		fdef.filter.categoryBits = (short) (layer);
		fdef.filter.maskBits = (short) (Vars.BIT_HALFGROUND | Vars.BIT_GROUND);
		body.createFixture(fdef).setUserData("foot");
	}

	protected void createInteractSensor(){
		float w1 = w + INTERACTION_SPACE;
		int d = 1;
		if(isFacingLeft()) d = -1;
		
		PolygonShape shape = new PolygonShape();
		shape.setAsBox((w1/2f)/Vars.PPM, (rh+1)/Vars.PPM, new Vector2(d*INTERACTION_SPACE/(2*Vars.PPM), 0), 0);
		fdef.shape = shape;
		
		

		fdef.isSensor = true;
		fdef.filter.categoryBits = Vars.BIT_GROUND | Vars.BIT_HALFGROUND | Vars.BIT_LAYER1 | Vars.BIT_PLAYER_LAYER | Vars.BIT_LAYER3;
		fdef.filter.maskBits = Vars.BIT_LAYER1 | Vars.BIT_PLAYER_LAYER | Vars.BIT_LAYER3;
		body.createFixture(fdef).setUserData("interact");
	}
	
	protected void createAttackSensor(){
		float w1 = w + attackRange;
		int d = 1;
		if(isFacingLeft()) d = -1;
		
		PolygonShape shape = new PolygonShape();
		shape.setAsBox((w1/2f)/Vars.PPM, (rh-1)/Vars.PPM, new Vector2(d*attackRange/(2*Vars.PPM), 0), 0);
		fdef.shape = shape;
		
		fdef.isSensor = true;
		fdef.filter.categoryBits = Vars.BIT_GROUND | Vars.BIT_HALFGROUND | Vars.BIT_LAYER1 | Vars.BIT_PLAYER_LAYER | Vars.BIT_LAYER3;
		fdef.filter.maskBits = Vars.BIT_LAYER1 | Vars.BIT_PLAYER_LAYER | Vars.BIT_LAYER3;
		body.createFixture(fdef).setUserData("attack");
	}
	
	protected void createVisionSensor(){
		float w1 = w + visionRange;
		int d = 1, h = main.getScene().height/8;
		if(isFacingLeft()) d = -1;
		
		
		PolygonShape shape = new PolygonShape();
		shape.setAsBox((w1/2f+rh)/Vars.PPM, h/Vars.PPM, new Vector2((d*visionRange/2-d*w)/Vars.PPM, (h-height)/(Vars.PPM)), 0);
		fdef.shape = shape;

		fdef.isSensor = true;
		fdef.filter.categoryBits = Vars.BIT_GROUND | Vars.BIT_HALFGROUND | Vars.BIT_LAYER1 | Vars.BIT_PLAYER_LAYER | Vars.BIT_LAYER3;
		fdef.filter.maskBits = Vars.BIT_LAYER1 | Vars.BIT_PLAYER_LAYER | Vars.BIT_LAYER3;
		body.createFixture(fdef).setUserData("vision");
	}


	public Mob copy(){
		Mob n = new Mob(name, ID, sceneID, 0, 0, layer);

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
	
	public String toString(){ return ID +": " + name; }
	
	@Override
	public void read(Json json, JsonValue val) {
		super.read(json, val);
		try {
			//TODO figure out why this doesnt work
			this.respawnPoint = json.fromJson(Vector2.class, val.get("respawnPoint").child().toString());
		} catch (SerializationException | NullPointerException e) {
		}
		
		this.iff = IFFTag.valueOf(val.getString("iff"));
		this.name = val.getString("name");
		this.strength = val.getDouble("strength");
		this.level = val.getInt("level");
		this.experience = val.getFloat("experience");
		this.action = Anim.valueOf(val.getString("action"));
		this.defaultState = AIState.valueOf(val.getString("defaultState"));
		this.powerType = Entity.DamageType.valueOf(val.getString("powerType"));
		this.visionRange = val.getFloat("visionRange");
		
		int attackFocusId = val.getInt("attackFocus");
		int aiFocusId = val.getInt("AIfocus");
		int interactableId = val.getInt("interactable");
		if (attackFocusId > -1 || aiFocusId > -1 || interactableId > -1) {
			JsonSerializer.pushMobRef(this, attackFocusId, aiFocusId, interactableId);
		}
				
		//other stuff that typically happens in constructor
		Texture texture = Game.res.getTexture(ID + "face");
		if (texture != null) face = TextureRegion.split(texture, 64, 64)[0];
		texture = Game.res.getTexture("healthBar");
		if (texture != null) healthBar = TextureRegion.split(texture, 12, 1)[0];
		determineGender();
		idleDelay = 3*Vars.ANIMATION_RATE*animation.getDefaultLength();
	}

	@Override
	public void write(Json json) {
		super.write(json);
		json.writeValue("respawnPoint", this.respawnPoint);
		json.writeValue("iff", this.iff);
		json.writeValue("name", this.name);
		//json.writeValue("voice", this.voice);	//todo: implement voice
		json.writeValue("strength", this.strength);
		json.writeValue("level", this.level);
		json.writeValue("experience", this.experience);
		json.writeValue("action", this.action);
		json.writeValue("defaultState", this.defaultState);
		json.writeValue("powerType", this.powerType);
		json.writeValue("visionRange", this.visionRange);
		
		json.writeValue("attackFocus", (this.attackFocus != null) ? this.attackFocus.sceneID : -1);
		json.writeValue("AIfocus", (this.AIfocus != null) ? this.AIfocus.sceneID : -1);	
		json.writeValue("interactable", (this.interactable != null) ? this.interactable.sceneID : -1);
		
		//other fields probably necessary
		
		//TODO: path loading
	}

	// determines what animation gets played first 
	protected static final int[] actionPriorities = {0,
			1, /*WALKING*/
			1, /*RUNNING*/
			5, /*JUMPING*/
			3, /*FALLING_TRANS*/
			3, /*FALLING*/
			4, /*LANDING*/
			3, /*DUCK*/
			1, /*DUCKING*/
			0, /*LOOK_UP*/
			0, /*LOOKING_UP*/
			4, /*SIT_DOWN*/
			4, /*SITTING*/
			1, /*IDLE*/
			2, /*GET_DOWN*/
			2, /*SNOOZING*/
			2, /*GET_UP*/
			3, /*LIE_DOWN*/
			3, /*SLEEPING*/
			2, /*DANCE*/
			2, /*TURN*/
			2, /*TURN_RUN*/
			2, /*ON_FIRE*/
			5, /*FLINCHING*/
			2, /*STUMBLE*/
			4, /*KOCKED_OUT*/
			4, /*RECOVER*/
			3, /*AIMING_TRANS*/
			3, /*AIMING*/
			4, /*ATTACKING*/
			4, /*PUNCHING*/
			6, /*DIE_TRANS*/
			6, /*DEAD*/
			3, /*EMBRACE*/
			3, /*HUGGING*/
			3, /*ENGAGE_KISS*/
			3, /*KISSING*/
			0, /*FLOATING*/
			1, /*SWIMMING*/
			2, /*TURN_SWIM*/
			3, /*SPECIAL1*/
			3, /*SPECIAL2*/
			3, /*SPECIAL3*/
			3}; /*SPECIAL3_TRANS*/
	protected static final int[] actionLengths =    {0,
			8,  /*WALKING*/
			8,  /*RUNNING*/
			3,  /*JUMPING*/
			1,  /*FALL_TRANS*/
			2,  /*FALLING*/
			3,  /*LANDING*/
			3,  /*DUCK*/
			1,  /*DUCKING*/
			1,  /*LOOK_UP*/
			16, /*LOOKING_UP*/
			4,  /*SIT_DOWN*/
			16, /*SIITING*/
			16, /*IDLE*/
			2,  /*GET_DOWN*/
			16, /*SNOOZING*/
			4,  /*GET_UP*/
			4,  /*LIE_DOWN*/
			16, /*SLEEPING*/
			12, /*DANCE*/
			2,  /*TURN*/
			4,  /*TURN_RUN*/
			1,  /*ON_FIRE*/
			1,  /*FLINCHING*/
			1,  /*STUMBLE*/
			16, /*KNOCKED_OUT*/
			1,  /*RECOVER*/
			1,  /*AIMING_TRANS*/
			1,  /*AIMING*/
			1,  /*ATTACKING*/
			4,  /*PUNCHING*/
			1,  /*DIE_TRANS*/
			1,  /*DEAD*/
			1,  /*EMBRACE*/
			1,  /*HUGGING*/
			1,  /*ENGRAGE_KISS*/
			1,  /*KISSING*/
			16,  /*FLOATING*/
			8,  /*SWIMMING*/
			1,  /*TURN_SWIM*/
			1,  /*SPECIAL1*/
			1,  /*SPECIAL2*/
			1,  /*SPECIAL3*/
			1}; /*SPECIAL3_TRANS*/
}
