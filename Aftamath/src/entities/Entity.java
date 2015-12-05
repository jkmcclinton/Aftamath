package entities;

import static handlers.Vars.PPM;

import java.util.HashMap;
import java.util.Map;

import handlers.Animation;
import handlers.FadingSpriteBatch;
import handlers.JsonSerializer;
import handlers.Vars;
import main.Game;
import main.Main;
import scenes.Script;
import scenes.Script.ScriptType;

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

public class Entity implements Serializable {
	
	//global mapping of IDs (in Tiled, custom prop "ID") to an Entity reference
	public static Map<Integer, Entity> idToEntity;
	
	public String ID;
	public Animation animation;
	public boolean isInteractable, isAttackable, dead, controlled;
	public boolean burning, flamable, frozen, init, active, destructable;
	public float x, y;
	public int height, width, rw, rh;
	public Object o;

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
	protected float invulnerableTime;
	protected Vector2 goalPosition;
	protected Texture texture;
	protected boolean facingLeft, invulnerable;
	protected World world;
	protected Main main;
	protected Body body;
	protected Script script, attackScript;
	protected BodyDef bdef = new BodyDef();
	protected FixtureDef fdef = new FixtureDef();
	protected MassData mdat = new MassData();
	protected short layer, origLayer;
	protected Array<Mob> followers;
	
	static {
		idToEntity = new HashMap<Integer, Entity>();
	}
	
	public enum DamageType{
		PHYSICAL, BULLET, FIRE, ICE, ELECTRO, ROCK, WIND;
	}
	
	protected static final float MAX_DISTANCE = 65;
	protected static final double DEFAULT_MAX_HEALTH = 20;

	//no-arg should only be used by serializer
	public Entity() {
		this.init();
	} 
	
	public Entity (float x, float y, String ID) {
		this(x, y, -1, -1, ID);
	}
	
	public Entity(float x, float y, int width, int height, String ID) {
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
		followers = new Array<>();
		origLayer = Vars.BIT_LAYER1;
	}
	
	public void loadSprite() {
		animation = new Animation();
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
		
		if (invulnerable && invulnerableTime > 0) 
			invulnerableTime-= dt;
		else if (invulnerable && invulnerableTime <= 0 && invulnerableTime > -1) 
			invulnerable = false;
		
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
	}
	
	public void render(FadingSpriteBatch sb) {
		Color overlay = sb.getColor();
		if(frozen)
			sb.setColor(Vars.blendColors(Vars.FROZEN_OVERLAY, overlay));
		sb.draw(animation.getFrame(), getPixelPosition().x - rw, getPixelPosition().y - rh);
		if(frozen)
			sb.setColor(overlay);
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
		animation.setFrames(reg, delay, isFacingLeft());
	}
	
	public void setGameState(Main gs){
		this.main = gs;
		this.world = gs.getWorld();
		if (script != null) script.setPlayState(gs);
	}
	
	public Main getGameState(){return main;}
	public void setDialogueScript(String ID){  
		if(ID==null)return;
		script = new Script(ID, ScriptType.DIALOGUE, main, this);
		isInteractable = true;
	}

	public void setAttackScript(String ID){  
		if(ID==null)return;
		attackScript = new Script(ID, ScriptType.ATTACKED, main, this);
	}
	
	public void changeLayer(short layer){
		this.layer = layer;
		if(body!=null){
			main.addBodyToRemove(body);
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
	
	public int getSceneID(){ return sceneID; }
	public void setSceneID(int ID){ sceneID = ID; }
	public void setPosition(Vector2 location){
		x=location.x;
		y=location.y;
	}
	
	public Vector2 getPosition(){ return body.getPosition(); }
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
				
				if(getHealth()<=0)
					die();
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
	

	protected void setDimensions(){
		setDimensions(getWidth(ID), getHeight(ID));
	}
	
	protected void setDimensions(int width, int height){
		//if (width < 0 || height < 0) {
		//	width = getWidth(this.ID);
		//	height = getHeight(this.ID);
		//}
		
		this.width = width; 
		this.height = height;

		//units in pixels, measures radius of image
		this.rw = width/2; 
		this.rh = height/2;
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

	protected static int getHeight(String ID){
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
		this.maxHealth = val; 
		if(health>maxHealth) 
			health = val;
	}
	
	public double getHealth() { return health; }
	public boolean isVulnerable(){ return !invulnerable; }
	public void setDestructability(boolean val){
		destructable = val;
		invulnerable = !val;
	}

	public void addFollower(Mob m){ if (!followers.contains(m, true)) followers.add(m); }
	public void removeFollower(Mob m){ followers.removeValue(m, true); }
	public Array<Mob> getFollowers(){ return followers; }
	public int getFollowerIndex(Mob m) {
		Main.debugText = "" + followers; 
		return followers.indexOf(m, true) + 1; 
	}

	public int compareTo(Entity e){
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
				if(e.ID.equals(ID) && this.getClass().equals(o.getClass()) && 
						e.getPosition().equals(getPosition()) && e.isFacingLeft()==isFacingLeft() && 
						e.animation.equals(animation))
					return true;
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
		bdef.type = BodyType.StaticBody;
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

	@Override
	public void read(Json json, JsonValue val) {
		this.ID = val.getString("ID");
		this.sceneID = val.getInt("sceneID");
		float posX = val.getFloat("posX");
		float posY = val.getFloat("posY");
		Vector2 pos = new Vector2(posX, posY);
		this.o = pos;
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
		
		Array<Integer> mobRef = new Array<Integer>();
		for (Mob m : this.followers) {
			mobRef.add(m.sceneID);
		}
		json.writeValue("followers", mobRef);
		
		json.writeValue("script", this.script);
		json.writeValue("attackScript", this.attackScript);
	}
	
}
