package entities;

import static handlers.Vars.PPM;
import handlers.Animation;
import handlers.FadingSpriteBatch;
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

public class Entity{
	
	public String ID;
	public Animation animation;
	public boolean isInteractable, isAttackable, dead, controlled;
	public boolean burning, flamable, frozen;
	public float x, y;
	public int height, width, rw, rh;

	//damage constants
	public static final float MAX_BURN_TIME = 10f; // in seconds
	public static final float MAX_FROZEN_TIME = 20f; // in seconds
	public static final float VULNERABLE = 2.15f;
	public static final float WEAK = 1.5f;
	public static final float RESISTANT = 1/2f;
	public static final float VERY_RESISTANT = 1/5f;
	
	protected double health, maxHealth, resistance = 1;
	protected boolean invulnerable;
	protected int sceneID;
	protected float burnTime, burnDelay, totBurnLength, frozenTime, totFreezeLength;
	protected float invulnerableTime;
	protected Vector2 goalPosition;
	protected Texture texture;
	protected boolean facingLeft;
	protected World world;
	protected Main main;
	protected Body body;
	protected Script script, attackScript;
	protected BodyDef bdef = new BodyDef();
	protected FixtureDef fdef = new FixtureDef();
	protected MassData mdat = new MassData();
	protected short layer, origLayer;
	protected Array<Mob> followers;
	
	public enum DamageType{
		PHYSICAL, BULLET, FIRE, ICE, ELECTRO, ROCK, WIND;
	}
	
	protected static final float MAX_DISTANCE = 50;
	protected static final double DEFAULT_MAX_HEALTH = 20;

	public Entity(){} 
	
	public Entity (float x, float y, String ID) {
		this.ID = ID;
		this.x = x;
		this.y = y;
		isAttackable = false;
		
		setDimensions();
		
		this.health = maxHealth = DEFAULT_MAX_HEALTH;
		loadSprite();
		followers = new Array<>();
		origLayer = Vars.BIT_LAYER1;
	}
	
	public Entity(float x, float y, int width, int height, String ID) {
		this.ID = ID;
		this.x = x;
		this.y = y;
		isAttackable = false;
		
		setDimensions(width, height);
		
		this.health = maxHealth = DEFAULT_MAX_HEALTH;
		loadSprite();
		followers = new Array<>();
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
				damage(maxHealth/(2*DEFAULT_MAX_HEALTH), DamageType.FIRE);
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
		animation.setFrames(reg, delay, facingLeft);
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
			create();
		} else {
			System.out.println(layer);
			fdef.filter.maskBits = (short) (layer | Vars.BIT_GROUND | Vars.BIT_PROJECTILE);
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
	public Vector2 getPixelPosition(){ return new Vector2(body.getPosition().x*Vars.PPM, body.getPosition().y*Vars.PPM); }
	public Body getBody() { return body; }

	public Script getScript() { return script; }
	public String toString(){ return ID; }
	
	public void changeDirection(){
		if (facingLeft) facingLeft = false;
		else facingLeft = true;

		animation.flip(facingLeft);
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
		if(!dead)
			if(!invulnerable){
				main.playSound(getPosition(), "damage");
				health -= val*resistance;
				
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
				else;
					//shake
			}
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
			if(dx > 0 && facingLeft) changeDirection();
			else if (dx < 0 && !facingLeft) changeDirection();
		}
	}
	
	public void faceObject(Entity obj){
		if (obj != null)
			if(obj.getBody() != null){
				float dx = obj.getPosition().x - getPosition().x;
				if(dx > 0 && facingLeft) changeDirection();
				else if (dx < 0 && !facingLeft) changeDirection();
			}
	}
	
	public boolean getDirection(){ return facingLeft; }
	

	protected void setDimensions(){
		setDimensions(getWidth(ID), getHeight(ID));
	}
	
	protected void setDimensions(int width, int height){
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
						e.getPosition().equals(getPosition()) && e.facingLeft==facingLeft && 
						e.animation.equals(animation))
					return true;
			} else
				return super.equals(o);
		}
		return false;
	}
	
	public void create(){
		//hitbox
		PolygonShape shape = new PolygonShape();
		shape.setAsBox((rw)/PPM, (rh)/PPM);
		
		bdef.position.set(x/PPM, y/PPM);
		bdef.type = BodyType.StaticBody;
		fdef.shape = shape;
		body = world.createBody(bdef);
		body.setUserData(this);
		fdef.filter.maskBits = (short) (layer | Vars.BIT_GROUND | Vars.BIT_PROJECTILE);
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
}
