import java.io.*;
import java.net.*;
import java.util.Scanner;

class Client {
    public static void main(String argv[]) {
        try (Socket socket = new Socket("localhost", 2624);
                Scanner input = new Scanner(System.in)) {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            System.out.println("\n Connected.\n");
            print_help();

            byte response_buffer[] = new byte[64];
            int response_len;
            String response_string;
            String[] message;
            String[] response;
            int chips = 100;

            // TODO : implement a more robust way to detect when a message is finished
            // (delimiter, fixed size, header)

            while (true) {
                // --- SERVER RESPOND WITH GAME STATUS ---
                response_len = in.read(response_buffer);
                if (response_len <= 0) break;

                response_string = new String(response_buffer, 0, response_len);
                response = response_string.trim().replaceAll(" {2,}", "").split(" ");


                // --- CLIENT UPDATE INFO (PRINT) ---
                if (response[0].equals("BANKROLL")) {
                    chips = Integer.parseInt(response[1]);
                    System.out.println("You have currently " + chips + " chips");
                }

                // --- CLIENT ACTIOM ---
                // treat input
                System.out.print("> ");
                message = input.nextLine().trim().replaceAll(" {2,}", "").toLowerCase().split(" ");

                if (message[0].equals("quit")) {
                    int score = chips - 100;
                    System.out.println("You quit the game with a ");
                    break;
                }

                // if (!message.equals("pass")) {
                //     out.write(message.getBytes());
                //     out.flush();
                // }

            }
        } catch (UnknownHostException error) {
            System.err.println("Unkown host: " + error.getMessage());
        } catch (IOException error) {
            System.err.println("Network error: " + error.getMessage());
        }
    }

    static void print_help() {
        System.out.println("""
                \033[4mCommands:\033[0m
                bet <amount>        place a bet and start a new round
                hit                 draw an additional card
                stand               end your turn and let the dealer play
                bank                check your current bankroll
                quit                leave the table
                help                display this help message
                """);
    }
}
