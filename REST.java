import ru.novocar.HttpServer;

public class REST {
	
	private static HttpServer restServer;
	
	public static void main(String args[]) {
		if (args.length > 0)
			restServer = new HttpServer(Integer.parseInt(args[0]) );
		else
			restServer = new HttpServer();
		
		restServer.start();
	}
	
}