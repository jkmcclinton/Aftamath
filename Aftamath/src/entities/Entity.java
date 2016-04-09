package entities;

import static handlers.Vars.PPM;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.MassData;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.SerializationException;

import box2dLight.Light;
import handlers.Animation;
import handlers.FadingSpriteBatch;
import handlers.JsonSerializer;
import handlers.Vars;
import main.Game;
import main.Main;
import scenes.Scene;
import scenes.Script;
import scenes.Script.ScriptType;

public class Entity implements Serializable {
	
	//global mapping of IDs (in Tiled, custom prop "ID") to an Entity reference
	public static Map<Integer, Entity> idToEntity;
	
	public String ID;
	public Animation animation;
	public boolean isInteractable, isAttackable, dead, controlled;
	public boolean burning, flamable, frozen, init, active, destructable;
	public float x, y;
	public int height, width, rw, rh;

	//damage constants
	public static final float MAX_BURN_TIME = 10f; // in seconds
	public static final float MAX_FROZEN_TIME = 20f; // in seconds
	public static final float VULNERABLE = 2.15f;
	public static final float WEAK = 1.5f;
	public static final float RESISTANT = 1/2f;
	public static final float VERY_RESISTANT = 1/5f;
	
	protected double health;
	protected double maxHealth;
	protected double resistance = 1;
	protected int sceneID;
	protected float burnTime, burnDelay, totBurnLength, frozenTime, totFreezeLength;
	protected float invulnerableTime, maxSpeed = MOVE_SPEED;
	protected Texture texture;
	protected boolean facingLeft, invulnerable, died;
	protected World world;
	protected Main main;
	protected Scene currentScene;
	protected Body body;
	protected Script script, attackScript, supAttackScript;
	protected BodyDef bdef = new BodyDef();
	protected FixtureDef fdef = new FixtureDef();
	protected MassData mdat = new MassData();
	protected short layer, origLayer;
	protected HashMap<Mob, Boolean> followers;
	protected Light light;
	
	protected static final float MAX_DISTANCE = 65;
	protected static final double DEFAULT_MAX_HEALTH = 20;
	
	private Path path;
	private boolean moving;
	
	private static final float MOVE_SPEED = .70f;
	static {
		idToEntity = new HashMap<Integer, Entity>();
	}
	
	public enum DamageType{
		PHYSICAL, BULLET, FIRE, ICE, ELECTRO, ROCK, WIND, DARKMAGIC;
	}

	//no-arg should only be used by serializer
	public Entity() {
		this.init();
	} 
	
	public Entity (float x, float y, String ID) {
		this(x, y, -1, -1, ID);
	}
	
	public Entity(float x, float y, float width, float height, String ID) {
		this.init();
		this.ID = ID;
		this.x = x;
		this.y = y;
		
		if(width>-1)
			setDimensions(width, height);
		else
			setDimensions();
		loadSprite();
	}

	protected void init() {
		isAttackable = false;
		destructable = false;
		invulnerable = true;
		this.health = maxHealth = DEFAULT_MAX_HEALTH;
		followers = new HashMap<>();
		origLayer = Vars.BIT_LAYER1;
	}
	
	public void loadSprite() {
		animation = new Animation(this);
		texture = Game.res.getTexture(ID);
		
		if (texture != null){
			TextureRegion[] sprites = TextureRegion.split(texture, width, height)[0];
			setDefaultAnimation(sprites);
		} else {
			TextureRegion[] sprites = TextureRegion.split(Game.res.getTexture("empty"), 1, 1)[0];
			setDefaultAnimation(sprites);
//			System.out.println("Could not find \""+ID+".png\"");
		}
	}
	
	public void update(float dt){
		if(!frozen) animation.update(dt);
		else{
			frozenTime+=dt;
			if (frozenTime>=totFreezeLength)
				frozen = false;
		}
		
		//step invulnerability timer
		if (invulnerable && invulnerableTime > 0) 
			invulnerableTime-= dt;
		else if (invulnerable && invulnerableTime <= 0 && invulnerableTime > -1) 
			invulnerable = false;
		
		//apply burn effect
		if(burning){
			burnTime+=dt;
			burnDelay+=dt;
			if(burnDelay>= 3){
				damage(getMaxHealth()/(2*DEFAULT_MAX_HEALTH), DamageType.FIRE);
				burnDelay = 0;
			} if(burnTime >= totBurnLength){
				burning = false;
				burnTime = 0;
			}
		}
		
		// make entity move to path
		//NOTE, should be merged with AI state?
		if(moving){
			if(body.getType().equals(BodyType.KinematicBody)){
//				System.out.println("moving kinematic entity");
				moving = !moveToLoc(goalPosition);
				if(!moving){
					goalPosition = null;
					if(path!=null){
						path.stepIndex();
						if(path.completed){
							body.setLinearVelocity(new Vector2(0, 0));
							moving = false;
						} else{
							moving = true;
							goalPosition = path.getCurrent();
						}
					}
				}
			} else if(body.getType().equals(BodyType.DynamicBody)){
//				System.out.println("moving dynamic entity");
				moving = !moveToLoc(goalPosition.x);
				if(!moving) {
					goalPosition = null;
					if(path!=null){
						path.stepIndex();
						if(path.completed){
							Vector2 v = body.getLinearVelocity();
							body.setLinearVelocity(new Vector2(v.x/2f, v.y));
							moving = false;
						} else {
							moving = true;
							goalPosition = path.getCurrent();
						}
					}
				}
			}
			
			if(!moving){
				maxSpeed = MOVE_SPEED;
				path = null;
				Script s = main.currentScript;
				if(s!=null)
					if(s.getActiveObject()!=null)
						if(s.getActiveObject().equals(this))
							s.removeActiveObj();
			}
		}	
	}
	
	//for entities w/ kinematic bodies
	private boolean moveToLoc(Vector2 loc){
		if(isReachable(loc)){
			float dx = loc.x - getPixelPosition().x;
			float dy = loc.y - getPixelPosition().y;
			if(Math.abs(dx) > 2*maxSpeed || Math.abs(dy) > 2*maxSpeed){
				Vector2 v = body.getLinearVelocity();
				if (dx > 1.5f*maxSpeed) v.x = maxSpeed;
				else if (dx < -1.5f*maxSpeed) v.x = -maxSpeed;
//				else setPosition(new Vector2(loc.x, getPixelPosition().y));

				if (dy > 1.5f*maxSpeed) v.y = maxSpeed;
				else if (dy < -1.5f*maxSpeed) v.y = -maxSpeed;
//				else setPosition(new Vector2(getPixelPosition().x, loc.y-rh));
				body.setLinearVelocity(v);
				return false;
			} else {
				body.setLinearVelocity(new Vector2(0,0));
				return true;
			}
		}
		body.setLinearVelocity(new Vector2(0, 0));
		return true;
	}
	
	private boolean moveToLoc(float gx){
		if(isReachable(gx)){
			float dx = gx - getPixelPosition().x;
			if(Math.abs(dx) > 2){
				if (dx > 0) right();
				else left();
			} else {
				Vector2 v =body.getLinearVelocity();
				body.setLinearVelocity(new Vector2(v.x/2f, v.y));
				return true;
			}
			return false;
		}
		Vector2 v = body.getLinearVelocity();
		body.setLinearVelocity(new Vector2(v.x/2f, v.y));
		return true;
	}
	
	public void left(){
		if (!facingLeft) changeDirection();
		if (body.getLinearVelocity().x > -maxSpeed) 
			body.applyForceToCenter(-5f*x, 0, true);
	}
	
	public void right(){
		if (facingLeft) changeDirection();
		if (body.getLinearVelocity().x < maxSpeed) 
			body.applyForceToCenter(5f*x, 0, true);
	}
	
	private boolean isReachable(Vector2 loc){
		if(loc.x>getCurrentScene().width) return false;
		if(loc.x<0) return false;
		if(loc.y>getCurrentScene().height) return false;
		if(loc.y<0) return false;
		return true;
	}
	
	private boolean isReachable(float x){
//		if(!canMove())
//			return false;
		
		//adjust goal if location is outside of level
		if(x>getCurrentScene().width)
			return false;
		if(x<0)
			return false;
		return true;
	}
	
	private Vector2 goalPosition;
	public void move(Vector2 goal){
		goalPosition = goal;
		moving = true;
	}
	
	public void moveToPath(Path path){
		if (path==null) return;
		this.path = path.copy();
		moving = true;
		maxSpeed = path.getSpeed();
		goalPosition = path.getCurrent();
	}
	
	public void render(FadingSpriteBatch sb) {
		Color overlay = sb.getColor();
		if(frozen){
			sb.draw(animation.getFrame(), getPixelPosition().x - rw, getPixelPosition().y - rh);
			sb.setColor(Vars.blendColors(Vars.FROZEN_OVERLAY, overlay));
		} else {
			if(light !=null)
				sb.setColor(Vars.DAY_OVERLAY);
			}
		sb.draw(animation.getFrame(), getPixelPosition().x - rw, getPixelPosition().y - rh);
		if(sb.isDrawingOverlay())
			sb.setColor(overlay);
		
		if(light!=null)
			light.setPosition(getPixelPosition());
	}
	
	//used by scripts to change animations, possitbly??
	public void doAction(int action){ }
	
	public void setDefaultAnimation(TextureRegion reg){
		setDefaultAnimation(new TextureRegion[]{reg}, Vars.ANIMATION_RATE);
	}
	public void setDefaultAnimation(TextureRegion[] reg){
		setDefaultAnimation(reg,Vars.ANIMATION_RATE);
	}
	
	public void setDefaultAnimation(TextureRegion[] reg, float delay){
		animation.initFrames(reg, delay, facingLeft);
	}
	
	public void setGameState(Main gs){
		this.main = gs;
		this.world = gs.getWorld();
		this.currentScene = main.getScene();
		if (script != null) script.setPlayState(gs);
	}
	
	public Main getGameState(){return main;}
	public void setDialogueScript(String ID){  
		if(ID==null)return;
		
		if(ID.equals("none") || ID.equals("null") || ID.equals("empty")){
			script = null;
			isInteractable = false;
		} else {
			script = new Script(ID, ScriptType.DIALOGUE, main, this);
			isInteractable = true;
		}
	}

	public void setAttackScript(String ID){  
		if(ID==null)return;
		
		if(ID.equals("none") || ID.equals("null") || ID.equals("empty")){
			attackScript = null;
		} else {
		attackScript = new Script(ID, ScriptType.ATTACKED, main, this);
		if(supAttackScript==null)
			supAttackScript = new Script(ID, ScriptType.ATTACKED, main, this);
		}
	}
	
	public void setSupAttackScript(String ID){
		if(ID==null)return;
		if(ID.equals("none") || ID.equals("null") || ID.equals("empty"))
			supAttackScript = null;
		else
			supAttackScript = new Script(ID, ScriptType.ATTACKED, main, this);
	}
	
	public void changeLayer(short layer){
		this.layer = layer;
		if(body!=null){
			main.removeBody(body);
			fdef.filter.maskBits = (short) (layer | Vars.BIT_GROUND | Vars.BIT_BATTLE);
			fdef.filter.categoryBits = layer;
			create();
		} else {
//			System.out.println(layer);
			fdef.filter.maskBits = (short) (layer | Vars.BIT_GROUND | Vars.BIT_BATTLE);
			fdef.filter.categoryBits = layer;
		}
	}
	
	public short getLayer(){ return layer; }
	public Scene getCurrentScene(){ return currentScene; }
	public int getSceneID(){ return sceneID; }
	public void setSceneID(int ID){ sceneID = ID; }
	public Light getLight(){ return this.light; }
	
	/**
	 * links the entity to a light, 
	 * @param light
	 */
	public void addLight(Light light){
		try{
			if(this.light!=null)
				this.light.remove();
		} catch(Exception e){
			//cannot remove light
		}
		this.light = light;
	}
	
	/**units in pixels*/
	public void setPosition(Vector2 location){
		if(location==null) return;
		x=location.x;
		y=location.y+rh;
		respawn();
	}
	
	public void respawn(){
//		if(!(this instanceof SpeechBubble))
//			System.out.println("respawning: "+this);
		dead = died = false;
		animation.reset();
		main.removeBody(body);
		body.setUserData(this.copy());
		create();
		modifyHealth(maxHealth);
	}
	
	public Vector2 getPosition(){ 
		if(body!=null) {
			return body.getPosition();
		}
		else {
			return new Vector2(x, y);
		}
	}
	public Vector2 getPixelPosition(){ 
		if(body!=null) {
			return new Vector2(body.getPosition().x*Vars.PPM, body.getPosition().y*Vars.PPM);
		}
		else {
			return new Vector2(x, y);
		}
	}
	
	public Body getBody() { return body; }
	public void setBody(Body b) { this.body = b; } 

	public Script getScript() { return script; }
	public String toString(){ return ID; }
	
	public void changeDirection(){
		if (isFacingLeft()) facingLeft = false;
		else facingLeft = true;

		animation.flip(isFacingLeft());
	}
	
	public boolean isFacingLeft() { return facingLeft; }
	public void setDirection(boolean val){ 
		facingLeft = val;
		animation.flip(val);
	}

	public void ignite(){
		if(!flamable || burning) return;
		totBurnLength = (float) (Math.random()*MAX_BURN_TIME);
		burning = true;
		burnTime = 0;
//		gs.addParticleEffect(new Particle(Particle.FIRE, this));
	}
	
	public void freeze(){
		main.playSound(getPosition(), "freeze");
		totFreezeLength = (float) (Math.random()*MAX_FROZEN_TIME);
		frozen = true;
		frozenTime = 0;
//		gs.addParticleEffect(new Particle(Particle.SPARKLE, this));
	}
	
	public void thaw(){
		main.playSound(getPosition(), "thaw");
		frozen = false;
	}
	
	public void setResistance(float val){ resistance = val; }
	
	public void damage(double val){
		damage(val, DamageType.PHYSICAL);
	}
	
	public void damage (double val, DamageType type){
		if(!destructable) return;
		if(!dead)
			if(!invulnerable){
				if(main==null) return; //possibility for things like the ground that don't need constant updating
				main.playSound(getPosition(), "damage");
				health = getHealth() - val*resistance;
				
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
				
				if(getHealth()<=0){
					if(health<0)health = 0;
					die();
				}
				else;
					//shake
			}
		main.addHealthBar(this);
	}
	
	public void die(){
		main.playSound(getPosition(), "fallhit");
		dead = true;
//		deadTime = 0;
		
		body.setLinearVelocity(0, 0);
		//break
	}

	public void facePlayer(){
		if(main.character!=null){
			float dx = main.character.getPosition().x - getPosition().x;
			if(dx > 0 && isFacingLeft()) changeDirection();
			else if (dx < 0 && !isFacingLeft()) changeDirection();
		}
	}
	
	public void faceObject(Entity obj){
		if (obj != null)
			if(obj.getBody() != null){
				float dx = obj.getPosition().x - getPosition().x;
				if(dx > 0 && isFacingLeft()) changeDirection();
				else if (dx < 0 && !isFacingLeft()) changeDirection();
			}
	}
	
	public boolean getDirection(){ return isFacingLeft(); }
	

	public void setDimensions(){
		setDimensions(getWidth(ID), getHeight(ID));
	}
	
	protected void setDimensions(float width, float height){
		//if (width < 0 || height < 0) {
		//	width = getWidth(this.ID);
		//	height = getHeight(this.ID);
		//}
		
		this.width = (int) width; 
		this.height = (int) height;

		//units in pixels, measures radius of image
		this.rw = (int) width/2; 
		this.rh = (int) height/2;
	}
	
	protected static int getWidth(String ID){
		try{
			Texture src = Game.res.getTexture(ID+"base");
			return src.getWidth();
		} catch(Exception e) {
			try{
				Texture src = Game.res.getTexture(ID);
				return src.getWidth();
			} catch(Exception e1) {
				return 1;
			}
		}
	}

	public static int getHeight(String ID){
		try{
			Texture src = Game.res.getTexture(ID+"base");
			return src.getHeight();
		} catch(Exception e) {
			try{
				Texture src = Game.res.getTexture(ID);
				return src.getHeight();
			} catch(Exception e1) {
				return 1;
			}
		}
	}
	
	public double getMaxHealth() { 	return maxHealth; }
	public void setMaxHealth(double val) {
		if(health>=maxHealth) 
			health = val; 
		this.maxHealth = val; 
	}

	public void heal(double healVal){
		heal(healVal, true);
	}
	
	public void heal(double healVal, boolean playSound){
		modifyHealth(Math.abs(healVal));
		if (playSound)
			main.playSound(getPosition(), "heal");
	}

	/**restore health to max and make invulnerable for 5 seconds*/
	public void restore(){
		heal(maxHealth);
		makeInvulnerable(5);
	}

	public void makeInvulnerable(float t){
		invulnerableTime = t;
		invulnerable = true;
	}

	/** Sets both health and max health
	 * must only be used for copying data
	 * @param health
	 * @param maxHealth
	 */
	public void resetHealth(double health, double maxHealth){
		this.health = health;
		this.maxHealth = maxHealth;
	}
	
	protected void modifyHealth(double val){
		health = health + val;
		if(health>maxHealth) 
			health = maxHealth;
	}
	public double getHealth() { return health; }
	public boolean isVulnerable(){ return !invulnerable; }
	public void setDestructability(boolean val){
		destructable = val;
		invulnerable = !val;
	}

	public void removeFollower(Mob m){ 
		followers.remove(m);
		if(m!=null)
			m.resetFollowIndex();
	}
	
	public void addFollower(Mob m){ addFollower(m, false); }
	public void addFollower(Mob m, boolean permanent){ 
		if (!followers.containsKey(m)){
			followers.put(m, permanent);
			m.setFollowIndex(followers.size());
		}
	}
	
	public HashMap<Mob, Boolean> getFollowers(){ return followers; }

	public int compareTo(Entity e){
		if(e ==null) return 0;		
		if (layer < e.layer) return 1;
		if (layer > e.layer) return -1;
		return 0;
	}
	
	public boolean equals(Object o){
		if(o instanceof Entity){
			Entity e = (Entity) o;
			
			if(e.getBody()==null || getBody()==null)
				return super.equals(o);
			if(e.ID!=null && e.animation!=null){
				return  e.ID.equals(ID) && this.getClass().equals(o.getClass()) && 
						e.getPosition().equals(getPosition()) && e.facingLeft==e.isFacingLeft() && 
						e.animation.equals(animation) && e.currentScene.equals(currentScene);
			} else
				return super.equals(o);
		}
		return false;
	}
	
	public void killVelocity(){
		Vector2 vel = body.getLinearVelocity();
		body.setLinearVelocity(0, vel.y);
	}
	
	public void create(){
		init = true;
		//hitbox
		PolygonShape shape = new PolygonShape();
		shape.setAsBox((rw)/PPM, (rh)/PPM);
		
		bdef.position.set(x/PPM, y/PPM);
		bdef.type = BodyType.KinematicBody;
		fdef.shape = shape;
		body = world.createBody(bdef);
		body.setUserData(this);
		fdef.filter.maskBits = (short) (layer | Vars.BIT_GROUND | Vars.BIT_BATTLE);
		fdef.filter.categoryBits = layer;
		body.createFixture(fdef).setUserData(Vars.trimNumbers(ID));
		body.setMassData(mdat);
		
		createCenter();
		if(!main.exists(this)) main.addObject(this);
	}
	
	protected void createCenter(){
		PolygonShape shape = new PolygonShape();
		shape.setAsBox(1/Vars.PPM, 1/Vars.PPM);
		fdef.shape = shape;
		
		fdef.isSensor = true;
		fdef.filter.categoryBits = (short) (layer);
		fdef.filter.maskBits = (short) (Vars.BIT_HALFGROUND | Vars.BIT_GROUND);
		body.createFixture(fdef).setUserData("center");
	}
	
	public Entity copy(){
		Entity n = new Entity(x, y+getHeight(ID)/2f, ID);;

		n.resetHealth(health, maxHealth);

		if(script!=null)
			n.setDialogueScript(script.ID);
		if(attackScript!=null)
			n.setAttackScript(attackScript.ID);
		if(supAttackScript!=null)
			n.setSupAttackScript(supAttackScript.ID);
		
		n.resistance = resistance;
		n.flamable = flamable;
		n.setDestructability(destructable);
		
		return n;
	}

	@Override
	public void read(Json json, JsonValue val) {
		this.ID = val.getString("ID");
		this.sceneID = val.getInt("sceneID");
		float posX = val.getFloat("posX");
		float posY = val.getFloat("posY");
		Vector2 pos = new Vector2(posX, posY);
		this.setPosition(pos);
		this.health = val.getDouble("health");
		this.burning = val.getBoolean("burning");
		this.frozen = val.getBoolean("frozen");
		this.burnTime = val.getFloat("burnTime");
		this.burnDelay = val.getFloat("burnDelay");
		this.facingLeft = val.getBoolean("facingLeft");
		this.isInteractable = val.getBoolean("isInteractable");
		this.layer = val.getShort("layer");
		this.origLayer = val.getShort("origLayer");
		
		try {
			this.script = json.fromJson(Script.class, val.get("script").toString());
			this.script.setOwner(this);
			this.script.setMainRef(this.main);
		} catch (SerializationException | NullPointerException e) {}
		
		try {
			this.attackScript = json.fromJson(Script.class, val.get("attackScript").toString());
			this.attackScript.setOwner(this);
			this.attackScript.setMainRef(this.main);
		} catch (SerializationException | NullPointerException e) {}
		
		Array<Integer> mobRef = new Array<Integer>();
		for (JsonValue child = val.get("followers").child(); child != null; child = child.next()) {
			mobRef.add(child.getInt("value"));
		}				
		if (mobRef.size > 0) {
			JsonSerializer.pushEntityRef(this, mobRef);
		}
		
		//other stuff from constructor
		setDimensions();		
		loadSprite();
	}

	@Override
	public void write(Json json) {
		json.writeValue("ID", this.ID);
		json.writeValue("sceneID", this.sceneID);
		Vector2 pos = this.getPixelPosition();
		json.writeValue("posX", pos.x);
		json.writeValue("posY", pos.y);			
		json.writeValue("health", this.getHealth());
		json.writeValue("burning", this.burning);
		json.writeValue("frozen", this.frozen);
		json.writeValue("burnTime", this.burnTime);
		json.writeValue("burnDelay", this.burnDelay);
		json.writeValue("facingLeft", this.facingLeft);
		json.writeValue("isInteractable", this.isInteractable);
		json.writeValue("layer", this.layer);
		json.writeValue("origLayer", this.origLayer);
		
		//TODO get this working with the distinction of 'follow permanence' 
		Array<Integer> mobRef = new Array<Integer>();
		for (Mob m : this.followers.keySet()) {
			mobRef.add(m.sceneID);
		}
		json.writeValue("followers", mobRef);
		
		json.writeValue("script", this.script);
		json.writeValue("attackScript", this.attackScript);
	}
	
//	public void finalize(){
//		try {
//			super.finalize();
//			System.out.println("FINALIZING");
//			if(texture!=null)
//				texture.dispose();
//		} catch (Throwable e) {
//			e.printStackTrace();
//		}
//	}
	
}
