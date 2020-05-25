// JCount.java

/*
 Basic GUI/Threading exercise.
*/

import javax.swing.*;

public class JCount extends JPanel {
	JTextField field;
	JLabel label;
	JButton start, stop;
	Thread currentWorker;

	public JCount() {
		// Set the JCount to use Box layout
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		// YOUR CODE HERE
		field = new JTextField("100000000");
		label = new JLabel("0");
		start = new JButton("Start");
		start.addActionListener(actionEvent -> JCount.this.onStart());
		stop = new JButton("Stop");
		stop.addActionListener(actionEvent -> JCount.this.onStop());
		this.add(field);
		this.add(label);
		this.add(start);
		this.add(stop);
		this.add(Box.createVerticalStrut(40));
		currentWorker = new Worker();
	}

	public void onStart(){
		if (currentWorker.isAlive()) currentWorker.interrupt();
		currentWorker = new Worker();
		currentWorker.start();
	}

	public void onStop(){
		if (currentWorker.isAlive()) currentWorker.interrupt();
	}

	public void updateLabel(int count){
		SwingUtilities.invokeLater(() -> label.setText(String.valueOf(count)));
	}

	class Worker extends Thread{
		public void run(){
			int bound = Integer.parseInt(field.getText());
			for (int i = 0; i < bound; i++) {
				try{
					sleep(1);
				} catch (InterruptedException ex) {
					System.out.println(ex.getMessage());
					return;
				}
				if (i % 1000 == 0){ updateLabel(i); }
				if(isInterrupted()) {
					System.out.println("thread is interrupted");
					return;
				}
			}
			System.out.println("Thread finished job without interruption");
		}
	}
	
	static public void main(String[] args)  {
		// Creates a frame with 4 JCounts in it.
		// (provided)
		JFrame frame = new JFrame("The Count");
		frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
		
		frame.add(new JCount());
		frame.add(new JCount());
		frame.add(new JCount());
		frame.add(new JCount());

		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
}

