package scenes;

import static handlers.Vars.PPM;
import handlers.Entity;
import handlers.Vars;

import java.util.ArrayList;

import main.Game;
import states.GameState;
import states.Play;
import box2dLight.PointLight;
import box2dLight.RayHandler;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

import entities.Ground;
import entities.Mob;
import entities.Player;

public abstract class Scene {

	private Scene previous;
	private Scene next;
	protected ArrayList<Entity> entities;
	protected ArrayList<PointLight> lights;
	public Script script;
	public int width;
	public int height;
	public String title;
	
	protected TiledMap tileMap;
	protected Texture background;
	protected Texture foreground;
	private OrthogonalTiledMapRenderer tmr, tmr2;
	protected static Vector2 gravity;
	public Music DEFAULT_SONG;
	
	protected World world;
	protected RayHandler rayHandler;
	protected static BodyDef bdef = new BodyDef();
	protected static FixtureDef fdef = new FixtureDef();
	
	protected Scene(World world, Scene previous, Scene next, String bg, String fg, String ID, Play p, Player player, int w, int h) {
		this.world = world;
		this.previous = previous;
		this.next = next;
		
		entities = new ArrayList<>();
		lights = new ArrayList<>();
		title = ID;
		script = new Script(ID, p, player, null);
		width = w;
		height = h;
		
		tileMap = new TmxMapLoader().load("res/maps/untitled.tmx");
		//tileMap = new TmxMapLoader().load("res/maps/" + title + ".tmx");
		tmr = new OrthogonalTiledMapRenderer(tileMap, p.getSpriteBatch());
		background = new Texture(Gdx.files.internal("res/images/scenes/" + bg + ".png"));
		//foreground = new Texture(Gdx.files.internal(fg));
	}
	
	public void renderBG(SpriteBatch sb){
		sb.disableBlending();
		sb.begin();
			sb.draw(background, 0, 0);
		sb.end();
		sb.enableBlending();
	}
	
	public void renderEnvironment(OrthographicCamera cam){
		tmr.setView(cam);
		tmr.render();
	}
	
	public void renderFG(SpriteBatch sb, OrthographicCamera cam){
		sb.begin();
			sb.draw(foreground, 0, 0);
		sb.end();
		
		tmr.setView(cam);
		tmr.render();
	}
	
	public void renderFG(OrthographicCamera cam){
		tmr2.setView(cam);
		tmr2.render();
	}
	
	
	public ArrayList<Entity> getInitEntities() { return entities; }
	
	public Scene getPrev() { return previous; }
	public Scene getNext() { return next; }
	//public void setPrev(Scene p) { previous = p;}
	//public void setNext(Scene n) { next = n; }
	
	public Vector2 getGravity() { return gravity; }
	
	public TiledMap getTileMap(){
		return tileMap;
	}
	
	public Mob getMob(String name) {
		for (Entity e : entities)
			if(e instanceof Mob){
				Mob m = (Mob) e;
				if(m.getName().equals(name))
					return m;
			}
		return null;
	}

	@SuppressWarnings("unused")
	public void create(Play p) {
		Ground g;
		TiledMapTileLayer layer = (TiledMapTileLayer) tileMap.getLayers().get("ground");
		
		for (int y = 0; y < layer.getHeight(); y++)
			for(int x = 0; x < layer.getWidth(); x++){
				Cell cell = layer.getCell(x, y);
				
				if (cell == null) continue;
				if (cell.getTile() == null) continue;

				g = new Ground(world, "ground", (x + .5f) * Ground.TILE_SIZE / PPM,
						(y + .5f) * Ground.TILE_SIZE / PPM);
			}
		
		//lights
		int distance = 10000;
		Color color = Color.YELLOW;
		lights.add(new PointLight(rayHandler, Vars.LIGHT_RAYS, color, distance, 0, 0 ));
		
		
		//left wall
		PolygonShape shape = new PolygonShape();
		shape.setAsBox(10 / PPM, Game.height / 2 / PPM);
		bdef.position.set(0, 3 * Game.height / 4 / PPM);
		Body body = world.createBody(bdef);
		fdef.shape = shape;
		fdef.filter.categoryBits = Vars.BIT_GROUND;
		fdef.filter.maskBits = Vars.BIT_LAYER1 | Vars.BIT_LAYER2 | Vars.BIT_LAYER3 | Vars.BIT_PROJECTILE;
		body.createFixture(fdef).setUserData("wall");

		//right wall
		bdef.position.set(Game.width / PPM, 3 * Game.height / 4 / PPM);
		body = world.createBody(bdef);
		fdef.filter.categoryBits = Vars.BIT_GROUND;
		fdef.filter.maskBits = Vars.BIT_LAYER1 | Vars.BIT_LAYER2 | Vars.BIT_LAYER3 | Vars.BIT_PROJECTILE;
		body.createFixture(fdef).setUserData("wall");
		
		layer = (TiledMapTileLayer) tileMap.getLayers().get("foreground");
		
		for (int y = 0; y < layer.getHeight(); y++)
			for(int x = 0; x < layer.getWidth(); x++){
				Cell cell = layer.getCell(x, y);
				
				if (cell == null) continue;
				if (cell.getTile() == null) continue;
			}
	}

	public String getType() {
		return null;
	}

	public void setRayHandler(RayHandler rayHandler) {
		this.rayHandler = rayHandler;
	}
}
