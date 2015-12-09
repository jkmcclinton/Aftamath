package handlers;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.utils.Array;

import entities.DamageField;
import entities.Entity;
import entities.Ground;
import entities.Mob;
import entities.Projectile;
import entities.SpeechBubble;
import entities.SpeechBubble.PositionType;
import entities.TextBox;
import entities.Warp;
import main.Main;

public class MyContactListener implements ContactListener {

	//	public Array<Body> bodiesToRemove;
	public static Array<String> unstandable = new Array<>();
	public static Array<String> unHittable = new Array<>();

	private Main main;

	static {
		//objects that cannot be stood ontop of
		unstandable.addAll("wall", "foot", "interact", "attack", "center", "vision", 
				"tiledobject", "refocusTrigger", "texttrigger");
		unHittable.addAll("warp", "eventTrigger", "texttrigger", "refocusTrigger", "damageField",
				"foot", "interact", "attack", "center", "vision","tiledobject");
	}

	public MyContactListener(Main p){
		super();
		main = p;
	}

	public void beginContact(Contact c) {
		Fixture fa = c.getFixtureA();
		Fixture fb = c.getFixtureB();
		Entity entA = (Entity) fa.getBody().getUserData();
		String typeA = (String) fa.getUserData();
		Entity entB = (Entity) fb.getBody().getUserData();
		String typeB = (String) fb.getUserData();

		if(typeB.equalsIgnoreCase("textBox") && typeA.equalsIgnoreCase("textBox")){
			((TextBox) entB).add(entA);
			((TextBox) entA).add(entB);
		} if(typeB.equals("foot") && !unstandable.contains(typeA, false) && 
				!fb.getBody().getUserData().equals(typeA) && !(entA instanceof EventTrigger)) {
			if(entA instanceof Ground){
				((Mob) entB).setGround(((Ground) entA).getType());
			}
			((Mob) entB).contacts.add(entA);
		} if(typeA.equals("foot") && !unstandable.contains(typeB, false) && 
				!fa.getBody().getUserData().equals(typeB) && !(entB instanceof EventTrigger)) {
			((Mob) entA).contacts.add(entB);
		} if(typeB.equals("projectile") && !unHittable.contains(typeA, false)) {
			((Projectile) entB).impact(entA);
		} if(typeA.equals("projectile") && !unHittable.contains(typeB, false)) {
			((Projectile) entA).impact(entB);
		} if(typeB.equals("damageField") && !unHittable.contains(typeA, false)) {
			((DamageField) entB).addVictim(entA);
		} if(typeA.equals("damageField") && !unHittable.contains(typeB, false)) {
			((DamageField) entA).addVictim(entB);
		} if(typeA.equals("reaper") && typeB.indexOf("player") != -1){
			((Mob) entB).damage(.1d);
		} if(typeB.equals("reaper") && typeA.indexOf("player") != -1){
			((Mob) entA).damage(.1d);
		} if(typeB.equals("interact") && !fa.isSensor())
			if(entA.isInteractable && !main.analyzing){
				((Mob) entB).setInteractable(entA);
				if(entB.equals(main.character)) 
					new SpeechBubble(entA, entA.getPixelPosition().x + 6, entA.rh +
							entA.getPixelPosition().y, 0, "...", PositionType.LEFT_MARGIN);
			}
		 if(typeA.equals("interact") && !fb.isSensor())
				if(entB.isInteractable && !main.analyzing){
					((Mob) entA).setInteractable(entB);
					if(entA.equals(main.character)) 
						new SpeechBubble(entB, entB.getPixelPosition().x + 6, entB.rh +
								entB.getPixelPosition().y, 0, "...", PositionType.LEFT_MARGIN);
				}
		if(typeA.equals("attack") && !fb.isSensor() && entB.isAttackable){ 
			((Mob) entA).addAttackable(entB);
		}if(typeB.equals("attack") && !fa.isSensor() && entA.isAttackable){ 
			((Mob) entB).addAttackable(entA);
		}if(typeA.equals("vision")){ 
			((Mob) entA).discover(entB);
		}if(typeB.equals("vision")){ 
			((Mob) entB).discover(entA);
		}if(typeA.equals("warp") && typeB.equals("foot")){
			if(((Warp) entA).conditionsMet()) {
				if(((Warp)entA).instant){
					if (entB.equals(main.character))
						main.initWarp((Warp) entA);
					else; //move the Mob to that Level
				} else {
					if (entB.equals(main.character)){
						if(((Warp)entA).getLink()!=null){
							if(((Warp)entA).getLink().owner.outside && ((Warp)entA).owner.outside){
								String message = "To " + ((Warp)entA).getLink().locTitle;
								(new SpeechBubble(entA, entA.getPixelPosition().x, entA.getPixelPosition().y
										+ entA.rh, 6, message, SpeechBubble.PositionType.CENTERED)).expand();
							} else
								new SpeechBubble(entA, entA.getPixelPosition().x, entA.rh +
										entA.getPixelPosition().y, "arrow");
						}
						Mob m = (Mob) entB;
						m.canWarp = true;
						m.setWarp((Warp) entA);
					}
				}
			}
		} if(typeB.equals("warp") && typeA.equals("foot")){
			if(((Warp) entB).conditionsMet()) {
				if(((Warp)entB).instant){
					if (entA.equals(main.character))
						main.initWarp((Warp) entB);
					else; //move the Mob to that Level
				} else {
					if (entA.equals(main.character)){
						if(((Warp)entB).getLink()!=null){
							if(((Warp)entB).getLink().owner.outside && ((Warp)entB).owner.outside){
								String message = "To " + ((Warp)entB).getLink().locTitle;
								(new SpeechBubble(entB, entB.getPixelPosition().x, entB.getPixelPosition().y
										+ entB.rh, 6, message, SpeechBubble.PositionType.CENTERED)).expand();
							} else
								new SpeechBubble(entB, entB.getPixelPosition().x, entB.rh +
										entB.getPixelPosition().y, "arrow");
						}
						Mob m = (Mob) entA;
						m.canWarp = true;
						m.setWarp((Warp) entB);
						if(((Warp)entB).instant && entA.equals(main.character))
							main.initWarp(((Warp)entB));
					}
				}
			}
		} if(typeA.equals("texttrigger") && typeB.equals("foot")){
			if (entB.equals(main.character))
				(new SpeechBubble(entA, entA.getPixelPosition().x, entA.getPixelPosition().y
						+ entA.rh, 6, ((TextTrigger)entA).message, ((TextTrigger)entA).positioning)).expand();
			
		} if(typeB.equals("texttrigger") && typeA.equals("foot")){
			if (entA.equals(main.character))
				(new SpeechBubble(entB, entB.getPixelPosition().x, entB.getPixelPosition().y
						+ entB.rh, 6, ((TextTrigger)entB).message, ((TextTrigger)entB).positioning)).expand();
			
		} if(typeA.equals("refocusTrigger") && entB instanceof Mob && !unstandable.contains(typeB, false)){
			Camera cam = main.getCam();
			if (main.character.equals((Mob) entB))
				if(cam.getTrigger()!=null){
					if (!cam.getTrigger().equals(entA))
						((RefocusTrigger) entA).trigger();
				}else ((RefocusTrigger) entA).trigger();
		} if(typeB.equals("refocusTrigger") && entA instanceof Mob && !unstandable.contains(typeA, false)){
			Camera cam = main.getCam();
			if (main.character.equals((Mob) entA))
				if(cam.getTrigger()!=null){
					if (!cam.getTrigger().equals(entB))
						((RefocusTrigger) entB).trigger();
				}else ((RefocusTrigger) entB).trigger();
		} if(typeA.equals("eventTrigger") && entB instanceof Mob && !unstandable.contains(typeB, false)){
			if (main.character.equals((Mob) entB))
//				if(!((EventTrigger) entA).triggered)
					((EventTrigger) entA).checkEvent();
		} if(typeB.equals("eventTrigger") && entA instanceof Mob && !unstandable.contains(typeA, false)){
			if (main.character.equals((Mob) entA))
//				if(!((EventTrigger) entB).triggered)
					((EventTrigger) entB).checkEvent();
		}

//		System.out.println("begin: " + typeA + " : " + typeB);
	}

	public void endContact(Contact c){
		Fixture fa = c.getFixtureA();
		Fixture fb = c.getFixtureB();

		if (fa == null || fb == null) return;

		Entity entA = (Entity) fa.getBody().getUserData();
		String typeA = (String) fa.getUserData();
		Entity entB = (Entity) fb.getBody().getUserData();
		String typeB = (String) fb.getUserData();

		if(typeB.equalsIgnoreCase("textBox") && typeA.equalsIgnoreCase("textBox")){
			((TextBox) entB).remove(entA);
			((TextBox) entA).remove(entB);
		} if(typeB.equals("foot") && !unstandable.contains(typeA, true) && 
				!fb.getBody().getUserData().equals(typeA)) {
			if(entB.equals(main.character))
				((Mob) entB).contacts.removeValue(entA, false);
		} if(typeA.equals("foot") && !unstandable.contains(typeB, true) && 
				!fa.getBody().getUserData().equals(typeB)) {
			((Mob) entA).contacts.removeValue(entB, false);
		} if(typeB.equals("interact")){
			if(entA == ((Mob) entB).getInteractable()){
				((Mob) entB).setInteractable(null);
			}
		} if(typeA.equals("interact")){
			if(entB == ((Mob) entA).getInteractable()){
				((Mob) entA).setInteractable(null);
			}
		} if(typeA.equalsIgnoreCase("attack")){ 
			((Mob) entA).removeAttackable(entB);
		} if(typeB.equalsIgnoreCase("attack")){ 
			((Mob) entB).removeAttackable(entA);
		} if(typeA.equalsIgnoreCase("vision")){ 
			((Mob) entA).loseSightOf(entB);
		} if(typeB.equalsIgnoreCase("vision")){ 
			((Mob) entB).loseSightOf(entA);
		} if(typeB.equals("damageField") && !unHittable.contains(typeA, false)) {
			((DamageField) entB).removeVictim(entA);
		} if(typeA.equals("damageField") && !unHittable.contains(typeB, false)) {
			((DamageField) entA).removeVictim(entB);
		} if(typeA.equals("warp") && typeB.equals("foot")){
			Mob m = (Mob) entB;
			if(m.getWarp()!=null)
				if(m.getWarp().equals(entA)) {
					m.setWarp(null);
					m.canWarp = false;
				}
			
			if(entB.equals(main.character))
				for(Entity e:main.getObjects())
					if(e instanceof SpeechBubble)
						if(((SpeechBubble)e).getOwner().equals(entA))
							main.removeBody(e.getBody());
		} if(typeB.equals("warp") && typeA.equals("foot")){
			Mob m = (Mob) entA;
			if(m.getWarp()!=null)
				if(m.getWarp().equals(entB)) {
					m.setWarp(null);
					m.canWarp = false;
				}  
			
			if(entA.equals(main.character))
				for(Entity e:main.getObjects())
					if(e instanceof SpeechBubble)
						if(((SpeechBubble)e).getOwner().equals(entB))
							main.removeBody(e.getBody());
		} if(typeA.equals("texttrigger") && typeB.equals("foot") && entB.equals(main.character)){
			for(Entity e:main.getObjects())
				if(e instanceof SpeechBubble)
					if(((SpeechBubble)e).getOwner().equals(entA))
						main.removeBody(e.getBody());
		} if(typeB.equals("texttrigger") && typeA.equals("foot") && entA.equals(main.character)){
			for(Entity e:main.getObjects())
				if(e instanceof SpeechBubble)
					if(((SpeechBubble)e).getOwner().equals(entB))
						main.removeBody(e.getBody());
		}

//		System.out.println("end: " + fbUD + " : " + faUD);
	}

	public void preSolve(Contact c, Manifold m){}
	public void postSolve(Contact c, ContactImpulse ci){}

}














