import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Random;

class Server {
    public static void main(String argv[]) {
        try (ServerSocket server_socket = new ServerSocket(2624)) {

            int INITIAL_BANKROLL = 100;

            // create card deck
            char[] card_rank = { '2', '3', '4', '5', '6', '7', '8', '9', 'T', 'J', 'Q', 'K', 'A' };
            char[] card_suit = { 'S', 'H', 'D', 'C' };
            String[] deck = new String[52];
            Short i = 0;

            for (char rank : card_rank) {
                for (char suit : card_suit) {
                    deck[i] = new StringBuilder().append(rank).append(suit).toString();
                    i++;
                }
            }

            while (true) {
                System.out.println("[main] Server waiting for connection");

                Socket client_socket = server_socket.accept();
                Player client_thread = new Player(client_socket, deck, INITIAL_BANKROLL);
                client_thread.start();

                System.out.println("[main] Client thread started");
            }
        } catch (IOException error) {
            System.err.println("Network error: " + error.getMessage());
        }
    }
}

class Player extends Thread {
    Socket client_socket;
    String[] player_deck;
    int player_bankroll = 100;

    Player(Socket _s, String[] deck, int bankroll) {
        client_socket = _s;
        player_deck = shuffle_deck(deck);
        player_bankroll = bankroll;
    }

    @Override
    public void run() {
        try {
            OutputStream out = client_socket.getOutputStream();
            InputStream in = client_socket.getInputStream();
            byte msg[] = new byte[64];

            // Initial bank response
            out.write(bank_response(player_bankroll).getBytes());

            while (true) {
                int msg_len = in.read(msg);
                System.out.println("Server msg buffer after read: " + new String(msg).replace('\0', '.'));
                if (msg_len <= 0)
                    break; // received nothing
                System.out.println("Message received by server: " + new String(msg, 0, msg_len));
                out.write(("Hi client, you send me: " + new String(msg, 0, msg_len)).getBytes());
                out.flush();
            }
            client_socket.close();
            System.out.println("Client thread closed");
        } catch (Exception e) {
            System.err.println("Worker issue " + e);
        }
    }

    static String bank_response(int bankroll) {
        return new String("BANKROLL " + bankroll + "\r\n\r\n");
    }

    /**
     * @param deck
     * @return deck shuffled with Durstenfeld algorithm
     */
    static String[] shuffle_deck(String[] deck) {
        int index;
        String temp;
        Random random = new Random();
        for (int i = deck.length - 1; i > 0; i--) {
            index = random.nextInt(i + 1); // nextInt(int bound) with bound exclusive, 0<=index<=i
            if (index != i) {
                temp = deck[i];
                deck[i] = deck[index];
                deck[index] = temp;
            }
        }
        return deck;
    }

}
