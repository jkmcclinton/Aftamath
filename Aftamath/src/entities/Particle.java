package entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import box2dLight.PointLight;
import handlers.Animation;
import handlers.FadingSpriteBatch;
import handlers.Vars;
import main.Game;
import main.Main;

public class Particle  {

	private PointLight light;
	private float t, lifeTime;
	private int w, h, dw, dh;
	private Animation anim;
	private String type;
	private Vector2 spawnLoc;
	private Main main;
	private boolean fade = true;
	
	protected Particle(Main m, float x, float y, /*float ow, float oh,*/ String ID, boolean direction) {
		type = ID.trim().toLowerCase();
		spawnLoc = new Vector2(x, y);
		this.main = m;
		float distance = 100f;
		float delay = Vars.ACTION_ANIMATION_RATE/2f;
		Color lightColor = null;
//		x += ow/2f;
		
		//this allows certain particles to have a looping animation
		boolean customDuration = false;
		
		if(!determineSize()) return;
		//draw size
		dw = w; dh = h;
		
		switch(ID){
		case "rock_break":
			break;
		case "dissipation":
			lightColor = new Color(Vars.SUNSET_GOLD); lightColor.a =.5f;
			distance = 100;
			break;
		case "embers":
			lightColor = new Color(Vars.SUNSET_ORANGE); lightColor.a =.5f;
			distance = 75;
			break;
		case "ice_shards":
			lightColor = new Color(Color.CYAN); lightColor.a =.5f;
			distance = 25;
			break;
		case "debris":
			break;
		case "spell_fizz":
			lightColor = new Color(Color.GREEN); lightColor.a =.5f;
			distance = 75;
			break;
		}
		
		Texture texture = Game.res.getTexture(type);
			TextureRegion[] frames;
			
		//since the object relies solely on the image, do not create particle if image is invalid 
		if(texture!=null){
			frames = TextureRegion.split(texture, w, h)[0];
			if(!customDuration) lifeTime = frames.length * delay;
			if(lightColor!=null)
				light = new PointLight(main.getRayHandler(), Vars.LIGHT_RAYS, lightColor, distance, x, y);
			anim = new Animation(null);
			anim.initFrames(frames, delay, direction);
			main.addParticle(this);
		}
	}
	
	public void update(float dt){
		anim.update(dt);
		t += dt;
		if(fade && light!=null){ 
			Color c = new Color(Color.BLACK);
			c.a = light.getColor().a;
			light.setColor(Vars.blendColors(t, 0, lifeTime, light.getColor(), c));
		}
		
		if(t > lifeTime) kill();
	}

	public void render(FadingSpriteBatch sb){
		Color overlay = sb.getColor();
		if(light!=null) sb.setColor(Vars.DAY_OVERLAY);	
		
		Vector2 loc = new Vector2(spawnLoc.x - dw/2, spawnLoc.y - dw/2);
		sb.draw(anim.getFrame(), loc.x, loc.y, dw, dh);
		sb.setColor(overlay);
	}
	
	public void kill(){
		if(light!=null) light.remove();
		main.removeParticle(this);
	}
	
	/**
	 * determine the pixel size of the particles
	 */
	public boolean determineSize(){
		boolean valid = true;
		Texture src = Game.res.getTexture(type+"base");
		if(src==null)
			src = Game.res.getTexture(type);
		if(src==null)
			valid = false;
		else {
			w = src.getWidth();
			h = src.getHeight();
		}
		
		return valid;
	}
	
	public PointLight getLight(){ return light; }

	public void dispose() {
		anim.dispose();
	}

}
