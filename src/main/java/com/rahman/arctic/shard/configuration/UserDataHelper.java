package com.rahman.arctic.shard.configuration;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.commons.codec.digest.Crypt;

public class UserDataHelper {
	
	public static String createBasicLinuxUserData(String username, String password) {
		return createLinuxUserData(username, password, "Generated User", "ALL=(ALL) NOPASSWD:ALL", "SHA-512", 4096);
	}
	
	public static String createLinuxUserData(String username, String password, String gecos, String sudo, String hashType, int rounds) {
		String hashedPass = Crypt.crypt(password, "$6$" + "xWLG30EKmD");
		
		String tmp = "#cloud-config\n\n" +
		        "users:\n" +
		        "  - name: " + username + "\n" +
		        "    gecos: " + gecos + "\n" +
		        "    groups: wheel\n" +
		        "    lock_passwd: false\n" +
		        "    shell: /bin/bash\n" +
		        "    passwd: " + hashedPass + "\n" +
		        "    sudo: " + sudo + "\n" +
		        "  - name: ansible_prov\n" +
		        "    gecos: Generated User\n" +
		        "    groups: wheel\n" +
		        "    lock_passwd: false\n" +
		        "    shell: /bin/bash\n" +
		        "    passwd: $6$rounds=4096$W8VXLKTbsnl25U9Z$z9KwaHffDjL8tA41c.zt6Ex1DPScS58OIviw.VCCrZ4vNrjcDSCWkw6nu2yKD/sbxz0ulHW9jnv9LfFXz7Ky.0\n" +
		        "    sudo: ALL=(ALL) NOPASSWD:ALL\n";
		
		return Base64.getEncoder().encodeToString(tmp.getBytes(StandardCharsets.UTF_8));
	}
	
}