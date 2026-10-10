import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Random;

class Server {
    public static void main(String argv[]) {
        try (ServerSocket server_socket = new ServerSocket(2624)) {

            final int INITIAL_BANKROLL = 100;

            // create card deck
            char[] card_rank = { '2', '3', '4', '5', '6', '7', '8', '9', 'T', 'J', 'Q', 'K', 'A' };
            char[] card_suit = { 'S', 'H', 'D', 'C' };
            String[] deck = new String[52];
            Short index = 0;

            for (char rank : card_rank) {
                for (char suit : card_suit) {
                    deck[index] = new StringBuilder().append(rank).append(suit).toString();
                    index++;
                }
            }

            while (true) {
                System.out.println("[main] Server waiting for connection");

                Socket client_socket = server_socket.accept();
                Game client_thread = new Game(client_socket, deck, INITIAL_BANKROLL);
                client_thread.start();

                System.out.println("[main] Client thread started");
            }
        } catch (IOException error) {
            System.err.print("Network error: " + error.getMessage());
        }
    }
}

class Game extends Thread {
    Socket client_socket;
    OutputStream out;
    InputStream in;
    String[] player_deck;
    int player_bankroll;
    String response_to_client = new String();
    boolean in_round = false;
    Round current_round;

    Game(Socket _s, String[] deck, int bankroll) {
        try {
            client_socket = _s;
            out = client_socket.getOutputStream();
            in = client_socket.getInputStream();
            player_deck = shuffle_deck(deck);
            player_bankroll = bankroll;
        } catch (Exception e) {
            System.err.println("Error creating player " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            // Initial message to send initial bankroll
            add_bank_response();
            send_response();

            InputReader input_reader = new InputReader(in);
            String[] input_string;
            int amount;

            while (true) {
                input_string = input_reader.ReadLine();
                if (input_reader.stream_end)
                    break;

                if (input_string[0].equals("BET")) {
                    if (in_round) {
                        out.write("ROUND IN PROGRESS\r\n\r\n".getBytes());
                        continue;
                    }

                    try {
                        amount = Integer.parseInt(input_string[1]);
                    } catch (NumberFormatException e) {
                        out.write("INVALID BET\r\n\r\n".getBytes());
                        continue;
                    }

                    if (amount > player_bankroll) {
                        out.write("INSUFFICIENT FUNDS\r\n\r\n".getBytes());
                        continue;
                    }

                    current_round = new Round(player_deck, player_bankroll, amount);

                }
            }

            client_socket.close();
            System.out.println("Client thread closed");
        } catch (Exception e) {
            System.err.println("Player issue " + e);
        }
    }

    void send_response() {
        try {
            out.write((response_to_client + "\r\n").getBytes());
            out.flush();
            response_to_client = "";
        } catch (IOException e) {
            System.err.println("Error writing to socket: " + e.getMessage());
        }
    }

    void add_bank_response() {
        response_to_client += "BANKROLL " + player_bankroll + "\r\n";
    }

    void add_hand_response(String[] player_card, String[] dealer_card, int player_total, int dealer_total) {
        response_to_client += "PLAYER " + String.join(" ", player_card) + " " + player_total + "\r\n";
        response_to_client += "DEALER " + String.join(" ", dealer_card) + " " + dealer_total + "\r\n";
    }

    void add_roundstatus_response(int amount, boolean blackjack) {
        if (blackjack)
            response_to_client += "BLACKJACK " + amount + "\r\n";
        else if (amount > 0)
            response_to_client += "WIN " + amount + "\r\n";
        else if (amount < 0)
            response_to_client += "LOSE " + Math.abs(amount) + "\r\n";
        else
            response_to_client += "PUSH 0\r\n";
    }

    static String[] shuffle_deck(String[] deck) {
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

// TODO: check that string concatenation is not too bad in term of efficiency
class InputReader {
    InputStream in;
    boolean stream_end = false;
    private byte[] request_byte;
    private String request_string;

    InputReader(InputStream in) {
        this.in = in;
    }

    String[] ReadLine() {
        String line = "";
        while (true) {
            try {
                request_byte = in.readNBytes(2);
            } catch (Exception e) {
                System.err.println("Error reading from socket: " + e.getMessage());
            }
            if (request_byte.length <= 0) {
                stream_end = true;
                break;
            }
            request_string = new String(request_byte);
            if (request_string.equals("\r\n"))
                break;
            line += request_string;
        }
        return line.split(" ");
    }
}

class Round {
    String[] player_card;
    String[] dealer_card;
    boolean dealer_card2_visible = false;
    boolean blackjack = true;

    Round(String[] current_deck, int current_bankroll, int bet) {

    }
}
