package entities;

import static handlers.Vars.PPM;

import java.util.HashMap;

import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.PolygonShape;

import handlers.Vars;

public class Barrier extends Entity {
	
	private Polygon polygon;
	private String type;

	public Barrier(float x, float y, int width, int height, String ID) {
		this.ID = "barrier"+ID;
		this.x = x;
		this.y = y;
		isAttackable = false;
		type = "concrete";
		
		setDimensions(width, height);
		loadSprite();
		followers = new HashMap<>();
	}
	
	//create with a polygon
	public Barrier(Polygon polygon, String ID){
		this.polygon = polygon;
		this.ID = "barrier"+ID;
		isAttackable = false;
		
//		polygon.setScale(.5f, .5f);
		setDimensions(polygon.getBoundingRectangle().width, 
				polygon.getBoundingRectangle().height);
		this.x = polygon.getX() + width;
		this.y = polygon.getY() + height;
		
//		System.out.println("polyX: "+polygon.getX());
		
		loadSprite();
		followers = new HashMap<>();
	}
	
	public void update(float dt){
		super.update(dt);
//		if(polygon!=null) System.out.println("BarrierLoc: "+getPixelPosition());
	}
	
	public void setType(String type){this.type = type; }
	public String getType(){ return type; }
	
	public void create(){
		init = true;
		//hitbox
		PolygonShape shape = new PolygonShape();
		if(polygon!=null){
			float vertices[] = polygon.getTransformedVertices();
			
//			System.out.print("PVs: ");
			for(float f : polygon.getVertices()){
//				System.out.print(f+", ");
				f=f/PPM;
			}
//			System.out.print("\nPTFVs: ");
			for(float f : vertices){
//				System.out.print(f+", ");
				f=f/PPM;
			}
//			System.out.println();
			shape.set(vertices);
		}else
			shape.setAsBox((rw)/PPM, (rh)/PPM);
		
		bdef.position.set(x/PPM, y/PPM);
		bdef.type = BodyType.StaticBody;
		fdef.shape = shape;
		
		fdef.friction = .25f;
		fdef.filter.categoryBits = Vars.BIT_GROUND;
		fdef.filter.maskBits = Vars.BIT_LAYER1 | Vars.BIT_PLAYER_LAYER | Vars.BIT_LAYER3 | Vars.BIT_BATTLE;
		fdef.isSensor = false;
		body = world.createBody(bdef);
		body.createFixture(fdef).setUserData("barrier");
		body.setUserData(this);
	}
}
