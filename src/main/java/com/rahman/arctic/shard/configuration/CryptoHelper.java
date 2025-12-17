package com.rahman.arctic.shard.configuration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

@Component
public class CryptoHelper {

	private final SecretKey generatedKey;

	public CryptoHelper() {
		generatedKey = new SecretKeySpec(loadOrGenerateKeyFromFile(), "AES");
	}

	public String decryptValue(String encryptedString) {
		if(encryptedString == null || encryptedString.isBlank()) {
			return "";
		} else {
			byte[] encryptedBytes = Base64.getDecoder().decode(encryptedString);

			byte[] encryptedStringBytes = new byte[encryptedBytes.length - 12];
			byte[] ivBytes = new byte[12];
			for (int i = 0; i < 12; i++) {
				ivBytes[i] = encryptedBytes[i];
			}

			for (int k = 12; k < encryptedBytes.length; k++) {
				encryptedStringBytes[k - 12] = encryptedBytes[k];
			}

			GCMParameterSpec iv = new GCMParameterSpec(128, ivBytes);

			return new String(decrypt(encryptedStringBytes, iv), StandardCharsets.UTF_8);
		}
	}

	public String encryptValue(String plainTextString) {
		if(plainTextString == null || plainTextString.isBlank()) {
			return "";
		} else {
			GCMParameterSpec iv = new GCMParameterSpec(128, generateIv());
			byte[] encryptedString = encrypt(plainTextString, iv);

			ByteBuffer buffer = ByteBuffer.wrap(new byte[iv.getIV().length + encryptedString.length]);
			buffer.put(iv.getIV());
			buffer.put(encryptedString);
			byte[] encryptedPair = buffer.array();

			return Base64.getEncoder().encodeToString(encryptedPair);
		}
	}
	
	private byte[] loadOrGenerateKeyFromFile() {
		File f = new File("./.arctic/master.key");
		if(!f.exists()) {
			if(!f.getParentFile().exists()) f.getParentFile().mkdirs();
			
			byte[] key = new byte[32];
			new SecureRandom().nextBytes(key);
			
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(f))) {
				writer.write(Base64.getEncoder().encodeToString(key));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return key;
		}
		
		try(BufferedReader reader = new BufferedReader(new FileReader(f))) {
			String line = reader.readLine();
			if (line == null || line.isBlank()) {
			    // TODO: handle corrupt key file
			    return null;
			}
			return Base64.getDecoder().decode(line);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	private byte[] generateIv() {
		byte[] iv = new byte[12];
		new SecureRandom().nextBytes(iv);
		return iv;
	}

	private byte[] decrypt(byte[] cipherText, GCMParameterSpec iv) {
		try {
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, generatedKey, iv);
			return cipher.doFinal(cipherText);
		} catch (Exception e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	private byte[] encrypt(String input, GCMParameterSpec iv) {
		try {
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, generatedKey, iv);
			return cipher.doFinal(input.getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

}