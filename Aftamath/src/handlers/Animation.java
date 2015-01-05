package handlers;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import entities.Mob;

public class Animation {

	private float time;
	private float defaultDelay;
	private float delay;
	private int timesPlayed;
	public int currentFrame;
	
	private Array<TextureRegion[]> frames;
	private int actionLength;
	public int actionID;
	
	public Animation(){
	}
	
	public Animation(TextureRegion[] frames){
		this(frames, 1/12f);
	}
	
	public Animation(TextureRegion[] frames, float delay){
		setFrames(frames, delay, false);
	}
	
	public void setFrames(TextureRegion[] frames, boolean flip) {
		setFrames(frames, Vars.ANIMATION_RATE, flip);
	}
	
	public void setFrames(TextureRegion[] frames, float delay, boolean flip){
		this.frames = new Array<>();
		this.frames.add(frames);
		this.defaultDelay = delay;
		this.delay = defaultDelay;
		actionID = 0;
		
		time = 0;
		currentFrame = 0;
		timesPlayed = 0;
		
		if (flip) flip(flip);
	}
	
	public void setFrames(TextureRegion frames, float delay){
		this.frames = new Array<>();
		TextureRegion[] tmp = {frames};
		this.frames.add(tmp);
		this.delay = delay;
		actionID = 0;
		
		time = 0;
		currentFrame = 0;
		timesPlayed = 0;
	}
	
	public void setAction(TextureRegion[] frames, int length, boolean direction, int ID){
		setAction(frames, length, direction, ID, Vars.ANIMATION_RATE);
	}
	
	public void setAction(TextureRegion[] frames, int length, boolean direction, int ID, float delay) {
		if (actionID == ID) return;
		
		this.frames.add(frames);
		actionLength = length;
		actionID = ID;
		this.delay = delay;
		
		if (direction) flipAction(true);
		
		time = 0;
		currentFrame = 0;
		timesPlayed = 0;
	}
	
	public void removeAction(){
		if (frames.size <= 1) return;
		
		while (frames.size > 1) {
			frames.pop();
		}
		
		timesPlayed = 0;
		currentFrame = 0;
		actionLength = 0;
		actionID = 0;
		delay = defaultDelay;
	}
	
	public void flip (boolean flip){
		for (int i = 0; i<frames.size; i++){
			TextureRegion[] flipped = frames.get(i).clone();
			for(TextureRegion t : flipped){
				if(t.isFlipX()!=flip) t.flip(true, false);
			}
		
			frames.set(i, flipped);
		}
	}
	
	public void flipAction(boolean flip){
		TextureRegion[] flipped = frames.peek().clone();
		for (TextureRegion t : flipped){
			if(t.isFlipX()!=flip) t.flip(true, false);
		}
		
		frames.pop();
		frames.add(flipped);
	}
	
	public void update(float dt){
		if(delay <= 0) return;
		time += dt;
		while(frames.size >= 1 && time >= delay){
			step();
		}
	}
	
	private void step(){
		time -= delay;
		currentFrame++;
		if((frames.size == 1 && currentFrame == frames.peek().length) || (frames.size > 1 && currentFrame == actionLength)){
			currentFrame = 0;
			timesPlayed++;
			if(timesPlayed > 0 && frames.size > 1) removeAction();
		}
	}
	
	public TextureRegion getFrame() { 
		return frames.peek()[currentFrame]; 
		}
	public int getTimesPlayed() { return timesPlayed; }

	public void setSpeed(float delay) { this.delay = delay; }
	public float getSpeed() { return delay; }
	public int getSize() { return frames.size; }
	
}
