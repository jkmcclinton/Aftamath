package scenes;

import static handlers.Vars.PPM;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.PolylineMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Polyline;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

import box2dLight.ConeLight;
import box2dLight.Light;
import box2dLight.PointLight;
import box2dLight.RayHandler;
import entities.Barrier;
import entities.Entity;
import entities.Ground;
import entities.LightObj;
import entities.Mob;
import entities.MobAI.ResetType;
import entities.Path;
import entities.Spawner;
import entities.SpeechBubble;
import entities.Warp;
import handlers.Camera;
import handlers.Evaluator;
import handlers.EventTrigger;
import handlers.FadingSpriteBatch;
import handlers.Pair;
import handlers.PositionalAudio;
import handlers.TextTrigger;
import handlers.Vars;
import main.Game;
import main.Main;
import scenes.Script.ScriptType;

public class Scene {
	
	// global mapping of scenes to the entities they contain
	private static Map<String, Set<Integer>> sceneToEntityIds;
	
	public Script loadScript;
	public int width, height;
	public String title, ID;
	public Song[] DEFAULT_SONG;
//	public boolean newSong; //tells us whether or not to fade music when loading scene from previous scene
	public boolean outside, limitPower; //controls whether or not to step weather time or to add the day/night cycle effect
	public Color ambientLight, ambientOverlay;
	
	private float groundLevel;
	private Main main;
	private FadingSpriteBatch sb;
	private OrthogonalTiledMapRenderer tmr;
	private Vector2 spawnpoint, localSpawnpoint, camBotSpawnpoint;
	private ArrayList<Entity> entities;
	private ArrayList<LightObj> lights;
	private ArrayList<DynamicTile> tiles;
	private ArrayList<Path> paths;
	private HashMap<Script, Pair<String, Boolean>> conditionalScripts;
	private HashMap<Mob, String> pathsToAdd;
	private HashMap<Mob, String> fociToAdd;
	private TiledMap tileMap;
	private Texture background, midground, foreground, clouds, sky, grad, sun, moon;
	private World world;
	private RayHandler rayHandler;
	private boolean tileBG = true;
	
	private static Vector2 gravity;
	private static BodyDef bdef = new BodyDef();
	private static FixtureDef fdef = new FixtureDef();
	
	private static final int DAY = 0;
	private static final int NOON = 1;
	private static final int NIGHT = 2;
	
	static {
		sceneToEntityIds = new HashMap<String, Set<Integer> >();
	}
	
	public static void addEntityMapping(String sceneID, int entityID) {
		if (!sceneToEntityIds.containsKey(sceneID)) {
			sceneToEntityIds.put(sceneID, new HashSet<Integer>());
		}
		sceneToEntityIds.get(sceneID).add(entityID);
	}
	
	public static void removeEntityMapping(String sceneID, int entityID) {
		if (!sceneToEntityIds.containsKey(sceneID)) {
			sceneToEntityIds.put(sceneID, new HashSet<Integer>());
			return;
		}
		if (sceneToEntityIds.get(sceneID).contains(entityID)) {
			sceneToEntityIds.get(sceneID).remove(entityID);
		}
	}
	
	public static void switchEntityMapping(String srcSceneID, String dstSceneID, int entityID) {
		removeEntityMapping(srcSceneID, entityID);
		addEntityMapping(dstSceneID, entityID);
	}
		
	public static void clearEntityMapping() {
		sceneToEntityIds.clear();
	}
	
	//these accessors are meant to be readonly - do not modify the return value directly, use the other methods
	public static Set<Integer> getEntityMapping(String sceneID) {
		if (!sceneToEntityIds.containsKey(sceneID)) {
			sceneToEntityIds.put(sceneID, new HashSet<Integer>());
		}
		return sceneToEntityIds.get(sceneID);
	}
	
	public static Map<String, Set<Integer>> getSceneToEntityMapping() {
		return sceneToEntityIds;
	}
	
	public String toString(){
		return title;
	}
	
	//temporary object used only for creating warps
	public Scene(String ID) throws InterruptedException{
		ID = ID.replaceAll(" ", "");
		this.ID=ID;
		
		Gdx.app.postRunnable(new Runnable() {
			public void run(){
				Scene.this.tileMap = new TmxMapLoader().load("assets/maps/" + Scene.this.ID + ".tmx");
				System.out.println("Lollipoop: "+Scene.this.tileMap);
			}
		});		

		Thread.sleep(100);
		System.out.println("Lollipoop: "+Scene.this.tileMap);
		MapProperties prop = tileMap.getProperties();
		width = prop.get("width", Integer.class)*Vars.TILE_SIZE;
		height = prop.get("height", Integer.class)*Vars.TILE_SIZE;
		title = prop.get("name", String.class);
		if(title==null) title = ID;
		
		//determine if scen is outside
		String light,s;
		if((light = prop.get("ambient", String.class))!=null){
			if(light.toLowerCase().trim().equals("daynight")){
				outside = true;
				if((s = prop.get("outside", String.class))!=null){
					if(Vars.isBoolean(s))
						outside = Boolean.parseBoolean(s);
				}
			} else {
				outside = false;
				if((s = prop.get("outside", String.class))!=null){
					if(Vars.isBoolean(s))
						outside = Boolean.parseBoolean(s);
				}
			}
		} else
			outside = false;
	}
	
	public Scene(World world, Main m, String ID) {
		this.world = world;
		this.main = m;
		sb = m.getSpriteBatch();
		DEFAULT_SONG = new Song[3];
		gravity = new Vector2(0, Vars.GRAVITY);

		entities = new ArrayList<>();
		lights = new ArrayList<>();
		paths = new ArrayList<>();
		tiles = new ArrayList<>();
		pathsToAdd = new HashMap<>();
		fociToAdd = new HashMap<>();
		conditionalScripts = new HashMap<>();
		width = 1000;
		height = 1000;

		title = ID.toLowerCase();
		this.ID = ID.replaceAll(" ", "");

		tileMap = new TmxMapLoader().load("assets/maps/" + this.ID + ".tmx");
		
		MapProperties prop = tileMap.getProperties();
		width = prop.get("width", Integer.class)*Vars.TILE_SIZE;
		height = prop.get("height", Integer.class)*Vars.TILE_SIZE;

		title = prop.get("name", String.class);
		if(title==null) title = ID;
				
		// Create Music
		String[] bgm = {"","",""};
		bgm[DAY] = prop.get("bgm day", String.class);
		bgm[NOON] = prop.get("bgm noon", String.class);
		bgm[NIGHT] = prop.get("bgm night", String.class);
		
		//all three must be clearly defined in .tmx file to have dynamic bgm
		if(bgm[DAY]!=null&&bgm[NOON]!=null&&bgm[NIGHT]!=null){
			DEFAULT_SONG[DAY] = new Song(bgm[DAY]);
			DEFAULT_SONG[NOON] = new Song(bgm[NOON]);
			DEFAULT_SONG[NIGHT] = new Song(bgm[NIGHT]);
		} else if(bgm[DAY]!=null&&bgm[NIGHT]!=null){
			DEFAULT_SONG[DAY] = new Song(bgm[DAY]);
			DEFAULT_SONG[NOON] = new Song(bgm[DAY]);
			DEFAULT_SONG[NIGHT] = new Song(bgm[NIGHT]);
			
		} else {
			if((bgm[0] = prop.get("bgm", String.class))!=null){
				//give level a random song according to type
				if(bgm[0].trim().toLowerCase().equals("elevator")){
					int i = (int)(Math.random() * ((2) + 1));
					if(i==0) bgm[0] = "Bossa Elevator";
					else bgm[0] = "Elevator Music";
				} if(bgm[0].trim().toLowerCase().equals("radio")){
					int i = (int)(Math.random() * ((Game.SONG_LIST.size - 1) + 1));
					bgm[0] = Game.SONG_LIST.get(i);
				}
				//sets all songs to the same song; no song changing
				DEFAULT_SONG[DAY] = new Song(bgm[0]);
				DEFAULT_SONG[NOON] = new Song(bgm[0]);
				DEFAULT_SONG[NIGHT] = new Song(bgm[0]);
			} else {
				//sets all songs to silence by default
				DEFAULT_SONG[DAY]=new Song(Game.SONG_LIST.get(0));
				DEFAULT_SONG[NOON]=new Song(Game.SONG_LIST.get(0));
				DEFAULT_SONG[NIGHT]=new Song(Game.SONG_LIST.get(0));
			}
		}
		
		//load the image for the backgrounds and forgrounds
		String src = prop.get("bg", String.class);
		background = Game.res.getTexture(src+"_bg");
		midground = Game.res.getTexture(src+"_mg");
		src = prop.get("fg", String.class);
		foreground = Game.res.getTexture(src+"_fg");
		
		clouds = Game.res.getTexture("clouds");
		sky = Game.res.getTexture("sky");
		grad = Game.res.getTexture("sky_grad");
		sun = Game.res.getTexture("sun");
		moon = Game.res.getTexture("moon");
		
		//set a local zoom for the camera
		String zoom; float areaZoom;
		if((zoom = prop.get("zoom", String.class))!=null){
			try{
				Field f = Camera.class.getField(zoom);
				areaZoom = f.getFloat(f);
			} catch (Exception e){ areaZoom = Camera.ZOOM_NORMAL; }
		} else
			areaZoom = Camera.ZOOM_NORMAL;
		main.getCam().setDefaultZoom(areaZoom);
		main.getB2dCam().setDefaultZoom(areaZoom);
		
		String bool;
		if((bool = prop.get("tileBG", String.class))!=null)
			if(Vars.isBoolean(bool))
				tileBG = Boolean.parseBoolean(bool);
		
		
		//determine ambient lighting and spritebatch overlay
		ambientLight = Vars.DAY_LIGHT;
		ambientOverlay = Vars.DAY_OVERLAY;
		try {
			String s;
			String light;
			if((light = prop.get("ambient", String.class))!=null){
				if(light.toLowerCase().trim().equals("daynight")){
					ambientLight = m.getAmbientLight();
					ambientOverlay = m.getColorOverlay();
					outside = true;
					if((s = prop.get("outside", String.class))!=null){
						if(Vars.isBoolean(s))
							outside = Boolean.parseBoolean(s);
					}

					sb.setOverlayDraw(true);
				} else {
					Field f = Vars.class.getField(light+"_LIGHT");
					ambientLight = (Color) f.get(f);
					
					f = Vars.class.getField(light+"_OVERLAY");
					ambientOverlay = (Color) f.get(f);
					outside = false;
					if((s = prop.get("outside", String.class))!=null){
						if(Vars.isBoolean(s))
							outside = Boolean.parseBoolean(s);
					}
				}
			}
			
			if(!outside) limitPower = true;
			if((bool = prop.get("limitPower", String.class))!=null)
				if(Vars.isBoolean(bool))
					limitPower = Boolean.parseBoolean(bool);
		} catch (Exception e){ }
		
		//get scripts
		Iterator<String> it = prop.getKeys();
		String script, condition;
		while(it.hasNext()){
			script = it.next();
			if(Game.res.getScript(script)==null)
				continue;
			condition = prop.get(script, String.class);
			conditionalScripts.put(new Script(script, ScriptType.SCENE, m, null),
					new Pair<String, Boolean>(condition, false));
		}
		
		loadScript = new Script(this.ID, ScriptType.SCENE, m, null);
		tmr = new OrthogonalTiledMapRenderer(tileMap, m.getSpriteBatch());
	}
	
	
	//trigger any conditional script stuff
	public void update(float dt){
		Evaluator e = new Evaluator(main);
		for(Script s : conditionalScripts.keySet())
			if(e.evaluate(conditionalScripts.get(s).getKey()) 
					&& !conditionalScripts.get(s).getValue()){
				main.triggerScript(conditionalScripts.get(s).getKey());
				conditionalScripts.get(s).setValue(true);
			}
	}
	
	public void renderBG(FadingSpriteBatch sb){
		Camera cam = main.getCam();
		float z = cam.zoom;
		float w = cam.viewportWidth;
		float h = cam.viewportHeight;
		
		Color color = sb.getColor();
		if(!sb.fading) sb.setColor(Vars.DAY_OVERLAY);

		//draw sky stuff
		sb.begin();
		//the reason why we do not use just the outside variable is because we might want to
		//have windows from the indoors
		if(background!=null || outside){
			if(sky!=null){
				float t = main.dayTime;
				float x = cam.position.x-w*z/2f;
				float y1 = cam.position.y-h*z/2f;
				float s = sky.getHeight()*z;
				float o = (sky.getHeight()*t*s)/Main.DAY_TIME;
				float y = y1-s*sky.getHeight()+o+z*h;

				if(main.dayTime<=Main.NIGHT_TIME) 
					sb.draw(sky, x, y-2*s*sky.getHeight(), w*z, s*sky.getHeight());
				sb.draw(sky, x,	y, w*z, s*sky.getHeight());
				sb.draw(grad, x, y1/2f, w*z, h*z+y1/2);
			} if(sun!=null){
				Vector2 o = new Vector2(0, -10f);
				float t = ((main.dayTime-Main.DAY_TIME/24f)%Main.DAY_TIME)*(w+sun.getWidth()/4f)*z/(2*Main.NIGHT_TIME);
				float b = z*h/2f;
				float d = 314*z/4f, rate = .94f;
				float a = -(b-d) / (float) Math.pow((z*w/4f),2);
				float x1 = 2*t - z*(w+sun.getWidth()/z)/2f + cam.position.x;
				float x = x1 - cam.position.x*rate - z*o.x;
				float y = a*x*x+ b + cam.position.y*rate + z*o.y - sun.getHeight()/2f;

//				float rot = (20*main.dayTime*(360/Main.DAY_TIME))%360;
				sb.draw(sun, x1, y);
			} if(moon!=null){
				Vector2 o = new Vector2(0, -10f);
				float u = Main.DAY_TIME/16.2222f;
				float t = (main.dayTime-Main.NIGHT_TIME-u)%Main.DAY_TIME*
						(w+moon.getWidth()/4f)*z/(2*Main.NOON_TIME);
				float b = z*h/2f;
				float d = 314*z/4f, rate = .94f;
				float a = -(b-d) / (float) Math.pow((z*w/4f),2);
				float x1 = 2*t - z*(w+moon.getWidth()/z)/2f + cam.position.x;
				float x = x1 - cam.position.x*rate - z*o.x - moon.getWidth()/2f;
				float y = a*x*x+ b + cam.position.y*rate + z*o.y - moon.getHeight()/2f;;

				sb.draw(moon, x1, y);
			}
		}

		sb.setColor(color);

		if(background!=null){
			float rate = 1/2f;
			int bgw = background.getWidth();
			int bgh = background.getHeight();
			if(tileBG){
				int offset = (int) ((cam.position.x*rate+bgw/2f)/(float) bgw);
				sb.draw(background, cam.position.x*rate+bgw*offset, groundLevel-bgh/3.3f + cam.position.y*rate);
				sb.draw(background, cam.position.x*rate+bgw*(offset-1), groundLevel-bgh/3.3f + cam.position.y*rate);
			} else 
				sb.draw(background, cam.position.x*rate, height-bgh + cam.position.y*rate); 
		} if(midground!=null){
			float rate = 1/4f;
			int mgw = midground.getWidth();
			int mgh = midground.getHeight();
			if(tileBG){
				int offset = (int) ((cam.position.x*rate*3+mgw/2f)/(float) mgw);
				sb.draw(midground, cam.position.x*rate+mgw*offset, groundLevel-mgh/2.6f + cam.position.y*5/16f);
				sb.draw(midground, cam.position.x*rate+mgw*(offset-1), groundLevel-mgh/2.6f + cam.position.y*5/16f);
				sb.draw(midground, cam.position.x*rate+mgw*(offset+1), groundLevel-mgh/2.6f + cam.position.y*5/16f);
			} else 
				sb.draw(midground, cam.position.x*rate, height-mgh + cam.position.y*5/16f);
		} if(outside){
			if(clouds!=null)
				sb.draw(clouds, 0, 0);		// horizontal mvmt and frequency according to weather 
		}
		sb.end();
	}
	
	public void renderEnvironment(OrthographicCamera cam, FadingSpriteBatch sb){
		tmr.setView(cam);
		ArrayList<Entity> objs = main.getObjects();
		try{
			//draw furthest background layer
			for(MapLayer t : tileMap.getLayers()){
				if(t.getName().equals("fg") || 
						!t.getName().equals("bg1"))
					t.setVisible(false);
				else t.setVisible(true);
			}
			tmr.render();
			
			//draw objects on special layer
			sb.begin();
			for(int i = objs.size()-1; i>=0; i--){
				if(objs.get(i).getLayer()==0) continue;
				Entity e = objs.get(i);
				if(e.getLayer()!=Vars.BIT_LAYERSPECIAL && 
						e.getLayer()!=Vars.BIT_LAYERSPECIAL2)
					break;
				e.render(sb);
			}
			sb.end();
			
			//draw rest of layers
			for(MapLayer t : tileMap.getLayers()){
				if(t.getName().equals("fg") || 
						t.getName().equals("bg1"))
					t.setVisible(false);
				else t.setVisible(true);
			}
			tmr.render();
		}catch(Exception e){ 
			e.printStackTrace();
		}
	}
	
	public void renderFG(SpriteBatch sb){
		sb.begin();
			if(foreground!=null)
				sb.draw(foreground, 0, 0);
//			sb.draw(weather,0,0);	//if raining or foggy
		sb.end();
		
		try{
			for(MapLayer t : tileMap.getLayers())
				if(t.getName().equals("fg")){
					t.setVisible(true);
				}
				else t.setVisible(false);
			
			tmr.render();
			
		}catch(Exception e){ 
			e.printStackTrace();
		}
	}
	
	public ArrayList<Path> getInitPaths() { return paths;  }
	public void applyRefs(){
		for(Mob m :pathsToAdd.keySet())
			m.moveToPath(pathsToAdd.get(m));
		for(Mob m:fociToAdd.keySet())
			m.getCurrentState().focus = main.findObject(fociToAdd.get(m));
	}
	
	public ArrayList<LightObj> getLights() {return lights;}
	public ArrayList<Entity> getInitEntities() { return entities; }
	public Vector2 getGravity() { return gravity; }
	public TiledMap getTileMap(){ return tileMap; }
	public Vector2 getSpawnPoint(){ return spawnpoint; }
	public void setSpawnpoint(Vector2 newSpawn){ spawnpoint = newSpawn; }
	public Vector2 getLocalSpawnpoint(){ return localSpawnpoint; }
	public Vector2 getCBSP() { return camBotSpawnpoint; }

	//pull all scene data from the Tiled file
	public void create() {
		//TODO if already created, don't create
		if(main.getHud()!=null) //that means we're no longer initializing the game
			main.getHud().showLocation();
		TiledMapTileLayer ground = (TiledMapTileLayer) tileMap.getLayers().get("ground");
		TiledMapTileLayer g2 = (TiledMapTileLayer) tileMap.getLayers().get("ledge");
		TiledMapTileLayer fg = (TiledMapTileLayer) tileMap.getLayers().get("fg");
		TiledMapTileLayer bg1 = (TiledMapTileLayer) tileMap.getLayers().get("bg1");
		TiledMapTileLayer bg2 = (TiledMapTileLayer) tileMap.getLayers().get("bg2");
		TiledMapTileLayer ob = (TiledMapTileLayer) tileMap.getLayers().get("objects");
		
		//add in entities loaded from save file
		for (int sid : Scene.getEntityMapping(this.ID)) {
			if (Entity.hasMapping(sid)) {
				Entity e = Entity.getMapping(sid);
				e.setGameState(main);
				this.entities.add(e);
			} else {
				System.out.println("Scene to entity IDs mapping has stale data, entity ID "+sid+" does not exist");
			}
		}
		
		//initialize ground tiles
		for (int y = 0; y < ground.getHeight(); y++)
			for(int x = 0; x < ground.getWidth(); x++){
				Ground g;
				Cell cell = ground.getCell(x, y);
				Vector2 location = new Vector2((x+.5f) * Vars.TILE_SIZE / Vars.PPM,
						y * Vars.TILE_SIZE / Vars.PPM);
				if (cell != null){
					if (cell.getTile() != null){
						groundLevel = (y+1)*Vars.TILE_SIZE;
						String type = "";
						if(cell.getTile().getProperties().get("type")!=null)
							type = cell.getTile().getProperties().get("type", String.class);
						g = new Ground(world, type, (x * Vars.TILE_SIZE +7.1f) / PPM,
								(y * Vars.TILE_SIZE+Vars.TILE_SIZE/3.6f) / PPM);
						g.setGameState(main);
						if(cell.getTile().getProperties().get("light")!=null) 
							initLight(cell, x, y);
					}
				}

				if(g2!=null){
					cell = g2.getCell(x, y);
					if (cell != null)
						if (cell.getTile() != null) {
							String type = "";
							if(cell.getTile().getProperties().get("type")!=null)
								type = cell.getTile().getProperties().get("type", String.class);
							g = new Ground(world, type, (x * Vars.TILE_SIZE +7.1f) / PPM,
									(y * Vars.TILE_SIZE+Vars.TILE_SIZE/ /*1.8f*/ 3.6f) / PPM);
							g.setGameState(main);
							
							if(cell.getTile().getProperties().get("light")!=null) 
								initLight(cell, x, y);
						}
				}
				
				//collect foreground and background sounds and dynamic tiles???
				cell = fg.getCell(x, y);
				if (cell != null)
					if (cell.getTile() != null) {
						Object b = cell.getTile().getProperties().get("sound");
						if(b!=null){
							String src = cell.getTile().getProperties().get("sound", String.class);
							if(src!=null) new PositionalAudio(new Vector2(location.x, location.y), src, main);
						}
						
						b = cell.getTile().getProperties().get("light");
						if(b!=null) initLight(cell, x, y);
						
						b = cell.getTile().getProperties().get("dynamic");
						if(b!=null)
							tiles.add(new DynamicTile(cell, main));
					}
							
				cell = bg1.getCell(x, y);
				if (cell != null) 
					if (cell.getTile() != null) {
						Object b = cell.getTile().getProperties().get("sound");
						if(b!=null){
							String src = cell.getTile().getProperties().get("sound", String.class);
							if(src!=null) new PositionalAudio(new Vector2(location.x, location.y), src, main);
						}

						if(cell.getTile().getProperties().get("light")!=null) 
							initLight(cell, x, y);
						b = cell.getTile().getProperties().get("dynamic");
						if(b!=null)
							tiles.add(new DynamicTile(cell, main));
					}

				if(bg2!=null){
					cell = bg2.getCell(x, y);
					if (cell != null) 
						if (cell.getTile() != null) {
							Object b = cell.getTile().getProperties().get("sound");
							if(b!=null){
								String src = cell.getTile().getProperties().get("sound", String.class);
								if(src!=null) new PositionalAudio(new Vector2(location.x, location.y), src, main);
							}

							if(cell.getTile().getProperties().get("light")!=null) 
								initLight(cell, x, y);
							b = cell.getTile().getProperties().get("dynamic");
							if(b!=null)
								tiles.add(new DynamicTile(cell, main));
						}
				}

				if(ob!=null){
					cell = ob.getCell(x, y);
					if (cell != null) 
						if (cell.getTile() != null) {
							Object b = cell.getTile().getProperties().get("sound");
							if(b!=null){
								String src = cell.getTile().getProperties().get("sound", String.class);
								if(src!=null) new PositionalAudio(new Vector2(location.x, location.y), src, main);
							}

							if(cell.getTile().getProperties().get("light")!=null) 
								initLight(cell, x, y);
							b = cell.getTile().getProperties().get("dynamic");
							if(b!=null)
								tiles.add(new DynamicTile(cell, main));
						}
				}
			}
		
		if(outside)
			initSpawner();

		if(tileMap.getLayers().get("entities")!=null){
			MapObjects objects = tileMap.getLayers().get("entities").getObjects();
			for(MapObject object : objects) {

				String loadCondition = object.getProperties().get("load Condition", String.class);
				if(loadCondition==null) loadCondition = object.getProperties().get("loadCondition", String.class);
				boolean load = true;
				if(loadCondition!=null)
					load = main.evaluator.evaluate(loadCondition);

				if(load){
					if (object instanceof RectangleMapObject) {
						Rectangle rect = ((RectangleMapObject) object).getRectangle();
						rect.set(rect.x+7.1f, rect.y-Vars.TILE_SIZE*(9f/80f), rect.width, rect.height);

						//this indicaties the base spawn location
//					entities.add(new Entity(rect.x,rect.y,(int)rect.width,(int)rect.height,"TiledObject"));

						Object o = object.getProperties().get("NPC");
						if(o!=null) {
							String l = object.getProperties().get("layer", String.class);		//render/collision layer
							String ID = object.getProperties().get("NPC", String.class);		//name used for art file
							String sceneID = object.getProperties().get("ID", String.class);	//unique int ID across scenes
							String name = object.getProperties().get("name", String.class);		//character name
							String nickName = object.getProperties().get("nickName", String.class);
							String iff =  object.getProperties().get("iff", String.class);      //iff tag
							if(nickName==null) nickName = object.getProperties().get("nickname", String.class);
							String state = object.getProperties().get("state", String.class);	//AI state
							String focus = object.getProperties().get("focus", String.class);
							String script = object.getProperties().get("script", String.class);
							String aScript = object.getProperties().get("attackScript", String.class);
							String sScript = object.getProperties().get("supSttackScript", String.class);
							String dScript = object.getProperties().get("discoverScript", String.class);
							String dType = object.getProperties().get("onSight", String.class);
							String aType = object.getProperties().get("onAttacked", String.class);
							String pathName = object.getProperties().get("path", String.class);
							String powerType = object.getProperties().get("powerType", String.class);

							if(l!=null) {
								if (l.toLowerCase().equals("back")) 
									l = "3";
								else if (l.toLowerCase().equals("player")) 
									l = "2";
								else if(!l.toLowerCase().contains("special"))
									l = "1";
							} else l = "3";

							try {
								short lyr;
								if (l.equals("2")) {
									lyr = Vars.BIT_PLAYER_LAYER;
								} else {
									Field f = Vars.class.getField("BIT_LAYER"+l.toUpperCase());
									lyr = f.getShort(f);
								}
								ID = ID.toLowerCase();
								int sceneIDParsed = Integer.parseInt(sceneID);

								if (sceneID==null)
									System.out.println("'" + name + "', '"+ID+"' cannot be created without a sceneID!");
								else if (Entity.hasMapping(sceneIDParsed) && sceneIDParsed>0) {
									Entity c = Entity.getMapping(sceneIDParsed);
									boolean conflict = true;

									if(c instanceof Mob)
										if(((Mob)c).getName().equals(name) && c.ID.equals(ID))
											conflict = false;

									if(conflict){
										System.out.print("'" + name + "', '"+ID+"' cannot be created; ");
										if(c instanceof Mob)
											System.out.println("'" + ((Mob)c).getName() + "' already exists with ssceneID " + sceneID);
										else
											System.out.println("'" + c.ID + "' already exists with sceneID " + sceneID);
									}
									//ignore since object was already created via save file
								} else{
									Mob e = new Mob(name, ID, sceneIDParsed, rect.x, rect.y, lyr);
									e.setGameState(main);
									e.setState(state, null, -1, ResetType.NEVER.toString());
									e.setDialogueScript(script);
									e.setAttackScript(aScript);
									e.setSupAttackScript(sScript);
									e.setDiscoverScript(dScript);
									e.setResponseType(dType);
									e.setAttackType(aType);
									e.setPowerType(powerType);
									e.active = true;
									entities.add(e);

									if(pathName!=null)
										pathsToAdd.put(e, pathName);
									if(focus!=null) 
										fociToAdd.put(e, focus);
									if(nickName!=null)
										e.setNickName(nickName);
									if(iff!=null)
										e.setIFF(iff);

									if(sceneIDParsed>=0){
										Scene.addEntityMapping(this.ID, sceneIDParsed);
										Entity.addMapping(sceneIDParsed, e);
									}
								}

							} catch (NoSuchFieldException | SecurityException e) {
								e.printStackTrace();
							} catch (IllegalArgumentException e) {
								e.printStackTrace();
							} catch (IllegalAccessException e) {
								e.printStackTrace();
							} catch(NullPointerException e){
							}
						}
						
						o = object.getProperties().get("Entity");
						if(o==null) o = object.getProperties().get("entity");
						if(o!=null) {
							String l = object.getProperties().get("layer", String.class);		//render/collision layer
							String ID = object.getProperties().get("entity", String.class);		//name used for art file
							if(ID==null) ID = object.getProperties().get("Entity", String.class);
							String sceneID = object.getProperties().get("ID", String.class);	//unique int ID across scenes
//							String focus = object.getProperties().get("focus", String.class);
							String script = object.getProperties().get("script", String.class);
							String aScript = object.getProperties().get("attackScript", String.class);
							String sScript = object.getProperties().get("supSttackScript", String.class);
							String cDimStr = object.getProperties().get("entity", String.class);
							String ad = object.getProperties().get("ad", String.class);;
							boolean cDim = false;
							if(cDimStr!=null)
								if(Vars.isBoolean(cDimStr))
									cDim = Boolean.parseBoolean(cDimStr);

							if(l!=null) {
								if (l.toLowerCase().equals("back")) 
									l = "3";
								else if (l.toLowerCase().equals("player")) 
									l = "2";
								else if(!l.toLowerCase().contains("special"))
									l = "1";
							} else l = "3";

							try {
								short lyr;
								if (l.equals("2")) {
									lyr = Vars.BIT_PLAYER_LAYER;
								} else {
									Field f = Vars.class.getField("BIT_LAYER"+l.toUpperCase());
									lyr = f.getShort(f);
								}
								int sceneIDParsed = Integer.parseInt(sceneID);

								if (sceneID==null)
									System.out.println("'"+ID+"' cannot be created without a sceneID!");
								else if (Entity.hasMapping(sceneIDParsed)&& sceneIDParsed>=0) {
									Entity c = Entity.getMapping(sceneIDParsed);
									boolean conflict = true;

									//this is the same object we're looking at, therefore no conflict
									if(c.ID.equals(ID))
										conflict = false;

									if(conflict){
										System.out.print("'"+ID+"' cannot be created; ");
										if(c instanceof Mob)
											System.out.println("'" + ((Mob)c).getName() + "' already exists with ssceneID " + sceneID);
										else
											System.out.println("'" + c.ID + "' already exists with sceneID " + sceneID);
									}
									//ignore since object was already created via save file
								} else {
									Entity e;
									
									if(ad!=null)
										switch(ad.trim().toLowerCase()){
										case"subway":
											ID += ""+ ((int) (Math.random()*2 + 1));
											break;
										}
									
									if(cDim) e = new Entity(rect.x, rect.y+rect.height/2, rect.width, rect.height, ID);
									else e = new Entity(rect.x, rect.y+Mob.getHeight(ID)/2f, ID);
									e.setSceneID(sceneIDParsed);
									e.setGameState(main);
									e.setDialogueScript(script);
									e.setAttackScript(aScript);
									e.setSupAttackScript(sScript);
									e.changeLayer(lyr);
									e.active = true;
									entities.add(e);

									if(sceneIDParsed>=0){
										Scene.addEntityMapping(this.ID, sceneIDParsed);
										Entity.addMapping(sceneIDParsed, e);
									}
								}
							} catch (NoSuchFieldException | SecurityException e) {
								e.printStackTrace();
							} catch (IllegalArgumentException e) {
								e.printStackTrace();
							} catch (IllegalAccessException e) {
								e.printStackTrace();
							} catch(Exception e){
							}
						}

						if(object.getProperties().get("barrier")!=null) {
							String id = object.getProperties().get("ID", String.class);
//							String type = object.getProperties().get("type", String.class);
							Entity e = new Barrier(rect.x, rect.y+rect.height/2, 
									(int)rect.width, (int)rect.height, id);
							e.active = false;
							entities.add(e);
						}

						if(object.getProperties().get("spawn")!=null) {
							spawnpoint = new Vector2(rect.x, rect.y);
							localSpawnpoint = new Vector2(rect.x, rect.y);
						}

						if(object.getProperties().get("cambot spawn")!=null) {
							camBotSpawnpoint = new Vector2(rect.x, rect.y);
						}

						// pull warp from existing warps
						if(object.getProperties().get("warp")!=null) {
							String id = object.getProperties().get("ID", String.class);
							int warpID = Integer.parseInt(id);
							Warp w = main.findWarp(ID, warpID);
							if(w!=null){ //should only happen if main.cwarps is false
//								w.setOwner(this);
								w.active = false;
								entities.add(w);
							}
						}

						if(object.getProperties().get("event")!=null){
							Iterator<String> it = object.getProperties().getKeys();
							EventTrigger et = new EventTrigger(main, rect.x+rect.width/2-7.1f, 
									rect.y, rect.width, rect.height);
							String script, condition, s;

							while(it.hasNext()){
								script = it.next();
								//ignore invalid script names
								if(Game.res.getScript(script)==null)
									continue;
								condition = object.getProperties().get(script, String.class);
								et.addEvent(script, condition);
							}

							if((s = object.getProperties().get("retriggerable", String.class))!=null){
								if(!s.isEmpty()){
									for(String sc : s.split(","))
										et.setRetriggerable(sc, true);
								} else
									et.setRetriggerable(true);
							} if((s = object.getProperties().get("avoidHalt", String.class))!=null){
								if(!s.isEmpty()){
									for(String sc : s.split(","))
										et.setHalt(sc, false);
								} else
									et.setHalt(false);
							}
						}
					} else if(object instanceof PolylineMapObject){
						String name = object.getProperties().get("name", String.class);
						String speed = object.getProperties().get("speed", String.class);
						String behavior = object.getProperties().get("behavior", String.class);
						Polyline pl =((PolylineMapObject) object).getPolyline();

						Array<Vector2> vertices = new Array<Vector2>();
						for(int i = 0; i<pl.getVertices().length-1; i+=2)
							vertices.add(new Vector2(pl.getVertices()[i]+pl.getX(), 
									pl.getVertices()[i+1]+pl.getY()));

						if(name!=null && behavior != null){
							Path p = new Path(name, behavior, vertices);
							paths.add(p);

							if(speed!=null)
								if(Vars.isNumeric(speed))
									p.setSpeed(Float.parseFloat(speed));
						} else {
							if(name==null)
								System.out.println("Path cannot be created without a name.");
							if(behavior==null)
								System.out.println("Path cannot be created without a behavior.");
						}
					} else if(object instanceof PolygonMapObject){
						if(object.getProperties().get("barrier")!=null) {
							String id = object.getProperties().get("ID", String.class);
							String type = object.getProperties().get("type", String.class);
							Barrier e = new Barrier(((PolygonMapObject)object).getPolygon(), id);
							e.active = false;
							
							if(type!=null)
								e.setType(type);
							entities.add(e);
						}
					}
				}
			}
		}
		
		//left wall
		float offset = 0;
		if(main.findWarp(ID, 0)==null) offset = 3*Vars.TILE_SIZE/PPM;
		PolygonShape shape = new PolygonShape();
		shape.setAsBox(Vars.TILE_SIZE / PPM, height/2 / PPM);
		bdef.position.set(offset, height/ 2 / PPM);
		Body body = world.createBody(bdef);
		fdef.shape = shape;
		fdef.filter.categoryBits = Vars.BIT_GROUND;
		fdef.filter.maskBits = Vars.BIT_LAYER1 | Vars.BIT_PLAYER_LAYER | Vars.BIT_LAYER3 | Vars.BIT_BATTLE;
		body.createFixture(fdef).setUserData("wall");

		//right wall
		offset = 0;
		if(main.findWarp(ID, 1)==null) offset = -3*Vars.TILE_SIZE/PPM;
		bdef.position.set(width / PPM+offset,  height/ 2 / PPM);
		body = world.createBody(bdef);
		fdef.filter.categoryBits = Vars.BIT_GROUND;
		fdef.filter.maskBits = Vars.BIT_LAYER1 | Vars.BIT_PLAYER_LAYER | Vars.BIT_LAYER3 | Vars.BIT_BATTLE;
		body.createFixture(fdef).setUserData("wall");
		
		if(Main.cwarps)entities.addAll(retrieveSideWarps());
	}
	
	//create a static/dynamic lighting object according to cell property
	private void initLight(Cell cell, float x, float y){
		String type = cell.getTile().getProperties().get("light", String.class);
		Light l = null; boolean scheduled = true; Color c;
		type = type.toLowerCase().trim();
		switch(type){
		case"street":
			l = new ConeLight(rayHandler, Vars.LIGHT_RAYS, Vars.SUNSET_GOLD, 
					200, (x*Vars.TILE_SIZE + 13f), (y*Vars.TILE_SIZE + 14), 270, 35);
			break;
		case"justice":
			c = new Color(Vars.SUNSET_GOLD);
			c.a = 200/255f;
			l = new ConeLight(rayHandler, Vars.LIGHT_RAYS, c, 
					200, (x*Vars.TILE_SIZE + 12f), (y*Vars.TILE_SIZE + 11), 270, 90);
			break;
//		case "poolIn
		case "pool":
			c = new Color(Vars.FROZEN_OVERLAY);
			c.a = 30/255f;
			l = new PointLight(rayHandler, Vars.LIGHT_RAYS, c,
					225, (x*Vars.TILE_SIZE + 8.1f), (y*Vars.TILE_SIZE + Vars.TILE_SIZE/3.6f));
			scheduled = false;
			break;
		case"justice_hall":
			c = new Color(207/255f, 236/255f, 255/255f, 15/255f);
			l = new PointLight(rayHandler, Vars.LIGHT_RAYS, c,
					225, (x*Vars.TILE_SIZE + 8.1f), (y*Vars.TILE_SIZE + Vars.TILE_SIZE/3.6f));
			scheduled = false;
			break;
		case "window":
			break;
		case"sewer":
			l = new ConeLight(rayHandler, Vars.LIGHT_RAYS, new Color(139/255f, 195/255f, 217/255f, Vars.ALPHA), 
					210, (x*Vars.TILE_SIZE + 8.1f), (y*Vars.TILE_SIZE + Vars.TILE_SIZE/3.6f), 270, 35);
			scheduled = false;
			break;
		case "boardwalk":
			l = new PointLight(rayHandler, Vars.LIGHT_RAYS, new Color(120/255f, 128/255f, 121/255f, 200/255f),
					275, (x*Vars.TILE_SIZE + 8.1f), (y*Vars.TILE_SIZE + Vars.TILE_SIZE/3.6f));
			break;
		case "spotlight":
			l = new ConeLight(rayHandler, Vars.LIGHT_RAYS, new Color(120/255f, 128/255f, 121/255f, 200/255f), 
				275, (x*Vars.TILE_SIZE + 8.1f), (y*Vars.TILE_SIZE + Vars.TILE_SIZE/3.6f), 90, 35);
			break;
		}

		if(l!=null)
			lights.add(new LightObj(type, l, scheduled));
	}
	
	//these spawners are placed at both ends of the level and spawn civilians who walk left and right
	private void initSpawner(){
		// TODO get map properties relating to spawner
		MapProperties prop = tileMap.getProperties();
		 String en = prop.get("spawners", String.class);
		 boolean enabled = true;
		 if(Vars.isBoolean(en))
			 enabled = Boolean.parseBoolean(en);
		
		 if(!enabled) return;
		 
		 // spawn density set to 1 mob per 10 tiles in scene
		 int spawnMax = width / (Vars.TILE_SIZE*10);
		 
		//left spawner
		Spawner s = new Spawner(0, groundLevel, "civilian", "movetopath", "genericCivilian", 
				"onAttacked", null, null, null);
		entities.add(s);
		Array<Vector2> points = new Array<>();
		points.add(new Vector2(width-2*Vars.TILE_SIZE, groundLevel));
		points.add(new Vector2(width, groundLevel));
		Path p = new Path(ID, ID, points);
		s.setPath(p);
		s.setID("spawner "+ID+" :: left");
		s.setMax(spawnMax/2);
		s.setGameState(main);
		s.initOccupy(this);
		
		//right spawner
		s = new Spawner(width - 2*Vars.TILE_SIZE, groundLevel, "civilian", "movetopath", "genericCivilian", 
				"onAttacked", null, null, null);
		entities.add(s);
		points.clear();
		points.add(new Vector2(2*Vars.TILE_SIZE, groundLevel));
		points.add(new Vector2(0, groundLevel));
		p = new Path(ID, ID, points);
		s.setPath(p);
		s.setID("spawner "+ID+" :: right");
		s.setMax(spawnMax/2);
		s.setGameState(main);
		s.initOccupy(this);
	}
	
	
	// create all existing warps on level
	public Array<Warp> createWarps(){
		Array<Warp> warps = new Array<>();
		MapProperties prop = tileMap.getProperties();

		//create right warp
		if(prop.get("next")!=null) {
			String next = prop.get("next", String.class);
			String nextWarp = prop.get("nextWarp", String.class);

			int nextID = 0;
			if(nextWarp!=null) nextID = Integer.parseInt(nextWarp);
			Rectangle rect = new Rectangle(width-Vars.TILE_SIZE*2, 0, Vars.TILE_SIZE*2, height);

			Warp w = new Warp(this, next.replaceAll(" ", ""), 1, nextID, rect.x, 
					rect.y+rect.height/2, rect.width, rect.height);

			w.setInstant(true);
			warps.add(w);
		}

		//create left warp
		if(prop.get("previous")!=null) {
			String next = prop.get("previous", String.class);
			String nextWarp = prop.get("prevWarp", String.class);

			int nextID = 1;
			if(nextWarp!=null) nextID = Integer.parseInt(nextWarp);
			Rectangle rect = new Rectangle(Vars.TILE_SIZE*2, 0, Vars.TILE_SIZE*2, height);

			Warp w = new Warp(this, next.replaceAll(" ", ""), 0, nextID, rect.x, 
					rect.y+rect.height/2, rect.width, rect.height);

			w.setInstant(true);
			warps.add(w);
		}

		//create all other warps
		if(tileMap.getLayers().get("entities")!=null){
			MapObjects objects = tileMap.getLayers().get("entities").getObjects();
			for(MapObject object : objects) {
				if (object instanceof RectangleMapObject) {
					Rectangle rect = ((RectangleMapObject) object).getRectangle();
					if(object.getProperties().get("warp")!=null) {
						String next = object.getProperties().get("next", String.class);
						String nextWarp = object.getProperties().get("nextWarp", String.class);
						String id = object.getProperties().get("ID", String.class);
						String condition = object.getProperties().get("warp", String.class);
						boolean instant = false;

						//instant property must have a constant tile offset to ensure that the player
						//doesn't constantly bounce between levels
						if(object.getProperties().get("instant")!=null)
							instant = true;

						int warpID = Integer.parseInt(id);
						int nextID = Integer.parseInt(nextWarp);
						Warp w = new Warp(this, next.replaceAll(" ", ""), warpID, nextID, rect.x+rect.width/2f, 
								rect.y+rect.height/2-Vars.TILE_SIZE*(9f/80f), rect.width, rect.height);
						w.setCondition(condition);
						w.setInstant(instant);
						warps.add(w);
					}
				}
			}
		}
		
		return warps;
	}
	
	//pull side warps from hashtable for adding into world
	public ArrayList<Entity> retrieveSideWarps(){
		ArrayList<Entity> warps = new ArrayList<>();
		MapProperties prop = tileMap.getProperties();

		Warp w;
		if(prop.get("next")!=null){
			w = main.findWarp(ID, 1);
			w.setOffset(-4*Vars.TILE_SIZE, this.groundLevel);
			warps.add(w);
			// adds a trigger to show the title of the next location
			if(w.getLink().outside){
				TextTrigger tt = new TextTrigger(w.getWarpLoc().x + Vars.TILE_SIZE, w.getWarpLoc().y+
						TextTrigger.DEFAULT_HEIGHT/2f-.36f*Vars.TILE_SIZE, "To "+w.getLink().locTitle, SpeechBubble.PositionType.RIGHT_MARGIN);
				w.setTextTrigger(tt);
				warps.add(tt);
			}
		} if(prop.get("previous")!=null){
			w = main.findWarp(ID, 0);
			w.setOffset(4*Vars.TILE_SIZE, this.groundLevel);
			warps.add(w);
			if(w.getLink().outside){
				TextTrigger tt = new TextTrigger(w.getWarpLoc().x - Vars.TILE_SIZE, w.getWarpLoc().y+
						TextTrigger.DEFAULT_HEIGHT/2f-.36f*Vars.TILE_SIZE, "To "+w.getLink().locTitle, SpeechBubble.PositionType.LEFT_MARGIN);
				w.setTextTrigger(tt);
				warps.add(tt);
			}
		}
		
		return warps;
	}

	public String getType() {
		return null;
	}

	public void setRayHandler(RayHandler rh) {
		sb.setRayHandler(rh);
		this.rayHandler = rh;
	}
	
	public static Scene levelFromID(String ID, Warp warp, int warpID){
		if(warp==null) return null;
		Main main = warp.getGameState();
		Scene s = new Scene(main.getWorld(), main, ID);
		s.setRayHandler(main.getScene().rayHandler);
		return s;
	}
}
