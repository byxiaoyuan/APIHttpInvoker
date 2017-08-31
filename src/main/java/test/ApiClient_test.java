package test;

import java.util.HashMap;
import java.util.Map;

import http.HttpInvoker;

public class ApiClient_test {
	
	private  HttpInvoker httpInvoker; 
	//��֤��ʽ
	private String authoration = "apicode";
	//����api��ַ
	private String testUrl = "https://api.yonyoucloud.com/apis/dst/DSTUfidaEightInterfaces/getPersonBatch";
	//���󷽷�����
	private String methodType = "POST";
	//�̳߳ز����ļ�·��
	private static final String propertyUrl = ApiClient_test.class.getResource("HttpClient.properties").getPath();
	
	public ApiClient_test() throws Exception{
		System.out.println(propertyUrl);
		httpInvoker = new HttpInvoker(propertyUrl);
	}
	
	public void test(){
		Map<String,String> params = new HashMap<String,String>();
		params.put("method", "");
		params.put("format", "");
		params.put("appKey","");
		params.put("request", "");
		params.put("plat", "");
		params.put("session", "");
		
		Map<String,String> header = new HashMap<String,String>();
		header.put("authoration", authoration);
		header.put("apicode", "411af791cf2c4b7dbb7137833fbc6d73");
		
		String result = httpInvoker.invoker(testUrl, params, methodType, header);
		System.out.println(result);
		httpInvoker.destoy();
	}
	
	//�ر��̳߳�
	public void destoy(){
		httpInvoker.destoy();
	}
	
	public static void main(String[] args){
		ApiClient_test  test = null;
		try {
		test = new ApiClient_test();
		
		for(int i = 0; i < 5 ; i++){

				test.test();
				Thread.sleep(18000);
			
		}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			test.destoy();
		}
		
		
	}
	
}
