package main;

import java.util.HashMap;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

import entities.Entity.DamageType;
import entities.Mob;
import handlers.JsonSerializer;

//contains all data related to the player
public class Player implements Serializable {

	public boolean stopPartnerDisabled = false;
	
	private double money, goalMoney;
	private House home;
	private Mob myPartner;
	private Main main;
	private double relationship;
	private double bravery;
	private double nicety;
	private float N, B, L;
	private String info;
	private String partnerTitle;
	private HashMap<DamageType, Integer> typeCounter;
	
	//no-arg constructor used by serializer
	public Player() {
		this.init();
	}
	
	public Player(Main main) {
		this.main = main;
		this.init();
	}
	
	private void init() {
		money = goalMoney = 500.00;
		home = new House();
		
		L = 1; B = 1; N = 1;
		this.info = "";
		this.partnerTitle = "";
		typeCounter = new HashMap<>();			
	}
	
	public void doRandomPower(){
		doRandomPower(main.character.target());
	}
	
	//make the player do some random SUPA power
	//if the player has done this enough times, the most common power will become their permanent power
	//if they don't have one already
	public void doRandomPower(Vector2 target){
		double rnd = Math.random()*(DamageType.values().length-2)+1;
		int type = (int) rnd;
		DamageType dT = DamageType.values()[type];
		
		main.character.setPowerType(dT);
		if(typeCounter.containsKey(dT))
			typeCounter.put(dT, typeCounter.get(dT)+1);
		else
			typeCounter.put(dT, 1);
		
//		System.out.println(typeCounter);
		int max = 0, c;
		for(DamageType d : typeCounter.keySet()){
			c = typeCounter.get(d);
			if(c>=max) {
				max = c; 
			}
		}
		
		main.character.powerAttack(target);

		if(max<3)
			main.character.setPowerType(DamageType.PHYSICAL);
		else
			main.character.setPowerType(dT);
	}
	
	//following methods are getters and setters
	
	public void addPartner(Mob newPartner, String title, String info){
		setPartner(newPartner);
		this.info = info;
		this.partnerTitle = title;
		relationship = 0;
		L = 0;
		
		main.history.setFlag("hasPartner", true);
	}
	
	public void removePartner(){
		if(myPartner==null) return;
		main.character.removeFollower(myPartner);
		myPartner.setState("STATIONARY", null, -1, "NEVER");
		myPartner = null;
		this.info = "";
		this.partnerTitle = "";
		main.history.setFlag("hasPartner", false);
	}
	
	public void setPartner(Mob mob2) { this.myPartner = mob2; }
	public Mob getPartner(){ return myPartner; }
	public void resetRelationship(double d){ relationship = d; }
	public void setRelationship(double amount){ relationship += amount * L; }
	public void setLoveScale(float val){ L = val; }
	public float getLoveScale(){return L;}
	public double getRelationship(){ return relationship; }
	public String getPartnerInfo(){ return info; }
	public void resetPartnerInfo(String info){this.info=info;}
	public String getPartnerTitle(){ return partnerTitle; }
	public void setPartnerTitle(String title){ partnerTitle = title; }
	
	public void resetFollowers(HashMap <Mob, Boolean> followers){
		for(Mob m : followers.keySet())
			main.character.getFollowers().put(m, followers.get(m));
	}
	
	public void addFunds(double amount){ goalMoney += amount; }
	public void updateMoney(){
		double dx = (goalMoney - money)/2;
		if(Math.abs(dx)<.01) {
			money = goalMoney;
			dx = 0;
		}
		money += dx;
	}
	
	public double getMoney(){ return money; }
	public void resetMoney(double money){ this.money = goalMoney = money; }
	
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
	
	public void spawnPartner(Vector2 location){
		myPartner.spawn(location);
	}

	public void setTypeCounter(HashMap<DamageType, Integer> tc){ typeCounter = tc; }

	public void setMainRef(Main main) { this.main = main; }
	
	@Override
	public void read(Json json, JsonValue val) {
		this.money = val.getDouble("money");
		goalMoney = money;
		this.info = val.getString("info");
		this.relationship = val.getDouble("relationship");
		this.bravery = val.getDouble("bravery");
		this.nicety = val.getDouble("nicety");
		this.L = val.getFloat("Llimit");
		this.B = val.getFloat("Blimit");
		this.N = val.getFloat("Nlimit");
		for (JsonValue child = val.get("typeCounter").child(); child != null; child = child.next()) {
			typeCounter.put(DamageType.valueOf(child.name()), child.getInt("value"));
		}
		this.partnerTitle = val.getString("partnerTitle");
		int partnerId = val.getInt("partner");
		if (partnerId > -1) {
			JsonSerializer.pushPlayerRef(this, partnerId);
		}
	}

	@Override
	public void write(Json json) {
		json.writeValue("money", this.money);
		json.writeValue("info", this.info);
		json.writeValue("relationship", this.relationship);
		json.writeValue("bravery", this.bravery);
		json.writeValue("nicety", this.nicety);
		json.writeValue("Llimit", this.L);
		json.writeValue("Blimit", this.B);
		json.writeValue("Nlimit", this.N);
		json.writeValue("typeCounter", this.typeCounter);
		json.writeValue("partnerTitle", this.partnerTitle);
		json.writeValue("partner", (this.myPartner != null) ? this.myPartner.getSceneID() : -1);
		
	}
}
