package kbvo_CSCI201_Assignment2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
	private List<ServerThread> serverThreads = Collections.synchronizedList(new ArrayList<>());
	private Map<String, String[]> gameData = Collections.synchronizedMap(new HashMap<>());
	private List<String> myWords = Collections.synchronizedList(new ArrayList<>());
	private int connectedPlayers;
	private int totalPlayers;
	private boolean gameStarted;
	private String[][] gameBoard = new String[21][21];
	private String savedClues = null;
	private Map<String, int[]> placedWordStart = new HashMap<>();
	private AtomicInteger currentTurnIndex = new AtomicInteger(0);

	public Server(int port) {
		try (ServerSocket ss = new ServerSocket(port)) {
			connectedPlayers = 0;
			System.out.println("Listening on port 3456.");
			System.out.println("Waiting for players...");
			while (true) {
				Socket s = ss.accept();
				if (gameStarted) {
	                PrintWriter out = new PrintWriter(s.getOutputStream(), true);
	                out.println("Terminate client.");
	                continue;
	            }
				connectedPlayers++;
				System.out.println("\n" + "Connection from: " + s.getInetAddress().getHostAddress() + "\n");
				ServerThread st = new ServerThread(s, this);
				serverThreads.add(st);
			}
		} catch (IOException ioe) {
			System.out.println("Error: Server is already up.");
		}
	}

	public void broadcast(String message, ServerThread st) {
		for (ServerThread threads : serverThreads) {
			if (threads != st) {
				threads.sendMessage(message);
			}
		}
	}

	
	public void startGame() {
		if (gameData.isEmpty() && myWords.isEmpty()) {
			Game game = new Game();
			game.start();

			try {
				game.join();
			} catch (InterruptedException ie) {
				System.out.println("Game thread interrupted.");
			}

			gameData = game.getGameData();
			myWords = game.getWords();
			if (gameData.isEmpty() && myWords.isEmpty()) {
				broadcast("No valid game files in the directory.\n", null);
			}
		}
	}

	public synchronized void nextTurn() {
        currentTurnIndex.updateAndGet(i -> (i + 1) % totalPlayers);
    }

	public synchronized int getCurrentTurnIndex() {
        return currentTurnIndex.get();
    }
	
	public synchronized int getConnectedPlayers() {
		return connectedPlayers;
	}

	public synchronized void setConnectedPlayers(int connectedPlayers) {
		this.connectedPlayers = connectedPlayers;
	}

	
	public synchronized int getTotalPlayers() {
		return totalPlayers;
	}

	public synchronized void setTotalPlayers(int totalPlayers) {
		this.totalPlayers = totalPlayers;
	}
	
	public synchronized Map<String, String[]> getGameData() {
		return gameData;
	}

	public synchronized void setGameData(Map<String, String[]> gameData) {
		this.gameData = gameData;
	}



	public synchronized List<ServerThread> getServerThreads() {
		return serverThreads;
	}

	public synchronized void setServerThreads(List<ServerThread> serverThreads) {
		this.serverThreads = serverThreads;
	}

	public synchronized Map<String, int[]> getPlacedWordStart() {
		return placedWordStart;
	}

	public synchronized void setPlacedWordStart(Map<String, int[]> placedWordStart) {
		this.placedWordStart = placedWordStart;
	}

	public synchronized boolean isGameStarted() {
		return gameStarted;
	}

	public synchronized void setGameStarted(boolean gameStarted) {
		this.gameStarted = gameStarted;
	}

	
	public void placeWordsOnBoard() {
		for (int i = 0; i < gameBoard.length; i++) {
		    Arrays.fill(gameBoard[i], "   ");
		}
		List<String> unplacedWords = myWords;
		ArrayDeque<String> placedWords = new ArrayDeque<>();

		Collections.sort(unplacedWords, (s1, s2) -> Integer.compare(s2.length(), s1.length()));

		String firstWord = unplacedWords.remove(0);
		int firstRow = gameBoard.length / 2;
		int firstCol = gameBoard[0].length / 2;
		String firstDirection = gameData.get(firstWord)[0];
		if (firstDirection.equals("ACROSS")) {
			while (firstCol >= 0) {
				if (checkWordAcross(firstRow, firstCol, firstWord)) {
					placedWords.add(firstWord);
					placedWordStart.put(firstWord, placeWordAcross(firstRow, firstCol, firstWord));
					break;
				}
				firstCol -= 1;
			}
		} 
		else {
			while (firstRow >= 0) {
				if (checkWordDown(firstRow, firstCol, firstWord)) {
					placedWords.add(firstWord);
					placedWordStart.put(firstWord, placeWordDown(firstRow, firstCol, firstWord));
					break;
				}
				firstRow -= 1;
			}
		}

		if (firstRow < 0 || firstCol < 0) {
			System.out.println(firstWord + " is too big for crossword grid. Crossword grid is 21x21 as is NY Times Sunday crossword.");
			return;
		}

		int n = unplacedWords.size();
		int count = 0;
		while (count < n) {
			for (int k = 0; k < unplacedWords.size(); ++k) {
				String unplacedWord = unplacedWords.remove(0);
				String direction = gameData.get(unplacedWord)[0];
				int index = Integer.parseInt(gameData.get(unplacedWord)[1]);

				for (String placedWord : placedWords) {
					if (index == Integer.parseInt(gameData.get(placedWord)[1])) {
						if (direction.equals("ACROSS")) {
							if (checkWordAcrossHead(placedWordStart.get(placedWord)[0],
									placedWordStart.get(placedWord)[1], unplacedWord)) {
								placedWords.add(unplacedWord);
								placedWordStart.put(unplacedWord,
										placeWordAcrossHead(placedWordStart.get(placedWord)[0],
												placedWordStart.get(placedWord)[1], unplacedWord));
							}
						} 
						else {
							if (checkWordDownHead(placedWordStart.get(placedWord)[0],
									placedWordStart.get(placedWord)[1], unplacedWord)) {
								placedWords.add(unplacedWord);
								placedWordStart.put(unplacedWord, placeWordDownHead(placedWordStart.get(placedWord)[0],
										placedWordStart.get(placedWord)[1], unplacedWord));
							}
						}
						break;
					}
				}

				if (!placedWords.contains(unplacedWord)) {
					for (String placedWord : placedWords) {
						if ((direction.equals("ACROSS") && gameData.get(placedWord)[0].equals("DOWN"))
								|| (direction.equals("DOWN") && gameData.get(placedWord)[0].equals("ACROSS"))) {
							for (int i = unplacedWord.length() - 1; i >= 0; --i) {
								for (int j = placedWord.length() - 1; j >= 0; --j) {
									if (Character.toLowerCase(placedWord.charAt(j)) == Character
											.toLowerCase((unplacedWord.charAt(i))) && (i != 0 && j != 0)) {
										if (direction.equals("ACROSS")) {
											if (checkIntersectWordAcross(i, j, unplacedWord, placedWord,
													placedWordStart)) {
												placedWords.add(unplacedWord);
												placedWordStart.put(unplacedWord, placeIntersectWordAcross(i, j,
														unplacedWord, placedWord, placedWordStart));
												break;
											}
										} 
										else {
											if (checkIntersectWordDown(i, j, unplacedWord, placedWord,
													placedWordStart)) {
												placedWords.add(unplacedWord);
												placedWordStart.put(unplacedWord, placeIntersectWordDown(i, j,
														unplacedWord, placedWord, placedWordStart));
												break;
											}
										}
									}
								}
								if (placedWords.contains(unplacedWord)) {
									break;
								}
							}
							if (placedWords.contains(unplacedWord)) {
								break;
							}
						}
					}
					if (!placedWords.contains(unplacedWord)) {
						unplacedWords.add(unplacedWord);
					}
				}
				if (unplacedWords.isEmpty()) {
					break;
				}
			}
			if (unplacedWords.isEmpty()) {
				break;
			}
			count += 1;
		}

		if (!unplacedWords.isEmpty()) {
			System.out.println("Randomizer activated");
			for (String unplacedWord : unplacedWords) {
				while (!placedWords.contains(unplacedWord)) {
					Random rand = new Random();
					int randRowIndex = rand.nextInt(gameBoard.length);
					int randColIndex = rand.nextInt(gameBoard[0].length);
					if (gameData.get(unplacedWord)[0].equals("ACROSS")) {
						if (checkWordAcross(randRowIndex, randColIndex, unplacedWord)) {
							placedWords.add(unplacedWord);
							placedWordStart.put(unplacedWord,
									placeWordAcross(randRowIndex, randColIndex, unplacedWord));
						}
					} 
					else {
						if (checkWordDown(randRowIndex, randColIndex, unplacedWord)) {
							placedWords.add(unplacedWord);
							placedWordStart.put(unplacedWord, placeWordDown(randRowIndex, randColIndex, unplacedWord));
						}
					}
				}
			}
		}
	}

	private boolean checkWordAcross(int row, int col, String word) {
		if (col + word.length() - 1 >= gameBoard[0].length) {
			return false;
		}
		for (int i = 0; i < word.length(); i++) {
			if (!gameBoard[row][col + i].equals("   ")) {
				return false;
			}
		}
		return true;
	}

	private boolean checkWordDown(int row, int col, String word) {
		if (row + word.length() - 1 >= gameBoard.length) {
			return false;
		}
		for (int i = 0; i < word.length(); i++) {
			if (!gameBoard[row + i][col].equals("   ")) {
				return false;
			}
		}
		return true;
	}

	private int[] placeWordAcross(int row, int col, String word) {
		for (int i = 0; i < word.length(); i++) {
			if (i == 0) {
				gameBoard[row][col + i] = gameData.get(word)[1] + "_ ";
			} else {
				gameBoard[row][col + i] = " _ ";
			}
		}
		int[] returnVal = { row, col };
		return returnVal;
	}

	public synchronized void placeAnswerAcross(int row, int col, String word) {
		for (int i = 0; i < word.length(); i++) {
			if (i == 0) {
				gameBoard[row][col + i] = gameData.get(word)[1] + Character.toUpperCase(word.charAt(i)) + " ";
			} else {
				gameBoard[row][col + i] = " " + Character.toUpperCase(word.charAt(i)) + " ";
			}
		}
	}
	
 	private int[] placeWordDown(int row, int col, String word) {
		for (int i = 0; i < word.length(); i++) {
			if (i == 0) {
				gameBoard[row + i][col] = gameData.get(word)[1] + "_ ";
			} else {
				gameBoard[row + i][col] = " _ ";
			}
		}
		int[] returnVal = { row, col };
		return returnVal;
	}
 	
 	public synchronized void placeAnswerDown(int row, int col, String word) {
 		for (int i = 0; i < word.length(); i++) {
			if (i == 0) {
				gameBoard[row + i][col] = gameData.get(word)[1] + Character.toUpperCase(word.charAt(i)) + " ";
			} else {
				gameBoard[row + i][col] = " " + Character.toUpperCase(word.charAt(i)) + " ";
			}
		}
	}

	private boolean checkWordAcrossHead(int row, int col, String word) {
		if (col + word.length() - 1 >= gameBoard[0].length) {
			return false;
		}
		for (int i = 1; i < word.length(); i++) {
			if (!gameBoard[row][col + i].equals("   ")) {
				return false;
			}
		}
		return true;
	}

	private boolean checkWordDownHead(int row, int col, String word) {
		if (row + word.length() - 1 >= gameBoard.length) {
			return false;
		}
		for (int i = 1; i < word.length(); i++) {
			if (!gameBoard[row + i][col].equals("   ")) {
				return false;
			}
		}
		return true;
	}

	private int[] placeWordAcrossHead(int row, int col, String word) {
		for (int i = 1; i < word.length(); i++) {
			gameBoard[row][col + i] = " _ ";
		}
		int[] returnVal = { row, col };
		return returnVal;
	}

	private int[] placeWordDownHead(int row, int col, String word) {
		for (int i = 1; i < word.length(); i++) {
			gameBoard[row + i][col] = " _ ";
		}
		int[] returnVal = { row, col };
		return returnVal;
	}

	private boolean checkIntersectWordAcross(int unplacedCharIndex, int placedCharIndex, String unplacedWord,
			String placedWord, Map<String, int[]> placedWordStart) {
		int intersectRow = placedWordStart.get(placedWord)[0];
		int intersectCol = placedWordStart.get(placedWord)[1];

		intersectRow += placedCharIndex;

		if (intersectCol - unplacedCharIndex < 0
				|| intersectCol + unplacedWord.length() - unplacedCharIndex - 1 >= gameBoard[0].length) {
			return false;
		}

		for (int i = unplacedCharIndex; i > 0; --i) {
			if (!gameBoard[intersectRow][intersectCol - i].equals("   ")) {
				return false;
			}
		}

		for (int i = 1; i < unplacedWord.length() - unplacedCharIndex; ++i) {
			if (!gameBoard[intersectRow][intersectCol + i].equals("   ")) {
				return false;
			}
		}
		return true;
	}

	private boolean checkIntersectWordDown(int unplacedCharIndex, int placedCharIndex, String unplacedWord,
			String placedWord, Map<String, int[]> placedWordStart) {
		int intersectRow = placedWordStart.get(placedWord)[0];
		int intersectCol = placedWordStart.get(placedWord)[1];

		intersectCol += placedCharIndex;

		if (intersectRow - unplacedCharIndex < 0
				|| intersectRow + unplacedWord.length() - unplacedCharIndex - 1 >= gameBoard.length) {
			return false;
		}

		for (int i = unplacedCharIndex; i > 0; --i) {
			if (!gameBoard[intersectRow - i][intersectCol].equals("   ")) {
				return false;
			}
		}

		for (int i = 1; i < unplacedWord.length() - unplacedCharIndex; ++i) {
			if (!gameBoard[intersectRow + i][intersectCol].equals("   ")) {
				return false;
			}
		}
		return true;
	}

	private int[] placeIntersectWordAcross(int unplacedCharIndex, int placedCharIndex, String unplacedWord,
			String placedWord, Map<String, int[]> placedWordStart) {
		int intersectRow = placedWordStart.get(placedWord)[0];
		int intersectCol = placedWordStart.get(placedWord)[1];

		intersectRow += placedCharIndex;

		for (int i = unplacedCharIndex; i > 0; --i) {
			if (i == unplacedCharIndex) {
				gameBoard[intersectRow][intersectCol - i] = gameData.get(unplacedWord)[1] + "_ ";
			} else {
				gameBoard[intersectRow][intersectCol - i] = " _ ";
			}
		}

		for (int i = 1; i < unplacedWord.length() - unplacedCharIndex; ++i) {
			gameBoard[intersectRow][intersectCol + i] = " _ ";
		}

		int[] returnVal = { intersectRow, intersectCol - unplacedCharIndex };
		return returnVal;
	}

	private int[] placeIntersectWordDown(int unplacedCharIndex, int placedCharIndex, String unplacedWord,
		String placedWord, Map<String, int[]> placedWordStart) {
		int intersectRow = placedWordStart.get(placedWord)[0];
		int intersectCol = placedWordStart.get(placedWord)[1];

		intersectCol += placedCharIndex;

		for (int i = unplacedCharIndex; i > 0; --i) {
			if (i == unplacedCharIndex) {
				gameBoard[intersectRow - i][intersectCol] = gameData.get(unplacedWord)[1] + "_ ";
			} else {
				gameBoard[intersectRow - i][intersectCol] = " _ ";
			}
		}

		for (int i = 1; i < unplacedWord.length() - unplacedCharIndex; ++i) {
			gameBoard[intersectRow + i][intersectCol] = " _ ";
		}

		int[] returnVal = { intersectRow - unplacedCharIndex, intersectCol };
		return returnVal;
	}

	
	public String displayBoard() {
		StringBuilder boardDisplay = new StringBuilder();
		boolean[] nonEmptyRows = new boolean[gameBoard.length];
		boolean[] nonEmptyCols = new boolean[gameBoard[0].length];
		
		for (int i = 0; i < gameBoard.length; i++) {
			for (int j = 0; j < gameBoard[0].length; j++) {
				if (!gameBoard[i][j].equals("   ")) {
					nonEmptyRows[i] = true;
					nonEmptyCols[j] = true;
				}
			}
		}

		for (int i = 0; i < gameBoard.length; i++) {
			if (nonEmptyRows[i]) {
				ArrayList<String> rowDisplay = new ArrayList<>();
				for (int j = 0; j < gameBoard[0].length; j++) {
					if (nonEmptyCols[j]) {
						rowDisplay.add(gameBoard[i][j]);
					}
				}
				String rowString = rowDisplay.toString().replace("[", " ").replace("]", " ").replace(",", " ");
				boardDisplay.append(rowString).append("\n");
			}
		}
		boardDisplay.append("\n");
		boardDisplay.append(printClues(gameData));
		return boardDisplay.toString();
	}
	
	
	public String displayFinalBoard() {
		StringBuilder boardDisplay = new StringBuilder();
		boolean[] nonEmptyRows = new boolean[gameBoard.length];
		boolean[] nonEmptyCols = new boolean[gameBoard[0].length];
		
		for (int i = 0; i < gameBoard.length; i++) {
			for (int j = 0; j < gameBoard[0].length; j++) {
				if (!gameBoard[i][j].equals("   ")) {
					nonEmptyRows[i] = true;
					nonEmptyCols[j] = true;
				}
			}
		}

		for (int i = 0; i < gameBoard.length; i++) {
			if (nonEmptyRows[i]) {
				ArrayList<String> rowDisplay = new ArrayList<>();
				for (int j = 0; j < gameBoard[0].length; j++) {
					if (nonEmptyCols[j]) {
						rowDisplay.add(gameBoard[i][j]);
					}
				}
				String rowString = rowDisplay.toString().replace("[", " ").replace("]", " ").replace(",", " ");
				boardDisplay.append(rowString).append("\n");
			}
		}
		boardDisplay.append("\n");
		boardDisplay.append(savedClues);
		return boardDisplay.toString();
	}
	
	
	private String printClues(Map<String, String[]> gameData) {
		StringBuilder cluesDisplay = new StringBuilder();
		List<String[]> acrossClues = new ArrayList<>();
        List<String[]> downClues = new ArrayList<>();

        for (Map.Entry<String, String[]> entry : gameData.entrySet()) {
            String[] clueData = entry.getValue();
            String direction = clueData[0];
            if (direction.equalsIgnoreCase("Across")) {
                acrossClues.add(clueData);
            } 
            else if (direction.equalsIgnoreCase("Down")) {
                downClues.add(clueData);
            }
        }
        
        Comparator<String[]> indexComparator = Comparator.comparingInt(clue -> Integer.parseInt(clue[1]));
        Collections.sort(acrossClues, indexComparator);
        Collections.sort(downClues, indexComparator);
        
        if (!acrossClues.isEmpty()) {
        	 cluesDisplay.append("Across\n");
        }
        for (String[] clue : acrossClues) {
        	 cluesDisplay.append(clue[1] + " " + clue[2] + "\n");
        }
        
        if (!downClues.isEmpty()) {
       	 cluesDisplay.append("Down\n");
        }
        for (String[] clue : downClues) {
            cluesDisplay.append(clue[1] + " " + clue[2] + "\n");
        }
        
        if (savedClues == null) {
        	savedClues = cluesDisplay.toString();
        }
	    return cluesDisplay.toString();
	}
	
	
	public void sendBoard() {
		System.out.println("Sending game board.\n");
		broadcast("BOARD_START\n" + displayBoard() + "\nBOARD_END", null);
	}
	
	
	public void sendFinalBoard() {
		System.out.println("Sending game board.\n");
		broadcast("BOARD_START\n" + displayFinalBoard() + "\nBOARD_END", null);
	}
	
	
	public void printScores() {
		int maxScore = 0;
	    List<Integer> winners = new ArrayList<>();
	    
	    broadcast("Final Score", null);
	    for (ServerThread thread : serverThreads) {
	        int playerScore = thread.getPlayerScore();
	        int playerIndex = thread.getPlayerIndex() + 1;
	        broadcast("Player " + playerIndex + " – " + playerScore + " correct answers.", null);
	        
	        if (playerScore > maxScore) {
	            maxScore = playerScore;
	            winners.clear();
	            winners.add(playerIndex);
	        } 
	        else if (playerScore == maxScore) {
	            winners.add(playerIndex);
	        }
	    }
	    broadcast("\n", null);
	    
	    if (winners.size() == 1) {
	        broadcast("Player " + winners.get(0) + " is the winner.", null);
	    } 
	    else {
	    	StringBuilder tieMessage = new StringBuilder("There is a tie between ");
	        for (int i = 0; i < winners.size(); i++) {
	        	tieMessage.append("Player ").append(winners.get(i));
	            if (i < winners.size() - 1) {
	            	tieMessage.append(" and ");
	            }
	        }
	        tieMessage.append(".");
	        broadcast(tieMessage.toString(), null);
	    }
	}
	
	public void resetServer() {
		serverThreads.clear();
		gameData.clear();
		myWords.clear();
		connectedPlayers = 0;
		totalPlayers = 0;
		gameBoard = new String[21][21];
		savedClues = null;
		placedWordStart.clear();
		currentTurnIndex = new AtomicInteger(0);
		gameStarted = false;
	}
	
	public static void main(String[] args) {
		new Server(3456);
	}
}

class ServerThread extends Thread {
	private PrintWriter pw;
	private BufferedReader br;
	private Server server;
	private boolean isFirstPlayer = false;
	private Socket s;
	private int playerIndex;
	private int playerScore;

	public ServerThread(Socket s, Server server) {
		try {
			this.server = server;
			playerIndex = server.getConnectedPlayers() - 1;
			this.s = s;
			pw = new PrintWriter(s.getOutputStream(), true);
			br = new BufferedReader(new InputStreamReader(s.getInputStream()));
			playerScore = 0;

			if (server.getConnectedPlayers() == 1) {
				isFirstPlayer = true;
			}
			this.start();
		} catch (IOException ioe) {
			System.out.println("IO Error in ServerThread constructor.");
		}
	}
	
	public int getPlayerIndex() {
		return playerIndex;
	}

	
	public void setPlayerIndex(int playerIndex) {
		this.playerIndex = playerIndex;
	}

	
	public int getPlayerScore() {
		return playerScore;
	}

	
	public void setPlayerScore(int playerScore) {
		this.playerScore = playerScore;
	}
	

	
	private void receivePlayerCount() {
		int totalCount = 0;
		while (totalCount < 1 || totalCount > 3) {
			sendMessage("How many players will there be?");
			try {
				String playerCountStr = br.readLine();
				if (playerCountStr != null) {
					totalCount = Integer.parseInt(playerCountStr);
					server.setTotalPlayers(totalCount);
				}
			} catch (IOException | NumberFormatException e) {
				continue;
			}
		}
	}
	
	
	
	private String promptDir() {
		String dir = null;
		boolean acrossPresent = false;
		boolean downPresent = false;
		
		for (Map.Entry<String, String[]> entry : server.getGameData().entrySet()) {
			if (entry.getValue()[0].equals("ACROSS")) {
				acrossPresent = true;
			}
			if (entry.getValue()[0].equals("DOWN")) {
				downPresent = true;
			}
			if (acrossPresent && downPresent) {
				break;
			}
		}
		
		while (true) {
			sendMessage("Would you like to answer a question across (a) or down (d)?");
			try {
				dir = br.readLine();
				if (dir.equals("a") && acrossPresent) {
					return dir;
				}
				else if (dir.equals("d") && downPresent) {
					return dir;
				}
				sendMessage("That is not a valid option.");
			} catch (IOException e) {
				sendMessage("That is not a valid option.");
				continue;
			}
		}
	}
	
	
	private String promptIndex(String ansDir) {
		String index = null;
		if (ansDir.equals("a")) {
			ansDir = "ACROSS";
		}
		else {
			ansDir = "DOWN";
		}
		while (true) {
			sendMessage("Which number?");
			try {
				index = br.readLine();
				Integer.parseInt(index);
				for (Map.Entry<String, String[]> entry : server.getGameData().entrySet()) {
					if (entry.getValue()[0].equals(ansDir) && entry.getValue()[1].equals(index)) {
						return index;
					}
				}
				sendMessage("That is not a valid option.");
			} catch (NumberFormatException | IOException e) {
				sendMessage("That is not a valid option.");
				continue;
			}
		}
	}
	
	
	private boolean checkGuess(String guess, String ansDir, String ansIndex) {
		boolean correct = false;
		String word = null;
		Map<String, String[]> tempMap = server.getGameData();
		for (Map.Entry<String, String[]> entry : tempMap.entrySet()) {
			word = entry.getKey();
			if (word.toLowerCase().equals(guess.toLowerCase()) && ansDir.toUpperCase().equals(entry.getValue()[0]) && ansIndex.equals(entry.getValue()[1])) {
				correct = true;
				if (entry.getValue()[0].equals("ACROSS")) {
					server.placeAnswerAcross(server.getPlacedWordStart().get(word)[0], server.getPlacedWordStart().get(word)[1], word);
				}
				else {
					server.placeAnswerDown(server.getPlacedWordStart().get(word)[0], server.getPlacedWordStart().get(word)[1], word);
				}
				tempMap.remove(word);
				server.setGameData(tempMap);
				this.playerScore++;
				return correct;
			}
		}
		return correct;
	}

	public void sendMessage(String message) {
		pw.println(message);
	}

	public void run() {
		synchronized (server) {
			if (isFirstPlayer) {
				receivePlayerCount();
				System.out.println("Number of players: " + server.getTotalPlayers());
			}

			if (!isFirstPlayer) {
				sendMessage("There is a game waiting for you.");
				for (int i = 1; i < server.getConnectedPlayers(); ++i) {
					sendMessage("Player " + i + " has already joined.");
				}
				server.broadcast("Player " + server.getConnectedPlayers() + " has joined from "
						+ s.getInetAddress().getHostAddress() + ".", this);
			}

			if (server.getConnectedPlayers() < server.getTotalPlayers()) {
				System.out.println("Waiting for player " + (server.getConnectedPlayers() + 1) + ".");
				server.broadcast("\nWaiting for player " + (server.getConnectedPlayers() + 1) + ".\n", null);
				server.startGame();
				try {
					server.wait();
				} catch (InterruptedException ie) {
					System.out.println("InterruptedException on player waiting for other players to join");
				}
			}
			if (server.getConnectedPlayers() == server.getTotalPlayers() && server.getGameData().isEmpty() && (playerIndex + 1) == server.getTotalPlayers()) {
				server.startGame();
			}
			if (server.getConnectedPlayers() == server.getTotalPlayers() && !server.getGameData().isEmpty() && (playerIndex + 1) == server.getTotalPlayers()) {
				server.setGameStarted(true);
				System.out.println("\nGame can now begin.\n");
				server.placeWordsOnBoard();
				server.broadcast("\nThe game is beginning.\n", null);
				server.notifyAll();
			}
			else if (server.getGameData().isEmpty()) {
				if ((playerIndex + 1) == server.getTotalPlayers()) {
					System.out.println("Game over. All players joined, but no valid game file.\n");
					server.broadcast("\nGame cannot start due to lack of valid game file.", null);
					server.broadcast("Terminate client.", null);
					server.resetServer();
					System.out.println("Waiting for players...");
					server.notifyAll();
				}
				return;
			}
		}

		while (!server.getGameData().isEmpty()) {
			synchronized (server) {
				if (server.getCurrentTurnIndex() == playerIndex) {
					server.sendBoard();
					System.out.println("Player " + (playerIndex + 1) + "'s turn.\n");
					server.broadcast("Player " + (playerIndex + 1) + "'s turn.\n", this);
	
					String ansDir = promptDir();
					String ansIndex = promptIndex(ansDir);
					if (ansDir.equals("a")) {
						ansDir = "across";
					}
					else {
						ansDir = "down";
					}
					sendMessage("What is your guess for " + ansIndex + " " + ansDir + "?");
					String guess = null;
					try {
						guess = br.readLine();
					} catch (IOException ie) {
						System.out.println("IO Error when reading player's guess.");
					}
					System.out.println("Player " + (playerIndex + 1) + " guessed \"" + guess + "\" for " + ansIndex + " "
							+ ansDir + ".");
					server.broadcast("Player " + (playerIndex + 1) + " guessed \"" + guess + "\" for " + ansIndex + " "
							+ ansDir + ".", this);
	
					boolean correct = checkGuess(guess, ansDir, ansIndex);
	
					if (correct) {
						System.out.println("\nThat is correct.\n");
						server.broadcast("\nThat is correct.\n", null);
					} 
					else {
						System.out.println("\nThat is incorrect.\n");
						server.broadcast("\nThat is incorrect.\n", null);
						server.nextTurn();
					}
					server.notifyAll();
				}
				else {
					try {
						server.wait();
					} catch (InterruptedException ie) {
						System.out.println("InterruptedException on player waiting for turn");
					}
				}
			}
		}
		
		if (playerIndex == 0) {
			server.sendFinalBoard();
			System.out.println("The game has concluded.");
			System.out.println("Sending scores.\n");
			server.printScores();
			server.broadcast("Terminate client.", null);
			server.resetServer();
			System.out.println("Waiting for players...");
		}
	}
}

class Game extends Thread {

	private Map<String, String[]> gameData;
	private List<String> words;
	private Set<File> invalidFiles;

	public Game() {
		gameData = new HashMap<String, String[]>();
		words = new ArrayList<String>();
		invalidFiles = new HashSet<File>();
	}

	public static List<File> scanGameDataFiles(String folderPath) {
		File folder = new File(folderPath);

		FilenameFilter csvFilter = (dir, name) -> name.toLowerCase().endsWith(".csv");
		File[] csvFiles = folder.listFiles(csvFilter);

		if (csvFiles != null) {
			return Arrays.asList(csvFiles);
		} 
		else {
			return Collections.emptyList();
		}
	}

	public boolean parseGameFile(File file) {
		System.out.println("Reading random game file.");
		Set<String> acrossNums = new HashSet<>();
		Set<String> downNums = new HashSet<>();
		boolean acrossFound = false;
		boolean downFound = false;
		String currDirection = "";

		try (Scanner scanner = new Scanner(file)) {
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine().trim();
				if (line.isEmpty()) {
					continue;
				}

				if (line.equals("ACROSS,,")) {
					if (acrossFound) {
						System.out.println("Invalid file " + file.getName() + ": ACROSS section found more than once.");
						invalidFiles.add(file);
						gameData.clear();
						words.clear();
						return false;
					}
					acrossFound = true;
					currDirection = "ACROSS";
					continue;
				} 
				else if (line.equals("DOWN,,")) {
					if (downFound) {
						System.out.println("Invalid file " + file.getName() + ": DOWN section found more than once.");
						invalidFiles.add(file);
						gameData.clear();
						words.clear();
						return false;
					}
					downFound = true;
					currDirection = "DOWN";
					continue;
				}

				String[] entry = line.split(",");

				if (entry.length != 3) {
					System.out.println("Invalid file " + file.getName() + ": Entry with missing/excess parameters");
					invalidFiles.add(file);
					gameData.clear();
					words.clear();
					return false;
				}

				String index = entry[0].trim();
				String word = entry[1].trim();
				String question = entry[2].trim();

				if (!isInteger(index)) {
					System.out.println("Invalid file " + file.getName() + ": Index is not an integer: " + index);
					invalidFiles.add(file);
					gameData.clear();
					words.clear();
					return false;
				}
				if (word.contains(" ")) {
					System.out.println("Invalid file " + file.getName() + ": Word contains whitespace: " + word);
					invalidFiles.add(file);
					gameData.clear();
					words.clear();
					return false;
				}
				if (!question.contains(" ")) {
					System.out.println(
							"Invalid file " + file.getName() + ": Question doesn't contain whitespace: " + question);
					invalidFiles.add(file);
					gameData.clear();
					words.clear();
					return false;
				}

				if (currDirection.equals("ACROSS") || currDirection.equals("DOWN")) {
					String[] formattedEntry = { currDirection, index, question};
					gameData.put(word, formattedEntry);
					words.add(word);
				} 
				else {
					System.out.println("Invalid file " + file.getName() + ":  No direction given for word: " + word);
					invalidFiles.add(file);
					gameData.clear();
					words.clear();
					return false;
				}

				if (currDirection.equals("ACROSS")) {
					acrossNums.add(index);
				} 
				else if (currDirection.equals("DOWN")) {
					downNums.add(index);
				}
			}

			for (String num : acrossNums) {
				if (downNums.contains(num)) {
					String acrossWord = getWordForIndex(num, "ACROSS");
					String downWord = getWordForIndex(num, "DOWN");

					if (Character.toLowerCase(acrossWord.charAt(0)) != Character.toLowerCase(downWord.charAt(0))) {
						System.out.println("Invalid file " + file.getName() + ": Words for index " + num + ": " + acrossWord
								+ " and " + downWord + " do not start with the same letter. ");
						invalidFiles.add(file);
						gameData.clear();
						words.clear();
						return false;
					}
				}
			}

			if (!acrossFound) {
				System.out.println("Invalid file " + file.getName() + ": ACROSS section not found.");
				invalidFiles.add(file);
				gameData.clear();
				words.clear();
				return false;
			}
			if (!downFound) {
				System.out.println("Invalid file " + file.getName() + ": DOWN section not found.");
				invalidFiles.add(file);
				gameData.clear();
				words.clear();
				return false;
			}

		} catch (FileNotFoundException fnfe) {
			System.out.println("Error: File " + file.getName() + " not found.");
			return false;
		}
		System.out.println("File read successfully.");
		return true;
	}

	private boolean isInteger(String str) {
		try {
			Integer.parseInt(str);
			return true;
		} catch (NumberFormatException nfe) {
			return false;
		}
	}

	private String getWordForIndex(String number, String direction) {
		for (Map.Entry<String, String[]> entry : gameData.entrySet()) {
			String[] value = entry.getValue();
			if (value[0].equals(direction) && value[1].equals(number)) {
				return entry.getKey();
			}
		}
		return "";
	}

	public Map<String, String[]> getGameData() {
		return gameData;
	}

	public List<String> getWords() {
		return words;
	}

	@Override
	public void run() {
		List<File> files = new ArrayList<>(scanGameDataFiles("gamedata"));
		if (files.isEmpty()) {
			System.out.println("No CSV files found in gamedata folder.");
			return;
		}
		Random random = new Random();
		File randFile = files.get(random.nextInt(files.size()));
		boolean parseSuccess = parseGameFile(randFile);
		while (!parseSuccess) {
			Iterator<File> iterator = files.iterator();
			while (iterator.hasNext()) {
				File file = iterator.next();
				if (invalidFiles.contains(file)) {
					iterator.remove();
				}
			}
			if (files.isEmpty()) {
				System.out.println("No valid files in gamedata folder.");
				return;
			} 
			else {
				randFile = files.get(random.nextInt(files.size()));
				parseSuccess = parseGameFile(randFile);
			}
		}
	}
}
