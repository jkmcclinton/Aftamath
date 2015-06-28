package entities;

import handlers.Vars;

import java.util.HashMap;

import main.House;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class Player extends Mob {

	public boolean stopPartnerDisabled = false;
	
	private double money, goalMoney;
	private House home;
	private NPC myPartner;
	//private String nickName;
	
	private double relationship;
	private double bravery;
	private double nicety;
	private float N, B, L;
	private String info;
	private HashMap<DamageType, Integer> typeCounter;
	
	public Player(String name, String gender, String newID) {
		super(name, gender + newID, 0, 0, Vars.BIT_PLAYER_LAYER);
		this.name = name;
		this.gender = gender;
		
		money = goalMoney = 1500.00;
		home = new House();
		
		L = 1; B = 1; N = 1;
		typeCounter = new HashMap<>();
	}

	public void update(float dt) {
		super.update(dt);
		updateMoney();
	}
	
	public void doRandomPower(Vector2 target){
		int type = (int) Math.random()*(DamageType.values().length-1);
		DamageType dT = DamageType.values()[type];
		
		powerType = dT;
		typeCounter.put(dT, typeCounter.get(dT)+1);
		
		int max = 0, c;
		for(DamageType d : typeCounter.keySet()){
			c = typeCounter.get(d);
			if(c>=max) {
				max = c; 
			}
		}
		
		powerAttack(target);

		if(max<3)
			powerType = DamageType.PHYSICAL;
		else
			powerType = dT;
	}
	
	public void follow(Entity focus) {
//		this.focus = focus;
//		controlled = true;
//		controlledAction = Action.WALKING;
		
	}

	public void stay() {
		controlled = false;
		controlledAction = null;
	}
	//following methods are getters and setters
	
	public void goOut(NPC newPartner, String info){
		myPartner = newPartner;
		this.info = info;
		relationship = 0;
		L = 0;
		
		main.history.setFlag("hasPatner", true);
	}
	
	public void breakUp(){
		myPartner = new NPC();
		main.history.setFlag("hasPatner", false);
//		relationship = 0;
//		L = 0;
	}
	
	public NPC getPartner(){ return myPartner; }
	public void resetRelationship(double d){ relationship = d; }
	public void setRelationship(double amount){ relationship += amount * L; }
	public void setLoveScale(float val){ L = val; }
	public float getLoveScale(){return L;}
	public double getRelationship(){ return relationship; }
	public String getPartnerInfo(){ return info; }
	public void resetPartnerInfo(String info){this.info=info;}
	
	public void resetFollowers(Array <Mob> followers){
		for(Mob m : followers)
			this.followers.add(m);
	}
	
	public void addFunds(double amount){ goalMoney += amount; }
	private void updateMoney(){
		double dx = (goalMoney - money)/2;
//		if(dx < 1d && dx != 0) dx = 1 * (dx/Math.abs(dx));
		
		money += dx;
	}
	
	public double getMoney(){ return money; }
	public void resetMoney(double money){ this.money = money; }
	
	public void evict(){ home = new House(); }
	public void moveHome(House newHouse){ home = newHouse; }
	public House getHome(){  return home; }
	
	public void resetNiceness(double d){ nicety = d; }
	public void setNiceness(double d){ nicety += d * N; }
	public void setNicenessScale(float val){ N = val; }
	public double getNiceness() { return nicety; }
	public float getNicenessScale() { return N; }

	public void resetBravery(double d){ nicety = d; }
	public void setBravery(double d){ bravery += d * B; }
	public void setBraveryScale(float val){ B = val; }
	public double getBravery() { return bravery; }
	public float getBraveryScale() { return B; }
	
	public Player copy(){
		Player p = new Player(name, gender, ID);
		
		//copy stats
		p.resetMoney(money);
//		p.setResistance(resistance);
		p.resetHealth(health, maxHealth);
		p.resetBravery(bravery);
		p.setBraveryScale(B);
		p.resetNiceness(nicety);
		p.setNicenessScale(N);
		p.goOut(myPartner.copy(), info);
		p.resetPartnerInfo(info);
		p.resetRelationship(relationship);
		p.setLoveScale(L);
		p.resetFollowers(followers);
		p.moveHome(p.getHome());
		p.setTypeCounter(typeCounter);
		
		p.setPowerType(powerType);
		p.resetLevel(level);
		p.setGameState(main);
		
		return p;
	}
	
	public void spawnPartner(Vector2 location){
		myPartner.spawn(location);
	}

	public void setGender(String gender) { this.gender = gender;}
	public void setTypeCounter(HashMap<DamageType, Integer> tc){ typeCounter = tc; }
}
