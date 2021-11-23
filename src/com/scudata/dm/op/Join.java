package com.scudata.dm.op;

import java.util.ArrayList;

import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.CompressIndexTable;
import com.scudata.dm.ComputeStack;
import com.scudata.dm.Context;
import com.scudata.dm.DataStruct;
import com.scudata.dm.IndexTable;
import com.scudata.dm.ListBase1;
import com.scudata.dm.Record;
import com.scudata.dm.Sequence;
import com.scudata.dm.Table;
import com.scudata.dw.compress.Column;
import com.scudata.dw.compress.ColumnList;
import com.scudata.expression.CurrentElement;
import com.scudata.expression.CurrentSeq;
import com.scudata.expression.Expression;
import com.scudata.expression.Function;
import com.scudata.resources.EngineMessage;

/**
 * ���α��ܵ�����join����
 * @author WangXiaoJun
 *
 */
public class Join extends Operation {
	private String fname; // @oѡ����ʹ�ã�ԭ��¼������Ϊ�¼�¼���ֶ�
	private Expression [][]exps; // �����ֶα���ʽ����
	private Sequence []codes; // ���������
	private Expression [][]dataExps; // �������������ʽ����
	private Expression [][]newExps; // ȡ���Ĵ�������ֶα���ʽ����
	private String [][]newNames; // ȡ���Ĵ�������ֶ�������
	private String opt; // ѡ��
	
	private DataStruct oldDs; // Դ�����ݽṹ
	private DataStruct newDs; // ��������ݽṹ
	private IndexTable []indexTables; // �������hashֵ����
	private int [][]srcIndexs; // exps�ֶ���data��λ��
	private int [][]refIndexs; // newExps�ֶ��ڴ������λ��
	private int [][]tgtIndexs; // newExps�ֶ��ڽ������λ��
	
	private boolean isIsect; // �����ӣ�Ĭ��Ϊ������
	private boolean isOrg;
	private boolean containNull; // �Ƿ��еĴ����Ϊ��
	
	public Join(String fname, Expression[][] exps, Sequence[] codes,
			  Expression[][] dataExps, Expression[][] newExps,
			  String[][] newNames, String opt) {
		this(null, fname, exps, codes, dataExps, newExps, newNames, opt);
	}

	public Join(Function function, String fname, Expression[][] exps, Sequence[] codes,
				  Expression[][] dataExps, Expression[][] newExps,
				  String[][] newNames, String opt) {
		super(function);
		this.fname = fname;
		this.exps = exps;
		this.codes = codes;
		this.dataExps = dataExps;
		this.newExps = newExps;
		this.opt = opt;
		
		if (opt != null) {
			if (opt.indexOf('i') != -1) isIsect = true;
			if (opt.indexOf('o') != -1) isOrg = true;
		}

		ArrayList<String[]> srcFieldsList = new ArrayList<String[]>();
		ArrayList<String> refFieldList = new ArrayList<String>();
		
		int count = newExps.length;
		if (newNames == null) newNames = new String[count][];
		for (int i = 0; i < count; ++i) {
			if (codes[i] == null) {
				containNull = true;
			} else if (codes[i].length() == 0 && codes[i].getIndexTable() == null) {
				// ����ĸ�����¼��Ϊ0��������
				codes[i] = null;
				containNull = true;
			}
			
			Expression []curExps = newExps[i];
			int curLen = curExps.length;

			if (newNames[i] == null) newNames[i] = new String[curLen];
			String []curNames = newNames[i];

			for (int j = 0; j < curLen; ++j) {
				if (curNames[j] == null || curNames[j].length() == 0) {
					curNames[j] = curExps[j].getFieldName();
				}
			}
			
			// x��~ʱ���ڽ������м�¼F��C:����Ӧ��ϵ����ʶ��Ԥ�������
			if (curLen == 1 && curExps[0].getHome() instanceof CurrentElement) {
				Expression []srcExps = exps[i];
				int srcCount = srcExps.length;
				String []srcFields = new String[srcCount];
				for (int f = 0; f < srcCount; ++f) {
					srcFields[f] = srcExps[f].getFieldName();
				}
				
				srcFieldsList.add(srcFields);
				refFieldList.add(curNames[0]);
			}
		}
		
		this.newNames = newNames;
	}
	
	/**
	 * ȡ�����Ƿ�����Ԫ������������˺�������ټ�¼
	 * �˺��������α�ľ�ȷȡ����������ӵĲ�������ʹ��¼��������ֻ�谴���������ȡ������
	 * @return true���ᣬfalse������
	 */
	public boolean isDecrease() {
		return isIsect;
	}
	
	/**
	 * �����������ڶ��̼߳��㣬��Ϊ����ʽ���ܶ��̼߳���
	 * @param ctx ����������
	 * @return Operation
	 */
	public Operation duplicate(Context ctx) {
		Expression [][]exps1 = dupExpressions(exps, ctx);
		Expression [][]dataExps1 = dupExpressions(dataExps, ctx);
		Expression [][]newExps1 = dupExpressions(newExps, ctx);
				
		return new Join(function, fname, exps1, codes, dataExps1, newExps1, newNames, opt);
	}

	private void init(Sequence data, Context ctx) {
		if (newDs != null) {
			return;
		}
		
		Sequence seq = new Sequence();
		String []oldKey = null;
		if (isOrg) {
			seq.add(fname);
			for (int i = 0; i < newNames.length; ++i) {
				seq.addAll(newNames[i]);
			}
		} else {
			oldDs = data.dataStruct();
			if (oldDs == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException(mm.getMessage("engine.needPurePmt"));
			}
			
			oldKey = oldDs.getPrimary();
			seq.addAll(oldDs.getFieldNames());
			int dcount = newNames.length;
			tgtIndexs = new int[dcount][];
			
			for (int i = 0; i < dcount; ++i) {
				String []curNames = newNames[i];
				int curLen = curNames.length;
				int []tmp = new int[curLen];
				tgtIndexs[i] = tmp;
				
				for (int f = 0; f < curLen; ++f) {
					// ����¼ӵ��ֶ���Դ�����Ѵ������д�����ֶ�
					int index = oldDs.getFieldIndex(curNames[f]);
					if (index == -1) {
						tmp[f] = seq.length();
						seq.add(curNames[f]);
					} else {
						tmp[f] = index;
					}
				}
			}
		}

		String []names = new String[seq.length()];
		seq.toArray(names);
		newDs = new DataStruct(names);
		if (oldKey != null) {
			newDs.setPrimary(oldKey);
		}

		int count = codes.length;
		indexTables = new IndexTable[count];

		refIndexs = new int[count][];
		srcIndexs = new int[count][];
		for (int i = 0; i < count; ++i) {
			if (codes[i] == null) {
				continue;
			}
			
			int valCount = newExps[i].length;
			refIndexs[i] = new int[valCount];
			DataStruct ds = codes[i].dataStruct();
			for (int f = 0; f < valCount; f++) {
				int idx = ds.getFieldIndex(newExps[i][f].getIdentifierName());
				if (idx < 0) {
					refIndexs[i] = null;
					break;
				} else {
					refIndexs[i][f] = idx;
				}
			}
			
			valCount = exps[i].length;
			srcIndexs[i] = new int[valCount];
			ds = data.dataStruct();
			for (int f = 0; f < valCount; f++) {
				int idx = ds.getFieldIndex(exps[i][f].getIdentifierName());
				if (idx < 0) {
					srcIndexs[i] = null;
					break;
				} else {
					srcIndexs[i][f] = idx;
				}
			}
		}
		
		for (int i = 0; i < count; ++i) {
			Sequence code = codes[i];
			if (code == null) {
				continue;
			}

			Expression []curExps = dataExps[i];
			IndexTable indexTable;
			if (curExps == null) {
				indexTable = code.getIndexTable();
				if (indexTable == null) {
					Object obj = code.getMem(1);
					if (obj instanceof Record) {
						String[] pks = ((Record)obj).dataStruct().getPrimary();
						if (pks == null) {
							MessageManager mm = EngineMessage.get();
							throw new RQException(mm.getMessage("ds.lessKey"));
						}
						
						int pkCount = pks.length;
						if (exps[i].length != pkCount) {
							MessageManager mm = EngineMessage.get();
							throw new RQException("join" + mm.getMessage("function.invalidParam"));
						}

						if (pkCount > 1) {
							curExps = new Expression[pkCount];
							dataExps[i] = curExps;
							for (int k = 0; k < pkCount; ++k) {
								curExps[k] = new Expression(ctx, pks[k]);
							}
						}
					}

					indexTable = IndexTable.instance(code, curExps, ctx);
				}
			} else {
				int fcount = exps[i].length;
				if (fcount != curExps.length) {
					MessageManager mm = EngineMessage.get();
					throw new RQException("join" + mm.getMessage("function.invalidParam"));
				}

				// ���������#����������������
				if (fcount != 1 || !(curExps[0].getHome() instanceof CurrentSeq)) {
					indexTable = code.getIndexTable(curExps, ctx);
					if (indexTable == null) {
						indexTable = IndexTable.instance(code, curExps, ctx);
					}
				} else {
					indexTable = null;
				}
			}

			indexTables[i] = indexTable;
		}
	}
	
	/**
	 * �����α��ܵ���ǰ���͵�����
	 * @param seq ����
	 * @param ctx ����������
	 * @return
	 */
	public Sequence process(Sequence seq, Context ctx) {
		init(seq, ctx);
		if (isIsect) {
			return join_i(seq, ctx);
		} else {
			return join(seq, ctx);
		}
	}
	
	private Sequence join(Sequence data, Context ctx) {
		int len = data.length();
		Table result = new Table(newDs, len);
		ComputeStack stack = ctx.getComputeStack();
		
		// ��������C����ǰ���F������һ����join���
		
		if (isOrg) {
			for (int i = 1; i <= len; ++i) {
				Record old = (Record)data.getMem(i);
				result.newLast().setNormalFieldValue(0, old);
			}
			
			Sequence.Current current1 = data.new Current();
			Sequence.Current current2 = result.new Current();
			stack.push(current2);
			stack.push(current1);

			try {
				for (int fk = 0, fkCount = exps.length; fk < fkCount; ++fk) {
					IndexTable indexTable = indexTables[fk];
					Expression []curExps = exps[fk];
					Expression []curNewExps = newExps[fk];
					int newCount = curNewExps.length;
					int []refIndexs = this.refIndexs[fk];
					int []tgtIndexs = this.tgtIndexs[fk];
					
					if (codes[fk] == null) {
					} else if (indexTable != null) {
						int pkCount = curExps.length;
						Object []pkValues = new Object[pkCount];
		
						for (int i = 1; i <= len; ++i) {
							current1.setCurrent(i);
							current2.setCurrent(i);
							for (int f = 0; f < pkCount; ++f) {
								pkValues[f] = curExps[f].calculate(ctx);
							}
		
							Record obj = (Record)indexTable.find(pkValues);
							Record r = (Record)result.getMem(i);
							if (obj != null) {
								if (refIndexs == null) {
									try {
										stack.push(obj);
										for (int f = 0; f < newCount; ++f) {
											r.setNormalFieldValue(tgtIndexs[f], curNewExps[f].calculate(ctx));
										}
									} finally {
										stack.pop();
									}
								} else {
									Record rec= (Record) obj;
									for (int f = 0; f < newCount; ++f) {
										r.setNormalFieldValue(tgtIndexs[f], rec.getFieldValue(refIndexs[f]));
									}
								}
							} else {
								for (int f = 0; f < newCount; ++f) {
									r.setNormalFieldValue(tgtIndexs[f], null);
								}
							}
						}
					} else {
						Expression exp = curExps[0];
						ListBase1 codeMems = codes[fk].getMems();
						int codeLen = codeMems.size();
						for (int i = 1; i <= len; ++i) {
							current1.setCurrent(i);
							current2.setCurrent(i);
							Object val = exp.calculate(ctx);
							Record r = (Record)result.getMem(i);
							
							if (val instanceof Number) {
								int seq = ((Number)val).intValue();
								if (seq > 0 && seq <= codeLen) {
									Record obj = (Record)codeMems.get(seq);
									if (refIndexs == null) {
										try {
											stack.push(obj);
											for (int f = 0; f < newCount; ++f) {
												r.setNormalFieldValue(tgtIndexs[f], curNewExps[f].calculate(ctx));
											}
										} finally {
											stack.pop();
										}
									} else {
										Record rec= (Record) obj;
										for (int f = 0; f < newCount; ++f) {
											r.setNormalFieldValue(tgtIndexs[f], rec.getFieldValue(refIndexs[f]));
										}
									}
								} else {
									for (int f = 0; f < newCount; ++f) {
										r.setNormalFieldValue(tgtIndexs[f], null);
									}
								}
							} else {
								for (int f = 0; f < newCount; ++f) {
									r.setNormalFieldValue(tgtIndexs[f], null);
								}
							}
						}
					}
				}
			} finally {
				stack.pop();
				stack.pop();
			}
		} else {
			for (int i = 1; i <= len; ++i) {
				Record old = (Record)data.getMem(i);
				result.newLast(old.getFieldValues());
			}
			
			Sequence.Current current = result.new Current();
			stack.push(current);

			try {
				for (int fk = 0, fkCount = exps.length; fk < fkCount; ++fk) {
					IndexTable indexTable = indexTables[fk];
					Expression []curExps = exps[fk];
					Expression []curNewExps = newExps[fk];
					int newCount = curNewExps.length;
					int []refIndexs = this.refIndexs[fk];
					int []tgtIndexs = this.tgtIndexs[fk];
					
					if (codes[fk] == null) {
					} else if (indexTable != null) {
						int pkCount = curExps.length;
		
						if (pkCount == 1) {
							Object pkValue;
							for (int i = 1; i <= len; ++i) {
								current.setCurrent(i);
								pkValue = curExps[0].calculate(ctx);
			
								Record obj = (Record)indexTable.find(pkValue);
								Record r = (Record)result.getMem(i);
								if (obj != null) {
									if (refIndexs == null) {
										try {
											stack.push(obj);
											for (int f = 0; f < newCount; ++f) {
												r.setNormalFieldValue(tgtIndexs[f], curNewExps[f].calculate(ctx));
											}
										} finally {
											stack.pop();
										}
									} else {
										Record rec= (Record) obj;
										for (int f = 0; f < newCount; ++f) {
											r.setNormalFieldValue(tgtIndexs[f], rec.getFieldValue(refIndexs[f]));
										}
									}
								} else {
									for (int f = 0; f < newCount; ++f) {
										r.setNormalFieldValue(tgtIndexs[f], null);
									}
								}
							}
						} else {
							Object []pkValues = new Object[pkCount];
							for (int i = 1; i <= len; ++i) {
								current.setCurrent(i);
								for (int f = 0; f < pkCount; ++f) {
									pkValues[f] = curExps[f].calculate(ctx);
								}
			
								Record obj = (Record)indexTable.find(pkValues);
								Record r = (Record)result.getMem(i);
								if (obj != null) {
									if (refIndexs == null) {
										try {
											stack.push(obj);
											for (int f = 0; f < newCount; ++f) {
												r.setNormalFieldValue(tgtIndexs[f], curNewExps[f].calculate(ctx));
											}
										} finally {
											stack.pop();
										}
									} else {
										Record rec= (Record) obj;
										for (int f = 0; f < newCount; ++f) {
											r.setNormalFieldValue(tgtIndexs[f], rec.getFieldValue(refIndexs[f]));
										}
									}
								} else {
									for (int f = 0; f < newCount; ++f) {
										r.setNormalFieldValue(tgtIndexs[f], null);
									}
								}
							}
						}
					} else {
						Expression exp = curExps[0];
						ListBase1 codeMems = codes[fk].getMems();
						int codeLen = codeMems.size();
						for (int i = 1; i <= len; ++i) {
							current.setCurrent(i);
							Object val = exp.calculate(ctx);
							Record r = (Record)result.getMem(i);
							
							if (val instanceof Number) {
								int seq = ((Number)val).intValue();
								if (seq > 0 && seq <= codeLen) {
									Record obj = (Record)codeMems.get(seq);
									if (refIndexs == null) {
										try {
											stack.push(obj);
											for (int f = 0; f < newCount; ++f) {
												r.setNormalFieldValue(tgtIndexs[f], curNewExps[f].calculate(ctx));
											}
										} finally {
											stack.pop();
										}
									} else {
										Record rec= (Record) obj;
										for (int f = 0; f < newCount; ++f) {
											r.setNormalFieldValue(tgtIndexs[f], rec.getFieldValue(refIndexs[f]));
										}
									}
								} else {
									for (int f = 0; f < newCount; ++f) {
										r.setNormalFieldValue(tgtIndexs[f], null);
									}
								}
							} else {
								for (int f = 0; f < newCount; ++f) {
									r.setNormalFieldValue(tgtIndexs[f], null);
								}
							}
						}
					}
				}
			} finally {
				stack.pop();
			}
		}

		return result;
	}
	
	private Table join_i(Sequence data, Context ctx) {
		if (containNull) return null;
		
		Expression [][]exps = this.exps;
		Expression [][]newExps = this.newExps;
		int fkCount = exps.length;
		
		if (fkCount == 1) {
			if (codes[0].getMems() instanceof ColumnList) {
				return join_i_1_c(data, ctx);
			} else {
				return join_i_1(data, ctx);
			}
		}
		
		int len = data.length();
		Table result = new Table(newDs, len);

		Object [][]pkValues = new Object[fkCount][];
		for (int fk = 0; fk < fkCount; ++fk) {
			pkValues[fk] = new Object[exps[fk].length];
		}

		IndexTable []indexTables = this.indexTables;
		Sequence []codes = this.codes;
		boolean isOrg = this.isOrg;
		ComputeStack stack = ctx.getComputeStack();
		
		if (isOrg) {
			Record record = new Record(newDs);
			stack.push(record);
			Sequence.Current current = data.new Current();
			stack.push(current);

			try {
				Next:
				for (int i = 1; i <= len; ++i) {
					current.setCurrent(i);
					Record old = (Record)data.getMem(i);
					record.setNormalFieldValue(0, old);
				
					for (int fk = 0; fk < fkCount; ++fk) {
						Record dr;
						if (indexTables[fk] != null) {
							Expression []curExps = exps[fk];
							Object []curPkValues = pkValues[fk];
							for (int f = 0; f < curExps.length; ++f) {
								curPkValues[f] = curExps[f].calculate(ctx);
							}

							dr = (Record)indexTables[fk].find(curPkValues);
							if (dr == null) continue Next;
						} else {
							Object val = exps[fk][0].calculate(ctx);
							if (val instanceof Number) {
								int seq = ((Number)val).intValue();
								if (seq > 0 && seq <= codes[fk].length()) {
									dr = (Record)codes[fk].getMem(seq);
								} else {
									continue Next;
								}
							} else {
								continue Next;
							}
						}
						
						Expression []curNewExps = newExps[fk];
						int newCount = curNewExps.length;
						int []refIndexs = this.refIndexs[fk];
						int []tgtIndexs = this.tgtIndexs[fk];
						
						if (refIndexs == null) {
							try {
								stack.push(dr);
								for (int f = 0; f < newCount; ++f) {
									record.setNormalFieldValue(tgtIndexs[f], curNewExps[f].calculate(ctx));
								}
							} finally {
								stack.pop();
							}
						} else {
							Record rec= (Record) dr;
							for (int f = 0; f < newCount; ++f) {
								record.setNormalFieldValue(tgtIndexs[f], rec.getFieldValue(refIndexs[f]));
							}
						}
					}
					
					result.newLast(record.getFieldValues());
				}
			} finally {
				stack.pop();
				stack.pop();
			}
		} else {
			Record record = new Record(newDs);
			stack.push(record);

			try {
				Next:
				for (int i = 1; i <= len; ++i) {
					Record old = (Record)data.getMem(i);
					record.set(old);
				
					for (int fk = 0; fk < fkCount; ++fk) {
						Record dr;
						if (indexTables[fk] != null) {
							Expression []curExps = exps[fk];
							Object []curPkValues = pkValues[fk];
							for (int f = 0; f < curExps.length; ++f) {
								curPkValues[f] = curExps[f].calculate(ctx);
							}

							dr = (Record)indexTables[fk].find(curPkValues);
							if (dr == null) continue Next;
						} else {
							Object val = exps[fk][0].calculate(ctx);
							if (val instanceof Number) {
								int seq = ((Number)val).intValue();
								if (seq > 0 && seq <= codes[fk].length()) {
									dr = (Record)codes[fk].getMem(seq);
								} else {
									continue Next;
								}
							} else {
								continue Next;
							}
						}
						
						Expression []curNewExps = newExps[fk];
						int newCount = curNewExps.length;
						int []refIndexs = this.refIndexs[fk];
						int []tgtIndexs = this.tgtIndexs[fk];
						
						if (refIndexs == null) {
							try {
								stack.push(dr);
								for (int f = 0; f < newCount; ++f) {
								record.setNormalFieldValue(tgtIndexs[f], curNewExps[f].calculate(ctx));
								}
							} finally {
								stack.pop();
							}
						} else {
							Record rec= (Record) dr;
							for (int f = 0; f < newCount; ++f) {
								record.setNormalFieldValue(tgtIndexs[f], rec.getFieldValue(refIndexs[f]));
							}
						}
					}
					
					result.newLast(record.getFieldValues());
				}
			} finally {
				stack.pop();
			}
		}
		
		if (result.length() != 0) {
			return result;
		} else {
			return null;
		}
	}
	
	//codesֻ��1������ʱ
	private Table join_i_1(Sequence data, Context ctx) {
		Expression []exps = this.exps[0];
		Expression []newExps = this.newExps[0];
		int expsCount = exps.length;
		int newCount = newExps.length;
		
		int len = data.length();
		Table result = new Table(newDs, len);

		Object []pkValues = new Object[expsCount];

		IndexTable indexTables = this.indexTables[0];
		Sequence codes = this.codes[0];
		boolean isOrg = this.isOrg;
		ComputeStack stack = ctx.getComputeStack();
		int []refIndexs = this.refIndexs[0];
		int []srcIndexs = this.srcIndexs[0];
		int []tgtIndexs = this.tgtIndexs[0];
		
		if (isOrg) {
			Record record = new Record(newDs);
			stack.push(record);
			Sequence.Current current = data.new Current();
			stack.push(current);

			try {
				Next:
				for (int i = 1; i <= len; ++i) {
					current.setCurrent(i);
					Record old = (Record)data.getMem(i);
					record.setNormalFieldValue(0, old);
					int findex = 1;

					Record dr;
					if (indexTables != null) {
						if (srcIndexs == null) {
							for (int f = 0; f < expsCount; ++f) {
								pkValues[f] = exps[f].calculate(ctx);
							}
						} else {
							for (int f = 0; f < expsCount; ++f) {
								pkValues[f] = old.getFieldValue(srcIndexs[f]);
							}
						}
						
						dr = (Record)indexTables.find(pkValues);
						if (dr == null) continue Next;
					} else {
						Object val = exps[0].calculate(ctx);
						if (val instanceof Number) {
							int seq = ((Number)val).intValue();
							if (seq > 0 && seq <= codes.length()) {
								dr = (Record)codes.getMem(seq);
							} else {
								continue Next;
							}
						} else {
							continue Next;
						}
					}
					
					if (refIndexs == null) {
						try {
							stack.push(dr);
							for (int f = 0; f < newCount; ++f, ++findex) {
								record.setNormalFieldValue(findex, newExps[f].calculate(ctx));
							}
						} finally {
							stack.pop();
						}
					} else {
						Record rec= (Record) dr;
						for (int f = 0; f < newCount; ++f, ++findex) {
							record.setNormalFieldValue(findex, rec.getFieldValue(refIndexs[f]));
						}
					}
					
					result.newLast(record.getFieldValues());
				}
			} finally {
				stack.pop();
				stack.pop();
			}
		} else {
			Record record = new Record(newDs);
			stack.push(record);

			try {
				Next:
				for (int i = 1; i <= len; ++i) {
					Record old = (Record)data.getMem(i);
					record.set(old);

					Record dr;
					if (indexTables != null) {
						if (srcIndexs == null) {
							for (int f = 0; f < expsCount; ++f) {
								pkValues[f] = exps[f].calculate(ctx);
							}
						} else {
							for (int f = 0; f < expsCount; ++f) {
								pkValues[f] = old.getFieldValue(srcIndexs[f]);
							}
						}
						
						dr = (Record)indexTables.find(pkValues);
						if (dr == null) continue Next;
					} else {
						Object val = exps[0].calculate(ctx);
						if (val instanceof Number) {
							int seq = ((Number)val).intValue();
							if (seq > 0 && seq <= codes.length()) {
								dr = (Record)codes.getMem(seq);
							} else {
								continue Next;
							}
						} else {
							continue Next;
						}
					}
					
					if (refIndexs == null) {
						try {
							stack.push(dr);
							for (int f = 0; f < newCount; ++f) {
								record.setNormalFieldValue(tgtIndexs[f], newExps[f].calculate(ctx));
							}
						} finally {
							stack.pop();
						}
					} else {
						Record rec= (Record) dr;
						for (int f = 0; f < newCount; ++f) {
							record.setNormalFieldValue(tgtIndexs[f], rec.getFieldValue(refIndexs[f]));
						}
					}

					result.newLast(record.getFieldValues());
				}
			} finally {
				stack.pop();
			}
		}
		
		if (result.length() != 0) {
			return result;
		} else {
			return null;
		}
	}
	
	//codesֻ��1��������code��ѹ����ʱ
	private Table join_i_1_c(Sequence data, Context ctx) {
		Expression []exps = this.exps[0];
		Expression []newExps = this.newExps[0];
		int expsCount = exps.length;
		int newCount = newExps.length;
		
		ColumnList mems = (ColumnList) codes[0].getMems();
		Column columns[] = mems.getColumns();
		
		int len = data.length();
		Table result = new Table(newDs, len);

		Object []pkValues = new Object[expsCount];

		CompressIndexTable indexTables = (CompressIndexTable) this.indexTables[0];
		Sequence codes = this.codes[0];
		boolean isOrg = this.isOrg;
		ComputeStack stack = ctx.getComputeStack();
		int []refIndexs = this.refIndexs[0];
		int []srcIndexs = this.srcIndexs[0];
		int []tgtIndexs = this.tgtIndexs[0];
		
		//�ж��Ƿ�findex���˳���Ƿ������Ȼ˳��
		boolean eindexIsOrder = true;
		for (int f = 0; f < expsCount; ++f) {
			if (f != srcIndexs[f]) {
				eindexIsOrder = false;
				break;
			}
		}
		
		boolean findexIsOrder = true;
		for (int f = 0; f < newCount; ++f) {
			if (f != refIndexs[f]) {
				findexIsOrder = false;
				break;
			}
		}
		
		if (isOrg) {
			Record record = new Record(newDs);
			stack.push(record);
			Sequence.Current current = data.new Current();
			stack.push(current);

			try {
				Next:
				for (int i = 1; i <= len; ++i) {
					current.setCurrent(i);
					Record old = (Record)data.getMem(i);
					record.setNormalFieldValue(0, old);
					int findex = 1;
				

					int dr;
					if (indexTables != null) {
						if (srcIndexs == null) {
							for (int f = 0; f < expsCount; ++f) {
								pkValues[f] = exps[f].calculate(ctx);
							}
						} else if (eindexIsOrder) {
							for (int f = 0; f < expsCount; ++f) {
								pkValues[f] = old.getFieldValue(f);
							}
						} else {
							for (int f = 0; f < expsCount; ++f) {
								pkValues[f] = old.getFieldValue(srcIndexs[f]);
							}
						}
						
						dr = indexTables.pfindByFields(pkValues);
						if (dr == 0) continue Next;
					} else {
						Object val = exps[0].calculate(ctx);
						if (val instanceof Number) {
							int seq = ((Number)val).intValue();
							if (seq > 0 && seq <= codes.length()) {
								dr = seq;
							} else {
								continue Next;
							}
						} else {
							continue Next;
						}
					}
					
					if (refIndexs == null) {
						try {
							stack.push((Record)mems.get(dr));
							for (int f = 0; f < newCount; ++f, ++findex) {
								record.setNormalFieldValue(findex, newExps[f].calculate(ctx));
							}
						} finally {
							stack.pop();
						}
					} else if (findexIsOrder) {
						for (int f = 0; f < newCount; ++f, ++findex) {
							record.setNormalFieldValue(findex, columns[f].getData(dr));
						}
					} else {
						for (int f = 0; f < newCount; ++f, ++findex) {
							record.setNormalFieldValue(findex, columns[refIndexs[f]].getData(dr));
						}
					}
				
					
					result.newLast(record.getFieldValues());
				}
			} finally {
				stack.pop();
				stack.pop();
			}
		} else {
			Record record = new Record(newDs);
			stack.push(record);

			try {
				Next:
				for (int i = 1; i <= len; ++i) {
					Record old = (Record)data.getMem(i);
					record.set(old);

					int dr;
					if (indexTables != null) {
						if (srcIndexs == null) {
							for (int f = 0; f < expsCount; ++f) {
								pkValues[f] = exps[f].calculate(ctx);
							}
						} else if (eindexIsOrder) {
							for (int f = 0; f < expsCount; ++f) {
								pkValues[f] = old.getFieldValue(f);
							}
						} else {
							for (int f = 0; f < expsCount; ++f) {
								pkValues[f] = old.getFieldValue(srcIndexs[f]);
							}
						}
						
						dr = indexTables.pfindByFields(pkValues);
						if (dr == 0) continue Next;
					} else {
						Object val = exps[0].calculate(ctx);
						if (val instanceof Number) {
							int seq = ((Number)val).intValue();
							if (seq > 0 && seq <= codes.length()) {
								dr = seq;
							} else {
								continue Next;
							}
						} else {
							continue Next;
						}
					}
					
					if (refIndexs == null) {
						try {
							stack.push((Record)mems.get(dr));
							for (int f = 0; f < newCount; ++f) {
								record.setNormalFieldValue(tgtIndexs[f], newExps[f].calculate(ctx));
							}
						} finally {
							stack.pop();
						}
					} else if (findexIsOrder) {
						for (int f = 0; f < newCount; ++f) {
							record.setNormalFieldValue(tgtIndexs[f], columns[f].getData(dr));
						}
					} else {
						for (int f = 0; f < newCount; ++f) {
							record.setNormalFieldValue(tgtIndexs[f], columns[refIndexs[f]].getData(dr));
						}
					}

					result.newLast(record.getFieldValues());
				}
			} finally {
				stack.pop();
			}
		}
		
		if (result.length() != 0) {
			return result;
		} else {
			return null;
		}
	}
}