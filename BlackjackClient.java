import java.io.*;
import java.net.*;
import java.util.Scanner;

class BlackjackClient {

    public static void main(String[] argv) {
        try (
            Socket socket = new Socket("localhost", 2624);
            Scanner input = new Scanner(System.in)
        ) {
            socket.setSoTimeout(300000);
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            InputReader inputReader = new InputReader(in, true);

            String conversationString;
            String[] conversation;
            // try {
            //     conversation = inputReader.ReadLine();
            // } catch (SocketTimeoutException e) {
            //     System.err.println("Server takes too long too respond. Quit game");
            //     return;
            // } catch (IOException e) {
            //     System.err.println("Response abnormally long. Quit game");
            //     return;
            // }
            // if (inputReader.stream_end) return;

            // if (conversation[0].equals("BANKROLL")) 
            //     System.out.println("\r\nConnected. You start with " + conversation[1].replaceAll("(\\r\\n)+$", "") + " chips.\n");
            // else
                System.out.println("\n Connected.\n");
            print_help();

            while (true) {
                // --- CLIENT ACTION ---
                // treat input
                while (true) {
                    System.out.print("> ");
                    conversationString = input
                        .nextLine()
                        .trim()
                        .replaceAll(" {2,}", "")
                        .toLowerCase();
                    conversation = conversationString.split(" ");

                    if (!conversation[0].equals("help")) break;

                    print_help();
                }

                if (conversation[0].equals("quit")) {
                    break;
                }

                conversationString = conversationString.toUpperCase();
                out.write((conversationString + "\r\n\r\n").getBytes());
                out.flush();

                // --- SERVER RESPOND WITH GAME STATUS ---
                try {
                    conversation = inputReader.ReadLine();
                } catch (SocketTimeoutException e) {
                    System.err.println("Server takes too long too respond or response abnormally long. Quit game");
                    break;
                }
                if (inputReader.stream_end) break;

                conversationString = String.join(" ", conversation);

                // --- CLIENT UPDATE INFO (PRINT) ---
                System.out.print(conversationString);
                if (conversationString.contains("GAME OVER")) break;

            }
        } catch (UnknownHostException error) {
            System.err.println("Unkown host: " + error.getMessage());
        } catch (IOException error) {
            System.err.println("Network error: " + error.getMessage());
        }
    }

    static void print_help() {
        System.out.println(
            """
            \033[4mCommands:\033[0m
            bet <amount>        place a bet and start a new round
            hit                 draw an additional card
            stand               end your turn and let the dealer play
            bank                check your current bankroll
            quit                leave the table
            help                display this help message
            """
        );
    }
}
