package com.rahman.arctic.shard.messaging;

import org.springframework.stereotype.Service;

/**
 * Sets where to print the ArcticTask information to
 * @author SGT Rahman
 *
 */
@Service
public class IcebergViewer {

//	@Autowired
//	private static SimpMessagingTemplate tmp;
	
	/**
	 * Sends ArcticTask information to the WebSocket associated to the RangeExercise
	 * @param ex RangeExercise ID
	 * @param mess Message to send to the WebSocket
	 */
	public static void sendConsoleBuildUpdate(String ex, ConsoleMessage mess) {
		System.out.println(mess.displayMessage());
//		if(ex == null) {
//			System.out.println(mess.displayMessage());
//		} else {
//			ObjectMapper mapper = new ObjectMapper();
//			ObjectWriter w = mapper.writer();
//			
//			try {
//				tmp.convertAndSend("/stomp-api/v1/listener/build/" + ex.getId(), w.writeValueAsString(mess));
//			} catch (MessagingException | JsonProcessingException e) {
//				e.printStackTrace();
//			}
//		}
	}
	
}