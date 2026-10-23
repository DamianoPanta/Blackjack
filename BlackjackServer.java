import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

class BlackjackServer {

    public static void main(String[] argv) {
        try (ServerSocket server_socket = new ServerSocket(2624)) {
            while (true) {

                Socket client_socket = server_socket.accept();
                Game client_thread = new Game(
                        client_socket);
                client_thread.start();

            }
        } catch (IOException error) {
            System.err.print("Network error: " + error.getMessage());
        }
    }
}

class Game extends Thread {

    final int INITIAL_BANKROLL = 100;
    Socket client_socket;
    OutputStream out;
    InputStream in;
    String[] playerDeck;
    DeckManager deckManager;
    int playerBankroll = INITIAL_BANKROLL;
    String responseToClient = new String();
    boolean inRound = false;
    boolean gameOver = false;
    Round round;

    Game(Socket _s) {
        try {
            client_socket = _s;
            client_socket.setSoTimeout(300000); // 5 minutes
            out = client_socket.getOutputStream();
            in = client_socket.getInputStream();

            playerDeck = shuffleDeck(createDeck());
            deckManager = new DeckManager(playerDeck);

        } catch (Exception e) {
            System.err.println("Error creating player " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            // Initial message to send initial bankroll
            // addBankResponse();
            // sendResponse();

            InputReader inputReader = new InputReader(in, false);
            String[] inputString;
            int amount;

            while (!gameOver) {
                try {
                    inputString = inputReader.ReadLine();
                } catch (SocketTimeoutException e) {
                    System.err.println("Client takes too long too respond. Close client socket");
                    break;
                } catch (IOException e) {
                    System.err.println("Request abnormally long. Close client socket");
                    break;
                }
                if (inputReader.stream_end)
                    break;

                if (inputString[0].equals("BET")) {
                    if (inputString.length < 2) {
                        out.write("INVALID BET\r\n\r\n".getBytes());
                        continue;
                    }

                    String token = inputString[1];
                    if (!token.matches("[1-9][0-9]*")) {
                        out.write("INVALID BET\r\n\r\n".getBytes());
                        continue;
                    }

                    // 10^9 < Integer.MAX_VALUE < 10^10
                    if (token.length() > 9 || Integer.parseInt(token) > playerBankroll) {
                        out.write("INSUFFICIENT FUNDS\r\n\r\n".getBytes());
                        continue;
                    }

                    if (inRound) {
                        out.write("ROUND IN PROGRESS\r\n\r\n".getBytes());
                        continue;
                    }

                    amount = Integer.parseInt(token);
                    playerBankroll -= amount;
                    round = new Round(deckManager, amount);
                    inRound = true;
                    handleRoundOutcome(round, round.start());
                } else if (inputString[0].equals("HIT")) {
                    if (!inRound) {
                        out.write("NO ACTIVE ROUND\r\n\r\n".getBytes());
                        continue;
                    } else {
                        handleRoundOutcome(round, round.hit());
                    }
                } else if (inputString[0].equals("STAND")) {
                    if (!inRound) {
                        out.write("NO ACTIVE ROUND\r\n\r\n".getBytes());
                        continue;
                    } else
                        handleRoundOutcome(round, round.stand());
                } else if (inputString[0].equals("BANK")) {
                    addBankResponse();
                } else if (inputString[0].equals("QUIT")) {
                    gameOver = true;
                    continue;
                } else {
                    out.write("WRONG\r\n\r\n".getBytes());
                    continue;
                }

                sendResponse();
            }

            if (!client_socket.isClosed())
                client_socket.close();

        } catch (Exception e) {
            System.err.println("Player issue " + e);
        }
    }

    void handleRoundOutcome(Round round, RoundOutcome outcome) {
        addHandResponse(round);
        if (outcome != null) {
            int delta = outcome.settle(round.getBet());
            playerBankroll += delta;
            inRound = false;
            addRoundstatusResponse(round, outcome);

            if (deckManager.deck_index < 3 || playerBankroll <= 0) {
                gameOver = true;
                addGameoverResponse();
            }
        }
    }

    void sendResponse() {
        try {
            out.write((responseToClient + "\r\n").getBytes());
            out.flush();
            responseToClient = "";
        } catch (IOException e) {
            System.err.println("Error writing to socket: " + e.getMessage());
        }
    }

    void addBankResponse() {
        responseToClient += "BANKROLL " + playerBankroll + "\r\n";
    }

    void addHandResponse(Round round) {
        List<String> playerCard = round.getPlayerCard();
        List<String> dealerCard = round.getDealerCard();
        int playerTotal = round.getPlayerTotal();
        int dealerTotal = round.getDealerTotal();

        responseToClient += "PLAYER " +
                String.join(" ", playerCard) +
                " " +
                playerTotal +
                "\r\n";
        if (!round.getDealerCardVisible()) {
            String temp = dealerCard.get(1);
            dealerCard.set(1, "??");
            responseToClient += "DEALER " + String.join(" ", dealerCard) + "\r\n";
            dealerCard.set(1, temp);
        } else
            responseToClient += "DEALER " +
                    String.join(" ", dealerCard) +
                    " " +
                    dealerTotal +
                    "\r\n";
    }

    void addRoundstatusResponse(Round round, RoundOutcome outcome) {
        int bet = round.getBet();
        switch (outcome) {
            case BLACKJACK:
                responseToClient += "BLACKJACK " + (int) Math.floor(bet * 1.5) + "\r\n";
                break;
            case WIN:
                responseToClient += "WIN " + bet + "\r\n";
                break;
            case LOSE:
                responseToClient += "LOSE " + bet + "\r\n";
                break;
            case PUSH:
                responseToClient += "PUSH 0\r\n";
                break;
        }
    }

    void addGameoverResponse() {
        responseToClient += "GAME OVER\r\n";
        addBankResponse();
    }

    String[] createDeck() {
        // create card deck
        char[] card_rank = {
                '2',
                '3',
                '4',
                '5',
                '6',
                '7',
                '8',
                '9',
                'T',
                'J',
                'Q',
                'K',
                'A',
        };
        char[] card_suit = { 'S', 'H', 'D', 'C' };
        String[] deck = new String[52];
        Short index = 0;

        for (char rank : card_rank) {
            for (char suit : card_suit) {
                deck[index] = new StringBuilder()
                        .append(rank)
                        .append(suit)
                        .toString();
                index++;
            }
        }
        return deck;
    }

    String[] shuffleDeck(String[] deck) {
        int index;
        String temp;
        Random random = new Random();
        String[] player_deck = Arrays.copyOf(deck, deck.length); // need new object

        for (int i = player_deck.length - 1; i > 0; i--) {
            index = random.nextInt(i + 1); // nextInt(int bound) with bound exclusive, 0<=index<=i
            if (index != i) {
                temp = player_deck[i];
                player_deck[i] = player_deck[index];
                player_deck[index] = temp;
            }
        }
        return player_deck;
    }
}
