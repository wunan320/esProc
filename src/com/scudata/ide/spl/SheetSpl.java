package com.scudata.ide.spl;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import com.scudata.app.common.AppConsts;
import com.scudata.app.common.AppUtil;
import com.scudata.cellset.ICellSet;
import com.scudata.cellset.INormalCell;
import com.scudata.cellset.datamodel.CellSet;
import com.scudata.cellset.datamodel.Command;
import com.scudata.cellset.datamodel.NormalCell;
import com.scudata.cellset.datamodel.PgmCellSet;
import com.scudata.cellset.datamodel.PgmNormalCell;
import com.scudata.common.ByteMap;
import com.scudata.common.CellLocation;
import com.scudata.common.IByteMap;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.common.StringUtils;
import com.scudata.common.UUID;
import com.scudata.dm.Context;
import com.scudata.dm.DfxManager;
import com.scudata.dm.Env;
import com.scudata.dm.FileObject;
import com.scudata.dm.JobSpace;
import com.scudata.dm.JobSpaceManager;
import com.scudata.dm.Param;
import com.scudata.dm.ParamList;
import com.scudata.dm.Sequence;
import com.scudata.expression.Expression;
import com.scudata.expression.Node;
import com.scudata.expression.fn.Call;
import com.scudata.expression.fn.Func;
import com.scudata.expression.fn.Func.CallInfo;
import com.scudata.ide.common.CellSetTxtUtil;
import com.scudata.ide.common.ConfigFile;
import com.scudata.ide.common.ConfigOptions;
import com.scudata.ide.common.DataSource;
import com.scudata.ide.common.GC;
import com.scudata.ide.common.GM;
import com.scudata.ide.common.GV;
import com.scudata.ide.common.IAtomicCmd;
import com.scudata.ide.common.IPrjxSheet;
import com.scudata.ide.common.PrjxAppMenu;
import com.scudata.ide.common.control.CellRect;
import com.scudata.ide.common.control.IEditorListener;
import com.scudata.ide.common.control.PanelConsole;
import com.scudata.ide.common.dialog.DialogArgument;
import com.scudata.ide.common.dialog.DialogCellSetProperties;
import com.scudata.ide.common.dialog.DialogEditConst;
import com.scudata.ide.common.dialog.DialogInputArgument;
import com.scudata.ide.common.dialog.DialogInputPassword;
import com.scudata.ide.common.dialog.DialogRowHeight;
import com.scudata.ide.common.dialog.DialogSQLEditor;
import com.scudata.ide.common.dialog.DialogSelectDataSource;
import com.scudata.ide.common.resources.IdeCommonMessage;
import com.scudata.ide.custom.Server;
import com.scudata.ide.spl.control.ContentPanel;
import com.scudata.ide.spl.control.ControlUtils;
import com.scudata.ide.spl.control.EditControl;
import com.scudata.ide.spl.control.SplControl;
import com.scudata.ide.spl.control.SplEditor;
import com.scudata.ide.spl.dialog.DialogExecCmd;
import com.scudata.ide.spl.dialog.DialogFTP;
import com.scudata.ide.spl.dialog.DialogOptionPaste;
import com.scudata.ide.spl.dialog.DialogOptions;
import com.scudata.ide.spl.dialog.DialogPassword;
import com.scudata.ide.spl.dialog.DialogSearch;
import com.scudata.ide.spl.resources.IdeSplMessage;
import com.scudata.resources.EngineMessage;
import com.scudata.util.CellSetUtil;

/**
 * ������spl�ļ��༭����
 *
 */
public class SheetSpl extends IPrjxSheet implements IEditorListener {
	private static final long serialVersionUID = 1L;
	/**
	 * ����ؼ�
	 */
	public SplControl splControl = null;
	/**
	 * ����༭��
	 */
	public SplEditor splEditor = null;

	/**
	 * �Ҽ������˵�
	 */
	private PopupSpl popupSpl = null;

	/**
	 * �ļ�·��
	 */
	private String filePath = null;

	/**
	 * ������
	 */
	private Context splCtx = new Context();

	/**
	 * ����ִ��ʱ���ӳ��������ǵ�Ԫ������ֵ��ִ��ʱ�䣨���룩
	 */
	private Map<String, Long> debugTimeMap = new HashMap<String, Long>();

	/**
	 * ����ִ�еĵ�Ԫ������
	 */
	private transient CellLocation exeLocation = null;

	/**
	 * ����ѡ���״̬
	 */
	public byte selectState = GCSpl.SELECT_STATE_NONE;

	/**
	 * �Ƿ���Զ�̷������ϵ��ļ�
	 */
	public boolean isServerFile = false;

	/**
	 * �������Ե���Ϣ
	 */
	public StepInfo stepInfo = null;

	/**
	 * ���������Ƿ��ж���
	 */
	public boolean isStepStop = false;
	/**
	 * �Ƿ��ж���������
	 */
	public boolean stepStopOther = false;

	/**
	 * ���캯��
	 * 
	 * @param filePath �ļ�·��
	 * @param cs       �������
	 * @throws Exception
	 */
	public SheetSpl(String filePath, PgmCellSet cs) throws Exception {
		this(filePath, cs, null);
	}

	/**
	 * ���캯��
	 * 
	 * @param filePath �ļ�·��
	 * @param cs       �������
	 * @param stepInfo �������Ե���Ϣ
	 * @throws Exception
	 */
	public SheetSpl(String filePath, PgmCellSet cs, StepInfo stepInfo) throws Exception {
		super(filePath);
		this.stepInfo = stepInfo;
		if (stepInfo != null) {
			this.sheets = stepInfo.sheets;
			this.sheets.add(this);
		}
		if (stepInfo != null && cs != null) {
			splCtx = cs.getContext();
		}
		try {
			ImageIcon image = GM.getLogoImage(true);
			final int size = 20;
			image.setImage(image.getImage().getScaledInstance(size, size, Image.SCALE_DEFAULT));
			setFrameIcon(image);
		} catch (Throwable t) {
		}
		this.filePath = filePath;
		splEditor = new SplEditor(splCtx) {
			public PgmCellSet generateCellSet(int rows, int cols) {
				return new PgmCellSet(rows, cols);
			}

		};
		this.splControl = splEditor.getComponent();
		splControl.setSplScrollBarListener();
		splEditor.addSplListener(this);
		if (stepInfo != null) {
			INormalCell currentCell = cs.getCurrent();
			if (currentCell == null) {
				setExeLocation(stepInfo.startLocation);
			} else {
				setExeLocation(new CellLocation(currentCell.getRow(), currentCell.getCol()));
			}
			splControl.contentView.setEditable(false);
		}
		loadBreakPoints();
		if (cs != null) {
			splEditor.setCellSet(cs);
		}

		setTitle(this.filePath);
		popupSpl = new PopupSpl();

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(splEditor.getComponent(), BorderLayout.CENTER);
		addInternalFrameListener(new Listener(this));
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		if (!isSubSheet()) {
			splCtx = new Context();
			Context pCtx = GMSpl.prepareParentContext();
			splCtx.setParent(pCtx);
			splControl.cellSet.setContext(splCtx);
			splControl.cellSet.reset();
		}

	}

	/**
	 * �Ƿ�ETL�༭
	 * 
	 * @return
	 */
	public boolean isETL() {
		return false;
	}

	/**
	 * ȡ�����������
	 * 
	 * @return
	 */
	public Context getSplContext() {
		return splCtx;
	}

	/**
	 * ���δ�ʱѡ���һ����Ԫ��
	 */
	private boolean isInitSelect = true;

	/**
	 * ѡ���һ����Ԫ��
	 */
	public void selectFirstCell() {
		if (stepInfo != null)
			return;
		if (isInitSelect) {
			isInitSelect = false;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					splEditor.selectFirstCell();
					selectState = GC.SELECT_STATE_CELL;
					refresh();
				}
			});
		}
	}

	/**
	 * ���������ı��ļ�
	 * 
	 * @return
	 */
//	public boolean exportTxt() {
//		File oldFile = new File(filePath);
//		String oldFileName = oldFile.getName();
//		int index = oldFileName.lastIndexOf(".");
//		if (index > 0) {
//			oldFileName = oldFileName.substring(0, index + 1);
//			oldFileName += AppConsts.FILE_SPL;
//		}
//		File f = GM.dialogSelectFile(AppConsts.FILE_SPL, GV.lastDirectory,
//				IdeSplMessage.get().getMessage("public.export"), oldFileName, GV.appFrame);
//		if (f == null)
//			return false;
//		if (f.exists() && !f.canWrite()) {
//			JOptionPane.showMessageDialog(GV.appFrame, IdeCommonMessage.get().getMessage("public.readonly", filePath));
//			return false;
//		}
//		String filePath = f.getAbsolutePath();
//		try {
//			AppUtil.writeSPLFile(filePath, splControl.cellSet);
//		} catch (Throwable e) {
//			GM.showException(e);
//			return false;
//		}
//		JOptionPane.showMessageDialog(GV.appFrame, IdeSplMessage.get().getMessage("public.exportsucc", filePath));
//		return true;
//	}

	/**
	 * ����
	 */
	public boolean save() {
		if (isServerFile) { // Զ���ļ��ı���
			String serverName = filePath.substring(0, filePath.indexOf(':'));
			if (StringUtils.isValidString(serverName)) {
				Server server = GV.getServer(serverName);
				if (server != null) {
					try {
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						if (filePath.toLowerCase().endsWith("." + AppConsts.FILE_SPL)) {
							String cellSetStr = CellSetUtil.toString(splControl.cellSet);
							out.write(cellSetStr.getBytes());
						} else {
							CellSetUtil.writePgmCellSet(out, splControl.cellSet);
						}
						String fileName = filePath.substring(filePath.indexOf(':') + 1).replaceAll("\\\\", "/");
						if (fileName.startsWith("/")) {
							fileName = fileName.substring(1);
						}
						server.save(fileName, out.toByteArray());
					} catch (Exception e) {
						GM.showException(e);
						return false;
					}
				}
			}
		} else if (GMSpl.isNewGrid(filePath, GCSpl.PRE_NEWPGM) || !AppUtil.isSPLFile(filePath)) { // �½�
			boolean hasSaveAs = saveAs();
			if (hasSaveAs) {
				storeBreakPoints();
				if (stepInfo != null && isStepStop) { // ����֮������������
					stepInfo = null;
					isStepStop = false;
					stepStopOther = false;
					if (sheets != null)
						sheets.remove(this);
					sheets = null; // ��ǰ������֮ǰ�ĵ�������û�й�����
					resetRunState();
				}
			}
			return hasSaveAs;
		} else {
			File f = new File(filePath);
			if (f.exists() && !f.canWrite()) {
				JOptionPane.showMessageDialog(GV.appFrame,
						IdeCommonMessage.get().getMessage("public.readonly", filePath));
				return false;
			}

			try {
				if (ConfigOptions.bAutoBackup.booleanValue()) {
					String saveFile = filePath + ".bak";
					File fb = new File(saveFile);
					fb.delete();
					f.renameTo(fb);
				}
				GVSpl.panelValue.setCellSet((PgmCellSet) splControl.cellSet);
				if (filePath.toLowerCase().endsWith("." + AppConsts.FILE_SPL)) {
					AppUtil.writeSPLFile(filePath, splControl.cellSet);
				} else {
					CellSetUtil.writePgmCellSet(filePath, splControl.cellSet);
				}
				DfxManager.getInstance().clear();
				((PrjxAppMenu) GV.appMenu).refreshRecentFile(filePath);
			} catch (Throwable e) {
				GM.showException(e);
				return false;
			}
		}

		GM.setCurrentPath(filePath);
		splEditor.setDataChanged(false);
		splEditor.getSplListener().commandExcuted();
		return true;
	}

	/**
	 * ����Ϊ
	 */
	public boolean saveAs() {
		boolean isSplFile = AppUtil.isSPLFile(filePath);
		boolean isNewFile = GMSpl.isNewGrid(filePath, GCSpl.PRE_NEWPGM) || !isSplFile;
		String fileExt = AppConsts.FILE_SPLX;
		if (isSplFile) {
			int index = filePath.lastIndexOf(".");
			fileExt = filePath.substring(index + 1);
		}
		String path = filePath;
		if (stepInfo != null && isStepStop) {
			if (StringUtils.isValidString(stepInfo.filePath))
				path = stepInfo.filePath;
		}
		// if (AppUtil.isSPLFile(path)) {
		// int index = path.lastIndexOf(".");
		// path = path.substring(0, index);
		// }
		File saveFile = GM.dialogSelectFile(AppConsts.SPL_FILE_EXTS, GV.lastDirectory,
				IdeCommonMessage.get().getMessage("public.saveas"), path, GV.appFrame);
		if (saveFile == null) {
			return false;
		}

		String sfile = saveFile.getAbsolutePath();
		GV.lastDirectory = saveFile.getParent();

		if (!AppUtil.isSPLFile(sfile)) {
			saveFile = new File(saveFile.getParent(), saveFile.getName() + "." + fileExt);
			sfile = saveFile.getAbsolutePath();
		}

		if (!GM.canSaveAsFile(sfile)) {
			return false;
		}
		if (!isNewFile) {
			storeBreakPoints(filePath, sfile);
		}
		changeFileName(sfile);
		return save();
	}

	/**
	 * ���浽FTP
	 */
	private void saveFTP() {
		if (!save())
			return;
		DialogFTP df = new DialogFTP();
		df.setFilePath(this.filePath);
		df.setVisible(true);
	}

	/**
	 * �޸��ļ���
	 */
	public void changeFileName(String newName) {
		GV.appMenu.removeLiveMenu(filePath);
		GV.appMenu.addLiveMenu(newName);
		this.filePath = newName;
		this.setTitle(newName);
		GV.toolWin.changeFileName(this, newName);
		((SPL) GV.appFrame).resetTitle();
	}

	/**
	 * ˢ��
	 */
	public void refresh() {
		refresh(false);
	}

	/**
	 * ˢ��
	 * 
	 * @param keyEvent �Ƿ񰴼��¼�
	 */
	private void refresh(boolean keyEvent) {
		refresh(keyEvent, true);
	}

	/**
	 * ˢ��
	 * 
	 * @param keyEvent       �Ƿ񰴼��¼�
	 * @param isRefreshState �Ƿ�ˢ��״̬
	 */
	private void refresh(boolean keyEvent, boolean isRefreshState) {
		if (splEditor == null) {
			return;
		}
		if (isClosed()) {
			return;
		}
		if (!(GV.appMenu instanceof MenuSpl)) {
			return;
		}
		// Menu
		MenuSpl md = (MenuSpl) GV.appMenu;
		md.setEnable(md.getMenuItems(), true);

		boolean isDataChanged = splEditor.isDataChanged();
		md.setMenuEnabled(GCSpl.iSAVE, isDataChanged);
		md.setMenuEnabled(GCSpl.iSAVEAS, !isServerFile);
		md.setMenuEnabled(GCSpl.iSAVEALL, true);
		md.setMenuEnabled(GCSpl.iSAVE_FTP, !isServerFile);

		md.setMenuEnabled(GCSpl.iREDO, splEditor.canRedo());
		md.setMenuEnabled(GCSpl.iUNDO, splEditor.canUndo());

		boolean canCopy = selectState != GCSpl.SELECT_STATE_NONE && true;
		md.setMenuEnabled(GCSpl.iCOPY, canCopy);
		md.setMenuEnabled(GCSpl.iCOPYVALUE, canCopy);
		md.setMenuEnabled(GCSpl.iCODE_COPY, canCopy);
		md.setMenuEnabled(GCSpl.iCOPY_HTML, canCopy);
		md.setMenuEnabled(GCSpl.iCUT, canCopy);

		md.setMenuEnabled(GCSpl.iMOVE_COPY_UP, selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iMOVE_COPY_DOWN, selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iMOVE_COPY_LEFT, selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iMOVE_COPY_RIGHT, selectState != GCSpl.SELECT_STATE_NONE);

		boolean canPaste = GMSpl.canPaste() && selectState != GCSpl.SELECT_STATE_NONE;
		md.setMenuEnabled(GCSpl.iPASTE, canPaste);
		md.setMenuEnabled(GCSpl.iPASTE_ADJUST, canPaste);
		md.setMenuEnabled(GCSpl.iPASTE_SPECIAL, canPaste);

		md.setMenuEnabled(GCSpl.iCTRL_ENTER, selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iDUP_ROW, selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iDUP_ROW_ADJUST, selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iCTRL_INSERT, selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iALT_INSERT, selectState != GCSpl.SELECT_STATE_NONE);

		md.setMenuEnabled(GCSpl.iCLEAR, selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iFULL_CLEAR, selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iBREAKPOINTS, selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iDELETE_ROW, selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iDELETE_COL, selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iCTRL_BACK, selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iCTRL_DELETE, selectState != GCSpl.SELECT_STATE_NONE);

		md.setMenuRowColEnabled(selectState == GCSpl.SELECT_STATE_ROW || selectState == GCSpl.SELECT_STATE_COL);
		md.setMenuVisible(GCSpl.iROW_HEIGHT, selectState == GCSpl.SELECT_STATE_ROW);
		md.setMenuVisible(GCSpl.iROW_ADJUST, selectState == GCSpl.SELECT_STATE_ROW);
		md.setMenuVisible(GCSpl.iROW_HIDE, selectState == GCSpl.SELECT_STATE_ROW);
		md.setMenuVisible(GCSpl.iROW_VISIBLE, selectState == GCSpl.SELECT_STATE_ROW);

		md.setMenuVisible(GCSpl.iCOL_WIDTH, selectState == GCSpl.SELECT_STATE_COL);
		md.setMenuVisible(GCSpl.iCOL_ADJUST, selectState == GCSpl.SELECT_STATE_COL);
		md.setMenuVisible(GCSpl.iCOL_HIDE, selectState == GCSpl.SELECT_STATE_COL);
		md.setMenuVisible(GCSpl.iCOL_VISIBLE, selectState == GCSpl.SELECT_STATE_COL);

		md.setMenuEnabled(GCSpl.iTEXT_EDITOR, selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iNOTE, selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iTIPS, selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iSEARCH, selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iREPLACE, selectState != GCSpl.SELECT_STATE_NONE);

		md.setMenuEnabled(GCSpl.iEDIT_CHART, selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iFUNC_ASSIST, selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iSHOW_VALUE, selectState != GCSpl.SELECT_STATE_NONE);
		md.setMenuEnabled(GCSpl.iCLEAR_VALUE, selectState != GCSpl.SELECT_STATE_NONE);

		md.setMenuEnabled(GCSpl.iDRAW_CHART, GVSpl.panelValue.tableValue.canDrawChart());
		md.setMenuVisible(GCSpl.iEDIT_CHART, true);
		md.setMenuVisible(GCSpl.iDRAW_CHART, true);
		// Toolbar
		GVSpl.appTool.setBarEnabled(true);
		GVSpl.appTool.setButtonEnabled(GCSpl.iSAVE, isDataChanged);
		GVSpl.appTool.setButtonEnabled(GCSpl.iCLEAR, selectState != GCSpl.SELECT_STATE_NONE);
		GVSpl.appTool.setButtonEnabled(GCSpl.iBREAKPOINTS, selectState != GCSpl.SELECT_STATE_NONE && !isStepStop);
		GVSpl.appTool.setButtonEnabled(GCSpl.iUNDO, splEditor.canUndo());
		GVSpl.appTool.setButtonEnabled(GCSpl.iREDO, splEditor.canRedo());

		if (splEditor != null && selectState != GCSpl.SELECT_STATE_NONE) {
			NormalCell nc = splEditor.getDisplayCell();
			boolean lockOtherCell = false;
			if (nc != null) {
				IByteMap values = splEditor.getProperty();
				GV.toolBarProperty.refresh(selectState, values);
				Object value = nc.getValue();
				GVSpl.panelValue.tableValue.setCellId(nc.getCellId());
				String oldId = GVSpl.panelValue.tableValue.getCellId();
				if (nc.getCellId().equals(oldId)) { // refresh
					GVSpl.panelValue.tableValue.setValue1(value, nc.getCellId());
				} else {
					lockOtherCell = true;
					GVSpl.panelValue.tableValue.setValue(value);
				}
				GVSpl.panelValue.setDebugTime(debugTimeMap.get(nc.getCellId()));
			}

			if (lockOtherCell && GVSpl.panelValue.tableValue.isLocked()) {
				String cellId = GVSpl.panelValue.tableValue.getCellId();
				if (StringUtils.isValidString(cellId)) {
					try {
						INormalCell lockCell = splControl.cellSet.getCell(cellId);
						Object oldVal = GVSpl.panelValue.tableValue.getOriginalValue();
						Object newVal = lockCell.getValue();
						boolean isValChanged = false;
						if (oldVal == null) {
							isValChanged = newVal != null;
						} else {
							isValChanged = !oldVal.equals(newVal);
						}
						if (isValChanged)
							GVSpl.panelValue.tableValue.setValue1(newVal, cellId);
					} catch (Exception e) {
					}
				}
			}
		}

		GV.toolBarProperty.setEnabled(selectState != GCSpl.SELECT_STATE_NONE);

		GVSpl.tabParam.resetParamList(splCtx.getParamList());

		if (GVSpl.panelValue.tableValue.isLocked1()) {
			GVSpl.panelValue.tableValue.setLocked1(false);
		}

		if (splControl.cellSet.getCurrentPrivilege() != PgmCellSet.PRIVILEGE_FULL) {
			md.setEnable(md.getMenuItems(), false);
			md.setMenuEnabled(GCSpl.iSAVE, isDataChanged);

			md.setMenuEnabled(GCSpl.iPROPERTY, true);
			md.setMenuEnabled(GCSpl.iCONST, false);
			md.setMenuEnabled(GCSpl.iPASSWORD, true);

			GVSpl.appTool.setBarEnabled(false);
			GVSpl.appTool.setButtonEnabled(GCSpl.iSAVE, isDataChanged);

			GV.toolBarProperty.setEnabled(false);
		}

		boolean canShow = false;
		if (GV.useRemoteServer && GV.fileTree != null && GV.fileTree.getServerList() != null
				&& GV.fileTree.getServerList().size() > 0) {
			canShow = true;
		}
		md.setMenuEnabled(GCSpl.iREMOTE_SERVER_LOGOUT, canShow);
		md.setMenuEnabled(GCSpl.iREMOTE_SERVER_DATASOURCE, canShow);
		md.setMenuEnabled(GCSpl.iREMOTE_SERVER_UPLOAD_FILE, canShow);

		md.setMenuEnabled(GCSpl.iVIEW_CONSOLE, ConfigOptions.bIdeConsole.booleanValue());
		if (stepInfo != null) {
			// �жϵ��������Ժ�,��ǰ������call(spl)ʱ�˵�����
			if (!isStepStopCall()) {
				md.setMenuEnabled(md.getAllMenuItems(), false);
				GVSpl.appTool.setButtonEnabled(GCSpl.iCLEAR, false);
				GVSpl.appTool.setButtonEnabled(GCSpl.iBREAKPOINTS, false);
				GV.toolBarProperty.setEnabled(false);
			}
		}
		resetRunState(isRefreshState, false);
		md.resetPasswordMenu(splControl.cellSet.getCurrentPrivilege() == PgmCellSet.PRIVILEGE_FULL);
	}

	/**
	 * �Ƿ񵥲�����ֹͣ
	 * 
	 * @return
	 */
	private boolean isStepStopCall() {
		if (stepInfo == null)
			return false;
		return isStepStop && stepInfo.parentCall != null;
	}

	/**
	 * ȡ����
	 */
	public String getSheetTitle() {
		return getFileName();
	}

	/**
	 * ���ñ���
	 */
	public void setSheetTitle(String filePath) {
		this.filePath = filePath;
		setTitle(filePath);
		this.repaint();
	}

	/**
	 * ȡ�ļ�·��
	 */
	public String getFileName() {
		return filePath;
	}

	/**
	 * �Զ������߳�
	 */
	private CalcCellThread calcCellThread = null;

	/**
	 * ���㵱ǰ��
	 */
	public void calcActiveCell() {
		calcActiveCell(true);
	}

	/**
	 * ���㵱ǰ��
	 * 
	 * @param lock �Ƿ����
	 */
	public void calcActiveCell(boolean lock) {
		splControl.getContentPanel().submitEditor();
		splControl.getContentPanel().requestFocus();
		CellLocation cl = splControl.getActiveCell();
		if (cl == null)
			return;
		if (GVSpl.appFrame instanceof SPL) {
			PanelConsole pc = ((SPL) GVSpl.appFrame).getPanelConsole();
			if (pc != null)
				pc.autoClean();
		}
		calcCellThread = new CalcCellThread(cl);
		calcCellThread.start();
		if (lock)
			GVSpl.panelValue.tableValue.setLocked(true);
	}

	/**
	 * ��Ԫ�������߳�
	 *
	 */
	class CalcCellThread extends Thread {
		/**
		 * ��Ԫ������
		 */
		private CellLocation cl;

		/**
		 * ���캯��
		 * 
		 * @param cl ��Ԫ������
		 */
		public CalcCellThread(CellLocation cl) {
			this.cl = cl;
		}

		/**
		 * ִ�м���
		 */
		public void run() {
			try {
				int row = cl.getRow();
				int col = cl.getCol();
				splControl.setCalcPosition(new CellLocation(row, col));
				long t1 = System.currentTimeMillis();
				splControl.cellSet.runCell(row, col);
				long t2 = System.currentTimeMillis();
				String cellId = CellLocation.getCellId(row, col);
				debugTimeMap.put(cellId, t2 - t1);
				NormalCell nc = (NormalCell) splControl.cellSet.getCell(row, col);
				if (nc != null) {
					Object value = nc.getValue();
					GVSpl.panelValue.tableValue.setValue1(value, nc.getCellId());
				}
			} catch (Exception x) {
				String msg = x.getMessage();
				if (!StringUtils.isValidString(msg)) {
					StringBuffer sb = new StringBuffer();
					Throwable t = x.getCause();
					if (t != null) {
						sb.append(t.getMessage());
						sb.append("\r\n");
					}
					StackTraceElement[] ste = x.getStackTrace();
					for (int i = 0; i < ste.length; i++) {
						sb.append(ste[i]);
						sb.append("\r\n");
					}
					msg = sb.toString();
					showException(msg);
				} else {
					showException(x);
				}
			} finally {
				splControl.contentView.repaint();
				SwingUtilities.invokeLater(new Thread() {
					public void run() {
						refresh();
					}
				});
			}
		}
	}

	/**
	 * ���������Ի���
	 * 
	 * @param replace boolean �Ƿ����滻�Ի���
	 */
	public void dialogSearch(boolean replace) {
		if (GVSpl.searchDialog != null) {
			GVSpl.searchDialog.setVisible(false);
		}
		GVSpl.searchDialog = new DialogSearch();
		GVSpl.searchDialog.setControl(splEditor, replace);
		GVSpl.searchDialog.setVisible(true);
	}

	/**
	 * ��ΪXML��Node�������������ֻ���������ſ�ͷ
	 * 
	 * @param name �ڵ���
	 * @return
	 */
	private String getBreakPointNodeName(String nodeName) {
		if (nodeName == null)
			return "";
		nodeName = nodeName.replaceAll("[^0-9a-zA-Z-._]", "_");
		return "_" + nodeName;
	}

	/**
	 * ���ضϵ�
	 */
	private void loadBreakPoints() {
		ConfigFile cf = null;
		try {
			cf = ConfigFile.getConfigFile();
			String oldNode = cf.getConfigNode();
			cf.setConfigNode(ConfigFile.NODE_BREAKPOINTS);
			String breaks = cf.getAttrValue(getBreakPointNodeName(filePath));
			if (StringUtils.isValidString(breaks)) {
				StringTokenizer token = new StringTokenizer(breaks, ";");
				ArrayList<CellLocation> breakPoints = new ArrayList<CellLocation>();
				while (token.hasMoreElements()) {
					String cellName = token.nextToken();
					CellLocation cp = new CellLocation(cellName);
					breakPoints.add(cp);
				}
				splEditor.getComponent().setBreakPoints(breakPoints);
				cf.setConfigNode(oldNode);
			}
		} catch (Throwable ex) {
		}
	}

	/**
	 * ��ֹ����ϵ�
	 */
	private boolean preventStoreBreak = false;

	/**
	 * ����ϵ�
	 */
	private void storeBreakPoints() {
		storeBreakPoints(null, filePath);
	}

	/**
	 * ����ϵ�
	 * 
	 * @param oldName  �ɽڵ���
	 * @param filePath ��·��
	 */
	private void storeBreakPoints(String oldName, String filePath) {
		if (preventStoreBreak) {
			return;
		}
		// δ����״̬������,*���Ų��ܵ� xml ��Key
		if (filePath.endsWith("*")) {
			return;
		}

		if (GMSpl.isNewGrid(filePath, GCSpl.PRE_NEWPGM) || !AppUtil.isSPLFile(filePath)) {
			return;
		}

		ConfigFile cf = null;
		String oldNode = null;
		try {
			cf = ConfigFile.getConfigFile();
			oldNode = cf.getConfigNode();
			cf.setConfigNode(ConfigFile.NODE_BREAKPOINTS);
			ArrayList<CellLocation> breaks = splEditor.getComponent().getBreakPoints();
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < breaks.size(); i++) {
				CellLocation cp = breaks.get(i);
				if (i > 0) {
					sb.append(";");
				}
				sb.append(cp.toString());
			}
			if (oldName != null)
				cf.setAttrValue(getBreakPointNodeName(oldName), "");
			cf.setAttrValue(getBreakPointNodeName(filePath), sb.toString());
			cf.save();
		} catch (Throwable ex) {
		} finally {
			cf.setConfigNode(oldNode);
		}
	}

	/**
	 * ���û���
	 */
	public void reset() {
		if (runThread != null) {
			terminate();
		}
		closeRunThread();
		debugTimeMap.clear();
		if (!isSubSheet()) {
			setExeLocation(null);
			splCtx = new Context();
			Context pCtx = GMSpl.prepareParentContext();
			splCtx.setParent(pCtx);
			splControl.cellSet.setContext(splCtx);
			splControl.cellSet.reset();
			closeSpace();
		}
		GVSpl.tabParam.resetParamList(null);
		GVSpl.panelSplWatch.watch(null);
	}

	/**
	 * ������߳���
	 */
	private ThreadGroup tg = null;
	/**
	 * �߳�����
	 */
	private int threadCount = 0;
	/**
	 * �����߳�
	 */
	private transient RunThread runThread = null;
	/**
	 * ����ռ�
	 */
	private JobSpace jobSpace;

	/**
	 * ִ��
	 */
	public void run() {
		if (!prepareStart()) {
			return;
		}
		if (stepInfo == null)
			if (jobSpace == null)
				return;
		beforeRun();
		threadCount++;
		synchronized (threadLock) {
			runThread = new RunThread(tg, "t" + threadCount, false);
			runThread.start();
		}
	}

	/**
	 * �Ƿ񵥲����Ե��Ӵ��ڡ�true�ǵ��Խ���򿪵��ļ���false���½����ߴ��ļ��򿪵��ļ�
	 * 
	 * @return
	 */
	private boolean isSubSheet() {
		return stepInfo != null;
	}

	/**
	 * ����ִ��
	 * 
	 * @param debugType ���Է�ʽ
	 */
	public void debug(byte debugType) {
		synchronized (threadLock) {
			if (runThread == null) {
				if (!prepareStart())
					return;
				if (!isSubSheet())
					if (jobSpace == null)
						return;
				beforeRun();
				threadCount++;
				runThread = new RunThread(tg, "t" + threadCount, true);
				runThread.setDebugType(debugType);
				runThread.start();
			} else {
				preventRun();
				runThread.continueRun(debugType);
			}
		}
	}

	/**
	 * ��ͣ���߼���ִ��
	 */
	public synchronized void pause() {
		synchronized (threadLock) {
			if (runThread == null)
				return;
			if (runThread.getRunState() == RunThread.PAUSED) {
				runThread.continueRun();
			} else {
				runThread.pause();
			}
		}
	}

	/**
	 * ִ�е�׼������
	 * 
	 * @return
	 */
	private boolean prepareStart() {
		try {
			preventRun();
			reset();
			if (!isSubSheet())
				if (!prepareArg())
					return false;
			if (stepInfo == null) {
				String uuid = UUID.randomUUID().toString();
				jobSpace = JobSpaceManager.getSpace(uuid);
				splCtx.setJobSpace(jobSpace);
			}
			tg = new ThreadGroup(filePath);
			threadCount = 0;
			return true;
		} catch (Throwable e) {
			GM.showException(e);
			resetRunState();
			return false;
		}
	}

	/**
	 * ִ��ǰ��������
	 */
	private void beforeRun() {
		splControl.contentView.submitEditor();
		splControl.contentView.initEditor(ContentPanel.MODE_HIDE);
		GVSpl.panelValue.tableValue.setValue(null);
		if (GVSpl.appFrame instanceof SPL) {
			PanelConsole pc = ((SPL) GVSpl.appFrame).getPanelConsole();
			if (pc != null)
				pc.autoClean();
		}
	}

	/**
	 * ���������Ƿ�������ļ�����ֹ2�δ�
	 */
	private boolean subSheetOpened = false;
	/**
	 * �������Ե���ҳ�б�����˳���
	 */
	public List<SheetSpl> sheets = null;

	/**
	 * ȡ��ҳ����
	 * 
	 * @return
	 */
	private SheetSpl getParentSheet() {
		if (sheets == null)
			return null;
		for (int i = 0; i < sheets.size(); i++) {
			if (sheets.get(i) == this) {
				if (i == 0)
					return null;
				else
					return sheets.get(i - 1);
			}
		}
		return null;
	}

	/**
	 * ȡ��ҳ����
	 * 
	 * @return
	 */
	private SheetSpl getSubSheet() {
		if (sheets == null)
			return null;
		for (int i = 0; i < sheets.size(); i++) {
			if (sheets.get(i) == this) {
				if (i == sheets.size() - 1)
					return null;
				else
					return sheets.get(i + 1);
			}
		}
		return null;
	}

	/**
	 * �������Ժ󣬽�����ҳִ�н��
	 * 
	 * @param returnVal   ����ֵ
	 * @param continueRun �Ƿ����ִ��
	 */
	public void acceptResult(final Object returnVal, final boolean continueRun) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					SheetSpl subSheet = getSubSheet();
					if (subSheet != null) {
						((SPL) GV.appFrame).closeSheet(subSheet, false);
					}
					if (exeLocation == null)
						return;
					PgmNormalCell lastCell = (PgmNormalCell) splControl.cellSet.getCell(exeLocation.getRow(),
							exeLocation.getCol());
					lastCell.setValue(returnVal);
					splControl.cellSet.setCurrent(lastCell);
					splControl.cellSet.setNext(exeLocation.getRow(), exeLocation.getCol() + 1, false);
					INormalCell nextCell = splControl.cellSet.getCurrent();
					if (nextCell != null)
						setExeLocation(new CellLocation(nextCell.getRow(), nextCell.getCol()));
					else {
						setExeLocation(null);
						synchronized (threadLock) {
							if (runThread != null)
								runThread.continueRun();
						}
					}
					splControl.contentView.repaint();
					GV.appFrame.showSheet(SheetSpl.this);
					subSheetOpened = false;
					if (continueRun) {
						synchronized (threadLock) {
							if (runThread != null)
								runThread.continueRun();
						}
					}
				} catch (Exception e) {
					GM.showException(e);
				}
			}
		});

	}

	/**
	 * ��������ֹͣ
	 * 
	 * @param stopOther �Ƿ�ֹͣ����ҳ
	 */
	public void stepStop(boolean stopOther) {
		stepStopOther = stopOther;
		debug(RunThread.STEP_STOP);
	}

	/**
	 * ִ���߳�
	 *
	 */
	class RunThread extends Thread {
		/**
		 * �Ƿ����ģʽ
		 */
		private boolean isDebugMode = true;

		/** ִ����� */
		static final byte FINISH = 0;
		/** ����ִ�� */
		static final byte RUN = 1;
		/** ��ִͣ�� */
		static final byte PAUSING = 2;
		/** ִ�б���ͣ�� */
		static final byte PAUSED = 3;
		/**
		 * ִ�е�״̬
		 */
		private Byte runState = FINISH;

		/** ����ִ�� */
		static final byte DEBUG = 1;
		/** ִ�е���� */
		static final byte CURSOR = 2;
		/** �������� */
		static final byte STEP_OVER = 3;
		/** �������Խ��� */
		static final byte STEP_INTO = 4;
		/** �������Է��� */
		static final byte STEP_RETURN = 5;
		/** ֻ�������̵߳ȴ���ʲô������ */
		static final byte STEP_INTO_WAIT = 6;
		/** �������Է��غ����ִ�� */
		static final byte STEP_RETURN1 = 7;
		/** ��������ֹͣ */
		static final byte STEP_STOP = 8;
		/**
		 * ���Է�ʽ
		 */
		private byte debugType = DEBUG;
		/**
		 * �Ƿ���ͣ��
		 */
		private Boolean isPaused = Boolean.FALSE;
		/**
		 * ��ǰ�������
		 */
		private CellLocation clCursor = null;
		/**
		 * ��ǰ�������
		 */
		private PgmCellSet curCellSet;

		/**
		 * ���캯��
		 * 
		 * @param tg          �߳���
		 * @param name        �߳�����
		 * @param isDebugMode �Ƿ����ģʽ
		 */
		public RunThread(ThreadGroup tg, String name, boolean isDebugMode) {
			super(tg, name);
			this.isDebugMode = isDebugMode;
			curCellSet = splControl.cellSet;
		}

		/**
		 * ִ��
		 */
		public void run() {
			runState = RUN;
			resetRunState();
			boolean isThreadDeath = false;
			boolean hasReturn = false;
			Object returnVal = null;
			try {
				do {
					synchronized (runState) {
						if (runState == PAUSING) {
							stepFinish();
							if (!GVSpl.panelSplWatch.isCalculating())
								GVSpl.panelSplWatch.watch(splControl.cellSet.getContext());
						}
					}
					while (isPaused) {
						try {
							sleep(5);
						} catch (Exception e) {
						}
					}

					if (debugType != STEP_INTO_WAIT) {
						long start = System.currentTimeMillis();
						PgmNormalCell pnc = null;
						if (exeLocation != null) {
							pnc = curCellSet.getPgmNormalCell(exeLocation.getRow(), exeLocation.getCol());
						} else if (curCellSet.getCurrent() != null) {
							INormalCell icell = curCellSet.getCurrent();
							pnc = curCellSet.getPgmNormalCell(icell.getRow(), icell.getCol());
						}
						if (pnc != null) {
							if (stepInfo != null && stepInfo.endRow > -1) {
								if (pnc.getRow() > stepInfo.endRow) {
									break;
								}
							}
						}
						// �������Խ���
						if (debugType == STEP_INTO) {
							if (!subSheetOpened) {
								if (pnc != null) {
									Expression exp = pnc.getExpression();
									if (exp != null) {
										Node home = exp.getHome();
										if (home instanceof Call) { // call����
											Call call = (Call) home;
											PgmCellSet subCellSet = call.getCallPgmCellSet(splCtx);
											subCellSet.setCurrent(subCellSet.getPgmNormalCell(1, 1));
											subCellSet.setNext(1, 1, true); // ���Ӹ�ʼִ��
											openSubSheet(pnc, subCellSet, null, null, -1, call);
										} else if (home instanceof Func) { // Func��
											// Funcʹ��������Ϊ��֧�ֵݹ�
											Func func = (Func) home;
											CallInfo ci = func.getCallInfo(splCtx);
											PgmCellSet cellSet = ci.getPgmCellSet();
											int row = ci.getRow();
											int col = ci.getCol();
											Object[] args = ci.getArgs();
											int rc = cellSet.getRowCount();
											int cc = cellSet.getColCount();
											if (row < 1 || row > rc || col < 1 || col > cc) {
												MessageManager mm = EngineMessage.get();
												throw new RQException(mm.getMessage("engine.callNeedSub"));
											}

											PgmNormalCell nc = cellSet.getPgmNormalCell(row, col);
											Command command = nc.getCommand();
											if (command == null || command.getType() != Command.FUNC) {
												MessageManager mm = EngineMessage.get();
												throw new RQException(mm.getMessage("engine.callNeedSub"));
											}

											// ������������ĸ���
											PgmCellSet pcs = cellSet.newCalc();
											int endRow = cellSet.getCodeBlockEndRow(row, col);
											for (int r = row; r <= endRow; ++r) {
												for (int c = col; c <= cc; ++c) {
													INormalCell tmp = cellSet.getCell(r, c);
													INormalCell cellClone = (INormalCell) tmp.deepClone();
													cellClone.setCellSet(pcs);
													pcs.setCell(r, c, cellClone);
												}
											}
											int colCount = pcs.getColCount();

											// �Ѳ���ֵ�赽func��Ԫ���ϼ�����ĸ���
											if (args != null) {
												int paramRow = row;
												int paramCol = col;
												for (int i = 0, pcount = args.length; i < pcount; ++i) {
													pcs.getPgmNormalCell(paramRow, paramCol).setValue(args[i]);
													if (paramCol < colCount) {
														paramCol++;
													} else {
														break;
													}
												}
											}
											pcs.setCurrent(pcs.getPgmNormalCell(row, col));
											pcs.setNext(row, col + 1, false); // ���Ӹ�ʼִ��
											openSubSheet(pnc, pcs, ci, new CellLocation(row, col + 1), endRow, null);
										}
										final SheetSpl subSheet = getSubSheet();
										if (subSheet != null) {
											SwingUtilities.invokeLater(new Runnable() {
												public void run() {
													try {
														subSheet.debug(STEP_INTO_WAIT);
													} catch (Exception e) {
														GM.showException(e);
													}
												}
											});
										}
									}
								}
							} else {
								final SheetSpl subSheet = getSubSheet();
								if (subSheet != null) {
									SwingUtilities.invokeLater(new Runnable() {
										public void run() {
											try {
												((SPL) GV.appFrame).showSheet(subSheet);
											} catch (Exception e) {
											}
										}
									});
								}
							}
						} else if (debugType == STEP_RETURN) {
							isDebugMode = false; // һֱ����
							debugType = STEP_RETURN1;
						} else if (debugType == STEP_STOP) {
							isStepStop = true;
							if (stepStopOther) {
								if (sheets != null)
									for (SheetSpl sheet : sheets) {
										if (sheet != SheetSpl.this)
											sheet.stepStop(false);
									}
							}
							return; // ֱ�ӽ��������߳�
						} else {
							if (pnc == null) {
								exeLocation = curCellSet.runNext();
							} else {
								if (stepInfo != null && stepInfo.endRow > -1) {
									Command cmd = pnc.getCommand();
									if (cmd != null && cmd.getType() == Command.RETURN) {
										hasReturn = true;
										Expression exp1 = cmd.getExpression(curCellSet, splCtx);
										if (exp1 != null) {
											returnVal = exp1.calculate(splCtx);
										}
										break;
									}
								}
								exeLocation = curCellSet.runNext();
							}
						}
						if (isDebugMode && pnc != null) {
							long end = System.currentTimeMillis();
							String cellId = CellLocation.getCellId(pnc.getRow(), pnc.getCol());
							debugTimeMap.put(cellId, end - start);
						}
					}
					if (isDebugMode) {
						if (checkBreak()) {
							if (!GVSpl.panelSplWatch.isCalculating()) {
								GVSpl.panelSplWatch.watch(splControl.cellSet.getContext());
							}
							while (true) {
								if (isPaused) {
									try {
										sleep(5);
									} catch (Exception e) {
									}
								} else {
									break;
								}
							}
						}
					}
				} while (exeLocation != null);
			} catch (ThreadDeath td) {
				isThreadDeath = true;
			} catch (Throwable x) {
				if (x != null) {
					Throwable cause = x.getCause();
					if (cause != null && cause instanceof ThreadDeath) {
						isThreadDeath = true;
					}
				}
				if (!isThreadDeath) {
					String msg = x.getMessage();
					if (!StringUtils.isValidString(msg)) {
						StringBuffer sb = new StringBuffer();
						Throwable t = x.getCause();
						if (t != null) {
							sb.append(t.getMessage());
							sb.append("\r\n");
						}
						StackTraceElement[] ste = x.getStackTrace();
						for (int i = 0; i < ste.length; i++) {
							sb.append(ste[i]);
							sb.append("\r\n");
						}
						msg = sb.toString();
						showException(msg);
					} else {
						showException(x);
					}
				}
			} finally {
				runState = FINISH;
				if (!isThreadDeath)
					resetRunState(false, true);
				GVSpl.panelSplWatch.watch(splControl.cellSet.getContext());
				closeRunThread();
				// ������ӳ��򣬼�����ɺ�رյ�ǰ������ʾ��ҳ��
				SheetSpl parentSheet = getParentSheet();
				if (stepInfo != null && !isStepStop) { // ����ж��ӳ���Ͳ��ٷ���ֵ��
					if (!isThreadDeath) {
						if (returnVal == null && !hasReturn) {
							if (stepInfo.endRow > -1) {
								// δ����returnȱʡ���ش���������һ�������ֵ
								int endRow = stepInfo.endRow;
								CallInfo ci = stepInfo.callInfo;
								for (int r = endRow; r >= ci.getRow(); --r) {
									for (int c = curCellSet.getColCount(); c > ci.getCol(); --c) {
										PgmNormalCell cell = curCellSet.getPgmNormalCell(r, c);
										if (cell.isCalculableCell() || cell.isCalculableBlock()) {
											returnVal = cell.getValue();
										}
									}
								}
							} else {
								if (curCellSet.hasNextResult()) {
									returnVal = curCellSet.nextResult();
								}
							}
						}
					}
					if (parentSheet != null)
						parentSheet.acceptResult(returnVal, debugType == DEBUG);
				}
				if (!isStepStop) {
					if (sheets != null) {
						if (parentSheet == null) // ��������sheets
							sheets = null;
					}
				}
			}
		}

		/**
		 * �������Խ������ҳ
		 * 
		 * @param pnc           �������
		 * @param subCellSet    ���������
		 * @param ci            CallInfo����
		 * @param startLocation ������ʼ���������
		 * @param endRow        ����������
		 * @param call          Call����
		 */
		private void openSubSheet(PgmNormalCell pnc, final PgmCellSet subCellSet, CallInfo ci,
				CellLocation startLocation, int endRow, Call call) {
			String newName = new File(filePath).getName();
			if (AppUtil.isSPLFile(newName)) {
				int index = newName.lastIndexOf(".");
				newName = newName.substring(0, index);
			}
			String cellId = CellLocation.getCellId(pnc.getRow(), pnc.getCol());
			newName += "(" + cellId + ")";
			final String nn = newName;
			List<SheetSpl> sheets = SheetSpl.this.sheets;
			if (sheets == null) {
				sheets = new ArrayList<SheetSpl>();
				sheets.add(SheetSpl.this);
				SheetSpl.this.sheets = sheets;
			}
			final StepInfo stepInfo = new StepInfo(sheets);
			if (call != null) { // call spl
				stepInfo.filePath = call.getDfxPathName(splCtx);
			} else if (SheetSpl.this.stepInfo == null) { // ��ǰ��������
				stepInfo.filePath = filePath;
			} else { // ��ǰ���ӳ��򣬴���һ����ȡ
				stepInfo.filePath = SheetSpl.this.stepInfo.filePath;
			}
			stepInfo.splCtx = splCtx;
			stepInfo.parentLocation = new CellLocation(pnc.getRow(), pnc.getCol());
			stepInfo.callInfo = ci;
			stepInfo.startLocation = startLocation;
			stepInfo.endRow = endRow;
			stepInfo.parentCall = call;
			subSheetOpened = true;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					((SPL) GV.appFrame).openSheet(nn, subCellSet, false, stepInfo);
				}
			});
		}

		/**
		 * �����������
		 */
		private void stepFinish() {
			isPaused = Boolean.TRUE;
			runState = PAUSED;
			resetRunState(false, true);
		}

		/**
		 * ���ϵ�
		 * 
		 * @return
		 */
		private boolean checkBreak() {
			if (exeLocation == null)
				return false;
			if (debugType == STEP_INTO_WAIT || debugType == STEP_OVER || debugType == STEP_INTO) {
				stepFinish();
				if (ConfigOptions.bStepLastLocation.booleanValue()) {
					if (lastLocation != null) {
						SwingUtilities.invokeLater(new Thread() {
							public void run() {
								splEditor.selectCell(lastLocation.getRow(), lastLocation.getCol());
							}
						});
					}
				}
				return true;
			}
			if (splControl.isBreakPointCell(exeLocation.getRow(), exeLocation.getCol())) {
				stepFinish();
				return true;
			}
			if (debugType == CURSOR) {
				if (clCursor != null && exeLocation.equals(clCursor)) {
					stepFinish();
					return true;
				}
			}
			return false;
		}

		/**
		 * ��ͣ
		 */
		public void pause() {
			runState = PAUSING;
			resetRunState(false, false);
		}

		/**
		 * ȡִ��״̬
		 * 
		 * @return
		 */
		public byte getRunState() {
			return runState;
		}

		/**
		 * ���õ�������
		 * 
		 * @param debugType
		 */
		public void setDebugType(byte debugType) {
			this.debugType = debugType;
			if (debugType == CURSOR) {
				CellLocation activeCell = splControl.getActiveCell();
				if (activeCell != null)
					clCursor = new CellLocation(activeCell.getRow(), activeCell.getCol());
			}
		}

		/**
		 * �Ƿ����ģʽ
		 * 
		 * @return
		 */
		public boolean isDebugMode() {
			return isDebugMode;
		}

		/**
		 * ����ִ��
		 */
		public void continueRun() {
			continueRun(DEBUG);
		}

		/**
		 * ����ִ��
		 * 
		 * @param debugType ���Է�ʽ
		 */
		public void continueRun(byte debugType) {
			runState = RUN;
			setDebugType(debugType);
			resetRunState();
			isPaused = Boolean.FALSE;
		}

		/**
		 * �ر��߳�
		 */
		public void closeThread() {
			pause();
			closeResource();
		}
	}

	/**
	 * �ر�ҳ��������¼���ʱ���ر�����ռ�
	 */
	private void closeSpace() {
		if (jobSpace != null)
			JobSpaceManager.closeSpace(jobSpace.getID());
	}

	/**
	 * ������ɻ��жϺ���������ռ����Դ�������������
	 */
	private void closeResource() {
		if (jobSpace != null)
			jobSpace.closeResource();
	}

	/**
	 * �߳����쳣
	 * 
	 * @param ex
	 */
	private void showException(final Object ex) {
		SwingUtilities.invokeLater(new Thread() {
			public void run() {
				if (ex != null)
					GM.showException(ex);
			}
		});
	}

	/**
	 * ִ�в˵�״̬��Ϊ�����ã���ֹ�������
	 */
	private void preventRun() {
		setMenuToolEnabled(new short[] { GCSpl.iEXEC, GCSpl.iEXE_DEBUG, GCSpl.iSTEP_CURSOR, GCSpl.iSTEP_NEXT,
				GCSpl.iCALC_AREA, GCSpl.iCALC_LOCK }, false);
	}

	/**
	 * ����ִ�в˵�״̬
	 */
	public void resetRunState() {
		resetRunState(false, false);
	}

	/**
	 * ����ִ�в˵�״̬
	 * 
	 * @param isRefresh �Ƿ�ˢ�·������õ�
	 * @param afterRun  �Ƿ�ִ�н������õ�
	 */
	private synchronized void resetRunState(final boolean isRefresh, final boolean afterRun) {
		SwingUtilities.invokeLater(new Thread() {
			public void run() {
				resetRunStateThread(isRefresh, afterRun);
			}
		});
	}

	/**
	 * ����ִ�в˵�״̬�߳�
	 * 
	 * @param isRefresh �Ƿ�ˢ�·������õ�
	 * @param afterRun  �Ƿ�ִ�н������õ�
	 */
	private synchronized void resetRunStateThread(boolean isRefresh, boolean afterRun) {
		if (!(GV.appMenu instanceof MenuSpl))
			return;
		MenuSpl md = (MenuSpl) GV.appMenu;

		if (isStepStop) {
			setMenuToolEnabled(
					new short[] { GCSpl.iEXEC, GCSpl.iEXE_DEBUG, GCSpl.iSTEP_CURSOR, GCSpl.iSTEP_NEXT, GCSpl.iSTEP_INTO,
							GCSpl.iCALC_AREA, GCSpl.iCALC_LOCK, GCSpl.iSTEP_RETURN, GCSpl.iSTEP_STOP, GCSpl.iPAUSE },
					false);
			setMenuToolEnabled(new short[] { GCSpl.iSTOP }, true); // ֻ���ж���
			boolean editable = splControl.cellSet.getCurrentPrivilege() == PgmCellSet.PRIVILEGE_FULL;
			if (!isRefresh) {
				splEditor.getComponent().getContentPanel().setEditable(editable);
				if (editable)
					splControl.contentView.initEditor(ContentPanel.MODE_HIDE);
			}
			if (afterRun) {
				setExeLocation(exeLocation);
				splControl.contentView.repaint();
				refresh();
			}
			return;
		}

		boolean isPaused = false;
		boolean editable = true;
		boolean canStepInto = canStepInto();
		boolean isDebugMode = false;
		boolean isThreadNull;
		byte runState = RunThread.FINISH;
		synchronized (threadLock) {
			if (runThread != null) {
				synchronized (runThread) {
					isThreadNull = runThread == null;
					if (!isThreadNull) {
						isDebugMode = runThread.isDebugMode;
						runState = runThread.getRunState();
					}
				}
			} else {
				isThreadNull = true;
			}
		}
		if (isThreadNull) {
			setMenuToolEnabled(new short[] { GCSpl.iEXEC, GCSpl.iEXE_DEBUG }, stepInfo == null);
			setMenuToolEnabled(new short[] { GCSpl.iSTEP_CURSOR, GCSpl.iSTEP_NEXT }, true);
			setMenuToolEnabled(new short[] { GCSpl.iPAUSE }, false);
			setMenuToolEnabled(new short[] { GCSpl.iSTOP }, stepInfo != null);
			setMenuToolEnabled(new short[] { GCSpl.iSTEP_INTO }, canStepInto && stepInfo != null);
			setMenuToolEnabled(new short[] { GCSpl.iSTEP_RETURN }, stepInfo != null);
			setMenuToolEnabled(new short[] { GCSpl.iSTEP_STOP }, stepInfo != null && stepInfo.parentCall != null);
			setMenuToolEnabled(new short[] { GCSpl.iCALC_AREA, GCSpl.iCALC_LOCK },
					canRunCell() && (stepInfo == null || isStepStop));
		} else {
			switch (runState) {
			case RunThread.RUN:
				setMenuToolEnabled(new short[] { GCSpl.iEXEC, GCSpl.iEXE_DEBUG, GCSpl.iSTEP_CURSOR, GCSpl.iSTEP_NEXT,
						GCSpl.iSTEP_INTO, GCSpl.iCALC_AREA, GCSpl.iCALC_LOCK }, false);
				setMenuToolEnabled(new short[] { GCSpl.iSTEP_RETURN }, stepInfo != null);
				setMenuToolEnabled(new short[] { GCSpl.iSTEP_STOP }, stepInfo != null && stepInfo.parentCall != null);
				setMenuToolEnabled(new short[] { GCSpl.iPAUSE, GCSpl.iSTOP }, true);
				editable = false;
				break;
			case RunThread.PAUSING:
				setMenuToolEnabled(new short[] { GCSpl.iEXEC, GCSpl.iEXE_DEBUG, GCSpl.iSTEP_CURSOR, GCSpl.iSTEP_NEXT,
						GCSpl.iSTEP_INTO, GCSpl.iCALC_AREA, GCSpl.iCALC_LOCK, GCSpl.iPAUSE }, false);
				setMenuToolEnabled(new short[] { GCSpl.iSTEP_RETURN }, stepInfo != null);
				setMenuToolEnabled(new short[] { GCSpl.iSTEP_STOP }, stepInfo != null && stepInfo.parentCall != null);
				setMenuToolEnabled(new short[] { GCSpl.iSTOP }, true);
				break;
			case RunThread.PAUSED:
				setMenuToolEnabled(new short[] { GCSpl.iEXEC, GCSpl.iEXE_DEBUG }, false);
				setMenuToolEnabled(new short[] { GCSpl.iSTEP_CURSOR, GCSpl.iSTEP_NEXT }, isDebugMode);
				setMenuToolEnabled(new short[] { GCSpl.iSTEP_INTO }, canStepInto);
				setMenuToolEnabled(new short[] { GCSpl.iSTEP_RETURN }, stepInfo != null);
				setMenuToolEnabled(new short[] { GCSpl.iSTEP_STOP }, stepInfo != null && stepInfo.parentCall != null);
				setMenuToolEnabled(new short[] { GCSpl.iPAUSE, GCSpl.iSTOP }, true);
				isPaused = true;
				break;
			case RunThread.FINISH:
				setMenuToolEnabled(new short[] { GCSpl.iEXEC, GCSpl.iEXE_DEBUG, GCSpl.iSTEP_CURSOR, GCSpl.iSTEP_NEXT },
						true);
				setMenuToolEnabled(new short[] { GCSpl.iSTEP_INTO, GCSpl.iSTEP_RETURN, GCSpl.iSTEP_STOP }, false);
				setMenuToolEnabled(new short[] { GCSpl.iPAUSE, GCSpl.iSTOP }, false);
				setMenuToolEnabled(new short[] { GCSpl.iCALC_AREA, GCSpl.iCALC_LOCK }, canRunCell());
				break;
			}
		}
		if (splControl.cellSet.getCurrentPrivilege() != PgmCellSet.PRIVILEGE_FULL) {
			setMenuToolEnabled(new short[] { GCSpl.iEXE_DEBUG, GCSpl.iSTEP_CURSOR, GCSpl.iSTEP_NEXT, GCSpl.iSTEP_INTO,
					GCSpl.iSTEP_RETURN, GCSpl.iSTEP_STOP, GCSpl.iPAUSE }, false);
			isPaused = false;
			editable = false;
		}
		if (stepInfo != null) {
			editable = false;
		}
		md.resetPauseMenu(isPaused);
		((ToolBarSpl) GVSpl.appTool).resetPauseButton(isPaused);
		if (!isRefresh)
			splEditor.getComponent().getContentPanel().setEditable(editable);

		if (afterRun) {
			setExeLocation(exeLocation);
			splControl.contentView.repaint();
			refresh();
		}
	}

	/**
	 * �Ƿ���Ե��Խ���
	 * 
	 * @return
	 */
	private boolean canStepInto() {
		try {
			INormalCell cell = splControl.cellSet.getCurrent();
			if (!(cell instanceof PgmNormalCell)) {
				return false;
			}
			PgmNormalCell nc = (PgmNormalCell) cell;
			if (nc != null) {
				Expression exp = nc.getExpression();
				if (exp != null) {
					Node home = exp.getHome();
					if (home instanceof Call || home instanceof Func) {
						return true;
					}
				}
			}
		} catch (Throwable ex) {
		}
		return false;
	}

	/**
	 * ʹ�ú͹رռ����߳�ʱ��Ҫʹ�ô�����
	 */
	private byte[] threadLock = new byte[0];

	/**
	 * �ر�ִ���߳�
	 */
	private void closeRunThread() {
		synchronized (threadLock) {
			runThread = null;
		}
	}

	/**
	 * ���ò˵��͹������Ƿ����
	 * 
	 * @param ids
	 * @param enabled
	 */
	private void setMenuToolEnabled(short[] ids, boolean enabled) {
		MenuSpl md = (MenuSpl) GV.appMenu;
		for (int i = 0; i < ids.length; i++) {
			md.setMenuEnabled(ids[i], enabled);
			GVSpl.appTool.setButtonEnabled(ids[i], enabled);
		}
	}

	/**
	 * ִֹͣ��
	 */
	public synchronized void terminate() {
		if (sheets != null) { // �������Խ���
			int count = sheets.size();
			for (int i = 0; i < count; i++) {
				SheetSpl sheet = sheets.get(i);
				sheet.terminateSelf();
				if (sheet.stepInfo != null) {
					GV.appFrame.closeSheet(sheet);
					i--;
					count--;
				}
			}
			SheetSpl sheetParent = sheets.get(0);
			if (sheetParent != null) { // ��ʾ���յĸ�����
				try {
					sheetParent.stepInfo = null;
					GV.appFrame.showSheet(sheetParent);
				} catch (Exception e) {
					GM.showException(e);
				}
			}
			sheets = null;
		} else {
			terminateSelf();
		}
	}

	/**
	 * ִֹͣ�е�ǰҳ
	 */
	public synchronized void terminateSelf() {
		// ˳���Ϊ��ɱ�̺߳��ͷ���Դ
		Thread t = new Thread() {
			public void run() {
				synchronized (threadLock) {
					if (runThread != null) {
						synchronized (runThread) {
							if (runThread != null) {
								runThread.pause();
							}
							if (runThread != null && runThread.getRunState() != RunThread.FINISH) {
								if (tg != null) {
									try {
										if (tg != null)
											tg.interrupt();
									} catch (Throwable t) {
									}
									try {
										if (tg != null) {
											int nthreads = tg.activeCount();
											Thread[] threads = new Thread[nthreads];
											if (tg != null)
												tg.enumerate(threads);
											for (int i = 0; i < nthreads; i++) {
												try {
													threads[i].stop();
												} catch (Throwable t1) {
												}
											}
										}
									} catch (Throwable t) {
									}
								}
							}
						}
					}
				}
				if (tg != null) {
					try {
						if (tg != null && tg.activeCount() != 0)
							sleep(100);
						tg.destroy();
					} catch (Throwable t1) {
					}
				}
				tg = null;
				closeRunThread();
				try {
					closeResource();
				} catch (Throwable t1) {
					t1.printStackTrace();
				}
				SwingUtilities.invokeLater(new Thread() {
					public void run() {
						setExeLocation(null);
						refresh(false, false);
						if (isStepStop) {
							isStepStop = !isStepStop;
							stepInfo = null;
							subSheetClosed();
							resetRunState(false, true);
						}
						splControl.repaint();
					}
				});

			}
		};
		t.setPriority(1);
		t.start();
	}

	/**
	 * �����������ʽ
	 * 
	 * @param exps
	 */
	public void setCellSetExps(Sequence exps) {
		ByteMap bm = splControl.cellSet.getCustomPropMap();
		if (bm == null) {
			bm = new ByteMap();
		}
		bm.put(GC.CELLSET_EXPS, exps);
		splControl.cellSet.setCustomPropMap(bm);
		setChanged(true);
	}

	/**
	 * ��ʾ��Ԫ��ֵ
	 */
	public void showCellValue() {
		splEditor.showCellValue();
	}

	/**
	 * ��һ��ִ�и�����
	 */
	private CellLocation lastLocation = null;

	/**
	 * ����ִ�и�����
	 * 
	 * @param cl ����
	 */
	private void setExeLocation(CellLocation cl) {
		exeLocation = cl;
		if (cl != null) {
			splControl.setStepPosition(new CellLocation(cl.getRow(), cl.getCol()));
			lastLocation = new CellLocation(cl.getRow(), cl.getCol());
		} else {
			splControl.setStepPosition(null);
		}
	}

	/**
	 * ��Ԫ���Ƿ����ִ��
	 * 
	 * @return
	 */
	private boolean canRunCell() {
		if (splEditor == null || selectState == GCSpl.SELECT_STATE_NONE) {
			return false;
		}
		PgmNormalCell nc = (PgmNormalCell) splEditor.getDisplayCell();
		if (nc == null)
			return false;
		String expStr = nc.getExpString();
		if (!StringUtils.isValidString(expStr))
			return false;
		if (nc.getType() == PgmNormalCell.TYPE_COMMAND_CELL) {
			Command cmd = nc.getCommand();
			switch (cmd.getType()) {
			case Command.SQL:
				return true;
			default:
				return false;
			}
		}
		return true;
	}

	/**
	 * ׼������
	 * 
	 * @return
	 */
	private boolean prepareArg() {
		CellSet cellSet = splControl.cellSet;
		ParamList paras = cellSet.getParamList();
		if (paras == null || paras.count() == 0) {
			return true;
		}
		if (paras.isUserChangeable()) {
			try {
				DialogInputArgument dia = new DialogInputArgument(splCtx);
				dia.setParam(paras);
				dia.setVisible(true);
				if (dia.getOption() != JOptionPane.OK_OPTION) {
					return false;
				}
				HashMap<String, Object> values = dia.getParamValue();
				Iterator<String> it = values.keySet().iterator();
				while (it.hasNext()) {
					String paraName = it.next();
					Object value = values.get(paraName);
					splCtx.setParamValue(paraName, value, Param.VAR);
				}
			} catch (Throwable t) {
				GM.showException(t);
			}
		} else {
			// ȡ��ʼֵ������������
			// resetʱ�Ѿ�����Ĭ��ֵ�����ﲻ������
			// CellSetUtil.putArgValue(cellSet, null);
			// for (int i = 0; i < paras.count(); i++) {
			// Param p = paras.get(i);
			// if (p.getKind() == Param.VAR)
			// splCtx.setParamValue(p.getName(), p.getValue(), Param.VAR);
			// }
		}
		return true;
	}

	/**
	 * ��������Ի���
	 */
	public void dialogParameter() {
		DialogArgument dp = new DialogArgument();
		dp.setParameter(splControl.cellSet.getParamList());
		dp.setVisible(true);
		if (dp.getOption() == JOptionPane.OK_OPTION) {
			AtomicSpl ar = new AtomicSpl(splControl);
			ar.setType(AtomicSpl.SET_PARAM);
			ar.setValue(dp.getParameter());
			splEditor.executeCmd(ar);
		}
	}

	/**
	 * ��������Ի���
	 */
	public void dialogPassword() {
		DialogPassword dp = new DialogPassword();
		dp.setCellSet(splControl.cellSet);
		dp.setVisible(true);
		if (dp.getOption() != JOptionPane.OK_OPTION) {
			return;
		}
		refresh();
		setChanged(true);
	}

	/**
	 * ��������
	 */
	private void dialogInputPassword() {
		DialogInputPassword dip = new DialogInputPassword(true);
		dip.setPassword(null);
		dip.setVisible(true);
		if (dip.getOption() == JOptionPane.OK_OPTION) {
			String psw = dip.getPassword();
			splControl.cellSet.setCurrentPassword(psw);
			boolean isFull = splControl.cellSet.getCurrentPrivilege() == PgmCellSet.PRIVILEGE_FULL;
			((MenuSpl) GV.appMenu).resetPasswordMenu(isFull);
			boolean lastEditable = splControl.contentView.isEditable();
			if (lastEditable != isFull) {
				splControl.contentView.setEditable(isFull);
				if (isFull)
					splControl.contentView.initEditor(ContentPanel.MODE_SHOW);
			}
			refresh();
		}
	}

	/**
	 * �ر�ҳ
	 */
	public boolean close() {
		// ��ֹͣ���б༭���ı༭
		((EditControl) splEditor.getComponent()).acceptText();
		boolean isChanged = splEditor.isDataChanged();
		// û���ӳ�������񣬻������ӳ������Ѿ��ж�ִ�е�call���񣬶���ʾ����
		if (isChanged && (stepInfo == null || isStepStopCall())) {
			String t1, t2;
			t1 = IdeCommonMessage.get().getMessage("public.querysave", IdeCommonMessage.get().getMessage("public.file"),
					filePath);
			t2 = IdeCommonMessage.get().getMessage("public.save");
			int option = JOptionPane.showConfirmDialog(GV.appFrame, t1, t2, JOptionPane.YES_NO_CANCEL_OPTION);
			switch (option) {
			case JOptionPane.YES_OPTION:
				if (!save())
					return false;
				break;
			case JOptionPane.NO_OPTION:
				break;
			default:
				return false;
			}
		}
		if (tg != null) {
			try {
				tg.interrupt();
				tg.destroy();
			} catch (Throwable t) {
			}
		}
		try {
			closeSpace();
		} catch (Throwable t) {
			GM.showException(t);
		}
		if (stepInfo != null && stepInfo.isCall()) {
			try {
				if (stepInfo.parentCall != null) {
					stepInfo.parentCall.finish(splControl.cellSet);
				}
			} catch (Exception e) {
				GM.showException(e);
			}
		}
		GVSpl.panelValue.tableValue.setLocked1(false);
		GVSpl.panelValue.tableValue.setCellId(null);
		GVSpl.panelValue.tableValue.setValue(null);
		GVSpl.panelValue.setCellSet(null);
		storeBreakPoints();
		GM.setWindowDimension(this);
		dispose();
		if (stepInfo != null) {
			SheetSpl parentSheet = getParentSheet();
			if (parentSheet != null) {
				parentSheet.subSheetClosed();
			}
		}
		if (sheets != null) {
			sheets.remove(this);
		}
		return true;
	}

	/**
	 * ��ҳ�ر���
	 */
	public void subSheetClosed() {
		this.subSheetOpened = false;
	}

	/**
	 * ѡ���״̬������
	 */
	public void selectStateChanged(byte newState, boolean keyEvent) {
		selectState = newState;
		GVSpl.cmdSender = null;
		refresh(keyEvent);
	}

	/**
	 * ȡ��ǰѡ���״̬
	 * 
	 * @return
	 */
	public byte getSelectState() {
		return selectState;
	}

	/**
	 * �Ҽ������������˵���
	 */
	public void rightClicked(Component invoker, int x, int y) {
		popupSpl.getSplPop(selectState).show(invoker, x, y);
	}

	/**
	 * ��ʾ��ǰ�񣬲�����ʱ��������ǰ��λ��
	 */
	public boolean scrollActiveCellToVisible = true;

	/**
	 * ����ִ�к�
	 */
	public void commandExcuted() {
		splEditor.selectAreas(scrollActiveCellToVisible);
		scrollActiveCellToVisible = true;
		refresh();
		splControl.repaint();
		ControlUtils.clearWrapBuffer();
	}

	/**
	 * ���ȱ��浱ǰ�ļ�
	 */
	private static final String ERROR_NOT_SAVE = IdeSplMessage.get().getMessage("sheetdfx.savefilebefore");

	/**
	 * ����ͬ���ı��ļ�
	 */
	public void importSameNameTxt() {
		if (stepInfo != null)
			return;
		try {
			if (GMSpl.isNewGrid(filePath, GCSpl.PRE_NEWPGM)) { // �½�
				JOptionPane.showMessageDialog(GV.appFrame, ERROR_NOT_SAVE);
				return;
			}
			File f = new File(filePath);
			if (!f.isFile() || !f.exists()) {
				JOptionPane.showMessageDialog(GV.appFrame, ERROR_NOT_SAVE);
				return;
			}
			synchronized (threadLock) {
				if (runThread != null) {
					// ��δִ����ɵ������Ƿ��ж�ִ�У�
					int option = JOptionPane.showOptionDialog(GV.appFrame,
							IdeSplMessage.get().getMessage("sheetdfx.queryclosethread"),
							IdeSplMessage.get().getMessage("sheetdfx.closethread"), JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE, null, null, null);
					if (option == JOptionPane.OK_OPTION) {
						runThread.closeThread();
						try {
							Thread.sleep(50);
						} catch (Throwable t) {
						}
						terminate();
					}
				}
			}
			tg = null;
			closeRunThread();
			setExeLocation(null);
			EditControl control = (EditControl) splEditor.getComponent();
			boolean isEditable = control.getContentPanel().isEditable();
			PgmCellSet cellSet = splControl.cellSet;
			int index = filePath.lastIndexOf(".");
			String txtPath = filePath.substring(0, index) + "." + AppConsts.FILE_TXT;
			CellRect rect = new CellRect(1, 1, cellSet.getRowCount(), cellSet.getColCount());
			Vector<IAtomicCmd> cmds = splEditor.getClearRectCmds(rect, SplEditor.CLEAR);
			splEditor.executeCmd(cmds);
			CellSetTxtUtil.readCellSet(txtPath, cellSet);
			splEditor.setCellSet(cellSet);
			splCtx = new Context();
			Context pCtx = GMSpl.prepareParentContext();
			splCtx.setParent(pCtx);
			splControl.cellSet.setContext(splCtx);
			resetRunState();
			refresh();
			splControl.repaint();
			splEditor.selectFirstCell();
			control.getContentPanel().setEditable(isEditable);
			control.getContentPanel().initEditor(ContentPanel.MODE_HIDE);
			control.reloadEditorText();
		} catch (Exception e) {
			GM.showException(e);
		}
	}

	/**
	 * ���µ����ļ�
	 */
	public void reloadFile() {
		try {
			if (GMSpl.isNewGrid(filePath, GCSpl.PRE_NEWPGM)) { // �½�
				JOptionPane.showMessageDialog(GV.appFrame, ERROR_NOT_SAVE);
				return;
			}
			File f = new File(filePath);
			if (!f.isFile() || !f.exists()) {
				JOptionPane.showMessageDialog(GV.appFrame, ERROR_NOT_SAVE);
				return;
			}
			synchronized (threadLock) {
				if (runThread != null) {
					// ��δִ����ɵ������Ƿ��ж�ִ�У�
					int option = JOptionPane.showOptionDialog(GV.appFrame,
							IdeSplMessage.get().getMessage("sheetdfx.queryclosethread"),
							IdeSplMessage.get().getMessage("sheetdfx.closethread"), JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE, null, null, null);
					if (option == JOptionPane.OK_OPTION) {
						runThread.closeThread();
						try {
							Thread.sleep(50);
						} catch (Throwable t) {
						}
						terminate();
					}
				}
			}
			tg = null;
			closeRunThread();
			setExeLocation(null);
			EditControl control = (EditControl) splEditor.getComponent();
			boolean isEditable = control.getContentPanel().isEditable();
			PgmCellSet cellSet = AppUtil.readCellSet(filePath);
			splEditor.setCellSet(cellSet);
			splCtx = new Context();
			Context pCtx = GMSpl.prepareParentContext();
			splCtx.setParent(pCtx);
			splControl.cellSet.setContext(splCtx);
			resetRunState();
			refresh();
			splControl.repaint();
			splEditor.selectFirstCell();
			control.getContentPanel().setEditable(isEditable);
			control.getContentPanel().initEditor(ContentPanel.MODE_HIDE);
			control.reloadEditorText();
		} catch (Exception e) {
			GM.showException(e);
		}
	}

	/**
	 * ִ������
	 * 
	 * @param cmd GCSpl�ж���ĳ���
	 */
	public void executeCmd(short cmd) {
		switch (cmd) {
		case GCSpl.iFILE_REOPEN:
			reloadFile();
			break;
		case GCSpl.iSAVE_FTP:
			saveFTP();
			break;
		case GC.iOPTIONS:
			boolean showDB = ConfigOptions.bShowDBStruct;
			new DialogOptions().setVisible(true);
			((SPL) GV.appFrame).refreshOptions();
			if (showDB != ConfigOptions.bShowDBStruct) {
				if (GVSpl.tabParam != null) {
					GVSpl.tabParam.resetEnv();
				}
			}
			break;
		case GCSpl.iRESET:
			reset();
			break;
		case GCSpl.iEXEC:
			run();
			break;
		case GCSpl.iEXE_DEBUG:
			debug(RunThread.DEBUG);
			break;
		case GCSpl.iPAUSE:
			pause();
			break;
		case GCSpl.iCALC_LOCK:
			calcActiveCell(true);
			break;
		case GCSpl.iCALC_AREA:
			calcActiveCell(false);
			break;
		case GCSpl.iSHOW_VALUE:
			showCellValue();
			break;
		case GCSpl.iSTEP_NEXT:
			debug(RunThread.STEP_OVER);
			break;
		case GCSpl.iSTEP_INTO:
			debug(RunThread.STEP_INTO);
			break;
		case GCSpl.iSTEP_RETURN:
			debug(RunThread.STEP_RETURN);
			break;
		case GCSpl.iSTEP_STOP:
			stepStop(true);
			break;
		case GCSpl.iSTEP_CURSOR:
			debug(RunThread.CURSOR);
			break;
		case GCSpl.iSTOP:
			this.terminate();
			break;
		case GCSpl.iBREAKPOINTS:
			splControl.setBreakPoint();
			break;
		case GCSpl.iUNDO:
			splEditor.undo();
			break;
		case GCSpl.iREDO:
			splEditor.redo();
			break;
		case GCSpl.iCOPY:
			splEditor.copy();
			break;
		case GCSpl.iCOPYVALUE:
			splEditor.copy(false, true);
			break;
		case GCSpl.iCODE_COPY:
			splEditor.codeCopy();
			break;
		case GCSpl.iCOPY_HTML:
			if (splEditor.canCopyPresent())
				splEditor.copyPresent();
			break;
		case GCSpl.iCOPY_HTML_DIALOG:
			splEditor.copyPresentDialog();
			break;
		case GCSpl.iCUT:
			splEditor.cut();
			break;
		case GCSpl.iPASTE:
			splEditor.paste(false);
			break;
		case GCSpl.iPASTE_ADJUST:
			splEditor.paste(true);
			break;
		case GCSpl.iPASTE_SPECIAL:
			byte o = getPasteOption();
			if (o != SplEditor.PASTE_OPTION_NORMAL) {
				splEditor.paste(isAdjustPaste, o);
			}
			break;
		case GCSpl.iCLEAR_VALUE:
			splEditor.clear(SplEditor.CLEAR_VAL);
			break;
		case GCSpl.iPARAM:
			dialogParameter();
			break;
		case GCSpl.iPASSWORD:
			if (splControl.cellSet.getCurrentPrivilege() == PgmCellSet.PRIVILEGE_FULL)
				dialogPassword();
			else {
				dialogInputPassword();
			}
			break;
		case GCSpl.iCTRL_BACK:
			splControl.ctrlBackSpace();
			break;
		case GCSpl.iCLEAR:
			splEditor.clear(SplEditor.CLEAR_EXP);
			break;
		case GCSpl.iFULL_CLEAR:
			splEditor.clear(SplEditor.CLEAR);
			break;
		case GCSpl.iCTRL_DELETE:
			splControl.ctrlDelete();
			break;
		case GCSpl.iDELETE_COL:
		case GCSpl.iDELETE_ROW:
			splEditor.delete(cmd);
			break;
		case GCSpl.iTEXT_EDITOR:
			splEditor.textEditor();
			break;
		case GCSpl.iNOTE:
			splEditor.note();
			break;
		case GCSpl.iTIPS:
			splEditor.setTips();
			break;
		case GCSpl.iSEARCH:
			dialogSearch(false);
			break;
		case GCSpl.iREPLACE:
			dialogSearch(true);
			break;
		case GCSpl.iROW_HEIGHT:
			CellRect cr = splEditor.getSelectedRect();
			int row = cr.getBeginRow();
			float height = splControl.cellSet.getRowCell(row).getHeight();
			DialogRowHeight drh = new DialogRowHeight(true, height);
			drh.setVisible(true);
			if (drh.getOption() == JOptionPane.OK_OPTION) {
				height = drh.getRowHeight();
				splEditor.setRowHeight(height);
			}
			break;
		case GCSpl.iCOL_WIDTH:
			cr = splEditor.getSelectedRect();
			int col = cr.getBeginCol();
			float width = splControl.cellSet.getColCell(col).getWidth();
			drh = new DialogRowHeight(false, width);
			drh.setVisible(true);
			if (drh.getOption() == JOptionPane.OK_OPTION) {
				width = drh.getRowHeight();
				splEditor.setColumnWidth(width);
			}
			break;
		case GCSpl.iROW_ADJUST:
			splEditor.adjustRowHeight();
			break;
		case GCSpl.iCOL_ADJUST:
			splEditor.adjustColWidth();
			break;
		case GCSpl.iROW_HIDE:
			splEditor.setRowVisible(false);
			break;
		case GCSpl.iROW_VISIBLE:
			splEditor.setRowVisible(true);
			break;
		case GCSpl.iCOL_HIDE:
			splEditor.setColumnVisible(false);
			break;
		case GCSpl.iCOL_VISIBLE:
			splEditor.setColumnVisible(true);
			break;
		case GCSpl.iEDIT_CHART:
			splEditor.dialogChartEditor();
			break;
		case GCSpl.iFUNC_ASSIST:
			splEditor.dialogFuncEditor();
			break;
		case GCSpl.iDRAW_CHART:
			GVSpl.panelValue.tableValue.drawChart();
			break;
		case GCSpl.iCTRL_ENTER:
			splEditor.hotKeyInsert(SplEditor.HK_CTRL_ENTER);
			break;
		case GCSpl.iCTRL_INSERT:
			splEditor.hotKeyInsert(SplEditor.HK_CTRL_INSERT);
			break;
		case GCSpl.iALT_INSERT:
			splEditor.hotKeyInsert(SplEditor.HK_ALT_INSERT);
			break;
		case GCSpl.iMOVE_COPY_UP:
		case GCSpl.iMOVE_COPY_DOWN:
		case GCSpl.iMOVE_COPY_LEFT:
		case GCSpl.iMOVE_COPY_RIGHT:
			splEditor.moveCopy(cmd);
			break;
		case GCSpl.iINSERT_COL:
			splEditor.insertCol(true);
			break;
		case GCSpl.iADD_COL:
			splEditor.insertCol(false);
			break;
		case GCSpl.iDUP_ROW:
			splEditor.dupRow(false);
			break;
		case GCSpl.iDUP_ROW_ADJUST:
			splEditor.dupRow(true);
			break;
		case GC.iPROPERTY:
			PgmCellSet pcs = (PgmCellSet) splEditor.getComponent().getCellSet();
			DialogCellSetProperties dcsp = new DialogCellSetProperties(
					pcs.getCurrentPrivilege() == PgmCellSet.PRIVILEGE_FULL);
			dcsp.setPropertyMap(pcs.getCustomPropMap());
			dcsp.setVisible(true);
			if (dcsp.getOption() == JOptionPane.OK_OPTION) {
				pcs.setCustomPropMap(dcsp.getPropertyMap());
				splEditor.setDataChanged(true);
			}
			break;
		case GCSpl.iCONST:
			DialogEditConst dce = new DialogEditConst(false);
			ParamList pl = Env.getParamList(); // GV.session
			Vector<String> usedNames = new Vector<String>();
			if (pl != null) {
				for (int j = 0; j < pl.count(); j++) {
					usedNames.add(((Param) pl.get(j)).getName());
				}
			}
			dce.setUsedNames(usedNames);
			dce.setParamList(splControl.cellSet.getParamList());
			dce.setVisible(true);
			if (dce.getOption() == JOptionPane.OK_OPTION) {
				AtomicSpl ar = new AtomicSpl(splControl);
				ar.setType(AtomicSpl.SET_CONST);
				ar.setValue(dce.getParamList());
				splEditor.executeCmd(ar);
				refresh();
			}
			break;
		case GCSpl.iSQLGENERATOR: {
			DialogSelectDataSource dsds = new DialogSelectDataSource(DialogSelectDataSource.TYPE_SQL);
			dsds.setVisible(true);
			if (dsds.getOption() != JOptionPane.OK_OPTION) {
				return;
			}
			DataSource ds = dsds.getDataSource();
			try {
				DialogSQLEditor dse = new DialogSQLEditor(ds);
				dse.setCopyMode();
				dse.setVisible(true);
			} catch (Throwable ex) {
				GM.showException(ex);
			}
			break;
		}
		case GCSpl.iEXEC_CMD:
			// ��ֹͣ���б༭���ı༭
			((EditControl) splEditor.getComponent()).acceptText();
			boolean isChanged = splEditor.isDataChanged();
			if (isChanged) {
				String t1, t2;
				t1 = IdeCommonMessage.get().getMessage("public.querysave",
						IdeCommonMessage.get().getMessage("public.file"), filePath);
				t2 = IdeCommonMessage.get().getMessage("public.save");
				int option = JOptionPane.showConfirmDialog(GV.appFrame, t1, t2, JOptionPane.YES_NO_CANCEL_OPTION);
				switch (option) {
				case JOptionPane.YES_OPTION:
					if (!save())
						return;
					break;
				case JOptionPane.NO_OPTION:
					break;
				default:
					return;
				}
			}
			DialogExecCmd dec = new DialogExecCmd();
			if (StringUtils.isValidString(filePath)) {
				FileObject fo = new FileObject(filePath, "s", new Context());
				if (fo.isExists())
					dec.setSplFile(filePath);
			}
			dec.setVisible(true);
			break;
		}
	}

	/**
	 * �Ƿ����ճ��
	 */
	private boolean isAdjustPaste = false;

	/**
	 * ȡճ��ѡ��
	 * 
	 * @return
	 */
	private byte getPasteOption() {
		byte option = SplEditor.PASTE_OPTION_NORMAL;
		if (GVSpl.cellSelection != null) {
			switch (GVSpl.cellSelection.selectState) {
			case GC.SELECT_STATE_ROW: // ���ѡ�е�������
				option = SplEditor.PASTE_OPTION_INSERT_ROW;
				break;
			case GC.SELECT_STATE_COL: // ���ѡ�е�������
				option = SplEditor.PASTE_OPTION_INSERT_COL;
				break;
			}
		}
		if (option == SplEditor.PASTE_OPTION_NORMAL) {
			DialogOptionPaste dop = new DialogOptionPaste();
			dop.setVisible(true);
			if (dop.getOption() == JOptionPane.OK_OPTION) {
				option = dop.getPasteOption();
				isAdjustPaste = dop.isAdjustExp();
			}
		}
		return option;
	}

	/**
	 * ȡ�������
	 * 
	 * @return �������
	 */
	public ICellSet getCellSet() {
		return splControl.getCellSet();
	}

	/**
	 * �����������
	 * 
	 * @param cellSet �������
	 */
	public void setCellSet(Object cellSet) {
		try {
			splEditor.setCellSet((PgmCellSet) cellSet);
		} catch (Exception ex) {
		}
		this.repaint();
	}

	/**
	 * ���������Ƿ��޸���
	 * 
	 * @param isChanged �����Ƿ��޸���
	 */
	public void setChanged(boolean isChanged) {
		splEditor.setDataChanged(isChanged);
	}

	/**
	 * ��ǰҳ������
	 *
	 */
	class Listener extends InternalFrameAdapter {
		/**
		 * ��ǰҳ����
		 */
		SheetSpl sheet;

		/**
		 * ���캯��
		 * 
		 * @param sheet ҳ����
		 */
		public Listener(SheetSpl sheet) {
			super();
			this.sheet = sheet;
		}

		/**
		 * ��ǰҳ������
		 */
		public void internalFrameActivated(InternalFrameEvent e) {
			// ���߳������Եȴ���Ĵ��ڳ��׹رղż���ô���
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					GV.appSheet = sheet;

					GVSpl.splEditor = sheet.splEditor;
					sheet.splControl.repaint();
					GV.appFrame.changeMenuAndToolBar(((SPL) GV.appFrame).newMenuSpl(), GVSpl.getSplTool());

					GV.appMenu.addLiveMenu(sheet.getSheetTitle());
					GV.appMenu.resetPrivilegeMenu();
					GM.setCurrentPath(sheet.getFileName());
					if (sheet.splControl == null) {
						GVSpl.panelValue.setCellSet(null);
						GVSpl.panelValue.tableValue.setValue(null);
						GVSpl.tabParam.resetParamList(null);
						return;
					}
					((ToolBarProperty) GV.toolBarProperty).init();
					sheet.refresh();
					sheet.resetRunState();
					((SPL) GV.appFrame).resetTitle();
					GV.toolWin.refreshSheet(sheet);
					sheet.selectFirstCell();
					GVSpl.panelSplWatch.setCellSet(sheet.splControl.cellSet);
					GVSpl.panelSplWatch.watch(sheet.getSplContext());
					GVSpl.panelValue.setCellSet(sheet.splControl.cellSet);
					if (GVSpl.searchDialog != null && GVSpl.searchDialog.isVisible()) {
						if (splEditor != null)
							GVSpl.searchDialog.setControl(splEditor);
					}
				}
			});
		}

		/**
		 * ��ǰҳ���ڹر�
		 */
		public void internalFrameClosing(InternalFrameEvent e) {
			GVSpl.appFrame.closeSheet(sheet);
			GV.toolBarProperty.setEnabled(false);
		}

		/**
		 * ��ǰҳ���ڷǼ���״̬
		 */
		public void internalFrameDeactivated(InternalFrameEvent e) {
			GVSpl.splEditor = null;
			// �����ı�û�л��ƣ����ڲ��ʱˢ��һ��
			sheet.splControl.repaint();
			GV.toolBarProperty.setEnabled(false);
			GVSpl.panelSplWatch.setCellSet(null);
			if (GVSpl.matchWindow != null) {
				GVSpl.matchWindow.dispose();
				GVSpl.matchWindow = null;
			}
		}
	}

	/**
	 * �ύ��Ԫ��༭
	 */
	public boolean submitEditor() {
		try {
			splControl.contentView.submitEditor();
			return true;
		} catch (Exception ex) {
			GM.showException(ex);
		}
		return false;
	}

	/**
	 * ���񱣴浽�������
	 * 
	 * @param os
	 * @return
	 */
	public boolean saveOutStream(OutputStream os) {
		try {
			CellSetUtil.writePgmCellSet(os, (PgmCellSet) splControl.cellSet);
			DfxManager.getInstance().clear();
			((PrjxAppMenu) GV.appMenu).refreshRecentFile(filePath);
		} catch (Throwable e) {
			GM.showException(e);
			return false;
		}
		splEditor.setDataChanged(false);
		splEditor.getSplListener().commandExcuted();
		return true;
	}
}