package handlers;

import java.util.ArrayDeque;

import main.Game;
import main.GameState;
import main.Play;
import main.Title;

public class GameStateManager {

	private Game game;
	private ArrayDeque<GameState> gameStates;
	private boolean fading;
	private int fadeType = 1;
	
	public static final int PLAY = 1;
	public static final int TITLE = 2;
	
	public float volume = Game.musicVolume;
	
	public GameStateManager(Game game) {
		this.game = game;
		gameStates = new ArrayDeque<GameState>();
//		pushState(TITLE);
		pushState(PLAY);
		gameStates.peek().create();
	}

	public void update(float dt) {
		GameState g = gameStates.peekFirst();
		boolean faded = game.getSpriteBatch().update(dt);
		
		if(fading){
			volume += dt/2 * fadeType;
			if (volume > Game.musicVolume)
				volume = Game.musicVolume;
			if (volume < 0)
				volume = 0;

			g.getSong().setVolume(volume);

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
	
	public GameState getState(int state){
		if(state == PLAY) return new Play(this);
		if(state == TITLE) return new Title(this);
		return null;
	}
	
	public void setState(int state) {
		setState(state, false);
	}
	
	public void setState(int state, boolean fade){
		if (fade) {
			pushState(state);
			fading = true;
			fadeType = -1;
			gameStates.peekFirst().getSpriteBatch().fade();
		} else {
			popState();
			pushState(state);
		}
	}

	private void pushState(int state) {
		if(gameStates.size() == 2)
			return;
		
		gameStates.add(getState(state));
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
			result += gs.getClass().getName();
		
		System.out.println(result);
	}

}
