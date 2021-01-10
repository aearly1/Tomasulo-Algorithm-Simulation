import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Queue;


public class Processor {
	static ArrayList<Hashtable<String, String>> DisplayTable = new ArrayList<Hashtable<String,String>>(); 
	Queue<Hashtable<String, String>> instructionQueue; 
	RegFile floatingPointRegFile;
	ADD_SUB_RS a_s_reservationStation;
	/*
	 * You guys need to add the load buffer, store buffer, mul/div reservation stations
	 */
	int cycle;
	
	public Processor()
	{
		instructionQueue = new LinkedList<Hashtable<String,String>>();
		floatingPointRegFile = new RegFile();
		a_s_reservationStation = new ADD_SUB_RS(3, 2);
		/*
		 * You guys need to initialiaze the load buffer, store buffer, mul/div reservation stations
		 */
	}
	
	public void issue()
	//method responsible for issue logic of pipeline
	{
		if(!instructionQueue.isEmpty())
		{
			if(a_s_reservationStation.freeRS() && (instructionQueue.peek().get("OP").equals("ADD") || instructionQueue.peek().get("OP").equals("SUB")))//instruction is an add or sub and there is space in the reservation station so no need to stall queue
			{
				Hashtable<String, String> instruction = instructionQueue.remove(); //dequeue instruction
				Hashtable<String, Object> operand1= floatingPointRegFile.readFromRegFile(Integer.parseInt(instruction.get("RS").substring(1))); //get operand 1 of instruction from register file
				Hashtable<String, Object> operand2= floatingPointRegFile.readFromRegFile(Integer.parseInt(instruction.get("RT").substring(1))); //get operand 2 of instruction from register file
				
				String tag = a_s_reservationStation.issueToReservationStation(instruction.get("OP"), operand1, operand2, instruction.get("Order")); //place instruction in add/sub reservation station
				
				floatingPointRegFile.setTag(tag, Integer.parseInt(instruction.get("RD").substring(1)));//update the Q0 tag in the reg file
				Processor.DisplayTable.get(Integer.parseInt(instruction.get("Order"))).put("Issue", cycle+"");//update issue field in table
			}
			/*
			 * You guys need to add your issue logic
			 */
		}
	}
	
	public void execute()
	//method responsible for execute logic of pipeline
	{
		a_s_reservationStation.executeInstructions(cycle);
		/*
		 * You guys need to add your execute logic
		 */
	}
	
	public void writeResult()
	//method responsible for writeResult logic of pipeline
	{
		boolean anInstructionAlreadyWroteBack = false; //ONLY WRITE ONE INSTRUCTION CAN WRITE BACK IN A CYCLE
		Hashtable finshedInstruction=a_s_reservationStation.getFinishedInstruction(cycle); //check if add/sub reservation station has any instruction that need to write back
		if(!anInstructionAlreadyWroteBack && finshedInstruction!=null)
		{
			//one of the add and sub instructions needs to write back and the buffer is not being used to write back
			
			anInstructionAlreadyWroteBack = true;//now no other instruction will be able to write since this instruction is writing
			
			a_s_reservationStation.recieveTagFromBus((String)finshedInstruction.get("Tag"),(Double)finshedInstruction.get("Val"));//send the tag and the written valueof the finished instruction to the add/sub reservation station so that instructions waiting for the value can use it
			/*
			 * do the same for the other three reservation stations when Basant, Ashraf and Moataz do their parts
			 */
			floatingPointRegFile.WriteToRegFile((String)finshedInstruction.get("Tag"), (Double)finshedInstruction.get("Val")); //send the tag and the written value of the finished instruction to the regFile
			a_s_reservationStation.eraseInstruction((String) finshedInstruction.get("Tag")); //erase instruction from reservation station
			Processor.DisplayTable.get(Integer.parseInt((String) finshedInstruction.get("RowNr"))).put("Write Result", cycle+"");//update write result field in table
		}
		/*
		 * You guys need to add your write result logic
		 */
	}
	
	public static void DisplayTable()
	//method used to display the display table
	{
		System.out.println("Display Table:");
		for(Hashtable<String, String> ht: Processor.DisplayTable)
		{
			System.out.println("Instruction=" + ht.get("Instruction") + ", "  + "Write Register=" + ht.get("Write Register") + ", " +"j=" + ht.get("j") + ", " + "k=" + ht.get("k") + ", " + "Issue=" + ht.get("Issue") + ", " + "Start of Execution=" + ht.get("Start of Execution") + ", " + "End of Execution=" + ht.get("End of Execution") + ", " + "Write Result=" + ht.get("Write Result"));
		}
	}
	public static void main(String[] args) 
	{
		Processor p = new Processor();
		
		//CREATING THE INSTRUCTIONS
		Hashtable<String, String> instruction1 = new Hashtable();
		instruction1.put("Order", "0");//This tells you in in which row of the display table will u find this instruction
		instruction1.put("OP", "SUB");
		instruction1.put("RD", "F0");
		instruction1.put("RS", "F0");
		instruction1.put("RT", "F0");
		Hashtable<String, String> instruction2 = new Hashtable();
		instruction2.put("Order", "1"); //This tells you in in which row of the display table will u find this instruction
		instruction2.put("OP", "ADD");
		instruction2.put("RD", "F0");
		instruction2.put("RS", "F1");
		instruction2.put("RT", "F5");
		Hashtable<String, String> instruction3 = new Hashtable();
		instruction3.put("Order", "2"); //This tells you in in which row of the display table will u find this instruction
		instruction3.put("OP", "ADD");
		instruction3.put("RD", "F3");
		instruction3.put("RS", "F0");
		instruction3.put("RT", "F7");
		Hashtable<String, String> instruction4 = new Hashtable();
		instruction4.put("Order", "3"); //This tells you in in which row of the display table will u find this instruction
		instruction4.put("OP", "ADD");
		instruction4.put("RD", "F1");
		instruction4.put("RS", "F3");
		instruction4.put("RT", "F0");
		Hashtable<String, String> instruction5 = new Hashtable();
		instruction5.put("Order", "4"); //This tells you in in which row of the display table will u find this instruction
		instruction5.put("OP", "SUB");
		instruction5.put("RD", "F0");
		instruction5.put("RS", "F6");
		instruction5.put("RT", "F6");
		Hashtable<String, String> instruction6 = new Hashtable();
		instruction6.put("Order", "5"); //This tells you in in which row of the display table will u find this instruction
		instruction6.put("OP", "ADD");
		instruction6.put("RD", "F12");
		instruction6.put("RS", "F12");
		instruction6.put("RT", "F13");
		
		//ADDING INSTRUCTIONS TO INSTRUCTION QUEUE
		p.instructionQueue.add(instruction1);
		p.instructionQueue.add(instruction2);
		p.instructionQueue.add(instruction3);
		p.instructionQueue.add(instruction4);
		p.instructionQueue.add(instruction5);
		p.instructionQueue.add(instruction6);
		
		//ADDING INSTRUCTIONS TO DISPLAY TABLE
		//add instr1
		Hashtable<String, String> displayTableRow = new Hashtable();
		displayTableRow.put("Instruction", instruction1.get("OP"));
		displayTableRow.put("Write Register", instruction1.get("RD"));
		displayTableRow.put("j", instruction1.get("RS"));
		displayTableRow.put("k", instruction1.get("RT"));
		displayTableRow.put("Issue", "");
		displayTableRow.put("Start of Execution", "");
		displayTableRow.put("End of Execution", "");
		displayTableRow.put("Write Result", "");
		Processor.DisplayTable.add(displayTableRow);
		//add instr2
		displayTableRow = new Hashtable();
		displayTableRow.put("Instruction", instruction2.get("OP"));
		displayTableRow.put("Write Register", instruction2.get("RD"));
		displayTableRow.put("j", instruction2.get("RS"));
		displayTableRow.put("k", instruction2.get("RT"));
		displayTableRow.put("Issue", "");
		displayTableRow.put("Start of Execution", "");
		displayTableRow.put("End of Execution", "");
		displayTableRow.put("Write Result", "");
		Processor.DisplayTable.add(displayTableRow);
		//add instr3
		displayTableRow = new Hashtable();
		displayTableRow.put("Instruction", instruction3.get("OP"));
		displayTableRow.put("Write Register", instruction3.get("RD"));
		displayTableRow.put("j", instruction3.get("RS"));
		displayTableRow.put("k", instruction3.get("RT"));
		displayTableRow.put("Issue", "");
		displayTableRow.put("Start of Execution", "");
		displayTableRow.put("End of Execution", "");
		displayTableRow.put("Write Result", "");
		Processor.DisplayTable.add(displayTableRow);
		//add instr4
		displayTableRow = new Hashtable();
		displayTableRow.put("Instruction", instruction4.get("OP"));
		displayTableRow.put("Write Register", instruction4.get("RD"));
		displayTableRow.put("j", instruction4.get("RS"));
		displayTableRow.put("k", instruction4.get("RT"));
		displayTableRow.put("Issue", "");
		displayTableRow.put("Start of Execution", "");
		displayTableRow.put("End of Execution", "");
		displayTableRow.put("Write Result", "");
		Processor.DisplayTable.add(displayTableRow);
		//add instr5
		displayTableRow = new Hashtable();
		displayTableRow.put("Instruction", instruction5.get("OP"));
		displayTableRow.put("Write Register", instruction5.get("RD"));
		displayTableRow.put("j", instruction5.get("RS"));
		displayTableRow.put("k", instruction5.get("RT"));
		displayTableRow.put("Issue", "");
		displayTableRow.put("Start of Execution", "");
		displayTableRow.put("End of Execution", "");
		displayTableRow.put("Write Result", "");
		Processor.DisplayTable.add(displayTableRow);
		//add instr6
		displayTableRow = new Hashtable();
		displayTableRow.put("Instruction", instruction6.get("OP"));
		displayTableRow.put("Write Register", instruction6.get("RD"));
		displayTableRow.put("j", instruction6.get("RS"));
		displayTableRow.put("k", instruction6.get("RT"));
		displayTableRow.put("Issue", "");
		displayTableRow.put("Start of Execution", "");
		displayTableRow.put("End of Execution", "");
		displayTableRow.put("Write Result", "");
		Processor.DisplayTable.add(displayTableRow);
		
		//CYCLE 0 (INITIAL STATE BEFORE RUNNING THE ALGORITHM)
		System.out.println("Cycle: " + p.cycle);
		System.out.println();
		DisplayTable();
		System.out.println();
		System.out.println(p.floatingPointRegFile);
		System.out.println(p.a_s_reservationStation);
		System.out.println("---");
		p.cycle++;
		
		while(!p.instructionQueue.isEmpty() || !p.a_s_reservationStation.isEmpty())//keep executing until everything is empty baisically
		{
			//executing the 3 stages of the pipeline
			p.issue();
			p.execute();
			p.writeResult();
			
			//PRINTING CONTENTS OF ALL TABLES DURING A CYCLE
			System.out.println("Cycle: " + p.cycle);
			System.out.println();
			DisplayTable();
			System.out.println();
			System.out.println(p.floatingPointRegFile);
			System.out.println(p.a_s_reservationStation);
			System.out.println("---");
			
			p.cycle++;//incrementing the cycle
		}
	}
}
