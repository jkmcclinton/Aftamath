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

import entities.MobAI.AIType;
import entities.MobAI.ResetType;
import entities.Projectile.ProjectileType;
import handlers.Anim2.LoopBehavior;
import handlers.FadingSpriteBatch;
import handlers.JsonSerializer;
import handlers.Vars;
import main.Game;
import main.Main.InputState;
import scenes.Script;
import scenes.Script.ScriptType;

public class Mob2 extends Entity{

	public Array<Entity> contacts;
	public double strength = DEFAULT_STRENGTH;
	public float attackRange = DEFAULT_ATTACK_RANGE, attackTime, attackDelay=DEFAULT_ATTACK_DELAY;;
	public Sound voice;
	public boolean canWarp, canClimb, wasOnGround, running;
	public boolean climbing, falling, snoozing, knockedOut;
	public float experience;

	//used for adjusting the distance between the character
	//and the object currently being interacted with
	public boolean positioning;
	public Entity positioningFocus;
	
	protected int level;
	protected DamageType powerType;
	protected IFFTag iff;
	protected Vector2 respawnPoint;
	protected String groundType, gender, name;
	protected Anim action;
	protected float knockOutTime, idleTime, idleDelay;
	protected float stepTime;
	protected float maxSpeed = WALK_SPEED;
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

	public boolean returnToPrevLoc, prevDirection, triggered;

	protected Vector2 goalPosition, prevLocation;
	protected float inactiveWait, inactiveTime, doTime, doDelay, time;
	protected boolean reached, locked, attacked;
	protected Entity focus;
	protected Script discoverScript;
	protected AttackType attackType;
	protected SightResponse responseType;
//	protected AIState state, defaultState;
	protected MobAI state, defaultState;

	//determines when the NPC fights back
	public static enum AttackType{
		ON_SIGHT, ON_ATTACKED, ON_DEFEND, /*ON_EVENT,*/ RANDOM, NEVER
	}

	//determines what the NPC does when the character is spotted
	public static enum SightResponse{
		FOLLOW, ATTACK, TALK, EVADE, IGNORE
	}

	//determines what 
//	public static enum AIState{
//		STATIONARY, IDLEWALK, FOLLOWING, FACEPLAYER, FACEOBJECT, ATTACKING, FIGHTING, 
//		EVADING, EVADING_ALL, DANCING, BLOCKPATH, TIMEDFIGHT, PATH, PATH_PAUSE
//	}
	
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
	private static final float DEFAULT_ATTACK_DELAY = .5f;
	protected static final float DEFAULT_ATTACK_RANGE = 20;
	protected static final float DEFAULT_VISION_RANGE = 10*Vars.TILE_SIZE;
	protected static final double DEFAULT_STRENGTH = 1;
	protected static final double DAMAGE_THRESHOLD = 4;
	protected static final int INTERACTION_SPACE = 17;

	//used solely for controlling animation
	public static enum Anim {
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
//	public static enum ScriptAI{
//		MOVE, RUN, JUMP, FLY, SLEEP, FLAIL, IDLE, AIM, LOSEAIM, PUNCH, ATTACK, HUG, 
//		KISS, SNOOZE, DANCE, SPECIAL1, SPECIAL2, SPECIAL3, STOP
//	}
	
	public static HashMap<Anim, Integer> animationIndicies = new HashMap<>();
	static{
		for(int i = 0; i<Anim.values().length; i++){
			animationIndicies.put(Anim.values()[i], i);
//			System.out.println(Anim.values()[i]);
		}
	}
	
	public static final Array<String> NPCTypes = new Array<>();
	static{
		FileHandle[] handles = Gdx.files.internal("assets/images/enitities/mobs").list();
		
		for(FileHandle f:handles){
			if(f.extension().equals("png") && !f.nameWithoutExtension().contains("base"))
				NPCTypes.add(f.nameWithoutExtension());
		}
	}
	
	//Instantiate NPC mobs
	public Mob2(String name, String ID, int sceneID, float x, float y, short layer){
		this(name, ID, sceneID, 0, DamageType.PHYSICAL, x, y, layer);
	}
	
	public Mob2(String name, String ID, int sceneID, Vector2 location, short layer) {
		this(name, ID, sceneID, 0, DamageType.PHYSICAL, location.x, location.y, layer);
	}

	public Mob2(String name, String ID, int sceneID, int level, DamageType type, float x, float y, short layer){
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
	public Mob2(){
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
		defaultState = new MobAI(this, AIType.STATIONARY);
		state = defaultState;
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
			 if (!equals(main.character)){
				if (isOnGround() && !locked && !controlled && state!=null)
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
					}

					state.act(dt);
			}

			// idle player actions
			if (action == Anim.STANDING && main.currentScript==null) {
//				animation.removeAction();
				idleTime+=dt;
				if(idleTime>=idleDelay){
					timesIdled +=4;
					idleTime = 0;
					if(timesIdled >=4 && main.character.equals(this) && 
							(main.stateType==InputState.MOVE || main.stateType==InputState.MOVELISTEN)){
//						setTransAnimation(Anim.GET_DOWN, Anim.SNOOZING, true);
						timesIdled = 0;
						snoozing = true;
//						controlledAction = ScriptAI.SNOOZE;
					}
					else{
						if(this.equals(main.character))
							if(Math.random()>.25d)
								setAnimation(Anim.IDLE, LoopBehavior.ONCE);
							else
								setAnimation(Anim.DANCE, LoopBehavior.ONCE);
						else
							setAnimation(Anim.IDLE, LoopBehavior.ONCE);
					}
				}
			}

			if(knockedOut){
				knockOutTime+=dt;
				if(knockOutTime>=3f)
					setAnimation(Anim.RECOVER, LoopBehavior.ONCE);
				else if(action!=Anim.RECOVER && action!=Anim.STUMBLE)
					setAnimation(Anim.KNOCKED_OUT, LoopBehavior.CONTINUOUS);
			}else if(knockedOut && action==Anim.RECOVER)
				if(animationIndicies.get(Anim.RECOVER)<=actionLengths.length)
					if(animation.getIndex()==actionLengths[animationIndicies.get(Anim.RECOVER)]-1){
						knockedOut = controlled = false;
					}

			//make player sleep
//			if(snoozing && action!=Anim.GET_DOWN)
//				setAnimation(Anim.SNOOZING, true);

			if (!isOnGround()) 
				setAnimation(Anim.FALLING, LoopBehavior.CONTINUOUS);
			if(isOnGround() && !wasOnGround && (getAnimationAction()==Anim.FALLING||
					getAnimationAction()==Anim.FALLING_TRANS))
				setAnimation(Anim.LANDING, LoopBehavior.ONCE);

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

			if (animation.type < 4 && getAnimationAction()!= Anim.JUMPING && getAnimationAction()!= Anim.FLINCHING
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

	public void setAnimation(Anim anim, LoopBehavior loop){
		setAnimation(anim, loop, Vars.ANIMATION_RATE);
	}
	
	public void setAnimation(Anim anim, LoopBehavior loop, float delay){
		setAnimation(anim, loop, delay, false);
	}
	
	public void setAnimation(Anim anim, LoopBehavior loop, float delay, boolean backwards){
		int type = animationIndicies.get(anim);
		int priority = actionPriorities[type];
		int length = actionLengths[type];
		action = anim;
		
		Array<TextureRegion> frames = new Array<>();
		frames.addAll(TextureRegion.split(texture, width, height)[type]);
		for(int i = length; i < frames.size; i++)
			frames.removeIndex(i);
		
		animation.setFrames(frames.toArray(), delay, priority, type, backwards);
	}

	public void setTransAnimation(Anim transAnim, Anim primeAnim){
		setTransAnimation(transAnim, Vars.ACTION_ANIMATION_RATE,
				primeAnim, Vars.ACTION_ANIMATION_RATE);
	}
	
	public void setTransAnimation(Anim transAnim, Anim primeAnim, LoopBehavior loop){
		setTransAnimation(transAnim, Vars.ACTION_ANIMATION_RATE,
				primeAnim, Vars.ACTION_ANIMATION_RATE, loop, -1);
	}
	
	public void setTransAnimation(Anim transAnim, float transDelay, Anim primaryAnim, float primaryDelay) {
		setTransAnimation(transAnim, transDelay, primaryAnim, primaryDelay, LoopBehavior.ONCE, -1);	
	}
	
	public void setTransAnimation(Anim transAnim, float transDelay, Anim primaryAnim, float primaryDelay,
			LoopBehavior loop, float loopTime) {
		if (this.action == transAnim)
			return;

		int type = animationIndicies.get(primaryAnim);
		int priority = actionPriorities[type];
		int length = actionLengths[type];
		int transType = animationIndicies.get(transAnim);
		action = transAnim;
		
		try{
			Array<TextureRegion> primaryFrames = new Array<>();
			primaryFrames.addAll(TextureRegion.split(texture, width, height)[type]);
			for(int i = length; i < primaryFrames.size; i++)
				primaryFrames.removeIndex(i);
			Array<TextureRegion> transFrames = new Array<>();
			transFrames.addAll(TextureRegion.split(texture, width, height)[transType]);
			for(int i = length; i < transFrames.size; i++)
				transFrames.removeIndex(i);
			animation.setWithTransition(transFrames.toArray(), transDelay, primaryFrames.toArray(),
					primaryDelay, priority, type, facingLeft);
			//			(nextSprites, actionLengths[aI], isFacingLeft(), 
			//					aI, sprites, actionLengths[tAI], tAI, looping);
		} catch (Exception e){

		}
	}

	public Anim getAnimationAction(){
		return Anim.values()[animation.type];
	}
	
	public static Anim getAnimationAction(int index){
		if(index >=0 && index < Anim.values().length)
			return Anim.values()[index];
		else return null;
	}
	
	public void watchPlayer(){
		setState("FACEPLAYER");
	}
	
	public void watchObject(Entity e){
		focus = e;
		setState("FACEOBJECT");
	}

	public void resetState(){ state = defaultState; }
	public MobAI getState(){ return state; }
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
	
	// for setting mobs to always follow path
	public void moveToPath(String src){
		Path p = main.getPath(src);
		if(p!=null){
			goalPosition = path.getCurrent();
			state = new MobAI(this, AIType.PATH, ResetType.ON_AI_COMPLETE);
			state.path = p;
		}
	}

	public void moveToPath(Path path){
		if(path!=null){
			setState("MOVE");
			state.path = path;
		}
	}
	
	public void facePlayer(){
		faceObject(main.character);
	}
	
	public void faceObject(Entity obj){
		float dx = obj.getPosition().x - getPosition().x;
		if(dx > 0 && facingLeft) changeDirection();
		else if(dx < 0 && !facingLeft) changeDirection();
	}

	public void evade(){
		state = new MobAI(this, AIType.EVADING_ALL);
	}

	public void evade(Entity focus){
		state = new MobAI(this, AIType.EVADING);
		this.focus = focus;
	}

	public void follow(Entity focus){
		state = new MobAI(this, AIType.FOLLOWING);
		focus.addFollower(this);
	}

	public void stay(){
		state = new MobAI(this, AIType.FACEPLAYER);
		main.character.removeFollower(this);
	}

	public void fight(Entity d){
		attackFocus = d;
		state = new MobAI(this, AIType.FIGHTING);
		iff = IFFTag.HOSTILE; 
		doTime= (float) (Math.random()*3);
		attacked = false;
		reached = false;
	}
	
	public void timedFight(Entity d, float time){
		attackFocus = d;
		state = new MobAI(this, AIType.TIMEDFIGHT, time);
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
		state = new MobAI(this, AIType.FLAIL);
	}

	public void damage(double val, DamageType type){
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
					setAnimation(Anim.FLINCHING, LoopBehavior.ONCE);
				else {
					setTransAnimation(Anim.STUMBLE, Anim.KNOCKED_OUT);
				}
			}
	}

	public void damage(double val, DamageType type, Mob2 owner){
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

				setAnimation(Anim.FLINCHING, LoopBehavior.ONCE);
			}

		if(owner.equals(main.character)){
//				if(state.type!=AIType.ATTACKING){
					Script script = attackScript;
					if(script!=null)
						main.triggerScript(script);
					else
						switch(getAttackType()){
						case ON_ATTACKED:
						fight(this);
							break;
						case ON_DEFEND:
							attack(getPosition());
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
//		}
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
			animation.reset();

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
		setState("MOVE");
	}

	public void setState(String type) {
		try{
			AIType s = AIType.valueOf(type.toUpperCase());
			if (s == AIType.FOLLOWING) follow(main.character);
			else this.state = new MobAI(this, s, ResetType.ON_AI_COMPLETE);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void setState(String type, Entity focus) {
		try{
			AIType s = AIType.valueOf(type.toUpperCase());
			if (s == AIType.FOLLOWING) follow(focus);
			else this.state = new MobAI(this, s, ResetType.ON_AI_COMPLETE);
			this.focus = focus;
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void setState(String type, Vector2 goal){ // change to setState
		goalPosition = goal;
		setState(type);
	}

	public void setState(String state, float time) {
		try{
			AIType s = AIType.valueOf(state.toUpperCase());
			if (s == AIType.FOLLOWING) follow(main.character);
			else this.state = new MobAI(this, s, ResetType.ON_AI_COMPLETE);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void setState(String state, float time, Entity focus) {
		try{
			AIType s = AIType.valueOf(state.toUpperCase());
			if (s == AIType.FOLLOWING) follow(focus);
			else this.state = new MobAI(this, s, ResetType.ON_AI_COMPLETE);
			this.focus = focus;
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void lookUp(){
		if(snoozing){
			wake();
			return;
		}
		setTransAnimation(Anim.LOOK_UP, Vars.ACTION_ANIMATION_RATE, Anim.LOOKING_UP, 
				Vars.ANIMATION_RATE, LoopBehavior.CONTINUOUS, -1);
	}

	public void duck(){
		if(snoozing){
			wake();
			return;
		}
		setTransAnimation(Anim.DUCK, Anim.DUCKING);
	}
	
	public void unDuck(){
		//normalize body shape
		
		setAnimation(Anim.DUCK, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE, true);
	}
	
	public void wake(){
		animation.reset();
		setAnimation(Anim.GET_DOWN, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE, true);
		snoozing = false;
	}

	public void jump() {
		inactiveTime = 0;
		timesIdled=0;
		if(snoozing){
			wake();
			return;
		}
		
		//can't move if the current action disallows it
		if(immobileActions.contains(getAnimationAction(), true) || frozen)
			return;
		if (isOnGround()) {
			action = Anim.JUMPING;

			main.playSound(getPosition(), "jump1");
			body.applyForceToCenter(0f, 160f, true);
			setAnimation(Anim.JUMPING, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE*1.5f);
		}
	}

	// determine if object is in way
	public boolean mustJump(){
		return false;
	}

	public void changeDirection(){
		int d = -1;
		if (facingLeft) {facingLeft = false; d = 1; }
		else facingLeft = true;
		
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
			if(Math.abs(body.getLinearVelocity().x)> WALK_SPEED){
				main.playSound(getPosition(), "skid");
				setAnimation(Anim.TURN_RUN, LoopBehavior.ONCE);
			}else
				setAnimation(Anim.TURN, LoopBehavior.ONCE);
		animation.flip(facingLeft);
	}

	public boolean canMove() {
		inactiveTime = 0;
		timesIdled=0;
		if(snoozing){
			//force mob to wake up
			wake();
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
	
	//optimize!
	public boolean canRun() {
		float dx;
		for(Entity e : discovered){
			if (e.getBody()==null) continue;
			dx = e.getPosition().x - getPosition().x;

			//ignore mobs on other layers
			if(e instanceof Mob2)
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
		if (!facingLeft) changeDirection();
		if(action==Anim.TURN || action==Anim.TURN_RUN || action==Anim.TURN_SWIM) return;
		if(getAnimationAction()==Anim.DUCKING) {
			unDuck();
			return;
		}

		float x = 1;
		if(running) x = 2;
		if (body.getLinearVelocity().x > -maxSpeed) body.applyForceToCenter(-5f*x, 0, true);
		if (Math.abs(body.getLinearVelocity().x)> WALK_SPEED+.15f)
			setAnimation(Anim.RUNNING, LoopBehavior.ONCE);
		else setAnimation(Anim.WALKING, LoopBehavior.ONCE);

		if (!(this.equals(main.character)) && mustJump()){
			jump();
		}
	}

	public void right(){
		if(!canMove()) return;
//		if (animation.actionID != JUMPING && animation.actionID != RUNNING) action = WALKING;
		if (facingLeft) changeDirection();
		if(action==Anim.TURN || action==Anim.TURN_RUN || action==Anim.TURN_SWIM) return;
		if(getAnimationAction()==Anim.DUCKING) {
			unDuck();
			return;
		}

		float x = 1;
		if(running) x = 2;
		if (body.getLinearVelocity().x < maxSpeed) body.applyForceToCenter(5f*x, 0, true);
		if (Math.abs(body.getLinearVelocity().x)> WALK_SPEED+.15f) 
			setAnimation(Anim.RUNNING, LoopBehavior.ONCE);
		else setAnimation(Anim.WALKING, LoopBehavior.ONCE);

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
				setAnimation(Anim.ATTACKING, LoopBehavior.ONCE);
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
				setAnimation(Anim.ATTACKING, LoopBehavior.ONCE);
				powerAttack(attackFocus.getPosition());
			}
		} else 
			punch();
	}
	
	public void punch(){
		setAnimation(Anim.PUNCHING, LoopBehavior.ONCE);
		for(Entity e : attackables){
			if(e instanceof Mob2)
				if(((Mob2)e).getIFF()!=IFFTag.FRIENDLY)
					((Mob2) e).damage(strength, DamageType.PHYSICAL, this);
				else{
					// either damage the mob or don't at all
					((Mob2) e).damage(0, DamageType.PHYSICAL, this);
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
		
		setAnimation(Anim.ATTACKING, LoopBehavior.ONCE);
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
		if(facingLeft) d = -1;
		
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
		if(facingLeft) d = -1;
		
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
		if(facingLeft) d = -1;
		
		
		PolygonShape shape = new PolygonShape();
		shape.setAsBox((w1/2f+rh)/Vars.PPM, h/Vars.PPM, new Vector2((d*visionRange/2-d*w)/Vars.PPM, (h-height)/(Vars.PPM)), 0);
		fdef.shape = shape;

		fdef.isSensor = true;
		fdef.filter.categoryBits = Vars.BIT_GROUND | Vars.BIT_HALFGROUND | Vars.BIT_LAYER1 | Vars.BIT_PLAYER_LAYER | Vars.BIT_LAYER3;
		fdef.filter.maskBits = Vars.BIT_LAYER1 | Vars.BIT_PLAYER_LAYER | Vars.BIT_LAYER3;
		body.createFixture(fdef).setUserData("vision");
	}


	public Mob2 copy(){
		Mob2 n = new Mob2(name, ID, sceneID, 0, 0, layer);

		n.resetHealth(health, maxHealth);
		n.defaultState = defaultState;

		if(script!=null)
			n.setAttackScript(script.ID);
		if(discoverScript!=null)
			n.setAttackScript(discoverScript.ID);
		if(attackScript!=null)
			n.setAttackScript(attackScript.ID);

		return n;
	}
	
	
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
//		this.defaultState = MobAI.valueOf(val.getString("defaultState"));
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
	
	public String toString(){ return ID +": " + name; }

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
