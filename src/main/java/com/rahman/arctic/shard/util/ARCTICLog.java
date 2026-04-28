package com.rahman.arctic.shard.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

public final class ARCTICLog {

	public interface LogSink {
		void send(String topic, String line);
	}

	private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final InheritableThreadLocal<String> CURRENT = new InheritableThreadLocal<>();
	private static final Path LOG_ROOT = Paths.get("deployment-logs");

	private static volatile LogSink sink;

	private ARCTICLog() {}

	public static void setSink(LogSink s) {
		sink = s;
	}

	public static void setDeployment(String deploymentId) {
		if (deploymentId == null || deploymentId.isBlank()) CURRENT.remove();
		else CURRENT.set(deploymentId);
	}

	public static void clearDeployment() {
		CURRENT.remove();
	}

	public static String currentDeployment() {
		return CURRENT.get();
	}

	public static String print(String tag, String msg) {
		return print(CURRENT.get(), tag, msg);
	}

	public static String print(String deploymentId, String tag, String msg) {
		String line = format(tag, msg);
		System.out.println(line);
		if (deploymentId != null && !deploymentId.isBlank()) {
			appendFile(deploymentId, line);
			LogSink s = sink;
			if (s != null) {
				try {
					s.send("/topic/deployment/" + deploymentId + "/monitor", line);
				} catch (Exception e) {
					System.err.println("[ARCTICLog] sink failed: " + e.getMessage());
				}
			}
		}
		return line;
	}

	public static String err(String tag, String msg) {
		return err(CURRENT.get(), tag, msg);
	}

	public static String err(String deploymentId, String tag, String msg) {
		String line = format(tag, msg);
		System.err.println(line);
		if (deploymentId != null && !deploymentId.isBlank()) {
			appendFile(deploymentId, line);
			LogSink s = sink;
			if (s != null) {
				try {
					s.send("/topic/deployment/" + deploymentId + "/monitor", line);
				} catch (Exception e) {
					System.err.println("[ARCTICLog] sink failed: " + e.getMessage());
				}
			}
		}
		return line;
	}

	public static Runnable wrap(Runnable r) {
		String captured = CURRENT.get();
		return () -> {
			String prior = CURRENT.get();
			if (captured != null) CURRENT.set(captured);
			try {
				r.run();
			} finally {
				if (prior == null) CURRENT.remove();
				else CURRENT.set(prior);
			}
		};
	}

	public static List<String> readHistory(String deploymentId) {
		if (deploymentId == null || deploymentId.isBlank()) return Collections.emptyList();
		Path p = LOG_ROOT.resolve(deploymentId + ".log");
		if (!Files.exists(p)) return Collections.emptyList();
		try {
			return Files.readAllLines(p, StandardCharsets.UTF_8);
		} catch (IOException e) {
			System.err.println("[ARCTICLog] failed reading history for " + deploymentId + ": " + e.getMessage());
			return Collections.emptyList();
		}
	}

	private static String format(String tag, String msg) {
		String t = tag == null ? "" : tag;
		String m = msg == null ? "" : msg;
		return "[" + t + "] - [" + LocalDateTime.now().format(STAMP) + "] - " + m;
	}

	private static void appendFile(String deploymentId, String line) {
		try {
			Files.createDirectories(LOG_ROOT);
			Files.writeString(LOG_ROOT.resolve(deploymentId + ".log"), line + System.lineSeparator(),
					StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			System.err.println("[ARCTICLog] failed writing file for " + deploymentId + ": " + e.getMessage());
		}
	}
}
