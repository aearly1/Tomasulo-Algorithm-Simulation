import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Queue;


public class Processor {
	static ArrayList<Hashtable<String, String>> DisplayTable = new ArrayList<Hashtable<String,String>>();
	Queue<Hashtable<String, String>> instructionQueue; 
	RegFile floatingPointRegFile;
	IntegerRegFile intRegFile;
	ADD_SUB_RS a_s_reservationStation;
	LOAD_BUFFER l_buffer;
	STORE_BUFFER s_buffer;
	DIV_MUL_RS d_m_reservationStation;
	/*
	 * You guys need to add the load buffer, store buffer 
	 */

	int cycle;
	public Processor()
	{
		instructionQueue = new LinkedList<Hashtable<String,String>>();
		floatingPointRegFile = new RegFile();
		intRegFile = new IntegerRegFile();
		a_s_reservationStation = new ADD_SUB_RS(3, 2);
		l_buffer = new LOAD_BUFFER(3, 1);
		s_buffer = new STORE_BUFFER(2,2);
		d_m_reservationStation=new DIV_MUL_RS(4,3,2);
		/*
		 * You guys need to initialiaze the load buffer, store buffer
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
			//instruction is a load and there is space in the reservation station so no need to stall queue
			else if(l_buffer.freeRS() && instructionQueue.peek().get("OP").equals("Load"))
			{
				//dequeue instruction
				Hashtable<String, String> instruction = instructionQueue.remove();

				int base = intRegFile.readFromRegFile(Integer.parseInt(instruction.get("RS").substring(1)));

				//get the target register name
				int targetReg= Integer.parseInt(instruction.get("RT").substring(1));

				//get instruction's immediate value
				int immediate = Integer.parseInt(instruction.get("Immediate"));

				//calculate the effective address
				int address = immediate + base;

				//place instruction in load buffer
				String tag = l_buffer.issueToReservationStation(address, instruction.get("Order"));

				floatingPointRegFile.setTag(tag, targetReg);//update the Q0 tag in the reg file
				Processor.DisplayTable.get(Integer.parseInt(instruction.get("Order"))).put("Issue", cycle+"");//update issue field in table
			}

			//instruction is a load and there is space in the reservation station so no need to stall queue
			else if(s_buffer.freeRS() && instructionQueue.peek().get("OP").equals("Store"))
			{
				//dequeue instruction
				Hashtable<String, String> instruction = instructionQueue.remove();

				int base = intRegFile.readFromRegFile(Integer.parseInt(instruction.get("RS").substring(1)));

				//get the source register value
				Hashtable<String, Object> operand =
						floatingPointRegFile.readFromRegFile(Integer.parseInt(instruction.get("RT").substring(1)));

				//get instruction's immediate value
				int immediate = Integer.parseInt(instruction.get("Immediate"));

				//calculate the effective address
				int address = immediate + base;

				//place instruction in load buffer
				String tag = s_buffer.issueToReservationStation(operand, address, instruction.get("Order"));

				Processor.DisplayTable.get(Integer.parseInt(instruction.get("Order"))).put("Issue", cycle+"");//update issue field in table
			}
			else if(this.d_m_reservationStation.isfree() && (instructionQueue.peek().get("OP").equals("DIV") || instructionQueue.peek().get("OP").equals("MUL")))
			{
				Hashtable<String, String> instruction = instructionQueue.remove(); //dequeue instruction
				Hashtable<String, Object> operand1= floatingPointRegFile.readFromRegFile(Integer.parseInt(instruction.get("RS").substring(1))); 
				Hashtable<String, Object> operand2= floatingPointRegFile.readFromRegFile(Integer.parseInt(instruction.get("RT").substring(1))); 
				
				String tag = this.d_m_reservationStation.issue(instruction.get("OP"), operand1, operand2, instruction.get("Order")); 
				floatingPointRegFile.setTag(tag, Integer.parseInt(instruction.get("RD").substring(1)));
				Processor.DisplayTable.get(Integer.parseInt(instruction.get("Order"))).put("Issue", cycle+"");	
			}
		}
	}
	
	public void reserveMemory(){

		//the instructions with the earliest issue time from both load and store buffers
		Object[] load = l_buffer.getCandidate();
		Object[] store = s_buffer.getCandidate();

		//their issue time
		int loadMin = (int) load[0];
		int storeMin = (int) store[0];

		Hashtable<String, Object> loadRS = (Hashtable<String, Object>) load[1];
		Hashtable<String, Object> storeRS = (Hashtable<String, Object>) store[1];

		//the instruction that will lock the mutex
		Hashtable<String, Object> newOwner = null;

		if (loadMin == -1 && storeMin != -1){
			newOwner = storeRS;
		}
		else if (loadMin != -1 && storeMin == -1){
			newOwner = loadRS;
		}
		else if (loadMin != -1 && storeMin != -1){
			if(loadMin <= storeMin){
				newOwner = loadRS;
			}
			else {
				newOwner = storeRS;
			}
		}

		//give the memory to the chosen instruction
		if (newOwner != null)
			DataMemory.reserve((String)  newOwner.get("Tag"));
	}
	
	public void execute()
	//method responsible for execute logic of pipeline
	{
		a_s_reservationStation.executeInstructions(cycle);
		d_m_reservationStation.execute(cycle);

		reserveMemory();
		l_buffer.executeInstructions(cycle);
		s_buffer.executeInstructions(cycle);

	}
	
	public void writeResult()
	//method responsible for writeResult logic of pipeline
	{
		//check if add/sub reservation station has any instruction that need to write back
		Hashtable finshedInstructionAddSub = a_s_reservationStation.getFinishedInstruction(cycle);
		Hashtable finshedD_M = d_m_reservationStation.getResults(cycle); 

		Hashtable finshedInstructionLoad = l_buffer.getFinishedInstruction(cycle);
		Hashtable finishedInstructionStore = s_buffer.getFinishedInstruction(cycle);
		//TODO: other instructions

		if(finshedInstructionAddSub!=null)
		{
			//one of the add and sub instructions needs to write back and the buffer is not being used to write back

			a_s_reservationStation.recieveTagFromBus((String)finshedInstructionAddSub.get("Tag"),(Double)finshedInstructionAddSub.get("Val"));//send the tag and the written valueof the finished instruction to the add/sub reservation station so that instructions waiting for the value can use it
			s_buffer.recieveTagFromBus((String)finshedInstructionAddSub.get("Tag"),(Double)finshedInstructionAddSub.get("Val"));
			d_m_reservationStation.recieveTag((String)finshedInstructionAddSub.get("Tag"),(Double)finshedInstructionAddSub.get("Val"));

			floatingPointRegFile.WriteToRegFile((String)finshedInstructionAddSub.get("Tag"), (Double)finshedInstructionAddSub.get("Val")); //send the tag and the written value of the finished instruction to the regFile
			a_s_reservationStation.eraseInstruction((String) finshedInstructionAddSub.get("Tag")); //erase instruction from reservation station
			Processor.DisplayTable.get(Integer.parseInt((String) finshedInstructionAddSub.get("RowNr"))).put("Write Result", cycle+"");//update write result field in table
		}

		//check if load reservation station has any instruction that need to write back
		else if(finshedInstructionLoad!=null)
		{
			//one of the load instructions needs to write back and the buffer is not being used to write back
			String tag = (String) finshedInstructionLoad.get("Tag");

			a_s_reservationStation.recieveTagFromBus(tag, (Double)finshedInstructionLoad.get("Val"));//send the tag and the written valueof the finished instruction to the add/sub reservation station so that instructions waiting for the value can use it
			s_buffer.recieveTagFromBus(tag, (Double)finshedInstructionLoad.get("Val"));
			d_m_reservationStation.recieveTag(tag,(Double)finshedInstructionLoad.get("Val"));

			floatingPointRegFile.WriteToRegFile(tag, (Double)finshedInstructionLoad.get("Val")); //send the tag and the written value of the finished instruction to the regFile
			l_buffer.eraseInstruction(tag); //erase instruction from reservation station
			Processor.DisplayTable.get(Integer.parseInt((String) finshedInstructionLoad.get("RowNr"))).put("Write Result", cycle+"");//update write result field in table
		}

		//check if load reservation station has any instruction that need to write back
		else if(finishedInstructionStore!=null)
		{
			//one of the store has finished
			String tag = (String) finishedInstructionStore.get("Tag");

			s_buffer.eraseInstruction(tag); //erase instruction from reservation station
			Processor.DisplayTable.get(Integer.parseInt((String) finishedInstructionStore.get("RowNr"))).put("Write Result", cycle+"");//update write result field in table
		}

		else if(finshedD_M!=null)
		{
						
			a_s_reservationStation.recieveTagFromBus((String)finshedD_M.get("Tag"),(Double)finshedD_M.get("Val"));
			d_m_reservationStation.recieveTag((String)finshedD_M.get("Tag"),(Double)finshedD_M.get("Val"));
			s_buffer.recieveTagFromBus((String)finshedD_M.get("Tag"), (Double)finshedD_M.get("Val"));

			floatingPointRegFile.WriteToRegFile((String)finshedD_M.get("Tag"), (Double)finshedD_M.get("Val")); //send the tag and the written value of the finished instruction to the regFile
			d_m_reservationStation.eraseInstruction((String) finshedD_M.get("Tag")); //erase instruction from reservation station
			Processor.DisplayTable.get(Integer.parseInt((String) finshedD_M.get("RowNr"))).put("Write Result", cycle+"");//update write result field in table
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
			if(ht.get("Instruction").equals("Load"))
				System.out.println("Instruction=" + ht.get("Instruction") + ", "  + "Write Register=" + ht.get("Write Register") + ", " +"Base Register=" + ht.get("Base Register") + ", " + "Immediate=" + ht.get("Immediate") + ", " + "Issue=" + ht.get("Issue") + ", " + "Start of Execution=" + ht.get("Start of Execution") + ", " + "End of Execution=" + ht.get("End of Execution") + ", " + "Write Result=" + ht.get("Write Result"));
			else if(ht.get("Instruction").equals("Store"))
				System.out.println("Instruction=" + ht.get("Instruction") + ", "  + "Source Register=" + ht.get("Source Register") + ", " +"Base Register=" + ht.get("Base Register") + ", " + "Immediate=" + ht.get("Immediate") + ", " + "Issue=" + ht.get("Issue") + ", " + "Start of Execution=" + ht.get("Start of Execution") + ", " + "End of Execution=" + ht.get("End of Execution") + ", " + "Write Result=" + ht.get("Write Result"));
			else
				System.out.println("Instruction=" + ht.get("Instruction") + ", "  + "Write Register=" + ht.get("Write Register") + ", " +"j=" + ht.get("j") + ", " + "k=" + ht.get("k") + ", " + "Issue=" + ht.get("Issue") + ", " + "Start of Execution=" + ht.get("Start of Execution") + ", " + "End of Execution=" + ht.get("End of Execution") + ", " + "Write Result=" + ht.get("Write Result"));
		}
	}
	public static void main(String[] args) 
	{
		Processor p = new Processor();
		DataMemory.configure(1000); //data memory of size 1000 double words
		
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
		instruction3.put("OP", "DIV");//start at 6 //finishes at 9
		instruction3.put("RD", "F3");
		instruction3.put("RS", "F0");
		instruction3.put("RT", "F7");
		Hashtable<String, String> instruction4 = new Hashtable();
		instruction4.put("Order", "3"); //This tells you in in which row of the display table will u find this instruction
		instruction4.put("OP", "Load");
		instruction4.put("RS", "R0");
		instruction4.put("RT", "F3");
		instruction4.put("Immediate", "0");
		Hashtable<String, String> instruction5 = new Hashtable();
		instruction5.put("Order", "4"); //This tells you in in which row of the display table will u find this instruction
		instruction5.put("OP", "ADD");
		instruction5.put("RD", "F1");
		instruction5.put("RS", "F3");
		instruction5.put("RT", "F0");
		Hashtable<String, String> instruction6 = new Hashtable();
		instruction6.put("Order", "5"); //This tells you in in which row of the display table will u find this instruction
		instruction6.put("OP", "SUB");
		instruction6.put("RD", "F0");
		instruction6.put("RS", "F6");
		instruction6.put("RT", "F6");
		Hashtable<String, String> instruction7 = new Hashtable();
		instruction7.put("Order", "6"); //This tells you in in which row of the display table will u find this instruction
		instruction7.put("OP", "ADD");
		instruction7.put("RD", "F12");
		instruction7.put("RS", "F12");
		instruction7.put("RT", "F13");
		Hashtable<String, String> instruction8 = new Hashtable();
		instruction8.put("Order", "7"); //This tells you in in which row of the display table will u find this instruction
		instruction8.put("OP", "Store");
		instruction8.put("RS", "R0");
		instruction8.put("RT", "F2");
		instruction8.put("Immediate", "0");
		Hashtable<String, String> instruction9 = new Hashtable();
		instruction9.put("Order", "8"); //This tells you in in which row of the display table will u find this instruction
		instruction9.put("OP", "Store");
		instruction9.put("RS", "R1");
		instruction9.put("RT", "F9");
		instruction9.put("Immediate", "0");
		Hashtable<String, String> instruction10 = new Hashtable();
		instruction10.put("Order", "9"); //This tells you in in which row of the display table will u find this instruction
		instruction10.put("OP", "Load");
		instruction10.put("RS", "R0");
		instruction10.put("RT", "F15");
		instruction10.put("Immediate", "0");
		Hashtable<String, String> instruction11 = new Hashtable();
		instruction11.put("Order", "10"); //This tells you in in which row of the display table will u find this instruction
		instruction11.put("OP", "DIV");
		instruction11.put("RD", "F8");
		instruction11.put("RS", "F11");
		instruction11.put("RT", "F12");
		Hashtable<String, String> instruction12 = new Hashtable();
		instruction12.put("Order", "11"); //This tells you in in which row of the display table will u find this instruction
		instruction12.put("OP", "SUB");
		instruction12.put("RD", "F9");
		instruction12.put("RS", "F6");
		instruction12.put("RT", "F6");
		Hashtable<String, String> instruction13 = new Hashtable();
		instruction13.put("Order", "12"); //This tells you in in which row of the display table will u find this instruction
		instruction13.put("OP", "ADD");
		instruction13.put("RD", "F12");
		instruction13.put("RS", "F12");
		instruction13.put("RT", "F13");
		Hashtable<String, String> instruction14 = new Hashtable();
		instruction14.put("Order", "13"); //This tells you in in which row of the display table will u find this instruction
		instruction14.put("OP", "MUL");
		instruction14.put("RD", "F8");
		instruction14.put("RS", "F9");
		instruction14.put("RT", "F12");
		
		//ADDING INSTRUCTIONS TO INSTRUCTION QUEUE
		p.instructionQueue.add(instruction1);
		p.instructionQueue.add(instruction2);
		p.instructionQueue.add(instruction3);
		p.instructionQueue.add(instruction4);
		p.instructionQueue.add(instruction5);
		p.instructionQueue.add(instruction6);
		p.instructionQueue.add(instruction7);
		p.instructionQueue.add(instruction8);
		p.instructionQueue.add(instruction9);
		p.instructionQueue.add(instruction10);
		p.instructionQueue.add(instruction11);
		p.instructionQueue.add(instruction12);
		p.instructionQueue.add(instruction13);
		p.instructionQueue.add(instruction14);

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
		displayTableRow.put("Write Register", instruction4.get("RT"));
		displayTableRow.put("Issue", "");
		displayTableRow.put("Base Register", instruction4.get("RS"));
		displayTableRow.put("Immediate", instruction4.get("Immediate"));
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
		//add instr7
		displayTableRow = new Hashtable();
		displayTableRow.put("Instruction", instruction7.get("OP"));
		displayTableRow.put("Write Register", instruction7.get("RD"));
		displayTableRow.put("j", instruction7.get("RS"));
		displayTableRow.put("k", instruction7.get("RT"));
		displayTableRow.put("Issue", "");
		displayTableRow.put("Start of Execution", "");
		displayTableRow.put("End of Execution", "");
		displayTableRow.put("Write Result", "");
		Processor.DisplayTable.add(displayTableRow);

		//add instr8
		displayTableRow = new Hashtable();
		displayTableRow.put("Instruction", instruction8.get("OP"));
		displayTableRow.put("Source Register", instruction8.get("RT"));
		displayTableRow.put("Issue", "");
		displayTableRow.put("Base Register", instruction8.get("RS"));
		displayTableRow.put("Immediate", instruction8.get("Immediate"));
		displayTableRow.put("Start of Execution", "");
		displayTableRow.put("End of Execution", "");
		displayTableRow.put("Write Result", "");
		Processor.DisplayTable.add(displayTableRow);

		//add instr9
		displayTableRow = new Hashtable();
		displayTableRow.put("Instruction", instruction9.get("OP"));
		displayTableRow.put("Source Register", instruction9.get("RT"));
		displayTableRow.put("Issue", "");
		displayTableRow.put("Base Register", instruction9.get("RS"));
		displayTableRow.put("Immediate", instruction9.get("Immediate"));
		displayTableRow.put("Start of Execution", "");
		displayTableRow.put("End of Execution", "");
		displayTableRow.put("Write Result", "");
		Processor.DisplayTable.add(displayTableRow);

		//add instr10
		displayTableRow = new Hashtable();
		displayTableRow.put("Instruction", instruction10.get("OP"));
		displayTableRow.put("Write Register", instruction10.get("RT"));
		displayTableRow.put("Issue", "");
		displayTableRow.put("Base Register", instruction10.get("RS"));
		displayTableRow.put("Immediate", instruction10.get("Immediate"));
		displayTableRow.put("Start of Execution", "");
		displayTableRow.put("End of Execution", "");
		displayTableRow.put("Write Result", "");
		Processor.DisplayTable.add(displayTableRow);
		
		//add instr11
		displayTableRow = new Hashtable();
		displayTableRow.put("Instruction", instruction11.get("OP"));
		displayTableRow.put("Write Register", instruction11.get("RD"));
		displayTableRow.put("j", instruction11.get("RS"));
		displayTableRow.put("k", instruction11.get("RT"));
		displayTableRow.put("Issue", "");
		displayTableRow.put("Start of Execution", "");
		displayTableRow.put("End of Execution", "");
		displayTableRow.put("Write Result", "");
		Processor.DisplayTable.add(displayTableRow);
		
		//add instr12
		displayTableRow = new Hashtable();
		displayTableRow.put("Instruction", instruction12.get("OP"));
		displayTableRow.put("Write Register", instruction12.get("RD"));
		displayTableRow.put("j", instruction12.get("RS"));
		displayTableRow.put("k", instruction12.get("RT"));
		displayTableRow.put("Issue", "");
		displayTableRow.put("Start of Execution", "");
		displayTableRow.put("End of Execution", "");
		displayTableRow.put("Write Result", "");
		Processor.DisplayTable.add(displayTableRow);

		//add instr13
		displayTableRow = new Hashtable();
		displayTableRow.put("Instruction", instruction12.get("OP"));
		displayTableRow.put("Write Register", instruction12.get("RD"));
		displayTableRow.put("j", instruction12.get("RS"));
		displayTableRow.put("k", instruction12.get("RT"));
		displayTableRow.put("Issue", "");
		displayTableRow.put("Start of Execution", "");
		displayTableRow.put("End of Execution", "");
		displayTableRow.put("Write Result", "");
		Processor.DisplayTable.add(displayTableRow);

		//add instr13
		displayTableRow = new Hashtable();
		displayTableRow.put("Instruction", instruction14.get("OP"));
		displayTableRow.put("Write Register", instruction14.get("RD"));
		displayTableRow.put("j", instruction14.get("RS"));
		displayTableRow.put("k", instruction14.get("RT"));
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
		System.out.println();
		System.out.println(p.d_m_reservationStation);
		System.out.println("---");
		p.cycle++;
		
		while(!p.instructionQueue.isEmpty() 
		|| !p.a_s_reservationStation.isEmpty()
		||!p.d_m_reservationStation.isempty()
		|| !p.l_buffer.isEmpty() 
		|| !p.s_buffer.isEmpty())//keep executing until everything is empty baisically
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
			System.out.println(p.d_m_reservationStation);
			System.out.println(p.l_buffer);
			System.out.println(p.s_buffer);
			System.out.println("---");
			
			p.cycle++;//incrementing the cycle
		}
		System.out.println("Memory Location 0 = " + DataMemory.readLocation(0));
	}
}
