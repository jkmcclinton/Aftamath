package handlers;

import main.Play;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

public class RefocusTrigger {

	public float x, y, zoom, width, height;
	
	private String focus;
	private Body body;
	private BodyDef bdef = new BodyDef();
	private FixtureDef fdef = new FixtureDef();
	private Play play;
	private World world;
	private Camera camera, b2dCam;
	
	public RefocusTrigger(World world, Play play, float x, float y, 
			float w, float h, float zoom, String focus){
		this.x = x;
		this.y = y;
		this.width = w;
		this.height = h;
		this.world = world;
		this.zoom = zoom;
		this.focus = focus;
		camera = play.getCam();
		b2dCam = play.getB2dCam();
		this.play = play; 
		
		create();
	}
	
	public void trigger(){
		camera.setTrigger(this);
		camera.zoom(zoom);
		if(camera.getCharacter()==null)
			camera.setCharacter(play.player);
		if(b2dCam.getCharacter()==null)
			b2dCam.setCharacter(play.player);
		
		b2dCam.setTrigger(this);
		b2dCam.instantZoom(zoom);
		
		if (Vars.isNumeric(focus)){
			String[] tmp = focus.split(" ");
			camera.setFocus(new Vector2(Float.parseFloat(tmp[0]), Float.parseFloat(tmp[1])));
			b2dCam.setFocus(new Vector2(Float.parseFloat(tmp[0]), Float.parseFloat(tmp[1])));
		}else{
			if (focus.equals("player")){
				camera.removeFocus();
				b2dCam.removeFocus();
			}
		}
	}

	public void create() {
		PolygonShape shape = new PolygonShape();
		shape.setAsBox((width/2-2)/Vars.PPM, (height/2)/Vars.PPM);

		bdef.position.set(x/Vars.PPM, y/Vars.PPM);
		bdef.type = BodyType.KinematicBody;
		fdef.shape = shape;
		
		fdef.isSensor = true;
		body = world.createBody(bdef);
		body.setUserData(this);
		fdef.filter.maskBits = (short) ( Vars.BIT_GROUND | Vars.BIT_PROJECTILE| Vars.BIT_LAYER1| Vars.BIT_LAYER2| Vars.BIT_LAYER3);
		fdef.filter.categoryBits = (short) ( Vars.BIT_GROUND | Vars.BIT_PROJECTILE| Vars.BIT_LAYER1| Vars.BIT_LAYER2| Vars.BIT_LAYER3);
		body.createFixture(fdef).setUserData(Vars.trimNumbers("refocusTrigger"));
	}

}
