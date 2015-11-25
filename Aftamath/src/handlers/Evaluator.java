package handlers;

import main.Main;
import scenes.Script;

import com.badlogic.gdx.utils.Array;

import entities.Entity;
import entities.Mob;

public class Evaluator {
	
	private Main main;
	private Script script;

	public Evaluator(Main main) {
		 this.main = main;
	}
	
	public boolean evaluate(String statement){
		return evaluate(statement, null);
	}
	
	public boolean evaluate(String statement, Script script){
		if(statement==null) return true;
		if(statement.isEmpty()) return true;
		
		String obj, property = null;
		boolean not=false, result=false;
		this.script = script;
		
		if(statement.contains(">") || statement.contains("<") || statement.contains("=")){
			String value = "", val = "";
			
			//separate value and condition from tmp
			obj = "";
			String condition = "";
			String tmp = Vars.remove(statement, " "), index;
			int first=-1;
			for(int i = 0; i < tmp.length() - 1; i++){
				index = tmp.substring(i,i+1);
				if((index.equals(">") || index.equals("<") || index.equals("="))
						&& condition.length()<2){
					if(first==-1){
						condition += index;
						first = i;
					} else if (i-first==1)
						condition+=index;
				}
			}

			if(tmp.indexOf(condition)<1){
				System.out.print("No object found to compare with in statement: "+statement);
				if(script!=null)
					System.out.println("; Line: "+(script.index+1)+"\tScript: "+script.ID);
				else
				
				return false;
			}

			obj = tmp.substring(0, tmp.indexOf(condition));
			val = tmp.substring(tmp.indexOf(condition)+condition.length());
			
			if(obj.contains("+")||obj.contains("-")||obj.contains("*")||obj.contains("/"))
				property = evaluateExpression(obj);
			else
				property = determineValue(obj, false);

			if(val.contains("+")||val.contains("-")||val.contains("*")||val.contains("/"))
				value = evaluateExpression(val);
			else
				value = determineValue(val, false);

			System.out.println(statement);
			System.out.println("p: " + property + "\tc: " + condition + "\tv: " + value);

			//actual comparator
			try{
				switch (condition){
				case "=":
					if (Vars.isNumeric(property) && Vars.isNumeric(value))
						result =(Double.parseDouble(property) == Double.parseDouble(value));
					else
						result = property.equals(value);
					break;
				case ">":
					if (Vars.isNumeric(property) && Vars.isNumeric(value))
						result = (Double.parseDouble(property) > Double.parseDouble(value));
					break;
				case ">=":
				case "=>":
					if (Vars.isNumeric(property) && Vars.isNumeric(value))
						result = (Double.parseDouble(property) > Double.parseDouble(value));
					break;
				case "<":
					if (Vars.isNumeric(property) && Vars.isNumeric(value))
						result = (Double.parseDouble(property) < Double.parseDouble(value));
					break;
				case "<=":
				case "=<":
					if (Vars.isNumeric(property) && Vars.isNumeric(value))
						result = (Double.parseDouble(property) <= Double.parseDouble(value));
					break;
				default:
					System.out.println("\""+condition+"\" is not a vaild operator; Line: "+(script.index+1)+"\tScript: "+script.ID);
				}

//				System.out.println("result: "+result);
				if(not) return !result;
				else return result;
			} catch(Exception e){
				System.out.println("Could not compare \""+property+"\" with \""+value+"\" by condition \""+condition+"\"; Line: "+(script.index+1)+"\tScript: "+script.ID);
				e.printStackTrace();
				return false;
			}
		} else {
			//remove brackets
			if(!statement.isEmpty())
				if(statement.contains("[") && statement.contains("]"))
					statement = statement.substring(statement.indexOf("[")+1, statement.indexOf("]"));
			if(statement.startsWith("!")){
				not = true;
				statement = statement.substring(1);
			}

			if(main.history.getFlag(statement)!=null){
				result = main.history.getFlag(statement);
			} else if(main.history.findEvent(statement)){
				result = true;
			} 
		}

		if(not) return !result;
		return result;
	}
	
	private String evaluateExpression(String obj){
		Array<String> arguments;
		String result, res, val;
		String tmp=Vars.remove(obj," ");
		tmp=tmp.replace("-", "&");

		for(int i = 0; i<tmp.length()-1; i++){
			if(tmp.substring(i, i+1).equals("(")){
				if(tmp.lastIndexOf(")")==-1){
					System.out.println("Error evaluating: \"" +tmp +
							"\"\nMissing a \")\"; Line: "+(script.index+1)+"\tScript: "+script.ID);
					return null;
				}
				String e =evaluateExpression(tmp.substring(i+1, tmp.lastIndexOf(")")));
				tmp = tmp.substring(0,i)+e
						+tmp.substring(tmp.lastIndexOf(")")+1);
				i=tmp.lastIndexOf(")");
			}
		}

		//sort expressions
		//not programmed, sorry

		//evaluate
		//separates all arguments and contstants from operators
		arguments = new Array<>(tmp.split("(?<=[&+*/])|(?=[&+*/])"));
		if(arguments.size<3||arguments.contains("", false)) return obj;

		result = arguments.get(0);
		if(!Vars.isNumeric(result)){
			res = determineValue(result, false);
			if (res != null)
				result = new String(res);
		}

		//continuously evaluate operations until there aren't enough
		//left to perform a single operation
		while(arguments.size>=3){
			val = arguments.get(2);
			switch(arguments.get(1)){
			case"+":
				if(Vars.isNumeric(result)&& Vars.isNumeric(val)){
					result = String.valueOf(Float.parseFloat(result)+Float.parseFloat(val));
				}else{
					res = determineValue(result, false);
					if (res != null)
						result = new String(res);

					res = determineValue(val, false);
					if (res != null)
						val = new String(res);

					if(Vars.isNumeric(result)&& Vars.isNumeric(val))
						result = String.valueOf(Float.parseFloat(result)+Float.parseFloat(val));
					else result += val;
				}
				break;
			case"&":
				if(Vars.isNumeric(result)&& Vars.isNumeric(val)){
					result = String.valueOf(Float.parseFloat(result)-Float.parseFloat(val));
				}else{
					res = determineValue(result, false);
					if (res != null)
						result = new String(res);

					res = determineValue(val, false);
					if (res != null)
						val = new String(res);

					if(Vars.isNumeric(result)&& Vars.isNumeric(val))
						result = String.valueOf(Float.parseFloat(result)-Float.parseFloat(val));
					else System.out.println("Conversion error: res: \""+result+"\" op: - val: \""+val);
				}break;
			case"*":
				if(Vars.isNumeric(result)&& Vars.isNumeric(val)){
					result = String.valueOf(Float.parseFloat(result)*Float.parseFloat(val));
				}else{
					res = determineValue(result, false);
					if (res != null)
						result = new String(res);

					res = determineValue(val, false);
					if (res != null)
						val = new String(res);

					if(Vars.isNumeric(result)&& Vars.isNumeric(val))
						result = String.valueOf(Float.parseFloat(result)*Float.parseFloat(val));
					else System.out.println("Conversion error: res: \""+result+"\" op: - val: \""+val);
				}break;
			case"/":
				if(Vars.isNumeric(result)&& Vars.isNumeric(val)){
					result = String.valueOf(Float.parseFloat(result)/Float.parseFloat(val));
				}else{
					res = determineValue(result, false);
					if (res != null)
						result = new String(res);

					res = determineValue(val, false);
					if (res != null)
						val = new String(res);

					if(Vars.isNumeric(result)&& Vars.isNumeric(val))
						result = String.valueOf(Float.parseFloat(result)/Float.parseFloat(val));
					else System.out.println("Conversion error: res: \""+result+"\" op: - val: \""+val);
				}break;
			}

			arguments.removeIndex(1);
			arguments.removeIndex(1);
		}

		return result;
	}

	//determine wether argument is and object property, variable, flag, or event
	//by default, if no object can be found it is automatically assumed to be an event
	//should possibly change in the future;
	private String determineValue(String obj, boolean boolPossible){
		String prop = "", not = "", property = null;
		Object object=null;
		
		//ensure that invalid characters do not change the outcome
		obj = (obj.replace("[", "")).replace("]", "");

		if(Vars.isNumeric(obj))
			property = obj;
		//the object is a string and must be parsed out
		else if(obj.contains("{") && obj.contains("}"))
			return getSubstitutions(obj.substring(obj.indexOf("{")+1, obj.lastIndexOf("{")));
		//the value is an object's property
		else if(obj.contains(".")){
			prop = obj.substring(obj.indexOf(".")+1);
			obj = obj.substring(0, obj.indexOf("."));
			if(obj.startsWith("!")){
				obj = obj.substring(1);
				not = "!";
			}

			object = findObject(obj);
			if (object==null){
				System.out.println("Could not find object with name \"" +obj+"\"; Line: "+(script.index+1)+"\tScript: "+script.ID);
				return null;
			}

			property = findProperty(prop, object);

			if(property == null){
				System.out.println("\""+prop+"\" is an invalid object property for object \""+ obj+"\"; Line: "+(script.index+1)+"\tScript: "+script.ID);
				return null;
			}

			property = not + property;
		} else {
			//find variable
			if(script!=null)
				object = script.getVariable(obj);
			
			if (object==null)
				object = main.history.getVariable(obj);
			
			if (object!=null){
				if(object instanceof String)
					property = (String) object;
				else if(object instanceof Float)
					property = String.valueOf((Float) object);
				else if(object instanceof Integer)
					property = String.valueOf((Integer) object);
				else
					property = null;
			} else if (boolPossible){ 
				if(obj.startsWith("!")){
					not = "!";
					obj = obj.substring(1);
				}

				//find flag or event
				if(main.history.getFlag(obj)!=null){
					property = not+String.valueOf(main.history.getFlag(obj));
				} else 
					property = not + main.history.findEvent(obj);
			} else
				System.out.println("Could not determine value for \""+obj+"\"; Line: "+(script.index+1)+"\tScript: "+script.ID);
		}

		return property;
	}
	
	private Entity findObject(String objectName){
		Entity object = null;

		if(Vars.isNumeric(objectName)){
			for(Entity d : main.getObjects())
				if (d.getSceneID() == Integer.parseInt(objectName)) return d;
		} else switch(objectName) {
		case "player":
			object = main.character;
			break;
		case "partner":
			if(main.player.getPartner().getName() != null)
				object = main.player.getPartner();
			break;
		case "narrator":
			object = main.narrator;
			break;
		case "this":
			if(script!=null)
				object = script.getOwner();
			break;
		default:
			for(Entity d : main.getObjects()){
				if(d instanceof Mob){
					if (((Mob)d).getName().toLowerCase().equals(objectName.toLowerCase()))
						return d;
				} else
					if (d.ID.toLowerCase().equals(objectName.toLowerCase()))
						return d;
			}
		}
		return object;
	}
	
	private String findProperty(String prop, Object object){
		String property = null;
		switch(prop.toLowerCase()){
		case "name":
			if(object instanceof Mob)
				property = ((Mob)object).getName();
			break;
		case "health":
			if(object instanceof Mob) 
				property = String.valueOf(((Mob)object).getHealth());
			break;
		case "money":
			property = String.valueOf(Double.valueOf(main.player.getMoney()).longValue());
			break;
		case "gender":
			if(object instanceof Mob) 
				property = ((Mob)object).getGender();
			break;
		case "love":
		case "relationship":
			property = String.valueOf(main.player.getRelationship());
			break;
		case "niceness": 
			property = String.valueOf(main.player.getNiceness());
			break;
		case "bravery": 
			property = String.valueOf(main.player.getBravery());
			break; 
		case "lovescale":
			property = String.valueOf(main.player.getLoveScale());
			break;
		case "nicenessscale": 
			property = String.valueOf(main.player.getNicenessScale());
			break;
		case "braveryscale":
			property = String.valueOf(main.player.getBraveryScale());
			break;
		case "house": 
			property = (main.player.getHome().getType());
			break;
			//		case "haspartner":
			//			if(object instanceof Player)
//				if (player.getPartner()!=null){
//					if(player.getPartner().getName()!=null)
//						if(!player.getPartner().getName().equals(""))
//							property = String.valueOf(true);
//				} else
//					property = String.valueOf(false);
//			break;
		case "location":
			if(object instanceof Entity)
				property = String.valueOf(((Entity)object).getPosition().x);
			break;
		case "power":
		case "level":
			if(object instanceof Mob)
				property = String.valueOf(((Mob)object).getLevel());
			break;
		case "powertype":
			if(object instanceof Mob)
				property = String.valueOf(((Mob)object).getPowerType());
			break;
		}
		return property;
	}
	
	//	string substitutions
	private String getSubstitutions(String txt){
		while(txt.contains("/")){
			if(txt.contains("/player")){
				txt = txt.substring(0, txt.indexOf("/player")) + main.character.getName() + 
						txt.substring(txt.indexOf("/player") + "/player".length());
			}if(txt.contains("/playerg")){
				String g = "guy";
				if(main.character.getGender().equals("female")) g="girl";
				txt = txt.substring(0, txt.indexOf("/playerg")) + g + 
						txt.substring(txt.indexOf("/playerg") + "/playerg".length());
			}if(txt.contains("/playergps")){
				String g = "his";
				if(main.character.getGender().equals("female")) g="her";
				txt = txt.substring(0, txt.indexOf("/playergps")) +g + 
						txt.substring(txt.indexOf("/playergps") + "/playergps".length());
			}if(txt.contains("/playergp")){
				String g = "his";
				if(main.character.getGender().equals("female")) g="hers";
				txt = txt.substring(0, txt.indexOf("/playergp")) + g + 
						txt.substring(txt.indexOf("/playergp") + "/playergp".length());
			}if(txt.contains("/playergo")){
				String g = "he";
				if(main.character.getGender().equals("female")) g="she";
				txt = txt.substring(0, txt.indexOf("/playergo")) + g + 
						txt.substring(txt.indexOf("/playergo") + "/playergo".length());
			} if(txt.contains("/partner")){
				String s = "";
				if(main.player.getPartner()==null)
					s=main.player.getPartner().getName();
				txt = txt.substring(0, txt.indexOf("/partner")) + s + 
						txt.substring(txt.indexOf("/partner") + "/partner".length());
			}if(txt.contains("/partnerg")){
				String g = "";
				if(main.player.getPartner()!=null){
					if(main.player.getPartner().getGender().equals("female")) g="girl";
					else g = "guy"; 
				}
				txt = txt.substring(0, txt.indexOf("/partnerg")) + g + 
						txt.substring(txt.indexOf("/partnerg") + "/partnerg".length());
			}if(txt.contains("/partnergps")){
				String g = "";
				if(main.player.getPartner()!=null){
					if(main.player.getPartner().getGender().equals("female")) g="her";
					else g = "his"; 
				}
				txt = txt.substring(0, txt.indexOf("/partnergps")) + g + 
						txt.substring(txt.indexOf("/partnergps") + "/partnergps".length());
			}if(txt.contains("/partnergp")){
				String g = "";
				if(main.player.getPartner()!=null){
					if(main.player.getPartner().getGender().equals("female")) g="hers";
					else g = "his"; 
				}
				txt = txt.substring(0, txt.indexOf("/partnergp")) + g + 
						txt.substring(txt.indexOf("/partnergp") + "/partnergp".length());
			}if(txt.contains("/partnergo")){
				String g = "";
				if(main.player.getPartner()!=null){
					if(main.player.getPartner().getGender().equals("female")) g="she";
					else g = "he"; 
				}
				txt = txt.substring(0, txt.indexOf("/partnergo")) + g + 
						txt.substring(txt.indexOf("/partnergo") + "/partnergo".length());
			} if (txt.contains("/partnert")) {
				txt = txt.substring(0, txt.indexOf("/partnert")) + main.player.getPartnerTitle() + 
						txt.substring(txt.indexOf("/partnergt") + "/partnergt".length());
			} if(txt.contains("/house")){
				txt = txt.substring(0, txt.indexOf("/house")) + main.player.getHome().getType() + 
						txt.substring(txt.indexOf("/house") + "/house".length());
			} if(txt.contains("/address")){
				txt = txt.substring(0, txt.indexOf("/address")) + main.player.getHome().getType() + 
						txt.substring(txt.indexOf("/address") + "/address".length());
			} if(txt.contains("/variable[")&& txt.indexOf("]")>=0){
				String varName = txt.substring(txt.indexOf("/variable[")+"/variable[".length(), txt.indexOf("]"));
				Object var = main.history.getVariable(varName);
				if(var!= null) {
					txt = txt.substring(0, txt.indexOf("/variable[")) + var +
							txt.substring(txt.indexOf("/variable[")+"/variable[".length()+ varName.length() + 1);
				}
			}
		}
		
		return txt;
	}

}
