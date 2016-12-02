package handlers;

import main.GameState;
import main.Main;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;

public class PositionalAudio {
	
	public Music sound;
	public Vector2 location;
	
	public PositionalAudio(Vector2 location, String src, GameState gs){
		this.location = location;
		sound = Gdx.audio.newMusic(new FileHandle("assets/sounds/"+src+".wav"));
		sound.setLooping(true);
		gs.playSound(location, sound);
		if(gs instanceof Main)
			((Main) gs).addSound(this);
	}
	
	public void stop(){
		sound.stop();
	}

	public void dispose() {
		sound.dispose();
	}
}
