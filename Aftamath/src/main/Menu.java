package main;

import java.util.ArrayList;
import java.util.HashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import entities.MenuObj;
import entities.MenuObj.SourceType;
import handlers.Animation;
import handlers.FadingSpriteBatch;
import handlers.JsonSerializer;
import handlers.Pair;
import handlers.Vars;

public class Menu {

	public MenuObj[][] objs; //list of all objects
	public MenuType type;
	public Pair<MenuType, MenuType> tabs;
	public int width, maxX, maxY, height, x0, y0;
	public boolean overlay;
	
	private GameState gs;
	private boolean scrolling;
	private float scrollTime;
	private Texture image, leftTab, centerTab, rightTab;
	private Animation highlight;
	private Vector2 startPoint, scrollOff, goalScroll, prevScroll;
	private Array<MenuObj> vertical, horizontal, scrollObjs; //projections of traversable objs
	private HashMap<MenuObj, Vector2> projections; // mapping of traversable objects to projections
	
	private static final float SCROLL_TIME = .25f;
	
	// sliding variables
	//private boolean sliding;
	//private Vector2 slideDirection;
	//private float slideTime;
	
	public static enum MenuType{
		STATS, LOAD, SAVE, PAUSE, JOURNAL, OPTIONS, YESNO, MAP
	}
	
	public Menu(MenuType menuType, GameState gs) {
		this.type = menuType;
		this.gs = gs;
		overlay = false;
		startPoint = new Vector2(0, 0);
		scrollOff = new Vector2(0, 0);
		
		//load background image for window
		image = Game.res.getTexture(type.toString().toLowerCase()+"_menu");
		if(image == null)
			image = Game.res.getTexture("menu_default");
		width = image.getWidth();
		height = image.getHeight();
		addObjects();
		
		highlight = new Animation(null);
		highlight.initFrames(TextureRegion.split(Game.res.getTexture("btn_highlight"), 1, 1)[0],
				 Vars.ACTION_ANIMATION_RATE, false);
	}
	
	// eat a long one
	public void update(float dt){
		highlight.update(dt);
		
		if(scrolling){
			scrollTime+=dt;
			scrollOff.x = Vars.easingFunction(scrollTime, .2f, prevScroll.x, goalScroll.x);
			scrollOff.y = Vars.easingFunction(scrollTime, .2f, prevScroll.y, goalScroll.y);
			
			if(scrollTime>=SCROLL_TIME)
				scrolling = false;
		}
		
		for(MenuObj[] l : objs){
			if(l==null) continue;
			for(MenuObj m : l){
				if(m==null) continue;
				m.update(dt);
			}
		}
//		System.out.println(scrollMenuCam.position+"\tmov: "+scrollMenuCam.moving);
	}
	
	public void render(FadingSpriteBatch sb){
		Color color = sb.getColor();
		sb.setColor(Vars.DAY_OVERLAY);
		sb.begin();
		sb.draw(image, x0, y0-height, width, height);
		if(tabs!=null) renderTabs(sb);
		
		JournalEntry j = null;
		for(MenuObj[] l : objs){
			if(l==null) continue;
			for(MenuObj m : l){
				if(m==null) continue; 
				if (m.hidden) continue;
				sb.setProjectionMatrix(gs.hudCam.combined);
				m.render(sb);
				
//				if(scrollObjs.contains(m, true))
//					sb.setProjectionMatrix(this.scrollMenuCam.combined);
				
				// highlight current object
				if(m==objs[(int) gs.cursor.x][(int) gs.cursor.y]){
					if(m instanceof JournalEntry)
						j = (JournalEntry) m;
					else if(m.hiLite){
						MenuObj b = objs[(int) gs.cursor.x][(int) gs.cursor.y];
						if(b.getType()==SourceType.TEXT)
							sb.draw(highlight.getFrame(), b.x-1, b.y-2 - b.height, b.width+2, b.height+2);
						else
							sb.draw(highlight.getFrame(), b.x-1, b.y-1, b.width+2, b.height+2);
					}
				}
			}
		}
		
		if(j!=null)
			j.render(sb);
		
		sb.end();
		sb.setColor(color);
	}
	
	private void renderTabs(FadingSpriteBatch sb){
		int h = 38;
		sb.draw(leftTab, x0 + 15, y0 - h);
		sb.draw(centerTab, x0 + 44, y0 - h);
		sb.draw(rightTab, x0 + 73, y0 - h);
	}
	
	public void onClick(Vector2 cursor){
		objs[(int) cursor.x][(int) cursor.y].onClick();
	}
	
	/**
	 * can the user click on the current object?
	 * @param cursor
	 * @return
	 */
	public boolean isActive(Vector2 cursor){
		if(cursor.x>=objs.length) cursor.x = 0;
		if(cursor.x<0) cursor.x = objs.length;

		if(cursor.y>=objs[(int) cursor.x].length) cursor.y = 0;
		if(cursor.y<0) cursor.y = objs[(int) cursor.x].length;
		
		if(objs[(int) cursor.x][(int)cursor.y]==null) return false;
		return objs[(int) cursor.x][(int)cursor.y].clickable;
	}
	
	public void slideIn(Vector2 direction){
		
	}
	
	/**
	 * populate window
	 */
	private void addObjects() {
		ArrayList<Pair<MenuObj, Vector2>> mos = new ArrayList<>();
		projections = new HashMap<>();
		scrollObjs = new Array<>();
		MenuObj b; String s;
		int w, h;
		
		//top right corner of menu
		x0 = Game.width/4 - width/2;
		y0 = Game.height/4 + height/2;
//		System.out.println(type+" corner: "+new Vector2(x0/2, y0/2));
		
		try{
			switch(type){
			case LOAD:
				// load file 1
				s = findSaveFile(1);
				if(s!=null){
					String[] data = s.split("/l");
					// backing
					w = 91; h = 145;
					b = new MenuObj("file", SourceType.IMAGE, x0 + 15, y0 - 44 - h, w, h, gs);
					b.setMethod(GameState.class.getMethod("loadGame", new Class[]{int.class}), gs);
					b.setArgs(1);
					mos.add(new Pair<>(b, new Vector2(0, 0)));
					
					// player image
					w = 40; h = 50;
					b = new MenuObj(data[0], SourceType.IMAGE, x0 + 41, y0 - 52-h, w, h, gs);
					mos.add(new Pair<>(b, new Vector2(0, 1)));
					b.clickable = false;
					
					// player name
					b = new MenuObj(data[1], SourceType.TEXT, x0 + 33, y0 - 116, 8, 2, gs);
					mos.add(new Pair<>(b, new Vector2(0, 2)));
					b.clickable = false;
					
					// location
					b = new MenuObj(data[2], SourceType.TEXT, x0 + 26, y0 - 142, 10, 2, gs);
					mos.add(new Pair<>(b, new Vector2(0, 3)));
					b.clickable = false;
					
					// playtime
					b = new MenuObj(Vars.formatTime(Float.parseFloat(data[3])), SourceType.TEXT, x0 + 33, y0 - 168, 100, 1, gs);
					mos.add(new Pair<>(b, new Vector2(0, 4)));
					b.clickable = false;
					
				} else {
					// backing
					w = 91; h = 145;
					b = new MenuObj("file", SourceType.IMAGE, x0 + 15, y0 - 44 - h, w, h, gs);
					mos.add(new Pair<>(b, new Vector2(0, 5)));
					b.clickable = false;
					
					//empty
					b = new MenuObj("EMPTY", SourceType.TEXT, x0, y0, 100, 1, gs);
					mos.add(new Pair<>(b, new Vector2(0, 6)));
					b.x += 15+w/2-b.width/2; b.y += -44 - h/2; 
					b.clickable = false;
				}
				
				// load file 2
				s = findSaveFile(2);
				if(s!=null){
					String[] data = s.split("/l");
					// backing
					w = 91; h = 145;
					b = new MenuObj("file", SourceType.IMAGE, x0 + 121, y0 - 44 - h, w, h, gs);
					b.setMethod(GameState.class.getMethod("loadGame", new Class[]{int.class}), gs);
					b.setArgs(2);
					mos.add(new Pair<>(b, new Vector2(1, 0)));
					
					// player image
					w = 40; h = 50;
					b = new MenuObj(data[0], SourceType.IMAGE, x0 + 147, y0 - 52-h, w, h, gs);
					mos.add(new Pair<>(b, new Vector2(1, 1)));
					b.clickable = false;
					
					// player name
					b = new MenuObj(data[1], SourceType.TEXT, x0 + 140, y0 - 116, 8, 2, gs);
					mos.add(new Pair<>(b, new Vector2(1, 2)));
					b.clickable = false;
					
					// location
					b = new MenuObj(data[2], SourceType.TEXT, x0 + 133, y0 - 142, 10, 2, gs);
					mos.add(new Pair<>(b, new Vector2(1, 3)));
					b.clickable = false;
					
					// playtime
					b = new MenuObj(Vars.formatTime(Float.parseFloat(data[3])), 
							SourceType.TEXT, x0 + 140, y0 - 168, 100, 1, gs);
					mos.add(new Pair<>(b, new Vector2(1, 4)));
					b.clickable = false;
					
				} else {
					// backing
					w = 91; h = 145;
					b = new MenuObj("file", SourceType.IMAGE, x0 + 121, y0 - 44 - h, w, h, gs);
					mos.add(new Pair<>(b, new Vector2(1, 5)));
					
					//empty
					b = new MenuObj("EMPTY", SourceType.TEXT, x0, y0, 100, 1, gs);
					mos.add(new Pair<>(b, new Vector2(1, 6)));
					b.x += 121+w/2 - b.width/2; b.y += -44 - h/2; 
					b.clickable = false;
				}

				// load file 3
				s = findSaveFile(3);
				if(s!=null){
					String[] data = s.split("/l");
					// backing
					w = 91; h = 145;
					b = new MenuObj("file", SourceType.IMAGE, x0 + 227, y0 - 44 - h, w, h, gs);
					b.setMethod(GameState.class.getMethod("loadGame", new Class[]{int.class}), gs);
					b.setArgs(3);
					mos.add(new Pair<>(b, new Vector2(2, 0)));
					
					// player image
					w = 40; h = 50;
					b = new MenuObj(data[0], SourceType.IMAGE, x0 + 253, y0 - 52-h, w, h, gs);
					mos.add(new Pair<>(b, new Vector2(2, 1)));
					b.clickable = false;
					
					// player name
					b = new MenuObj(data[1], SourceType.TEXT, x0 + 244, y0 - 116, 8, 2, gs);
					mos.add(new Pair<>(b, new Vector2(2, 2)));
					b.clickable = false;
					
					// location
					b = new MenuObj(data[2], SourceType.TEXT, x0 + 237, y0 - 142, 10, 2, gs);
					mos.add(new Pair<>(b, new Vector2(2, 3)));
					b.clickable = false;
					
					// playtime
					b = new MenuObj(Vars.formatTime(Float.parseFloat(data[3])), SourceType.TEXT, x0 + 244, y0 - 168, 100, 1, gs);
					mos.add(new Pair<>(b, new Vector2(2, 4)));
					b.clickable = false;
				} else {
					// backing
					w = 91; h = 145;
					b = new MenuObj("file", SourceType.IMAGE, x0 + 227, y0 - 44 - h, w, h, gs);
					mos.add(new Pair<>(b, new Vector2(2, 5)));
					b.clickable = false;
					
					//empty
					b = new MenuObj("EMPTY", SourceType.TEXT, x0, y0, 100, 1, gs);
					mos.add(new Pair<>(b, new Vector2(2, 6)));
					b.x += 227+w/2 - b.width/2; b.y += -44 - h/2; 
					b.clickable = false;
				}
				
				// back
				h = 18;
				b = new MenuObj("Back", "default_button", x0 + 134 , y0 - 201 - h, gs);
				mos.add(new Pair<>(b, new Vector2(2, 7)));
				b.setMethod(Menu.class.getMethod("back", new Class[]{}), this);

				break;
			case SAVE:
				// save file 1
				s = findSaveFile(1);
				if(s!=null){
					String[] data = s.split("/l");
					
					// player image
					w = 40; h = 50;
					b = new MenuObj(data[0], SourceType.IMAGE, x0 + 41, y0 - 52-h, w, h, gs);
					mos.add(new Pair<>(b, new Vector2(0, 1)));
					b.clickable = false;
					
					// player name
					b = new MenuObj(data[1], SourceType.TEXT, x0 + 33, y0 - 116, 8, 2, gs);
					mos.add(new Pair<>(b, new Vector2(0, 2)));
					b.clickable = false;
					
					// location
					b = new MenuObj(data[2], SourceType.TEXT, x0 + 26, y0 - 142, 10, 2, gs);
					mos.add(new Pair<>(b, new Vector2(0, 3)));
					b.clickable = false;
					
					// playtime
					b = new MenuObj(Vars.formatTime(Float.parseFloat(data[3])), SourceType.TEXT, x0 + 33, y0 - 168, 100, 1, gs);
					mos.add(new Pair<>(b, new Vector2(0, 4)));
					b.clickable = false;
					
				} else {
					//empty
					w = 91; h = 145;
					b = new MenuObj("EMPTY", SourceType.TEXT, x0, y0, 100, 1, gs);
					mos.add(new Pair<>(b, new Vector2(0, 1)));
					b.x += 15+w/2-b.width/2; b.y += -44 - h/2; 
					b.clickable = false;
				}
				
				// backing
				w = 91; h = 145;
				b = new MenuObj("file", SourceType.IMAGE, x0 + 15, y0 - 44 - h, w, h, gs);
				b.setMethod(GameState.class.getMethod("saveGame", new Class[]{int.class}), gs);
				b.setArgs(1);
				mos.add(new Pair<>(b, new Vector2(0, 0)));
				
				// save file 2
				s = findSaveFile(2);
				if(s!=null){
					String[] data = s.split("/l");
					
					// player image
					w = 40; h = 50;
					b = new MenuObj(data[0], SourceType.IMAGE, x0 + 147, y0 - 52-h, w, h, gs);
					mos.add(new Pair<>(b, new Vector2(1, 1)));
					b.clickable = false;
					
					// player name
					b = new MenuObj(data[1], SourceType.TEXT, x0 + 140, y0 - 116, 8, 2, gs);
					mos.add(new Pair<>(b, new Vector2(1, 2)));
					b.clickable = false;
					
					// location
					b = new MenuObj(data[2], SourceType.TEXT, x0 + 133, y0 - 142, 10, 2, gs);
					mos.add(new Pair<>(b, new Vector2(1, 3)));
					b.clickable = false;
					
					// playtime
					b = new MenuObj(Vars.formatTime(Float.parseFloat(data[3])), SourceType.TEXT, x0 + 140, y0 - 168, 100, 1, gs);
					mos.add(new Pair<>(b, new Vector2(1, 4)));
					b.clickable = false;
					
				} else {
					//empty
					w = 91; h = 145;
					b = new MenuObj("EMPTY", SourceType.TEXT, x0, y0, 100, 1, gs);
					mos.add(new Pair<>(b, new Vector2(1, 1)));
					b.x += 121+w/2 - b.width/2; b.y += -44 - h/2; 
					b.clickable = false;
				}
				
				// backing
				w = 91; h = 145;
				b = new MenuObj("file", SourceType.IMAGE, x0 + 121, y0 - 44 - h, w, h, gs);
				b.setMethod(GameState.class.getMethod("saveGame", new Class[]{int.class}), gs);
				b.setArgs(2);
				mos.add(new Pair<>(b, new Vector2(1, 0)));

				// save file 3
				s = findSaveFile(3);
				if(s!=null){
					String[] data = s.split("/l");
					
					// player image
					w = 40; h = 50;
					b = new MenuObj(data[0], SourceType.IMAGE, x0 + 253, y0 - 52 -h, w, h, gs);
					mos.add(new Pair<>(b, new Vector2(2, 1)));
					b.clickable = false;
					
					// player name
					b = new MenuObj(data[1], SourceType.TEXT, x0 + 244, y0 - 116, 8, 2, gs);
					mos.add(new Pair<>(b, new Vector2(2, 2)));
					b.clickable = false;
					
					// location
					b = new MenuObj(data[2], SourceType.TEXT, x0 + 237, y0 - 142, 10, 2, gs);
					mos.add(new Pair<>(b, new Vector2(2, 3)));
					b.clickable = false;
					
					// playtime
					b = new MenuObj(Vars.formatTime(Float.parseFloat(data[3])), SourceType.TEXT, x0 + 244, y0 - 168, 100, 1, gs);
					mos.add(new Pair<>(b, new Vector2(2, 4)));
					b.clickable = false;
				} else {
					//empty
					w = 91; h = 145;
					b = new MenuObj("EMPTY", SourceType.TEXT, x0, y0, 100, 1, gs);
					mos.add(new Pair<>(b, new Vector2(2, 1)));
					b.x += 227+w/2 - b.width/2; b.y += -44 - h/2; 
					b.clickable = false;
				}
				
				// backing
				w = 91; h = 145;
				b = new MenuObj("file", SourceType.IMAGE, x0 + 227, y0 - 44 - h, w, h, gs);
				b.setMethod(GameState.class.getMethod("saveGame", new Class[]{int.class}), gs);
				b.setArgs(3);
				mos.add(new Pair<>(b, new Vector2(2, 0)));

				// BACK
				h = 18;
				b = new MenuObj("Back", "default_button", x0 + 134 , y0 - 201 - h, gs);
				mos.add(new Pair<>(b, new Vector2(2, 7)));
				b.setMethod(Menu.class.getMethod("back", new Class[]{}), this);
				
				break;
			case STATS:
				// tabs
				tabs = new Pair<>(MenuType.JOURNAL, MenuType.MAP);
				leftTab = Game.res.getTexture("journal_tab");
				centerTab = Game.res.getTexture("stats_tab");
				rightTab = Game.res.getTexture("map_tab");
				
				// play time

				// allignment (good/evil)

				// bravery

				// relationship
				// partner info
				
				// level
				// experience
				
				// back
				h = 18;
				b = new MenuObj("Back", "default_button", x0 + 134 , y0 - 201 - h, gs);
				mos.add(new Pair<>(b, new Vector2(0, 1)));
				b.setMethod(Menu.class.getMethod("back", new Class[]{}), this);

				break;
			case JOURNAL:
				// tabs
				tabs = new Pair<>(MenuType.MAP, MenuType.STATS);
				leftTab = Game.res.getTexture("map_tab");
				centerTab = Game.res.getTexture("journal_tab");
				rightTab = Game.res.getTexture("stats_tab");
				int e = 0;
				
				// events
				JournalEntry j; 
				for(String s1 : ((Main)gs).history.getEventList().keySet())
					if(Game.EVENT_TO_TEXTURE.get(s1)!=null){
						j = new JournalEntry(s1, e, this, gs); j.hiLite = false;
						mos.add(new Pair<MenuObj, Vector2>(j, new Vector2(e, 0)));
						scrollObjs.add(j);
						e++;
					}
				
				// empty
				if(e==0){
					b = new MenuObj("NOTHING'S HAPPENED YET!", SourceType.TEXT, x0, y0, 100, 1, gs);
					b.x += width/2 - b.width/2; b.y += -height/2 + b.height/2;
					mos. add(new Pair<>(b, new Vector2(0, 0)));
					b.clickable = false;
				}	
				
				// scroll left
				
				// scroll right
				
				// back
				h = 18;
				b = new MenuObj("Back", "default_button", x0 + 134 , y0 - 201 - h, gs);
				mos.add(new Pair<>(b, new Vector2(0, 1)));
				b.setMethod(Menu.class.getMethod("back", new Class[]{}), this);

				break;
			case PAUSE:
				int menuSize = 7;
				
				// RESUME
				float x = (Game.width/2 - "Resume".length() * Vars.TEXT_PERIODX - Vars.TEXT_PERIODX/2)/2;
				float y = (Game.height/2 + height)/2 + (-2 + menuSize) * Vars.TEXT_PERIODY + 12;
				b = new MenuObj("Resume", SourceType.TEXT, x, y, 100, 1, gs);
				mos.add(new Pair<>(b, new Vector2(0, 0)));
				b.setMethod(GameState.class.getMethod("back", new Class[]{}), gs);
				
				// JOURNAL
				x = (Game.width/2 - "Journal".length() * Vars.TEXT_PERIODX - Vars.TEXT_PERIODX/2)/2;
				y = (Game.height/2 + height)/2 + (-1 + menuSize) * Vars.TEXT_PERIODY + 12;
				b = new MenuObj("Journal", SourceType.TEXT, x, y, 100, 1, gs);
				mos.add(new Pair<>(b, new Vector2(0, 1)));
				b.setMethod(GameState.class.getMethod("journal", new Class[]{}), gs);
				
				// OPTIONS
				x = (Game.width/2 - "Options".length() * Vars.TEXT_PERIODX - Vars.TEXT_PERIODX/2)/2;
				y = (Game.height/2 + height)/2 + (0 + menuSize) * Vars.TEXT_PERIODY + 12;
				b = new MenuObj("Options", SourceType.TEXT, x, y, 100, 1, gs);
				mos.add(new Pair<>(b, new Vector2(0, 2)));
				b.setMethod(GameState.class.getMethod("options", new Class[]{}), gs);
				
				// LOADGAME
				x = (Game.width/2 - "Load Game".length() * Vars.TEXT_PERIODX - Vars.TEXT_PERIODX/2)/2;
				y = (Game.height/2 + height)/2 + (1 + menuSize) * Vars.TEXT_PERIODY + 12;
				b = new MenuObj("Load Game", SourceType.TEXT, x, y, 100, 1, gs);
				mos.add(new Pair<>(b, new Vector2(0, 3)));
				b.setMethod(GameState.class.getMethod("loadGame", new Class[]{}), gs);
				
				// SAVEGAME
				x = (Game.width/2 - "Save Game".length() * Vars.TEXT_PERIODX - Vars.TEXT_PERIODX/2)/2;
				y = (Game.height/2 + height)/2 + (2 + menuSize) * Vars.TEXT_PERIODY + 12;
				b = new MenuObj("Save Game", SourceType.TEXT, x, y, 100, 1, gs);
				mos.add(new Pair<>(b, new Vector2(0, 4)));
				b.setMethod(GameState.class.getMethod("saveGame", new Class[]{}), gs);
				
				// QUITTOMENU
				x = (Game.width/2 - "Quit to Menu".length() * Vars.TEXT_PERIODX - Vars.TEXT_PERIODX/2)/2;
				y = (Game.height/2 + height)/2 + (3 + menuSize) * Vars.TEXT_PERIODY + 12;
				b = new MenuObj("Quit to Menu", SourceType.TEXT, x, y, 100, 1, gs);
				mos.add(new Pair<>(b, new Vector2(0, 5)));
				b.setMethod(GameState.class.getMethod("quitToMenu", new Class[]{}), gs);
				
				// QUIT
				x = (Game.width/2 - "Quit".length() * Vars.TEXT_PERIODX - Vars.TEXT_PERIODX/2)/2;
				y = (Game.height/2 + height)/2 + (4 + menuSize) * Vars.TEXT_PERIODY + 12;
				b = new MenuObj("Quit", SourceType.TEXT, x, y, 100, 1, gs);
				mos.add(new Pair<>(b, new Vector2(0, 6)));
				b.setMethod(GameState.class.getMethod("quit", new Class[]{}), gs);
				
				break;
			case MAP:
				// tabs
				tabs = new Pair<>(MenuType.STATS, MenuType.JOURNAL);
				leftTab = Game.res.getTexture("stats_tab");
				centerTab = Game.res.getTexture("map_tab");
				rightTab = Game.res.getTexture("journal_tab");
				
				// player Face init
				s = ((Main)gs).character.ID;
				Texture t = Game.res.getTexture(s + "badge");
				String curLoc = GameState.prevLoc;
				if(locToIMG.containsKey(((Main)gs).getScene().ID))
					curLoc = locToIMG.get(((Main)gs).getScene().ID);
				GameState.prevLoc = curLoc;
				Vector2 loc = null, d;
				
				// map
				w = 309; h = 143; int x1 = this.x0+12, y1 = this.y0-45;
				b = new MenuObj("map", SourceType.IMAGE, x1, y1-h, w, h, gs);
				mos.add(new Pair<>(b, new Vector2(0, 0)));
				b.clickable = false;
				
				// location nodes
				w = 39; h = 40;
				b = new MenuObj("boardwalk", SourceType.IMAGE, x1 + 187, y1 - 41 - h, w, h, gs); d = new Vector2(6, 2);
				if(b.getText().equals(curLoc)) {loc = new Vector2(b.x + b.width/2, b.y + b.height/2); startPoint = d;}
				b.hidden = true; b.hiLite = false; mos.add(new Pair<>(b, d));
				
				w = 34; h = 5;
				b = new MenuObj("bridge", SourceType.IMAGE, x1 + 12, y1 - 85 - h, w, h, gs); d = new Vector2(1, 4);
				if(b.getText().equals(curLoc)) {loc = new Vector2(b.x + b.width/2, b.y + b.height/2); startPoint = d;}
				b.hidden = true; b.hiLite = false; mos.add(new Pair<>(b, d));

				w = 51; h = 49;
				b = new MenuObj("business", SourceType.IMAGE, x1 + 148, y1 - 27 - h, w, h, gs); d = new Vector2(4, 2);
				if(b.getText().equals(curLoc)) {loc = new Vector2(b.x + b.width/2, b.y + b.height/2); startPoint = d;}
				b.hidden = true; b.hiLite = false; mos.add(new Pair<>(b, d));

				w = 46; h = 54;
				b = new MenuObj("central_park", SourceType.IMAGE, x1 + 112, y1 - 19 - h, w, h, gs); d = new Vector2(3, 2);
				if(b.getText().equals(curLoc)) {loc = new Vector2(b.x + b.width/2, b.y + b.height/2); startPoint = d;}
				b.hidden = true; b.hiLite = false; mos.add(new Pair<>(b, d));

				w = 33; h = 18;
				b = new MenuObj("commercial_NW", SourceType.IMAGE, x1 + 106, y1 - 0 - h, w, h, gs); d = new Vector2(2, 1);
				if(b.getText().equals(curLoc)) {loc = new Vector2(b.x + b.width/2, b.y + b.height/2); startPoint = d;}
				b.hidden = true; b.hiLite = false; mos.add(new Pair<>(b, d));
				
				w = 44; h = 40;
				b = new MenuObj("commercial_S", SourceType.IMAGE, x1 + 89, y1 - 90 - h, w, h, gs); d = new Vector2(3, 4);
				if(b.getText().equals(curLoc)) {loc = new Vector2(b.x + b.width/2, b.y + b.height/2); startPoint = d;}
				b.hidden = true; b.hiLite = false; mos.add(new Pair<>(b, d));

				w = 10; h = 8;
				b = new MenuObj("church", SourceType.IMAGE, x1 + 182, y1 - 33 - h, w, h, gs); d = new Vector2(5, 2);
				if(b.getText().equals(curLoc)) {loc = new Vector2(b.x + b.width/2, b.y + b.height/2); startPoint = d;}
				b.hidden = true; b.hiLite = false; mos.add(new Pair<>(b, d));

				w = 9; h = 7;
				b = new MenuObj("crosswalk", SourceType.IMAGE, x1 + 152, y1 - 97 - h, w, h, gs); d = new Vector2(5, 4);
				if(b.getText().equals(curLoc)) {loc = new Vector2(b.x + b.width/2, b.y + b.height/2); startPoint = d;}
				b.hidden = true; b.hiLite = false; mos.add(new Pair<>(b, d));

				w = 47; h = 38;
				b = new MenuObj("municipal", SourceType.IMAGE, x1 + 65, y1 - 52 - h, w, h, gs); d = new Vector2(2, 3);
				if(b.getText().equals(curLoc)) {loc = new Vector2(b.x + b.width/2, b.y + b.height/2); startPoint = d;}
				b.hidden = true; b.hiLite = false; mos.add(new Pair<>(b, d));

				w = 46; h = 25;
				b = new MenuObj("factory", SourceType.IMAGE, x1 + 208, y1 - 0 - h, w, h, gs); d = new Vector2(6, 0);
				if(b.getText().equals(curLoc)) {loc = new Vector2(b.x + b.width/2, b.y + b.height/2); startPoint = d;}
				b.hidden = true; b.hiLite = false; mos.add(new Pair<>(b, d));

				w = 39; h = 21;
				b = new MenuObj("hero_HQ", SourceType.IMAGE, x1 + 167, y1 - 75 - h, w, h, gs); d = new Vector2(5, 3);
				if(b.getText().equals(curLoc)) {loc = new Vector2(b.x + b.width/2, b.y + b.height/2); startPoint = d;}
				b.hidden = true; b.hiLite = false; mos.add(new Pair<>(b, d));

				w = 32; h = 35;
				b = new MenuObj("high_rise", SourceType.IMAGE, x1 + 137, y1 - 68 - h, w, h, gs); d = new Vector2(4, 3);
				if(b.getText().equals(curLoc)) {loc = new Vector2(b.x + b.width/2, b.y + b.height/2); startPoint = d;}
				b.hidden = true; b.hiLite = false; mos.add(new Pair<>(b, d));

				w = 18; h = 21;
				b = new MenuObj("mansion", SourceType.IMAGE, x1 + 0, y1 - 76 - h, w, h, gs); d = new Vector2(0, 4);
				if(b.getText().equals(curLoc)) {loc = new Vector2(b.x + b.width/2, b.y + b.height/2); startPoint = d;}
				b.hidden = true; b.hiLite = false; mos.add(new Pair<>(b, d));

				w = 35; h = 31;
				b = new MenuObj("downtown", SourceType.IMAGE, x1 + 107, y1 - 67 - h, w, h, gs); d = new Vector2(3, 3);
				if(b.getText().equals(curLoc)) {loc = new Vector2(b.x + b.width/2, b.y + b.height/2); startPoint = d;}
				b.hidden = true; b.hiLite = false; mos.add(new Pair<>(b, d));

				w = 37; h = 26;
				b = new MenuObj("residential_N", SourceType.IMAGE, x1 + 137, y1 - 0 - h, w, h, gs); d = new Vector2(3, 1);
				if(b.getText().equals(curLoc)) {loc = new Vector2(b.x + b.width/2, b.y + b.height/2); startPoint = d;}
				b.hidden = true; b.hiLite = false; mos.add(new Pair<>(b, d));

				w = 62; h = 35;
				b = new MenuObj("residential_SE", SourceType.IMAGE, x1 + 130, y1 - 91 - h, w, h, gs); d = new Vector2(4, 4);
				if(b.getText().equals(curLoc)) {loc = new Vector2(b.x + b.width/2, b.y + b.height/2); startPoint = d;}
				b.hidden = true; b.hiLite = false; mos.add(new Pair<>(b, d));

				w = 49; h = 37;
				b = new MenuObj("residential_SW", SourceType.IMAGE, x1 + 43, y1 - 78 - h, w, h, gs); d = new Vector2(2, 4);
				if(b.getText().equals(curLoc)) {loc = new Vector2(b.x + b.width/2, b.y + b.height/2); startPoint = d;}
				b.hidden = true; b.hiLite = false; mos.add(new Pair<>(b, d));

				w = 23; h = 32;
				b = new MenuObj("tha_hood", SourceType.IMAGE, x1 + 169, y1 - 0 - h, w, h, gs); d = new Vector2(4, 1);
				if(b.getText().equals(curLoc)) {loc = new Vector2(b.x + b.width/2, b.y + b.height/2); startPoint = d;}
				b.hidden = true; b.hiLite = false; mos.add(new Pair<>(b, d));

				w = 12; h = 11;
				b = new MenuObj("under_bridge", SourceType.IMAGE, x1 + 190, y1 - 24 - h, w, h, gs); d = new Vector2(5, 1);
				if(b.getText().equals(curLoc)) {loc = new Vector2(b.x + b.width/2, b.y + b.height/2); startPoint = d;}
				b.hidden = true; b.hiLite = false;  mos.add(new Pair<>(b, d));

				w = 21; h = 27;
				b = new MenuObj("villain_HQ", SourceType.IMAGE, x1 + 189, y1 - 0 - h, w, h, gs); d = new Vector2(5, 0);
				if(b.getText().equals(curLoc)) {loc = new Vector2(b.x + b.width/2, b.y + b.height/2); startPoint = d;}
				b.hidden = true; b.hiLite = false; mos.add(new Pair<>(b, d));

				w = 40; h = 32;
				b = new MenuObj("warehouse", SourceType.IMAGE, x1 + 201, y1 - 18 - h, w, h, gs); d = new Vector2(6, 1);
//				b = new MenuObj("warehouse", SourceType.IMAGE, 0, 0, w, h, gs); d = new Vector2(6, 1);
				if(b.getText().equals(curLoc)) {loc = new Vector2(b.x + b.width/2, b.y + b.height/2); startPoint = d;}
				b.hidden = true; b.hiLite = false; mos.add(new Pair<>(b, d));
				
				// player face
				if(curLoc!=null)
					if(!curLoc.isEmpty()){
						if(t!=null){ w = t.getWidth(); h = t.getHeight(); }
						b = new MenuObj(t, loc.x-w/2, loc.y-h/2, gs);
						b.setRotation(dirMapping.get(curLoc).getValue());
						mos.add(new Pair<>(b, new Vector2(7, 5)));
						b.clickable = false;
						b.getImage().flip(((Main)gs).character.isFacingLeft(), false);
					}
				
				// location Name
				b = new MenuObj("", SourceType.TEXT, x0 + 166, y0 - 206 , 40, 1, gs);
				b.y += b.height/2;
				mos.add(new Pair<>(b, new Vector2(6, 5)));
				b.clickable = false;
				
				// back
				h = 18;
				b = new MenuObj("Back", "default_button", x0 + 76 , y0 - 201 - h, gs);
				mos.add(new Pair<>(b, new Vector2(3, 5)));
				b.setMethod(Menu.class.getMethod("back", new Class[]{}), this);
				
				break;
			case OPTIONS:
				startPoint = new Vector2(1, 0);
				
				// music volume
				b = new MenuObj("MUSIC VOLUME", SourceType.TEXT, x0 + 51, y0 - 55, 100, 1, gs);
				b.clickable = false;
				mos.add(new Pair<>(b, new Vector2(0, 0)));
				
				// music volume slider
				b = new Slider(Game.class.getField("musicVolume"), Game.maxVolume, 240, 135.5f, gs);
				mos.add(new Pair<>(b, startPoint));
				
				// sound volume
				b = new MenuObj("SOUND VOLUME", SourceType.TEXT, x0 + 51, y0 - 70, 100, 1, gs);
				b.clickable = false;
				mos.add(new Pair<>(b, new Vector2(0, 1)));
				
				// sound volume slider
				b = new Slider(Game.class.getField("soundVolume"), Game.maxVolume, 240, 120.5f, gs);
				mos.add(new Pair<>(b, new Vector2(1, 1)));
				
				// window style left arrow
				b = new MenuObj("arrow_left", SourceType.IMAGE, x0 + 59, y0 - 95, 12, 18, gs);
				b.setMethod(Menu.class.getMethod("changeWindowMode", new Class[]{}), this);
				mos.add(new Pair<>(b, new Vector2(0, 2)));
				
				// window style right arrow
				b = new MenuObj("arrow_right", SourceType.IMAGE, x0 + 167, y0 - 95, 12, 18, gs);
				b.setMethod(Menu.class.getMethod("changeWindowMode", new Class[]{}), this);
				mos.add(new Pair<>(b, new Vector2(1, 2)));
				
				// window style text
				s = "WINDOWED"; if(Game.fullscreen) s = "FULLSCREEN";
				b = new MenuObj(s, SourceType.TEXT, x0 + 84, y0 - 90, 100, 1, gs);
				b.clickable = false;
				mos.add(new Pair<>(b, new Vector2(2, 2)));

				// back
				b = new MenuObj("Back", "default_button", x0 + 86, y0 - 126, gs);
				b.setMethod(Menu.class.getMethod("back", new Class[]{}), this);
				mos.add(new Pair<>(b, new Vector2(0, 3)));
				break;
			case YESNO:
				overlay = true;
				// text
				
				// YES
				b = new MenuObj("no", "no_button", Game.width/4 - 5, 5, gs);
				mos.add(new Pair<>(b, new Vector2(0, 1)));
				b.setMethod(Menu.class.getMethod("back", new Class[]{}), this);
				
				// NO
				b = new MenuObj("yes", "yes_button", Game.width/4 - 100, 5, gs);
				mos.add(new Pair<>(b, new Vector2(1, 1)));
				b.setMethod(Menu.class.getMethod("back", new Class[]{}), this);
				
				break;
			}
			
			// find size of object list
			maxX = 0; maxY = 0;
			for(Pair<MenuObj, Vector2> p : mos){
				Vector2 loc = p.getValue();
				if(maxX < loc.x) maxX = (int) loc.x;
				if(maxY < loc.y) maxY = (int) loc.y;
			}
			
			// sort and store references to buttons
			objs = new MenuObj[maxX+1][maxY+1];
			for(Pair<MenuObj, Vector2> p : mos){
				Vector2 loc = p.getValue();
				objs[(int) loc.x][(int) loc.y] = p.getKey();
				p.getKey().setMenuMapping(loc);
			}
			
			//project obj array to both vertical and horizontal list
			// TODO optimize!! this is TERRIBLE
			vertical = new Array<>();
			horizontal = new Array<>();
			for(MenuObj[] l : objs)
				for(MenuObj m : l){
					if(m==null) continue;
					if(!m.clickable)continue;
					vertical.add(m);
					
					projections.put(m, new Vector2(0, vertical.size -1));
				}
			
			for(int j = 0; j < objs[0].length; j++)
				for(int i = 0; i < objs.length; i++){
					MenuObj m = objs[i][j];
					if(m==null) continue;
					if(!m.clickable)continue;
					horizontal.add(objs[i][j]);
					
					projections.put(m, new Vector2(horizontal.size -1, projections.get(m).y));
				}
			
			// insure the default cursor is not mapped to an inactive or null object
			if(projections.get(getObj(startPoint))==null)
				startPoint = horizontal.get(0).getMenuMapping();
//			printHorizontally();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * disposes image textures and removes window; calls GameState.back()
	 * to remove 
	 */
	public void back() {
		for(MenuObj[] l : objs)
			for(MenuObj m : l){
				if(m==null) continue;
				m.dispose();
			}
		
		image.dispose();
		gs.back();
	}
	
	public Vector2 getNextObj(Vector2 start, int direction){
		MenuObj m = getObj(start);
		float x = projections.get(m).x;
		float y = projections.get(m).y;
		Vector2 next = start.cpy();
		
		switch (direction){
		case 1: // UP
			y = y > 0 ? y - 1 : vertical.size - 1;
			next = vertical.get((int) y).getMenuMapping();
			break;
		case 2: // RIGHT
			x = x < horizontal.size-1 ? x + 1 : x;
			next = horizontal.get((int) x).getMenuMapping();
			break;
		case 3: // DOWN
			y = y < vertical.size-1 ? y + 1 : 0;
			next = vertical.get((int) y).getMenuMapping();
			break;
		case 4: // LEFT
			x = x > 0 ? x - 1 : x;
			next = horizontal.get((int) x).getMenuMapping();
			break;
		}
		
		return next;
	}
	
	public void changeWindowMode(){
        Game.fullscreen = !Game.fullscreen;
        DisplayMode currentMode = Gdx.graphics.getDesktopDisplayMode();
        MenuObj m = null;
        
        for(MenuObj[] l : objs){
        	for(MenuObj obj : l){
        		if(obj==null) continue;
        		if(obj.getText()==null) continue;
        		if(obj.getText().equals("WINDOWED") || 
        				obj.getText().equals("FULLSCREEN")){
        			m = obj;
        			break;
        		}
        	}
        	if(m!=null) break;	
        }	
        
        if(Game.fullscreen){
        	Gdx.graphics.setDisplayMode(currentMode.width, currentMode.height, Game.fullscreen);
        	if(m!=null) {
        		m.setText("FULLSCREEN");
        	//move text
        	}
        } else {
        	Gdx.graphics.setDisplayMode(Game.width, Game.height, Game.fullscreen);
        	if(m!=null) {
        		m.setText("WINDOWED");
        	}
        }
	}
	
	public void updateMap(Vector2 prev){
		MenuObj m = getObj(prev);
		if(m!=null) //hide previous
			if(!m.hiLite)
				m.hidden = true;
		
		m = getObj(gs.cursor);
		if(!m.hiLite) {
			m.hidden = false; // show current
			MenuObj t = getObj(new Vector2(6, 5));
			if(t!=null) //update text
				t.setText(dirMapping.get(m.getText()).getKey());
		}
	}
	
	public void updateEntry(Vector2 prev){
		MenuObj m = getObj(prev);
		if(m!= null)
			if(m instanceof JournalEntry)
				((JournalEntry) m).deacvtivate();
		
		m = getObj(gs.cursor);
		if(m instanceof JournalEntry)
			((JournalEntry) m).activate();
	}
	
	public void printHorizontally(){
		for(MenuObj m : horizontal) System.out.println(m);
	}
	
	public MenuType getLeftTab(){
		if(tabs!=null) return tabs.getKey();
		else return null;
	}
	
	public MenuType getRightTab(){
		if(tabs!=null) return tabs.getValue();
		else return null;
	}
	
	public void increaseScrollOffX(){
		prevScroll = scrollOff.cpy();
		goalScroll = new Vector2(prevScroll.x + JournalEntry.PERIODX*4, prevScroll.y);
		scrolling = true;
		scrollTime = 0;
	}
	
	public void decreaseScrollOffX(){
		prevScroll = scrollOff.cpy();
		goalScroll = new Vector2(prevScroll.x - JournalEntry.PERIODX*4, prevScroll.y);
		scrolling = true;
		scrollTime = 0;
	}
	
	public Vector2 getScrollOff(){ return scrollOff; }
	public Array<MenuObj> getScrollObjs(){ return scrollObjs; }
	
	public void reload(){
		for(MenuObj[] l : objs)
			for(MenuObj m : l){
				if(m==null)continue;
				m.dispose();
			}
		objs = new MenuObj[0][0];
		addObjects();
	}
	
	public Vector2 getStartPoint(){ return startPoint; }
	
	/**
	 * find savegame file; if file at index exists, preload data and store it into a parseable string
	 * @param i savegame index
	 * @return string of format "playerType/lname/llocation/lplayTime"
	 */
	private String findSaveFile(int i){
		FileHandle src = Gdx.files.internal("saves/savegame"+i+".txt");
		if(src.exists())
			return JsonSerializer.getSummary(src);
		return null;
	}
	
	public String toString(){ return this.type.toString(); }
	public MenuObj getObj(Vector2 c){ return objs[(int)c.x][(int)c.y]; }
	
	private static final HashMap<String, Pair<String, PovDirection>> dirMapping = new HashMap<>();
	public static final HashMap<String, String> locToIMG = new HashMap<>();
	static{
		dirMapping.put("boardwalk", new Pair<>("Boardwalk", PovDirection.west));
		dirMapping.put("bridge", new Pair<>("Bridge", PovDirection.north));
		dirMapping.put("business", new Pair<>("Business District", PovDirection.north));
		dirMapping.put("central_park", new Pair<>("Central Park", PovDirection.east));
		dirMapping.put("church", new Pair<>("Church", PovDirection.west));
		dirMapping.put("commercial_NW", new Pair<>("Commercial District NW", PovDirection.south));
		dirMapping.put("commercial_S", new Pair<>("Commercial District S", PovDirection.north));
		dirMapping.put("crosswalk", new Pair<>("Crosswalk", PovDirection.north));
		dirMapping.put("downtown", new Pair<>("Downtown", PovDirection.north));
		dirMapping.put("factory", new Pair<>("Factory", PovDirection.north));
		dirMapping.put("hero_HQ", new Pair<>("Hero HQ", PovDirection.west));
		dirMapping.put("high_rise", new Pair<>("High Rise", PovDirection.north));
		dirMapping.put("mansion", new Pair<>("Mansion", PovDirection.west));
		dirMapping.put("municipal", new Pair<>("Municipal District", PovDirection.north));
		dirMapping.put("residential_N", new Pair<>("Residential District N", PovDirection.south));
		dirMapping.put("residential_SE", new Pair<>("Residential District SE", PovDirection.north));
		dirMapping.put("residential_SW", new Pair<>("Residential District SW", PovDirection.north));
		dirMapping.put("tha_hood", new Pair<>("Tha Hood", PovDirection.south));
		dirMapping.put("under_bridge", new Pair<>("Under Bridge", PovDirection.west));
		dirMapping.put("villain_HQ", new Pair<>("Villain HQ", PovDirection.north));
		dirMapping.put("warehouse", new Pair<>("Warehouse", PovDirection.west));
		
		// scene names (.tmx) to .png map files
		locToIMG.put("Boardwalk", "boardwalk");
		locToIMG.put("Bridge", "bridge");
		locToIMG.put("BusinessDistrict", "business");
		locToIMG.put("Church", "church");
		locToIMG.put("CentralPark", "central_park");
		locToIMG.put("CommercialDistrictNW", "commercial_NW");
		locToIMG.put("CommercialDistrictS", "commercial_S");
		locToIMG.put("Crosswalk", "crosswalk");
		locToIMG.put("Downtown", "downtown");
		locToIMG.put("Factory", "factory");
		locToIMG.put("HeroHQ", "hero_HQ");
		locToIMG.put("HighRise", "high_rise");
		locToIMG.put("Mansion", "mansion");
		locToIMG.put("municipal", "municipal");
		locToIMG.put("ResidentialDistrictN", "residential_N");
		locToIMG.put("ResidentialDistrictSE", "residential_SE");
		locToIMG.put("ResidentialDistrictSW", "residential_SW");
		locToIMG.put("ThaHood", "tha_hood");
		locToIMG.put("UnderBridge", "under_bridge");
		locToIMG.put("VillainHQ", "villain_HQ");
		locToIMG.put("Warehouse", "warehouse");
	}
}
