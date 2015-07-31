package edu.bupt.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.AsyncTask;
import android.util.Log;

public class HttpClientHelper extends AsyncTask<String,String,String>{
	
	public String get(String url) {
		String result = "";
		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpRequest = new HttpGet(url);
		InputStream is = null;
		try {
			HttpResponse httpResponse = httpClient.execute(httpRequest);
			if(httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				is = httpResponse.getEntity().getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				StringBuilder builder = new StringBuilder();
				String line = "";
				while((line=reader.readLine())!=null) {
					builder.append(line + "\n");
					publishProgress(line + "\n");
				}
				is.close();
				result = builder.toString();
			} else {
				result = "Request error!";
			}
			httpClient.getConnectionManager().shutdown();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	protected String doInBackground(String... params) {
		Log.d("get request", "running");
		return get(params[0]);
	}

	@Override
	protected void onProgressUpdate(String... values) {
		super.onProgressUpdate(values);
		updateProgress(values[0]);
	}

	@Override
	protected void onPostExecute(String result) {
		super.onPostExecute(result);
		onFinished();
	}
	
	public void onFinished() {
	}
	
	public void updateProgress(String values) {
	}
	
}
