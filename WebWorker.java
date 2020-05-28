import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

public class WebWorker extends Thread {
    String url;
    int rowToBeUpdated;
    WebFrame frame;
    CountDownLatch latch;
    long startTime, endTime;

    public WebWorker(String url, int rowToBeUpdated, WebFrame frame, CountDownLatch latch){
        this.url = url;
        this.rowToBeUpdated = rowToBeUpdated;
        this.frame = frame;
        this.latch = latch;
    }

    public void run(){
        frame.runningThreadsNum++;
        try {
            download();
        } catch (IOException e) {
            e.printStackTrace();
        }
        frame.runningThreadsNum--;
        frame.completedThreadsNum++;
        frame.progressBar.setValue(frame.completedThreadsNum);
        frame.updateLabels();
        frame.canStartWorking.release();
        latch.countDown();
    }

    private void download() throws IOException {
        startTime = System.currentTimeMillis();
        InputStream input = null;
        StringBuilder contents;
        try {
            URL url = new URL(this.url);
            URLConnection connection = url.openConnection();

            // Set connect() to throw an IOException
            // if connection does not succeed in this many msecs.
            connection.setConnectTimeout(5000);

            connection.connect();
            input = connection.getInputStream();

            BufferedReader reader  = new BufferedReader(new InputStreamReader(input));

            char[] array = new char[1000];
            int len;
            contents = new StringBuilder(1000);
            while ((len = reader.read(array, 0, array.length)) > 0) {
                contents.append(array, 0, len);
                Thread.sleep(100);
            }
            endTime = System.currentTimeMillis();
            // Successful download if we get here
            if(isInterrupted()) {
                updateTable("Interrupted");
            }
            else {
                String date = new SimpleDateFormat("hh:mm:ss ").format(new Date());
                double elapsedTime = (double)(endTime - startTime) / 1000;
                int bytes = contents.length();
                updateTable(date + elapsedTime + " ms " + bytes + " bytes ");
            }
        }
        // Otherwise control jumps to a catch...
        catch(IOException ignored) { updateTable("error"); }
        catch(InterruptedException exception) { updateTable("Interrupted"); }
        // "finally" clause, to close the input stream
        // in any case
        finally {
            if (input != null) input.close();
        }
    }

    public void updateTable(String input){
        frame.model.setValueAt(input, rowToBeUpdated, 1);
    }
	
}
