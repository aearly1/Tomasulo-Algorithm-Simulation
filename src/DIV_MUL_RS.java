import java.util.Hashtable;

public class DIV_MUL_RS {
	Hashtable<String, Object>[] reservationStation;
	int div_latency,mul_latency,ava_res;
	
	public DIV_MUL_RS(int d_lat,int m_lat,int count)
	{
		     
		reservationStation = new Hashtable[count];
		this.ava_res=count;
		this.div_latency=d_lat;
		this.mul_latency=m_lat;
		for(int i=0;i<count;i++)
		{
			this.reservationStation[i]=new Hashtable<String,Object>();
			this.reservationStation[i].put("RowNr","");
			this.reservationStation[i].put("Tag","M"+(i+1));
			this.reservationStation[i].put("Time", 0);
			this.reservationStation[i].put("Busy", 0);
			this.reservationStation[i].put("Op", ""); 
			this.reservationStation[i].put("Vj", ""); 
			this.reservationStation[i].put("Vk", ""); 
			this.reservationStation[i].put("Qj", 0);
			this.reservationStation[i].put("Qk", 0);
		}
		//System.out.println(mul_latency);
		//System.out.println(div_latency);
	}
	
	public boolean isfree()
	{
		return this.ava_res!=0;
		
	}
	
	public boolean isempty()
	{
		return this.reservationStation.length==this.ava_res;
	}
	
	
	
	
	public String issue(String OP, Hashtable operand1, Hashtable operand2, String rowNr)
	
	{
		for (int i = 0; i < reservationStation.length; i++) {
			if(reservationStation[i].get("Busy").equals(0))
			{
				
				
				this.ava_res--;
				
				reservationStation[i].put("Busy", 1); 
				reservationStation[i].put("Op", OP); 
				
				
				if(operand1.get("Qi").equals(0))
				{
					reservationStation[i].put("Vj", (Double)operand1.get("Val")); 
					reservationStation[i].put("Qj", operand1.get("Qi"));//actually = 0 
				}
				else
				{
					reservationStation[i].put("Vj", ""); 
					reservationStation[i].put("Qj", operand1.get("Qi"));
				}
				
				
				if(operand2.get("Qi").equals(0))
				{
					reservationStation[i].put("Vk", (Double)operand2.get("Val")); 
					reservationStation[i].put("Qk", operand2.get("Qi"));//actually = 0 
				}
				else
				{
					reservationStation[i].put("Vk", ""); 
					reservationStation[i].put("Qk", operand2.get("Qi"));
				}
				
				
				if(reservationStation[i].get("Qj").equals(0) && reservationStation[i].get("Qk").equals(0))
				{
					if (OP.equals("DIV"))
					{
						
						reservationStation[i].put("Time", this.div_latency+1);
					}
					else
					{
						
						reservationStation[i].put("Time", this.mul_latency+1);
					}
				}
				reservationStation[i].put("RowNr", rowNr);
				
				return (String) reservationStation[i].get("Tag");
				}
				}
		return null;
		}
		
	
	
	
	
	
	public void execute(int cycle)
	{
		for (int i = 0; i < reservationStation.length; i++)
		{
			if(reservationStation[i].get("Busy").equals(1) && !reservationStation[i].get("Time").equals(0))
			{
				if(reservationStation[i].get("Op").equals("DIV"))
				{
					if(reservationStation[i].get("Time").equals(this.div_latency))
					{
						//System.out.println("Start of Execution"+ cycle+""+reservationStation[i].get("Op"));
						
						Processor.DisplayTable.get(Integer.parseInt((String) reservationStation[i].get("RowNr"))).put("Start of Execution", cycle+"");
					}
				}
				else
				{
					if(reservationStation[i].get("Time").equals(this.mul_latency))
					{
						//System.out.println("Start of Execution"+ cycle+""+reservationStation[i].get("Op"));
						Processor.DisplayTable.get(Integer.parseInt((String) reservationStation[i].get("RowNr"))).put("Start of Execution", cycle+"");
					}
				}
				
				reservationStation[i].put("Time",(Integer)reservationStation[i].get("Time")-1);
				
				if(reservationStation[i].get("Time").equals(0))
				{
					Processor.DisplayTable.get(Integer.parseInt((String) reservationStation[i].get("RowNr"))).put("End of Execution", cycle+"");//update end of execution field in table
				}
			}
		}
	}
	
	
	
	
	
	public Hashtable getResults(int cycle)
	{
		for (int i = 0; i < reservationStation.length; i++)
		{
			if(reservationStation[i].get("Busy").equals(1) && reservationStation[i].get("Time").equals(0) && reservationStation[i].get("Qj").equals(0) && reservationStation[i].get("Qk").equals(0) && !Processor.DisplayTable.get(Integer.parseInt((String) reservationStation[i].get("RowNr"))).get("End of Execution").equals(cycle+""))
			{
				
				Hashtable tagAndVal= new Hashtable<String, String>();
				
				tagAndVal.put("Tag", reservationStation[i].get("Tag"));
				
				double result=0;
				if(reservationStation[i].get("Op").equals("DIV"))
					result=(Double) reservationStation[i].get("Vj")/(Double) reservationStation[i].get("Vk");
				else 
					result=(Double)reservationStation[i].get("Vj")*(Double)reservationStation[i].get("Vk");

				tagAndVal.put("Val", result);
				tagAndVal.put("RowNr", reservationStation[i].get("RowNr"));
				
				return tagAndVal;
			}
		}
		return null;
	}
	
	
	
	
	
	public void recieveTag(String tag, double val)
	
	{
		for (int i = 0; i < reservationStation.length; i++) {
			if(reservationStation[i].get("Qj").equals(tag))
			{
				reservationStation[i].put("Vj", val);
				reservationStation[i].put("Qj", 0);
				if(reservationStation[i].get("Qj").equals(0) && reservationStation[i].get("Qk").equals(0))
				{
					if(this.reservationStation[i].get("Op").equals("DIV"))
					{
						reservationStation[i].put("Time", this.div_latency);
					}
					else
					{
						reservationStation[i].put("Time", this.mul_latency);
					}
				}
				
			}
			if(reservationStation[i].get("Qk").equals(tag))
			{
				reservationStation[i].put("Vk", val); 
				reservationStation[i].put("Qk", 0);
				if(reservationStation[i].get("Qj").equals(0) && reservationStation[i].get("Qk").equals(0))
				{
					if(this.reservationStation[i].get("Op").equals("DIV"))
					{
						reservationStation[i].put("Time", this.div_latency);
					}
					else
					{
						reservationStation[i].put("Time", this.mul_latency);
					}
				}
			}
		}
	}
	
	
	
	
	public void eraseInstruction(String tag)
	
	{
		int i= Integer.parseInt(tag.substring(1))-1;
		reservationStation[i] = new Hashtable<String, Object>();
		reservationStation[i].put("Tag", "M"+(i+1));
		reservationStation[i].put("Time", 0);
		reservationStation[i].put("Busy", 0); 
		reservationStation[i].put("Op", ""); 
		reservationStation[i].put("Vj", ""); 
		reservationStation[i].put("Vk", ""); 
		reservationStation[i].put("Qj", 0);
		reservationStation[i].put("Qk", 0); 
		this.ava_res++;
	}
	
	
	public String toString()
	
	{
		String output="Multiplication/Division Reservation Station: " + '\n';
		for(Hashtable<String, Object> ht: reservationStation)
		{
			output+= "Tag=" + ht.get("Tag")+", "+ "Time=" + ht.get("Time") +  ", " +"Busy=" + ht.get("Busy") + ", " + "Op=" + ht.get("Op") + ", " + "Vj=" + ht.get("Vj") + ", " + "Vk=" + ht.get("Vk") + ", " + "Qj=" + ht.get("Qj") + ", " + "Qk=" + ht.get("Qk") + ", " + '\n';
		}
		return  output;
	}
	
//	public static void main(String[]args)
//	{
//		DIV_MUL_RS X=new DIV_MUL_RS(4,3,2);
//		Hashtable<String, Object> A=new Hashtable<String, Object>();
//		Hashtable<String, Object> B=new Hashtable<String, Object>();
//		A.put("Qi","A1");
//		A.put("Val",10.0);
//		B.put("Qi",0);
//		B.put("Val",11.0);
//		System.out.println(X);
//		X.issue("MUL", A, A,"1");
//		X.execute(1);
//		System.out.println(X);
//		
//		
//		//X.issue("DIV", B, B,"2");
//		X.execute(2);
//		System.out.println(X);
//		
//		X.recieveTag("A1",5);
//		//X.execute(3);
//		System.out.println(X);
//		
//		X.eraseInstruction("M1");
//		System.out.println(X);
//		
//		
////		X.execute(4);
////		System.out.println(X);
////		System.out.println(X);
////		System.out.println(X.getResults(4));
////		X.execute(5);
////		System.out.println(X);
////		System.out.println(X.getResults(5));
////		System.out.println(X.issue("MUL", A, B,"1")+"");
////		System.out.println(X.isfree());
//		
//		
//	}
}
