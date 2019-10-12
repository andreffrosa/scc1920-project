package scc.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Encryption {

	private static final String DIGEST_ALGORITHM = "SHA256";

	public static String computeHash(byte[] data) throws NoSuchAlgorithmException {
		MessageDigest d = MessageDigest.getInstance(DIGEST_ALGORITHM);
		d.update(data);
		return java.util.Base64.getEncoder().encodeToString(d.digest()).replace('/', '-');
	}

}
