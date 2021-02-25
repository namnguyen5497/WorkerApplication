package WorkerApplication;

import java.io.BufferedReader;
import java.io.InputStreamReader;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DockerController {
	private static final DockerController docker = new DockerController();
	
	private DockerController(){}
	
	private static final Logger LOGGER = LogManager.getLogger(DockerController.class);
	
	public static DockerController getInstance(){
		return docker;
	}
	
	//official
	public static void deployService(String csePoa, String cseId, String cseName, 
										String serviceId, String serviceName, int commandIdNumber,
										int startImage, int endImage, long... ts) {

		String command = serviceName.split("Image")[0];

		if (command.isEmpty()) {
			return;
		}
		if (serviceName == "") {
			return;
		}
		String timeCommand = "";
		for (long t : ts) {
			timeCommand += t + " ";
		}
		String commandDeploy = "docker exec " + serviceName
				+ " python3 /opt/generateCommand.py " + command + " " + csePoa
				+ " " + cseId + " " + cseName + " " + commandIdNumber + " " 
				+ serviceId + " " + startImage + " " + endImage + " "  
				+ timeCommand;
		
		try {
			Process proc = Runtime.getRuntime().exec(commandDeploy);

			Boolean status = proc.waitFor() == 0 && proc.exitValue() == 0;
			System.out.println("Deploy status: " + status);
			if(status){
				System.out.println("Command deploy container: " + commandDeploy);
			}
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(
					proc.getInputStream()));

			String line = null;
			while ((line = stdInput.readLine()) != null) {
				System.out.println(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
			
	}
}

