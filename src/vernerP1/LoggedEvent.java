package vernerP1;

public class LoggedEvent {
	public String status;
	public String message;
	public Exception exception;
	public int	level;
	
	public LoggedEvent(String status, String m, Exception e, int level) {
		this.status = status;
		this.message = m;
		this.exception = e;
		this.level = level;
	}
	
	public String toString() {
		if (exception== null)
			return (status + " L" + level + " :" + message);
		else
			return (status + " L" + level + " :" + message + " Exception " + exception);
	}
}
