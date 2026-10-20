import java.io.InputStream;
import java.net.SocketTimeoutException;

class InputReader {

    InputStream in;
    boolean stream_end = false;
    private byte[] request_byte = new byte[10];
    private String request_string = new String();
    private int request_len;
    private int maxReadIteration = 10;
    private int readIteration = 0;
    boolean isClient;

    InputReader(InputStream in, boolean isClient) {
        this.in = in;
        this.isClient = isClient;
    }

    String[] ReadLine() throws SocketTimeoutException {
        request_string = "";
        readIteration = 0;
        while (!request_string.endsWith("\r\n\r\n") && readIteration <= maxReadIteration) {
            try {
                request_len = in.read(request_byte);
            } catch (Exception e) {
                System.err.println("Error reading from socket: " + e.getMessage());
            }
            if (request_len <= 0) {
                stream_end = true;
                return "".split("");
            }
            request_string += new String(request_byte, 0, request_len);
            readIteration++;
        }

        if (readIteration > maxReadIteration) {
            throw new SocketTimeoutException();
        }

        // remove special characters for server
        if (!isClient)
            request_string = request_string.replaceAll("(\\r\\n)+$", "");

        return request_string.split(" ");
    }
}
