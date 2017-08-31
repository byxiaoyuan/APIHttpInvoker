package util;

import java.security.SignatureException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import consts.Constants;
import consts.HttpHeader;
import consts.SystemHeader;




/**
 * 签名工具
 */
public class SignUtil {
	/**
	 * 计算签名
	 *
	 * @param secret
	 *            APP密钥
	 * @param method
	 *            HttpMethod
	 * @param path
	 * @param headers
	 * @param querys
	 * @param bodys
	 * @param signHeaderPrefixList
	 *            自定义参与签名Header前缀
	 * @return 签名后的字符串
	 * @throws JSONException
	 */
	public static String sign(String secret, String path, Header[] headers, String bodys) throws JSONException {
		Map<String, String> headerMap = new HashMap<String, String>();
		for (Header header : headers) {
			headerMap.put(header.getName(), header.getValue());
		}

		return signSha1(secret, null, path, headerMap, null, bodys, null);
	}

	/**
	 * json格式String转map
	 * 
	 * @param jsonString
	 * @throws JSONException
	 */
	private static Map<String, String> jsonString2Map(String jsonString) throws JSONException {
		if (null == jsonString)
			return null;

		JSONObject json = new JSONObject(jsonString);
		Map<String, String> map = new HashMap<String, String>();

		@SuppressWarnings("unchecked")
		Iterator<String> it = json.keys();
		while (it.hasNext()) {
			String key = (String) it.next();
			map.put(key, json.getString(key));
		}

		return map;
	}

	/**
	 * 计算签名
	 *
	 * @param secret
	 *            APP密钥
	 * @param method
	 *            HttpMethod
	 * @param path
	 * @param headers
	 * @param querys
	 * @param bodys
	 * @param signHeaderPrefixList
	 *            自定义参与签名Header前缀
	 * @return 签名后的字符串
	 */
	public static String signSha1(String secret, String method, String path, Map<String, String> headers,
			Map<String, String> querys, String bodys, List<String> signHeaderPrefixList) {
		try {
			return hmacSHA1(buildStringToSign(method, path, headers, querys, bodys, signHeaderPrefixList), secret);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 计算签名
	 *
	 * @param secret
	 *            APP密钥
	 * @param method
	 *            HttpMethod
	 * @param path
	 * @param headers
	 * @param querys
	 * @param bodys
	 * @param signHeaderPrefixList
	 *            自定义参与签名Header前缀
	 * @return 签名后的字符串
	 */
	public static String sign(String secret, String method, String path, Map<String, String> headers,
			Map<String, String> querys, String bodys, List<String> signHeaderPrefixList) {
		try {
			Mac hmacSha1 = Mac.getInstance(Constants.HMAC_SHA1);
			byte[] keyBytes = secret.getBytes(Constants.ENCODING);
			hmacSha1.init(new SecretKeySpec(keyBytes, 0, keyBytes.length, Constants.HMAC_SHA1));

			return new String(Base64.encodeBase64(
					hmacSha1.doFinal(buildStringToSign(method, path, headers, querys, bodys, signHeaderPrefixList)
							.getBytes(Constants.ENCODING))),
					Constants.ENCODING);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 构建待签名字符串
	 * 
	 * @param method
	 * @param path
	 * @param headers
	 * @param querys
	 * @param bodys
	 * @param signHeaderPrefixList
	 * @return
	 */
	private static String buildStringToSign(String method, String path, Map<String, String> headers,
			Map<String, String> querys, String bodys, List<String> signHeaderPrefixList) {
		StringBuilder sb = new StringBuilder();

		if (null != method)
			sb.append(method.toUpperCase()).append(Constants.LF);
		
		sb.append(buildHeaders(headers));
		sb.append(buildResource(path, querys));

		if (null != bodys)
			sb.append(Constants.LF).append(bodys);

		return sb.toString();
	}

	/**
	 * 构建待签名Path+Query+BODY
	 *
	 * @param path
	 * @param querys
	 * @param bodys
	 * @return 待签名
	 */
	private static String buildResource(String path, Map<String, String> querys) {
		StringBuilder sb = new StringBuilder();

		if (!StringUtils.isBlank(path)) {
			int index = path.indexOf("/apis/");
			if (index > 0)
				path = path.substring(index);
			sb.append(path);
		}
		Map<String, String> sortMap = new TreeMap<String, String>();
		if (null != querys) {
			for (Map.Entry<String, String> query : querys.entrySet()) {
				if (!StringUtils.isBlank(query.getKey())) {
					sortMap.put(query.getKey(), query.getValue());
				}
			}
		}

		StringBuilder sbParam = new StringBuilder();
		for (Map.Entry<String, String> item : sortMap.entrySet()) {
			if (!StringUtils.isBlank(item.getKey())) {
				if (0 < sbParam.length()) {
					sbParam.append(Constants.SPE3);
				}
				sbParam.append(item.getKey());
				if (!StringUtils.isBlank(item.getValue())) {
					sbParam.append(Constants.SPE4).append(item.getValue());
				}
			}
		}
		if (0 < sbParam.length()) {
			sb.append(Constants.SPE5);
			sb.append(sbParam);
		}

		return sb.toString();
	}

	/**
	 * 构建待签名Http头
	 *
	 * @param headers
	 *            请求中所有的Http头
	 * @param signHeaderPrefixList
	 *            自定义参与签名Header前缀
	 * @return 待签名Http头
	 */
	private static String buildHeaders(Map<String, String> headers, List<String> signHeaderPrefixList) {
		StringBuilder sb = new StringBuilder();

		if (null != signHeaderPrefixList) {
			signHeaderPrefixList.remove(SystemHeader.X_CA_SIGNATURE);
			signHeaderPrefixList.remove(HttpHeader.HTTP_HEADER_ACCEPT);
			signHeaderPrefixList.remove(HttpHeader.HTTP_HEADER_CONTENT_MD5);
			signHeaderPrefixList.remove(HttpHeader.HTTP_HEADER_CONTENT_TYPE);
			signHeaderPrefixList.remove(HttpHeader.HTTP_HEADER_DATE);
			Collections.sort(signHeaderPrefixList);
			if (null != headers) {
				Map<String, String> sortMap = new TreeMap<String, String>();
				sortMap.putAll(headers);
				StringBuilder signHeadersStringBuilder = new StringBuilder();
				for (Map.Entry<String, String> header : sortMap.entrySet()) {
					if (isHeaderToSign(header.getKey(), signHeaderPrefixList)) {
						sb.append(header.getKey());
						sb.append(Constants.SPE2);
						if (!StringUtils.isBlank(header.getValue())) {
							sb.append(header.getValue());
						}
						sb.append(Constants.LF);
						if (0 < signHeadersStringBuilder.length()) {
							signHeadersStringBuilder.append(Constants.SPE1);
						}
						signHeadersStringBuilder.append(header.getKey());
					}
				}
				headers.put(SystemHeader.X_CA_SIGNATURE_HEADERS, signHeadersStringBuilder.toString());
			}
		}

		return sb.toString();
	}

	/**
	 * 构建待签名Http头
	 *
	 * @param headers
	 *            请求中所有的Http头
	 * @param signHeaderPrefixList
	 *            自定义参与签名Header前缀
	 * @return 待签名Http头
	 */
	private static String buildHeaders(Map<String, String> headers) {
		StringBuilder sb = new StringBuilder();

		if (null != headers) {
			Map<String, String> sortMap = new TreeMap<String, String>(new Comparator<String>() {
				public int compare(String key1, String key2) {
					return key1.compareTo(key2);
				}
			});

			sortMap.putAll(headers);
			StringBuilder signHeadersStringBuilder = new StringBuilder();
			for (Map.Entry<String, String> header : sortMap.entrySet()) {
				sb.append(header.getKey());
				sb.append(Constants.SPE2);
				if (!StringUtils.isBlank(header.getValue())) {
					sb.append(header.getValue());
				}
				sb.append(Constants.LF);
				if (0 < signHeadersStringBuilder.length()) {
					signHeadersStringBuilder.append(Constants.SPE1);
				}
				signHeadersStringBuilder.append(header.getKey());
			}
			headers.put(SystemHeader.X_CA_SIGNATURE_HEADERS, signHeadersStringBuilder.toString());
		}

		return sb.toString();
	}

	/**
	 * Http头是否参与签名 return
	 */
	private static boolean isHeaderToSign(String headerName, List<String> signHeaderPrefixList) {
		if (StringUtils.isBlank(headerName)) {
			return false;
		}

		if (headerName.startsWith(Constants.CA_HEADER_TO_SIGN_PREFIX_SYSTEM)) {
			return true;
		}

		if (null != signHeaderPrefixList) {
			for (String signHeaderPrefix : signHeaderPrefixList) {
				if (headerName.equalsIgnoreCase(signHeaderPrefix)) {
					return true;
				}
			}
		}

		return false;
	}

	public static String hmacSHA1(String data, String key) throws java.security.SignatureException {
		String result;
		String HMAC_SHA1_ALGORITHM = "HmacSHA1";
		try {

			// get an hmac_sha1 key from the raw key bytes
			SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);

			// get an hmac_sha1 Mac instance and initialize with the signing key
			Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
			mac.init(signingKey);

			// compute the hmac on input data bytes
			byte[] rawHmac = mac.doFinal(data.getBytes());

			// base64-encode the hmac
			result = new String(Base64.encodeBase64(rawHmac), "UTF-8");
			// result = BASE64Encoder.encode(rawHmac);
			// result = Encoding.EncodeBase64(rawHmac);

		} catch (Exception e) {
			throw new SignatureException("Failed to generate HMAC : " + e.getMessage());
		}
		return result;
	}
}
