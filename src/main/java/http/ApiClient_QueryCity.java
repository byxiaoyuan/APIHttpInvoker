package http;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ApiClient_QueryCity {

	private HttpInvoker httpInvoker;
	// 验证方式 apicode或apikey 默认apicode
	private String authoration = "apicode";

	// 测试api地址
	private String testUrl = "https://api.yonyoucloud.com/apis/dst/regionsOfChina/allRegions";
	// 请求方法类型
	private String methodType = "GET";
	// 线程池参数文件路
	private static final String propertyUrl = ApiClient_test.class.getResource("HttpClient.properties").getPath();;

	public ApiClient_QueryCity() throws Exception {
		httpInvoker = new HttpInvoker(propertyUrl);
	}

	public void test() {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
		System.out.println("start :" + df.format(new Date()));// new
																// Date()为获取当前系统时间
		System.out.println("start :" + System.currentTimeMillis());

		Map<String, String> params = new HashMap<String, String>();
		Map<String, String> header = new HashMap<String, String>();
		header.put("authoration", authoration);
		header.put("apicode", "7e5a9de589804d7987ae95dd0738b071");

	

		String result = httpInvoker.invoker(testUrl, params, methodType, header);
		System.out.println(result);
		SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
		System.out.println("end :" + df2.format(new Date()));// new
																// Date()为获取当前系统时间
		System.out.println("end :" + System.currentTimeMillis());
	}

	// 关闭线程
	public void destoy() {
		httpInvoker.destoy();
	}

	public static void main(String[] args) {

		ApiClient_QueryCity apiClient = null;
		try {
			apiClient = new ApiClient_QueryCity();
			for (int i = 0; i < 5; i++) {
				apiClient.test();
				Thread.sleep(180000 * (i+1));
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			apiClient.destoy();
		}
	}
}