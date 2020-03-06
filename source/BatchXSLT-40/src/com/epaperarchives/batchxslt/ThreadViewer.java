package com.epaperarchives.batchxslt;

import java.awt.BorderLayout;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

public class ThreadViewer extends JPanel {
	private final ThreadViewerTableModel tableModel = new ThreadViewerTableModel();

	public ThreadViewer() {

		JTable table = new JTable(tableModel);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

		TableColumnModel colModel = table.getColumnModel();
		int numColumns = colModel.getColumnCount();

		for (int i = 0; i < numColumns - 1; i++) {
			TableColumn col = colModel.getColumn(i);

			col.sizeWidthToFit();
			col.setPreferredWidth(col.getWidth() + 5);
			col.setMaxWidth(col.getWidth() + 5);
		}

		JScrollPane sp = new JScrollPane(table);

		setLayout(new BorderLayout());
		add(sp, BorderLayout.CENTER);
	}

    /* finalize() is deprecated as of Java 9 (wiil this be fine?)
	public void dispose() {
		tableModel.stopRequest();
	}

	protected void finalize() throws Throwable {
		dispose();
	}
    */


	public static void main(String[] args) {
		JFrame f = new JFrame(); 
		ThreadViewer viewer = new ThreadViewer();

		f.setContentPane(viewer);
		f.setSize(500, 300);
		f.setVisible(true);
			
		f.setDefaultCloseOperation(1);

		// Keep the main thread from exiting by blocking
		// on wait() for a notification that never comes.
		/*
		Object lock = new Object();
		synchronized (lock) {
			try {
				lock.wait();
			} catch (InterruptedException x) {
			}
		}
		 */
	}
}

class ThreadViewerTableModel extends AbstractTableModel {
	private final Object dataLock;

	private int rowCount;

	private Object[][] cellData;

	private Object[][] pendingCellData;

	private final int columnCount;

	private final String[] columnName;

	private final Class[] columnClass;

	private final Thread internalThread;

	private volatile boolean noStopRequested;

	public ThreadViewerTableModel() {
		rowCount = 0;
		cellData = new Object[0][0];

		String[] names = { "Priority", "Alive", "Daemon", "Interrupted",
				"ThreadGroup", "Thread Name" };
		columnName = names;

		Class[] classes = { Integer.class, Boolean.class, Boolean.class,
				Boolean.class, String.class, String.class };
		columnClass = classes;

		columnCount = columnName.length;

		dataLock = new Object();

		noStopRequested = true;
		Runnable r = new Runnable() {
            @Override
			public void run() {
				try {
					runWork();
				} catch (Exception x) {
					// in case ANY exception slips through
					x.printStackTrace();
				}
			}
		};

		internalThread = new Thread(r, "ThreadViewer");
		internalThread.setPriority(Thread.MAX_PRIORITY - 2);
		internalThread.setDaemon(true);
		internalThread.start();
	}

	private void runWork() {
		Runnable transferPending = new Runnable() {
            @Override
			public void run() {
				transferPendingCellData();
				fireTableDataChanged();
			}
		};

		while (noStopRequested) {
			try {
				createPendingCellData();
				SwingUtilities.invokeAndWait(transferPending);
				Thread.sleep(2000);
			} catch (InvocationTargetException tx) {
				tx.printStackTrace();
				stopRequest();
			} catch (InterruptedException x) {
				Thread.currentThread().interrupt();
			}
		}
	}

	public void stopRequest() {
		noStopRequested = false;
		internalThread.interrupt();
	}

	public boolean isAlive() {
		return internalThread.isAlive();
	}

	private void createPendingCellData() {
		Thread[] thread = findAllThreads();
		Object[][] cell = new Object[thread.length][columnCount];

		for (int i = 0; i < thread.length; i++) {
			Thread t = thread[i];
			Object[] rowCell = cell[i];

			rowCell[0] = Integer.valueOf(t.getPriority());
			rowCell[1] = t.isAlive();
			rowCell[2] = t.isDaemon();
			rowCell[3] = t.isInterrupted();
			rowCell[4] = t.getThreadGroup().getName();
			rowCell[5] = t.getName();
		}

		synchronized (dataLock) {
			pendingCellData = cell;
		}
	}

	private void transferPendingCellData() {
		synchronized (dataLock) {
			cellData = pendingCellData;
			rowCount = cellData.length;
		}
	}

    @Override
	public int getRowCount() {
		return rowCount;
	}

	public Object getValueAt(int row, int col) {
		return cellData[row][col];
	}

	public int getColumnCount() {
		return columnCount;
	}

    @Override
	public Class getColumnClass(int columnIdx) {
		return columnClass[columnIdx];
	}

    @Override
	public String getColumnName(int columnIdx) {
		return columnName[columnIdx];
	}

	public static Thread[] findAllThreads() {
		ThreadGroup group = Thread.currentThread().getThreadGroup();

		ThreadGroup topGroup = group;

		while (group != null) {
			topGroup = group;
			group = group.getParent();
		}

		int estimatedSize = topGroup.activeCount() * 2;
		Thread[] slackList = new Thread[estimatedSize];

		int actualSize = topGroup.enumerate(slackList);

		Thread[] list = new Thread[actualSize];
		System.arraycopy(slackList, 0, list, 0, actualSize);

		return list;
	}
}
