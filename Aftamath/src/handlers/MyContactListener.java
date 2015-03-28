package handlers;

import main.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.utils.Array;
import entities.Mob;
import entities.Player;
import entities.Projectile;
import entities.SpeechBubble;

public class MyContactListener implements ContactListener {
	
	public Array<Body> bodiesToRemove;
	public static Array<String> unstandable = new Array<>();
	
	static {
		//objects that cannot be stood ontop of
		unstandable.addAll("wall", "foot", "interact");
	}
	
	public MyContactListener(){
		super();
		bodiesToRemove = new Array<>();
	}
	
	public void beginContact(Contact c) {
		Fixture fa = c.getFixtureA();
		Fixture fb = c.getFixtureB();
		Entity entA = (Entity) fa.getBody().getUserData();
		String typeA = (String) fa.getUserData();
		Entity entB = (Entity) fb.getBody().getUserData();
		String typeB = (String) fb.getUserData();
		
		if(typeB.equals("foot") && !unstandable.contains(typeA, true) && 
				!fb.getBody().getUserData().equals(typeA)) {
			Gdx.audio.newSound(new FileHandle("res/sounds/step1.wav")).play(Game.volume);
			((Mob) entB).numContacts++;
		}
		
		if(typeA.equals("foot") && !unstandable.contains(typeB, true) && 
				!fa.getBody().getUserData().equals(typeB)) {
			Gdx.audio.newSound(new FileHandle("res/sounds/step1.wav")).play(Game.volume);
			((Mob) entA).numContacts++;
		}
		
		if(typeB.equals("projectile")) {
			if (entA instanceof Mob){
				if (((Projectile) entB).getOwner() != entA) {
					Gdx.audio.newSound(new FileHandle("res/sounds/chirp2.wav")).play(Game.volume);
					((Mob) entA).damage(((Projectile) entB).getDamageVal());
					bodiesToRemove.add(entB.getBody());
				}
			} else {
				Gdx.audio.newSound(new FileHandle("res/sounds/chirp2.wav")).play(Game.volume);
				bodiesToRemove.add(entB.getBody());
			}
		}
		
		if(typeA.equals("reaper") && typeB.indexOf("player") != -1){
			((Mob) entB).damage(1);
		}
		
		if(typeB.equals("reaper") && typeA.indexOf("player") != -1){
			((Mob) entA).damage(1);
		}
		
		if(typeA.equals("projectile")) {
			if(entB instanceof Mob){
				if (((Projectile) entA).getOwner() != entB) {
					Gdx.audio.newSound(new FileHandle("res/sounds/chirp2.wav")).play(Game.volume);
					((Mob) entB).damage(((Projectile) entA).getDamageVal());
					bodiesToRemove.add(entA.getBody());
				}
			} else {
				Gdx.audio.newSound(new FileHandle("res/sounds/chirp2.wav")).play(Game.volume);
				bodiesToRemove.add(entB.getBody());
			}
		}
		
		if(typeB.equals("interact") && !fa.isSensor() && entA.isInteractable){
//			if(entB.getScript()!=null){
				((Mob) entB).setInteractable(entA);
				if(entB instanceof Player) 
					new SpeechBubble(entA, entA.getPosition().x*Vars.PPM + 6, entA.rh   +
							entA.getPosition().y*Vars.PPM, 1);
//			}
		}
		
		if (typeA.equals("warp")) {
			if(((Warp) entA).conditionsMet()) {
				//add scene to player's interactable
			}
		}
		
		if (typeB.equals("warp")) {
			if(((Warp) entB).conditionsMet()) {
				//add scene to player's interactable
			}
		}
		
//		System.out.println("begin: " + typeA + " : " + typeB);
		
	}
	
	public void endContact(Contact c){
		Fixture fa = c.getFixtureA();
		Fixture fb = c.getFixtureB();
		
		if (fa == null || fb == null) return;
		
		Entity dispA = (Entity) fa.getBody().getUserData();
		String faUD = (String) fa.getUserData();
		Entity dispB = (Entity) fb.getBody().getUserData();
		String fbUD = (String) fb.getUserData();
		
		if(fbUD.equals("foot") && !unstandable.contains(faUD, true) && 
				!fb.getBody().getUserData().equals(faUD)) {
			((Mob) dispB).numContacts--;
		}
		
		if(faUD.equals("foot") && !unstandable.contains(fbUD, true) && 
				!fa.getBody().getUserData().equals(fbUD)) {
			//Gdx.audio.newSound(new FileHandle("res/sounds/step1.wav")).play();
			((Mob) dispA).numContacts--;
		}
		
		if(fbUD.equals("interact")){
			if(dispA == ((Mob) dispB).getInteractable())
				((Mob) dispB).setInteractable(null);
		}
		
//		System.out.println("end: " + fbUD + " : " + faUD);
	}
	
	public void preSolve(Contact c, Manifold m){}
	public void postSolve(Contact c, ContactImpulse ci){}
	public Array<Body> getBodies() { return bodiesToRemove; }

}














