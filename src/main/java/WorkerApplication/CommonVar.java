package WorkerApplication;

public class CommonVar {
	private CommonVar(){}
	
	public static final String ORIGINATOR="admin:admin";
	public static final String CSEPROTOCOL="http";
	public static final String WORKERCSEIP = "192.168.0.103";
	public static final int WORKERCSEPORT = 8181;
	public static final String WORKERCSEID = "worker-4-id";
	public static final String WORKERCSENAME = "worker-4";
	
	public static final String AENAME = "WorkerAE";
	public static final String AEPROTOCOL="http";
	public static final String AEIP = "192.168.0.103";
	public static final int AEPORT = 1500;	
	public static final String AESUB="WorkerSub";
	
	public static final String WORKERCSEPOA = CSEPROTOCOL+"://"+WORKERCSEIP+":"+WORKERCSEPORT;
	public static final String APPPOA = AEPROTOCOL+"://"+AEIP+":"+AEPORT;
	public static final String NU = "/"+WORKERCSEID+"/"+WORKERCSENAME+"/"+AENAME;
 
	public static final String[] CONTAINER = {"COMMAND", "MONITOR", "SERVICE", "RESULT", "DATA"};
	public static final String[] SUBCONTAINER = {"COMMAND"};
	
	
	public static final String DATADIR = "/home/pi/data/cut_image";
}
