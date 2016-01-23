package scenes;

import static handlers.Vars.PPM;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.MapProperties;
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

import box2dLight.PointLight;
import box2dLight.RayHandler;
import entities.Barrier;
import entities.Entity;
import entities.Ground;
import entities.Mob;
import entities.MobAI.ResetType;
import entities.Path;
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
	public static Map<String, Set<Integer> > sceneToEntityIds;
	public Script loadScript;
	public int width, height;
	public String title, ID;
	public Song[] DEFAULT_SONG;
	public boolean newSong; //tells us whether or not to fade music when loading scene from previous scene
	public boolean outside; //controls whether or not to step weather time or to add the day/night cycle effect
	
	private float groundLevel;
	private Color ambient;
	private Main main;
	private FadingSpriteBatch sb;
	private OrthogonalTiledMapRenderer tmr;
	private Vector2 spawnpoint, localSpawnpoint, camBotSpawnpoint;
	private ArrayList<Entity> entities;
	private ArrayList<PointLight> lights;
	private ArrayList<Path> paths;
	private HashMap<Script, Pair<String, Boolean>> conditionalScripts;
	private HashMap<Mob, String> pathsToAdd;
	private HashMap<Mob, String> fociToAdd;
	private TiledMap tileMap;
	private Texture background, midground, foreground, clouds, sky, grad, sun, moon;
	private World world;
	private RayHandler rayHandler;
//	private Mob character;
	private boolean tileBG = true;
	
	private static Vector2 gravity;
	private static BodyDef bdef = new BodyDef();
	private static FixtureDef fdef = new FixtureDef();
	
	private static final int DAY = 0;
	private static final int NOON = 1;
	private static final int NIGHT =2;
	
	static {
		sceneToEntityIds = new HashMap<String, Set<Integer> >();
	}
	
	//temporary object used only for creating warps
	public Scene(String ID){
		ID = ID.replaceAll(" ", "");
		this.ID=ID;
		tileMap = new TmxMapLoader().load("assets/maps/" + ID + ".tmx");
		MapProperties prop = tileMap.getProperties();
		width = prop.get("width", Integer.class)*Vars.TILE_SIZE;
		height = prop.get("height", Integer.class)*Vars.TILE_SIZE;
		title = prop.get("name", String.class);
		if(title==null) title = ID;
		
		String light= "";
		if((light=prop.get("ambient", String.class))!=null){
			if(light.equals("daynight".toUpperCase()))
				outside = true;
			else
				if(prop.get("outside", String.class)!=null)
					outside = true;
		}
	}
	
	public Scene(World world, Main m, String ID) {
		this.world = world;
		this.main = m;
		sb = m.getSpriteBatch();
//		character = m.character;
		DEFAULT_SONG = new Song[3];
		gravity = new Vector2(0, Vars.GRAVITY);

		entities = new ArrayList<>();
		lights = new ArrayList<>();
		paths = new ArrayList<>();
		pathsToAdd = new HashMap<>();
		fociToAdd = new HashMap<>();
		conditionalScripts = new HashMap<>();
		width = 1000;
		height = 1000;

		title = ID.toLowerCase();
		this.ID = ID.replaceAll(" ", "");

		tileMap = new TmxMapLoader().load("assets/maps/" + this.ID + ".tmx");
		if (!sceneToEntityIds.containsKey(this.ID)) {	//first time scene is referenced
			sceneToEntityIds.put(this.ID, new HashSet<Integer>());
		}
		
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

		newSong = true;
		
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
		if((zoom = prop.get("tileBG", String.class))!=null){
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
			try{
				tileBG = Boolean.parseBoolean(bool);
			} catch(Exception e){ }
		
		try {
			
			// redo me
			String light = "";
			if((light = prop.get("ambient", String.class))!=null){
				if(light.equals("daynight".toUpperCase())){
					outside = true;
					ambient = m.getColorOverlay();
				}else{
					Field f = Vars.class.getField(light);
					ambient = (Color) f.get(f);
					if(prop.get("outside", String.class)!=null)
						outside=true;
				}
			}
		} catch (Exception e){
			ambient = Vars.DAYLIGHT;
			newSong = true;
		} finally{
			sb.setRHAmbient(ambient);
		}
		
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
	
	
	//trigger any script stuff
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
		boolean overlay = sb.isDrawingOverlay();

		if(overlay) sb.setOverlayDraw(false);

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

//				Main.debugText+="/l/l cam: ("+x+" , "+ y1+")"+
//						"   ("+(cam.position.x)+" , "+ 	(cam.position.y)+")";
//				Main.debugText+="/l sky: ("+x+" , "+ y+")";
//				Main.debugText+="/l"+(y+sky.getHeight()*s);
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

		if(overlay) sb.setOverlayDraw(true);

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
	
	public void renderEnvironment(OrthographicCamera cam){
		tmr.setView(cam);
		try{
			for(MapLayer t : tileMap.getLayers()){
				if(t.getName().equals("fg"))
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
	
	public ArrayList<Entity> getInitEntities() { return entities; }
	public Vector2 getGravity() { return gravity; }
	public TiledMap getTileMap(){ return tileMap; }
	public Vector2 getSpawnPoint(){ return spawnpoint; }
	public void setSpawnpoint(Vector2 newSpawn){ spawnpoint = newSpawn; }
	public Vector2 getLocalSpawnpoint(){ return localSpawnpoint; }
	public Vector2 getCBSP() { return camBotSpawnpoint; }

	public void create() {
		if(main.getHud()!=null) //that means we're no longer initializing the game
			main.getHud().showLocation();
		TiledMapTileLayer ground = (TiledMapTileLayer) tileMap.getLayers().get("ground");
		TiledMapTileLayer fg = (TiledMapTileLayer) tileMap.getLayers().get("fg");
		TiledMapTileLayer bg1 = (TiledMapTileLayer) tileMap.getLayers().get("bg1");
//		TiledMapTileLayer bg2 = (TiledMapTileLayer) tileMap.getLayers().get("bg2");
		
		//add in entities loaded from save file
		if (Scene.sceneToEntityIds.containsKey(this.ID)) {
			for (int sid : Scene.sceneToEntityIds.get(this.ID)) {
				this.entities.add(Entity.idToEntity.get(sid));
			}
		}
		
		Ground g;
		for (int y = 0; y < ground.getHeight(); y++)
			for(int x = 0; x < ground.getWidth(); x++){
				Cell cell = ground.getCell(x, y);
				Vector2 location = new Vector2((x+.5f) * Vars.TILE_SIZE / Vars.PPM,
						y * Vars.TILE_SIZE / Vars.PPM);
				if (cell == null) continue;
				if (cell.getTile() == null) continue;
				
				groundLevel = (y-1)*Vars.TILE_SIZE + Vars.TILE_SIZE/3.6f;
				String type = "";
				if(cell.getTile().getProperties().get("type")!=null)
					type = cell.getTile().getProperties().get("type", String.class);
				g = new Ground(world, type, (x * Vars.TILE_SIZE +7.1f) / PPM,
						(y * Vars.TILE_SIZE+Vars.TILE_SIZE/ /*1.8f*/ 3.6f) / PPM);
				g.setGameState(main);
				
				cell = fg.getCell(x, y);
				if (cell != null) {
					if (cell.getTile() != null) {
						Object b = cell.getTile().getProperties().get("sound");
						if(b!=null){
							String src = cell.getTile().getProperties().get("sound", String.class);
							if(src!=null) new PositionalAudio(new Vector2(location.x, location.y), src, main);
						}
					}
				}
//				
				cell = bg1.getCell(x, y);
				if (cell != null) {
					if (cell.getTile() != null) {
						Object b = cell.getTile().getProperties().get("sound");
						if(b!=null){
							String src = cell.getTile().getProperties().get("sound", String.class);
							if(src!=null) new PositionalAudio(new Vector2(location.x, location.y), src, main);
						}
					}
				}
			}
		
		//lights
		int distance = 10000;
		Color color = Color.YELLOW;
		lights.add(new PointLight(rayHandler, Vars.LIGHT_RAYS, color, distance, 0, 0 ));

		TiledMapTileLayer layer = (TiledMapTileLayer) tileMap.getLayers().get("fg");
		if(layer!=null){
			layer.hashCode();
		}

		if(tileMap.getLayers().get("entities")!=null){
			MapObjects objects = tileMap.getLayers().get("entities").getObjects();
			for(MapObject object : objects) {
				if (object instanceof RectangleMapObject) {
					Rectangle rect = ((RectangleMapObject) object).getRectangle();
//					if(object.getProperties().get("warp")!=null)
//						System.out.println(new Vector2(rect.x,rect.y));
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
							if (l.toLowerCase().equals("back")) {
								l = "3";
							} else if (l.toLowerCase().equals("player")) {
								l = "2";
							} else {
								l = "1";
							}
						}
						else l = "1";

						try {
							short lyr;
							if (l.equals("2")) {	//TODO refactor
								lyr = Vars.BIT_PLAYER_LAYER;
							} else {
								Field f = Vars.class.getField("BIT_LAYER"+l);
								lyr = f.getShort(f);
							}
							ID = ID.toLowerCase();
							int sceneIDParsed = Integer.parseInt(sceneID);

							if (sceneID==null)
								System.out.println("'" + name + "', '"+ID+"' cannot be created without a sceneID!");
							else if (Entity.idToEntity.containsKey(sceneIDParsed)) {
								Entity c = Entity.idToEntity.get(sceneIDParsed);
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
								
								if(sceneIDParsed>=0)
									Scene.sceneToEntityIds.get(this.ID).add(sceneIDParsed);
							}
						} catch (NoSuchFieldException | SecurityException e) {
							e.printStackTrace();
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						}
					}
					
					if(object.getProperties().get("barrier")!=null) {
						String id = object.getProperties().get("ID", String.class);
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
						w.setOwner(this);
						w.active = false;
						entities.add(w);
					}
					
					if(object.getProperties().get("event")!=null){
						Iterator<String> it = object.getProperties().getKeys();
						EventTrigger et = new EventTrigger(main, rect.x, rect.y, rect.width, rect.height);
						String script, condition, s;
						
						while(it.hasNext()){
							script = it.next();
							if(script.equals("x") || script.equals("y") || script.equals("event")
									|| script.equals("retriggerable") || script.equals("avoidHalt"))
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
					String behavior = object.getProperties().get("behavior", String.class);
					Polyline pl =((PolylineMapObject) object).getPolyline();
					
					Array<Vector2> vertices = new Array<Vector2>();
					for(int i = 0; i<pl.getVertices().length-1; i+=2)
								vertices.add(new Vector2(pl.getVertices()[i]+pl.getX(), 
										pl.getVertices()[i+1]+pl.getY()));
					
					paths.add(new Path(name, behavior, vertices));
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
		
		entities.addAll(createLinkWarps());
	}
	
	
	// create all existing warps on level
	public Array<Warp> createWarps(){
		Array<Warp> warps = new Array<>();
		MapProperties prop = tileMap.getProperties();

		//create right warp
		if(prop.get("next")!=null) {
			String next = prop.get("next", String.class);
			String nextWarp = prop.get("nextWarp", String.class);
			boolean instant = true;

			int nextID = 0;
			if(nextWarp!=null) nextID = Integer.parseInt(nextWarp);
			Rectangle rect = new Rectangle(width-Vars.TILE_SIZE*2, 0, Vars.TILE_SIZE*2, height);

			Warp w = new Warp(this, next.replaceAll(" ", ""), 1, nextID, rect.x, 
					rect.y+rect.height/2, rect.width, rect.height);

			w.setInstant(instant);
			warps.add(w);
		}

		//create left warp
		if(prop.get("previous")!=null) {
			String next = prop.get("previous", String.class);
			String nextWarp = prop.get("prevWarp", String.class);
			boolean instant = true;

			int nextID = 1;
			if(nextWarp!=null) nextID = Integer.parseInt(nextWarp);
			Rectangle rect = new Rectangle(Vars.TILE_SIZE*2, 0, Vars.TILE_SIZE*2, height);

			Warp w = new Warp(this, next.replaceAll(" ", ""), 0, nextID, rect.x, 
					rect.y+rect.height/2, rect.width, rect.height);

			w.setInstant(instant);
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
						Warp w = new Warp(this, next.replaceAll(" ", ""), warpID, nextID, rect.x, 
								rect.y+rect.height/2, rect.width, rect.height);
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
	public ArrayList<Entity> createLinkWarps(){
		ArrayList<Entity> warps = new ArrayList<>();
		MapProperties prop = tileMap.getProperties();

		Warp w;
		if(prop.get("next")!=null){
			w = main.findWarp(ID, 1);
			w.setOffset(-4*Vars.TILE_SIZE, this.groundLevel);
			w.setOwner(this);
			warps.add(w);
			// adds a trigger to show the title of the next location
			if(w.getLink().owner.outside)
				warps.add(new TextTrigger(w.getWarpLoc().x + Vars.TILE_SIZE, w.getWarpLoc().y+
						TextTrigger.DEFAULT_HEIGHT-Vars.TILE_SIZE, "To "+w.getLink().locTitle, SpeechBubble.PositionType.RIGHT_MARGIN));
		} if(prop.get("previous")!=null){
			w = main.findWarp(ID, 0);
			w.setOffset(4*Vars.TILE_SIZE, this.groundLevel);
			w.setOwner(this);
			warps.add(w);
			if(w.getLink().owner.outside)
				warps.add(new TextTrigger(w.getWarpLoc().x - Vars.TILE_SIZE, w.getWarpLoc().y+
						TextTrigger.DEFAULT_HEIGHT-Vars.TILE_SIZE, "To "+w.getLink().locTitle, SpeechBubble.PositionType.LEFT_MARGIN));
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
	
	public Scene levelFromID(String ID, Warp warp, int warpID){
		Scene s = new Scene(world, main, ID);
		s.setRayHandler(rayHandler);
		
		if(warp!=null)
			if(!warp.owner.newSong) s.newSong = false;
		
//		Vector2 linkLoc = main.findWarp(ID, warpID).getLink();
//		if(linkLoc!=null) warp.setLink(linkLoc);
//		else warp.setLink(s.spawnpoint);
		
		return s;
	}
}
