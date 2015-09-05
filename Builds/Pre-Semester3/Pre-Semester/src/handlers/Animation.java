package handlers;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import entities.Mob;
import entities.Mob.Action;

public class Animation {

	public boolean transitioning, controlled;
	public int currentFrame;
	public int actionIndex, nextID;

	private float time;
	private float defaultDelay;
	private float delay;
	private int timesPlayed;
	private Array<TextureRegion[]> frames;
	private int actionLength, nextActionLength;
	
	public Animation(){}

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
		add(frames);
		this.defaultDelay = delay;
		this.delay = defaultDelay;
		actionIndex = 0;

		time = 0;
		currentFrame = 0;
		timesPlayed = 0;

		if (flip) flip(flip);
	}

	public void setFrames(TextureRegion frames, float delay){
		this.frames = new Array<>();
		TextureRegion[] tmp = {frames};
		add(tmp);
		this.delay = delay;
		actionIndex = 0;

		time = 0;
		currentFrame = 0;
		timesPlayed = 0;
	}

	public void setAction(TextureRegion[] frames, int length, boolean direction, int ID, boolean controlled){
		setAction(frames, length, direction, ID, Vars.ACTION_ANIMATION_RATE, controlled);
	}

	public void setAction(TextureRegion[] frames, int length, boolean direction, int ID, float delay, boolean controlled) {
		if (actionIndex == ID && actionIndex != Mob.animationIndicies.get(Action.JUMPING)) return;

		if(!add(frames))
			return;
		
		this.controlled = controlled;
		actionLength = length;
		actionIndex = ID;
		this.delay = delay;
		transitioning = false;

		if (direction) flipAction(true);

		time = 0;
		currentFrame = 0;
		timesPlayed = 0;
	}

	public void setTransitionAction(TextureRegion[] frames, int transLength, boolean direction, int transID,
			TextureRegion[] transFrames, int length, int ID, boolean looping){
		if(actionIndex == ID){
			return;
		}

		removeAction();
		transitioning = true;
		controlled = true;
		
		if(!add(frames))
			return;
		this.delay = Vars.ACTION_ANIMATION_RATE;
		nextActionLength = length;
		nextID = ID;
		
		add(transFrames);
		actionLength = transLength;
		actionIndex = transID;

		if (direction) flipAction(true);

		time = 0;
		currentFrame = 0;
		timesPlayed = 0;
	}

	public void removeTop(){
		if(frames == null) return;
		if (frames.size <= 1) return;
		frames.pop();

		actionIndex = nextID;
		timesPlayed = 0;
		currentFrame = 0;
		actionLength = nextActionLength;
	}

	public void removeAction(){
		if(frames == null) return;
		if (frames.size <= 1) return;

		while (frames.size > 1) {
			frames.pop();
		}

		timesPlayed = 0;
		currentFrame = 0;
		actionLength = 0;
		actionIndex = 0;
		delay = defaultDelay;
		transitioning = false;
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
		TextureRegion[] first = frames.get(0).clone();
		Array<TextureRegion[]> tmp = new Array<TextureRegion[]>();
		tmp.add(first);

		int i = 1;
		while (i < frames.size){
			TextureRegion[] flipped = frames.get(i).clone();
			for (TextureRegion t : flipped){
				if(t.isFlipX()!=flip) t.flip(true, false);
			}
			i++;
			tmp.add(flipped);
		}

		frames = new Array<TextureRegion[]>();
		frames.addAll(tmp);
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
			if(timesPlayed > 0 ) {
				if (transitioning){
					if(frames.size > 2)
						removeTop();
					else if(!controlled)
						removeAction();
				} else if (frames.size > 1 && !controlled)
					removeAction();
			}
		}
	}
	
	private boolean add(TextureRegion[] frames){
//		if(this.frames.size>=2 && !transitioning)
//			return false;
		this.frames.add(frames);
		return true;
	}

	public TextureRegion getFrame() {
		return frames.peek()[currentFrame]; 
	}
	public int getTimesPlayed() { return timesPlayed; }
	public Array<TextureRegion[]> getFrames(){return frames; }
	public void setSpeed(float delay) { this.delay = delay; }
	public float getSpeed() { return delay; }
	public int getSize() { return frames.size; }
	public int getDefaultLength() { return frames.get(0).length; }
	public int getIndex() {
		return currentFrame;
	}

}
