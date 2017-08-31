package http;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import util.SignUtil;

public class HttpInvoker {

	CloseableHttpClient httpClient;

	PoolingHttpClientConnectionManager poolConnManager;

	public HttpInvoker(String url) throws Exception {
		init(url);
	}

	public void init(String url) throws Exception {
		
		Properties prop = new Properties();  
		InputStream is = new BufferedInputStream(new FileInputStream(url));
		prop.load(is);
		is.close();
		
		//�������ӳ����������
		int maxTotal = StringUtils.isNotEmpty(prop.getProperty("maxTotal")) ? 
				Integer.parseInt(prop.getProperty("maxTotal")) : 200;
		//������󲢷����ʸ���
		int defaultMaxPerRoute = StringUtils.isNotEmpty(prop.getProperty("defaultMaxPerRoute")) ? 
				Integer.parseInt(prop.getProperty("defaultMaxPerRoute")) : 20;
		//���ö�ȡ���ݵĺ��뼶��ʱʱ��
		int soTimeout = StringUtils.isNotEmpty(prop.getProperty("soTimeout")) ? 
				Integer.parseInt(prop.getProperty("soTimeout")) : 10000;	
		//�������������ӵĺ��뼶��ʱʱ��
		int connectionRequestTimeout = StringUtils.isNotEmpty(prop.getProperty("connectionRequestTimeout")) ?
				Integer.parseInt(prop.getProperty("connectionRequestTimeout")) : 3000;
		//�������ӽ���ʱ�ĺ��뼶��ʱʱ��		
		int connectTimeout = StringUtils.isNotEmpty(prop.getProperty("connectTimeout")) ? 
				Integer.parseInt(prop.getProperty("connectTimeout")) : 3000;
		//�����׽��ֵĺ��뼶��ʱʱ��
		int socketTimeout = StringUtils.isNotEmpty(prop.getProperty("socketTimeout")) ? 
				Integer.parseInt(prop.getProperty("socketTimeout")) : 10000;
		
		SSLContext sslcontext = SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy(){
			@Override
            public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                return true;
            }
		}).build();
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext);
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
				.register("http", PlainConnectionSocketFactory.getSocketFactory()).register("https", sslsf).build();
		poolConnManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
		// Increase max total connection to 200
		poolConnManager.setMaxTotal(maxTotal);
		// Increase default max connection per route to 20
		poolConnManager.setDefaultMaxPerRoute(defaultMaxPerRoute);
		SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(soTimeout).build();
		poolConnManager.setDefaultSocketConfig(socketConfig);

		RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(connectionRequestTimeout)
				.setConnectTimeout(connectTimeout).setSocketTimeout(socketTimeout).build();
		httpClient = HttpClients.custom().setConnectionManager(poolConnManager).setDefaultRequestConfig(requestConfig)
				.build();
		if (poolConnManager.getTotalStats() != null) {
			System.out.println("now client pool " + poolConnManager.getTotalStats().toString());
		}
	}

	public void destoy() {
		try {
			if (null != httpClient)
				httpClient.close();
			if (null != poolConnManager)
				poolConnManager.close();
		} catch (IOException e) {
			throw new RuntimeException("Error in destroy HttpClient : ", e);
		}

	}
	
	public String invoker(String testUrl, Map<String, String> params, String methodType, Map<String,String> header){
		if("POST".equalsIgnoreCase(methodType) || "PUT".equalsIgnoreCase(methodType))
			return this.putOrPostMethod(testUrl, params, methodType, header);
		return this.getOrDeleteMethod(testUrl, params, methodType, header);
			
	}

	private String getOrDeleteMethod(String testUrl, Map<String, String> params, String methodType, Map<String,String> header) {
		String result = null;

		HttpRequestBase method;

		try {

			testUrl += "?" + urlencode(params);

			if ("GET".equalsIgnoreCase(methodType))
				method = new HttpGet(testUrl);
			else
				method = new HttpDelete(testUrl);

			StringBuilder headersToSign = new StringBuilder();
			// ����RequestHeader
			if (null != header) {
				for(Map.Entry<String, String> map : header.entrySet()){
					method.addHeader(map.getKey(), map.getValue());
					if (headersToSign.length() > 0)
						headersToSign.append(";");
						headersToSign.append(map.getKey());
				}

				if ("appkey".equalsIgnoreCase(header.get("authoration"))) {
					String signature = SignUtil.sign(header.get("X-Ca-secret"), testUrl,
							method.getAllHeaders(), null);
					method.addHeader("X-Ca-Signature", signature);
					method.addHeader("X-Ca-Signature-Header", headersToSign.toString());
				}
			}

			if (null == httpClient)
				init("");

			HttpResponse httpResponse = httpClient.execute(method);

			int statusCode = httpResponse.getStatusLine().getStatusCode();
			if (statusCode != 200) {
				HttpEntity entity = httpResponse.getEntity();
				String errMsg = EntityUtils.toString(entity);
				result = "error status : " + statusCode + ", " + errMsg;
			} else {
				HttpEntity entity = httpResponse.getEntity();
				result = EntityUtils.toString(entity);
			}
			method.releaseConnection();
		} catch (Exception e) {
			result = e.getMessage();
			if (result == null && e.getCause() != null)
				result = e.getCause().getMessage();

			if (result == null)
				result = "Unknown exception";
		}

		return result;
	}

	private String putOrPostMethod(String testUrl, Map<String,String> params, String methodType, Map<String,String> header) {
		String result = null;
		HttpEntityEnclosingRequestBase method;

		try {
			if ("PUT".equalsIgnoreCase(methodType))
				method = new HttpPut(testUrl);
			else
				method = new HttpPost(testUrl);
			
			JSONObject paramsJson = new JSONObject(params);
			
			HttpEntity requestEntity = new StringEntity(paramsJson.toString(), ContentType.APPLICATION_JSON);
			method.setEntity(requestEntity);

			// ����RequestHeader
			StringBuilder headersToSign = new StringBuilder();
			if (null != header) {

				for(Map.Entry<String, String> map :  header.entrySet()){
					method.addHeader(map.getKey(), map.getValue());
					if (headersToSign.length() > 0)
						headersToSign.append(";");
					headersToSign.append(map.getKey());
				}

				// ����ǩ��
				if ("appkey".equalsIgnoreCase(header.get("authoration"))) {
					String signature = SignUtil.sign(header.get("X-Ca-secret"), testUrl,
							method.getAllHeaders(), null);
					method.addHeader("X-Ca-Signature", signature);
					method.addHeader("X-Ca-Signature-Header", headersToSign.toString());
				}
			}

			if (null == httpClient)
				init("");

			HttpResponse httpResponse = httpClient.execute(method);

			int statusCode = httpResponse.getStatusLine().getStatusCode();
			if (statusCode != 200) {
				HttpEntity entity = httpResponse.getEntity();
				String errMsg = EntityUtils.toString(entity);
				result = "error status : " + statusCode + ", " + errMsg;
			} else {
				HttpEntity entity = httpResponse.getEntity();
				result = EntityUtils.toString(entity);
			}
			method.releaseConnection();
		} catch (Exception e) {
			result = e.getMessage();
			if (result == null && e.getCause() != null)
				result = e.getCause().getMessage();

			if (result == null)
				result = "Unknown exception";
		}

		return result;
	}

	public static String urlencode(Map<String, String> data) {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> i : data.entrySet()) {
			try {
				if (("").equals(i.getKey())) {
					sb.append(URLEncoder.encode(i.getValue() + "", "UTF-8"));
				} else {
					sb.append(i.getKey()).append("=").append(URLEncoder.encode(i.getValue() + "", "UTF-8")).append("&");
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

}
