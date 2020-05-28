import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

public class WebFrame extends JFrame{
    private static final String INITIAL_RUNNING_LABEL = "Running: 0";
    private static final String INITIAL_COMPLETED_LABEL = "Completed: 0";
    private static final String INITIAL_ELAPSED_LABEL = "Elapsed: 0";

    DefaultTableModel model;
    JTable table;
    JPanel tablePanel, controlPanel;
    JButton singleFetchBtn, concurrentFetchBtn, stopBtn;
    JLabel runningLbl, completedLbl, elapsedLbl;
    JTextField textField;
    JProgressBar progressBar;

    String filename;
    Semaphore canStartWorking;
    int runningThreadsNum, completedThreadsNum;
    long startTime, endTime;
    List<String> urls;
    List<WebWorker> workers;
    Launcher launcher;

    public WebFrame(String filename){
        this.filename = filename;
        this.setSize(new Dimension(650, 600));

        tablePanel = new JPanel();
        tablePanel.setLayout(new BorderLayout());
        controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));

        setUp();

        this.add(tablePanel, BorderLayout.NORTH);
        this.add(controlPanel, BorderLayout.SOUTH);

        runningThreadsNum = 0;
        completedThreadsNum = 0;
    }

    public void setUp(){
        SwingUtilities.invokeLater(() -> {
            try {
                setUpTable();
                setUpButtons();
                setUpLabels();
                setUpProgressBarAndStopBtn();
            } catch (IOException e) { e.printStackTrace(); }
        });
        setVisible(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    private void setUpTable() throws IOException {
        model = new DefaultTableModel(new String[] { "url", "status" }, 0);
        table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        readDataIntoTable(filename);
        tablePanel.add(table);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(600, 300));
        tablePanel.add(scrollPane);
    }

    private void readDataIntoTable(String filename) throws IOException {
        urls = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) { urls.add(line); }
        } catch (FileNotFoundException e) { e.printStackTrace(); }
        for (String url : urls){
            model.addRow(new Object[] { url, ""});
        }
    }

    private void setUpButtons() {
        singleFetchBtn = new JButton("Single Thread Fetch");
        singleFetchBtn.setEnabled(true);
        singleFetchBtn.addActionListener(e -> onSingleThreadFetch());
        controlPanel.add(singleFetchBtn);
        controlPanel.add(Box.createVerticalStrut(10));

        concurrentFetchBtn = new JButton("Concurrent Fetch");
        concurrentFetchBtn.setEnabled(true);
        concurrentFetchBtn.addActionListener(e -> onConcurrentThreadFetch());
        controlPanel.add(concurrentFetchBtn);
        controlPanel.add(Box.createVerticalStrut(10));

        textField = new JTextField();
        textField.setMaximumSize(new Dimension(50, 20));
        controlPanel.add(textField);
        controlPanel.add(Box.createVerticalStrut(10));
    }

    private void setUpLabels() {
        runningLbl = new JLabel(INITIAL_RUNNING_LABEL);
        controlPanel.add(runningLbl);

        completedLbl = new JLabel(INITIAL_COMPLETED_LABEL);
        controlPanel.add(completedLbl);

        elapsedLbl = new JLabel(INITIAL_ELAPSED_LABEL);
        controlPanel.add(elapsedLbl);
        controlPanel.add(Box.createVerticalStrut(10));
    }

    private void setUpProgressBarAndStopBtn() {
        progressBar = new JProgressBar(0);
        controlPanel.add(progressBar);
        controlPanel.add(Box.createVerticalStrut(10));

        stopBtn = new JButton("Stop");
        stopBtn.setEnabled(false);
        stopBtn.addActionListener(e -> onStop());
        controlPanel.add(stopBtn);
        controlPanel.add(Box.createVerticalStrut(10));
    }

    public void onSingleThreadFetch(){
        startWorking(1);
    }

    public void onConcurrentThreadFetch(){
        startWorking(Integer.parseInt(textField.getText()));
    }

    public void startWorking(int numWorkers){
        setRunningState();
        launcher = new Launcher(numWorkers);
        launcher.start();
    }

    public void onStop(){
        launcher.interrupt();
        for (WebWorker worker : workers){
            if(worker.isAlive()) worker.interrupt();
        }
        setReadyState();
    }

    public void setReadyState(){
        SwingUtilities.invokeLater(() -> {
            singleFetchBtn.setEnabled(true);
            concurrentFetchBtn.setEnabled(true);
            stopBtn.setEnabled(false);
            progressBar.setValue(0);
            textField.setEditable(true);
        });
    }

    public void resetStatusLabels(){
        runningLbl.setText(INITIAL_RUNNING_LABEL);
        completedLbl.setText(INITIAL_COMPLETED_LABEL);
        elapsedLbl.setText(INITIAL_ELAPSED_LABEL);
    }

    public void setRunningState(){
        SwingUtilities.invokeLater(() -> {
            singleFetchBtn.setEnabled(false);
            concurrentFetchBtn.setEnabled(false);
            stopBtn.setEnabled(true);
            progressBar.setMaximum(urls.size());
            textField.setEditable(false);
            resetStatusLabels();
        });
    }

    void updateLabels() {
        SwingUtilities.invokeLater(() -> {
            runningLbl.setText("Running: " + runningThreadsNum);
            completedLbl.setText("Complete: " + completedThreadsNum);
            elapsedLbl.setText("Elapsed: " + (double)(endTime - startTime)/1000);
        });
    }

    class Launcher extends Thread{
        CountDownLatch latch;
        int numWorkers;

        public Launcher(int numWorkers){
            workers = new ArrayList<>();
            this.numWorkers = numWorkers;
            canStartWorking = new Semaphore(numWorkers);
            latch = new CountDownLatch(urls.size());
            completedThreadsNum = 0;
            for (int i = 0; i < urls.size(); i++) {
                model.setValueAt("", i, 1);
            }
        }

        public void run(){
            startTime = System.currentTimeMillis();
            runningThreadsNum++;

            for (int i = 0; i < urls.size(); i++){
                try { canStartWorking.acquire(); }
                catch (InterruptedException e) { return; }
                WebWorker worker = new WebWorker(urls.get(i), i, WebFrame.this, latch);
                workers.add(worker);
                worker.start();
            }

            try { latch.await(); }
            catch (InterruptedException e) { e.getMessage(); }

            endTime = System.currentTimeMillis();
            runningThreadsNum--;
            updateLabels();
            setReadyState();
        }
    }
}
