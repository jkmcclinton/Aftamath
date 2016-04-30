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
	
	//can be pretty taxing to compute
	//computes boolean algebra from left to right
	public boolean evaluate(String origStatement, Script script){
		if(origStatement==null) return true;
		if(origStatement.isEmpty()) return true;
		this.script = script;
		String statement = origStatement;
		
//		System.out.println(statement);
			
		//split by and/or operators
		Array<String> arguments = new Array<>();
		while(statement.contains(" and ") || statement.contains(" or ")){
			int and = statement.indexOf(" and ");
			int or = statement.indexOf(" or ");
			int first = statement.indexOf("(");
			int last = statement.indexOf(")");
			boolean split = false;
			
			if(first>last || (first==-1 && last!=-1) || (first!=-1 && last==-1)){
				System.out.println("Mismatch parenthesis; \""+origStatement+"\"");
				if(this.script!=null) System.out.println("Line: "+(script.index+1)+"\tScript: "+script.ID);
				return false;
			}
			
			if(and < or){
				if(and > -1 && !(and>first && and<last)){
					arguments.add(statement.substring(0, statement.indexOf(" and ")));
					statement = statement.substring(and+ " and ".length());
					arguments.add("and");
					split = true;
				} 
				or = statement.indexOf(" or ");
				if(or > -1 && !(or>first && or<last)){
					arguments.add(statement.substring(0, statement.indexOf(" or ")));
					statement = statement.substring(or+ " or ".length());
					arguments.add("or");
					split = true;
				}
			} else {
				if(or > -1 && !(or>first && or<last)){
					arguments.add(statement.substring(0, statement.indexOf(" or ")));
					statement = statement.substring(or+ " or ".length());
					arguments.add("or");
					split = true;
				} 
				and = statement.indexOf(" and ");
				if(and > -1 && !(and>first && and<last)){
					arguments.add(statement.substring(0, statement.indexOf(" and ")));
					statement = statement.substring(and+ " and ".length());
					arguments.add("and");
					split = true;
				}
			}
			if(!split) break;
		}
		arguments.add(statement);
		
		if(arguments.size%2!=1){
			System.out.println("Invalid number of arguments; "+origStatement);
			if(this.script!=null) 
				System.out.println("Line: "+(script.index+1)+"\tScript: "+script.ID);
		}
//		System.out.println("eval args: "+arguments);
		String s; boolean not, result;
		//evaluate individual arguments
		for(int i = 0; i<arguments.size; i+=2){
			not = false;
			s = arguments.get(i).trim();
			if(s.startsWith("!")){
				not = true;
				s = s.substring(1);
			}
			
			//evaluate comparisons
			if(s.contains("[")){ // contains expressions of format [expression1 comparison expression2]
				if(s.lastIndexOf("]")==-1){
					System.out.print("Mismatch braces; "+origStatement);
					if(this.script!=null) System.out.println("; Line: "+(script.index+1)+"\tScript: "+script.ID);
					else System.out.println();
					
					return false;
				}
				result = evaluateComparison(s.substring(s.indexOf("[")+1, 
						s.lastIndexOf("]")));
			//evaluate parentheticals by calling this method
			} else if(s.contains("(")) { // contains format (boolean1 operator boolean2)
				result = evaluate(s.substring(s.indexOf("(")+1, s.lastIndexOf(")")), script);
			//evaluate boolean variables
			} else{
				String val = null;
				if(script!=null){
					val = String.valueOf(script.getVariable(s));
					if(val.equals("null")) val = null;
				}
				
				// determine if value is a flag or event
				if(val==null||val.isEmpty())
					if(main.history.getFlag(s)!=null)
						val = String.valueOf(main.history.getFlag(s));
					else if(main.history.findEvent(s))
						val = "true";
				
				if(val!=null)
					result = Boolean.parseBoolean(val);
				else
					result = false;
			}
			
			if(not) result = !result;
			arguments.set(i, String.valueOf(result));
		}
//		System.out.println(arguments);
		
		//combine by operators from left to right
		//[true, and, false] >> [false]
		while(arguments.size>=3){
			if(arguments.get(1).equals("or")){
				arguments.set(0, String.valueOf(Boolean.parseBoolean(arguments.get(0)) ||
						Boolean.parseBoolean(arguments.get(2))));
			} else if(arguments.get(1).equals("and")){
				arguments.set(0, String.valueOf(Boolean.parseBoolean(arguments.get(0)) &&
						Boolean.parseBoolean(arguments.get(2))));
			} else {
				System.out.println("\""+arguments.get(0)+"\" is not a known logical operator");
			}

			arguments.removeIndex(1);
			arguments.removeIndex(1);
		}
//		System.out.println(arguments);
		return Boolean.parseBoolean(arguments.get(0));
	}
	
	//determines the boolean result of a comparison between expressions,
	//properties, and variables
	private boolean evaluateComparison(String statement){
		String obj, property = null;
		boolean result=false;

		if(statement.contains(">") || statement.contains("<") || statement.contains("=")){
			String value = "", val = "";

			//separate value and condition from tmp
			obj = "";
			String condition = "";
			String tmp = Vars.remove(statement, " "), index;
			int first=-1;
			for(int i = 0; i < tmp.length() - 1; i++){
				index = tmp.substring(i,i+1);
				if((index.equals(">") || index.equals("<") || index.equals("=") || 
						(index.equals("!") && tmp.contains("!="))) && condition.length()<2){
					if(first==-1){
						condition += index;
						first = i;
					} else if (i-first==1)
						condition+=index;
				}
			}

			if(tmp.indexOf(condition)<1){
				System.out.println("No object found to compare with in statement: "+statement);
				if(script!=null)
					System.out.println("Line: "+(script.index+1)+"\tScript: "+script.ID);
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

//			System.out.println(statement);
//			System.out.println("p: " + property + "\tc: " + condition + "\tv: " + value);

			//actual comparator
			try{
				switch (condition){
				case "=":
					if (Vars.isNumeric(property) && Vars.isNumeric(value))
						result =(Double.parseDouble(property) == Double.parseDouble(value));
					else
						result = property.equals(value);
					break;
				case "!=":
					if (Vars.isNumeric(property) && Vars.isNumeric(value))
						result =(Double.parseDouble(property) != Double.parseDouble(value));
					else
						result = !property.equals(value);
					break;
				case ">":
					if (Vars.isNumeric(property) && Vars.isNumeric(value))
						result = (Double.parseDouble(property) > Double.parseDouble(value));
					break;
				case ">=":
				case "=>":
					if (Vars.isNumeric(property) && Vars.isNumeric(value))
						result = (Double.parseDouble(property) >= Double.parseDouble(value));
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
					System.out.print("\""+condition+"\" is not a vaild operator");
					if(script!=null)System.out.println("; Line: "+(script.index+1)+"\tScript: "+script.ID);
					else System.out.println();
				}

//				System.out.println("result: "+result);
				return result;
			} catch(Exception e){
				System.out.print("Could not compare \""+property+"\" with \""+value+"\" by condition \""+condition+"\"");
				if(this.script!=null) System.out.println("; Line: "+(script.index+1)+"\tScript: "+script.ID);
				else System.out.println();
//				e.printStackTrace();
				return false;
			}
		} else {
			System.out.print("No mathematical expression found in statement \"" + statement + "\"");
			if(this.script!=null) System.out.println("; Line: "+(script.index+1)+"\tScript: "+script.ID);
			else System.out.println();
			return false;
		}
	}
	
	public String evaluateExpression(String obj, Script script){
		this.script = script;
		return evaluateExpression(obj);
	}
	
	//returns the solution to a set of mathematical operators
	//TODO does not handle parsing of negative numbers!
	private String evaluateExpression(String obj){
		Array<String> arguments;
		String result, res, val;
		String tmp=Vars.remove(obj," ");
		tmp=tmp.replace("-", "&");

		for(int i = 0; i<tmp.length()-1; i++){
			if(tmp.substring(i, i+1).equals("(")){
				if(tmp.lastIndexOf(")")==-1){
					System.out.print("Error evaluating: \"" +tmp +"\"\nMissing a \")\"");
					if(this.script!=null) System.out.println("; Line: "+(script.index+1)+"\tScript: "+script.ID);
					else System.out.println();
					return null;
				}
				String e =evaluateExpression(tmp.substring(i+1, tmp.lastIndexOf(")")));
				tmp = tmp.substring(0,i)+e
						+tmp.substring(tmp.lastIndexOf(")")+1);
				i=tmp.lastIndexOf(")");
			}
		}

		//TODO sort expressions
		//not programmed, sorry

		//evaluate
		//separates all arguments and contstants from operators
		arguments = new Array<>(tmp.split("(?<=[&+*/])|(?=[&+*/])"));
		if(arguments.size<3||arguments.contains("", false)) return obj;
//		System.out.println(arguments);

		result = arguments.get(0);
		if(!Vars.isNumeric(result)){
			res = determineValue(result, false);
			if (res != null)
				result = new String(res);
		}

		//continuously evaluate operations until there aren't enough
		//left to perform a single operation
		//[1, +, 2] >> [3]
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
//			System.out.println("removing: "+arguments);
		}

		return result;
	}
	
	public String determineValue(String obj, Script script){
		this.script = script;
		return determineValue(obj, false);
	}

	//determine whether argument is and object property, variable, flag, or event
	//by default, if no object can be found it is automatically assumed to be an event
	//should possibly change in the future;
	private String determineValue(String obj, boolean boolPossible){
		String prop = "", not = "", property = null;
		Object object=null;
		String orig = obj;
		try{
		//ensure that invalid characters do not change the outcome
		obj = (obj.replace("[", "")).replace("]", "");

		if(Vars.isNumeric(obj))
			property = obj;
		//the object is a string and must be isolated
		else if(obj.contains("{") && obj.contains("}"))
			return getSubstitutions(obj.substring(obj.indexOf("{")+1, obj.lastIndexOf("}")));
		//random between [0,10]
		else if(obj.toLowerCase().equals("random")) 
			return String.valueOf(Math.random()*10);
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
				System.out.print("Could not find object with name \"" +obj+"\"");
				if(this.script!=null) System.out.println("; Line: "+(script.index+1)+"\tScript: "+script.ID);
				else System.out.println();
				return null;
			}

			property = findProperty(prop, object);

			if(property == null){
				System.out.print("\""+prop+"\" is an invalid object property for object \""+ obj+"\"");
				if(this.script!=null) System.out.println("; Line: "+(script.index+1)+"\tScript: "+script.ID);
				else System.out.println();
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
			} else {
				System.out.print("Could not determine value for \""+obj+"\"");
				if(script!=null) System.out.println("; Line: "+(script.index+1)+"\tScript: "+script.ID);
				else System.out.println();
			}
				
		}

		return property;
		} catch(Exception e){
			e.printStackTrace();
			System.out.println("FATAL error trying to determine value for \"" +orig+"\"");
			return null;
		}
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
			if(main.player.getPartner()!=null)
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
		case "title":
			if(object instanceof Mob)
				if(object.equals(main.player.getPartner()))
					property = main.player.getPartnerTitle();
			break;
		}
		return property;
	}
	
	public String getSubstitutions(String txt, Script scr){
		this.script = scr;
		return getSubstitutions(txt);
	}
	
//	string substitutions
	private String getSubstitutions(String txt){
		while(txt.contains("/playergpp")){
			String g = "him";
			if(main.character.getGender().equals("female")) g="her";
			txt = txt.substring(0, txt.indexOf("/playergpp")) +g + 
					txt.substring(txt.indexOf("/playergpp") + "/playergpp".length());
		} while(txt.contains("/playergps")){
			String g = "his";
			if(main.character.getGender().equals("female")) g="her";
			txt = txt.substring(0, txt.indexOf("/playergps")) +g + 
					txt.substring(txt.indexOf("/playergps") + "/playergps".length());
		} while(txt.contains("/playergp")){
			String g = "his";
			if(main.character.getGender().equals("female")) g="hers";
			txt = txt.substring(0, txt.indexOf("/playergp")) + g + 
					txt.substring(txt.indexOf("/playergp") + "/playergp".length());
		} while(txt.contains("/playergo")){
			String g = "he";
			if(main.character.getGender().equals("female")) g="she";
			txt = txt.substring(0, txt.indexOf("/playergo")) + g + 
					txt.substring(txt.indexOf("/playergo") + "/playergo".length());
		} while(txt.contains("/playerg")){
			String g = "guy";
			if(main.character.getGender().equals("female")) g="girl";
			txt = txt.substring(0, txt.indexOf("/playerg")) + g + 
					txt.substring(txt.indexOf("/playerg") + "/playerg".length());
		} while(txt.contains("/player")){
			txt = txt.substring(0, txt.indexOf("/player")) + main.character.getName() + 
					txt.substring(txt.indexOf("/player") + "/player".length());
		} while(txt.contains("/partnergps")){
			String g = "";
			if(main.player.getPartner()!=null){
				if(main.player.getPartner().getGender().equals("female")) g="her";
				else g = "his"; 
			}
			txt = txt.substring(0, txt.indexOf("/partnergps")) + g + 
					txt.substring(txt.indexOf("/partnergps") + "/partnergps".length());
		} while(txt.contains("/partnergps")){
			String g = "";
			if(main.player.getPartner()!=null){
				if(main.player.getPartner().getGender().equals("female")) g="her";
				else g = "his"; 
			}
			txt = txt.substring(0, txt.indexOf("/partnergps")) + g + 
					txt.substring(txt.indexOf("/partnergps") + "/partnergps".length());
		}while(txt.contains("/partnergpp")){
			String g = "";
			if(main.player.getPartner()!=null){
				if(main.player.getPartner().getGender().equals("female")) g="her";
				else g = "him"; 
			}
			txt = txt.substring(0, txt.indexOf("/partnergpp")) + g + 
					txt.substring(txt.indexOf("/partnergpp") + "/partnergpp".length());
		} while(txt.contains("/partnergo")){
			String g = "";
			if(main.player.getPartner()!=null){
				if(main.player.getPartner().getGender().equals("female")) g="she";
				else g = "he"; 
			}
			txt = txt.substring(0, txt.indexOf("/partnergo")) + g + 
					txt.substring(txt.indexOf("/partnergo") + "/partnergo".length());
		} while(txt.contains("/partnerg")){
			String g = "";
			if(main.player.getPartner()!=null){
				if(main.player.getPartner().getGender().equals("female")) g="girl";
				else g = "guy"; 
			}
			txt = txt.substring(0, txt.indexOf("/partnerg")) + g + 
					txt.substring(txt.indexOf("/partnerg") + "/partnerg".length());
		} while (txt.contains("/partnert")) {
			txt = txt.substring(0, txt.indexOf("/partnert")) + main.player.getPartnerTitle() + 
					txt.substring(txt.indexOf("/partnert") + "/partnert".length());
		} while(txt.contains("/partner")){
			String s = "";
			if(main.player.getPartner()==null)
				s=main.player.getPartner().getName();
			txt = txt.substring(0, txt.indexOf("/partner")) + s + 
					txt.substring(txt.indexOf("/partner") + "/partner".length());
		} while(txt.contains("/house")){
			txt = txt.substring(0, txt.indexOf("/house")) + main.player.getHome().getType() + 
					txt.substring(txt.indexOf("/house") + "/house".length());
		} while(txt.contains("/address")){
			txt = txt.substring(0, txt.indexOf("/address")) + main.player.getHome().getType() + 
					txt.substring(txt.indexOf("/address") + "/address".length());
		} while(txt.contains("/variable[")&& txt.indexOf("]")>=0){
			String varName = txt.substring(txt.indexOf("/variable[")+"/variable[".length(), txt.indexOf("]"));
			Object var = null;
			if(script!=null)
				var =script.getVariable(varName);
			if (var==null) var = main.history.getVariable(varName);
			if (var==null && main.history.flagList.containsKey(varName))
				var = main.history.getFlag(varName);
			if(var!= null) {
				txt = txt.substring(0, txt.indexOf("/variable[")) + var +
						txt.substring(txt.indexOf("/variable[")+"/variable[".length()+ varName.length() + 1);
			} else{
				System.out.print("No variable with name \""+ varName +"\" found");
				if(script!=null)
					System.out.println("; Line: "+(script.index+1)+"\tScript: "+script.ID);
				else 
					System.out.println();
				txt = txt.substring(0, txt.indexOf("/variable[")) +
						txt.substring(txt.indexOf("/variable[")+"/variable[".length()+ varName.length() + 1);
			}
		} while(txt.contains("/var[")&& txt.indexOf("]")>=0){
			String varName = txt.substring(txt.indexOf("/var[")+"/var[".length(), txt.indexOf("]"));
			Object var = null;
			if(script!=null)
				var = script.getVariable(varName);
			if (var==null) var = main.history.getVariable(varName);
			if (var==null && main.history.flagList.containsKey(varName))
				var = main.history.getFlag(varName);
			if(var!= null) {
				txt = txt.substring(0, txt.indexOf("/var[")) + var +
						txt.substring(txt.indexOf("/var[")+"/var[".length()+ varName.length() + 1);
			} else {
				System.out.print("No variable with name \""+ varName +"\" found");
				if(script!=null)
					System.out.println("; Line: "+(script.index+1)+"\tScript: "+script.ID);
				else 
					System.out.println();

				txt = txt.substring(0, txt.indexOf("/var[")) +
						txt.substring(txt.indexOf("/var[")+"/var[".length()+ varName.length() + 1);
			}

		} if(txt.contains("/cc")){
			String[] words = txt.split(" ");
			txt="";
			for(int i=0; i<words.length;i++){
				if(words[i].contains("/cc")){
					words[i] = words[i].toUpperCase();
					//remove delimiter
					words[i] = words[i].substring(0, words[i].indexOf("/CC")) + "" + 
							words[i].substring(txt.indexOf("/CC") + "/CC".length());
				}
				txt += words[i];
			}
		} if(txt.contains("/c")){
			String[] words = txt.split(" ");
			txt="";
			for(int i=0; i<words.length;i++){
				if(words[i].contains("/c")){
					//capitalize first letter
					words[i] = words[i].substring(0, 1).toUpperCase() + words[i].substring(1);
					//remove delimiter
					words[i] = words[i].substring(0, words[i].indexOf("/c")) + "" + 
							words[i].substring(txt.indexOf("/c") + "/c".length());
				}
				txt += words[i];
			}
		}
		return txt;
	}
}
