package handlers;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import entities.Entity;
import entities.Mob;
import entities.MobAI.ResetType;

public class Animation {

	public int priority, type, transType, timesPlayed;

	private TextureRegion[] primaryFrames, transFrames, baseFrames;
	private float primaryDelay, transDelay, baseDelay;
	private float time, totTime, resetTime;
	private int currentIndex;
	private LoopBehavior loopBehavior; 
	private Entity owner;
	boolean backwards;

	public enum LoopBehavior {
		TIMED, ONCE, CONTINUOUS;
	}

	public void initFrames(TextureRegion[] baseFrames, float baseDelay, boolean direction) {
		this.baseFrames = baseFrames;
		this.baseDelay = baseDelay;
		if(direction) flip(true);
	}

	public Animation(Entity owner) {this.owner = owner; }

	public void update(float dt) {
		TextureRegion[] frames = transFrames; 
		float delay = transDelay;
		if (frames == null) {
			frames = primaryFrames;
			delay = primaryDelay;
		}
		if (frames == null) {
			frames = baseFrames;
			delay = baseDelay;
		}

		//		if(owner.toString().contains("Trevon"))
		//			printDebug(frames, delay);

		time += dt;
		totTime += dt;
		if (time >= delay) {
			time = 0;
			if (backwards) currentIndex--;
			else currentIndex++;

			//reached end of animation
			if ((backwards&&currentIndex == -1) || (!backwards&&currentIndex == frames.length)) {
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
						System.out.println(totTime+" :: "+resetTime);
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
				else
					//continue base loop
					currentIndex = 0;
			}
		}
	}

	/**get the image at the current index of the animation*/
	public TextureRegion getFrame() {
		TextureRegion[] frames = transFrames; 
		if (frames == null)
			frames = primaryFrames;
		if (frames == null)
			frames = baseFrames;
		if(currentIndex>=frames.length)
			currentIndex = frames.length-1;
		return frames[currentIndex]; 
	}

	public void printDebug(TextureRegion[] frames, float delay){
		String s;
		s =loopBehavior + " "+Mob.getAnimName(type) + "("+ (currentIndex+1) +"/"+frames.length+")\t:: ";
		if(transFrames!=null)	
			s+=Mob.getAnimName(transType) + "("+ (currentIndex+1) +"/"+frames.length+")";
		else
			s+="_____(0/0)";
		System.out.println(s);
	}

	public void setFrames(TextureRegion[] frames, float delay, int priority, int type,
			LoopBehavior loop, float resetTime, boolean backwards) {
		if ((priority >= this.priority && type != this.type) || 
				backwards !=this.backwards) {
			this.primaryFrames = frames;
			this.loopBehavior = loop;
			this.resetTime = resetTime;
			this.priority = priority;
			this.type = type;
			this.primaryDelay = delay;
			this.backwards = backwards;
			totTime = 0;
			time = 0;

			if (backwards) 
				currentIndex = frames.length - 1;
			else 
				currentIndex = 0;

			if(owner!=null)
				if (owner.isFacingLeft()) 
					flip(true);
			timesPlayed = 0;
		}
	}

	public void setFrames(TextureRegion[] frames, float delay, int priority, int type, LoopBehavior loop) {
		setFrames(frames, delay, priority, type, loop, 0, false);
	}

	public void setFrames(TextureRegion[] frames, float delay, int priority, int type, LoopBehavior loop, 
			boolean backwards) {
		setFrames(frames, delay, priority, type, loop, 0, backwards);
	}

	public void setWithTransition(TextureRegion[] transFrames, float transDelay, int transType,
			TextureRegion[] primaryFrames, float primaryDelay, int priority, int type) {
		setWithTransition(transFrames, transDelay, transType, primaryFrames, primaryDelay, priority, type,
				LoopBehavior.CONTINUOUS, 0);
	}

	public void setWithTransition(TextureRegion[] transFrames, float transDelay, int transType, 
			TextureRegion[] primaryFrames, float primaryDelay, int priority, int type, 
			LoopBehavior loop, float resetTime) {
		if (priority >= this.priority && type != this.type) {
			setFrames(primaryFrames, primaryDelay, priority, type, loop, resetTime, false);
			currentIndex = 0;
			this.transFrames = transFrames;
			this.transDelay = transDelay;
			this.transType = transType;

			if(owner!=null)
				if (owner.isFacingLeft()) 
					flip(true);
		}
	}

	public void addTransition(TextureRegion[] transFrames, float transDelay, int transType, boolean direction) {
		this.transFrames = transFrames;
		this.transDelay = transDelay;
		this.transType = transType;
		currentIndex = 0;
		totTime = 0;
		time = 0;
		if(owner!=null)
			if (owner.isFacingLeft()) 
				flip(true);
	}

	private void removePrimaryFrames() {
		primaryFrames = null;
		primaryDelay = 0;
		priority = 0;
		type = 0;
		loopBehavior = null;
		currentIndex = 0;
		timesPlayed = 0;
		resetTime = 0;
		backwards = false;

		if(owner!=null)
			if(owner instanceof Mob)
				if(((Mob)owner).getCurrentState().resetType.equals(ResetType.ON_ANIM_END))
					((Mob) owner).resetState();
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

	public void setBaseDelay(float delay) { this.baseDelay = delay; }
	public float getSpeed() {
		TextureRegion[] f = transFrames;
		float d = transDelay;
		if(f==null){
			f=primaryFrames;
			d=primaryDelay;
		} if(f==null) d=baseDelay;

		return d; 
	}

	public int getCurrentType(){
		int i = type;
		if(transFrames!=null)
			i = transType;
		return i;
	}

	public boolean hasTrans(){ return transFrames==null; }
	public int getTimesPlayed() { return timesPlayed; }
	public int getDefaultLength() { return baseFrames.length; }
	public int getIndex(){
		if(!backwards)
			return currentIndex;
		else
			return primaryFrames.length - currentIndex - 1;
	}
}
