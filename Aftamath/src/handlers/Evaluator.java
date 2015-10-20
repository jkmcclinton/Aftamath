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
		String obj, property = null;
		boolean not=false, result=false;
		this.script = script;
		
		if(statement==null)
			return true;
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


//			System.out.println("p: " + property + "\nc: " + condition + "\nv: " + value);

			//actual comparator
			try{
				switch (condition){
				case "=":
					if (Vars.isNumeric(property) && Vars.isNumeric(value))
						result =( Float.parseFloat(property) == Float.parseFloat(value));
					else
						result = property.equals(value);
					break;
				case ">":
					if (Vars.isNumeric(property) && Vars.isNumeric(value))
						result = (Float.parseFloat(property) > Float.parseFloat(value));
					break;
				case ">=":
				case "=>":
					if (Vars.isNumeric(property) && Vars.isNumeric(value))
						result = (Float.parseFloat(property) >= Float.parseFloat(value));
					break;
				case "<":
					if (Vars.isNumeric(property) && Vars.isNumeric(value))
						result = (Float.parseFloat(property) < Float.parseFloat(value));
					break;
				case "<=":
				case "=<":
					if (Vars.isNumeric(property) && Vars.isNumeric(value))
						result = (Float.parseFloat(property) <= Float.parseFloat(value));
					break;
				default:
					System.out.println("\""+condition+"\" is not a vaild operator; Line: "+(script.index+1)+"\tScript: "+script.ID);
				}

				if(not) return !result;
				else return result;
			} catch(Exception e){
				System.out.println("Could not compare \""+property+"\" with \""+value+"\" by condition \""+condition+"\"; Line: "+(script.index+1)+"\tScript: "+script.ID);
				e.printStackTrace();
				return false;
			}
		} else {
			//remove brackes
			if(!statement.isEmpty())
				statement = statement.substring(1, statement.length()-1);
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

		//evaluate
		arguments = new Array<>(tmp.split("(?<=[&+*/])|(?=[&+*/])"));
		if(arguments.size<3||arguments.contains("", false)) return obj;

		result = arguments.get(0);
		if(!Vars.isNumeric(result)){
			res = determineValue(result, false);
			if (res != null)
				result = new String(res);
		}

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

		if(Vars.isNumeric(obj))
			property = obj;
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
			property = String.valueOf(main.player.getMoney());
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

}
