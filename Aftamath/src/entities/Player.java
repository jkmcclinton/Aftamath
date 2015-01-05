package entities;

import states.Play;
import main.House;
import handlers.SuperMob;
import handlers.Vars;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

public class Player extends SuperMob {

	private double money, goalMoney;
	private House home;
	private Partner myPartner;
	//private String nickName;
	private String gender;
	
	private double relationship;
	private double bravery;
	private double nicety;
	private float N, B, H, L;
	
	private Array<Mob> followers;
	
	public Player(String name, String gender, String newID, int x, int y) {
		super(name, gender + newID, 0, 0, x, y, DEFAULT_WIDTH, DEFAULT_HEIGHT, Vars.BIT_LAYER2);
		this.name = name;
		this.gender = gender;
		
		money = goalMoney = 1500.00;
		myPartner = new Partner("Kira", "female", 1, x - width * 4, y + 30);
		myPartner.setScript("script2");
//		myPartner = new Partner();
		home = new House();
		followers = new Array<>();
		
		H = 1; L = 1; B = 1; N = 1;
	}
	
	//following methods are getters and setters
	
	public void update(float dt) {
		super.update(dt);
		updateMoney();
	}
	
	public void heal(double healVal){
		health += healVal * H;
		if (health > MAX_HEALTH) health = MAX_HEALTH;
	}
	
	public void damage(double damageVal){
		if (!dead) {
			if (!invulnerable) health -= H * damageVal;
			TextureRegion[] sprites = TextureRegion.split(texture, width*2, height*2)[FLINCHING];
			animation.setAction(sprites, actionLengths[FLINCHING], direction, FLINCHING);
		}
		if (health <= 0 && !dead) die();
	}
	
	public String getGender(){
		return gender;
	}
	
	public void goOut(Partner newPartner, String info){
		myPartner = newPartner;
		myPartner.setInfo(info);
		relationship = 0;
	}
	
	public void breakUp(){
		myPartner = new Partner();
		relationship = 0;
	}
	
	public Partner getPartner(){ return myPartner; }
	
	public void increaseRelationship(double amount){ relationship += amount * L; }
	public void decreaseRelationship(double amount){ relationship -= amount * L; }
	public void setLoveScale(float val){ L = val; }
	public double getRelationship(){ return relationship; }
	
	public void addFollower(Mob m){ if (!followers.contains(m, true)) followers.add(m); }
	public void removeFollower(Mob m){ followers.removeValue(m, true); }
	public Array<Mob> getFollowers(){ return followers; }
	public int getFollowerIndex(Mob m) {
		Play.debugText = "" + followers; 
		return followers.indexOf(m, true) + 1; 
	}
	
	public void subtractMoney(double amount){ goalMoney -= amount; } 
	public void addMoney(double amount){ goalMoney += amount; }
	
	private void updateMoney(){
		double dx = (goalMoney - money)/2;
//		if(dx < 1d && dx != 0) dx = 1 * (dx/Math.abs(dx));
		
		money += dx;
	}
	
	public double getMoney(){ return money; }
	
	public void evict(){ home = new House(); }
	public void moveHome(House newHouse){ home = newHouse; }
	public House getHome(){  return home; }
	
	public void setNiceness(double d){ nicety += d * N; }
	public void setNicenessScale(float val){ N = val; }
	public double getNiceness() { return nicety; }
	
	public void setBravery(double d){ bravery += d * B; }
	public void setBraveryScale(float val){ B = val; }
	public double getBravery() { return bravery; }
	
	public void setHealthScale(float val){ H = val; }
}
