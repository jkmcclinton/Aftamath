package scenes;

import handlers.Vars;
import main.Game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;

public class Song {

	public Music intro, main;
	public String title;
	public boolean fading, looping;
	public float prevVolume;
	
	private boolean stopped, paused, toStop;
	private int state, fadeType;
	private float volume, goalVolume, speed;
	
	private static final int INTRO=0;
	private static final int MAIN=1;
	private static final int FADE_OUT=-1;
	private static final int FADE_IN=1;
	private static final float NORMAL=1;
	public static final float FAST=3;
	
	public Song(Music intro, Music main){
		this.intro = intro;
		this.main = main;
		volume = Game.musicVolume;
	}
	
	public Song(String src){
		this(src, true);
	}
	
	public Song(String src, boolean looping){
		if(Vars.isNumeric(src)){
			int m = Integer.parseInt(src);
			if (m<0||m>=Game.SONG_LIST.size)
				System.out.println("\""+src+"\" is an invalid index for music.");
			else src = Game.SONG_LIST.get(m);
		}
		
		title = new String(src);
		volume = Game.musicVolume;
		this.looping = looping;
		
		try{
			state=INTRO;
			intro = Gdx.audio.newMusic(new FileHandle("res/music/"+src+" Intro.wav"));
		} catch( Exception e){state=MAIN;}
		
		try{
			this.main = Gdx.audio.newMusic(new FileHandle("res/music/"+src+".wav"));
		} catch(Exception e){
			System.out.println("\""+src+"\" is not a valid song title. See /Aftamath/res/music for list of songs.");
//			e.printStackTrace();
			this.main = Gdx.audio.newMusic(new FileHandle("res/music/Silence.wav"));
		} finally {
			if(looping) this.main.setLooping(true);
		}
	}
	
	public void update(float dt){
		if(!paused&&!stopped)
			if(state==INTRO){
				if(!intro.isPlaying()&&!main.isPlaying()){
					main.setVolume(volume);
					main.play();
					state=MAIN;
				}
			} 

		if(fading){
			fade(dt);
		}else fadeType=0;
	}
	
	public void play(){
		if (state==INTRO) {
//			intro.setVolume(volume);
			intro.play();
		} else {
//			main.setVolume(volume);
			main.play();
		}
		stopped = paused = false;
	}
	
	public void pause(){
		if(state==INTRO)
			intro.pause();
		else main.pause();
		paused = true;
	}
	
	public void stop(){
		if(state==INTRO)
			intro.stop();
		else main.stop();
		
		if(intro!=null)
			state=INTRO;
				
		stopped = true;
		setVolume(Game.musicVolume);
	}
	
	public void fade(float dt){
//		System.out.println(title+": "+volume);
		float volume = getVolume();
		volume += dt * fadeType * speed;
		if (volume >= goalVolume && fadeType==FADE_IN){
			volume = goalVolume;
			fading = false;
			fadeType=0;
		} if (volume <= goalVolume && fadeType==FADE_OUT){
			volume = goalVolume;
			
			if(toStop)
				dispose();
			else if(goalVolume==0f)
				pause();
			
			fading = toStop = false;
			fadeType=0;
		}
		
		setVolume(volume);
	}
	
	public void fadeOut(){
		fadeOut(true, NORMAL);
	}
	
	public void fadeOut(float goal){
		fadeOut(goal, false, NORMAL);
	}
	
	public void fadeOut(boolean stop){
		fadeOut(0f, stop, NORMAL);
	}
	
	public void fadeOut(boolean stop, float speed){
		fadeOut(0f, stop, speed);
	}
	
	public void fadeOut(float goal, float speed){
		fadeOut(goal, false, speed);
	}
	
	public void fadeOut(float goal, boolean stop, float speed){
		if(fading && fadeType==FADE_OUT) return;
		fadeType = FADE_OUT;
		goalVolume = goal;
		prevVolume = volume;
		toStop = stop;
		fading = true;
		this.speed=speed;
	}
	
	public void fadeIn(){
		fadeIn(Game.musicVolume);
	}
	
	public void fadeIn(float goal){
		if(fading && fadeType==FADE_IN) return;
		fadeType = FADE_IN;
		goalVolume = goal;
		toStop = false;
		fading = true;
		speed=NORMAL;
		
		if(!isPlaying()) play();
	}
	
	public void dispose(){
		if(intro!=null) {
			intro.stop();
			intro.dispose();
		}
		
		main.stop();
		main.dispose();
	}
	
	public void setVolume(){ setVolume(this.volume); }
	public void setVolume(float volume){ 
		if(intro!=null)intro.setVolume(volume);
		main.setVolume(volume);
		this.volume=volume;
	}
	
	public float getVolume(){
		if(state==INTRO) return intro.getVolume();
		return main.getVolume();
	}
	
	public boolean isPlaying(){
		if(state==INTRO)
			return intro.isPlaying();
		else return main.isPlaying();
	}
	
	public String toString(){ return title; }
	
	public Song copy(){
		Song s = new Song(title);
		s.fading=false;
		return s;
	}
	
	public boolean equals(Object o){
		if(o instanceof Song)
			return ((Song)o).title.equals(title);
		return false;
	}
}
