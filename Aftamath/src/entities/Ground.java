package entities;

import static handlers.Vars.PPM;
import handlers.Vars;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.World;

public class Ground extends Entity{
	
	private static final int TILE_SIZE = 16;
	private String type;

	public Ground(World world, String type, float x, float y){
		width = height = TILE_SIZE;
		this.x = x;
		this.y = y;
		this.world = world;
		create();
		
		this.type = type;
		ID="ground";
	}
	
	public String getType(){ return type; }
	
	public void create(){
		init = true;
		bdef.type = BodyType.StaticBody;
		bdef.position.set(x, y);
		
		ChainShape cs = new ChainShape();
		Vector2[] v = new Vector2[5];
		
		v[0] = new Vector2(-TILE_SIZE / 2 / PPM, -TILE_SIZE / 2 / PPM);
		v[1] = new Vector2(-TILE_SIZE / 2 / PPM, TILE_SIZE / 2 / PPM);
		v[2] = new Vector2(TILE_SIZE / 2 / PPM, TILE_SIZE / 2 / PPM);
		v[3] = new Vector2(TILE_SIZE / 2 / PPM, -TILE_SIZE / 2 / PPM);
		v[4] = new Vector2(-Vars.TILE_SIZE / 2 / Vars.PPM, -Vars.TILE_SIZE / 2 / Vars.PPM);
		cs.createChain(v);
		
		fdef.friction = .25f;
		fdef.shape = cs;
		fdef.filter.categoryBits = Vars.BIT_GROUND;
		fdef.filter.maskBits = Vars.BIT_LAYER1 | Vars.BIT_PLAYER_LAYER | Vars.BIT_LAYER3 | Vars.BIT_BATTLE;
		fdef.isSensor = false;
		body = world.createBody(bdef);
		body.createFixture(fdef).setUserData("ground");
		body.setUserData(this);
	}
	
	public boolean equals(Object o){
		if(o instanceof Entity){
			Entity e = (Entity) o;
			if (e instanceof Ground)
				return (x==e.x && y==e.y) ; 
		}
		return false;
	}
}
