import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;

class InputReader {

    InputStream in;
    boolean streamEnd = false;
    private byte[] requestByte = new byte[64];
    private String requestString = new String();
    private final int MAX_REQUEST_BYTES = 256;
    boolean isClient;

    InputReader(InputStream in, boolean isClient) {
        this.in = in;
        this.isClient = isClient;
    }

    String[] ReadLine() throws SocketTimeoutException, IOException {
        requestString = "";
        int total = 0;
        int len = 0;
        while (!requestString.endsWith("\r\n\r\n")) {
            try {
                len = in.read(requestByte);
            } catch (Exception e) {
                System.err.println("Error reading from socket: " + e.getMessage());
            }
            if (len <= 0) {
                streamEnd = true;
                return new String[0];
            }
            total += len;
            if (total > MAX_REQUEST_BYTES) throw new IOException();
            requestString += new String(requestByte, 0, len);
        }

        // remove special characters for server
        if (!isClient)
            requestString = requestString.replaceAll("(\\r\\n)+$", "");

        return requestString.split(" ");
    }
}
