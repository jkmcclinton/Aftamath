package entities;

import java.util.HashMap;

import com.badlogic.gdx.Gdx;
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
import handlers.Animation.LoopBehavior;
import handlers.FadingSpriteBatch;
import handlers.JsonSerializer;
import handlers.Vars;
import main.Game;
import main.Main.InputState;
import scenes.Script;
import scenes.Script.ScriptType;

public class Mob extends Entity{

	public Array<Entity> contacts;
	public double strength = DEFAULT_STRENGTH;
	public float attackRange = DEFAULT_ATTACK_RANGE, attackTime, attackDelay=DEFAULT_ATTACK_DELAY, aimMax = DEFAULT_AIM_THRESHOLD;;
	public float voice;
	public boolean canWarp, canClimb, wasOnGround, running;
	public boolean climbing, falling, snoozing, knockedOut;
	public float experience, aimTime, powerCoolDown;

	//used for adjusting the distance between the character
	//and the object currently being interacted with
	public boolean positioning;
	public boolean returnToPrevLoc, prevDirection, triggered;
	public Entity positioningFocus;
	
	protected int level;
	protected DamageType powerType;
	protected IFFTag iFF;
	public Vector2 respawnPoint;
	protected String groundType, gender, name, nickName;
	protected float knockOutTime, idleTime, idleDelay;
	protected float stepTime, maxSpeed = WALK_SPEED;
	protected boolean aiming;
	public boolean ducking;
	protected boolean ctrlReached;
	protected boolean aimSounded=false;
	protected int timesIdled, followIndex = -1;
	protected TextureRegion[] face, healthBar;
	protected Warp warp;
	protected Array<Entity> attackables, discovered;
	protected Entity attackFocus, AIfocus;
	protected float visionRange = DEFAULT_VISION_RANGE;
	protected Entity interactable;
	protected static final int IDLE_LIMIT = 500;
	protected static Array<Anim> immobileActions = new Array<>(); 

	protected float inactiveWait, doTime, doDelay, time;
	protected boolean reached, locked, attacked;
	protected Script discoverScript;
	protected AttackType attackType;
	protected SightResponse responseType;
	protected MobAI currentState, defaultState;

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
	protected static final float DEFAULT_AIM_THRESHOLD = .8f;
	protected static final float DEFAULT_COOLDOWN = 3f;
	protected static final double DEFAULT_STRENGTH = 1;
	protected static final double DAMAGE_THRESHOLD = 4;
	protected static final int INTERACTION_SPACE = 17;

	//determines when the NPC fights back
	public static enum AttackType{
		ON_SIGHT, ENGAGE, HIT_ONCE, RANDOM, NEVER
	}

	//determines what the NPC does when the character is spotted
	public static enum SightResponse{
		FOLLOW, ATTACK, TALK, EVADE, IGNORE
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
	
	public static HashMap<Anim, Integer> animationIndicies = new HashMap<>();
	static{
		for(int i = 0; i<Anim.values().length; i++){
			animationIndicies.put(Anim.values()[i], i);
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
		this.nickName = name;
		this.layer = layer;
		origLayer = layer;
		try{
			this.voice = Vars.VOICES.get(ID);
		} catch(Exception e){voice = 0;}

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
			
			if(conflict && this.sceneID != -1)
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

	protected void init() {
		attackables = new Array<>();
		discovered = new Array<>();
		contacts = new Array<>();
		followers = new HashMap<>();
		flamable = true;
		isAttackable = true;
		destructable = true;
		attackTime = attackDelay;
		iFF=IFFTag.FRIENDLY;
		health = maxHealth = DEFAULT_MAX_HEALTH;
		defaultState = new MobAI(this, AIType.STATIONARY, null);
		currentState = defaultState;
		attackType = AttackType.NEVER;
		responseType = SightResponse.IGNORE;
		inactiveWait = (float)(Math.random() *(IDLE_LIMIT)+100);
		time = 0;
	}

	public void update(float dt){
		if(dead && Mob.getAnimName(animation.getCurrentType()).equals(Anim.DEAD)){
			main.removeBody(getBody());
			return;
		}

		attackTime+=dt;
		
		if(warp!=null)
			if(!warp.conditionsMet()){
				warp = null;
				canWarp = false;
			}
		
		if(frozen)
			super.update(dt);
		else{
			if(!Mob.getAnimName(animation.getCurrentType()).equals(Anim.AIMING) &&
					!Mob.getAnimName(animation.getCurrentType()).equals(Anim.AIM_TRANS))
				aiming = false;
			if(!Mob.getAnimName(animation.getCurrentType()).equals(Anim.DUCKING) &&
					!Mob.getAnimName(animation.getCurrentType()).equals(Anim.DUCK))
				ducking = false;
			
			if(aiming) {
				aimTime+=dt;
				if(aimTime>=aimMax && !aimSounded && powerCoolDown==0 && this.equals(main.character)){
					main.playSound("jump4");
					aimSounded = true;
				}
			}
			
			if(powerCoolDown>0) powerCoolDown-=dt;
			else powerCoolDown = 0;
			 if (!this.equals(main.character)){
				if (isOnGround() && !locked && !controlled)
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

					currentState.update(dt);
			} else if(controlled)
				currentState.update(dt);

			// idle player actions
			if (getAction() == Anim.STANDING && main.currentScript==null) {
				idleTime+=dt;
				if(idleTime>=idleDelay){
					timesIdled +=1;
					idleTime = 0;
					if(timesIdled >=4 && main.character.equals(this) && 
							(main.stateType==InputState.MOVE || main.stateType==InputState.MOVELISTEN)){
						snooze();
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

			//do stuff for if the mob is knocked
			if(knockedOut){
				knockOutTime+=dt;
				if(knockOutTime>=3f)
					setAnimation(Anim.RECOVER, LoopBehavior.ONCE);
				else if(getAction()!=Anim.RECOVER && getAction()!=Anim.STUMBLE)
					setAnimation(Anim.KNOCKED_OUT, LoopBehavior.CONTINUOUS);
			}else if(knockedOut && getAction()==Anim.RECOVER)
				if(animationIndicies.get(Anim.RECOVER)<=actionLengths.length)
					if(animation.getIndex()==actionLengths[animationIndicies.get(Anim.RECOVER)]-1){
						knockedOut = controlled = false;
					}

			if (!isOnGround()) 
			setTransAnimation(Anim.FALL_TRANS, Vars.ACTION_ANIMATION_RATE, Anim.FALLING, 
					Vars.ANIMATION_RATE, LoopBehavior.CONTINUOUS, -1);
			if(isOnGround() && !wasOnGround && (getAction()==Anim.FALLING||
					getAction()==Anim.FALL_TRANS))
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
			
			//apply burning damage
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

			//reset animation for next update
//			if (animation.type < 4 && getAnimationAction()!= Anim.JUMPING && getAnimationAction()!= Anim.FLINCHING
//					&& getAnimationAction()!= Anim.LANDING) {
//				action = Anim.STANDING;
//				if(this.equals(main.character))
//					System.out.println("resetting");
//			} 

			wasOnGround = isOnGround();

			//Detect if mob has reached running speed
			if (Math.abs(body.getLinearVelocity().x)>=WALK_SPEED && !running)
				running = true;
			else if (Math.abs(body.getLinearVelocity().x)>=WALK_SPEED && running){
				running = false; maxSpeed=WALK_SPEED;
			}
		}
	}
	
	public void collisionUpdate(){
		if(interactable!=null);
//			if(this.equals(main.character)){
////			interactPair = null;
//			}
	}
	
	public void render(FadingSpriteBatch sb){
		super.render(sb);
	}

	/**units in pixels*/
	public void setPosition(Vector2 location){
		if(location==null) return;
		x=location.x;
		y=location.y+rh;
		setRespawnpoint(location);
	}
	
	public void setAnimation(Anim anim, float time){
		setAnimation(anim, LoopBehavior.TIMED, Vars.ACTION_ANIMATION_RATE, time, false);
	}

	public void setAnimation(Anim anim, LoopBehavior loop){
		setAnimation(anim, loop, Vars.ACTION_ANIMATION_RATE);
	}
	
	public void setAnimation(Anim anim, LoopBehavior loop, float delay){
		setAnimation(anim, loop, delay, -1, false);
	}

	public void setAnimation(Anim anim, LoopBehavior loop, float delay, boolean backwards) {
		setAnimation(anim, loop, delay, -1, backwards);
	}
	
	public void setAnimation(Anim anim, LoopBehavior loop, float delay, float time, boolean backwards){
		int type = animationIndicies.get(anim);
		int priority = actionPriorities[type];
		int length = actionLengths[type];
		TextureRegion[] primeArr = new TextureRegion[length];
		
		if(this.equals(main.character) && getAction()==Anim.LOOKING_UP)
			main.getCam().removeFocus();
		try{
			Array<TextureRegion> frames = new Array<>();
			frames.addAll(TextureRegion.split(texture, width, height)[type]);
			for(int i = 0; i < length; i++)
				primeArr[i] = frames.get(i);
			animation.setFrames(primeArr, delay, priority, type, loop, time, backwards);
		} catch(Exception e){
//			e.printStackTrace();
		}
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
		int type = animationIndicies.get(primaryAnim);
		int priority = actionPriorities[type];
		int length = actionLengths[type];
		int transType = animationIndicies.get(transAnim);
		int transLength = actionLengths[transType];
		TextureRegion[] primeArr = new TextureRegion[length];
		TextureRegion[] transArr = new TextureRegion[transLength];
		
		try{
			Array<TextureRegion> frames = new Array<>();
			frames.addAll(TextureRegion.split(texture, width, height)[type]);
			for(int i = 0; i < length; i++)
				primeArr[i] = frames.get(i);
			frames.clear();
			frames.addAll(TextureRegion.split(texture, width, height)[transType]);
			for(int i = 0; i < transLength; i++)
				transArr[i] = frames.get(i);
			animation.setWithTransition(transArr, transDelay, transType, primeArr,
					primaryDelay, priority, type);
		} catch (Exception e){

		}
	}
	
	public void addTransAnimation(Anim transAnim){
		addTransAnimation(transAnim, Vars.ACTION_ANIMATION_RATE);
	}
	
	public void addTransAnimation(Anim transAnim, float transDelay){
		int transType = animationIndicies.get(transAnim);
		int transLength = actionLengths[transType];
		TextureRegion[] transArr = new TextureRegion[transLength];
		
		try{
			Array<TextureRegion> frames = new Array<>();
			frames.addAll(TextureRegion.split(texture, width, height)[transType]);
			for(int i = 0; i < transLength; i++)
				transArr[i] = frames.get(i);
			animation.addTransition(transArr, transDelay, transType, facingLeft);
		} catch (Exception e){

		}
		
	}

	public Anim getAction(){
		return Anim.values()[animation.type];
	}
	
	public static Anim getAnimName(int index){
		if(index >=0 && index < Anim.values().length)
			return Anim.values()[index];
		else return null;
	}
	
	public void watchPlayer(){
		setState("FACEPLAYER", null, -1, "ON_SCRIPT_END");
	}
	
	public void watchObject(Entity e){
		setState("FACEOBJECT", e, -1, "ON_SCRIPT_END");
	}

	public MobAI getState(){ return currentState; }
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
		Path path = main.getPath(src);
		if(path!=null){
			currentState = new MobAI(this, AIType.PATH, ResetType.ON_AI_COMPLETE, null);
			currentState.setGoal(path.getCurrent());
			currentState.path = path;
		}
	}

	public void moveToPath(Path path, boolean defaulted){
		moveToPath(path);
		if(defaulted){
			currentState.resetType = ResetType.NEVER;
			defaultState = currentState;
		}
	}
	
	public void moveToPath(Path path){
		if(path!=null){
			try{
				AIType s = AIType.PATH;
				controlled = true;
				currentState = new MobAI(this, s, ResetType.ON_AI_COMPLETE, path);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void facePlayer(){
		faceObject(main.character);
	}
	
	public void faceObject(Entity obj){
		if (obj==null) return;
		float dx = obj.getPosition().x - getPosition().x;
		if(dx > 0 && facingLeft) changeDirection();
		else if(dx < 0 && !facingLeft) changeDirection();
	}

	public void evade(){
		if(currentState.equals(defaultState))
			defaultState = currentState;
		currentState = new MobAI(this, AIType.EVADING_ALL, null);
	}

	public void evade(Entity focus){
		if(currentState.equals(defaultState))
			defaultState = currentState;
		currentState = new MobAI(this, AIType.EVADING, focus);
	}

	public void follow(Entity focus){
		if(currentState.equals(defaultState))
			defaultState = currentState;
		currentState = new MobAI(this, AIType.FOLLOWING, focus);
		focus.addFollower(this);
	}
	
	public void setFollowIndex(int index){ followIndex = index; }
	public void resetFollowIndex(){ followIndex = -1; }

	public boolean stay(){
		if(!currentState.type.equals(AIType.FOLLOWING)) return false;
		if(!currentState.equals(defaultState))
			defaultState = currentState;
		currentState.focus.removeFollower(this);
		currentState = new MobAI(this, AIType.FACEPLAYER, main.character);
		return true;
	}

	public void fight(Entity d){
		attackFocus = d;
		if(currentState.equals(defaultState))
			defaultState = currentState;
		currentState = new MobAI(this, AIType.FIGHTING, d);
		iFF = IFFTag.HOSTILE; 
		doTime= (float) (Math.random()*3);
		attacked = false;
		reached = false;
	}
	
	public void timedFight(Entity d, float time){
		attackFocus = d;
		if(currentState.equals(defaultState))
			defaultState = currentState;
		currentState = new MobAI(this, AIType.FIGHTING, time, null);
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
	public String getNickName(){ return nickName; }
	public void setNickName(String name){ this.nickName = name; }
	
	public void ignite(){
		super.ignite();
//		currentState = new MobAI(this, AIType.FLAIL);
	}



	public void damage (double val, DamageType type){
		if(!dead){
			if(!invulnerable){
				if(type.equals(DamageType.PHYSICAL))
					main.playSound(getPosition(), "damage");
				health = health - val;

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
					if(health<0)health = 0;
					die();
				}
				else if(val<=DAMAGE_THRESHOLD)
					setAnimation(Anim.FLINCHING, LoopBehavior.ONCE);
				else {
					setTransAnimation(Anim.STUMBLE, Anim.KNOCKED_OUT);
				}
			}
			
			if(val<=DAMAGE_THRESHOLD){
				setAnimation(Anim.FLINCHING, LoopBehavior.ONCE);
			} else {
				setTransAnimation(Anim.STUMBLE, Anim.KNOCKED_OUT);
			}
		}
		main.addHealthBar(this);
	}

	public void damage(double val, DamageType type, Mob owner){
		if(!dead){
			if(!invulnerable){
				if(type.equals(DamageType.PHYSICAL))
					main.playSound(getPosition(), "damage");

				if(iFF!=IFFTag.FRIENDLY)
					health = health - val;

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
					if(health<0)health = 0;
					if(owner.equals(main.character) && this.iFF!=IFFTag.FRIENDLY)
						owner.experience+= .15f/owner.level;
					die();
				}
			}
			
			if(val<=DAMAGE_THRESHOLD){
				setAnimation(Anim.FLINCHING, LoopBehavior.ONCE);
			} else {
				setTransAnimation(Anim.STUMBLE, Anim.KNOCKED_OUT);
			}
		}
		main.addHealthBar(this);

		if(owner.equals(main.character) && !frozen){
				if(currentState.type!=AIType.FIGHTING){
					Script script = attackScript;
					
					if(type!=DamageType.PHYSICAL)
						script = supAttackScript;
					
					if(script!=null ){
						main.triggerScript(script);
						watchPlayer();
					}else
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
			
			main.removeBody(body);
			body.setUserData(this.copy());
			create();
		}
	}

	public void setRespawnpoint(Vector2 location){ respawnPoint = location.cpy(); }

	public void setGoal(float gx) {
		if(currentState.equals(defaultState))
			defaultState = currentState;
		setState("MOVE", new Vector2((float) gx + getPosition().x, getPosition().y));
	}

	public boolean setState(String type) {
		if(frozen) return false;
		try{
			if (type.toUpperCase().equals(AIType.FOLLOWING.toString()))
				follow(main.character);
			else {
			AIType s = AIType.valueOf(type.toUpperCase());
//				if(currentState.equals(defaultState))
//					defaultState = currentState;
				controlled = true;
				currentState = new MobAI(this, s, ResetType.ON_AI_COMPLETE, null);
			}
			return true;
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean setState(String type, Entity target) {
		if(frozen) return false;
		try{
			if (type.toUpperCase().equals(AIType.FOLLOWING.toString()))
				follow(target);
			else {
				AIType s = AIType.valueOf(type.toUpperCase());
				controlled = true;
				currentState = new MobAI(this, s, ResetType.ON_AI_COMPLETE, target);
			}
			return true;
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean setState(String type, Vector2 goal){
		if(frozen) return false;
		boolean b = setState(type);
		if(b) currentState.setGoal(goal);
		return b;
	}
	
	public boolean setState(String state, Entity target, float time, String resetType) {
		if(frozen) return false;
		try{
			AIType s = AIType.valueOf(state.toUpperCase());
			ResetType type = ResetType.valueOf(resetType.toUpperCase());
			if (s == AIType.FOLLOWING) 
				follow(target);
			else {
				if(time==-1){
					this.currentState = new MobAI(this, s, type, target);
					if(type.equals(ResetType.NEVER) && !MobAI.technical_types.contains(s, false))
						defaultState = currentState;
					else controlled = true;
				} else
					currentState = new MobAI(this, s, time, target);
			}
			return true;
		} catch(Exception e) {
//			e.printStackTrace();
			System.out.println("Cannot create AI of type \""+state+"\" and reset type \""+resetType+"\"");
			return false;
		}
	}
	
	public MobAI getCurrentState(){ return currentState; }
	public void resetState(){
		currentState.close();
		currentState = defaultState;
		currentState.begin();
		controlled = false;
	}
	
	//used only for copying mob data
	public void setDefaultState(MobAI cpy){
		defaultState = cpy;
	}
	
	public void setDefaultState(String s) {
		try{
			AIType type = AIType.valueOf(s.toUpperCase());
			if(!MobAI.technical_types.contains(type, false))
				defaultState = new MobAI(this, type, ResetType.NEVER, null);
		} catch(Exception e){
			
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
	
	public void unLookUp(){
		setAnimation(Anim.LOOK_UP, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE, true);
	}
	
	public void aim(){
		if(snoozing){
			wake();
		} else {
//			if(lookingUp)
//				unLookUp();
			aiming = true;
			setTransAnimation(Anim.AIM_TRANS, Vars.ACTION_ANIMATION_RATE, Anim.AIMING, 
					Vars.ANIMATION_RATE, LoopBehavior.CONTINUOUS, -1);
		}
	}
	
	public void unAim(){
		aimTime = 0;
		aiming = aimSounded = false;
		setAnimation(Anim.AIM_TRANS, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE, true);
	}

	public void duck(){
		if(snoozing){
			wake();
			return;
		}
		
		//shorten body height
		if(body.getFixtureList().get(0).getUserData().equals(Vars.trimNumbers(ID))){
			PolygonShape shape = (PolygonShape) body.getFixtureList().get(0).getShape();
			shape.setAsBox(w/Vars.PPM, (rh)/(Vars.PPM*1.75f), new Vector2(0, -(rh)/(Vars.PPM*2.25f)), 0);
		} 
		
		ducking = true;
		aiming = true;
		setTransAnimation(Anim.DUCK, Anim.DUCKING);
	}
	
	public void unDuck(){
		//normalize body height

		if(body.getFixtureList().get(0).getUserData().equals(Vars.trimNumbers(ID))){
			PolygonShape shape = (PolygonShape) body.getFixtureList().get(0).getShape();
			shape.setAsBox(w/Vars.PPM, (rh)/Vars.PPM);
		} 
		
		aiming = false;
		ducking = false;
		setAnimation(Anim.DUCK, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE, true);
	}
	
	public void knockOut(){
		snoozing = false;
		knockedOut = true;
		setTransAnimation(Anim.STUMBLE, Vars.ACTION_ANIMATION_RATE, Anim.KNOCKED_OUT, 
				Vars.ANIMATION_RATE, LoopBehavior.CONTINUOUS, -1);
	}
	
	public void recover(){
		aiming = false;
		knockedOut = false;
		setAnimation(Anim.RECOVER, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE, true);
	}
	
	private void resetIdle(){
		idleTime = 0;
		timesIdled = 0;
	}
	
	public void snooze(){
		setTransAnimation(Anim.GET_DOWN, Vars.ACTION_ANIMATION_RATE, Anim.SNOOZING, Vars.ANIMATION_RATE);
		snoozing = true;
	}
	
	public void wake(){
		animation.reset();
		setAnimation(Anim.GET_DOWN, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE, true);
		snoozing = false;
		resetIdle();
	}
	
	public void embrace(AIType type){
		if(type==AIType.KISS)
			setTransAnimation(Anim.ENGAGE_KISS, Vars.ACTION_ANIMATION_RATE, Anim.KISSING, Vars.ANIMATION_RATE, 
					LoopBehavior.CONTINUOUS, -1);
		if(type==AIType.HUG)
			setTransAnimation(Anim.EMBRACE, Vars.ACTION_ANIMATION_RATE, Anim.HUGGING, Vars.ANIMATION_RATE, 
					LoopBehavior.CONTINUOUS, -1);
	}
	
	public void release(AIType type){
		if(type==AIType.KISS)
			setAnimation(Anim.ENGAGE_KISS, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE, true);
		if(type==AIType.HUG)
			setAnimation(Anim.EMBRACE, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE, true);
	}

	public void jump() {
		main.getCam().removeFocus();
		resetIdle();
		if(snoozing){
			wake();
			return;
		}
		
		//can't move if the current action disallows it
		if(immobileActions.contains(getAction(), true) || frozen)
			return;
		if (isOnGround()) {
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
		if (facingLeft) {d = 1; }
		facingLeft = !facingLeft;
		
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
				int h = 6*Vars.TILE_SIZE;
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
		resetIdle();
		if(snoozing){
			//force mob to wake up
			wake();
		}
		
		//can't move if the current action disallows it
		if(immobileActions.contains(getAction(), true) || frozen)
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
		return true;
		
//		float dx;
//		for(Entity e : discovered){
//			if (e.getBody()==null) continue;
//			dx = e.getPosition().x - getPosition().x;
//
//			//ignore mobs on other layers
//			if(e instanceof Mob)
//				if(e.layer!=layer) 
//					continue;
//			if(Math.abs(dx)<Vars.TILE_SIZE/Vars.PPM)
//				return false;
//		}
//		return true;
	}

	public void run(){
		if(!canMove() || !canRun()) return;
//		if (getAction() != Anim.JUMPING) action = Anim.RUNNING;
		maxSpeed = RUN_SPEED;
	}

	public void left(){
		if(this.equals(main.character))
			main.getCam().removeFocus();
		if (!canMove()) return;
		if (!facingLeft) changeDirection();
		if(getAction()==Anim.TURN || getAction()==Anim.TURN_RUN || 
				getAction()==Anim.TURN_SWIM) return;
		if(getAction()==Anim.DUCKING) {
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
		if(this.equals(main.character))
			main.getCam().removeFocus();
		if(!canMove()) return;
		if (facingLeft) changeDirection();
		if(getAction()==Anim.TURN || getAction()==Anim.TURN_RUN || 
				getAction()==Anim.TURN_SWIM) return;
		if(getAction()==Anim.DUCKING) {
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
		if(snoozing) wake();
		killVelocity();
		
		Entity interactable = getInteractable();
		interactable.killVelocity();
		if(getInteractable() instanceof Mob)
			((Mob)interactable).watchPlayer();
		return interactable.getScript();
	}

	public TextureRegion getFace(int face){
		try{
			if (this.face != null)
				if(face>=this.face.length)
					return this.face[0];
				return this.face[face];
		} catch(Exception e){}
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
	
	public boolean sees(Entity e){
		return discovered.contains(e, false);
	}
	
	public boolean seesSomething(){
		return discovered.size>0;
	}
	
	public Entity getFirstDiscovered(){
		if(seesSomething())
			return discovered.get(0);
		return null;
	}
	
	public Array<Entity> getDiscovered(){ return discovered; }

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
		setAnimation(Anim.PUNCHING, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE/1.25f);
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
		if(powerCoolDown!=0) return;
		if(aiming && aimTime>aimMax)
			superAttack();
		else{
			aimTime = 0;
			switch (powerType){
			case DARKMAGIC:
			case ELECTRO:
			case FIRE: 
			case ICE:
			case ROCK:
				main.addObject(shoot(target));
				powerCoolDown = DEFAULT_COOLDOWN * .1f;
				break;
			default:
				break;
			}
		}
		
		addTransAnimation(Anim.ATTACKING);
	}
	
	//stronger, low range, multi target attack
	public void superAttack(){
		int x = 1;
		if(facingLeft) x = -1;
		Vector2 spawnLoc = new Vector2(getPixelPosition().x + x*rw, 
				getPixelPosition().y - rh);
		if(ducking)
			spawnLoc.x = getPixelPosition().x;
		
		DamageField dF = null;
		switch(powerType){
		case DARKMAGIC:
		case ELECTRO:
		case FIRE:
		case ICE:
			dF = new DamageField(spawnLoc.x, spawnLoc.y, level+1, this, powerType);
			break;
		case ROCK:
			if(main.getScene().outside){
				main.getCam().shake(1.5f);
				dF = new DamageField(spawnLoc.x, spawnLoc.y, level+1, this, powerType);
			}else
				main.playSound("bad1");
			break;
		default:
			break;
		}
		
		if(dF!=null){
			powerCoolDown = DEFAULT_COOLDOWN;
			aimSounded = false;
			dF.setGameState(this.main);
			dF.setDirection(facingLeft);
			dF.create();
			main.addObject(dF);
		}
	}

	public Projectile shoot(Vector2 target){
		int x = 1;
		if(facingLeft) x = -1;
		Vector2 spawnLoc = new Vector2((getPosition().x + x*rw/(Vars.PPM))*Vars.PPM, 
				getPixelPosition().y+4);

		Projectile p = null;
		ProjectileType pT = null;
		switch (powerType){
		case DARKMAGIC: pT = ProjectileType.SPELL; break;
		case FIRE: 	pT = ProjectileType.FIREBALL; break;
		case ICE: pT = ProjectileType.ICE_SPIKE; break;
		case BULLET:
//			item = bullet
//			p = new Projectile(this, ProjectileType.ITEM, spawnLoc.x, spawnLoc.y, target);
			break;
		case ELECTRO: pT = ProjectileType.ELECTRO_BALL; break;
		case PHYSICAL:
			//find equipped throwable item
			//throw item
			//if no throwable item, play empty clip sound
			break;
		case ROCK: pT = ProjectileType.BOULDER; break;
		case WIND: break;
		}
		
		if(pT!=null){
			p = new Projectile(this, pT, spawnLoc.x, spawnLoc.y, target);
			p.setGameState(this.main);
			p.setDirection(facingLeft);
			p.create();
		}
		return p;
	}

	public void levelUp(){
		if(level < 20) level++;
	}
	
	public IFFTag getIFF(){return iFF;}
	public void setIFF(IFFTag tag){iFF=tag;}
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
	
	public void setPowerType(String type){
		try{
			setPowerType(DamageType.valueOf(type.toUpperCase()));
		} catch(Exception e){
			if(type!=null)
				System.out.println("\""+type+"\" is and invalid power type");
		}
	}

	public void setPositioningFocus(Entity e){ positioningFocus = e; }
	public Entity getInteractable(){
		if(interactable==null) return null;
		if(interactable.frozen) 
			return null;
		return interactable; 
	}
	
	public void setInteractable(Entity e) { interactable = e; }
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
	public boolean aiming(){ return aiming; }
	public boolean aimSounded(){ return aimSounded; }

	public void spawn(Vector2 location){
		setPosition(location);
		create();
		main.addObject(this);
	}
	
	//allows the mob to automatically target something to shoot at
	public Vector2 target(){
		float distance = main.getScene().width;
		Entity target = null;
		if(discovered.size>=0){
			float dx, dy, d;
			for(Entity e: discovered){
				if(e.getBody()==null/* || !e.destructable*/) continue;
				dx = e.getPosition().x - getPosition().x;
				if((dx<0 && facingLeft) || (dx>=0 && !facingLeft)){
					dy = e.getPosition().y - getPosition().y;
					d = (float) Math.sqrt(dx*dx + dy*dy);
					if(d<distance/* && e is attackable?*/){
						distance = d;
						target = e;
					}
				}
			}
		}
		
		if(target!=null) return target.getPixelPosition();

		int x = 3;
		if(facingLeft) x *= -1;
		return new Vector2(getPixelPosition().x + x, getPixelPosition().y);
	}

	private final float w = (width-30)/2f;
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
		int d = 1, h = 6*Vars.TILE_SIZE;
		if(facingLeft) d = -1;
		
		
		PolygonShape shape = new PolygonShape();
		shape.setAsBox((w1/2f+rh)/Vars.PPM, h/Vars.PPM, new Vector2((d*visionRange/2-d*w)/Vars.PPM, (h-height)/(Vars.PPM)), 0);
		fdef.shape = shape;

		fdef.isSensor = true;
		fdef.filter.categoryBits = Vars.BIT_GROUND | Vars.BIT_HALFGROUND | Vars.BIT_LAYER1 | Vars.BIT_PLAYER_LAYER | Vars.BIT_LAYER3;
		fdef.filter.maskBits = Vars.BIT_LAYER1 | Vars.BIT_PLAYER_LAYER | Vars.BIT_LAYER3;
		body.createFixture(fdef).setUserData("vision");
	}

	public Mob copy(){
		Mob n = new Mob(name, ID, sceneID, x, y, layer);

		n.resetHealth(health, maxHealth);
		n.setDefaultState(defaultState);

		if(script!=null)
			n.setDialogueScript(script.ID);
		if(discoverScript!=null)
			n.setDiscoverScript(discoverScript.ID);
		if(attackScript!=null)
			n.setAttackScript(attackScript.ID);
		if(supAttackScript!=null)
			n.setSupAttackScript(supAttackScript.ID);
		
		n.level = level;
		n.strength = strength;
		n.resistance = resistance;
		n.attackRange = attackRange;
		n.visionRange = visionRange;
		n.flamable = flamable;
		n.setDestructability(destructable);
		n.nickName = nickName;
		
		return n;
	}
	
	@Override
	public void read(Json json, JsonValue val) {
		super.read(json, val);
		try {
			//TODO figure out why this doesnt work
			this.respawnPoint = json.fromJson(Vector2.class, val.get("respawnPoint").child().toString());
		} catch (SerializationException | NullPointerException e) { }
		
		this.iFF = IFFTag.valueOf(val.getString("iff"));
		this.name = val.getString("name");
		this.strength = val.getDouble("strength");
		this.level = val.getInt("level");
		this.experience = val.getFloat("experience");
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
		json.writeValue("iff", this.iFF);
		json.writeValue("name", this.name);
		//json.writeValue("voice", this.voice);	//TODO: implement voice
		json.writeValue("strength", this.strength);
		json.writeValue("level", this.level);
		json.writeValue("experience", this.experience);
		json.writeValue("defaultState", this.defaultState);
		json.writeValue("powerType", this.powerType);
		json.writeValue("visionRange", this.visionRange);
		
		json.writeValue("attackFocus", (this.attackFocus != null) ? this.attackFocus.sceneID : -1);
		json.writeValue("AIfocus", (this.AIfocus != null) ? this.AIfocus.sceneID : -1);	
		json.writeValue("interactable", (this.interactable != null) ? this.getInteractable().sceneID : -1);
		
		//other fields probably necessary
		
		//TODO: path loading
	}
	
	public String toString(){ return ID +": " + name; }

	//used solely for controlling animation
	public static enum Anim {
		STANDING,
		WALKING,
		RUNNING,
		JUMPING,
		FALL_TRANS,
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
		SPECIAL3_TRANS,
	}

	// determines what animation gets played first 
	protected static final int[] actionPriorities = {0,
			1, /*WALKING*/
			1, /*RUNNING*/
			5, /*JUMPING*/
			3, /*FALL_TRANS*/
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
			3, /*ON_FIRE*/
			5, /*FLINCHING*/
			2, /*STUMBLE*/
			4, /*KOCKED_OUT*/
			4, /*RECOVER*/
			3, /*AIM_TRANS*/
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
	protected static final int[] actionLengths =    {16,
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
			8,  /*ON_FIRE*/
			1,  /*FLINCHING*/
			1,  /*STUMBLE*/
			16, /*KNOCKED_OUT*/
			1,  /*RECOVER*/
			3,  /*AIM_TRANS*/
			16,  /*AIMING*/
			3,  /*ATTACKING*/
			4,  /*PUNCHING*/
			3,  /*DIE_TRANS*/
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
			16,  /*SPECIAL3*/
			3}; /*SPECIAL3_TRANS*/
}
