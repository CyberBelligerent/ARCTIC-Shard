package com.rahman.arctic.shard.messaging;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import lombok.Data;

/**
 * Helper class to print messages to IcebergViewer in a nice format
 * @author SGT Rahman
 *
 */
@Data
public class ConsoleMessage {

	private String time;
	private String message;
	
	public ConsoleMessage(String mess) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm:ss");
		LocalDateTime now = LocalDateTime.now();
		time = dtf.format(now);
		message = mess;
	}
	
	public String displayMessage() {
		return String.format("[%s] - %s", time, message);
	}
	
}