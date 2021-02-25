package WorkerApplication;

import java.io.File;

public class CountFile {
	
	private CountFile(){};
	
	public int countLeftFile(String serviceId){
		String folderDetect = "home/pi/data/cut_image/" + serviceId;
		int n = -1;
		try{
			File Files = new File(folderDetect);
			n = Files.list().length;
		}catch(Exception e){
			return n;
		}
		return n;
	}
}
