package WorkerApplication;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

public class RestHttpClient {
	public static HttpResponse get(String originator, String uri) {
		System.out.println("HTTP GET "+uri);
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpGet= new HttpGet(uri);
 
		httpGet.addHeader("X-M2M-Origin",originator);
		httpGet.addHeader("Accept","application/json");
	
		
 
		HttpResponse httpResponse = new HttpResponse();
 
		try {
			//System.out.println("Request to be sent: " + httpGet);
			CloseableHttpResponse closeableHttpResponse = httpclient.execute(httpGet);
			try{
				httpResponse.setStatusCode(closeableHttpResponse.getStatusLine().getStatusCode());
				httpResponse.setBody(EntityUtils.toString(closeableHttpResponse.getEntity()));
			}finally{
				closeableHttpResponse.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
		//System.out.println("HTTP Response "+httpResponse.getStatusCode()+"\n"+httpResponse.getBody());
		return httpResponse;	
	}
	
	public static HttpResponse post(String originator, String uri, String body, int ty) {
		//System.out.println("HTTP POST "+uri+"\n"+body);
 
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(uri);
		
		httpPost.addHeader("X-M2M-Origin",originator);
		//httpPost.addHeader("X-M2M-OT",ts.toString());
		httpPost.addHeader("Accept","application/json");	
		httpPost.addHeader("Content-Type","application/json;ty="+ty);
 
		HttpResponse httpResponse = new HttpResponse();
		try {
			CloseableHttpResponse closeableHttpResponse=null;
			try{
				httpPost.setEntity(new StringEntity(body));
				//System.out.println("Request to be sent: " + httpPost);
				//System.out.println("Request header: " + httpPost.getAllHeaders());
				closeableHttpResponse = httpclient.execute(httpPost);
				httpResponse.setStatusCode(closeableHttpResponse.getStatusLine().getStatusCode());
				String bodyToJson = EntityUtils.toString(closeableHttpResponse.getEntity());
				httpResponse.setBody(bodyToJson);
				//System.out.println(new JSONObject(bodyToJson));
 
			}finally{
				closeableHttpResponse.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
		//System.out.println("HTTP Response "+httpResponse.getStatusCode()+"\n"+httpResponse.getBody());
		return httpResponse ;	
	}
}
