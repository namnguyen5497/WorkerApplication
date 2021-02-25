package WorkerApplication;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class WorkerApplication {
	
	private static Container[] cnt = new Container[1];
	private static int index = 0;
	private static int nService = 1;
	
	private static OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
	
	private final static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	
	
	private static final Logger LOGGER = LogManager.getLogger(WorkerApplication.class);
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
	
	public static void main(String[] args) {
		
		//Start server 
		HttpServer server = null;
		try {
			server = HttpServer.create(new InetSocketAddress(CommonVar.AEPORT), 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
		server.createContext("/", new MyHandler());
		server.setExecutor(Executors.newCachedThreadPool());
		server.start();
		
		LOGGER.info("Worker AE started");
		
		//Create WorkerAE in worker'OM2M
		JSONArray array = new JSONArray();
		array.put(CommonVar.APPPOA);
		JSONObject obj = new JSONObject();
		obj.put("rn", CommonVar.AENAME);
		obj.put("api", 12346);
		obj.put("rr", true);
		obj.put("poa",array);
		JSONObject resource = new JSONObject();
		resource.put("m2m:ae", obj);
		RestHttpClient.post(CommonVar.ORIGINATOR, CommonVar.WORKERCSEPOA+"/~/"+
							CommonVar.WORKERCSEID+"/"+CommonVar.WORKERCSENAME, 
							resource.toString(), 2);
		LOGGER.info("Registered Worker AE to CSE");
		
		//Create container 
		for(String i : CommonVar.CONTAINER){
			obj = new JSONObject();
		    obj.put("rn", i);
		    obj.put("mni", "10000" );
		    resource.put("m2m:cnt", obj);
			RestHttpClient.post(CommonVar.ORIGINATOR, CommonVar.WORKERCSEPOA+"/~/"+
								CommonVar.WORKERCSEID+"/"+CommonVar.WORKERCSENAME,  
								resource.toString(), 3);
		}
		LOGGER.info("Created all containers on WorkerCse");
		
		//subscribe to container command
		JSONObject json;
		HttpResponse response;
		int j = 0;
		for(String i : CommonVar.SUBCONTAINER){
			array = new JSONArray();
			array.put(CommonVar.NU);
			obj = new JSONObject();
			obj.put("nu", array);
			obj.put("rn", CommonVar.AESUB);
			obj.put("nct", 2);
			resource = new JSONObject();		
			resource.put("m2m:sub", obj);
			response = RestHttpClient.post(CommonVar.ORIGINATOR, 
											CommonVar.WORKERCSEPOA+"/~/"+CommonVar.WORKERCSEID+"/"+
											CommonVar.WORKERCSENAME+"/"+i, 
											resource.toString(), 23);			
			json = new JSONObject(response.getBody());
			cnt[j] = new Container(i, json.getJSONObject("m2m:sub").get("pi").toString());
			j++;
		}
		
		/*
		 * 
		 */
		final Runnable periodRequest = new Runnable(){
			public void run() {
				if(nService < 101){
					System.out.println("Periodic sending service");
					//Date now = (Calendar.getInstance()).getTime();
					//Timestamp ts = new Timestamp(now.getTime());
					JSONObject obj = new JSONObject();
					JSONObject resource = new JSONObject();
					obj.put("rn", "testService_"+ sdf.format(Calendar.getInstance().getTime()));
					obj.put("cnf", "application/text");
					List<JSONObject> content = new ArrayList<JSONObject>();
					UUID serviceId = UUID.randomUUID(); //generate random unique serviceId 
					content.add((new JSONObject()).put("SERVICE", "DetectImage")); //0
					content.add((new JSONObject()).put("SERVICEID", serviceId.toString())); //1
					content.add((new JSONObject()).put("NOAWORKER", 0)); // 2 - Number of Assigned Workers
					content.add((new JSONObject()).put("DTSOURCE", CommonVar.WORKERCSEIP)); // 3
					content.add((new JSONObject()).put("WORKLOAD", 50)); //4 number of images
					content.add((new JSONObject()).put("NSERVICE", nService)); //5
					obj.put("con", content.toString());
					resource.put("m2m:cin", obj);
					RestHttpClient.post(CommonVar.ORIGINATOR, CommonVar.WORKERCSEPOA+"/~/"+
										CommonVar.WORKERCSEID+"/"+CommonVar.WORKERCSENAME
										+"/"+"SERVICE" , resource.toString(), 4);
					System.out.println("Sent service " + nService + " with ID: {}"+ serviceId.toString());
					nService++;
				}
			}	
		};
		//scheduler.scheduleAtFixedRate(periodRequest, 15000, 1200, TimeUnit.MILLISECONDS);
		// (runnable, runAfter, period, TimeUnit)
		
		//FOR TEST ONLY
		//scheduler.schedule(periodRequest,15000,TimeUnit.MILLISECONDS);
	}
 
	static class MyHandler implements HttpHandler {
		
		static SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
		static Calendar cal;
 
		public void handle(HttpExchange httpExchange)  {
			System.out.println("Event Recieved!");
 
			try{
				InputStream in = httpExchange.getRequestBody();
				
 
				String requestBody = "";
				int i;char c;
				while ((i = in.read()) != -1) {
					c = (char) i;
					requestBody = (String) (requestBody+c);
				}
				
				//System.out.println(requestBody);
				Headers inHeader = httpExchange.getRequestHeaders();
				String headerTimeStamp = inHeader.getFirst("X-M2M-OT");
				//System.out.println("HeaderOT: "+headerTimeStamp);
				
				JSONObject json = new JSONObject(requestBody);
				
				String responseBody ="";
				byte[] out = responseBody.getBytes("UTF-8");
				httpExchange.sendResponseHeaders(200, out.length);
				OutputStream os = httpExchange.getResponseBody();
				os.write(out);
				os.close();
				
				if(json.getJSONObject("m2m:sgn").has("m2m:vrq")){
					if (json.getJSONObject("m2m:sgn").getBoolean("m2m:vrq")) {
						//System.out.println("Confirm subscription"); 
						}
				}
				else {
					JSONObject rep = json.getJSONObject("m2m:sgn").getJSONObject("m2m:nev")
							.getJSONObject("m2m:rep").getJSONObject("m2m:cin");
					int ty = rep.getInt("ty");
					String pi = rep.getString("pi");
					//System.out.println("Resource type: "+ty);
					for(int j = 0; j<cnt.length; j++){
						if(pi.equals(cnt[j].getContainerID())){
							
							if(cnt[j].getContainerName().equals("COMMAND")){
								JSONArray content = new JSONArray(rep.getString("con"));
								JSONObject command = content.getJSONObject(0).getJSONObject("COMMAND"); //Command always at index 0
								int commandIdNumber = command.getInt("COMMANDID");
								int commandCode = command.getInt("COMMANDCODE");
								
								/*
								 * Period checkup
								 */
								if(commandCode == 1){
									//LOGGER.info("Received period checkup command");
									List<JSONObject> result = new ArrayList<JSONObject>();
									result.add((new JSONObject()).put("COMMANDID", commandIdNumber)); //0
									result.add((new JSONObject()).put("COMMANDCODE", commandCode)); //1
							
									List<JSONObject> resourceInfo = new ArrayList<JSONObject>();
									resourceInfo.add((new JSONObject()).put("CPU", osBean.getSystemCpuLoad()*100));
									resourceInfo.add((new JSONObject()).put("RAM", (osBean.getTotalPhysicalMemorySize() - osBean.getFreePhysicalMemorySize()) 
																/ osBean.getTotalPhysicalMemorySize())); // (Total - Free / Total)
									result.add((new JSONObject()).put("RESOURCEINFO", resourceInfo)); //2
									result.add((new JSONObject()).put("CWORKLOAD", getCurrentWorkload())); //3
									Date now = (Calendar.getInstance()).getTime();
									Timestamp ts = new Timestamp(now.getTime());
									result.add((new JSONObject()).put("TS", ts.toString())); //4  --> track time take to send monitor mess from this worker to manager
									JSONObject obj = new JSONObject();
									cal = Calendar.getInstance();
									obj.put("rn", "monitor_result_" + commandIdNumber + "_" + sdf.format(cal.getTime()));
									obj.put("cnf", "application/text");
									obj.put("con", result.toString());
									JSONObject resource = new JSONObject();
									resource.put("m2m:cin", obj);
									RestHttpClient.post(CommonVar.ORIGINATOR,CommonVar.WORKERCSEPOA+"/~/"+
														CommonVar.WORKERCSEID+"/"+CommonVar.WORKERCSENAME+
														"/MONITOR" , resource.toString(), 4);
								}
								
								/*
								 * Deploy container service
								 */
								if(commandCode == 2){ 
									//LOGGER.info("Received service deployment command");
									//Date now = (Calendar.getInstance()).getTime();
									//Timestamp tsNow = new Timestamp(now.getTime()); //t1
									//long deltaT1 = tsNow.getTime() - Timestamp.valueOf(headerTimeStamp).getTime(); //header = t0
									Date now = (Calendar.getInstance()).getTime();
									Timestamp ts = new Timestamp(now.getTime());
									String dataSource = command.getString("DTSOURCE");
									if(dataSource.equals(CommonVar.WORKERCSEID+"/"+CommonVar.WORKERCSENAME)){ 
										// DockerController.Deploy
										System.out.println("DataSource is at this worker, skip pulling data");
										ZipUtils.UnzipData(command.getInt("STARTIMAGE") + "-" + command.getInt("ENDIMAGE"), 
															command.getString("SERVICEID"));
										now = (Calendar.getInstance()).getTime();
										LOGGER.info("Unzip data time for service {}: {}", command.getString("SERVICEID"), 
													(new Timestamp(now.getTime())).getTime() - ts.getTime());
										
										DockerController.deployService(CommonVar.WORKERCSEPOA, // result destination
																		CommonVar.WORKERCSEID, 
																		CommonVar.WORKERCSENAME, 
																		command.getString("SERVICEID"), 
																		command.getString("SERVICE"), 
																		commandIdNumber,
																		command.getInt("STARTIMAGE"), 
																		command.getInt("ENDIMAGE") 
																		); //deltaT1
									}else{ //if datasource is at another worker, then pull first and deploy after that
										
										
										String ratioImages = command.getInt("STARTIMAGE") + "-" + command.getInt("ENDIMAGE");
										
										PullData.usingDiscovery(command.getString("SERVICEID"), 
																				dataSource,
																				ratioImages);
										now = (Calendar.getInstance()).getTime();
										Timestamp ts2 = new Timestamp(now.getTime());
										LOGGER.info("Pull data time for service {}: {}", command.getString("SERVICEID"), 
													ts2.getTime() - ts.getTime());
										//unzip data file to folder name = serviceId
										
										ZipUtils.UnzipData(ratioImages, command.getString("SERVICEID"));
										now = (Calendar.getInstance()).getTime();
										LOGGER.info("Unzip data time for service {}: {}", command.getString("SERVICEID"), 
													(new Timestamp(now.getTime())).getTime() - ts2.getTime());
										
										//LOGGER.INFO("Sending command to container");
										DockerController.deployService(CommonVar.WORKERCSEPOA,  //result destination
												command.getString("TARGETID"), 
												command.getString("TARGETNAME"), 
												command.getString("SERVICEID"), 
												command.getString("SERVICE"), 
												commandIdNumber, 
												command.getInt("STARTIMAGE"), 
												command.getInt("ENDIMAGE"));
												//deltaT1, deltaT2);	
									}
								}
								
								
								//Zip file command 
								if(commandCode == 3){ //zip file
									//get Zip image ratio
									int zipState = 1; // if Zipping success => send this state
									Date now = (Calendar.getInstance()).getTime();
									Timestamp tsZip = new Timestamp(now.getTime());
									JSONArray arr = new JSONArray(command.getString("ZIP"));
									for (int index = 0; index < arr.length(); index++){
										try{
											//System.out.println(" ratioImage = " + arr.getString(index));
											ZipUtils.ZipData(arr.getString(index), command.getString("SERVICEID"));
											ZipUtils.encodeDataAndPush("dataName");
										}catch(Exception e){
											System.out.println("Something Wrong with data IO");
											e.printStackTrace();
											zipState = 0; // if Zipping doesnt success
										}
									}
									now = (Calendar.getInstance()).getTime();
									LOGGER.info("Zip and push data time for service {}: {}", command.getString("SERVICEID"), 
												(new Timestamp(now.getTime())).getTime() - tsZip.getTime());
									//send back zip data completion messsage
									List<JSONObject> result = new ArrayList<JSONObject>();
									result.add((new JSONObject()).put("SERVICEID", command.getString("SERVICEID"))); //0
									result.add((new JSONObject()).put("SERVICE", "ZIP")); //1
									result.add((new JSONObject()).put("COMMANDID", commandIdNumber)); //2
									result.add((new JSONObject()).put("ZIPSTATE", zipState)); //3
									
									JSONObject obj = new JSONObject();
									cal = Calendar.getInstance();
									obj.put("rn", "zip_result" + commandIdNumber + "_" + command.getString("SERVICEID"));
									obj.put("cnf", "application/text");
									obj.put("con", result.toString());
									JSONObject resource = new JSONObject();
									resource.put("m2m:cin", obj);
									RestHttpClient.post(CommonVar.ORIGINATOR, CommonVar.WORKERCSEPOA+"/~/"+
														CommonVar.WORKERCSEID+"/"+CommonVar.WORKERCSENAME+
														"/RESULT" , resource.toString(), 4);
								}
							}
						}	
					}
				}
				
 
			} catch(Exception e){
				e.printStackTrace();
			}		
		}
	}
	private static int getCurrentWorkload(){
		int currentWorkload = 0;
		File folder = new File(CommonVar.DATADIR);
		File[] listOfFile = folder.listFiles();
		if(listOfFile.length == 0){
			return 0;
		}
		for(File file : listOfFile){
			if(file.isDirectory()){
				//System.out.println(file.getAbsolutePath());
				folder = new File(file.getAbsolutePath());
				currentWorkload += folder.listFiles().length;
			}
		}
		
		return currentWorkload;
	}
}



class Container{
	private String containerName;
	private String containerID;

	public Container(String containerName, String containerID){
		this.containerName = containerName;
		this.containerID= containerID;	
	}

	public String getContainerID(){
		return containerID;
	}

	public String getContainerName(){
		return containerName;
	}
}
