package com.weezlabs.tools.android.screenshoter.ui;


import com.weezlabs.libs.screenshoter.model.Mode;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * Created by vfarafonov on 15.02.2016.
 */
public class ModesTableModel extends AbstractTableModel {
	public final static int COLUMNS_COUNT = 3;
	private final static String[] COLUMN_NAMES = {"Size", "Density", ""};

	private final List<Mode> values_ = new ArrayList<>();

	@Override
	public int getRowCount() {
		return values_.size();
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return columnIndex == 2;
	}

	@Override
	public int getColumnCount() {
		return COLUMNS_COUNT;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (rowIndex < values_.size()) {
			Mode mode = values_.get(rowIndex);
			switch (columnIndex) {
				case 0:
					return mode.getResolution().toString();
				case 1:
					return mode.getDensity().toString();
				case 2:
					return mode.isActivated();
			}
		}
		return null;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		if (columnIndex == 2) {
			values_.get(rowIndex).setIsActivated((Boolean) aValue);
		} else {
			super.setValueAt(aValue, rowIndex, columnIndex);
		}
	}

	public List<Mode> getValues() {
		return values_;
	}

	@Override
	public String getColumnName(int column) {
		return COLUMN_NAMES[column];
	}

	public void clearRows() {
		int count = values_.size();
		values_.clear();
		fireTableRowsDeleted(0, count);
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
			case 0:
			case 1:
				return String.class;
			case 2:
				return Boolean.class;
			default:
				return String.class;
		}
	}

	public void add(Mode mode) {
		values_.add(mode);
		fireTableRowsInserted(values_.size() - 1, values_.size() - 1);
	}

	public void addAll(List<Mode> modes) {
		values_.clear();
		values_.addAll(modes);
		fireTableDataChanged();
	}

	public List<Mode> getExcludedModes() {
		List<Mode> modes = new ArrayList<>();
		for (Mode mode : values_) {
			if (!mode.isActivated()) {
				modes.add(mode);
			}
		}
		return modes;
	}
}
