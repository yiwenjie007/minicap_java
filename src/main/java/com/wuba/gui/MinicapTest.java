/**
 * 
 */
package com.wuba.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import com.wuba.minicap.Banner;
import org.apache.log4j.Logger;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.RawImage;
import com.android.ddmlib.TimeoutException;
import com.wuba.device.ADB;
import com.wuba.minicap.AndroidScreenObserver;
import com.wuba.minicap.MiniCapUtil;

/**
 * @author hui.qian qianhui@58.com
 * @date 2015年8月14日 下午7:40:17
 */

public class MinicapTest extends JFrame {
	private static final Logger LOG = Logger.getLogger("PageTest.class");

	private MyPanel mp = null;
	private IDevice device;
	private int width = 400;
	private int height = 600;
	private Thread thread = null;
	private Socket socket;
	private Banner banner = new Banner();
	private OutputStream outputStream = null;

	public MinicapTest() {
		ADB adb = new ADB();
		if (adb.getDevices().length <= 0) {
			LOG.error("无连接设备,请检查");
			return;
		}
		device = adb.getDevices()[0];
		mp = new MyPanel(device,this);
		this.getContentPane().add(mp);
		this.setSize(width, height);
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		this.setLocation((dim.width - this.getWidth()) / 2, 0);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {

			}
		});
		mp.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {

			}

			@Override
			public void mousePressed(MouseEvent e) {
				Point point = pointConvert(e.getPoint());
				if (outputStream != null) {
					String command = String.format("d 0 %s %s 50\n", (int)point.getX(), (int)point.getY());
					executeTouch(command);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				System.out.println("i release");
				if (outputStream != null) {
					String command =  "u 0\n";
					executeTouch(command);
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {

			}

			@Override
			public void mouseExited(MouseEvent e) {

			}
		});
		mp.addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseDragged(MouseEvent e) {
				System.out.println(e.getPoint().getX()+","+e.getPoint().getY());
				Point point = pointConvert(e.getPoint());
				if (outputStream != null) {
					String command = String.format("m 0 %s %s 50\n", (int)point.getX(), (int)point.getY());
					executeTouch(command);
				}
			}

			@Override
			public void mouseMoved(MouseEvent e) {

			}
		});
		this.setVisible(true);
		pack();

	}

	public static void main(String[] args) {
		new MinicapTest();
	}

	class MyPanel extends JPanel implements AndroidScreenObserver {

		BufferedImage image = null;
		MiniCapUtil minicap = null;

		public MyPanel(IDevice device,MinicapTest frame) {
			minicap = new MiniCapUtil(device);
			minicap.registerObserver(this);
//			minicap.takeScreenShotOnce();
			minicap.startScreenListener();
			try {
				try {
					Thread.sleep(4*1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				socket = new Socket("localhost", 1111);
				InputStream stream = socket.getInputStream();
				outputStream = socket.getOutputStream();
				int len = 4096;

				byte[] buffer;
				buffer = new byte[len];
				int realLen = stream.read(buffer);
				if (buffer.length != realLen) {
					buffer = subByteArray(buffer, 0, realLen);
				}
				String result = new String(buffer);
				String[] array = result.split(" |\n");

				banner.setVersion(Integer.parseInt(array[1]));
				banner.setMaxPoint(Integer.parseInt(array[3]));
				banner.setMaxPress(Integer.parseInt(array[6]));
				banner.setMaxX(Integer.parseInt(array[4]));
				banner.setMaxY(Integer.parseInt(array[5]));

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private byte[] subByteArray(byte[] byte1, int start, int end) {
			byte[] byte2 = new byte[0];
			try {
				byte2 = new byte[end - start];
			} catch (NegativeArraySizeException e) {
				e.printStackTrace();
			}
			System.arraycopy(byte1, start, byte2, 0, end - start);
			return byte2;
		}

		public void paint(Graphics g) {
			try {
				if (image == null)
					return;
				MinicapTest.this.setSize(width, height);
				g.drawImage(image, 0, 0, width, height - 20, null);
				this.setSize(width, height + 300);
				image.flush();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void frameImageChange(Image image) {
			this.image = (BufferedImage) image;
			int w = this.image.getWidth();
			int h = this.image.getHeight();
			float radio = (float) width / (float) w;
			height = (int) (radio * h);
			System.out.println("width : " + w + ",height : " + h);
			this.repaint();
		}
	}

	private Point pointConvert(Point point){
		return new Point((int)((point.getX()*1.0 / width) * banner.getMaxX()) , (int)((point.getY()*1.0 /height) * banner.getMaxY()) );
	}

	private void executeTouch(String command) {
		if (outputStream != null) {
			try {
				//System.out.println("command" + command);
				outputStream.write(command.getBytes());
				outputStream.flush();
				String endCommand = "c\n";
				outputStream.write(endCommand.getBytes());
				outputStream.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
