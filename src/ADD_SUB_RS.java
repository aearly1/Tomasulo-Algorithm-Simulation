import java.util.Hashtable;


public class ADD_SUB_RS {
	Hashtable<String, Object>[] reservationStation; //the actual reservation station
	int no_unused_rs; //this variable is to be used to know if there are any unused reservations stations
	int latency; //the latency of the add and sub instructions
	public ADD_SUB_RS(int no_Of_Stations, int latency)
	{
		//Add/Sub reservation station initialization
		reservationStation = new Hashtable[no_Of_Stations]; //creating X number of reservation stations
		no_unused_rs= no_Of_Stations; //initially all stations are unused
		this.latency=latency;
		
		for (int i = 0; i < reservationStation.length; i++) {
			reservationStation[i] = new Hashtable<String, Object>();
			reservationStation[i].put("RowNr","");// row number in display table
			reservationStation[i].put("Tag", "A"+(i+1));
			reservationStation[i].put("Time", 0);//This field is used to keep track of when the instruction finishes execution
			reservationStation[i].put("Busy", 0); 
			reservationStation[i].put("Op", ""); 
			reservationStation[i].put("Vj", ""); 
			reservationStation[i].put("Vk", ""); 
			reservationStation[i].put("Qj", 0);
			reservationStation[i].put("Qk", 0); 
		}
	}
	
	public boolean freeRS()
	//check if there is a free reservation station
	{
		return no_unused_rs!=0;
	}
	
	public String issueToReservationStation(String OP, Hashtable operand1, Hashtable operand2, String rowNr)
	//method used to place instruction in reservation station
	{
		for (int i = 0; i < reservationStation.length; i++) {
			if(reservationStation[i].get("Busy").equals(0))
			{
				//This reservation station is free so add instruction to it
				
				no_unused_rs--;//decrement number of unsed reservation stations by 1
				
				reservationStation[i].put("Busy", 1); //setting the busy to 1 
				reservationStation[i].put("Op", OP); //setting the operation to ADD or SUB (depending on the instruction)
				
				//setting the values of Qj and Vj
				if(operand1.get("Qi").equals(0))
				{
					reservationStation[i].put("Vj", (Double)operand1.get("Val")); 
					reservationStation[i].put("Qj", operand1.get("Qi"));
				}
				else
				{
					reservationStation[i].put("Vj", ""); 
					reservationStation[i].put("Qj", operand1.get("Qi"));
				}
				
				//setting the values of Qk and Vk
				if(operand2.get("Qi").equals(0))
				{
					reservationStation[i].put("Vk", (Double)operand2.get("Val")); 
					reservationStation[i].put("Qk", operand2.get("Qi"));
				}
				else
				{
					reservationStation[i].put("Vk", ""); 
					reservationStation[i].put("Qk", operand2.get("Qi"));
				}
				
				//set the time field  if the instruction is ready to execute in the next cycle
				if(reservationStation[i].get("Qj").equals(0) && reservationStation[i].get("Qk").equals(0))
				{
					reservationStation[i].put("Time", latency+1);
				}
				
				//add row number
				reservationStation[i].put("RowNr", rowNr);
				
				return (String) reservationStation[i].get("Tag");
			}
		}
		return null;
	}
	
	public void executeInstructions(int cycle)
	{
		for (int i = 0; i < reservationStation.length; i++)
		{
			if(reservationStation[i].get("Busy").equals(1) && !reservationStation[i].get("Time").equals(0))
			{
				if(reservationStation[i].get("Time").equals(latency))
				{
					Processor.DisplayTable.get(Integer.parseInt((String) reservationStation[i].get("RowNr"))).put("Start of Execution", cycle+"");//update start of execution field in table
				}
				//this reservation station contains an instruction in execution since busy=1 and the time is not equal to zero meaning it has not finished execution
				
				reservationStation[i].put("Time",(Integer)reservationStation[i].get("Time")-1);//it will execute in this cycle meaning the number of cycles left to finish will decrease by one
				
				if(reservationStation[i].get("Time").equals(0))
				{
					Processor.DisplayTable.get(Integer.parseInt((String) reservationStation[i].get("RowNr"))).put("End of Execution", cycle+"");//update end of execution field in table
				}
			}
		}
	}
	
	public void recieveTagFromBus(String tag, double val)
	//method used to update the Qj and Qk when an instruction writes back
	{
		for (int i = 0; i < reservationStation.length; i++) {
			if(reservationStation[i].get("Qj").equals(tag))
			{
				reservationStation[i].put("Vj", val);
				reservationStation[i].put("Qj", 0);
				if(reservationStation[i].get("Qj").equals(0) && reservationStation[i].get("Qk").equals(0))//check if both operands are now ready and if they are set the time field of station
				{
					reservationStation[i].put("Time", latency);
				}
			}
			if(reservationStation[i].get("Qk").equals(tag))
			{
				reservationStation[i].put("Vk", val); 
				reservationStation[i].put("Qk", 0);
				if(reservationStation[i].get("Qj").equals(0) && reservationStation[i].get("Qk").equals(0))//check if both operands are now ready and if they are set the time field of station
				{
					reservationStation[i].put("Time", latency);
				}
			}
		}
	}
	
	public Hashtable getFinishedInstruction(int cycle)
	{
		for (int i = 0; i < reservationStation.length; i++)
		{
			if(reservationStation[i].get("Busy").equals(1) && reservationStation[i].get("Time").equals(0) && reservationStation[i].get("Qj").equals(0) && reservationStation[i].get("Qk").equals(0) && !Processor.DisplayTable.get(Integer.parseInt((String) reservationStation[i].get("RowNr"))).get("End of Execution").equals(cycle+""))
			{
				//this instruction has finished and would like to write back so return its station tag and the value
				Hashtable tagAndVal= new Hashtable<String, String>();
				
				tagAndVal.put("Tag", reservationStation[i].get("Tag"));
				
				double result=0;
				if(reservationStation[i].get("Op").equals("ADD"))
					result=(Double) reservationStation[i].get("Vj")+(Double) reservationStation[i].get("Vk");
				else if(reservationStation[i].get("Op").equals("ADD"))
					result=(Double)reservationStation[i].get("Vj")-(Double)reservationStation[i].get("Vk");

				tagAndVal.put("Val", result);
				tagAndVal.put("RowNr", reservationStation[i].get("RowNr"));
				
				return tagAndVal;
			}
		}
		return null;
	}
	
	public void eraseInstruction(String tag)
	//method used to erase instruction from reservation station
	{
		int i= Integer.parseInt(tag.substring(1))-1;
		reservationStation[i] = new Hashtable<String, Object>();
		reservationStation[i].put("Tag", "A"+(i+1));
		reservationStation[i].put("Time", 0);//This field is used to keep track of when the instruction finishes execution
		reservationStation[i].put("Busy", 0); 
		reservationStation[i].put("Op", ""); 
		reservationStation[i].put("Vj", ""); 
		reservationStation[i].put("Vk", ""); 
		reservationStation[i].put("Qj", 0);
		reservationStation[i].put("Qk", 0); 
		no_unused_rs++;
	}
	
	public boolean isEmpty()
	{
		return no_unused_rs==reservationStation.length;
	}
	
	public String toString()
	//display reservation station content
	{
		String output="Addition/Subtraction Reservation Station: " + '\n';
		for(Hashtable<String, Object> ht: reservationStation)
		{
			output+=  "Time=" + ht.get("Time") + ", "  + "Tag=" + ht.get("Tag") + ", " +"Busy=" + ht.get("Busy") + ", " + "Op=" + ht.get("Op") + ", " + "Vj=" + ht.get("Vj") + ", " + "Vk=" + ht.get("Vk") + ", " + "Qj=" + ht.get("Qj") + ", " + "Qk=" + ht.get("Qk") + ", " + '\n';
		}
		return  output;
	}
}
