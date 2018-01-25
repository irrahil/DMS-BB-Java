package ru.novocar;
import java.io.*;
import java.net.*;
// import com.jacob.activeX.ActiveXComponent;
// import com.jacob.com.*;
import org.jawin.*;
import org.jawin.win32.*;
import java.nio.charset.Charset;
import java.util.Date;

//Данный класс описывает функциональность простого HTTP сервера для получения XML-данных по протоколу HTTP
public class HttpServer {
	
	private int port;
	private ServerSocket serverSocket;
	private InputStream in = null;
	private OutputStream out = null;
	// private ActiveXComponent v8App = null;
	// private Dispatch database = null;
	// jawin
	DispatchPtr v8App;
	DispatchPtr v8worker;
	
	
	//Конструктор с портом сервера по умолчанию
	public HttpServer() {
		serverSocket = null;
		port = 80;
		init();
	}
	
	public HttpServer(int serverPort) {
		serverSocket = null;
		port = serverPort;
		init();
	}
	
	
	
	private void init() {
		// jawin
		try {
			Ole32.CoInitialize();
		} catch(COMException e) {
			System.out.println(e.toString() );
		}
		try {
			serverSocket = new ServerSocket(port);
			System.out.println("REST server started on port: " + serverSocket.getLocalPort() );
		} catch (IOException e) {
			System.out.println("Port " + port + " is blocked. Choose another port");
			System.out.println("Exception info: " + e.toString() );
			serverSocket = null;
		}
		boolean result = connectTo1C();
		if (result == true)  {
				System.out.println("Successful connection to 1C");
		}
		else { 
			System.out.println("Error in connection");
		}
	}
	
	
	
	public void start() {
		if (serverSocket != null)
			while(true) {
				try {
					Socket clientSocket = serverSocket.accept();
					System.out.println("New incomming connection");
					this.in = clientSocket.getInputStream();
					this.out = clientSocket.getOutputStream();
					requestWork();
					clientSocket.close();
				}
				catch (IOException e) {
					System.out.println(e.toString() );
				}
			}
	}
	
   
   
   ////////////////////////////////////////////////////////////////////////////
   //		1C work
   
   
   
   private boolean connectTo1C() {
	   try {
		    v8App = new DispatchPtr("V83.COMConnector");
			// v8App = new ActiveXComponent("V83.COMConnector");
			String conString = "Srvr=\"192.168.1.74\";Ref=\"AlfaAutoSkoda\";Usr=\"DMSBB\";Pwd=\"dms-BB4\"";
			// String conString = "File=\"F:\\Develop\\AA-DMS\"; Usr=\"admin\"; Pwd=\"test\"";
			// Variant connection = v8App.invoke("Connect", "Srvr=\"192.168.1.74\";Ref=\"AlfaAutoSkoda\";Usr=\"DMSBB\";Pwd=\"dms-BB4\"");
			// Variant connection1C = Dispatch.call(v8App, "Connect", conString);
			// return connection1C.getBoolean();
			// jawin
			v8worker = (DispatchPtr) v8App.invoke("Connect", conString);
			return true;
		}catch (COMException e) {
			System.out.println(e.toString() );
			return false;
		}finally {
			
		}
   }
   
   
   	private String sendTo1C(String url, String body) {
		// try {
		// int functionId = Dispatch.getIDOfName(v8App, "DMSBB") ;
		// System.out.println(functionId) ;
		// Variant res = Dispatch.call(v8App, "DMSBB");
		// System.out.println(res.getvt() );
		//}catch (ComFailException e) {
		//	System.out.println(e.toString() );
		//}
		// jawin
		try {
			// DispatchPtr objModul = (DispatchPtr) v8worker.get("ВнешнееСоединение");
			String result = (String) v8worker.invoke("DMSBB", url, body);
			System.out.println("Result: " + result);
			return result;
		}catch (COMException e) {
			System.out.println(e.toString() );
			return "";
		}
	}
	
	
	
	
	//////////////////////////////////////////////////////////////////////////////
	//		SOCKET work
	
	private String getHeader(int code) {
      StringBuilder buffer = new StringBuilder();
      buffer.append("HTTP/1.1 " + code + " " + getAnswer(code) + "\n");
      buffer.append("Date: " + new Date().toGMTString() + "\n");
      buffer.append("Accept-Ranges: none\n");
      buffer.append("Content-Type: text/xml\n");
      buffer.append("\n");
      return buffer.toString();
   }
   
   
   
   private String getAnswer(int code) {
      switch (code) {
      case 200:
         return "OK";
      case 404:
         return "Not Found";
      default:
         return "Internal Server Error";
      }
   }
	
	
	private void requestWork() {
		//Получаем данные о заголовке
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(this.in) );
			StringBuilder headerBuilder = new StringBuilder();
			String strLine = null;
			//Чтение HEADER части
			int ContentLength = 0;
			while (true) {
				strLine = reader.readLine();
				if (strLine == null || strLine.isEmpty() ) {
					break;
				}
				if (strLine.contains("Content-Length:")  ) {
					String clStr = strLine.substring(strLine.indexOf(": ") + 2 );
					
					ContentLength = Integer.parseInt(clStr);
					
				}
				headerBuilder.append(strLine + System.getProperty("line.separator") );
			}
			//DEBUG
			String urlFromDMS = getURIFromHeader(headerBuilder.toString() );
			System.out.println("Header: " + headerBuilder.toString() );
			StringBuilder bodyBuilder = new StringBuilder();
			for (int i = 0; i < ContentLength; i++) {
				bodyBuilder.append( (char) reader.read() );
			}
			//DEBUG
			System.out.println("Body: " + bodyBuilder.toString() );
			String contentFromDMS = bodyBuilder.toString();
			//TODO sending to 1C
			String result = sendTo1C(urlFromDMS, contentFromDMS);
			PrintStream answer = new PrintStream(out, true, "UTF-8");
			answer.print(getHeader(200) );
			byte[] buffer = result.getBytes(Charset.forName("UTF-8")); 
			out.write(buffer, 0, buffer.length);
		} catch (IOException e) {
			System.out.println(e.toString() );
		}
		
	}
	
	
	
	private String getURIFromHeader(String header) {
      int from = header.indexOf(" ") + 1;
      int to = header.indexOf(" ", from);
      String uri = header.substring(from, to);
      int paramIndex = uri.indexOf("?");
      if (paramIndex != -1) {
         uri = uri.substring(0, paramIndex);
      }
      return uri;
   }

	
}