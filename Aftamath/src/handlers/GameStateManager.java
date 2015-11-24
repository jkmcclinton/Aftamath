package handlers;

import java.util.ArrayDeque;

import scenes.Song;
import main.Game;
import main.GameState;
import main.Main;
import main.Title;

public class GameStateManager {

	private Game game;
	private ArrayDeque<GameState> gameStates;
	private boolean fading;
	private int fadeType = 1;
	
	public static final int MAIN = 1;
	public static final int TITLE = 2;
	
	public float volume = Game.musicVolume;
	
	public GameStateManager(Game game) {
		this.game = game;
		gameStates = new ArrayDeque<GameState>();
//		pushState(TITLE);
		pushState(MAIN);
		gameStates.peek().create();
	}

	public void update(float dt) {
		GameState g = gameStates.peekFirst();
		boolean faded = game.getSpriteBatch().update(dt);
		
		if(fading){
			if(fadeType==1){
				volume += dt * fadeType;
				if (volume < 0)
					volume = 0;
			} else 
				volume = Game.musicVolume;

//			g.getSong().setVolume(volume);

			if(faded)
				if(fadeType == -1){
					fadeType = 1;
					popState();
				} else  {
					fading = false;
				}
		}
		
		g.update(dt);
	}
	
	public void render() {
		gameStates.peekFirst().render();
	}
	
	public GameState initState(int state, Object... args){
		if(state == MAIN) {
			String filename = null;
			if (args.length > 0) {
				filename = args[0].toString();
			}
			return new Main(this, filename);
		}
		if(state == TITLE) {
			return new Title(this);
		}
		return null;
	}
	
	public GameState getState(){ return gameStates.peekFirst();}
	
	public void setState(int state) {
		setState(state, false);
	}
	
	public void setState(int state, boolean fade, Object... args) {
		GameState g = gameStates.peekFirst();
		if (fade) {
			pushState(state, args);
			fading = true;
			fadeType = -1;
			g.getSpriteBatch().fade();
			Song s = g.getSong();
			if(s!=null)
				s.fadeOut();
		} else {
			popState();
			pushState(state, args);
		}		
	}
	
	private void pushState(int state, Object... args) {
		if(gameStates.size() == 2)
			return;
		
		gameStates.add(initState(state, args));
	}
	
	private void popState() {
		gameStates.pop().dispose();
		gameStates.peekFirst().create();
		gameStates.peekFirst().getSong().play();
	}
	
	public Game game() { return game; }
	
	public boolean isFading() { return fading; }
	public ArrayDeque<GameState> getStates(){ return gameStates; }
	
	public void printStates(){
		String result = "";
		
		for(GameState gs : gameStates)
			result += gs.getClass().getName()+", ";
		
		System.out.println(result);
	}

}
