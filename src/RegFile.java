import java.util.Arrays;
import java.util.Hashtable;

public class RegFile {
	Hashtable<String, Object>[] registerFile; //the actual register file
	
	public RegFile()
	{
		//REGISTER FILE INITIALIZATION
		registerFile = new Hashtable[32]; //create 32 floating point register
		for (int i = 0; i < registerFile.length; i++) {
			registerFile[i] = new Hashtable<String, Object>();
			registerFile[i].put("Register Name", "F"+i); //reg name
			registerFile[i].put("Qi", 0); //reservation tag (initially zero since no instruction is writing anything)
			registerFile[i].put("Val", (double)i); //value inside register (dummy value initially)
		}
	}
	
	public Hashtable<String, Object> readFromRegFile(int regNr)
	//method to be used when issuing the instruction to fetch the value or tag for the reservation station
	//I am returning the entire hash table bcs you might needd the value or tag depending on whether Qi is equal to zero or some reservation station tag
	{
		try
		{
			return registerFile[regNr];
		}
		catch(Exception e)
		{
			System.out.println("Register does not exist!!");
			return null;
		}
	}
	
	public void WriteToRegFile(String tag, double val)
	//method to be used in write back stage to write value in register file
	{
		for(Hashtable<String, Object> ht: registerFile)
		{
			if(ht.get("Qi").equals(tag))
			{
				//found the correct register 
				ht.put("Qi", 0); //erase tag
				ht.put("Val", val); //write vale in register
				break;
			}
		}
	}
	
	public void setTag(String tag, int regNr)
	//method to be used in issue stage to mark which register it will be writing to in the write back stage

	{
		try
		{
			registerFile[regNr].put("Qi", tag);
		}
		catch(Exception e)
		{
			System.out.println("Register does not exist!!");
		}
	}
	
	public String toString()
	//display register file content
	{
		String output="Register File: " + '\n';
		for(Hashtable<String, Object> ht: registerFile)
		{
			output+=  "Register Name=" + ht.get("Register Name") + ", "  + "Qi=" + ht.get("Qi") + ", " +"Val=" + ht.get("Val") + '\n';
		}
		return  output;
	}
	
	//ALI TESTING REGISTER FILE
	public static void main(String[] args) {
		RegFile reg = new RegFile();
		reg.setTag("A1", 1);
		//System.out.println(reg.readFromRegFile(32));
		System.out.println(reg.readFromRegFile(0));
		System.out.println(reg.readFromRegFile(1));
		reg.WriteToRegFile("A1", 1000);
		reg.WriteToRegFile("A2", 1000);
		//reg.setTag("A1", 32);
		System.out.println(reg);
	}
}
