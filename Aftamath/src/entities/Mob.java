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
	public Sound voice;
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
	protected IFFTag iff;
	protected Vector2 respawnPoint;
	protected String groundType, gender, name, nickName;
	protected float knockOutTime, idleTime, idleDelay;
	protected float stepTime, maxSpeed = WALK_SPEED;
	protected boolean aiming, ctrlReached, aimSounded=false;
	protected int timesIdled;
	protected TextureRegion[] face, healthBar;
	protected Warp warp;
	protected Array<Entity> attackables, discovered;
	protected Entity attackFocus, AIfocus;
	protected float visionRange = DEFAULT_VISION_RANGE;
	protected Entity interactable;
	protected static final int IDLE_LIMIT = 500;
	protected static Array<Anim> immobileActions = new Array<>(); 

	protected float inactiveWait, inactiveTime, doTime, doDelay, time;
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
	protected static final float DEFAULT_COOLDOWN = /*3f*/0;
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

	protected void init() {
		attackables = new Array<>();
		discovered = new Array<>();
		contacts = new Array<>();
		flamable = true;
		isAttackable = true;
		destructable = true;
		attackTime = attackDelay;
		iff=IFFTag.FRIENDLY;
		health = maxHealth = DEFAULT_MAX_HEALTH;
		defaultState = new MobAI(this, AIType.STATIONARY);
		currentState = defaultState;
		attackType = AttackType.NEVER;
		responseType = SightResponse.IGNORE;
		inactiveWait = (float)(Math.random() *(IDLE_LIMIT)+100);
		time = 0;
	}

	public void update(float dt){
		attackTime+=dt;
		
		if(frozen)
			super.update(dt);
		else{
			if(aiming) {
				aimTime+=dt;
				if(aimTime>=aimMax && !aimSounded && powerCoolDown==0){
//					main.playSound("jump4");
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
			}

			// idle player actions
			if (getAction() == Anim.STANDING && main.currentScript==null) {
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

			//make player sleep
//			if(snoozing && action!=Anim.GET_DOWN)
//				setAnimation(Anim.SNOOZING, true);

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
		setAnimation(anim, loop, Vars.ACTION_ANIMATION_RATE);
	}
	
	public void setAnimation(Anim anim, LoopBehavior loop, float delay){
		setAnimation(anim, loop, delay, false);
	}
	
	public void setAnimation(Anim anim, LoopBehavior loop, float delay, boolean backwards){
		int type = animationIndicies.get(anim);
		int priority = actionPriorities[type];
		int length = actionLengths[type];
		TextureRegion[] primeArr = new TextureRegion[length];
		
//		if(this.equals(main.character))
//			System.out.println("set");
		try{
			Array<TextureRegion> frames = new Array<>();
			frames.addAll(TextureRegion.split(texture, width, height)[type]);
			for(int i = 0; i < length; i++)
				primeArr[i] = frames.get(i);
			animation.setFrames(primeArr, delay, priority, type, backwards);
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
//		if (this.getAction() == transAnim)
//			return;

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
		setState("FACEPLAYER", null, -1, "NEVER");
	}
	
	public void watchObject(Entity e){
		setState("FACEOBJECT", e, -1, "NEVER");
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
			currentState = new MobAI(this, AIType.PATH, ResetType.ON_AI_COMPLETE);
			currentState.setGoal(path.getCurrent());
			currentState.path = path;
		}
	}

	public void moveToPath(Path path){
		if(path!=null){
			setState("MOVE");
			currentState.path = path;
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
		if(currentState.equals(defaultState))
			defaultState = currentState;
		currentState = new MobAI(this, AIType.EVADING_ALL);
	}

	public void evade(Entity focus){
		if(currentState.equals(defaultState))
			defaultState = currentState;
		currentState = new MobAI(this, AIType.EVADING);
		currentState.focus = focus;
	}

	public void follow(Entity focus){
		if(currentState.equals(defaultState))
			defaultState = currentState;
		currentState = new MobAI(this, AIType.FOLLOWING);
		focus.addFollower(this);
	}

	public void stay(){
		if(currentState.equals(defaultState))
			defaultState = currentState;
		currentState = new MobAI(this, AIType.FACEPLAYER);
		main.character.removeFollower(this);
	}

	public void fight(Entity d){
		attackFocus = d;
		if(currentState.equals(defaultState))
			defaultState = currentState;
		currentState = new MobAI(this, AIType.FIGHTING);
		iff = IFFTag.HOSTILE; 
		doTime= (float) (Math.random()*3);
		attacked = false;
		reached = false;
	}
	
	public void timedFight(Entity d, float time){
		attackFocus = d;
		if(currentState.equals(defaultState))
			defaultState = currentState;
		currentState = new MobAI(this, AIType.FIGHTING, time);
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
		currentState = new MobAI(this, AIType.FLAIL);
	}



	public void damage (double val, DamageType type){
		if(!dead)
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
		main.addHealthBar(this);
	}

	public void damage(double val, DamageType type, Mob owner){
		if(!dead)
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
					if(owner.equals(main.character) && this.iff!=IFFTag.FRIENDLY)
						owner.experience+= .15f/owner.level;
					die();
				}
				else
					if(val>DAMAGE_THRESHOLD)
						setTransAnimation(Anim.STUMBLE, Anim.KNOCKED_OUT);

				setAnimation(Anim.FLINCHING, LoopBehavior.ONCE);
			}
		main.addHealthBar(this);

		if(owner.equals(main.character)){
				if(currentState.type!=AIType.FIGHTING){
					Script script = attackScript;
					
					if(type!=DamageType.PHYSICAL)
						script = supAttackScript;
					
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
		setState("MOVE", new Vector2((float) gx/PPM + getPosition().x, getPosition().y));
	}

	public void setState(String type) {
		try{
			if (type.toUpperCase().equals(AIType.FOLLOWING.toString()))
				follow(main.character);
			else {
			AIType s = AIType.valueOf(type.toUpperCase());
				if(currentState.equals(defaultState))
					defaultState = currentState;
				currentState = new MobAI(this, s, ResetType.ON_AI_COMPLETE);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void setState(String type, Entity focus) {
		try{
			if (type.toUpperCase().equals(AIType.FOLLOWING.toString()))
				follow(focus);
			else {
			AIType s = AIType.valueOf(type.toUpperCase());
				if(currentState.equals(defaultState))
					defaultState = currentState;
				currentState = new MobAI(this, s, ResetType.ON_AI_COMPLETE);
			}
			currentState.focus = focus;
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void setState(String type, Vector2 goal){
		setState(type);
		currentState.setGoal(goal);
	}
	
	public void setState(String state, Entity target, float time, String resetType) {
		try{
			AIType s = AIType.valueOf(state.toUpperCase());
			ResetType type = ResetType.valueOf(resetType);
			if (s == AIType.FOLLOWING) 
				follow(target);
			else {
				if(time==-1)
					this.currentState = new MobAI(this, s, type);
				else {
					currentState = new MobAI(this, s, time);
					if(type.equals(ResetType.NEVER))
						defaultState = currentState;
				}
			}
			currentState.focus = target;
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public MobAI getCurrentState(){ return currentState; }
	public void resetState(){
		currentState = defaultState;
		controlled = false;
	}
	
	//used only for copying mob data
	public void setDefaultState(MobAI cpy){
		defaultState = cpy;
	}
	
	public void setDefaultState(String s) {
		try{
			AIType type = AIType.valueOf(s);
			defaultState = new MobAI(this, type, ResetType.NEVER);
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
	
	public void aim(){
		if(snoozing){
			wake();
		} else {
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
//		if (getAction() != Anim.JUMPING) action = Anim.RUNNING;
		maxSpeed = RUN_SPEED;
	}

	public void left(){
		if (!canMove()) return;
		//		if (animation.actionID != JUMPING && animation.actionID != RUNNING) action = WALKING;
		if (!facingLeft) changeDirection();
		if(getAction()==Anim.TURN || getAction()==Anim.TURN_RUN || getAction()==Anim.TURN_SWIM) return;
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
		if(!canMove()) return;
//		if (animation.actionID != JUMPING && animation.actionID != RUNNING) action = WALKING;
		if (facingLeft) changeDirection();
		if(getAction()==Anim.TURN || getAction()==Anim.TURN_RUN || getAction()==Anim.TURN_SWIM) return;
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
		
		DamageField dF = null;
		switch(powerType){
		case ELECTRO:
			dF = new DamageField(spawnLoc.x, spawnLoc.y, level+1, this, powerType);
			break;
		case FIRE:
			dF = new DamageField(spawnLoc.x, spawnLoc.y, level+1, this, powerType);
			break;
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
			break;
		case ELECTRO:
			p = new Projectile(this, ProjectileType.ELECTRO_BALL, spawnLoc.x, spawnLoc.y, target);
			break;
		case PHYSICAL:
			//find equipped throwable item
			//throw item
			//if no throwable item, play empty clip sound
			break;
		case ROCK:
			p = new Projectile(this, ProjectileType.BOULDER, spawnLoc.x, spawnLoc.y, target);
			break;
		case WIND:
			break;
		}
		
		p.setGameState(this.main);
		p.setDirection(facingLeft);
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
		
//		setTransAnimation(Anim.AIM_TRANS, Anim.ATTACKING);
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
	
	public void setPowerType(String type){
		try{
			setPowerType(DamageType.valueOf(type.toUpperCase()));
		} catch(Exception e){
			if(type!=null)
				System.out.println("\""+type+"\" is and invalid power type");
		}
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
	public boolean aiming(){ return aiming; }
	public boolean aimSounded(){ return aimSounded; }

	public void spawn(Vector2 location){
		setPosition(location);
		create();
		main.addObject(this);
	}
	
	//allows the mob to automatically target something to shoot at
	public Vector2 target(){
		if(discovered.size>=0){
			float dx;
			for(Entity e: discovered){
				if(e.getBody()==null/* || !e.destructable*/) continue;
				dx = e.getPosition().x - getPosition().x;
				if((dx<0 && facingLeft) || (dx>=0 && !facingLeft))
					return e.getPixelPosition();
			}
		}

		int x = 3;
		if(facingLeft) x *= -1;
		return new Vector2(getPixelPosition().x + x, getPixelPosition().y);
	}

	private final float w = (width-15)/2f;
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
		} catch (SerializationException | NullPointerException e) {
		}
		
		this.iff = IFFTag.valueOf(val.getString("iff"));
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
		json.writeValue("iff", this.iff);
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
		json.writeValue("interactable", (this.interactable != null) ? this.interactable.sceneID : -1);
		
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
			2, /*ON_FIRE*/
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
			1,  /*SPECIAL3*/
			1}; /*SPECIAL3_TRANS*/
}
