SET PATH="c:\Program Files (x86)\Java\jdk1.8.0_161\bin\"
javac -cp ".\jawin.jar" ru\novocar\HttpServer.java REST.java
jar cfm RESTServer.jar MANIFEST.MF REST.class ru\novocar\HttpServer.class
java -jar RESTServer.jar 
pause