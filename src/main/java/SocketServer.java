import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by FomeIV on 26.04.2016.
 * Сервер запускает qualThreads потоков обработчиков и ждет соединения на PORT порту
 * Клиент соединяется с сервером и посылает ему набор символов. В ответ получет этот же набор символов.
 * Если клиент посылает STOP, то сервер останавливает все потоки и останавливается сам.
 */
public class SocketServer {
    private static final int qualThreads = 20;
    private static final int PORT = 5050;
    private static final String STOP = "Bue";

    private static boolean stopFlag = false;
    private static Map<Integer, ConcurrentLinkedQueue<Socket>> msg = new HashMap<>();

    public static void main(String[] args) throws IOException {
        List<Thread> threads = new LinkedList<>();
        for (int i = 0; i < qualThreads; i++) {
            int finalI = i;
            Thread r = new Thread() {
                private final int threadNum = finalI;

                @Override
                public void run() {
                    String line;
                    while (!stopFlag) {
                        ConcurrentLinkedQueue<Socket> queue = msg.get(threadNum);
                        Socket s = queue.poll();
                        if (s != null) {
                            try {
                                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                                PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream())), true);
                                while (!stopFlag && (line = in.readLine()) != null) {
                                    if (line.equals(STOP)) {
                                        stopFlag = true;
                                    }
//                                    System.out.println(this.getName() + " " + line);
                                    out.println(line);
                                }
                                s.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                }
            };
            r.start();
            threads.add(r);
            msg.put(i, new ConcurrentLinkedQueue<>());
        }
        ServerSocket serv = new ServerSocket(PORT);
        Random rn = new Random();
        serv.setSoTimeout(1000);
        System.out.println("Server started");
        while (!stopFlag) {
            try {
                Socket s = serv.accept();
                ConcurrentLinkedQueue<Socket> q = msg.get(rn.nextInt(qualThreads));
                q.add(s);
            } catch (SocketTimeoutException ignored) {

            }
        }
        threads.forEach(Thread::interrupt);
    }
}
