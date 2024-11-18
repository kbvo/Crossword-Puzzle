package kbvo_CSCI201_Assignment2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client extends Thread {
	private BufferedReader in;
	private Socket s;
	private BufferedReader br;
	private PrintWriter pw;
	public Client() {
		in = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Welcome to 201 Crossword!");
		boolean connected = false;
		while (!connected) {
			try {
				System.out.print("Enter the server hostname: ");
				String hostname = in.readLine();
				System.out.print("Enter the server port: ");
				int port = Integer.parseInt(in.readLine());
				s = new Socket(hostname, port);
				System.out.println("");
				br = new BufferedReader(new InputStreamReader(s.getInputStream()));
	            pw = new PrintWriter(s.getOutputStream(), true);
	            connected = true;
	            
	            String prompt;
	            while (true) {
	            	if ((prompt = br.readLine()) != null) {
	            		if (prompt.equals("Terminate client.")) {
	            			System.exit(0); // Security risk if user inputs "Terminate client."
	            		}
	            		if (prompt.startsWith("BOARD_START")) {
	              
	                        StringBuilder boardDisplay = new StringBuilder();

	                        while (!(prompt = br.readLine()).equals("BOARD_END")) {
	                            boardDisplay.append(prompt).append("\n");
	                        }
	                        System.out.println(boardDisplay.toString() + "\n");
	            		}
	            		else if (prompt.endsWith("?")) {
	             	       System.out.print(prompt + " ");
	             	       String response = in.readLine();
	             	       pw.println(response);
	             	    }
	             	   	else {
	             		   System.out.println(prompt);
	             	   	}
	            	}   
	            }
			} 
			catch (IOException ioe) {
				System.out.println("Error: That is not a vaild server." + "\n");
			}
			catch (NumberFormatException nfe){
				System.out.println("Error: That is not a vaild server." + "\n");
			}
			catch (IllegalArgumentException iae) {
				System.out.println("Error: That is not a vaild server." + "\n");
			}
		}
	}
	
	
	public static void main(String [] args) {
		new Client();
	}
}
