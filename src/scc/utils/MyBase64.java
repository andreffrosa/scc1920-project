package scc.utils;

public class MyBase64 {
	
	public static String encode(byte[] data) {
		return data == null ? null : java.util.Base64.getEncoder().encodeToString(data).replace("/", "-");
	}
	
	public static String encode(String str) {
		return str == null ? null : java.util.Base64.getEncoder().encodeToString(str.getBytes()).replace("/", "-");
	}
	
	public static String decode(String code) {
		return code == null ? null : new String(java.util.Base64.getDecoder().decode(code.replace("-", "/")));
	}

}
