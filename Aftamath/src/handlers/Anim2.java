package handlers;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Anim2 {
	private TextureRegion[] primaryFrames, transFrames, baseFrames;
	private float primaryDelay, transDelay, baseDelay;
	public int priority, type, timesPlayed;
	private LoopBehavior loopBehavior; 
	private int currentIndex;
	private float time;
	private float totTime;
	private float resetTime;
	boolean backwards;

	public enum LoopBehavior {
		TIMED, ONCE, CONTINUOUS;
	}

	public Anim2(TextureRegion[] baseFrames, float baseDelay) {
		this.baseFrames = baseFrames;
		this.baseDelay = baseDelay;
	}

	public Anim2(TextureRegion[] baseFrames) {
		this(baseFrames, 0);
	}

	public Anim2() {}

	public void update(float dt) {
		TextureRegion[] frames = transFrames; 
		float delay = transDelay;
		if (transFrames == null) {
			frames = primaryFrames;
			delay = primaryDelay;
		}
		if (primaryFrames == null) {
			frames = baseFrames;
			delay = baseDelay;
		}

		time += dt;
		totTime += dt;
		if (time >= delay) {
			time = 0;
			if (backwards) currentIndex--;
			else currentIndex++;
	
			//reached end of animation
			if (currentIndex == -1 || currentIndex == frames.length) {
				//remove transition loop
				if (transFrames != null) {
					transFrames = null;
					transDelay = 0;
					if (backwards && primaryFrames != null) currentIndex = primaryFrames.length - 1;
					else currentIndex = 0;
				}
				//remove primary loop
				else if (primaryFrames != null) {
					switch (loopBehavior) {
					case ONCE:
						removePrimaryFrames();
						break;
					case TIMED:
						if (totTime >= resetTime)
							removePrimaryFrames();
						else {
							currentIndex = backwards ? frames.length - 1 : 0;
							timesPlayed++;
						}
						break;
					case CONTINUOUS:
						currentIndex = backwards ? frames.length - 1 : 0;
						timesPlayed++;
						break;
					}
				}
				else {
					currentIndex = 0;
				}
			}
		}
	}

	/**get the image at the current index of the animation*/
	public TextureRegion getFrame() {
		TextureRegion[] frames = transFrames; 
		if (transFrames == null)
			frames = primaryFrames;
		if (primaryFrames == null)
			frames = baseFrames;
		return frames[currentIndex]; 
	}

	public void setFrames(TextureRegion[] frames, float delay, int priority, int type, boolean direction,
			LoopBehavior loop, float resetTime, boolean backwards) {
		if (priority >= this.priority && type != this.type) {
			this.primaryFrames = frames;
			this.loopBehavior = loop;
			this.resetTime = resetTime;
			
			if (backwards) 
				currentIndex = frames.length - 1;
			else 
				currentIndex = 0;
			if (direction) 
				flip(true);
			timesPlayed = 0;
		}
	}

	public void setFrames(TextureRegion[] frames, float delay, int priority, int type, boolean direction) {
		setFrames(frames, delay, priority, type, direction, LoopBehavior.ONCE, 0, false);
	}

	public void setFrames(TextureRegion[] frames, float delay, int priority, int type, boolean direction, 
			boolean backwards) {
		setFrames(frames, delay, priority, type, direction, LoopBehavior.ONCE, 0, backwards);
	}

	public void setWithTransition(TextureRegion[] transFrames, float transDelay, 
			TextureRegion[] primaryFrames, float primaryDelay, int priority, int type, boolean direction) {
		setWithTransition(transFrames, transDelay, primaryFrames, primaryDelay, priority, type, direction,
				LoopBehavior.CONTINUOUS, 0);
	}

	public void setWithTransition(TextureRegion[] transFrames, float transDelay, 
			TextureRegion[] primaryFrames, float primaryDelay, int priority, int type, boolean direction, 
			LoopBehavior loop, float resetTime) {
		if (priority >= this.priority && type != this.type) {
			setFrames(primaryFrames, primaryDelay, priority, type, direction, loop, resetTime, false);
			currentIndex = 0;
			this.transFrames = transFrames;
			this.transDelay = transDelay;
		}
	}

	private void removePrimaryFrames() {
		primaryFrames = null;
		primaryDelay = 0;
		priority = 0;
		type = 0;
		currentIndex = 0;
		timesPlayed = 0;
	}
	
	/**return the animation to its default loop of images*/
	public void reset(){
		removePrimaryFrames();
		transFrames = null;
	}

	/**Horizontally flip all images in animations to match the direction it should currently
	 * be facing*/
	public void flip(boolean flip) {
		if (transFrames != null) {
			for (TextureRegion t: transFrames) {
				if (t.isFlipX() != flip) {
					t.flip(true, false);
				}
			}
		}
		if (primaryFrames != null) {
			for (TextureRegion t: primaryFrames) {
				if (t.isFlipX() != flip) {
					t.flip(true, false);
				}
			}
		}
		for (TextureRegion t: baseFrames) {
			if (t.isFlipX() != flip) {
				t.flip(true, false);
			}
		}
	}


	public void setPrimarySpeed(float delay) { this.primaryDelay = delay; }
	public float getPrimarySpeed() { return primaryDelay; }
	public int getTimesPlayed() { return timesPlayed; }
	public int getDefaultLength() { return baseFrames.length; }
	public int getIndex(){
		if(!backwards)
			return currentIndex;
		else
			return primaryFrames.length - currentIndex - 1;
	}

}
