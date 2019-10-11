package scc.helpers;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Util {

	private static final String DIGEST_ALGORITHM = "SHA256";

	public static String computeHash(byte[] data) throws NoSuchAlgorithmException {
		MessageDigest d = MessageDigest.getInstance(DIGEST_ALGORITHM);
		d.update(data);
		return java.util.Base64.getEncoder().encodeToString(d.digest()).replace('/', '-');
	}

}
