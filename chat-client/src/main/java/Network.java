import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Network implements AutoCloseable {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Callback callback;

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void connect(int port) throws IOException {
        socket = new Socket("localhost", port);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        new Thread(() -> {
            try {
                while (true) {
                    // /exit -> disconnect()
                    // /w user message  -> user
                    String message = in.readUTF();
                    if (callback != null) {
                        callback.call(message);
                    }
                }
            } catch (IOException e) {
                callback.call("Disconnect");
                close();
//                throw new RuntimeException(e);
            } finally {
                try {
                    close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    @Override
    public void close()  {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        System.exit(0);
    }

    public void sendMessage(String msg) throws IOException {
            out.writeUTF(msg);
    }
}
