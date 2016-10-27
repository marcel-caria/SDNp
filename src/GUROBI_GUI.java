import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.JLabel;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class GUROBI_GUI {
	boolean finish;
	JButton stopButton;
	JProgressBar progressBar;
	JLabel objectiveLabel;
	JLabel gapLabel;
	JLabel timeLabel;
	JFrame frmGurobiController;
	
	void make_GUI(){
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
//					GUROBI_GUI window = new GUROBI_GUI();
//					window.initialize();
//					window.frmGurobiController.setVisible(true);
					initialize();
					frmGurobiController.setVisible(true);
				} catch (Exception e) {e.printStackTrace();}
			}
		});
	}

	void initialize() {
		frmGurobiController = new JFrame();
		frmGurobiController.setTitle("GUROBI");
		frmGurobiController.setResizable(false);
		frmGurobiController.setBounds(100, 100, 214, 170);
		frmGurobiController.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmGurobiController.getContentPane().setLayout(null);
		
		finish = false;
		stopButton = new JButton("Stop");
		stopButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				finish = true;
			}
		});
		stopButton.setBounds(58, 108, 89, 23);
		frmGurobiController.getContentPane().add(stopButton);
		
		progressBar = new JProgressBar();
		progressBar.setBounds(10, 83, 188, 14);
		progressBar.setValue(0);
		frmGurobiController.getContentPane().add(progressBar);
		
		gapLabel = new JLabel("Initializing", SwingConstants.CENTER);
		gapLabel.setBounds(10, 58, 188, 14);
		frmGurobiController.getContentPane().add(gapLabel);
		
		timeLabel = new JLabel("Elapsed time:");
		timeLabel.setBounds(10, 33, 188, 14);
		frmGurobiController.getContentPane().add(timeLabel);
		
		objectiveLabel = new JLabel("Best objective:");
		objectiveLabel.setBounds(10, 11, 188, 14);
		frmGurobiController.getContentPane().add(objectiveLabel);
	}
	
}
