package WorkerApplication;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

public class PullData {

	private static final Logger LOGGER = LogManager.getLogger(PullData.class);

	public static void usingDiscovery(String serviceId, String dataSource,String ratioImages) throws IOException {

		String dataName = "image" + ratioImages + "_"+ serviceId;
		String target = CommonVar.WORKERCSEPOA + "/~/" + dataSource;

		// PULL
		HttpResponse response = new HttpResponse();
		response = RestHttpClient.get(CommonVar.ORIGINATOR, target + "/DATA/"+ dataName + "?con");
		JSONObject json = new JSONObject(response.getBody());
		JSONObject encodedCon = json.getJSONObject("m2m:cin");
		String encodedString = encodedCon.getString("con");

		String outputFilePath = CommonVar.DATADIR + "/" + dataName + ".zip";
		File outputFile = new File(outputFilePath);
		byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
		FileUtils.writeByteArrayToFile(outputFile, decodedBytes);

	
	}

	// official SFTP
	public static long usingSFTP(String serviceName, String dataSource,
			String serviceId, int startImage, int endImage) {
		// dataSource is an IP, need user name:
		// if manager is PC: userName@dataSource
		// String source =
		// "namnguyen@"+dataSource+":/home/namnguyen/data/origin_image/camera.jpg";
		String folderCut = "/origin_image";
		String folderDetect = "/cut_image";
		String destinationCut = "/home/pi/data/origin_image/image";
		String destinationDetect = "/home/pi/data/cut_image/image";
		// if source is PI
		// String sourceCut = "pi@"+dataSource+":/home/pi/data" + folderCut +
		// "/image";
		// String sourceDetect = "pi@"+dataSource+":/home/pi/data" +
		// folderDetect + "/image";

		// for test
		String sourceCut = "namnguyen@" + dataSource + ":/home/namnguyen/data"
				+ folderCut + "/image";
		String sourceDetect = "namnguyen@" + dataSource
				+ ":/home/namnguyen/data" + folderDetect + "/image";

		// String destination = "/home/pi/data/origin_image";
		Date now = (Calendar.getInstance()).getTime();
		Timestamp ts = new Timestamp(now.getTime());
		int i = endImage;
		Boolean status = false;
		while (i >= startImage) {

			String commandPullCut = "sftp " + sourceCut + i + ".jpg" + " "
					+ destinationCut + i + "_" + serviceId + ".jpg";
			;
			String commandPullDetect = "sftp " + sourceDetect + i + ".jpg"
					+ " " + destinationDetect + i + "_" + serviceId + ".jpg";
			;
			try {
				Process proc;
				if (serviceName.equals("CutImage")) {
					// LOGGER.info("Pull data using scp: {}", commandPullCut);
					System.out.println("Pull data using sftp: "
							+ commandPullCut);
					proc = Runtime.getRuntime().exec(
							new String[] { "/bin/bash", "-c", commandPullCut });
				} else {
					// LOGGER.info("Pull data using scp: {}",
					// commandPullDetect);
					System.out.println("Pull data using sftp: "
							+ commandPullDetect);
					proc = Runtime.getRuntime()
							.exec(new String[] { "/bin/bash", "-c",
									commandPullDetect });
				}
				status = proc.waitFor() == 0 && proc.exitValue() == 0;
				// LOGGER.info("data pull status on image{}.jpg is: {}", i,
				// status);
				System.out.println("Pull status: " + status);
				BufferedReader stdInput = new BufferedReader(
						new InputStreamReader(proc.getInputStream()));

				String line = null;
				while ((line = stdInput.readLine()) != null) {
					System.out.println(line);
				}
			} catch (Exception e) {
				e.printStackTrace();
				status = false;
			}
			i--;
		}
		if (status == false)
			LOGGER.info("Data pulled failed");
		now = (Calendar.getInstance()).getTime();
		long delta = (new Timestamp(now.getTime())).getTime() - ts.getTime();

		return delta;
	}

	// official SCP
	public static long usingSCP(String serviceId, String dataSource,String ratioImages) {

		String destinationDetect = "/home/pi/data/cut_image";

		String sourceDetect = "pi@" + dataSource + ":/home/pi/data/cut_image";

		Date now = (Calendar.getInstance()).getTime();
		Timestamp tsPull = new Timestamp(now.getTime());
		Boolean success = false;
		while (!success) {
			String commandPullDetect = "scp " + sourceDetect + "/image"
					+ ratioImages + "_" + serviceId + ".zip" + " "
					+ destinationDetect;
			try {
				Process proc;

				proc = Runtime.getRuntime().exec(
						new String[] { "/bin/bash", "-c", commandPullDetect });
				System.out.println(commandPullDetect);

				success = proc.waitFor() == 0 && proc.exitValue() == 0;
				System.out.println("data pull successful" + success);

				BufferedReader stdInput = new BufferedReader(
						new InputStreamReader(proc.getInputStream()));

				String line = null;
				System.out.println("**************");
				while ((line = stdInput.readLine()) != null) {
					// System.out.println(line);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		now = (Calendar.getInstance()).getTime();
		long delta = (new Timestamp(now.getTime())).getTime()
				- tsPull.getTime();
		return delta;

	}
}
