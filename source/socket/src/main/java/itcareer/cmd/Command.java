package itcareer.cmd;

public class Command {
	// Backend command
	public static final String BACKEND_POST_NOTIFICATION="BACKEND_POST_NOTIFICATION";
	public static final String BACKEND_PROCESS_VIDEO = "BACKEND_PROCESS_VIDEO";
	public static final String MEDIA_COMPLETED_PROCESS_VIDEO = "MEDIA_COMPLETED_PROCESS_VIDEO";

	//CLIENT
	public static final String CLIENT_INFO = "CLIENT_INFO";
	public static final String CLIENT_PING = "CLIENT_PING";
	public static final String CLIENT_RECEIVED_PUSH_NOTIFICATION = "CLIENT_RECEIVED_PUSH_NOTIFICATION";
	public static final String TEST_CMD = "TEST_CMD";



	public static boolean ignoreToken(String cmd){
		switch (cmd){
			case TEST_CMD:
				return true;
			default:
				return false;
		}
	}
}
