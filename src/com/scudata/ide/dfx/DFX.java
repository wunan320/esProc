package com.scudata.ide.dfx;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.scudata.app.common.Section;
import com.scudata.app.config.ConfigUtil;
import com.scudata.app.config.RaqsoftConfig;
import com.scudata.cellset.ICellSet;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.StringUtils;
import com.scudata.dm.Context;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.ide.common.AppFrame;
import com.scudata.ide.common.AppMenu;
import com.scudata.ide.common.ConfigFile;
import com.scudata.ide.common.ConfigOptions;
import com.scudata.ide.common.ConfigUtilIde;
import com.scudata.ide.common.DataSource;
import com.scudata.ide.common.DataSourceListModel;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.IPrjxSheet;
import com.scudata.ide.common.LookAndFeelManager;
import com.scudata.ide.common.PrjxAppMenu;
import com.scudata.ide.common.PrjxAppToolBar;
import com.scudata.ide.common.TcpServer;
import com.scudata.ide.common.ToolBarPropertyBase;
import com.scudata.ide.common.ToolBarWindow;
import com.scudata.ide.common.control.PanelConsole;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.custom.IResourceTree;
import com.scudata.ide.dfx.base.FileTree;
import com.scudata.ide.dfx.base.JTabbedParam;
import com.scudata.ide.dfx.base.PanelDfxWatch;
import com.scudata.ide.dfx.base.PanelValue;
import com.scudata.ide.dfx.dialog.DialogSplash;
import com.scudata.ide.dfx.resources.IdeDfxMessage;
import com.scudata.ide.dfx.update.UpdateManager;
import com.scudata.util.CellSetUtil;

/**
 * ������IDE����������
 *
 */
public class DFX extends AppFrame {
	private static final long serialVersionUID = 1L;

	/**
	 * MACϵͳʱ������DOCKͼ��
	 */
	static {
		try {
			if (com.scudata.ide.common.GM.isMacOS()) {
				ImageIcon ii = com.scudata.ide.common.GM.getLogoImage(true);
				if (ii != null) {
					com.scudata.ide.common.GM.setMacOSDockIcon(ii.getImage());
				}
			}
		} catch (Throwable t) {
			GM.outputMessage(t);
		}
	}

	/**
	 * ���ָ����
	 */
	private JSplitPane splitMain = new JSplitPane();

	/**
	 * ���������
	 */
	private JPanel barPanel = new JPanel();

	/**
	 * �в��ָ����
	 */
	private JSplitPane splitCenter = new JSplitPane();

	/**
	 * �ұ߷ָ����
	 */
	private JSplitPane splitEast = new JSplitPane();

	/**
	 * ���±�ǩʽ���
	 */
	private JTabbedParam tabParam;

	/**
	 * �˵�
	 */
	protected AppMenu currentMenu;

	/**
	 * ���������
	 */
	private JSplitPane spMain = new JSplitPane();

	/**
	 * �˳�ʱ�Ƿ�ر�JVM
	 */
	private boolean terminalVM = true;

	/**
	 * �Զ����ӵ�����Դ��������
	 */
	private String[] startDsNames = null;

	/**
	 * ��Դ���ؼ�
	 */
	protected IResourceTree fileTree;

	/**
	 * ��������Դ������
	 */
	private MessageManager mm = IdeDfxMessage.get();

	/**
	 * �ļ��������Ƿ��ʼ��
	 */
	private boolean isInit = false;

	/**
	 * ���캯��
	 */
	public DFX() {
		this(null);
	}

	/**
	 * ���캯��
	 * 
	 * @param openFile
	 *            ����ʱ�Զ����ļ�
	 */
	public DFX(String openFile) {
		this(openFile, true);
	}

	/**
	 * ���캯��
	 * 
	 * @param openFile
	 *            ����ʱ�Զ����ļ�
	 * @param terminalVMwhileExit
	 *            �˳�ʱ�Ƿ�ر�JVM
	 */
	public DFX(String openFile, boolean terminalVMwhileExit) {
		super();
		try {
			ConfigFile.getConfigFile().setConfigNode(ConfigFile.NODE_OPTIONS);
			GV.lastDirectory = ConfigFile.getConfigFile().getAttrValue(
					"fileDirectory");
		} catch (Throwable t) {
			GM.outputMessage(t);
		}
		try {
			Env.getCollator();
		} catch (Throwable t) {
		}
		setProgramPart();
		if (GV.config != null) {
			List<String> dsList = GV.config.getAutoConnectList();
			if (dsList != null && !dsList.isEmpty()) {
				startDsNames = new String[dsList.size()];
				for (int i = 0; i < dsList.size(); i++)
					startDsNames[i] = (String) dsList.get(i);
			} else {
				startDsNames = null;
			}
		}
		this.terminalVM = terminalVMwhileExit;
		try {
			GV.appFrame = this;
			GV.dsModel = new DataSourceListModel();

			GV.toolWin = new ToolBarWindow() {
				private static final long serialVersionUID = 1L;

				public void closeSheet(IPrjxSheet sheet) {
					((DFX) GV.appFrame).closeSheet(sheet);
				}

				public void dispSheet(IPrjxSheet sheet) throws Exception {
					((DFX) GV.appFrame).showSheet(sheet);
				}

				public String getSheetIconName() {
					return "file_dfx.png";
				}

				public ImageIcon getLogoImage() {
					return GM.getLogoImage(true);
				}

			};
			// Desk
			desk = new JDesktopPane();
			desk.setDragMode(JDesktopPane.LIVE_DRAG_MODE);
			desk.revalidate();

			GV.autoOpenFileName = openFile;

			newResourceTree();

			// Menu
			newMenuDfx();
			AppMenu menuBase = newMenuBase();
			GV.appMenu = menuBase;
			currentMenu = menuBase;
			setJMenuBar(GV.appMenu);

			GM.resetEnvDataSource(GV.dsModel);

			PanelValue panelValue = new PanelValue();
			PanelDfxWatch panelDfxWatch = new PanelDfxWatch();
			GVDfx.panelDfxWatch = panelDfxWatch;

			// ToolBar
			PrjxAppToolBar toolBase = null;
			ToolBarPropertyBase toolBarProperty = null;
			toolBase = GVDfx.getBaseTool();
			toolBarProperty = GVDfx.getDfxProperty();
			//
			GV.appTool = toolBase;
			GV.toolBarProperty = toolBarProperty;

			barPanel.setLayout(new BorderLayout());
			barPanel.add(GV.appTool, BorderLayout.NORTH);
			barPanel.add(GV.toolBarProperty, BorderLayout.CENTER);
			JPanel panelCenter = new JPanel(new BorderLayout());
			panelCenter.add(splitMain, BorderLayout.CENTER);
			panelCenter.add(GV.toolWin, BorderLayout.NORTH);
			splitMain.add(splitCenter, JSplitPane.LEFT);
			final int SPLIT_WIDTH = 8;
			splitMain.setOneTouchExpandable(true);
			splitMain.setDividerSize(SPLIT_WIDTH);
			splitMain.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
			final int POS_MAIN = new Double(0.25 * Toolkit.getDefaultToolkit()
					.getScreenSize().getWidth()).intValue();
			final int POS_DESK = new Double((1 - 0.25)
					* Toolkit.getDefaultToolkit().getScreenSize().getWidth())
					.intValue();
			splitMain.setDividerLocation(POS_DESK - POS_MAIN);

			splitCenter.setOneTouchExpandable(true);
			splitCenter.setDividerSize(SPLIT_WIDTH);
			splitCenter.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
			splitCenter.setRightComponent(desk);

			lastLeftLocation = 0;
			JTabbedPane jTPLeft = new JTabbedPane();
			jTPLeft.setMinimumSize(new Dimension(0, 0));
			JTabbedPane jTPRight = new JTabbedPane();
			jTPRight.setMinimumSize(new Dimension(0, 0));

			jTPLeft.addTab(mm.getMessage("public.file"), new JScrollPane(
					fileTree.getComponent()));

			jTPRight.addTab(mm.getMessage("dfx.tabvalue"), panelValue);
			tabParam = new JTabbedParam() {
				private static final long serialVersionUID = 1L;

				public void selectVar(Object val, String varName) {
					GVDfx.panelValue.tableValue.setValue1(val, varName);
					GVDfx.panelValue.valueBar.refresh();
					this.repaint();
				}
			};
			GVDfx.tabParam = tabParam;
			if (ConfigOptions.bIdeConsole.booleanValue()) {
				this.tabParam.consoleVisible(true);
			}
			splitCenter.setLeftComponent(jTPLeft);
			// ���ļ����Ϳ���̨��������������
			// ��tab��ǩ�����ļ�����tab��ǩ��һֱ���ڣ�����ס����̨���ȵĴ����ᵽ����
			if (ConfigOptions.iConsoleLocation != null
					&& ConfigOptions.iConsoleLocation.intValue() > -1) {
				lastLeftLocation = ConfigOptions.iConsoleLocation.intValue();
				if (lastLeftLocation <= SPLIT_GAP) {
					splitCenter.setDividerLocation(Math
							.round((POS_DESK - POS_MAIN) * 0.4f));
				} else {
					splitCenter.setDividerLocation(0);
				}
				splitCenter.setDividerLocation(lastLeftLocation);
			} else {
				splitCenter.setDividerLocation(0);
				isInit = true;
			}

			fileTree.changeMainPath(ConfigOptions.sMainPath);

			if (ConfigOptions.bWindowSize.booleanValue()) {
				lastRightLocation = (int) (Toolkit.getDefaultToolkit()
						.getScreenSize().getWidth() - panelValue.getWidth());
				splitMain.setDividerLocation(lastRightLocation);
			} else {
				lastRightLocation = POS_DESK;
				splitMain.setDividerLocation(lastRightLocation);
			}

			splitEast.setOneTouchExpandable(true);
			splitEast.setDividerSize(SPLIT_WIDTH);
			splitEast.setOrientation(JSplitPane.VERTICAL_SPLIT);
			final int POS_RIGHT_DFX = new Double(0.45 * Toolkit
					.getDefaultToolkit().getScreenSize().getHeight())
					.intValue();
			splitEast.setDividerLocation(POS_RIGHT_DFX);
			JPanel panelRight = new JPanel();
			panelRight.setLayout(new BorderLayout());
			splitEast.add(jTPRight, JSplitPane.TOP);
			splitEast.add(tabParam, JSplitPane.BOTTOM);
			panelRight.add(splitEast, BorderLayout.CENTER);
			splitMain.add(panelRight, JSplitPane.RIGHT);

			spMain.setOrientation(JSplitPane.VERTICAL_SPLIT);
			spMain.setDividerSize(4);
			spMain.setTopComponent(barPanel);
			spMain.setBottomComponent(panelCenter);
			getContentPane().add(spMain, BorderLayout.CENTER);
			spMain.setDividerLocation(TOOL_MIN_LOCATION);
			spMain.setBorder(BorderFactory.createRaisedBevelBorder());
			spMain.addPropertyChangeListener(new PropertyChangeListener() {

				public void propertyChange(PropertyChangeEvent e) {
					boolean isExpand = isToolBarExpand();
					GV.toolBarProperty.setExtendButtonIcon(isExpand);
				}

			});

			pack();
			initUI();
			GV.allFrames.add(this);

			int width = splitCenter.getWidth();
			if (isInit || lastLeftLocation == 0) { // �ļ��������Ƿ��ʼ��
				width = Math.round(width * 0.4f);
			} else {
				width = lastLeftLocation;
			}
			splitCenter.setLastDividerLocation(width);

		} catch (Throwable e) {
			GM.showException(e);
			exit();
		}
	}

	/**
	 * ������Դ��
	 */
	protected void newResourceTree() {
		fileTree = new FileTree();
		GV.fileTree = fileTree;
	}

	/**
	 * ���������˵�
	 * 
	 * @return
	 */
	protected AppMenu newMenuBase() {
		return GVDfx.getBaseMenu();
	}

	/**
	 * �����༭�˵�
	 * 
	 * @return
	 */
	protected AppMenu newMenuDfx() {
		return GVDfx.getDfxMenu();
	}

	/**
	 * ��ʼ������
	 */
	private void initUI() {
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.setEnabled(true);
		this.addWindowListener(new PRJX_this_windowAdapter(this));
		this.addComponentListener(new ComponentAdapter() {

			public void componentMoved(ComponentEvent e) {
				if (!GV.getFuncWindow().isDisplay()) {
					return;
				}
				if (thread == null) {
					thread = new ControlThread();
				}
				SwingUtilities.invokeLater(thread);
			}

			public void componentResized(ComponentEvent e) {
				GV.toolBarProperty.resetTextWindow(resizeFuncWin, true);
				GV.toolWin.refresh();
			}
		});
	}

	/** �̶߳��� */
	private ControlThread thread = null;
	private static boolean resizeFuncWin = false;

	/**
	 * ʹ���߳̽�����������ƶ�ʱ��������
	 */
	class ControlThread extends Thread {
		public void run() {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					if (resizeFuncWin) {
						GV.toolBarProperty.resetTextWindow(resizeFuncWin, true);
						resizeFuncWin = false;
					} else {
						GV.toolWin.refresh();
						resizeFuncWin = true;
					}
				}
			});
		}
	}

	/**
	 * ȡ���еĴ��ļ����ڶ���
	 * 
	 * @return
	 */
	public JInternalFrame[] getAllInternalFrames() {
		return desk.getAllFrames();
	}

	/**
	 * ȡ���е�ҳ����
	 * 
	 * @return
	 */
	public String[] getSheetTitles() {
		JInternalFrame[] sheets = GV.appFrame.getDesk().getAllFrames();
		if (sheets == null || sheets.length == 0) {
			return null;
		}
		int len = sheets.length;
		String[] titles = new String[len];
		for (int i = 0; i < len; i++) {
			titles[i] = (((IPrjxSheet) sheets[i]).getSheetTitle());
		}
		return titles;
	}

	/**
	 * �ر�ָ��ҳ
	 */
	public boolean closeSheet(Object sheet) {
		return closeSheet(sheet, true);
	}

	/**
	 * �ر�ָ��ҳ
	 * 
	 * @param sheet
	 *            ҳ����
	 * @param showSheet
	 *            �رպ��Ƿ���ʾ����ҳ���ر�ȫ��ҳʱӦ����false
	 * @return
	 */
	public boolean closeSheet(Object sheet, boolean showSheet) {
		if (sheet == null) {
			return false;
		}

		if (!((IPrjxSheet) sheet).close()) {
			return false;
		}

		String sheetTitle = ((IPrjxSheet) sheet).getSheetTitle();
		GV.appMenu.removeLiveMenu(sheetTitle);
		desk.getDesktopManager().closeFrame((JInternalFrame) sheet);

		JInternalFrame[] frames = desk.getAllFrames();

		if (frames.length == 0) {
			changeMenuAndToolBar(newMenuBase(), GVDfx.getBaseTool());
			GV.appMenu.setEnable(((PrjxAppMenu) (GV.appMenu)).getMenuItems(),
					false);
			GV.appTool.setBarEnabled(false);
			GV.toolWin.setVisible(false);
			GV.appSheet = null;
		} else if (showSheet) {
			try {
				if (frames.length > 0) {
					showSheet(frames[0], false);
				}
			} catch (Exception x) {
				// �Ҳ�������ʾ�ľ�����
			}
			try {
				((PrjxAppMenu) GV.appMenu).refreshRecentFileOnClose(sheetTitle,
						frames);
			} catch (Throwable t) {
			}
		}
		resetTitle();
		GV.toolWin.refresh();
		return true;

	}

	/**
	 * �ر�ȫ��ҳ
	 */
	public boolean closeAll() {
		JInternalFrame[] frames = desk.getAllFrames();
		IPrjxSheet sheet;
		try {
			for (int i = 0; i < frames.length; i++) {
				sheet = (IPrjxSheet) frames[i];
				if (!closeSheet(sheet, false)) {
					return false;
				}
			}
			closeRemoteServer();

		} catch (Exception x) {
			GM.showException(x);
			return false;
		}
		return true;
	}

	/**
	 * �˳�Զ�̷���
	 */
	protected boolean closeRemoteServer() {
		return true;
	}

	/**
	 * �����˳�
	 */
	public boolean exit() {
		try {
			List<String> connectedDSNames = new ArrayList<String>();
			int size = GV.dsModel.size();
			for (int i = 0; i < size; i++) {
				DataSource ds = (DataSource) GV.dsModel.get(i);
				if (!ds.isClosed()) {
					connectedDSNames.add(ds.getName());
				}
			}
			if (connectedDSNames.isEmpty()) {
				connectedDSNames = null;
			}

			if (fileTree != null && fileTree instanceof FileTree) {
				// �˳�ʱ����ס���δ��ļ������Ŀ���
				((FileTree) fileTree).saveExpandState(splitCenter
						.getDividerLocation());
			}
			GV.config.setAutoConnectList(connectedDSNames);
			ConfigUtilIde.writeConfig(false);
		} catch (Exception e) {
			GM.outputMessage(e);
		}

		try {
			if (splitCenter.getLeftComponent() == null) {
				ConfigOptions.iConsoleLocation = new Integer(-1);
			} else {
				int dl = splitCenter.getDividerLocation();
				if (GV.toolWin != null
						&& ConfigOptions.bViewWinList.booleanValue()) {
				}
				ConfigOptions.iConsoleLocation = new Integer(dl);
			}
			ConfigOptions.save();

			ConfigFile cf = ConfigFile.getConfigFile();
			cf.setConfigNode(ConfigFile.NODE_OPTIONS);
			cf.setAttrValue("fileDirectory", GV.lastDirectory);
			GM.setWindowDimension(GVDfx.panelValue);
			cf.save();

			if (GV.dsModel != null) {
				DataSource ds;
				for (int i = 0; i < GV.dsModel.size(); i++) {
					ds = (DataSource) GV.dsModel.getElementAt(i);
					if (ds == null || ds.isClosed()) {
						continue;
					}
					ds.close();
				}
			}
		} catch (Throwable x) {
			GM.showException(x);
		}
		try {
			if (!exitCustom())
				return false;
		} catch (Throwable x) {
			GM.showException(x);
		}

		GV.allFrames.remove(this);
		if (terminalVM) {
			System.exit(0);
		} else {
			this.dispose();
		}
		return false;
	}

	/**
	 * �˳��Զ������
	 */
	protected boolean exitCustom() {
		return true;
	}

	/**
	 * ����ر�������ҳ�����˳���
	 */
	public void quit() {
		if (closeAll()) {
			exit();
		}
	}

	/**
	 * ���ļ�
	 */
	public JInternalFrame openSheetFile(String filePath) throws Exception {
		synchronized (desk) {
			JInternalFrame o = getSheet(filePath);
			if (o != null) {
				if (!showSheet(o))
					return null;
				GV.toolWin.refresh();
				return null;
			} else {
				if (GV.appSheet != null && !GV.appSheet.submitEditor()) {
					return null;
				}
			}
			ICellSet cs = null;
			if (!StringUtils.isValidString(filePath)) { // �½�
				String pre;
				if (filePath == null) {
					pre = GCDfx.PRE_NEWPGM;
				} else {
					pre = GCDfx.PRE_NEWETL;
				}
				filePath = GMDfx.getNewName(pre);
			} else {
				// ��ͬ�Ĳ������ݣ����ܻ��ں����ӿո�
				filePath = filePath.trim();
				// ��ʱ���Ȩ��
				cs = readCellSet(filePath);
				if (cs == null)
					return null;
			}
			JInternalFrame sheet = openSheet(filePath, cs);
			return sheet;
		}
	}

	/**
	 * ���������ļ�
	 * 
	 * @param in
	 *            ������
	 * @param fileName
	 *            �ļ���
	 * @param isRemote
	 *            �Ƿ���Զ�̷������ļ�
	 * @return
	 */
	public JInternalFrame openSheetFile(InputStream in, String filePath,
			boolean isRemote) throws Exception {
		synchronized (desk) {
			JInternalFrame o = getSheet(filePath);
			ICellSet cs = CellSetUtil.readPgmCellSet(in);
			if (o != null) {
				((SheetDfx) o).setCellSet(cs);
				((SheetDfx) o).isServerFile = isRemote;
				if (showSheet(o)) {
					GV.toolWin.refresh();
					return null;
				}
			}
			if (!StringUtils.isValidString(filePath)) { // �½�
				filePath = GMDfx.getNewName();
			} else {
				// ��ͬ�Ĳ������ݣ����ܻ��ں����ӿո�
				filePath = filePath.trim();
			}
			JInternalFrame sheet = openSheet(filePath, cs);
			((SheetDfx) sheet).isServerFile = isRemote;
			return sheet;
		}
	}

	/**
	 * �½��ļ�
	 * 
	 * @param filePath
	 *            �ļ�·��
	 * @param isServerFile
	 *            �Ƿ������ӵ�Զ�̷��������ļ�
	 * @return
	 */
	public JInternalFrame newSheetFile(String filePath, boolean isServerFile) {
		synchronized (desk) {
			try {
				SheetDfx sheet = new SheetDfx(filePath, null);
				sheet.isServerFile = isServerFile;
				Dimension d = desk.getSize();
				boolean loadSheet = GM.loadWindowSize(sheet);
				if (!loadSheet) {
					sheet.setBounds(0, 0, d.width, d.height);
				}
				boolean setMax = false;
				if (GV.appSheet != null && GV.appSheet.isMaximum()
						&& !GV.appSheet.isIcon()) {
					GV.appSheet.resumeSheet();
					if (loadSheet) // not max
						((IPrjxSheet) sheet).setForceMax();
					setMax = true;
				}
				sheet.show();
				desk.add(sheet);
				if (setMax || !GM.loadWindowSize(sheet))
					sheet.setMaximum(true);
				sheet.setSelected(true);
				if (!GV.toolWin.isVisible()
						&& ConfigOptions.bViewWinList.booleanValue())
					GV.toolWin.setVisible(true);
				GV.toolWin.refresh();
				((IPrjxSheet) sheet).resetSheetStyle();
				return sheet;
			} catch (Exception e) {
				GM.showException(e);
			}
			return null;
		}
	}

	/**
	 * ��ҳ��
	 * 
	 * @param filePath
	 *            �ļ�·��
	 * @param cellSet
	 *            �������
	 * @return
	 */
	public synchronized JInternalFrame openSheet(String filePath, Object cellSet) {
		return openSheet(filePath, cellSet, cellSet != null);
	}

	/**
	 * ��ҳ��
	 * 
	 * @param filePath
	 *            �ļ�·��
	 * @param cellSet
	 *            �������
	 * @param refreshRecentFile
	 *            �Ƿ�ˢ������ļ�
	 * @return
	 */
	public synchronized JInternalFrame openSheet(String filePath,
			Object cellSet, boolean refreshRecentFile) {
		return openSheet(filePath, cellSet, refreshRecentFile, null);
	}

	/**
	 * ��ҳ��
	 * 
	 * @param filePath
	 *            �ļ�·��
	 * @param cellSet
	 *            �������
	 * @param refreshRecentFile
	 *            �Ƿ�ˢ������ļ�
	 * @param stepInfo
	 *            �ֲ�������Ϣ��û�еĴ�null
	 * @return
	 */
	public synchronized JInternalFrame openSheet(String filePath,
			Object cellSet, boolean refreshRecentFile, StepInfo stepInfo) {
		try {
			JInternalFrame sheet = new SheetDfx(filePath, (PgmCellSet) cellSet,
					stepInfo);

			Dimension d = desk.getSize();
			boolean loadSheet = GM.loadWindowSize(sheet);
			if (!loadSheet) {
				sheet.setBounds(0, 0, d.width, d.height);
			}
			boolean setMax = false;
			if (GV.appSheet != null && GV.appSheet.isMaximum()
					&& !GV.appSheet.isIcon()) {
				GV.appSheet.resumeSheet();
				if (loadSheet) // not max
					((IPrjxSheet) sheet).setForceMax();
				setMax = true;
			}
			sheet.show();
			desk.add(sheet);
			if (setMax || !GM.loadWindowSize(sheet))
				sheet.setMaximum(true);
			sheet.setSelected(true);
			if (refreshRecentFile)
				((PrjxAppMenu) GV.appMenu).refreshRecentFile(sheet.getTitle());
			if (!GV.toolWin.isVisible()
					&& ConfigOptions.bViewWinList.booleanValue())
				GV.toolWin.setVisible(true);
			GV.toolWin.refresh();
			((IPrjxSheet) sheet).resetSheetStyle();
			return sheet;
		} catch (Throwable ex) {
			GM.showException(ex);
		}
		return null;
	}

	/**
	 * ��ȡ����
	 * 
	 * @param filePath
	 * @return
	 * @throws Exception
	 */
	public static ICellSet readCellSet(String filePath) throws Exception {
		// �������˫��������·�����пո����
		filePath = filePath.trim();
		ICellSet cs = null;
		String path = filePath.toLowerCase();
		String psw = null;
		if (path.endsWith(GC.FILE_DFX) || path.endsWith(GC.FILE_SPL)) {
			BufferedInputStream bis = null;
			try {
				FileObject fo = new FileObject(filePath, "s");
				bis = new BufferedInputStream(fo.getInputStream());
				if (path.endsWith(GC.FILE_DFX)) {
					cs = CellSetUtil.readPgmCellSet(bis, psw);
				} else {
					cs = GMDfx.readSPL(filePath);
				}
			} finally {
				if (bis != null)
					bis.close();
			}
		}
		return cs;
	}

	/**
	 * ѡ��ȷ�Ϻ�ˢ��
	 */
	public void refreshOptions() {
		try {
			((PrjxAppMenu) GV.appMenu)
					.refreshRecentMainPath(ConfigOptions.sMainPath);
		} catch (Throwable e) {
		}
		fileTree.changeMainPath(ConfigOptions.sMainPath); // ˢ����Դ����Ŀ¼

		if (ConfigOptions.bIdeConsole.booleanValue()) {
			holdConsole();
			tabParam.consoleVisible(true);
		} else {
			if (splitCenter.getLeftComponent() != null) {
				lastLeftLocation = splitCenter.getDividerLocation();
				tabParam.consoleVisible(false);
			}
		}

		if (GV.appSheet != null) {
			GM.setCurrentPath(GV.appSheet.getSheetTitle());
		}
	}

	/**
	 * ��ʾ��һ��ҳ��
	 * 
	 * @param isCtrlDown
	 *            �Ƿ���CTRL��
	 */
	public void showNextSheet(boolean isCtrlDown) {
		JInternalFrame[] frames = desk.getAllFrames();
		if (frames.length <= 1) {
			return;
		}
		JInternalFrame activeSheet = getActiveSheet();
		int size = frames.length;
		int index = size - 1;
		for (int i = 0; i < size; i++) {
			if (frames[i].equals(activeSheet)) {
				if (isCtrlDown) {
					index = size - 1;
				} else {
					if (i == size - 1) {
						index = 0;
					} else {
						index = i + 1;
					}
				}
				break;
			}
		}
		try {
			if (!super.showSheet(frames[index])) {
				return;
			}
			GV.toolWin.refreshSheet(frames[index]);
		} catch (Exception ex) {
		}
	}

	/**
	 * �л������б�
	 */
	public void switchWinList() {
		ConfigOptions.bViewWinList = new Boolean(
				!ConfigOptions.bViewWinList.booleanValue());
		try {
			ConfigOptions.save();
		} catch (Throwable e) {
			GM.outputMessage(e);
		}
		GV.toolWin.setVisible(ConfigOptions.bViewWinList.booleanValue());
		if (GV.toolWin.isVisible())
			GV.toolWin.refresh();
	}

	/**
	 * ��ʾ���������ʱ��С��SPLIT_GAP��Ϊʱ����״̬
	 */
	private static int SPLIT_GAP = 50;

	/**
	 * ֮ǰ�������λ��
	 */
	private int lastLeftLocation;

	/**
	 * ��ʾ������
	 */
	public void viewTabConsole() {
		tabParam.consoleVisible(true);
	}

	/**
	 * ��ʾ������
	 */
	public void viewLeft() {
		int pos = splitCenter.getDividerLocation();
		int width = splitCenter.getWidth();
		if (pos <= 0 || (1 < pos && pos <= SPLIT_GAP)) { // ����״̬��չ��
			lastLeftLocation = lastLeftLocation == 0 ? Math.round(width * 0.4f)
					: lastLeftLocation;
			splitCenter.setDividerLocation(lastLeftLocation);
		} else { // չ��״̬������
			lastLeftLocation = pos;
			splitCenter.setDividerLocation(0);
		}
	}

	/**
	 * ֮ǰ�Ҳ�����λ��
	 */
	private int lastRightLocation;

	/**
	 * ��ʾ�Ҳ����
	 */
	public void viewRight() {
		int pos = splitMain.getDividerLocation();
		int width = splitMain.getWidth();
		if (width - pos <= SPLIT_GAP) { // ����״̬��չ��
			splitMain.setDividerLocation(lastRightLocation);
		} else { // չ��״̬������
			lastRightLocation = pos;
			splitMain.setDividerLocation(width);
		}
	}

	/**
	 * ���õ�ǰDFX����ҳ��ִ��״̬
	 */
	public void resetRunStatus() {
	}

	/**
	 * ������ļ�
	 */
	public void startAutoRecent() {
		if (StringUtils.isValidString(GV.autoOpenFileName)) {
			try {
				openSheetFile(GV.autoOpenFileName);
			} catch (Throwable x) {
				Logger.error(x);
			}
		}

		try {
			if (ConfigOptions.bAutoConnect.booleanValue()) {
				if (startDsNames != null) {
					for (int i = 0; i < startDsNames.length; i++) {
						final DataSource ds = GV.dsModel
								.getDataSource(startDsNames[i]);
						if (ds != null) {
							autoConnect = true;
							new Thread() {
								public void run() {
									try {
										ds.getDBSession();
									} catch (Throwable autox) {
										GM.outputMessage(autox);
									}
									startDBCount = new Integer(
											startDBCount.intValue() + 1);
									resetDBEnv();
								}
							}.start();
						}
					}
				}
			}
		} catch (Throwable x) {
		}

		if (!autoConnect) {
			calcInitDfx(); // ���Զ�����ʱ�������Ӻ��ټ���
		}
	}

	/**
	 * �Ƿ��Զ������������Դ����
	 */
	private boolean autoConnect = false;

	/**
	 * �����ʼ����������
	 */
	private void calcInitDfx() {
		if (GV.config == null)
			return;
		String dfxPath = GV.config.getInitDfx();
		if (StringUtils.isValidString(dfxPath)) {
			try {
				Context ctx = GMDfx.prepareParentContext();
				ConfigUtil.calcInitDfx(dfxPath, ctx);
			} catch (Throwable t) {
				// �����ʼ������{0}ʧ�ܣ�
				GM.showException(t, true, null, IdeCommonMessage.get()
						.getMessage("dfx.calcinitdfx", dfxPath));
			}
		}

	}

	/**
	 * �Ѿ����ӵ�����Դ����
	 */
	private static Integer startDBCount = new Integer(0);

	/**
	 * ��������Դ����
	 */
	private void resetDBEnv() {
		synchronized (startDBCount) {
			if (startDsNames != null
					&& startDsNames.length == startDBCount.intValue()) {
				GVDfx.tabParam.resetEnv();
				ConfigUtilIde.setTask();
				calcInitDfx();
			}
		}
	}

	/**
	 * ������������
	 */
	public boolean saveAll() {
		JInternalFrame[] sheets = getAllInternalFrames();
		if (sheets == null) {
			return false;
		}
		int count = sheets.length;
		for (int i = 0; i < count; i++) {
			if (!((IPrjxSheet) sheets[i]).save()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * ����չ���Ҳ����
	 */
	public void swapRightTab() {
		if (splitMain.getDividerLocation() == splitMain
				.getMaximumDividerLocation()) {
			splitMain.setDividerLocation(splitMain.getLastDividerLocation());
		} else {
			splitMain.setDividerLocation(splitMain.getMaximumDividerLocation());
		}
	}

	/**
	 * �����˵��͹�����
	 */
	public void changeMenuAndToolBar(JMenuBar menu, JToolBar toolBar) {
		if (GV.appSheet == null) {
			return;
		}
		currentMenu = (AppMenu) menu;
		setJMenuBar(menu);
		barPanel.removeAll();
		barPanel.add(toolBar, BorderLayout.NORTH);
		barPanel.add(GV.toolBarProperty, BorderLayout.CENTER);
		validate();
		repaint();
	}

	/** ��������С�߶� */
	private final int TOOL_MIN_LOCATION = 62;
	/** ���������߶� */
	private final int TOOL_MAX_LOCATION = 200;

	/**
	 * չ�������𹤾���
	 */
	public void setToolBarExpand() {
		boolean isExt = isToolBarExpand();
		if (isExt) {
			spMain.setDividerLocation(TOOL_MIN_LOCATION);
		} else {
			int height = getHeight();
			int dl = Math.min(height - 100, TOOL_MAX_LOCATION);
			dl = Math.max(dl, TOOL_MIN_LOCATION);
			spMain.setDividerLocation(dl);
		}
		GV.toolBarProperty.setExtendButtonIcon(isExt);
	}

	/**
	 * �������Ƿ�չ��״̬
	 * 
	 * @return
	 */
	private boolean isToolBarExpand() {
		int dl = spMain.getDividerLocation();
		return dl > TOOL_MIN_LOCATION + 10;
	}

	/**
	 * ����ͼƬ����
	 */
	public static DialogSplash splashWindow = null;

	/**
	 * ׼������
	 * 
	 * @param args
	 *            JVM����
	 * @return
	 * @throws Throwable
	 */
	public static String prepareEnv(String args[]) throws Throwable {
		String openDfx = "";
		String arg = "";
		String usage = "Usage: com.raqsoft.ide.dfx.DFX\n"
				+ "where possible options include:\n"
				+ "-help                            Print out these messages\n"
				+ "-?                               Print out these messages\n"
				+ "where dfx file option is to specify the default dfx file to be openned\n"
				+ "Example:\n"
				+ "java com.raqsoft.ide.dfx.DFX d:\\test.dfx      Start IDE with default file d:\\test.dfx\n";

		if (args.length == 1) { // exe �����Ĳ�����Ȼ��һ������
			arg = args[0].trim();
			if (arg.trim().indexOf(" ") > 0) {
				if (arg.charAt(1) != ':') {// ����·�����ļ������� [�̷�]:��ͷ
					// �����������Ϊһ���ļ���ʱ����Ҫ������ת�������ļ��������ո�ʱ�ʹ���
					Section st = new Section(arg, ' ');
					args = st.toStringArray();
				}
			}
		}
		if (args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				arg = args[i].toLowerCase();
				if (arg.equalsIgnoreCase("com.raqsoft.ide.dfx.DFX")) {
					// ��bat�򿪵��ļ��������������ǲ���
					continue;
				}
				if (!arg.startsWith("-")) {
					if (!StringUtils.isValidString(openDfx)) {
						openDfx = args[i];
					}
				} else if (arg.startsWith("-help") || arg.startsWith("-?")) {
					Logger.debug(usage);
					System.exit(0);
				}
			}
		}
		String sTmp, sPath;
		sTmp = System.getProperty("java.version");
		sPath = System.getProperty("java.home");

		MessageManager mm = IdeCommonMessage.get();
		if (sTmp.compareTo("1.4.1") < 0) {
			String t1 = mm.getMessage("prjx.jdkversion", "", sPath, sTmp);
			String t2 = mm.getMessage("public.prompt");
			JOptionPane.showMessageDialog(null, t1, t2,
					JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}
		return openDfx;
	}

	/**
	 * �ɹ�API���õĳ���������
	 * 
	 * @param args
	 * @return
	 * @throws Throwable
	 */
	public static DFX main0(String args[]) throws Throwable {
		String openRaq = prepareEnv(args);
		DFX frame = new DFX(openRaq);
		frame.setSize(Toolkit.getDefaultToolkit().getScreenSize());
		frame.setExtendedState(MAXIMIZED_BOTH);
		return frame;
	}

	/**
	 * ����������
	 * 
	 * @param args
	 *            JVM����
	 */
	public static void main(final String args[]) {
		mainInit();
		SwingUtilities.invokeLater(new Thread() {
			public void run() {
				initLNF();
				try {
					// ��ǰ��Ʒ����ʱ�����õ�ǰ�м�飬������try���棬�쳣���˳�
					String openFile = prepareEnv(args);
					DFX frame = new DFX(openFile);
					showFrame(frame);
				} catch (Throwable t) {
					t.printStackTrace();
					try {
						GM.showException(t);
					} catch (Exception e) {
					}
					System.exit(0);
				}
			}
		});
	}

	/**
	 * ��ʼ�������
	 */
	public static void mainInit() {
		resetInstallDirectories();
		GMDfx.setOptionLocale();
		try {
			GV.config = ConfigUtilIde.loadConfig(true);
		} catch (Throwable e) {
			GM.outputMessage(e);
		}
		if (GV.config == null)
			GV.config = new RaqsoftConfig();
		try {
			ConfigOptions.load();
		} catch (Throwable e) {
			GM.outputMessage(e);
		}
		GMDfx.setOptionLocale();

		ConfigFile sysConfig = ConfigFile.getSystemConfigFile();
		if (sysConfig != null) {
			// ����ʾsplashͼƬ�����ӹ�������ͬһ��������
			String splashFile = sysConfig.getAttrValue("splashFile");
			if (StringUtils.isValidString(splashFile)) {
				splashFile = GM.getAbsolutePath(splashFile);
			} else {
				splashFile = GC.IMAGES_PATH + "esproc" + GM.getLanguageSuffix()
						+ ".png";
			}
			splashWindow = new DialogSplash(splashFile);
			splashWindow.setVisible(true);
		}

		if (GV.config != null) {
			try {
				ConfigUtil.loadExtLibs(System.getProperty("start.home"),
						GV.config);
			} catch (Throwable t) {
				GM.outputMessage(t);
			}
		}

		try {
			if (sysConfig != null) {
				// ��ϵͳ�����ж�ȡ������ɫ��͸����
				ConfigOptions.fileColor = sysConfig.getAttrValue("fileColor");
				ConfigOptions.fileColorOpacity = sysConfig
						.getAttrValue("fileColorOpacity");
				ConfigOptions.headerColor = sysConfig
						.getAttrValue("headerColor");
				ConfigOptions.headerColorOpacity = sysConfig
						.getAttrValue("headerColorOpacity");
				ConfigOptions.cellColor = sysConfig.getAttrValue("cellColor");
				ConfigOptions.cellColorOpacity = sysConfig
						.getAttrValue("cellColorOpacity");
			}
		} catch (Throwable e) {
			GM.outputMessage(e);
		}
	}

	/**
	 * ��ʼ�������ʽ
	 */
	public static void initLNF() {
		try {
			boolean isHighVersionJDK = false;
			String javaVersion = System.getProperty("java.version");
			if (javaVersion.compareTo("1.9") > 0) {
				isHighVersionJDK = true;
			}
			if (!isHighVersionJDK) {
				UIManager.setLookAndFeel(LookAndFeelManager
						.getLookAndFeelName());
				if (GM.isMacOS()) {
					UIManager.put("ColorChooserUI",
							"javax.swing.plaf.basic.BasicColorChooserUI");
				}
			} else {
				UIManager.setLookAndFeel(UIManager
						.getSystemLookAndFeelClassName());
			}
			initGlobalFontSetting(new Font("Dialog", Font.PLAIN, 12));
		} catch (Throwable x) {
			GM.outputMessage(x);
		}
	}

	/**
	 * ��ʾIDE�����
	 * 
	 * @param frame
	 */
	public static void showFrame(DFX frame) {
		String port = GMDfx.getConfigValue("esproc_port");
		if (StringUtils.isValidString(port)) {
			int iport = -1001;
			try {
				iport = Integer.parseInt(port);
			} catch (Exception e1) {
				Logger.debug("Invalid esproc_port: " + port);
			}
			if (iport != -1001)
				new TcpServer(iport, frame).start();
		}
		frame.setSize(java.awt.Toolkit.getDefaultToolkit().getScreenSize());
		frame.setExtendedState(MAXIMIZED_BOTH);
		frame.setVisible(true);
		if (splashWindow != null) {
			splashWindow.closeWindow();
		}
		frame.startAutoRecent();
	}

	/**
	 * ȡ������
	 * 
	 * @return
	 */
	public PanelConsole getPanelConsole() {
		return tabParam.getPanelConsole();
	}

	/**
	 * ���ڼ����¼�
	 * 
	 * @param e
	 */
	protected void this_windowActivated(WindowEvent e) {
		GV.appFrame = this;
		GV.appMenu = currentMenu;
		GV.appMenu.resetLiveMenu();
		GV.appMenu.resetPrivilegeMenu();

		// ����ʱ�鿴�Ƿ����ⲿ�ĸ��ƣ�����У�������ڲ�������
		if (GV.cellSelection != null) {
			Object clip = GV.cellSelection.systemClip;
			if (clip != null && !clip.equals(GM.clipBoard())) {
				GV.cellSelection = null;
			}
		}
		GM.resetClipBoard();
		checkUpdate();
	}

	/** ֻ�ڵ�һ�μ���ʱ������ */
	private boolean isCheckUpdate = false;

	/**
	 * ������
	 */
	protected void checkUpdate() {
		if (!isCheckUpdate) {
			isCheckUpdate = true;
			Thread cu = new Thread() {
				public void run() {
					try {
						UpdateManager.checkUpdate(true);
					} catch (final Exception ex) {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								GM.showException(ex, true);
							}
						});
					}
				}
			};
			cu.start();
		}
	}

	/**
	 * �������ڹر�
	 * 
	 * @param e
	 */
	void this_windowClosing(WindowEvent e) {
		this.update(this.getGraphics());
		if (!closeAll()) {
			this.setDefaultCloseOperation(DFX.DO_NOTHING_ON_CLOSE);
			return;
		}
		if (!exit()) {
			this.setDefaultCloseOperation(DFX.DO_NOTHING_ON_CLOSE);
			return;
		} else {
			this.setDefaultCloseOperation(DFX.DISPOSE_ON_CLOSE);
		}
	}

	/**
	 * ȡ��Ʒ����
	 */
	public String getProductName() {
		return IdeDfxMessage.get().getMessage("dfx.productname");
	}
}

class PRJX_this_windowAdapter extends java.awt.event.WindowAdapter {
	DFX adaptee;

	PRJX_this_windowAdapter(DFX adaptee) {
		this.adaptee = adaptee;
	}

	public void windowActivated(WindowEvent e) {
		adaptee.this_windowActivated(e);
	}

	public void windowClosing(WindowEvent e) {
		adaptee.this_windowClosing(e);
	}
}